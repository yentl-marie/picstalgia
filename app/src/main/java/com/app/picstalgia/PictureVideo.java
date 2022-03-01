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
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;


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

public class PictureVideo  extends AppCompatActivity {
    private final String APP_TAG = "Picstalgia";

    private File photoFile;
    private File videoFile;
    private ProgressBar progressBar;
    private ImageButton editButton;
    private MediaResource mediaResource;

    int[] ids; //ids of views in showphoto

    @Override
    public void onBackPressed() {
        deleteMedia();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_video_activity);
        changeStatusBarColor();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaResource = new MediaResource(getBaseContext(), PictureVideo.this);

        progressBar = findViewById(R.id.progress_loader);
        ImageButton homeButton = findViewById(R.id.logo_name);
        MaterialButton photoButton = findViewById(R.id.photo_btn);
        MaterialButton videoButton = findViewById(R.id.video_btn);
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

        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordVideo();
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoFile = mediaResource.getPhotoFile();
                videoFile = mediaResource.getMediaFile();

                if(photoFile == null || videoFile == null) {
                    Toast.makeText(PictureVideo.this, "Please add the necessary media.", Toast.LENGTH_SHORT).show();
                } else {
                    Bitmap image = BitmapFactory.decodeFile(photoFile.getPath());
                    if(image.getWidth() < 150){
                        Toast.makeText(PictureVideo.this, "Image is too small!", Toast.LENGTH_SHORT).show();
                    } else {
                        if(isOnline()) {  // embedding and uploading image next
                            progressBar.setVisibility(View.VISIBLE);
                            finish();
                            Intent uploadIntent = new Intent(PictureVideo.this, Upload.class);
                            uploadIntent.putExtra("type", "video");
                            startActivity(uploadIntent);
                        } else {
                            Toast.makeText(PictureVideo.this, "Please connect to the Internet.", Toast.LENGTH_SHORT).show();
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
                        int editID = mediaResource.showImage(photoFile);
                        ids = mediaResource.getPhotoIds();
                        editButton = findViewById(editID);
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
                Intent editIntent = new Intent(PictureVideo.this, DsPhotoEditorActivity.class);
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

    //========================= edit photo ======================
    ActivityResultLauncher<Intent> photoEditorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Uri uri = result.getData().getData();
                    if(uri != null) {
                        //delete previous image and image view
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
                        int editID = mediaResource.showImage(photoFile);
                        ids = mediaResource.getPhotoIds();
                        editButton = findViewById(editID);
                        setEditButton();
                    } else {
                        Toast.makeText(PictureVideo.this, "Edited picture was not saved.", Toast.LENGTH_SHORT).show();
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
    //===================== for video recording ==========================
    public void recordVideo() {
        String timeStamp = ZonedDateTime.now( ZoneId.systemDefault() )
                .format( DateTimeFormatter.ofPattern( "uuuuMMddHHmmss" ) );
        String videoFileName = "picstalgia"+timeStamp+".mp4";
        videoFile = getFileUri(videoFileName, "/videos");


        Uri fileProvider = FileProvider.getUriForFile(PictureVideo.this, "com.app.picstalgia.fileprovider", videoFile);
        recordVideoLauncher.launch(fileProvider);

    }

    ActivityResultContract<Uri, Uri> recordVideoContract = new ActivityResultContract<Uri, Uri>() {
        @NonNull
        @Override
        public Intent createIntent(@NonNull  Context context, Uri input) {
            System.out.println("========record video ");
            Intent recordVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            recordVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60);
            recordVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, input);
            return recordVideoIntent;
        }

        @Override
        public Uri parseResult(int resultCode, @Nullable  Intent intent) {
            return intent.getData();
        }
    };

    ActivityResultLauncher<Uri> recordVideoLauncher = registerForActivityResult(
            recordVideoContract,
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    System.out.println("=========bitmap "+result);
                    if(result != null) {
                        mediaResource.showVideo(videoFile);
                    }
                }
            });


    //change the color of the area at the top
    public void changeStatusBarColor() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(PictureVideo.this, R.color.acapulco));
    }

    //delete media when going back

    private void deletePhoto() {
        photoFile = mediaResource.getPhotoFile();
        if(photoFile != null) {
            boolean delete = photoFile.delete();
            mediaResource.setPhotoPath(null);
        }
    }

    private void deleteMedia() {
        deletePhoto();

        videoFile = mediaResource.getMediaFile();
        if(videoFile != null) {
            boolean delete = videoFile.delete();
            mediaResource.setMedia(null, null);
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
}
