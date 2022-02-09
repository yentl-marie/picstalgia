package com.app.picstalgia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class GalleryImage extends MaterialCardView {
    private final String SELECT_MODE = "select_mode";
    private String currMode;
    private boolean isSelected;
    private Activity activity;
    private Context context;
    private ImageButton imageButton;
    private String imageUri;
    private String filename;
    private Drawable imageDrawable;
    private int displayWidth;
    private PopupWindow popupWindow;


    public GalleryImage(Context context, Activity activity) {
        super(context);

        this.context = context;
        this.activity = activity;
        this.currMode = "";
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayWidth = displayMetrics.widthPixels;

    }

    public void addView(String imageUri) {
        this.imageUri = imageUri;

        this.setLayoutParams(new LayoutParams(displayWidth/4, displayWidth/4));
        this.setElevation(5);
        this.setCardBackgroundColor(ColorStateList.valueOf(this.context.getColor(R.color.brown)));
        ViewGroup.MarginLayoutParams cardMargin = (ViewGroup.MarginLayoutParams) this.getLayoutParams();
        cardMargin.setMargins(displayWidth/32, displayWidth/32, displayWidth/32, displayWidth/32);

        addButtonView();
    }

    public void addButtonView() {
        imageButton = new ImageButton(context);

        imageButton.setLayoutParams(new LayoutParams(displayWidth/4 - 10, displayWidth/4 - 10));
        ViewGroup.MarginLayoutParams buttonMargin = (ViewGroup.MarginLayoutParams) imageButton.getLayoutParams();
        buttonMargin.setMargins(5,5,5,5);
        imageButton.setPadding(5,5,5,5);

        //get image from url
        Glide.with(this)
                .load(imageUri) // image url
                .placeholder(R.drawable.ic_baseline_photo_24) // any placeholder to load at start
                .error(R.drawable.ic_baseline_warning_24)  // any image in case of error
                .centerCrop()
                .into(imageButton);

        this.addView(imageButton);
        imageDrawable = imageButton.getBackground();

        imageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currMode.equals(SELECT_MODE)) {
                    if(!isSelected) {
                        selectImage();
                    } else {
                        unselectImage();
                    }
                } else {
                    viewImage();
                }

            }
        });
    }

    public void viewImage() {
        View popupView = showPopupWindow(R.layout.popup_view_media);
        popupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        ImageView imageView = popupView.findViewById(R.id.image_view);
        MaterialButton closeButton = popupView.findViewById(R.id.close_btn);
        MaterialButton playButton = popupView.findViewById(R.id.media_preview);


        //get image from url
        Glide.with(GalleryImage.this)
                .load(imageUri) // image url
                .placeholder(R.drawable.ic_baseline_photo_24) // any placeholder to load at start
                .error(R.drawable.ic_baseline_warning_24)  // any image in case of error
                .centerCrop()
                .into(imageView);

        imageView.setLayoutParams(new LinearLayout.LayoutParams(displayWidth-displayWidth/5, displayWidth));
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });

        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();

                String type = filename.split("_")[1];
                if(type.equals("url")) {
                    playUrlMedia(filename);
                }else {
                    Intent mediaPlayer = new Intent(context, MediaPlayer.class);
                    mediaPlayer.putExtra("filename", filename);
                    context.startActivity(mediaPlayer);
                }

            }
        });
    }

    public void playUrlMedia(String name) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("media").document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null) {
                        String link = doc.getString(name);
                        if (!link.startsWith("http://") && !link.startsWith("https://")) {
                            link = "http://" + link;
                        }

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        context.startActivity(browserIntent);
                    }
                }
            }
        });
    }

    public void setMode(String mode) {
        this.currMode = mode;
    }

    public View showPopupWindow(int layout) {
        //Create a View object yourself through inflater
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
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

    public void selectImage() {
        this.setCardBackgroundColor(ColorStateList.valueOf(this.context.getColor(R.color.green)));
        this.isSelected = true;
    }

    public void unselectImage() {
        this.setCardBackgroundColor(ColorStateList.valueOf(this.context.getColor(R.color.brown)));
        this.isSelected = false;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public String getUrl() {
        return this.imageUri;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return this.filename;
    }
}