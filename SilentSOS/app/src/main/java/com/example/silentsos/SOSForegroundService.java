package com.example.silentsos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SOSForegroundService extends Service {
    private AudioManager audioManager;
    private MediaSession mediaSession;
    private long lastPressTime = 0;
    private List<Boolean> mCurrentSequence = new ArrayList<>();
    private List<Boolean> mUserSequence = new ArrayList<>();
    private String mUserMessage;
    private List<EmergencyContact> mEmergencyContacts = new ArrayList<>();

    private int lastVolume;
    private Handler volumeHandler;
    private Runnable volumeChecker;

    private PendingIntent mPendingIntent;
    private FusedLocationProviderClient mFusedLocationClient;

    private String mFinalMessage;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("SOSForegroundService", "Starting");
        startForegroundServiceNotification();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startVolumePolling();
        loadUserData();
    }

    private void loadUserData(){
        // get alarm activation sequence and emergency contact info
        // must be done after foreground service is started to prevent it from slowing down the service
        FirebaseApp.initializeApp(this);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(
                        documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                User user = documentSnapshot.toObject(User.class);
                                Alarm alarm = user.getAlarm();
                                mUserSequence = alarm.getActivationSequence();
                                mUserMessage = alarm.getMessage();
                                mEmergencyContacts = user.getEmergencyContacts();

                                startVolumePolling();
                            }else{
                                stopSelf();
                            }
                        }
                )
                .addOnFailureListener(aVoid -> {
                            Toast.makeText(this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                            stopSelf();
                        }
                );
    }

    private void startVolumePolling() {
        volumeHandler = new Handler(getMainLooper());
        volumeChecker = new Runnable() {
            @Override
            public void run() {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                if (currentVolume > lastVolume) {
                    handleVolumeButton(KeyEvent.KEYCODE_VOLUME_UP);
                    Log.i("SOSForegroundService", "Up");
                } else if (currentVolume < lastVolume) {
                    handleVolumeButton(KeyEvent.KEYCODE_VOLUME_DOWN);
                    Log.i("SOSForegroundService", "Down");
                }

                lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // update in case reset fails

                volumeHandler.postDelayed(this, 100); // check every 100ms
            }
        };

        volumeHandler.post(volumeChecker);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // keep running until explicitly stopped
    }

    private void startForegroundServiceNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        int flagImmutable = PendingIntent.FLAG_IMMUTABLE;
        mPendingIntent = PendingIntent.getActivity(
                this,
                0, // Request code
                notificationIntent,
                flagImmutable
        );

        String channelId = "sos_service_channel";
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    channelId, "SOS Service", NotificationManager.IMPORTANCE_LOW);
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }

        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, channelId)
                    .setContentTitle("Super Silent is running")
                    .setContentText("Tap to return to the app")
                    .setSmallIcon(R.drawable.logo)
                    .setContentIntent(mPendingIntent)
                    .build();
        }

        startForeground(1, notification);
    }

    private void handleVolumeButton(int keyCode) {
        long now = System.currentTimeMillis();
        if (now - lastPressTime > 2000) { // reset sequence after 2s idle
            mCurrentSequence.clear();
        }
        lastPressTime = now;

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) mCurrentSequence.add(true);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) mCurrentSequence.add(false);

        Log.i("SOSForegroundService", mCurrentSequence.toString());

        if (mCurrentSequence.equals(mUserSequence)) {
            sendSOS();
            mCurrentSequence.clear();
        }
    }

    private void sendSOS(){
        // vibrate to let user know correct pattern was entered
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
        }

        // start building sos message
        mFinalMessage = mUserMessage;

        // get current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            Log.i("SOSForegroundService", "Location successfully fetched");
                            if (location != null) {
                                String locationString = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES) + " " + Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
                                mFinalMessage += "\n\nLast Known Location: "+locationString;
                                Log.i("SOSForegroundService", mFinalMessage);
                                sendSMSMessage();
                            }else{
                                Log.i("SOSForegroundService", "Location is null");
                                mFinalMessage += "\n\nLast Known Location was not available.";
                                sendSMSMessage();
                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("SOSForegroundService", "Error getting last known location", e);
                            mFinalMessage += "\n\nLast Known Location was not available.";
                            sendSMSMessage();
                        }
                    });
        }

    }

    private void sendSMSMessage(){
        // actually send SMS
        try {
            for (EmergencyContact contact : mEmergencyContacts){
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(
                        contact.getNumber(),
                        null,
                        mFinalMessage,
                        null,
                        null);
                Log.i("SOSForegroundService", "SMS Sending succeeded");
            }
        }catch (Exception e){
            Log.e("SOSForegroundService", "SMS Sending failed");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (volumeHandler != null && volumeChecker != null) {
            volumeHandler.removeCallbacks(volumeChecker);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
