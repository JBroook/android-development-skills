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
    private FirebaseAuth mAuth;
    private LinearLayout mButtonSequenceLayout;

    private List<Boolean> mSequence = new ArrayList<Boolean>();

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

        mBackButton = (Button) findViewById(R.id.backButton);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSequenceAndLeave();
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
        Intent data = new Intent();
        // converting List<Boolean> to primitive boolean[]
        boolean[] booleanSequence = new boolean[mSequence.size()];
        for (int i = 0; i < mSequence.size(); i++) {
            booleanSequence[i] = mSequence.get(i);
        }
        data.putExtra(KEY_NEW_SEQUENCE, booleanSequence);
        setResult(RESULT_OK, data);
        finish();
    }

    private void addNewButton(boolean isUp){
        mSequence.add(isUp);
        ImageView newImageView = new ImageView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, 100);
        layoutParams.setMargins(0, 0, 10, 0);

        newImageView.setLayoutParams(layoutParams);

        if(isUp){
            newImageView.setContentDescription("Up");
            newImageView.setImageResource(R.drawable.volume_up);
        }else{
            newImageView.setContentDescription("Down");
            newImageView.setImageResource(R.drawable.volume_down);
        }

        mButtonSequenceLayout.addView(newImageView);
    }
}