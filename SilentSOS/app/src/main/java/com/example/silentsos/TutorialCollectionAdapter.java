package com.example.silentsos;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TutorialCollectionAdapter extends FragmentStateAdapter {
    private String[] mPageTitles = new String[]{
            "Confused about how the app works?",
            "Always-on Notification",
            "How to activate the SOS",
            "Turn on your screen!",
            "Got it?"
    };

    private String[] mPageDescriptions = new String[]{
            "No worries! We're here to help!",
            "As long as you see Super Silent's notification, the app will be running in the background!",
            "If you're feeling unsafe, press any volume key one time to open your volume bar, then enter your activation sequence!",
            "Super Silent uses non-invasive permissions to function, so your phone screen must be on before it will work!",
            "Nice! Feel free to head on back to the home page!"
    };

    private int[] mPageImages = new int[]{
            R.drawable.logo,
            R.drawable.safety,
            R.drawable.connected,
            R.drawable.walking,
            R.drawable.login,
    };

    public TutorialCollectionAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance in createFragment(int).
        Fragment fragment = new TutorialPageFragment(
                mPageTitles[position],
                mPageDescriptions[position],
                mPageImages[position]
        );
        Bundle args = new Bundle();
        // The object is just an integer.
        args.putInt(TutorialPageFragment.ARG_OBJECT, position + 1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return mPageTitles.length;
    }
}