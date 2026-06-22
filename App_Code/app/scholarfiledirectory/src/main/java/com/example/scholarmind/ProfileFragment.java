package com.example.scholarmind;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scholarmind.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private TextView    tvFullName;
    private TextView    tvEmail;
    private TextView    tvUserId;
    private TextView    tvPapersCount;
    private TextView    tvLoading;
    private TextView    btnSignOut;

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
        tvFullName    = view.findViewById(R.id.tvFullName);
        tvEmail       = view.findViewById(R.id.tvEmail);
        tvUserId      = view.findViewById(R.id.tvUserId);
        tvPapersCount = view.findViewById(R.id.tvPapersCount);
        tvLoading     = view.findViewById(R.id.tvLoading);
        btnSignOut    = view.findViewById(R.id.btnSignOut);

        btnSignOut.setOnClickListener(v -> signOut());
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

        // Show short UID
        tvUserId.setText("ID: " + userId.substring(0, Math.min(8, userId.length())) + "...");

        // Show email from FirebaseAuth directly
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            tvEmail.setText(email != null ? email : "No email");
        }

        FirebaseManager.getInstance().getUserProfile(
                userId,
                documentSnapshot -> {
                    if (!isAdded()) return;
                    tvLoading.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        tvFullName.setText(fullName != null && !fullName.trim().isEmpty()
                                ? fullName : "Scholar");
                    } else {
                        tvFullName.setText("Scholar");
                    }
                },
                e -> {
                    if (!isAdded()) return;
                    tvLoading.setVisibility(View.GONE);
                    tvFullName.setText("Scholar");
                    Toast.makeText(requireContext(), "Could not load profile", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void loadPaperCount() {
        FirebaseManager.getInstance().getAllPapers(
                papers -> {
                    if (!isAdded()) return;
                    tvPapersCount.setText(papers.size() + " papers uploaded");
                },
                e -> {
                    if (!isAdded()) return;
                    tvPapersCount.setText("— papers uploaded");
                }
        );
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show();
        // Navigate back to login / splash
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
