package com.example.silentsos;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public class TutorialActivity extends AppCompatActivity {
    private TutorialPageCollection mFragmentCollection;
    private FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tutorial);

        mFragmentManager = getSupportFragmentManager();

        mFragmentCollection = (TutorialPageCollection) mFragmentManager.findFragmentById(R.id.fragmentCollection);
//        mFragmentCollection.
    }
}