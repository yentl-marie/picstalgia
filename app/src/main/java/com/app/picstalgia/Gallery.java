package com.app.picstalgia;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Gallery extends AppCompatActivity {
    private final String SELECT_MODE = "select_mode";
    private final String DELETE_ACTION = "deleting";
    private final String DOWNLOAD_ACTION = "downloading";
    private final String SHARE_ACTION = "sharing";
    private final String ERROR_URI = "image/error.bmp";
    private String mode;
    private ProgressBar progressBar;
    private StorageReference storageRef;
    Map<String, Object> urlMap;
    List<GalleryImage> imageList;
    String[] key;

    int errorNum; //for the select methods
    String mediaPath = ""; //for the audio and video storage references in delete()

    MaterialButton selectButton;
    LinearLayout actionTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity);
        changeStatusBarColor();

        storageRef = FirebaseStorage.getInstance().getReference();

        progressBar = findViewById(R.id.progress_loader);
        MaterialButton homeButton = findViewById(R.id.back_btn);
        MaterialButton refreshButton = findViewById(R.id.refresh_btn);
        selectButton = findViewById(R.id.select_btn);
        actionTab = findViewById(R.id.action_tab);

        imageList = new LinkedList<>();
        mode = "";
        getItems();

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mode.equals(SELECT_MODE)) {
                    for (GalleryImage image: imageList) {
                        image.unselectImage();                      //reset all images, remove selection
                    }

                } else {
                    refresh();
                }
            }
        });

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                select();
            }
        });
    }

    public void select() {
        MaterialButton deleteButton = findViewById(R.id.delete_btn);
        MaterialButton shareButton = findViewById(R.id.share_btn);
        MaterialButton downloadButton = findViewById(R.id.download_btn);

        if(mode.equals("")) {
            mode = SELECT_MODE;
            selectButton.setText("Cancel");

            for (GalleryImage image: imageList) {
                image.setMode(mode);
            }

            //set up the action tab buttons
            actionTab.setVisibility(View.VISIBLE);

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteImage();
                }
            });

            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    shareImage();
                }
            });

            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    downloadImage();
                }
            });
        } else {
            mode = "";
            selectButton.setText("Select");
            actionTab.setVisibility(View.GONE);

            for (GalleryImage image: imageList) {
                image.unselectImage();                      //reset all images, remove selection
                image.setMode(mode);
            }
        }
    }
    public void refresh() {
        LinearLayout galleryLayout = findViewById(R.id.linear_layout);
        galleryLayout.removeAllViews();
        progressBar.setVisibility(View.VISIBLE);
        getItems();

        mode = "";
        selectButton.setText("Select");
        actionTab.setVisibility(View.GONE);
    }

    public void deleteImage() {
        progressBar.setVisibility(View.VISIBLE);

        ActivityAsyncTask asyncTask = (ActivityAsyncTask) new ActivityAsyncTask(new GalleryActivityInterface() {
            @Override
            public void processFinish(ArrayList<Uri> error) {
                if(error == null) {
                    Toast.makeText(Gallery.this, "No image selected.", Toast.LENGTH_SHORT).show();
                } else {
                    refresh();
                    if(error.isEmpty()) {
                        Toast.makeText(Gallery.this, "Deleted successfully!", Toast.LENGTH_SHORT).show();  //no error
                    } else {
                        Toast.makeText(Gallery.this, "Error: Failed to delete files.", Toast.LENGTH_SHORT).show();;    //has error
                    }
                }
            }
        }).execute(DELETE_ACTION);

    }


    public void shareImage() {
        progressBar.setVisibility(View.VISIBLE);

        ActivityAsyncTask shareAsync = (ActivityAsyncTask) new ActivityAsyncTask(new GalleryActivityInterface() {
            @Override
            public void processFinish(ArrayList<Uri> output) {
                if(output == null) {
                    Toast.makeText(Gallery.this, "No image selected.", Toast.LENGTH_SHORT).show();
                } else {
                    if(!output.contains(Uri.parse(ERROR_URI))) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                        intent.setType("image/*");
                        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, output);
                        startActivity(Intent.createChooser(intent , "Share image"));
                        refresh();
                    } else {
                        Toast.makeText(Gallery.this, "Error detected.", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        }).execute(SHARE_ACTION);  //download first


    }

    public void downloadImage() {
        progressBar.setVisibility(View.VISIBLE);

        ActivityAsyncTask asyncTask = (ActivityAsyncTask) new ActivityAsyncTask(new GalleryActivityInterface() {
            @Override
            public void processFinish(ArrayList<Uri> uriList) {
                if(uriList == null) {
                    Toast.makeText(Gallery.this, "No image selected.", Toast.LENGTH_SHORT).show();
                } else {
                    refresh();
                    if(uriList.contains(Uri.parse(ERROR_URI))) {
                        Toast.makeText(Gallery.this, "Error detected.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Gallery.this, "See downloads in Picstalgia/Images folder in your local memory.", Toast.LENGTH_LONG).show();
                    }

                }

            }
        }).execute(DOWNLOAD_ACTION);

    }


    //change the color of the area at the top
    public void changeStatusBarColor() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(Gallery.this, R.color.acapulco));
    }

    public void getItems() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        progressBar.setVisibility(View.VISIBLE);
        db.collection("images").document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        urlMap = document.getData();
                        //save the filenames to list
                        Object[] obj = urlMap.keySet().toArray();
                        key = new String[obj.length];
                        for(int i = 0; i<obj.length; i++) {
                            key[i] = (String) obj[i];
                        }
                        addImageButtons();
                    } else {
                        Toast.makeText(Gallery.this, "No images available.", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(Gallery.this, "Error loading the album", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    System.out.println("===error: ");
                }
            }
        });
    }

    public void addImageButtons() {
        LinearLayout parent = findViewById(R.id.linear_layout);
        LinearLayout row = new LinearLayout(this);
        int j = 0;
        for(int i=0; i<key.length; i++) {
            GalleryImage image = new GalleryImage(this, this);
            imageList.add(image);

            image.setFilename(key[i]);
            image.addView((String) urlMap.get(key[i]));   //create the buttons, pass the url
            row.addView(image);                           //add buttons to view
            j++;

            if(j >= 3 || i == key.length-1) {
                j = 0;
                parent.addView(row);
                row = new LinearLayout(this);
            }
        }
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("mode", mode);
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final String stringMode = savedInstanceState.getString("mode");
        if (stringMode == null) {
            return;
        }

        this.mode = stringMode;
    }

    // The types specified here are the input data type, the progress type, and the result type
    private class ActivityAsyncTask extends AsyncTask<String, Void, ArrayList<Uri>> {
        public GalleryActivityInterface delegate = null;

        public ActivityAsyncTask(GalleryActivityInterface delegate) {
            this.delegate = delegate;
        }

        @SafeVarargs
        @Override
        protected final ArrayList<Uri> doInBackground(String... strings) {
            int selected = 0;
            ArrayList<Uri> uriList = new ArrayList<>();
            for (GalleryImage image : imageList) {
                if (image.isSelected()) {
                    selected++;
                    if(strings[0].equals(DOWNLOAD_ACTION) || strings[0].equals(SHARE_ACTION)) {
                        downloadImage(uriList, image, strings[0]);
                    } else {
                        deleteImage(uriList, image);
                    }
                }
            }

            if(selected == 0) {
                return null;
            }

            return uriList;
        }

        protected void onPreExecute(){
            // Runs on the UI thread before doInBackground
            // Good for toggling visibility of a progress indicator
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }


        protected void onPostExecute(ArrayList<Uri> result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
            delegate.processFinish(result);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
        }

        private void downloadImage(ArrayList<Uri> uriList, GalleryImage image, String action) {
            String[] key = image.getFilename().split("_");
            String name = key[0];

            //download using glide
            Glide.with(Gallery.this)
                    .asBitmap()
                    .load(image.getUrl()) // image url
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable @org.jetbrains.annotations.Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            Uri errorUri = Uri.parse(ERROR_URI);
                            uriList.add(errorUri);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            File file = new File(Environment.getExternalStorageDirectory()+"/Picstalgia/images/"+name+".bmp");

                            if(!file.exists()) {
                                BmpFile bmpFile = new BmpFile();
                                File bitmap = bmpFile.saveBitmap(resource, name);
                                uriList.add(FileProvider.getUriForFile(Gallery.this, "com.app.picstalgia.fileprovider", bitmap));
                            } else {
                                if(action.equals(DOWNLOAD_ACTION)) {
                                    Toast.makeText(Gallery.this, name+" already exists in the Picstalgia folder.", Toast.LENGTH_SHORT).show();
                                }
                                uriList.add(FileProvider.getUriForFile(Gallery.this, "com.app.picstalgia.fileprovider", file));
                            }
                        }




                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }


        private void deleteImage(ArrayList<Uri> uriList, GalleryImage image) {
            String[] key = image.getFilename().split("_");
            String name = key[0];
            String type = key[1];

            //get the storage references
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            StorageReference imageRef = storageRef.child("users/"+user.getUid()+"/images/"+name+".bmp");

            if(type.equals("video")) {
                mediaPath = "users/"+user.getUid()+"/videos/"+name+".mp4";
            } else if(type.equals("audio")) {
                mediaPath = "users/"+user.getUid()+"/audios/"+name+".mp3";
            }

            //delete the field in database
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("media").document(user.getUid())
                    .update(FieldPath.of(image.getFilename()), FieldValue.delete());


            db.collection("images").document(user.getUid())
                    .update(FieldPath.of(image.getFilename()), FieldValue.delete()).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull @NotNull Exception e) {
                    Uri errorUri = Uri.parse(ERROR_URI);  // just a dummy uri for detecting errors
                    uriList.add(errorUri);
                }
            }).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    //delete the files from storage
                    if(!type.equals("url")) {  //if image is not embedded with url
                        StorageReference mediaRef = storageRef.child(mediaPath);
                        mediaRef.delete().addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull @NotNull Exception e) {
                                Uri errorUri = Uri.parse(ERROR_URI);  // just a dummy uri for detecting errors
                                uriList.add(errorUri);
                            }
                        });

                    }
                    imageRef.delete().addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull @NotNull Exception e) {
                            Uri errorUri = Uri.parse(ERROR_URI);  // just a dummy uri for detecting errors
                            uriList.add(errorUri);
                        }
                    });
                }
            });

            //if uriList is empty no errors detected
        }
    }
}

