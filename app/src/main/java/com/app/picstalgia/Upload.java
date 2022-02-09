package com.app.picstalgia;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
//import com.watermark.androidwm.WatermarkBuilder;
//import com.watermark.androidwm.WatermarkDetector;
//import com.watermark.androidwm.listener.BuildFinishListener;
//import com.watermark.androidwm.listener.DetectFinishListener;
//import com.watermark.androidwm.task.DetectionReturnValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Upload extends AppCompatActivity {
    private final String MEDIA_TASK = "uploading_media";
    private final String IMAGE_TASK = "uploading_image";
    private final String WATERMARK_TASK = "watermarking";
    private final String URL_TASK = "downloading_url";
    private final String DONE_SUCCESS = "done_success";
    private final String DONE_FAIL = "done_fail";

    private final String VIDEO_PIC = "video";
    private final String AUDIO_PIC = "audio";
    private final String URL_PIC = "url";

    private ImageView imageView;
    private TextView progressText;
    private ProgressBar progressLoader;
    private MaterialButton finishButton;
    private MaterialButton cancelButton;
    private MaterialButton retryButton;

    private StorageReference storageRef;
    private StorageReference videoRef;
    private StorageReference imageRef;
    private StorageReference audioRef;

    private MediaResource mediaResource;
    private String timeStamp;
    private String mediaName;
    private String currentTask;
    private Uri mediaUri;
    private File photoFile;
    private String activitySrc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activitySrc = getIntent().getStringExtra("type");

        setContentView(R.layout.upload_activity);
        mediaResource = new MediaResource(getBaseContext(), Upload.this);
        photoFile = mediaResource.getPhotoFile();

        imageView = findViewById(R.id.photo_view);
        progressText = findViewById(R.id.progress_text);
        progressLoader = findViewById(R.id.progress_loader);
        finishButton = findViewById(R.id.finish_btn);
        cancelButton = findViewById(R.id.cancel_btn);
        retryButton = findViewById(R.id.retry_btn);

        imageView.setImageURI(Uri.fromFile(photoFile));

        currentTask = "";
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //delete the files used
                mediaResource.getPhotoFile().delete();
                mediaResource.getMediaFile().delete();
                mediaResource.setPhotoPath(null);
                mediaResource.setMedia(null, null);
                goToMain();

            }
        });

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!currentTask.equals(MEDIA_TASK)) {
                    deleteMedia();
                }
                progressLoader.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.INVISIBLE);
                retryButton.setVisibility(View.INVISIBLE);
                startUploading();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!currentTask.equals(MEDIA_TASK)) {
                    deleteMedia();
                }
                //delete the files used
                mediaResource.getPhotoFile().delete();
                mediaResource.getMediaFile().delete();
                mediaResource.setPhotoPath(null);
                mediaResource.setMedia(null, null);
                goToMain();


            }
        });
    }


    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Can't go back to previous page.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume(){ // so that functions can process after loading the interface
        super.onResume();

        if(currentTask.equals(DONE_SUCCESS)) {
            progressLoader.setVisibility(View.INVISIBLE);
            progressText.setText("Finished!");
            finishButton.setVisibility(View.VISIBLE);
        } else if(currentTask.equals(DONE_FAIL)) {
            progressLoader.setVisibility(View.INVISIBLE);
            progressText.setText("Error!");
            cancelButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.VISIBLE);
        } else {
            startUploading();
        }
    }


    //===============================UPLOADS==========================

    public void startUploading() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        storageRef = FirebaseStorage.getInstance().getReference();

        timeStamp = ZonedDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofPattern( "uuuuMMddHHmmss" ) );
        mediaName = "picstalgia"+timeStamp;

        if(activitySrc.equals(VIDEO_PIC)) {
            imageRef = storageRef.child("/users/"+user.getUid()+"/images/"+mediaName+".bmp");
            videoRef = storageRef.child("/users/"+user.getUid()+"/videos/"+mediaName+".mp4");
            uploadVideo();
        } else if (activitySrc.equals(AUDIO_PIC)) {
            imageRef = storageRef.child("/users/"+user.getUid()+"/images/"+mediaName+".bmp");
            audioRef = storageRef.child("/users/"+user.getUid()+"/audios/"+mediaName+".mp3");
            uploadAudio();
        } else if (activitySrc.equals(URL_PIC)) {
            imageRef = storageRef.child("/users/"+user.getUid()+"/images/"+mediaName+".bmp");
            String link = mediaResource.getLink();
            mediaUri = Uri.parse(link);
            watermarkImage(mediaUri);
        }
    }

    public void uploadVideo() {
        File videoFile = mediaResource.getMediaFile();
        if(videoFile != null) {
            // Create file metadata including the content type
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("video/mp4")
                    .build();

            Uri uri = Uri.fromFile(videoFile);
            UploadTask uploadTask = videoRef.putFile(uri, metadata);

            progressText.setText("Uploading video...");
            currentTask = MEDIA_TASK;

            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()) {
                        downloadUri(videoRef, "media");
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    currentTask = DONE_FAIL;
                    progressLoader.setVisibility(View.INVISIBLE);
                    cancelButton.setVisibility(View.VISIBLE);
                    retryButton.setVisibility(View.VISIBLE);
                    progressText.setText("Failed to get upload video");
                }
            });
        } else {
            currentTask = DONE_FAIL;
            progressLoader.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.VISIBLE);
            progressText.setText("Video file has been deleted.");
        }

    }

    public void uploadAudio() {
        File audioFile = mediaResource.getMediaFile();
        if(audioFile != null) {
            // Create file metadata including the content type
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("audio/mp3")
                    .build();

            Uri uri = Uri.fromFile(audioFile);
            UploadTask uploadTask = audioRef.putFile(uri, metadata);

            progressText.setText("Uploading audio...");
            currentTask = MEDIA_TASK;

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    downloadUri(audioRef, "media");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    currentTask = DONE_FAIL;
                    progressLoader.setVisibility(View.INVISIBLE);
                    cancelButton.setVisibility(View.VISIBLE);
                    retryButton.setVisibility(View.VISIBLE);
                    progressText.setText("Failed to get upload audio");
                }
            });
        } else {
            //update view
            currentTask = DONE_FAIL;
            progressLoader.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.VISIBLE);
            progressText.setText("Audio file has been deleted");
        }
    }


    public void uploadImage(Bitmap image) {
        // Create file metadata including the content type
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/bmp")
                .build();

        photoFile.delete();
        BmpFile bmpFile = new BmpFile();
        File file = bmpFile.saveBitmap(image);
        mediaResource.setPhotoPath(file.getPath());

        UploadTask uploadTask = imageRef.putFile(Uri.fromFile(file), metadata);

        progressText.setText("Uploading image...");
        currentTask = IMAGE_TASK;


        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()) {
                    downloadUri(imageRef, "image"); //add url to db
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                currentTask = DONE_FAIL;

                progressLoader.setVisibility(View.INVISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.VISIBLE);
                progressText.setText("Failed to upload final image");
            }
        });
    }

    public void saveToDatabase(Uri uri, String type) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, String> data = new HashMap<>();
        data.put(mediaName+"_"+activitySrc , uri.toString());

        if(type.equals("image")) {
            db.collection("images").document(user.getUid())
                    .set(data, SetOptions.merge());
        }else{
            db.collection("media").document(user.getUid())
                    .set(data, SetOptions.merge());
        }
    }

    //============================WATERMARKING================================

    public void downloadUri(StorageReference ref, String type){
        currentTask = URL_TASK;
        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                progressText.setText("Developing...");
                mediaUri = uri;
                if(type.equals("media")) {
                    watermarkImage(uri);       //watermark image after getting the uri
                } else if (type.equals("image")) {
                    saveToDatabase(uri, "image");       //save image url path to database

                    currentTask = DONE_SUCCESS;  // the uploading has finished!!!
                    progressLoader.setVisibility(View.INVISIBLE);
                    progressText.setText("Finished!");
                    finishButton.setVisibility(View.VISIBLE);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                currentTask = DONE_FAIL;
                progressLoader.setVisibility(View.INVISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.VISIBLE);
                progressText.setText("Failed to get audio url");
            }
        });
    }

    public void watermarkImage(Uri uri) {
        currentTask = WATERMARK_TASK;

        if(photoFile.exists()) {
            Bitmap polaroid = createPolaroid(BitmapFactory.decodeFile(photoFile.getPath()));  //create polaroid first for scanning to work
            BlindWatermark watermark = new BlindWatermark(photoFile, mediaName+"_"+activitySrc);

            Bitmap image = watermark.watermark(polaroid);

            saveToDatabase(uri, "media");
            uploadImage(image);

        } else {
            currentTask = DONE_FAIL;
            progressLoader.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            progressText.setText("Image file has been deleted.");
        }

    }


    public Bitmap createPolaroid(Bitmap image) {
        LinearLayout bg = new LinearLayout(this);
        bg.setDrawingCacheEnabled(true);
        bg.buildDrawingCache(true);

        //setting up the background color and border
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.WHITE); //background
        border.setStroke(1, Color.BLACK); //black border with full opacity
        bg.setBackground(border);
        bg.setOrientation(LinearLayout.VERTICAL);

        //frame
        LinearLayout frame = new LinearLayout(this);
        frame.setLayoutParams(new LinearLayout.LayoutParams(714, 714));
        ViewGroup.MarginLayoutParams frameMargin = (ViewGroup.MarginLayoutParams) frame.getLayoutParams();
        frameMargin.setMargins(25, 25, 25, 0);
        GradientDrawable frameBorder = new GradientDrawable();
        frameBorder.setColor(Color.WHITE);  //background
        frameBorder.setStroke(7, ColorStateList.valueOf(this.getColor(R.color.brown)));
        frame.setBackground(frameBorder);
        frame.setOrientation(LinearLayout.VERTICAL);

        //the image
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(700, 700));
        ViewGroup.MarginLayoutParams imageMargin = (ViewGroup.MarginLayoutParams) imageView.getLayoutParams();
        imageMargin.setMargins(7, 7,7,0);
        Bitmap resized = Bitmap.createScaledBitmap(image, 700, 700, true);
        imageView.setImageBitmap(resized);
        frame.addView(imageView);
        bg.addView(frame);

        //imageview of the logo
        ImageView logo = new ImageView(this);
        logo.setLayoutParams(new LinearLayout.LayoutParams(90, 90));
        ViewGroup.MarginLayoutParams logoMargin = (ViewGroup.MarginLayoutParams) logo.getLayoutParams();
        logoMargin.setMargins(30,55,0,55);

        logo.setImageResource(R.drawable.logo_circle);
        bg.addView(logo);

        bg.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        bg.layout(0, 0, bg.getMeasuredWidth(), bg.getMeasuredHeight());

        Bitmap polaroid = Bitmap.createBitmap(bg.getDrawingCache());
        bg.setDrawingCacheEnabled(false);
