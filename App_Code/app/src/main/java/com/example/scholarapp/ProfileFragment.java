package com.example.scholarapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private TextView tvFullName;
    private TextView tvEmail;
    private TextView tvUserId;
    private TextView tvPapersCount;
    private TextView tvLoading;
    private TextView btnSignOut;
    private SwitchMaterial switchDarkMode;
    private LinearLayout statUploads;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        bindViews(view);
        loadProfile();
        loadPaperCount();
        return view;
    }

    private void bindViews(View view) {
        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvUserId = view.findViewById(R.id.tvUserId);
        tvPapersCount = view.findViewById(R.id.tvPapersCount);
        tvLoading = view.findViewById(R.id.tvLoading);
        btnSignOut = view.findViewById(R.id.btnSignOut);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        statUploads = view.findViewById(R.id.statUploads);

        btnSignOut.setOnClickListener(v -> signOut());
        statUploads.setOnClickListener(v -> openUploads());

        SharedPreferences prefs = requireContext().getSharedPreferences("ScholarMindPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }

    private void loadProfile() {
        String userId = FirebaseManager.getInstance().getCurrentUserId();
        if (userId == null) {
            tvFullName.setText("Guest User");
            tvEmail.setText("Not signed in");
            tvUserId.setText("");
            tvLoading.setVisibility(View.GONE);
            return;
        }

        tvUserId.setText("ID: " + userId.substring(0, Math.min(8, userId.length())) + "...");

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            tvEmail.setText(email != null ? email : "No email");
        }

        FirebaseManager.getInstance().getUserProfile(
                userId,
                documentSnapshot -> {
                    if (!isAdded()) {
                        return;
                    }
                    tvLoading.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        tvFullName.setText(fullName != null && !fullName.trim().isEmpty() ? fullName : "Scholar");

                        Long uploadCount = documentSnapshot.getLong("uploadCount");
                        if (uploadCount != null) {
                            tvPapersCount.setText(uploadCount + " uploads");
                        }
                    } else {
                        tvFullName.setText("Scholar");
                    }
                },
                e -> {
                    if (!isAdded()) {
                        return;
                    }
                    tvLoading.setVisibility(View.GONE);
                    tvFullName.setText("Scholar");
                    Toast.makeText(requireContext(), "Could not load profile", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void loadPaperCount() {
        FirebaseManager.getInstance().getAllPapers(
                papers -> {
                    if (!isAdded()) {
                        return;
                    }
                    tvPapersCount.setText(papers.size() + " uploads");
                },
                e -> {
                    if (!isAdded()) {
                        return;
                    }
                    tvPapersCount.setText("0 uploads");
                }
        );
    }

    private void openUploads() {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).switchToTab(HomeActivity.TAB_HISTORY);
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
