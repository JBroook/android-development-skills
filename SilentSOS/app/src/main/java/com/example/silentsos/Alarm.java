package com.example.silentsos;

import java.util.List;

public class Alarm {
    private boolean mIncludeLocation;
    private String mMessage;
    private List<Boolean> mActivationSequence;

    // for firebase
    public Alarm() {}

    public Alarm(boolean includeLocation, String message, List<Boolean> activationSequence) {
        this.mIncludeLocation = includeLocation;
        this.mMessage = message;
        this.mActivationSequence = activationSequence;
    }

    public boolean isIncludeLocation() {
        return mIncludeLocation;
    }

    public void setIncludeLocation(boolean includeLocation) {
        mIncludeLocation = includeLocation;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public List<Boolean> getActivationSequence() {
        return mActivationSequence;
    }

    public void setActivationSequence(List<Boolean> activationSequence) {
        mActivationSequence = activationSequence;
    }
}
