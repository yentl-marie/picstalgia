package com.app.picstalgia;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static android.os.Build.VERSION.SDK_INT;

public class PictureAudio extends AppCompatActivity {
    private final String APP_TAG = "Picstalgia";
    private MediaResource mediaResource;

    private File photoFile;
    private File audioFile;

    private ProgressBar progressBar;
    private ImageButton editButton;

    int[] ids; //ids of views in showphoto

    @Override
    public void onBackPressed() {
        deleteMedia();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_audio_activity);
        changeStatusBarColor();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaResource = new MediaResource(getBaseContext(), PictureAudio.this);

        progressBar = findViewById(R.id.progress_loader);
        ImageButton homeButton = findViewById(R.id.logo_name);
        MaterialButton photoButton = findViewById(R.id.photo_btn);
        MaterialButton audioButton = findViewById(R.id.audio_btn);
        MaterialButton doneButton = findViewById(R.id.done_btn);
        MaterialButton cancelButton = findViewById(R.id.cancel_btn);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImage();
            }
        });

        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordAudio();
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoFile = mediaResource.getPhotoFile();
                audioFile = mediaResource.getMediaFile();

                if(photoFile == null || audioFile == null) {
                    Toast.makeText(PictureAudio.this, "Please add the necessary media.", Toast.LENGTH_SHORT).show();
                } else {
                    Bitmap image = BitmapFactory.decodeFile(photoFile.getPath());
                    if(image.getWidth() < 150){
                        Toast.makeText(PictureAudio.this, "Image is too small!", Toast.LENGTH_SHORT).show();
                    } else {
                        if(isOnline()) {   // embedding and uploading image next
                            progressBar.setVisibility(View.VISIBLE);
                            finish();
                            Intent uploadIntent = new Intent(PictureAudio.this, Upload.class);
                            uploadIntent.putExtra("type", "audio");
                            startActivity(uploadIntent);
                        } else {
                            Toast.makeText(PictureAudio.this, "Please connect to the Internet.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteMedia();
                finish();
            }
        });
    }

    public boolean isUserVerified() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser().isEmailVerified()) {
            return true;
        }
        return false;
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

    //======================== take a photo ====================
    public void getImage() {
        ImagePicker.with(this)
                .saveDir(Environment.getExternalStorageDirectory()+"/Picstalgia/images")
                .cropSquare()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .createIntent(new Function1<Intent, Unit>() {
                    @Override
                    public Unit invoke(Intent intent) {
                        imagePickerLauncher.launch(intent);
                        return null;
                    }
                });

    }

    //====================== crop image 1x1 =======================
    ActivityResultLauncher<Intent>  imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode() == RESULT_OK) {
                    Uri uri = result.getData().getData();
                    String filename = uri.getLastPathSegment();
                    System.out.println("======file: "+uri.getLastPathSegment() +"  path" +uri.getPath());
                    if(!filename.contains("picstalgia")) {
                        photoFile = new File(uri.getPath());
                        int editButtonID = mediaResource.showImage(photoFile);
                        ids = mediaResource.getPhotoIds();
                        editButton = findViewById(editButtonID);
                        setEditButton();
                    } else {
                        Toast.makeText(this, "The image is already a Picstalgia.", Toast.LENGTH_SHORT).show();
                    }

                }
            });

    public void setEditButton() {
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent editIntent = new Intent(PictureAudio.this, DsPhotoEditorActivity.class);
                editIntent.setData(Uri.fromFile(photoFile));
                editIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY, "Picstalgia/images");
                editIntent.putExtra(DsPhotoEditorConstants.DS_TOOL_BAR_BACKGROUND_COLOR, getColor(R.color.black));
                editIntent.putExtra(DsPhotoEditorConstants.DS_MAIN_BACKGROUND_COLOR, getColor(R.color.dim_grey));
                editIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_TOOLS_TO_HIDE,
                        new int[] {
                                DsPhotoEditorActivity.TOOL_WARMTH,
                                DsPhotoEditorActivity.TOOL_CROP,
                                DsPhotoEditorActivity.TOOL_PIXELATE,
                                DsPhotoEditorActivity.TOOL_ROUND});

                photoEditorLauncher.launch(editIntent);
            }
        });

    }

    //============================edit photo=============================
    ActivityResultLauncher<Intent> photoEditorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Uri uri = result.getData().getData();
                    if(uri != null) {
                        //delete previous image
                        ConstraintLayout constraintLayout = findViewById(R.id.picture_media_layout);
                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.clone(constraintLayout);
                        for(int id: ids) {
                            constraintLayout.removeView(findViewById(id));
                        }
                        constraintSet.connect(R.id.plus, ConstraintSet.TOP, R.id.relative_layout, ConstraintSet.BOTTOM, 40);
                        constraintSet.applyTo(constraintLayout);
                        deletePhoto();

                        //get path
                        String editPath = uri.getPath();
                        if(SDK_INT >= Build.VERSION_CODES.R) {
                            editPath = getRealPathFromURI(uri);
                        }

                        //replace file with new edited image
                        photoFile = new File(editPath);
                        mediaResource.setPhotoPath(editPath);
                        int editButtonID = mediaResource.showImage(photoFile);
                        ids = mediaResource.getPhotoIds();
                        editButton = findViewById(editButtonID);
                        setEditButton();
                    } else {
                        Toast.makeText(PictureAudio.this, "Edited picture was not saved.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(contentUri, proj, null, null,
                    null);
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //===========================recording audio===================
    public void recordAudio() {
        String timeStamp = ZonedDateTime.now( ZoneId.systemDefault() )
                .format( DateTimeFormatter.ofPattern( "uuuuMMddHHmmss" ) );
        String videoFileName = "picstalgia"+timeStamp+".mp3";
        audioFile = getFileUri(videoFileName, "/audios");

        Intent recorderIntent = new Intent(PictureAudio.this, AudioRecorder.class);
        recorderIntent.putExtra("OUTPUT_FILE", audioFile.getPath());
        recordAudioLauncher.launch(recorderIntent);
    }

    ActivityResultLauncher<Intent> recordAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == RESULT_OK) {
                        mediaResource.showAudio(audioFile);
                    } else {
                        System.out.println("======result canceled");
                        mediaResource.setMedia(null, null);
                        boolean delete = audioFile.delete();
                    }
                }
            });

    //delete media when going back
    private void deletePhoto() {
        photoFile = mediaResource.getPhotoFile();
        if(photoFile != null) {
            mediaResource.setPhotoPath(null);
            boolean delete = photoFile.delete();
        }
    }

    public void deleteMedia() {
        deletePhoto();

        audioFile = mediaResource.getMediaFile();
        if(audioFile != null) {
            mediaResource.setMedia(null, null);
            boolean delete = audioFile.delete();
        }
    }

    // Returns the File for a photo stored on disk given the fileName
    public File getFileUri(String fileName, String path) {
        // Get safe storage directory for photos
        File mediaStorageDir = new File(
                Environment.getExternalStorageDirectory()+"/Picstalgia",
                path);
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.d(APP_TAG, "failed to create directory");
        }

        // Return the file target for the photo based on filename
        File file = new File(mediaStorageDir.getPath() + File.separator + fileName);
        return file;
    }
    public void changeStatusBarColor() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(PictureAudio.this, R.color.acapulco));
    }
}
