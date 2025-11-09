package com.example.silentsos;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;



public class EditContactsActivity extends AppCompatActivity {
    private static final int REQUEST_CONTACT_PERMISSION = 1;
    private static final int PICK_CONTACT_REQUEST = 2;
    private ActivityResultLauncher<Intent> mContactsPickerLauncher;

    private Button mAddContactButton;
    private LinearLayout mContactsLayout;
    private List<EmergencyContact> mChosenContacts = new ArrayList<EmergencyContact>();
    private Button mFinishButton;
    private FirebaseAuth mAuth;
    private String mCurrentUserUid;
    private Button mBackButton;

    private void pickContact(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_CONTACT_PERMISSION);
        }else{
            openContactPicker();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CONTACT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactPicker();
            } else {
                Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openContactPicker(){
        Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        mContactsPickerLauncher.launch(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_contacts);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUserUid = mAuth.getCurrentUser().getUid();

        mContactsPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();

                            Uri contactUri = data.getData();
                            String[] projection = {
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                            };

                            try(Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)){
                                if (cursor != null && cursor.moveToFirst())
                                {
                                    int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                                    String name = cursor.getString(nameIndex);
                                    String number = cursor.getString(numberIndex);

//                                    Toast.makeText(ContactsActivity.this, name+" "+number, Toast.LENGTH_SHORT).show();
                                    addNewContact(name, number);
                                }
                            }
                        }
                    }
                });

        mAddContactButton = (Button) findViewById(R.id.addContactButton);
        mAddContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickContact();
            }
        });

        mContactsLayout = (LinearLayout) findViewById(R.id.contactLayout);
        // fetch and display already existing emergency contacts
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database
                .collection("users")
                .document(mCurrentUserUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        List<EmergencyContact> contactsList = documentSnapshot.toObject(User.class).getEmergencyContacts();

                        for (EmergencyContact contact : contactsList){
                            addNewContact(contact.getName(), contact.getNumber());
                        }
                    }
                });

        mBackButton = (Button) findViewById(R.id.backButton);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        mFinishButton = (Button) findViewById(R.id.finishButton);
        mFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save emergency contacts list to user in db
                FirebaseFirestore database = FirebaseFirestore.getInstance();
                database
                        .collection("users")
                        .document(mCurrentUserUid)
                        .update("emergencyContacts",mChosenContacts)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(EditContactsActivity.this, "Emergency contacts saved", Toast.LENGTH_SHORT).show();
                            SOSForegroundService.triggerReload(EditContactsActivity.this);
                            finish();
                        })
                        .addOnFailureListener(aVoid -> {
                            Toast.makeText(EditContactsActivity.this, "Failed to save contacts", Toast.LENGTH_SHORT).show();
                        });;
            }
        });
    }

    private void addNewContact(String name, String number){

        EmergencyContact newContact = new EmergencyContact(name, number);
        mChosenContacts.add(newContact);

        LayoutInflater inflater = LayoutInflater.from(this);

        final View newContactView = inflater.inflate(R.layout.contact_item, mContactsLayout, false);

        TextView nameTextView = newContactView.findViewById(R.id.nameTextView);
        TextView numberTextView = newContactView.findViewById(R.id.numberTextView);
        LinearLayout closeButton = newContactView.findViewById(R.id.closeButton);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContactsLayout.removeView(newContactView);
                mChosenContacts.remove(newContact);
            }
        });

        nameTextView.setText(name);
        numberTextView.setText(number);

        mContactsLayout.addView(newContactView, 0);
    }
}