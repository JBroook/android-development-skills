package com.example.silentsos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class LoginActivity extends AppCompatActivity {
    private EditText mEmailInput;
    private EditText mPasswordInput;
    private Button mLoginButton;
    private TextView mGoToRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();

        mEmailInput = (EditText) findViewById(R.id.emailInput);
        mPasswordInput = (EditText) findViewById(R.id.passwordInput);
        mLoginButton = (Button) findViewById(R.id.loginButton);
        mGoToRegister = (TextView) findViewById(R.id.goToRegister);

        mLoginButton.setOnClickListener(v -> loginUser());
        mGoToRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void loginUser(){
        String email = mEmailInput.getText().toString().trim();
        String password = mPasswordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please fill in all fields",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(
                            this,
                            "Login succeeded",
                            Toast.LENGTH_SHORT).show();


                    String Uid = mAuth.getCurrentUser().getUid();
                    FirebaseFirestore database = FirebaseFirestore.getInstance();
                    database
                            .collection("users")
                            .document(Uid)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                // this account doesn't have an associated User object
                                if(!documentSnapshot.exists()){

                                    User newUser = new User(Uid);

                                    database
                                            .collection("users")
                                            .document(Uid)
                                            .set(newUser)
                                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "Created Firestore user profile"))
                                            .addOnFailureListener(e -> Log.w("Firestore", "Error creating Firestore user", e));
                                }
                            });

                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener( e->
                        Toast.makeText(
                                this,
                                "Login failed",
                                Toast.LENGTH_SHORT
                        ).show()
                );


    }
}