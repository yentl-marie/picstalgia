package com.app.picstalgia;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;

public class MediaResource {
    public String VIDEO_TYPE = "video";
    public String AUDIO_TYPE = "audio";
    public String URL_TYPE = "url";

    private Context context;
    private Activity activity;

    private int displayWidth;
    private int displayHeight;

    //image view
    ImageButton photoView;
    ImageButton recaptureButton;
    ImageButton editButton;



    public MediaResource(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;
    }

    public int showImage(File photoFile) {
        setPhotoPath(photoFile.getPath());
        Bitmap imageBitmap = BitmapFactory.decodeFile(photoFile.getPath());
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, (displayWidth/3)+100, (displayWidth/3)+100, true);

        ConstraintLayout constraintLayout = this.activity.findViewById(R.id.picture_media_layout);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        //show image
        photoView = new ImageButton(this.context);
        photoView.setMaxWidth(10);
        photoView.setMaxHeight(10);
        photoView.setBackgroundColor(this.context.getColor(R.color.pale_orange));
        photoView.setPadding(15, 15, 15, 15);
        photoView.setImageBitmap(resizedBitmap);
        photoView.setId(View.generateViewId());
        photoView.bringToFront();
        photoView.bringToFront();
        constraintLayout.addView(photoView);

        //button if user want to change image
        recaptureButton = new ImageButton(this.context);
        recaptureButton.bringToFront();
        recaptureButton.setPadding(10,10,10,10);
        recaptureButton.setImageResource(R.drawable.ic_baseline_clear_24);
        recaptureButton.setImageTintList(ColorStateList.valueOf(this.context.getColor(R.color.black)));
        recaptureButton.setBackgroundColor(this.context.getColor(R.color.pale_orange));
        recaptureButton.setId(View.generateViewId());
        constraintLayout.addView(recaptureButton);

        editButton = new ImageButton(this.context);
        editButton.bringToFront();
        editButton.setPadding(10,10,10,10);
        editButton.setImageResource(R.drawable.ic_baseline_edit_24);
        editButton.setImageTintList(ColorStateList.valueOf(this.context.getColor(R.color.black)));
        editButton.setBackgroundColor(this.context.getColor(R.color.pale_orange));
        editButton.setId(View.generateViewId());
        constraintLayout.addView(editButton);

        //set constraints
        constraintSet.constrainHeight(photoView.getId(), ConstraintSet.WRAP_CONTENT);
        constraintSet.constrainWidth(photoView.getId(), ConstraintSet.WRAP_CONTENT);


        constraintSet.connect(photoView.getId(), ConstraintSet.TOP, R.id.textView, ConstraintSet.TOP, 0);
        constraintSet.connect(photoView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        constraintSet.connect(photoView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);

        constraintSet.connect(R.id.plus, ConstraintSet.TOP, photoView.getId(), ConstraintSet.BOTTOM, displayWidth/18);
        constraintSet.applyTo(constraintLayout);

        ConstraintSet constraintSet2 = new ConstraintSet();
        constraintSet2.clone(constraintLayout);

        constraintSet2.constrainHeight(recaptureButton.getId(), ConstraintSet.WRAP_CONTENT);
        constraintSet2.constrainWidth(recaptureButton.getId(), ConstraintSet.WRAP_CONTENT);

        constraintSet2.connect(recaptureButton.getId(), ConstraintSet.TOP, photoView.getId(), ConstraintSet.TOP, 0);
        constraintSet2.connect(recaptureButton.getId(), ConstraintSet.END, photoView.getId(), ConstraintSet.END, 0);
        constraintSet2.applyTo(constraintLayout);

        ConstraintSet constraintSet3 = new ConstraintSet();
        constraintSet3.clone(constraintLayout);

        constraintSet3.constrainHeight(editButton.getId(), ConstraintSet.WRAP_CONTENT);
        constraintSet3.constrainWidth(editButton.getId(), ConstraintSet.WRAP_CONTENT);

        constraintSet3.connect(editButton.getId(), ConstraintSet.BOTTOM, photoView.getId(), ConstraintSet.BOTTOM, 0);
        constraintSet3.connect(editButton.getId(), ConstraintSet.END, photoView.getId(), ConstraintSet.END, 0);
        constraintSet3.applyTo(constraintLayout);


        photoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri photoURI = FileProvider.getUriForFile(context,
                        "com.app.picstalgia.fileprovider",
                        photoFile);
                intent.setDataAndType(photoURI,"image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            }
        });

        recaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setPhotoPath(null);
                boolean delete = photoFile.delete();

