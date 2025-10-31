package com.example.silentsos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;


public class SOSForegroundService extends Service {
    private AudioManager audioManager;
    private MediaSession mediaSession;
    private long lastPressTime = 0;
    private StringBuilder sequence = new StringBuilder();
    private final String ACTIVATION_SEQUENCE = "1212"; // example sequence

    private int lastVolume;
    private Handler volumeHandler;
    private Runnable volumeChecker;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceNotification();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        startVolumePolling();
    }

    private void startVolumePolling() {
        volumeHandler = new Handler(getMainLooper());
        volumeChecker = new Runnable() {
            @Override
            public void run() {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                if (currentVolume > lastVolume) {
                    handleVolumeButton(KeyEvent.KEYCODE_VOLUME_UP);
//                    resetVolume(); // so user doesn't notice
                    Log.i("SOSForegroundService", "Up");
                } else if (currentVolume < lastVolume) {
                    handleVolumeButton(KeyEvent.KEYCODE_VOLUME_DOWN);
//                    resetVolume(); // so user doesn't notice
                    Log.i("SOSForegroundService", "Down");
                }

                lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // update in case reset fails

                volumeHandler.postDelayed(this, 100); // check every 100ms
            }
        };

        volumeHandler.post(volumeChecker);
    }

    private void resetVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, lastVolume, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Keep running until explicitly stopped
    }

    private void startForegroundServiceNotification() {
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
                    .setContentTitle("SOS service running")
                    .setSmallIcon(R.drawable.ic_sos_button)
                    .build();
        }

        startForeground(1, notification);
    }

//    private void setupMediaSession() {
//        mediaSession = new MediaSession(this, "SOSMediaSession");
//        mediaSession.setCallback(new MediaSession.Callback() {
//            @Override
//            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
//                KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
//                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
//                    int keyCode = keyEvent.getKeyCode();
//                    handleVolumeButton(keyCode);
//                }
//                return true;
//            }
//        });
//
//        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
//        mediaSession.setActive(true);
//    }

    private void handleVolumeButton(int keyCode) {
        long now = System.currentTimeMillis();
        if (now - lastPressTime > 2000) { // reset sequence after 2s idle
            sequence.setLength(0);
        }
        lastPressTime = now;



        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) sequence.append("1");
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) sequence.append("2");

        Log.i("SOSForegroundService", sequence.toString());

        if (sequence.toString().equals(ACTIVATION_SEQUENCE)) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            sequence.setLength(0);
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
