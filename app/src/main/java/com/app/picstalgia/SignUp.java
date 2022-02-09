package com.app.picstalgia;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.content.ContentValues.TAG;

public class SignUp extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        showSignupPage();
    }

    public void showSignupPage() {
        setContentView(R.layout.sign_up_activity);

        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        confirmLayout = findViewById(R.id.confirm_layout);
        progressBar = findViewById(R.id.progress_loader);

        CheckBox pwVisibility = findViewById(R.id.pw_checkbox);
        MaterialButton doneButton = findViewById(R.id.done_btn);
        MaterialButton cancelButton = findViewById(R.id.cancel_btn);

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
                if(s.length() < 6){
                    passwordLayout.setError("At least 6 characters");
                }else{
                    passwordLayout.setErrorEnabled(false);
                }

            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        confirmLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmLayout.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        pwVisibility.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked){
                    //view password
                    passwordLayout.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    confirmLayout.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }else{
                    //hide password
                    passwordLayout.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
                    confirmLayout.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
                }

            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isOnline()) {
                   signUpUser();

                } else {
                    Toast.makeText(SignUp.this, "Please connect to the Internet.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    public void changeStatusBarColor() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(SignUp.this, R.color.pale_orange));
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

    public void signUpUser() {
        String email = emailLayout.getEditText().getText().toString().trim();
        String password = passwordLayout.getEditText().getText().toString().trim();
        String confirm = confirmLayout.getEditText().getText().toString().trim();

        if(email.equals("")) {
            emailLayout.setError("Enter email");
        } else if(password.equals("")) {
            passwordLayout.setError("Enter password");
        } else if(confirm.equals("")) {
            confirmLayout.setError("Enter password");
        } else {
            if(password.equals(confirm)){
                progressBar.setVisibility(View.VISIBLE);
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignUp.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.INVISIBLE);
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d(TAG, "signUpWithEmail:success");
                                    sendVerificationEmail();
                                    updateUI();
                                } else {
                                    System.out.println("===========sign up error: "+task.getException().getMessage());
                                    // If sign in fails, display a message to the user.
                                    try {
                                        throw task.getException();
                                    } catch (FirebaseAuthWeakPasswordException e) {
                                        passwordLayout.setError("Weak password");
                                    } catch (FirebaseAuthInvalidCredentialsException e) {
                                        emailLayout.setError("Invalid email");
                                    } catch (FirebaseAuthUserCollisionException e) {
                                        Toast.makeText(SignUp.this, "User already has an account. Please log in.",
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    } catch (Exception e) {
                                        Log.e(TAG, e.getMessage());
                                        finish();
                                        Toast.makeText(SignUp.this, "Sign in failed.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
            }else {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "Passwords don't match.", Toast.LENGTH_SHORT).show();
                passwordLayout.getEditText().setText("");
                confirmLayout.getEditText().setText("");
            }


        }
    }
    public void sendVerificationEmail() {
        mAuth.getCurrentUser().sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email sent.");
                            Toast.makeText(SignUp.this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SignUp.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void updateUI() {
        Intent mainActivity = new Intent(SignUp.this, MainActivity.class);
        mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainActivity);
    }
}