                removePhotoView();
            }
        });

        return editButton.getId();
    }

    public int[] getPhotoIds() {
        return new int[] { photoView.getId(), recaptureButton.getId(), editButton.getId()};
    }
    public void removePhotoView() {
        ConstraintLayout constraintLayout = this.activity.findViewById(R.id.picture_media_layout);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        constraintLayout.removeView(photoView);
        constraintLayout.removeView(recaptureButton);
        constraintLayout.removeView(editButton);

        constraintSet.connect(R.id.plus, ConstraintSet.TOP, R.id.relative_layout, ConstraintSet.BOTTOM, 40);
        constraintSet.applyTo(constraintLayout);
    }

    public void showVideo(File videoFile) {
        String videoPath = videoFile.getPath();
        String[] names = videoPath.split("/");
        String filename = names[names.length - 1];

        setMedia(videoPath, VIDEO_TYPE);

        MaterialButton videoButton = this.activity.findViewById(R.id.video_btn);
        TextView vidText = this.activity.findViewById(R.id.textView2);

        videoButton.setVisibility(View.GONE);
        vidText.setVisibility(View.GONE);

        ConstraintLayout constraintLayout = (ConstraintLayout) this.activity.findViewById(R.id.picture_media_layout);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        int cardWidth = displayWidth-(displayWidth/4);
        int cardHeight = displayWidth/6;
        CardView cardView = new MaterialCardView(this.activity);
        cardView.setMinimumHeight(cardHeight);
        cardView.setMinimumWidth(cardWidth);
        cardView.setRadius(15);
        cardView.setId(View.generateViewId());
        cardView.setCardBackgroundColor(ColorStateList.valueOf(this.context.getColor(R.color.pale_orange)));

        //button for playing video
        MaterialButton playButton = new MaterialButton(this.activity);
        playButton.setIcon(ContextCompat.getDrawable(this.context, R.drawable.ic_baseline_play_arrow_24));
        playButton.setIconTint(ColorStateList.valueOf(this.context.getColor(R.color.light_brown)));
        playButton.setIconSize(cardHeight/2);
        playButton.setStateListAnimator(null);
        playButton.setBackgroundColor(Color.TRANSPARENT);
        playButton.setTextColor(this.context.getColor(R.color.brown));
        playButton.setAllCaps(false);
        playButton.setMaxWidth(150);
        playButton.setMaxHeight(150);
        playButton.setText(filename);
        playButton.setPadding(cardHeight/5,0,(int) (cardHeight/1.2),0);
        playButton.setId(View.generateViewId());
        cardView.addView(playButton);

        //button for retaking video
        ImageButton retakeButton = new ImageButton(this.context);
        retakeButton.bringToFront();
        retakeButton.bringToFront();
        retakeButton.setMaxWidth(10);
        retakeButton.setPadding(10,10,10,10);
        retakeButton.setImageResource(R.drawable.ic_baseline_clear_24);
        retakeButton.setImageTintList(ColorStateList.valueOf(this.context.getColor(R.color.brown)));
        retakeButton.setBackgroundTintList(ColorStateList.valueOf(this.context.getColor(R.color.pale_orange)));
        retakeButton.setId(View.generateViewId());
        cardView.addView(retakeButton);

        ViewGroup.MarginLayoutParams margin = (ViewGroup.MarginLayoutParams) retakeButton.getLayoutParams();
        margin.leftMargin = (int) (cardWidth-(cardHeight/1.2));
        margin.rightMargin = 5;
        margin.topMargin = 5;

        constraintLayout.addView(cardView);

        constraintSet.constrainHeight(cardView.getId(), ConstraintSet.WRAP_CONTENT);
        constraintSet.constrainWidth(cardView.getId(), ConstraintSet.WRAP_CONTENT);

        constraintSet.connect(cardView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        constraintSet.connect(cardView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        constraintSet.connect(cardView.getId(), ConstraintSet.TOP, R.id.plus, ConstraintSet.BOTTOM, cardHeight/3);
        constraintSet.applyTo(constraintLayout);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri videoURI = FileProvider.getUriForFile(context,
                        "com.app.picstalgia.fileprovider",
                        videoFile);
                intent.setDataAndType(videoURI,"video/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            }
        });

        retakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMedia(null, null);

                constraintLayout.removeView(cardView);
                boolean delete = videoFile.delete();

                vidText.setVisibility(View.VISIBLE);
                videoButton.setVisibility(View.VISIBLE);
            }
        });


    }

    public void showAudio(File audioFile) {
        String audioPath = audioFile.getPath();
        String[] names = audioPath.split("/");
        String filename = names[names.length - 1];
        setMedia(audioPath, AUDIO_TYPE);

        MaterialButton audioButton = this.activity.findViewById(R.id.audio_btn);
        TextView audioText = this.activity.findViewById(R.id.textView2);

        //hide objects
        audioButton.setVisibility(View.INVISIBLE);
        audioText.setVisibility(View.INVISIBLE);

        ConstraintLayout constraintLayout = (ConstraintLayout) this.activity.findViewById(R.id.picture_media_layout);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        int cardWidth = displayWidth-(displayWidth/4);
        int cardHeight = displayWidth/6;
        CardView cardView = new MaterialCardView(this.activity);
        cardView.setMinimumHeight(cardHeight);
        cardView.setMinimumWidth(cardWidth);
        cardView.setRadius(15);
        cardView.setId(View.generateViewId());
        cardView.setCardBackgroundColor(ColorStateList.valueOf(this.context.getColor(R.color.pale_orange)));

        //button for playing video
        MaterialButton playButton = new MaterialButton(this.activity);
        playButton.setIcon(ContextCompat.getDrawable(this.context, R.drawable.ic_baseline_keyboard_voice_24));
        playButton.setIconTint(ColorStateList.valueOf(this.context.getColor(R.color.light_brown)));
        playButton.setIconSize(cardHeight/2);
        playButton.setStateListAnimator(null);
        playButton.setBackgroundColor(Color.TRANSPARENT);
        playButton.setTextColor(this.context.getColor(R.color.brown));
        playButton.setAllCaps(false);
        playButton.setWidth(150);
        playButton.setHeight(150);
        playButton.setText(filename);
        playButton.setPadding(cardHeight/5,0,(int) (cardHeight/1.2),0);
        playButton.setId(View.generateViewId());
        cardView.addView(playButton);

        //button for retaking video
        ImageButton retakeButton = new ImageButton(this.context);
        retakeButton.bringToFront();
        retakeButton.bringToFront();
        retakeButton.setPadding(10,10,10,10);
        retakeButton.setImageResource(R.drawable.ic_baseline_clear_24);
        retakeButton.setImageTintList(ColorStateList.valueOf(this.context.getColor(R.color.brown)));
        retakeButton.setBackgroundTintList(ColorStateList.valueOf(this.context.getColor(R.color.pale_orange)));
        retakeButton.setId(View.generateViewId());
        cardView.addView(retakeButton);

        constraintLayout.addView(cardView);
        ViewGroup.MarginLayoutParams margin = (ViewGroup.MarginLayoutParams) retakeButton.getLayoutParams();
        margin.leftMargin = (int) (cardWidth-(cardHeight/1.2));
        margin.rightMargin = 5;
        margin.topMargin = 5;

        constraintSet.constrainHeight(cardView.getId(), ConstraintSet.WRAP_CONTENT);
        constraintSet.constrainWidth(cardView.getId(), ConstraintSet.WRAP_CONTENT);

        constraintSet.connect(cardView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        constraintSet.connect(cardView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        constraintSet.connect(cardView.getId(), ConstraintSet.TOP, R.id.plus, ConstraintSet.BOTTOM, cardHeight/3);
        constraintSet.applyTo(constraintLayout);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri audioURI = FileProvider.getUriForFile(context,
                        "com.app.picstalgia.fileprovider",
                        audioFile);
                intent.setDataAndType(audioURI,"audio/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        retakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMedia(null, null);
                constraintLayout.removeView(cardView);
                boolean delete = audioFile.delete();

                audioText.setVisibility(View.VISIBLE);
                audioButton.setVisibility(View.VISIBLE);
            }
        });
    }

    public void setPhotoPath(String path) {
        SharedPreferences sharedPref = this.context.getSharedPreferences("MEDIA_RESOURCE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("photo_path", path);
        editor.apply();
    }

    public void setMedia(String path, String type) {
        SharedPreferences sharedPref = this.context.getSharedPreferences("MEDIA_RESOURCE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("media_path", path);
        editor.putString("media_type", type);
        editor.apply();
    }

    public File getPhotoFile(){
        SharedPreferences sharedPref = this.context.getSharedPreferences("MEDIA_RESOURCE", Context.MODE_PRIVATE);
        String photoPath = sharedPref.getString("photo_path", null);
        if(photoPath != null){
            return new File(photoPath);
        }
        return null;
    }

    public File getMediaFile(){
        SharedPreferences sharedPref = this.context.getSharedPreferences("MEDIA_RESOURCE", Context.MODE_PRIVATE);
        String mediaPath = sharedPref.getString("media_path", null);
        if(mediaPath != null) {
            return new File(mediaPath);
        }
        return null;
    }

    public String getLink() {
        SharedPreferences sharedPref = this.context.getSharedPreferences("MEDIA_RESOURCE", Context.MODE_PRIVATE);
        return sharedPref.getString("media_path", null);
    }

    public String getMediaType(){
        SharedPreferences sharedPref = this.activity.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getString("media_type", null);
    }

}
