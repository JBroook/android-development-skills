package com.example.silentsos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.ContactsContract;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class EmergencyContact {
    private String mName;
    private String mNumber;

    // for firebase
    public EmergencyContact(){}

    public EmergencyContact(String name, String number){
        this.mName = name;
        this.mNumber = number;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        mNumber = number;
    }
}
