package com.example.silentsos;


import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.Inflater;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private FirebaseAuth mAuth;
    private String mUserId;
    private ImageView mSOSButton;
    private User mUser;
    private Button mEditButton;
    private Button mAddContactButton;
    private EditText mMessageEditText;
    private CheckBox mLocationCheckbox;
    private LinearLayout mActivationSequenceLayout;
    private LinearLayout mContactsLayout;
    private boolean mHoldingButton;
    private TextView mLocationText;
    private ImageView mUserImage;
    private Button mGoNowButton;

    // sos stuff
    private String mUserMessage;
    private List<EmergencyContact> mEmergencyContacts = new ArrayList<>();
    private FusedLocationProviderClient mFusedLocationClient;
    private String mFinalMessage;

    // received sms and location permission, proceed with launching foreground service
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i=0;i<grantResults.length;i++){
                if (grantResults[i]==PackageManager.PERMISSION_GRANTED){
                    switch (permissions[i]){
                        case Manifest.permission.SEND_SMS:
                            launchForegroundService();
                            break;
                        case Manifest.permission.ACCESS_COARSE_LOCATION:
//                            launchForegroundService();

                            break;
                        case Manifest.permission.ACCESS_FINE_LOCATION:
//                            launchForegroundService();
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void launchForegroundService(){
        // create notification
        Intent serviceIntent = new Intent(this, SOSForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void vibrateDevice() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(500); // Vibrate for 0.5 seconds
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
        boolean firstLaunch = preferences.getBoolean("first_launch", true);
        startActivity(new Intent(this, WelcomeActivity.class));
        if (firstLaunch) {
            startActivity(new Intent(this, WelcomeActivity.class));
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("first_launch", false);
            editor.apply();
        }


        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user==null){
            Log.w("Auth", "No user is signed in yet");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // request sms and location permissions
        List<String> permissionsToRequest = new ArrayList<>();
        String[] requiredPermissions = new String[]{
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                permissionsToRequest.add(permission);
            }else if (permission.equals(Manifest.permission.SEND_SMS)){
                launchForegroundService();
            }
        }

        String[] permissionArray = permissionsToRequest.toArray(new String[0]);
        if (permissionArray.length > 0) ActivityCompat.requestPermissions(this, permissionArray, PERMISSION_REQUEST_CODE);

        // user account page
        mUserImage = (ImageView) findViewById(R.id.userImage);
        mUserImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, UserActivity.class));
            }
        });


        // sos button functionality
        mLocationText = (TextView) findViewById(R.id.locationTextView);
        setCurrentLocation();

        Handler handler = new Handler();
        Runnable holdRunnable = new Runnable() {
            @Override
            public void run() {
                mHoldingButton = false;
                Toast.makeText(MainActivity.this, "SOS Sent!", Toast.LENGTH_SHORT).show();
                vibrateDevice();
                sendSOS();
            }
        };


        CountDownTimer holdTimer = new CountDownTimer(3000, 800) {
            public void onTick(long millisUntilFinished) {
                int countdown = (int) Math.round((double) millisUntilFinished / 1000);
                String progress = String.valueOf(countdown);

                TextView holdFor3SecText = (TextView) findViewById(R.id.holdFor3SecText);
                TextView sosButtonText = (TextView) findViewById(R.id.SOSButtonText);

                holdFor3SecText.setText("");
                sosButtonText.setText(progress);
            }

            @Override
            public void onFinish() {
                TextView holdFor3SecText = (TextView) findViewById(R.id.holdFor3SecText);
                TextView sosButtonText = (TextView) findViewById(R.id.SOSButtonText);

                holdFor3SecText.setText("Hold tight");
                sosButtonText.setText("SOS Sent");
            }
        };

        loadUserData();

        mSOSButton = (ImageView) findViewById(R.id.sosButton);
        mSOSButton.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL) {
                    mSOSButton.setImageResource(R.drawable.sos_button);
                    v.performClick();// for accessibility purposes

                    TextView holdFor3SecText = (TextView) findViewById(R.id.holdFor3SecText);
                    TextView sosButtonText = (TextView) findViewById(R.id.SOSButtonText);

                    if (mHoldingButton){
                        handler.removeCallbacks(holdRunnable);
                        mHoldingButton = false;
                        holdFor3SecText.setText(getString(R.string.hold_for_3_seconds));
                        sosButtonText.setText(getString(R.string.sos));
                        holdTimer.cancel();
                    }else{
                        holdFor3SecText.setText("Hold tight");
                        sosButtonText.setText("SOS Sent");
                    }
                    return true;

                }else if(event.getAction() == MotionEvent.ACTION_DOWN){
                    mSOSButton.setImageResource(R.drawable.sos_button_pressed);
                    mHoldingButton = true;

                    // show countdown till sos sent
                    holdTimer.start();

                    handler.postDelayed(holdRunnable, 3000);

                    return true;
                }
                return true;
            }
        });

        mEditButton = (Button) findViewById(R.id.editButton);
        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditAlarmActivity.class));
            }
        });

        mAddContactButton = (Button) findViewById(R.id.addContactButton);
        mAddContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditContactsActivity.class));
            }
        });

        mContactsLayout = (LinearLayout) findViewById(R.id.contactLayout);
        mActivationSequenceLayout = (LinearLayout) findViewById(R.id.buttonSequenceLayout);
        mLocationCheckbox = (CheckBox) findViewById(R.id.locationCheckbox);
        mMessageEditText = (EditText) findViewById(R.id.messageEditView);
        // set user values for alarm and contacts if that exists
        mUserId = user.getUid();

        // make user set up their alarm if they don't have one yet
        // alternatively, if they have, then update main page with user info
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("users")
                .document(mUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {// user exists
                        mUser = documentSnapshot.toObject(User.class);
                        boolean alarmSet = mUser.isAlarmSet();
                        // alarm doesn't exist yet
                        if (!alarmSet){
                            startActivity(new Intent(this, CreateAlarmActivity.class));
                            finish();
                        }

                        updatePage();
                    }else{
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(exception -> {

                });

        mGoNowButton = (Button) findViewById(R.id.goNowButton);
        mGoNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TutorialActivity.class));
            }
        });
    }

    private void setCurrentLocation(){
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            updateLocationText(location);
                        } else {
                            mFusedLocationClient.getCurrentLocation(
                                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                                    null
                            ).addOnSuccessListener(newLocation -> {
                                if (newLocation != null) {
                                    updateLocationText(newLocation);
                                } else {
                                    mLocationText.setText("Location not available");
                                }
                            });
                        }
                    });
        }else{
            mLocationText.setText("Location not available");
        }
    }

    private void updateLocationText(Location location){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latitude, longitude, 1
            );
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String locationName = address.getAddressLine(0); // Get the full address line
                mLocationText.setText(locationName);
            } else {
                mLocationText.setText("Location not available");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                                if(user.isAlarmSet()) {
                                    Alarm alarm = user.getAlarm();
                                    mUserMessage = alarm.getMessage();
                                    mEmergencyContacts = user.getEmergencyContacts();
                                }else{
                                    startActivity(new Intent(this, CreateAlarmActivity.class));
                                }
                            }
                        }
                )
                .addOnFailureListener(aVoid -> {
                            Toast.makeText(this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                        }
                );
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
                            // Got last known location. In some rare situations this can be null.
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

    private void updatePage(){
        if (mUser.isAlarmSet()) {
            Alarm alarm = mUser.getAlarm();
            mMessageEditText.setText(alarm.getMessage());
            mUserMessage = alarm.getMessage();

            mLocationCheckbox.setChecked(alarm.isIncludeLocation());

            updateButtonSequence(mActivationSequenceLayout, alarm.getActivationSequence());
        }

        // set emergency contacts
        mEmergencyContacts = mUser.getEmergencyContacts();
        mContactsLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        final View addNewContactView = inflater.inflate(R.layout.add_new_contact_button, mContactsLayout, false);
        mContactsLayout.addView(addNewContactView);
        mAddContactButton = (Button) addNewContactView.findViewById(R.id.addContactButton);
        mAddContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditContactsActivity.class));
            }
        });
        if(mEmergencyContacts!=null){
            for (EmergencyContact contact : mEmergencyContacts){
                addNewContact(contact.getName(), contact.getNumber());
            }
        }
    }

    private void addNewContact(String name, String number){
        EmergencyContact newContact = new EmergencyContact(name, number);

        LayoutInflater inflater = LayoutInflater.from(this);

        final View newContactView = inflater.inflate(R.layout.contact_item, mContactsLayout, false);

        TextView nameTextView = newContactView.findViewById(R.id.nameTextView);
        TextView numberTextView = newContactView.findViewById(R.id.numberTextView);
        LinearLayout closeButton = newContactView.findViewById(R.id.closeButton);

        closeButton.setVisibility(View.GONE);

        nameTextView.setText(name);
        numberTextView.setText(number);

        mContactsLayout.addView(newContactView, 0);
    }

    private void updateButtonSequence(LinearLayout activationSequenceLayout, List<Boolean> sequence){
        activationSequenceLayout.removeAllViews();
        for (boolean b : sequence) {
            ImageView newImageView = getImageView(b);

            activationSequenceLayout.addView(newImageView);
        }
    }

    @NonNull
    private ImageView getImageView(boolean b) {
        ImageView newImageView = new ImageView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, 100);
        layoutParams.setMargins(0, 0, 10, 0);

        newImageView.setLayoutParams(layoutParams);
        if (b) {
            newImageView.setContentDescription("Up");
            newImageView.setImageResource(R.drawable.volume_up);
        } else {
            newImageView.setContentDescription("Down");
            newImageView.setImageResource(R.drawable.volume_down);
        }
        return newImageView;
    }

    @Override
    protected void onResume(){
        super.onResume();
        setCurrentLocation();

        if(mUser!=null) {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection("users")
                    .document(mUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {// user exists
                            mUser = documentSnapshot.toObject(User.class);
                            updatePage();
                        }
                    });
        }
    }
}