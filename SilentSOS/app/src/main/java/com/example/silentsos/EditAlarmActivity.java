package com.example.silentsos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EditAlarmActivity extends AppCompatActivity {
    private static final String TAG = "EditAlarmActivity";
    private static final String KEY_NEW_SEQUENCE = "newSequence";

    private Button mFinishButton;
    private EditText mMessageEditText;
    private CheckBox mLocationCheckbox;
    private TextView mSetNewSequenceText;
    private FirebaseAuth mAuth;
    private User mUser;
    private String mUserId;
    private Button mBackButton;
    private LinearLayout mActivationSequenceLayout;

    private boolean[] mSequence = {true, true, false, false, false};

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseApp.initializeApp(this);
        if (FirebaseAuth.getInstance().getCurrentUser()==null){
            startActivity(new Intent(this, LoginActivity.class));
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_alarm);

        // set new button sequence set up
        ActivityResultLauncher<Intent> mNewSequenceActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
//                            doSomeOperations();
                            mSequence = data.getBooleanArrayExtra(KEY_NEW_SEQUENCE);
                            //convert primitive boolean to Boolean object
                            List<Boolean> processedSequence = new ArrayList<Boolean>();
                            for (boolean b : mSequence){
                                processedSequence.add(b);
                            }
                            updateButtonSequence(mActivationSequenceLayout, processedSequence);
                        }
                    }
                });

        // page set up
        mAuth = FirebaseAuth.getInstance();

        mFinishButton = (Button) findViewById(R.id.finishButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditView);
        mLocationCheckbox = (CheckBox) findViewById(R.id.locationCheckbox);
        mSetNewSequenceText = (TextView) findViewById(R.id.setNewSequenceText);
        mBackButton = (Button) findViewById(R.id.backButton);

        mFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAlarm();
            }
        });

        mSetNewSequenceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(EditAlarmActivity.this, ActivationSequenceActivity.class );
                mNewSequenceActivityResultLauncher.launch(i);
            }
        });


        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // restore already existing alarm buttons
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user==null){
            Log.w("Auth", "No user is signed in yet");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

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
                            Toast.makeText(this, String.valueOf(alarmSet), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, CreateAlarmActivity.class));
                            finish();
                        }

                        if (mUser.isAlarmSet()) {
                            Alarm alarm = mUser.getAlarm();
                            mMessageEditText.setText(alarm.getMessage());

                            mLocationCheckbox.setChecked(alarm.isIncludeLocation());

                            updateButtonSequence(mActivationSequenceLayout, alarm.getActivationSequence());
                        }
                    }else{
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(exception -> {

                });
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

    private void saveAlarm(){
        boolean includeLocation = mLocationCheckbox.isChecked();
        String message = mMessageEditText.getText().toString();
        boolean[] sequence = mSequence;

        List<Boolean> sequenceList = new ArrayList<>();

        for (boolean b : mSequence) {
            sequenceList.add(b);
        }

        Alarm newAlarm = new Alarm(
                includeLocation, message, sequenceList
        );

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String Uid = mAuth.getCurrentUser().getUid();


        database
                .collection("users")
                .document(Uid)
                .update("alarmSet", true)
                .addOnSuccessListener(Intent ->{
                    Log.w("Firestore", "Alarm set successfully");
                })
                .addOnFailureListener(e -> Log.w("Firestore", "Error creating Firestore user", e));

        database
                .collection("users")
                .document(Uid)
                .update("alarm", newAlarm)
                .addOnSuccessListener(aVoid ->{
                    finish();
                })
                .addOnFailureListener(e -> Log.w("Firestore", "Error creating Firestore user", e));

        Toast.makeText(this, "Alarm updated", Toast.LENGTH_SHORT).show();


    }
}