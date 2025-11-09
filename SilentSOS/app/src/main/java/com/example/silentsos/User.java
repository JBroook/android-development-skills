package com.example.silentsos;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String mUid;
    private boolean mAlarmSet = false;
    private Alarm mAlarm;
    private List<EmergencyContact> mEmergencyContacts;
    private String mEmail;
    private String mName;

    // for firebase
    public User(){}

    public User(String Uid){
        this.mUid = Uid;
        this.mAlarm = null;
        this.mEmergencyContacts = new ArrayList<>();
    }

    public Alarm getAlarm() {
        return mAlarm;
    }

    public void setAlarm(Alarm alarm) {
        mAlarm = alarm;
    }

    public List<EmergencyContact> getEmergencyContacts() {
        return mEmergencyContacts;
    }

    public void setEmergencyContacts(List<EmergencyContact> emergencyContacts) {
        mEmergencyContacts = emergencyContacts;
    }

    public boolean isAlarmSet() {
        return mAlarmSet;
    }

    public void setAlarmSet(boolean alarmSet) {
        mAlarmSet = alarmSet;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }
}
