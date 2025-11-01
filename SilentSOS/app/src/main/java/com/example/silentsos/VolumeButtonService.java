package com.example.silentsos;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.os.Vibrator;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;

public class VolumeButtonService extends AccessibilityService {

    private String userSequence = "UP,UP,DOWN"; // Example sequence
    private StringBuilder inputSequence = new StringBuilder();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            Log.i("VolumeButtonService", String.valueOf(inputSequence));
            String key = "";

            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                key = "UP";
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                key = "DOWN";
            }

            if (!key.isEmpty()) {
                inputSequence.append(key).append(",");
                checkSequence();
                return true;
            }
        }
        return false;
    }

    private void checkSequence() {
        String current = inputSequence.toString();
        if (current.contains(userSequence)) {
            Log.i("VolumeButtonService", "Sequence matched: " + current);
            vibrateDevice();
            inputSequence.setLength(0); // Reset
        }
    }

    private void vibrateDevice() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(500); // vibrate for 0.5 seconds
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {
        // required override
    }
}
