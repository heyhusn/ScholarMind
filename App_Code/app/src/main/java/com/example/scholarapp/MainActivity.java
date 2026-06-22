package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ScholarMindAuth";
    private EditText etEmail, etPassword;
    private MaterialButton btnSignIn, btnGoogleSignIn;
    private TextView tvForgot, tvSignUp;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            goToHomeActivity();
            return;
        }

        // Configure Google Sign In using the web client ID from google-services.json
        String webClientId = getString(R.string.default_web_client_id);
        Log.d(TAG, "Using Web Client ID: " + webClientId);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Sign-in result code: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Log.w(TAG, "Google Sign-In was cancelled by user or failed silently");
                        // Try to get the sign-in result anyway to capture the error code
                        if (result.getData() != null) {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleGoogleSignInResult(task);
                        } else {
                            Toast.makeText(this, "Sign-in cancelled. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "Unexpected result code: " + result.getResultCode());
                        Toast.makeText(this, "Sign-in failed (code " + result.getResultCode() + "). Please try again.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvForgot = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUpPrompt);

        btnSignIn.setOnClickListener(v -> handleLogin());
        btnGoogleSignIn.setOnClickListener(v -> {
            Log.d(TAG, "Google Sign-In button clicked, launching sign-in intent");
            // Sign out first to force account picker to show every time
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });
        tvForgot.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ForgotPasswordActivity.class)));
        tvSignUp.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SignUpActivity.class)));
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                Log.d(TAG, "Google Sign-In success, email: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } else {
                Log.e(TAG, "GoogleSignInAccount is null");
                Toast.makeText(this, "Sign-in failed: account is null", Toast.LENGTH_LONG).show();
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In ApiException: statusCode=" + e.getStatusCode() + ", message=" + e.getMessage());
            String errorMsg;
            switch (e.getStatusCode()) {
                case 10:
                    errorMsg = "Developer Error (10): SHA-1 or package name mismatch in Firebase.";
                    break;
                case 12500:
                    errorMsg = "Sign-in failed (12500): Update Google Play Services.";
                    break;
                case 12501:
                    errorMsg = "Sign-in cancelled (12501).";
                    break;
                case 7:
                    errorMsg = "Network error (7): Check your internet connection.";
                    break;
                default:
                    errorMsg = "Google Sign-In failed: error code " + e.getStatusCode();
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) { etEmail.setError("Email required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password required"); return; }

        btnSignIn.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnSignIn.setEnabled(true);
                    if (task.isSuccessful()) {
                        goToHomeActivity();
                    } else {
                        Toast.makeText(MainActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "Authenticating with Firebase using Google ID Token");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase signInWithCredential: SUCCESS, user=" + mAuth.getCurrentUser().getEmail());
                        goToHomeActivity();
                    } else {
                        Log.e(TAG, "Firebase signInWithCredential: FAILED", task.getException());
                        Toast.makeText(MainActivity.this, "Firebase Auth failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToHomeActivity() {
        startActivity(new Intent(MainActivity.this, DashboardActivity.class));
        finish();
    }
}