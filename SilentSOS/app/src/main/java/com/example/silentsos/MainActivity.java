package com.example.silentsos;


import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.Image;
import android.os.Bundle;
import android.os.Vibrator;
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

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_SMS_PERMISSION = 100;
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

    // request SMS permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // create notification
                Intent serviceIntent = new Intent(this, SOSForegroundService.class);
                ContextCompat.startForegroundService(this, serviceIntent);
            } else {
                Toast.makeText(this, "Permission denied to send SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
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

        mSOSButton = (ImageView) findViewById(R.id.sosButton);
        mSOSButton.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mSOSButton.setImageResource(R.drawable.sos_button);
                    v.performClick();// for accessibility purposes
                    vibrateDevice();
                }else if(event.getAction() == MotionEvent.ACTION_DOWN){
                    mSOSButton.setImageResource(R.drawable.sos_button_pressed);
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
    }

    private void updatePage(){
        if (mUser.isAlarmSet()) {
            Alarm alarm = mUser.getAlarm();
            mMessageEditText.setText(alarm.getMessage());

            mLocationCheckbox.setChecked(alarm.isIncludeLocation());

            updateButtonSequence(mActivationSequenceLayout, alarm.getActivationSequence());
        }

        // set emergency contacts
        List<EmergencyContact> contacts = mUser.getEmergencyContacts();
        if(contacts!=null){
            for (EmergencyContact contact : contacts){
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
}