package com.example.silentsos;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;


public class CreateAlarmActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_create_alarm);
    }
}