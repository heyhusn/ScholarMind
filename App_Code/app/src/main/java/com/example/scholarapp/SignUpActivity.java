package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private EditText etFullName, etSignUpEmail, etSignUpPassword;
    private CheckBox cbTerms;
    private MaterialButton btnSignUp, btnGoogleSignUp;
    private TextView tvSignInPrompt;
    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account);
                            }
                        } catch (ApiException e) {
                            Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        etFullName = findViewById(R.id.etFullName);
        etSignUpEmail = findViewById(R.id.etSignUpEmail);
        etSignUpPassword = findViewById(R.id.etSignUpPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        tvSignInPrompt = findViewById(R.id.tvSignInPrompt);

        btnSignUp.setOnClickListener(v -> handleSignUp());
        btnGoogleSignUp.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
        tvSignInPrompt.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        });
    }

    private void handleSignUp() {
        String fullName = etFullName.getText().toString().trim();
        String email = etSignUpEmail.getText().toString().trim();
        String password = etSignUpPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) { etFullName.setError("Name required"); return; }
        if (TextUtils.isEmpty(email)) { etSignUpEmail.setError("Email required"); return; }
        if (password.length() < 6) { etSignUpPassword.setError("Minimum 6 characters"); return; }
        if (!cbTerms.isChecked()) { Toast.makeText(this, "Accept Terms", Toast.LENGTH_SHORT).show(); return; }

        btnSignUp.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> userProfile = new HashMap<>();
                        userProfile.put("fullName", fullName);
                        userProfile.put("email", email);
                        userProfile.put("uploadCount", 0L);

                        fStore.collection("users").document(userId).set(userProfile)
                                .addOnSuccessListener(aVoid -> {
                                    startActivity(new Intent(SignUpActivity.this, UploadActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnSignUp.setEnabled(true);
                                    Toast.makeText(SignUpActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        btnSignUp.setEnabled(true);
                        Toast.makeText(SignUpActivity.this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> userProfile = new HashMap<>();
                        userProfile.put("fullName", acct.getDisplayName());
                        userProfile.put("email", acct.getEmail());
                        userProfile.put("uploadCount", 0L);

                        fStore.collection("users").document(userId).set(userProfile)
                                .addOnSuccessListener(aVoid -> {
                                    startActivity(new Intent(SignUpActivity.this, UploadActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignUpActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    // Proceed to UploadActivity regardless since Auth was successful
                                    startActivity(new Intent(SignUpActivity.this, UploadActivity.class));
                                    finish();
                                });
                    } else {
                        Toast.makeText(SignUpActivity.this, "Firebase Auth failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
