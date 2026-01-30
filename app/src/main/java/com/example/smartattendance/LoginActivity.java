package com.example.smartattendance;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase
        auth = FirebaseAuth.getInstance();

        // Views
        ImageView imgHeader = findViewById(R.id.imgHeader);
        View cardLogin = findViewById(R.id.cardLogin);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoRegister = findViewById(R.id.tvGoRegister);
        TextView tvForgot = findViewById(R.id.tvForgotPassword);

        // --- Animation (make it obvious) ---
        // Start with invisible then animate in
        imgHeader.setAlpha(0f);
        cardLogin.setAlpha(0f);

        Animation headerAnim = AnimationUtils.loadAnimation(this, R.anim.fade_slide_in);
        imgHeader.startAnimation(headerAnim);
        imgHeader.setAlpha(1f);

        Animation cardAnim = AnimationUtils.loadAnimation(this, R.anim.fade_slide_in);
        cardAnim.setStartOffset(250); // delay so card comes after header
        cardLogin.startAnimation(cardAnim);
        cardLogin.setAlpha(1f);

        // Click events
        btnLogin.setOnClickListener(v -> login());
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
        tvForgot.setOnClickListener(v -> showResetDialog());
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(pass)) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    startActivity(new Intent(LoginActivity.this, RoleRouterActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(LoginActivity.this,
                                "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void showResetDialog() {
        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("Enter your email");

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("We will send a reset link to your email.")
                .setView(inputEmail)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = inputEmail.getText().toString().trim();

                    if (email.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Email required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseAuth.getInstance()
                            .sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(LoginActivity.this, "Reset email sent", Toast.LENGTH_LONG).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
