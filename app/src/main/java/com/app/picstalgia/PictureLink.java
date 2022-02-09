package com.app.picstalgia;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static android.os.Build.VERSION.SDK_INT;

public class PictureLink extends AppCompatActivity {
    private final String APP_TAG = "Picstalgia";
    private MediaResource mediaResource;

    private File photoFile;

    private ProgressBar progressBar;
    private ImageButton editButton;
    private TextInputLayout linkLayout;

    int[] ids; //ids of views in showphoto

    @Override
    public void onBackPressed() {
        deleteMedia();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_link_activity);
        changeStatusBarColor();

        mediaResource = new MediaResource(getBaseContext(), PictureLink.this);

        ImageButton homeButton = findViewById(R.id.logo_name);
        MaterialButton photoButton = findViewById(R.id.photo_btn);
        linkLayout = findViewById(R.id.link_layout);
        progressBar = findViewById(R.id.progress_loader);
        MaterialButton clearButton = findViewById(R.id.clear_btn);
        MaterialButton openLinkButton = findViewById(R.id.open_link_btn);
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

        linkLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                linkLayout.setErrorEnabled(false);
                String url = linkLayout.getEditText().getText().toString();
                TextView linkText = findViewById(R.id.textView2);  //"Enter link" text

                if(verifyLink(url)) {
                    openLinkButton.setVisibility(View.VISIBLE);
                    linkText.setVisibility(View.INVISIBLE);
                } else {
                    openLinkButton.setVisibility(View.INVISIBLE);
                    linkText.setVisibility(View.VISIBLE);
                    linkText.setText("Invalid link!");
                    linkText.setTextColor(ColorStateList.valueOf(getColor(R.color.red)));
                    linkLayout.setErrorEnabled(true);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {  }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linkLayout.getEditText().setText(null);
                mediaResource.setMedia(null, null);
                openLinkButton.setVisibility(View.INVISIBLE);
            }
        });

        openLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String link =  linkLayout.getEditText().getText().toString().trim();
                if (!link.startsWith("http://") && !link.startsWith("https://")) {
                    link = "http://" + link;
                }

                if(verifyLink(link)) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                }

            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String link =  linkLayout.getEditText().getText().toString().trim();
                photoFile = mediaResource.getPhotoFile();
                if(!link.equals("") && photoFile != null) {
                    if(verifyLink(link)) {
                        mediaResource.setMedia(link, mediaResource.URL_TYPE);

                        Bitmap image = BitmapFactory.decodeFile(photoFile.getPath());
                        if(image.getWidth() < 150){
                            Toast.makeText(PictureLink.this, "Image is too small!", Toast.LENGTH_SHORT).show();
                        } else {
                            if(isOnline()) {    // embedding and uploading image next
                                progressBar.setVisibility(View.VISIBLE);
                                finish();
                                Intent uploadIntent = new Intent(PictureLink.this, Upload.class);
                                uploadIntent.putExtra("type", "url");
                                startActivity(uploadIntent);
                            } else {
                                Toast.makeText(PictureLink.this, "Please connect to the Internet.", Toast.LENGTH_SHORT).show();
                            }
                        }

                    } else {
                        Toast.makeText(PictureLink.this, "Invalid link", Toast.LENGTH_SHORT).show();
                        mediaResource.setMedia(null, null);
                    }
                } else {
                    Toast.makeText(PictureLink.this, "Please add the necessary media.", Toast.LENGTH_SHORT).show();
                    if(link.equals("")){
                        linkLayout.setError("Enter link");
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
                Intent editIntent = new Intent(PictureLink.this, DsPhotoEditorActivity.class);
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

    //===============================edit photo========================
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
                        boolean delete  = photoFile.delete();

                        //get path
                        String editPath = uri.getPath();
                        if(SDK_INT >= Build.VERSION_CODES.R) {
                            editPath = getRealPathFromURI(uri);
                        }

                        //replace file with new edited image
                        photoFile = new File(editPath);
                        int editButtonID = mediaResource.showImage(photoFile);
                        ids = mediaResource.getPhotoIds();
                        editButton = findViewById(editButtonID);
                        setEditButton();
                    } else {
                        System.out.println("==================no edit save");
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

    public boolean verifyLink(String link) {
        if (Patterns.WEB_URL.matcher(link).matches()) {
            return true;
        } else {
//            linkLayout.getEditText().getText().clear();
            return false;
        }
    }


    //delete media when going back to prev activity
    public void deleteMedia() {
        photoFile = mediaResource.getPhotoFile();
        if(photoFile != null) {
            mediaResource.setPhotoPath(null);
            boolean delete = photoFile.delete();
        }
    }

    public void changeStatusBarColor() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(PictureLink.this, R.color.acapulco));
    }
}
