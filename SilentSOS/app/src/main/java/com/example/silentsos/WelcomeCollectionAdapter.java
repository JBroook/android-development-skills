package com.example.silentsos;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class WelcomeCollectionAdapter extends FragmentStateAdapter {
    private String[] mPageTitles = new String[]{
            "Welcome to Super Silent!",
            "Silent safety is our priority",
            "How does it work?",
            "What now?",
    };

    private String[] mPageDescriptions = new String[]{
            "Swipe for the next page",
            "If you've ever been in an emergency where you needed to signal for help silently, Super Silent is perfect for you!",
            "By clicking a specific sequence on your volume keys, your SOS will be silently sent out to all your emergency contacts!",
            "Get started by signing in and setting up your silent SOS!"
    };

    private int[] mPageImages = new int[]{
            R.drawable.logo,
            R.drawable.safety,
            R.drawable.connected,
            R.drawable.login
    };

    public WelcomeCollectionAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance in createFragment(int).
        Fragment fragment = new WelcomePageFragment(
                mPageTitles[position],
                mPageDescriptions[position],
                mPageImages[position]
        );
        Bundle args = new Bundle();
        // The object is just an integer.
        args.putInt(WelcomePageFragment.ARG_OBJECT, position + 1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}