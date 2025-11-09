package com.example.silentsos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Objects;

// Instances of this class are fragments representing a single
// object in the collection.
public class TutorialPageFragment extends Fragment {
    public static final String ARG_OBJECT = "object";
    private String mTitle;
    private String mDescription;
    private int mImage;

    public TutorialPageFragment(String title, String description, int image) {
        super();
        this.mTitle = title;
        this.mDescription = description;
        this.mImage = image;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //Integer.toString(args.getInt(ARG_OBJECT))
        Bundle args = getArguments();
        ((TextView) view.findViewById(R.id.title))
                .setText(mTitle);
        ((TextView) view.findViewById(R.id.description))
                .setText(mDescription);
        ((ImageView) view.findViewById(R.id.image))
                .setImageResource(mImage);

        // last page
        if(Objects.equals(mTitle, "Got it?")){
            Button signInButton = view.findViewById(R.id.signInButton);
            signInButton.setVisibility(View.VISIBLE);
            signInButton.setText("Head Back");
            signInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(getActivity(), MainActivity.class));
                    getActivity().finish();
                }
            });
        }

    }
}