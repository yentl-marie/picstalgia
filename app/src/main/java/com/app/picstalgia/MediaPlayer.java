package com.app.picstalgia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.ExoPlayerFactory;
import androidx.media2.exoplayer.external.source.ExtractorMediaSource;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;


public class MediaPlayer extends AppCompatActivity {
    private String type;
    private String name;
    private String mediaUrl;
    private String imgUrl;
    // creating a variable for exoplayer
    PlayerView playerView;

    private ExoPlayer mPlayer;
    private String mVideoUrl;

    private long mCurrentMillis;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_player_activity);

        ImageButton closeButton = findViewById(R.id.close_btn);

        name = getIntent().getExtras().getString("filename");
        type = name.split("_")[1];

        //get the storage references
        fetchMediaFromDB(name);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent main = new Intent(MediaPlayer.this, MainMenu.class);
                main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(main);

                stop();
            }
        });

    }

    public void fetchMediaFromDB(String name) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("media").document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if(doc != null) {
                        mediaUrl = doc.getString(name);
                        initData();
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        Intent main = new Intent(MediaPlayer.this, MainMenu.class);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(main);

        stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mPlayer != null){
            mPlayer.setPlayWhenReady(true);
            mPlayer.seekTo(mCurrentMillis);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mPlayer != null) {
            mCurrentMillis = mPlayer.getCurrentPosition();

        }
    }

    public void stop() {
        if(mPlayer != null) {
            mPlayer.setPlayWhenReady(false);
            mPlayer.release();
            mPlayer = null;
        }
    }
    public void initData() {
        playerView = findViewById(R.id.exoplayerView);

        if(type.equals("video")) {
            initPlayer();
        } else if(type.equals("audio")) {
            ImageView imageView = findViewById(R.id.image_view);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("images").document(user.getUid())
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if(doc != null) {
                            imgUrl = doc.getString(name);
                            //get image from url
                            Glide.with(MediaPlayer.this)
                                    .load(imgUrl) // image url
                                    .placeholder(R.drawable.ic_baseline_photo_24) // any placeholder to load at start
                                    .error(R.drawable.ic_baseline_warning_24)  // any image in case of error
                                    .centerCrop()
                                    .into(imageView);

                            initPlayer();
                        }
                    }
                }
            });



        }
    }

    public void playUrl() {
        String link = mediaUrl;
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            link = "http://" + link;
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        startActivity(browserIntent);
    }

    public void initPlayer() {
        if (mPlayer != null) {
            // no need to continue creating the player, if it's probably there
            return;
        }
        // set default options for the player
        mPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(mPlayer);
//        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
//        mPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

        DefaultDataSource.Factory dataSourceFactory =
                new DefaultDataSource.Factory(this);
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mediaUrl));
        mPlayer.setMediaSource(mediaSource);
        mPlayer.prepare();

        // now is the more important part. here we check to see if we want to resume, or start from the beginning
        mPlayer.setPlayWhenReady(true);

    }

}