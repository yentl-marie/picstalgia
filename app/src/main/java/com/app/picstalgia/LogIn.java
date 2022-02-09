package com.app.picstalgia;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import static android.content.ContentValues.TAG;

public class LogIn extends AppCompatActivity {
    private boolean isPasswordVisible;
    private FirebaseAuth mAuth;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        showLogInPage();
    }

    public void showLogInPage() {
        setContentView(R.layout.activity_main);
        changeStatusBarColor();

        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        progressBar = findViewById(R.id.progress_loader);
        MaterialButton pwVisibility = findViewById(R.id.pw_visibility);
        MaterialButton forgotButton = findViewById(R.id.pw_forgot);
        MaterialButton loginButton = findViewById(R.id.log_in_btn);
        MaterialButton signupButton = findViewById(R.id.sign_up_btn);

        emailLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailLayout.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        passwordLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordLayout.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        isPasswordVisible = false;
        pwVisibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isPasswordVisible){
                    //view password
                    passwordLayout.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    pwVisibility.setIcon(ContextCompat.getDrawable(LogIn.this, R.drawable.ic_baseline_visibility_off_24));
                    isPasswordVisible = true;
                }else{
                    //hide password
                    passwordLayout.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
                    pwVisibility.setIcon(ContextCompat.getDrawable(LogIn.this, R.drawable.ic_baseline_visibility_24));
                    isPasswordVisible = false;
                }

            }
        });

        forgotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isOnline()) {
                    forgotPassword();
                } else {
                    Toast.makeText(LogIn.this, "Please connect to the Internet.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isOnline()) {
                    loginUser();
                } else {
                    Toast.makeText(LogIn.this, "Please connect to the Internet.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signUp = new Intent(LogIn.this, SignUp.class);
                startActivity(signUp);
            }
        });
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    public void forgotPassword() {
        String email = emailLayout.getEditText().getText().toString();
        if(email.equals("")){
            emailLayout.setError("Enter email");
        }else{
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Email sent.");
                                Toast.makeText(LogIn.this, "Email sent.", Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(LogIn.this, "Email not sent.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    public void changeStatusBarColor() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(LogIn.this, R.color.pale_orange));
    }

    public void loginUser() {
        String email = emailLayout.getEditText().getText().toString().trim();
        String password = passwordLayout.getEditText().getText().toString().trim();

        if(email.equals("")){
            emailLayout.setError("Enter email");
        } else if (password.equals("")){
            passwordLayout.setError("Enter password");
        } else {
            progressBar.setVisibility(View.VISIBLE);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LogIn.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressBar.setVisibility(View.INVISIBLE);
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                System.out.println("=========logged in");
                                updateUI();
                            }
                            if (!task.isSuccessful()) {
                                try
                                {
                                    throw task.getException();
                                }
                                // if user enters wrong email.
                                catch (FirebaseAuthInvalidUserException invalidEmail)
                                {
                                    Toast.makeText(LogIn.this, "Email does not exist. Please sign up!", Toast.LENGTH_SHORT).show();
                                    // TODO: take your actions!
                                }
                                // if user enters wrong password.
                                catch (FirebaseAuthInvalidCredentialsException wrongPassword)
                                {
                                    Log.d(TAG, "onComplete: wrong_password");
                                    System.out.println("=========not logged in");
                                    Toast.makeText(LogIn.this, "Failed to log in.", Toast.LENGTH_SHORT).show();
                                    passwordLayout.getEditText().setText(null);
                                    // TODO: Take your action
                                }
                                catch (Exception e)
                                {
                                    Log.d(TAG, "onComplete: " + e.getMessage());
                                }
                            }
                        }
                    });
        }
    }

    public void updateUI() {
        Intent mainActivity = new Intent(LogIn.this, MainActivity.class);
        mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainActivity);
    }

}