//
//        //save to file
//        try {
//            FileOutputStream out = new FileOutputStream(photoFile);
//            polaroid.compress(Bitmap.CompressFormat.JPEG, 100, out);
//            out.flush();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return polaroid;
    }

    //========================end methods===========================
    public void goToMain() {
        Intent mainMenu = new Intent(Upload.this, MainMenu.class);
        mainMenu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainMenu);
    }

    public void deleteMedia() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        if(activitySrc.equals(VIDEO_PIC)) {
            //delete from database
            db.collection("media").document(user.getUid())
                    .update(FieldPath.of(mediaName+"_"+VIDEO_PIC), FieldValue.delete());
            //delete from storage
            storageRef.child("users/"+user.getUid()+"/videos/"+mediaName+".mp4").delete();

        } else if(activitySrc.equals(AUDIO_PIC)) {
            //delete from database
            db.collection("media").document(user.getUid())
                    .update(FieldPath.of(mediaName+"_"+AUDIO_PIC), FieldValue.delete());
            //delete from storage
            storageRef.child("users/"+user.getUid()+"/audios/"+mediaName+".mp4").delete();
        }else {
            //delete from database
            db.collection("media").document(user.getUid())
                    .update(FieldPath.of(mediaName+"_"+URL_PIC), FieldValue.delete());
        }
    }

    //=========================Handle state changes====================
    @Override
    protected void onSaveInstanceState(Bundle outState) {  // dunno if this is necessary
        super.onSaveInstanceState(outState);
        // If there's an upload in progress, save the reference so you can query it later
        if(currentTask.equals(MEDIA_TASK)) {
            if(activitySrc.equals(VIDEO_PIC)) {
                outState.putString("reference", videoRef.toString());
            } else if (activitySrc.equals(AUDIO_PIC)) {
                outState.putString("reference", audioRef.toString());
            }
        } else if (currentTask.equals(IMAGE_TASK)) {
            outState.putString("reference", imageRef.toString());
        } else {
            outState.putString("reference", currentTask);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // If there was an upload in progress, get its reference and create a new StorageReference
        final String stringRef = savedInstanceState.getString("reference");
        if (stringRef == null) {
            return;
        }

        storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);

        if(currentTask.equals(MEDIA_TASK) || currentTask.equals(IMAGE_TASK)){
            // Find all UploadTasks under this StorageReference (in this example, there should be one)
            List<UploadTask> tasks = storageRef.getActiveUploadTasks();
            if (tasks.size() > 0) {
                // Get the task monitoring the upload
                UploadTask task = tasks.get(0);

                // Add new listeners to the task using an Activity scope
                task.addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {  //dunno if the previously stated listeners is still valid after interruption
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot state) {
                        if(currentTask.equals(MEDIA_TASK)) {
                            if(activitySrc.equals(VIDEO_PIC)) {
                                downloadUri(videoRef, "media");
                            } else if(activitySrc.equals(AUDIO_PIC)) {
                                downloadUri(audioRef, "media");
                            }
                        } else if(currentTask.equals(IMAGE_TASK)) {
                            //if the task is uploading image, the last step
                            currentTask = null;
                            progressLoader.setVisibility(View.INVISIBLE);
                            progressText.setText("Finished!");
                            finishButton.setVisibility(View.VISIBLE);

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        currentTask = DONE_FAIL;
                        progressLoader.setVisibility(View.INVISIBLE);
                        progressText.setText("Error while uploading.");
                        cancelButton.setVisibility(View.VISIBLE);
                        retryButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        } else {
            watermarkImage(mediaUri);
        }

    }
}

