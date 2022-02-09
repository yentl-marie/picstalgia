package com.app.picstalgia;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import static android.content.ContentValues.TAG;

public class MainMenu extends AppCompatActivity {
    private PopupWindow popupWindow;
    private boolean pwVisible;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_activity);

        mAuth = FirebaseAuth.getInstance();

        ImageButton picVidButton = findViewById(R.id.pic_vid_btn);
        ImageButton picAudButton = findViewById(R.id.pic_aud_btn);
        ImageButton picLinkButton = findViewById(R.id.pic_link_btn);
        ImageButton scanButton = findViewById(R.id.scan_btn);
        ImageButton galleryButton = findViewById(R.id.picstalgia_gallery_btn);

        changeStatusBarColor();
        MaterialButton profileButton = findViewById(R.id.profile_btn);

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProfileMenu();
            }
        });
        picVidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isUserVerified()){
                    Intent picVidIntent = new Intent(MainMenu.this, PictureVideo.class);
                    startActivity(picVidIntent);
                } else {
                    Toast.makeText(MainMenu.this, "Please verify email first.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        picAudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isUserVerified()){
                    Intent picAudIntent = new Intent(MainMenu.this, PictureAudio.class);
                    startActivity(picAudIntent);
                } else {
                    Toast.makeText(MainMenu.this, "Please verify email first.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        picLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isUserVerified()){
                    Intent picLinkIntent = new Intent(MainMenu.this, PictureLink.class);
                    startActivity(picLinkIntent);
                } else {
                    Toast.makeText(MainMenu.this, "Please verify email first.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent scannerIntent = new Intent(MainMenu.this, Scanner.class);
                startActivity(scannerIntent);

            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(MainMenu.this, Gallery.class);
                startActivity(galleryIntent);
            }
        });

    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            finishAffinity();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    public boolean isUserVerified() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser().isEmailVerified()) {
            return true;
        }
        return false;
    }
    public void showProfileMenu() {
        View popupView = showPopupWindow(R.layout.popup_profile_menu);

        Toolbar toolbar = findViewById(R.id.toolbar);
        popupWindow.showAtLocation(getWindow().getDecorView(), Gravity.TOP,
                toolbar.getWidth()-popupView.getWidth(), toolbar.getHeight()+getStatusBarHeight(this));

        TextView emailView = popupView.findViewById(R.id.email_view);
        MaterialButton verifyButton = popupView.findViewById(R.id.email_verify);
        MaterialButton changePwButton = popupView.findViewById(R.id.change_pw);
        MaterialButton deleteAcctButton = popupView.findViewById(R.id.delete_acct);
        MaterialButton signoutButton = popupView.findViewById(R.id.sign_out);

        mAuth.getCurrentUser().reload();
        if(mAuth.getCurrentUser().isEmailVerified()) {
            verifyButton.setClickable(false);
            verifyButton.setText("Account verified");
            verifyButton.setIcon(ContextCompat.getDrawable(MainMenu.this, R.drawable.ic_baseline_check_circle_24));
        } else {
            verifyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popupWindow.dismiss();
                    verifyEmail();
                }
            });
        }

        emailView.setText(mAuth.getCurrentUser().getEmail());

        changePwButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                changePassword();
            }
        });

        deleteAcctButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                deleteAccount();
            }
        });

        signoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                signOut();
            }
        });
    }
    public static int getStatusBarHeight(final Context context) {
        final Resources resources = context.getResources();
        final int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            return resources.getDimensionPixelSize(resourceId);
        else
            return (int) Math.ceil((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 24 : 25) * resources.getDisplayMetrics().density);
    }
    public void changeStatusBarColor() {

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(MainMenu.this, R.color.acapulco));
    }

    public void changePassword() {
        pwVisible = false;
        View popupView = showPopupWindow(R.layout.popup_new_password);

        popupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);

        TextInputLayout passwordLayout = popupView.findViewById(R.id.password_layout);
        TextInputLayout newPwLayout = popupView.findViewById(R.id.new_password_layout);
        TextInputLayout confirmPwLayout = popupView.findViewById(R.id.confirm_layout);
        MaterialButton pwVisibility = popupView.findViewById(R.id.pw_visibility);
        MaterialButton doneButton = popupView.findViewById(R.id.done_btn);
        MaterialButton cancelButton = popupView.findViewById(R.id.cancel_btn);
        progressBar = popupView.findViewById(R.id.progress_loader);

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
        newPwLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() < 6) {
                    newPwLayout.setError("At least 6 characters");
                }else{
                    newPwLayout.setErrorEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });
        confirmPwLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmPwLayout.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        pwVisibility.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(!pwVisible){
                    passwordLayout.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    newPwLayout.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    confirmPwLayout.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    pwVisibility.setIcon(ContextCompat.getDrawable(popupView.getContext(), R.drawable.ic_baseline_visibility_off_24));
                    pwVisible = true;
                } else {
                    passwordLayout.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
                    newPwLayout.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
                    confirmPwLayout.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
                    pwVisibility.setIcon(ContextCompat.getDrawable(popupView.getContext(), R.drawable.ic_baseline_visibility_24));
                    pwVisible = false;
                }
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = passwordLayout.getEditText().getText().toString();
                String newPassword = newPwLayout.getEditText().getText().toString();
                String confirmPassword = confirmPwLayout.getEditText().getText().toString();

                if(password.equals("")) {
                    passwordLayout.setError("Enter password");
                } else if (newPassword.equals("")) {
                    newPwLayout.setError("Enter new password");
                } else if (confirmPassword.equals("")) {
                    confirmPwLayout.setError("Enter confirm password");
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    if(newPassword.equals(confirmPassword)) {
                        AuthCredential credential = EmailAuthProvider.getCredential(mAuth.getCurrentUser().getEmail(), password);

                        // Prompt the user to re-provide their sign-in credentials
                        mAuth.getCurrentUser().reauthenticate(credential)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Log.d(TAG, "User re-authenticated.");
                                        if(task.isSuccessful()) {
                                            saveNewPassword(newPassword);
                                        } else {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            passwordLayout.setFocusable(true);
                                            passwordLayout.getEditText().setText(null);
                                            Toast.makeText(MainMenu.this, "Authentication failed.", Toast.LENGTH_SHORT).show();

                                        }
                                    }
                                });
                    } else {
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(MainMenu.this, "Passwords don't match.", Toast.LENGTH_SHORT).show();
                        newPwLayout.getEditText().setText("");
                        confirmPwLayout.getEditText().setText("");
                    }
                }

            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
    }

    public void saveNewPassword(String newPassword) {
        mAuth.getCurrentUser().updatePassword(newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User password updated.");
                            Toast.makeText(MainMenu.this, "User password updated.", Toast.LENGTH_SHORT).show();
                            popupWindow.dismiss();
                        } else {
                            Toast.makeText(MainMenu.this, "Failed changing password.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    public void verifyEmail() {
        mAuth.getCurrentUser().reload();
        if(mAuth.getCurrentUser().isEmailVerified()){
            Toast.makeText(this, "Email has been verified", Toast.LENGTH_SHORT).show();
        } else {
            mAuth.getCurrentUser().sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Email sent.");
                                Toast.makeText(MainMenu.this, "Verification email sent. Please wait a moment.", Toast.LENGTH_SHORT).show();
                            }
//                            else {
//                                Toast.makeText(MainMenu.this, "Failed sending verification email.", Toast.LENGTH_SHORT).show();
//                            }
                        }
                    });
        }

    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        finish();
        Intent mainActivity = new Intent(MainMenu.this, MainActivity.class);
        startActivity(mainActivity);
    }

    public void deleteAccount() {
        pwVisible = false;
        View popupView = showPopupWindow(R.layout.popup_delete_account);

        popupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);

        TextInputLayout passwordLayout = popupView.findViewById(R.id.password_layout);
        MaterialButton pwVisibility = popupView.findViewById(R.id.pw_visibility);
        MaterialButton doneButton = popupView.findViewById(R.id.done_btn);
        MaterialButton cancelButton = popupView.findViewById(R.id.cancel_btn);
        progressBar = popupView.findViewById(R.id.progress_loader);

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

        pwVisibility.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(!pwVisible){
                    passwordLayout.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    pwVisibility.setIcon(ContextCompat.getDrawable(popupView.getContext(), R.drawable.ic_baseline_visibility_off_24));
                    pwVisible = true;
                } else {
                    passwordLayout.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
                    pwVisibility.setIcon(ContextCompat.getDrawable(popupView.getContext(), R.drawable.ic_baseline_visibility_24));
                    pwVisible = false;
                }
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = passwordLayout.getEditText().getText().toString();

                if(password.equals("")) {
                    passwordLayout.setError("Enter password");
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    AuthCredential credential = EmailAuthProvider
                            .getCredential(mAuth.getCurrentUser().getEmail(), password);
                    //delete the data
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    FirebaseFirestore.getInstance().collection("media").document(user.getUid()).delete();
                    FirebaseFirestore.getInstance().collection("images").document(user.getUid()).delete();

                    StorageReference userRef = FirebaseStorage.getInstance().getReference().child("users/"+user.getUid());
                    userRef.delete();

                    // Prompt the user to re-provide their sign-in credentials
                    mAuth.getCurrentUser().reauthenticate(credential)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Log.d(TAG, "User re-authenticated.");
                                    if(task.isSuccessful()) {
                                        mAuth.getCurrentUser().delete()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        popupWindow.dismiss();
                                                        if (task.isSuccessful()) {
                                                            Log.d(TAG, "User account deleted.");
                                                            finish();
                                                            Intent mainActivity = new Intent(MainMenu.this, MainActivity.class);
                                                            mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                            startActivity(mainActivity);
                                                        } else {
                                                            Toast.makeText(MainMenu.this, "Failed deleting user account.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    } else {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        passwordLayout.setFocusable(true);
                                        passwordLayout.getEditText().setText(null);
                                        Toast.makeText(MainMenu.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });


    }

    public View showPopupWindow(int layout) {
        //Create a View object yourself through inflater
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(layout, null);

        //Specify the length and width through constants
        int width = WindowManager.LayoutParams.WRAP_CONTENT;
        int height = WindowManager.LayoutParams.WRAP_CONTENT;

        //Make Inactive Items Outside Of PopupWindow
        boolean focusable = true;

        //Create a window with our parameters
        popupWindow = new PopupWindow(popupView, width, height, focusable);

        //dismiss when clicked outside
        popupWindow.setOutsideTouchable(true);

        return popupView;
    }


}
