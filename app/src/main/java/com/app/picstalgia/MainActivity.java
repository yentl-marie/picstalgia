package com.app.picstalgia;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION.SDK_INT;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String[]> requestMultiplePermissionLauncher;

    String[] mediaPermissions = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    String[] permissions = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent loginIntent = new Intent(MainActivity.this, LogIn.class);
            startActivity(loginIntent);
        } else {
            checkPermissions();
        }

        requestMultiplePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), map -> {
                    System.out.println("======map "+map.entrySet());
                    if (map.containsValue(false)) {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                        System.out.println("======permission denied");
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                            new AlertDialog.Builder(this).setCancelable(false).setTitle("Hi User")
                                    .setMessage("The requested permissions are all required for this application. If you still deny , please go to Settings ").
                                    setPositiveButton("TRY AGAIN ", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (SDK_INT >= Build.VERSION_CODES.R) {
                                                requestMultiplePermissionLauncher.launch(mediaPermissions);
                                            } else {
                                                requestMultiplePermissionLauncher.launch(permissions);
                                            }
                                        }
                                    }).setNeutralButton("DISMISS", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();

                                        }
                            }).show();
                        }
                    } else {
                        System.out.println("===========permission accepted");
                        showMenu();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    public void checkPermissions() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager() || !isPermitted(mediaPermissions)) {
                showCheckPermissionLayout();
            } else {
                showMenu();
            }
        }else {
            if(!isPermitted(permissions)) {
                showCheckPermissionLayout();
            }else {
                showMenu();
            }
        }
    }

    public void showCheckPermissionLayout() {
        setContentView(R.layout.check_permission_layout);
        MaterialButton continueButton = findViewById(R.id.continue_btn);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askPermissions();
            }
        });

    }

    public void askPermissions() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            //different settings for android 11 in terms of read and write permissions
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                permissionLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                permissionLauncher.launch(intent);
            }

        }else {
            requestMultiplePermissionLauncher.launch(permissions);

        }
    }

    public boolean isPermitted(String[] permissions) {
        int result;
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
//            if(p.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
//                return true;
//            }
        }
        return true;
    }

    ActivityResultLauncher<Intent> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            // perform action when allow permission success
                            if(!isPermitted(mediaPermissions)) {
                                requestMultiplePermissionLauncher.launch(mediaPermissions);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                            askPermissions();
                        }
                    }
                }
            });

    private void showMenu(){
        Intent mainMenu = new Intent(MainActivity.this, MainMenu.class);
        startActivity(mainMenu);
    }

}