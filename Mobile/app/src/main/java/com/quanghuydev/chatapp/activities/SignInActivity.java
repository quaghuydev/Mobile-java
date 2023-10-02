package com.quanghuydev.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quanghuydev.chatapp.databinding.ActivitySignInBinding;
import com.quanghuydev.chatapp.ultilities.Contants;
import com.quanghuydev.chatapp.ultilities.PreferenceManager;

import java.util.HashMap;

public class SignInActivity extends AppCompatActivity {
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Contants.KEY_IS_SIGN_IN)){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        setListeners();
    }

    public void setListeners() {
        binding.textCreateAccount.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.btnSinIn.setOnClickListener(v -> {
            if (isValidSignInDetail()){
                signIn();
            }
        });
    }

    private void signIn() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        database.collection(Contants.KEY_COLLECTION_USERS).whereEqualTo(Contants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .whereEqualTo(Contants.KEY_PASSWORD, binding.inputPassword.getText().toString())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocumentChanges().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Contants.KEY_IS_SIGN_IN, true);
                        preferenceManager.putString(Contants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Contants.KEY_NAME, documentSnapshot.getString(Contants.KEY_NAME));
                        preferenceManager.putString(Contants.KEY_IMAGE, documentSnapshot.getString(Contants.KEY_IMAGE));
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        loading(false);
                        showToast("Login failed");
                    }

                });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.btnSinIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.btnSinIn.setVisibility(View.VISIBLE);

        }
    }

    private Boolean isValidSignInDetail() {
        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else {
            return true;
        }
    }

//    private void addDatatoFirebase() {
//        FirebaseFirestore database = FirebaseFirestore.getInstance();
//        HashMap<String, Object> data = new HashMap<>();
//        data.put("first_name", "huy");
//        data.put("last_name", "bui");
//        database.collection("users").add(data).addOnSuccessListener(documentReference -> {
//            Toast.makeText(getApplicationContext(), "data inserted", Toast.LENGTH_SHORT).show();
//        }).addOnFailureListener(exception -> {
//            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
//        });
//    }
}
