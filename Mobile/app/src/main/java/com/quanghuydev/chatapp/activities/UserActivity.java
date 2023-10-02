package com.quanghuydev.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;


import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.Constants;
import com.quanghuydev.chatapp.adapters.UserAdapter;
import com.quanghuydev.chatapp.databinding.ActivityUserBinding;
import com.quanghuydev.chatapp.listeners.UserListener;
import com.quanghuydev.chatapp.models.User;
import com.quanghuydev.chatapp.ultilities.Contants;
import com.quanghuydev.chatapp.ultilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class UserActivity extends BaseActivity implements UserListener {
    private ActivityUserBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getListUser();

    }

    private void setListeners() {
        binding.icBack.setOnClickListener(v -> onBackPressed());
    }

    private void getListUser() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Contants.KEY_COLLECTION_USERS).get().addOnCompleteListener(task -> {
            loading(false);
            String currentUserId = preferenceManager.getString(Contants.KEY_USER_ID);

            if (task.isSuccessful() && task.getResult() != null) {
                List<User> users = new ArrayList<>();
                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                    if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                        continue;
                    }
                    User user = new User();
                    user.name = queryDocumentSnapshot.getString(Contants.KEY_NAME);
                    user.email = queryDocumentSnapshot.getString(Contants.KEY_EMAIL);
                    user.image = queryDocumentSnapshot.getString(Contants.KEY_IMAGE);
                    user.token = queryDocumentSnapshot.getString(Contants.KEY_FCM_TOKEN);
                    user.id = queryDocumentSnapshot.getId();
                    users.add(user);
                }
                if (users.size() > 0) {
                    UserAdapter userAdapter = new UserAdapter(users,this);
                    binding.userRecycler.setAdapter(userAdapter);
                    binding.userRecycler.setVisibility(View.VISIBLE);
                } else {
                    showError();
                }
            } else {
                showError();
            }
        });
    }

    private void showError() {
        binding.textError.setText(String.format("%s", "No user available"));
        binding.textError.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);

        }
    }

    @Override
    public void userClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Contants.KEY_USER,user);
        startActivity(intent);
        finish();
    }
}