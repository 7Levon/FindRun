package com.example.findrun;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class login extends AppCompatActivity {
    TextView logsignup;
    Button button;
    EditText email, password;
    FirebaseAuth auth;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCancelable(false);
        getSupportActionBar().hide();
        auth = FirebaseAuth.getInstance();
        button = findViewById(R.id.logbutton);
        email = findViewById(R.id.editTexLogEmail);
        password = findViewById(R.id.editTextLogPassword);
        logsignup = findViewById(R.id.logsignup);

        logsignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(login.this,registration.class);
                startActivity(intent);
                finish();
            }
        });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String Email = email.getText().toString();
                String pass = password.getText().toString();

                if (TextUtils.isEmpty(Email)) {
                    Toast.makeText(login.this, "Enter The Email", Toast.LENGTH_SHORT).show();
                } else if (TextUtils.isEmpty(pass)) {
                    Toast.makeText(login.this, "Enter The Password", Toast.LENGTH_SHORT).show();
                } else if (!Email.matches(emailPattern)) {
                    email.setError("Give Proper Email Address");
                } else if (password.length() < 6) {
                    password.setError("More Than Six Characters");
                    Toast.makeText(login.this, "Password Needs To Be Longer Than Six Characters", Toast.LENGTH_SHORT).show();
                } else {
                    progressDialog.show();

                    auth.signInWithEmailAndPassword(Email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressDialog.dismiss();

                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    if (user.isEmailVerified()) {


                                        try {
                                            Intent intent = new Intent(login.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } catch (Exception e) {
                                            Toast.makeText(login.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(login.this, "Please verify your email address first.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                Toast.makeText(login.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

    }

}