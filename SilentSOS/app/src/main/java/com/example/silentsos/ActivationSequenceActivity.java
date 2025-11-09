package com.example.silentsos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActivationSequenceActivity extends AppCompatActivity {
    private static final String TAG = "ActivationSequenceActivity";
    private static final String KEY_NEW_SEQUENCE = "newSequence";

    private Button mBackButton;
    private Button mConfirmButton;
    private Button mResetButton;
    private TextView mSequenceBarTitle;
    private LinearLayout mButtonSequenceLayout;

    private List<Boolean> mSequence = new ArrayList<Boolean>();

    private FirebaseAuth mAuth;

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
        setContentView(R.layout.activity_activation_sequence);

        mAuth = FirebaseAuth.getInstance();

        mSequenceBarTitle = (TextView) findViewById(R.id.sequenceBarTitle);
        mSequenceBarTitle.setText(getString(R.string.activation_sequence)+" (0/5)");

        mBackButton = (Button) findViewById(R.id.backButton);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mConfirmButton = (Button) findViewById(R.id.confirmButton);

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSequenceAndLeave();
            }
        });

        mResetButton = (Button) findViewById(R.id.resetButton);

        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetSequence();
            }
        });

        mButtonSequenceLayout = (LinearLayout) findViewById(R.id.buttonSequenceLayout);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN){
            addNewButton(false);
        }else if (keyCode==KeyEvent.KEYCODE_VOLUME_UP){
            addNewButton(true);
        }
        return true;
    }

    private void saveSequenceAndLeave(){
        if(mSequence.size()==4) {
            Intent data = new Intent();
            // converting List<Boolean> to primitive boolean[]
            boolean[] booleanSequence = new boolean[mSequence.size()];
            for (int i = 0; i < mSequence.size(); i++) {
                booleanSequence[i] = mSequence.get(i);
            }
            data.putExtra(KEY_NEW_SEQUENCE, booleanSequence);
            setResult(RESULT_OK, data);
            finish();
        }else{
            Toast.makeText(this, "Sequence must be 4 buttons minimum", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSequence(){
        mButtonSequenceLayout.removeAllViews();
        mSequence.clear();
        mSequenceBarTitle.setText(getString(R.string.activation_sequence)+" (0/5)");
    }

    private void addNewButton(boolean isUp){
        if(mSequence.size()<5) {
            mSequence.add(isUp);
            ImageView newImageView = new ImageView(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, 100);
            layoutParams.setMargins(0, 0, 10, 0);

            newImageView.setLayoutParams(layoutParams);

            if (isUp) {
                newImageView.setContentDescription("Up");
                newImageView.setImageResource(R.drawable.volume_up);
            } else {
                newImageView.setContentDescription("Down");
                newImageView.setImageResource(R.drawable.volume_down);
            }

            mButtonSequenceLayout.addView(newImageView);
            mSequenceBarTitle.setText(getString(R.string.activation_sequence) + " (" + mSequence.size() + "/5)");
        }else{
            Toast.makeText(this, "Max length reached", Toast.LENGTH_SHORT).show();
        }
    }
}