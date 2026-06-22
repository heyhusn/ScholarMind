package com.example.scholarmind;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText etResetEmail;
    private MaterialButton btnSendReset;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnSendReset.setOnClickListener(v -> {
            String email = etResetEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) { etResetEmail.setError("Enter email"); return; }

            btnSendReset.setEnabled(false);
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                btnSendReset.setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Reset email sent!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(ForgotPasswordActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, MainActivity.class));
            finish();
        });
    }
}