package com.example.silentsos;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;


public class MainActivity extends AppCompatActivity {
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

    private boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;

        List<AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
            if (serviceInfo.packageName.equals(context.getPackageName())
                    && serviceInfo.name.equals(service.getName())) {
                return true;
            }
        }

        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onStart() {
        super.onStart();

        if (!isAccessibilityServiceEnabled(this, VolumeButtonService.class)) {
            // Permission not granted yet, prompt user to open Accessibility Settings
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        } else {
            // Permission already granted — continue normally
            Log.d("AccessibilityCheck", "Accessibility Service already enabled!");
        }
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