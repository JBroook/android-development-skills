package com.example.silentsos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {
    private EditText mEmailInput;
    private EditText mPasswordInput;
    private EditText mConfirmPasswordInput;
    private Button mRegisterButton;
    private TextView mGoToLogin;
    private EditText mNameInput;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        mEmailInput = (EditText) findViewById(R.id.emailInput);
        mNameInput = (EditText) findViewById(R.id.nameInput);
        mPasswordInput = (EditText) findViewById(R.id.passwordInput);
        mConfirmPasswordInput = (EditText) findViewById(R.id.confirmPasswordInput);
        mRegisterButton = (Button) findViewById(R.id.registerButton);
        mGoToLogin = (TextView) findViewById(R.id.goToRegister);

        mRegisterButton.setOnClickListener(v -> registerUser());
        mGoToLogin.setOnClickListener(v ->
            finish()
        );
    }

    private void registerUser(){
        String email = mEmailInput.getText().toString().trim();
        String name = mNameInput.getText().toString().trim();
        String password = mPasswordInput.getText().toString().trim();
        String confirmPassword = mConfirmPasswordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || name.isEmpty()){
            Toast.makeText(this, "Please fill in all fields",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        if (!password.equals(confirmPassword)){
            Toast.makeText(this, "Passwords do not match",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(
                            this,
                            "Account created",
                            Toast.LENGTH_SHORT).show();

                    // make new User object
                    FirebaseUser firebaseUser = authResult.getUser();
                    String Uid = firebaseUser.getUid();
                    User newUser = new User(Uid);
                    newUser.setEmail(email);
                    newUser.setName(name);

                    FirebaseFirestore database = FirebaseFirestore.getInstance();
                    database.collection("users").document(Uid).set(newUser)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "User document created successfully");
                        })
                        .addOnFailureListener(aVoid -> {
                            Log.d("Firestore", "User document failed to create");
                        });

                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener( e->
                        Toast.makeText(
                                this,
                                "Registration failed",
                                Toast.LENGTH_SHORT
                        ).show()
                );


    }
}