package com.example.silentsos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserActivity extends AppCompatActivity {
    private EditText mNameEditText;
    private EditText mEmailEditText;
    private Button mBackButton;
    private Button mLogoutButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        mNameEditText = (EditText) findViewById(R.id.nameTextView);
        mEmailEditText = (EditText) findViewById(R.id.emailTextView);
        mBackButton = (Button) findViewById(R.id.backButton);
        mLogoutButton = (Button) findViewById(R.id.logoutButton);

        // set user name and email
        String Uid = mAuth.getUid();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database
                .collection("users")
                .document(Uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                  if(documentSnapshot.exists()){
                      User user = documentSnapshot.toObject(User.class);
                      mNameEditText.setText(user.getName());
                      mEmailEditText.setText(user.getEmail());
                  }else{
                      Toast.makeText(this, "Failed to retrieve user details", Toast.LENGTH_SHORT).show();
                  }
                });



        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(UserActivity.this, LoginActivity.class);
                // clears backstack to prevent users from "back"ing into main page
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}