package com.app.picstalgia;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Scanner extends AppCompatActivity {
    private String src;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_scan_options);

        src = "";

        MaterialButton cameraButton = findViewById(R.id.camera_btn);
        MaterialButton galleryButton = findViewById(R.id.photo_library_btn);
        MaterialButton closeButton = findViewById(R.id.close_btn);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                src = "gallery";

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                Intent.createChooser(intent,"Select Picstalgia");
                imagePickerLauncher.launch(intent);
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraScanner = new Intent(Scanner.this, CameraScanner.class);
                startActivity(cameraScanner);
            }
        });
    }

    ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode() == RESULT_OK) {
                    Uri uri = result.getData().getData();
                    if(uri.getLastPathSegment().contains("picstalgia")) {
                        String path = Environment.getExternalStorageDirectory()+"/"+uri.getPath().split(":")[1];
                        System.out.println("======path: "+path);

                        File photoFile = new File(path);
                        if(photoFile.exists()) {
                            String name = scanFile(photoFile);
                            finish();

                            if(name != null) {
                                String type = name.split("_")[1];
                                if(type.equals("url")) {
                                    playUrlMedia(name);  //play url from here it won't open the player
                                } else {
                                    startMediaPlayer(name);
                                }
                            }else {
                                Toast.makeText(this, "Error: Media is not extracted.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Image file is not a Picstalgia.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void startMediaPlayer(String name) {
        Intent mediaPlayer = new Intent(this, MediaPlayer.class);
        mediaPlayer.putExtra("filename", name);
        startActivity(mediaPlayer);
    }

    private String scanFile(File photoFile) {
        Bitmap image = BitmapFactory.decodeFile(photoFile.getPath());

        BlindWatermark scanner = new BlindWatermark();
        Bitmap qr = scanner.scan(image);

        return scanner.readQRImage(qr);
    }

    public void playUrlMedia(String name) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("media").document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if(doc != null) {
                        String link = doc.getString(name);
                        if (!link.startsWith("http://") && !link.startsWith("https://")) {
                            link = "http://" + link;
                        }

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        startActivity(browserIntent);
                    }
                }
            }
        });

    }
}

