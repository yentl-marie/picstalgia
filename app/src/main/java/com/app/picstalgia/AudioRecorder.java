package com.app.picstalgia;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.view.View;

import java.io.IOException;

import pl.droidsonroids.gif.GifImageView;

public class AudioRecorder extends AppCompatActivity {
    public final String APP_TAG = "Picstalgia";
    private MaterialButton record, close, stop;
    private MediaRecorder myAudioRecorder;
    private String outputFile;
    private boolean hasFinishedRecording = false;
    private boolean isPaused = false;
    private boolean isMicAvailable = true;
    private int timePaused = 0;
    long startTime = 0;
    double totalTime;

    TextView timerTextView;
    Handler timerHandler;
    Runnable timerRunnable;
    ProgressBar progressBar;
    MediaPlayer mediaPlayer;
    ImageView displayPic;
    GifImageView displayGif;

    AudioManager audioManager;
    AudioAttributes playbackAttributes;
    AudioFocusRequest focusRequest;

    // media player is handled according to the
    // change in the focus which Android system grants for
    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                mediaPlayer.start();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                mediaPlayer.pause();
                mediaPlayer.seekTo(0);
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                mediaPlayer.release();
            }
        }
    };

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        timerHandler.removeCallbacksAndMessages(null);
        //remove mediaPlayer and audioFocus
        if(audioManager != null) {
            final int audioFocusRequest = audioManager.abandonAudioFocusRequest(focusRequest);
            if (audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }

        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_recorder_activity);

        timerTextView = findViewById(R.id.timer);
        progressBar = findViewById((R.id.progress_bar));
        record = findViewById(R.id.audio_record_btn);
        close = findViewById(R.id.close_recorder_btn);
        stop = findViewById(R.id.stop_record_btn);

        //get the audio path
        outputFile = getIntent().getStringExtra("OUTPUT_FILE");
        //setup the media recorer;
        mediaRecorderReady();


        //runs without a timer by reposting this handler at the end of the runnable
        timerHandler = new Handler();
        timerRunnable = new Runnable() {

            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                timerHandler.postDelayed(this, 10);

                timerTextView.setText(String.format("0%d:%02d", minutes, seconds));

                totalTime = (minutes*60)+seconds;
                double progress = (totalTime / 90) * 100;
                progressBar.setProgress(Math.toIntExact(Math.round(progress)));

                //time limit
                if(minutes == 1 && seconds == 30){
                    stopRecording();
                }
            }
        };

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                //remove mediaPlayer and audioFocus
                if(audioManager != null) {
                    final int audioFocusRequest = audioManager.abandonAudioFocusRequest(focusRequest);
                    if (audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                }
                finish();
            }
        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasFinishedRecording){
                    //change display pic
                    displayPic = findViewById(R.id.display_pic);
                    displayGif = findViewById(R.id.display_gif);

                    displayPic.setVisibility(View.INVISIBLE);
                    displayGif.setVisibility(View.VISIBLE);

                    //start recording
                    try {
                        myAudioRecorder.prepare();
                        myAudioRecorder.start();
                    } catch (IllegalStateException ise) {
                        // make something ...
                        System.out.println(ise);
                    } catch (IOException ioe) {
                        // make something
                    } catch (Exception exception) {
                        isMicAvailable = false;
                    }

                    if(isMicAvailable){
                        //hide record button
                        record.setVisibility(View.INVISIBLE);

                        //start timer
                        startTime = System.currentTimeMillis();
                        timerHandler.postDelayed(timerRunnable, 0);

                        //show stop button
                        stop.setVisibility(View.VISIBLE);
                        stop.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                displayPic.setVisibility(View.VISIBLE);
                                displayGif.setVisibility(View.INVISIBLE);

                                stopRecording();
                            }
                        });
                    }

                }else{
                    // finished recording and user played the audio
                    // get the audio system service for
                    // the audioManger instance
                    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                    // initiate the audio playback attributes
                    playbackAttributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();

                    // set the playback attributes for the focus requester
                    focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(playbackAttributes)
                            .setAcceptsDelayedFocusGain(true)
                            .setOnAudioFocusChangeListener(audioFocusChangeListener)
                            .build();

                    // request the audio focus and
                    // store it in the int variable
                    final int audioFocusRequest = audioManager.requestAudioFocus(focusRequest);

                    if (audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        audioPlayer();
                    }

                }
            }
        });
    }

    // ============================================RECORDER METHODS====================================

    public void mediaRecorderReady(){
        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        myAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        myAudioRecorder.setOutputFile(outputFile);
    }

    public void stopRecording(){
        if(myAudioRecorder != null){
            myAudioRecorder.release();
            myAudioRecorder = null;
        }

        //stop timer
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.removeCallbacksAndMessages(null);
        timerHandler = null;
        timerRunnable = null;

        hasFinishedRecording = true;

        // --------------PREPS FOR AUDIO PLAYER------------
        initMediaPlayer();

        //reset view
        playView();

        //reset timer
        timerTextView = findViewById(R.id.timer);
        timerTextView.setText("00:00");
        progressBar.setProgress(0);

        //check button
        MaterialButton check = findViewById(R.id.accept_audio_btn);
        check.setVisibility(View.VISIBLE);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                //remove mediaPlayer and audioFocus
                if(audioManager != null) {
                    final int audioFocusRequest = audioManager.abandonAudioFocusRequest(focusRequest);
                    if (audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                }
                finish();
            }
        });

        //set the progress bar for the audio player
        int millis = mediaPlayer.getDuration() / 1000;
        int totalSec = (int) millis % 60;
        int totalMin = (int) millis / 60;

        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if(!isPaused){
                    long millis = System.currentTimeMillis() - startTime;
                    int seconds = (int) (millis / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    timerHandler.postDelayed(this, 10);

                    timerTextView.setText(String.format("0%d:%02d/0%d:%02d", minutes, seconds, totalMin, totalSec));

                    double currTime = (minutes*60+seconds);
                    double progress = (currTime/totalTime) * 100;
                    progressBar.setProgress(Math.toIntExact(Math.round(progress)));
                }
            }
        };
    }

    // ============================================PLAYER METHODS====================================

    public void audioPlayer(){
        //change display picture
        displayGif.setImageResource(R.drawable.player);
        displayPic.setVisibility(View.INVISIBLE);
        displayGif.setVisibility(View.VISIBLE);

        // record button changes its function to player
        isPaused = false;
        mediaPlayer.seekTo(timePaused);
        mediaPlayer.start();

        //start progress bar
        startTime = System.currentTimeMillis() - mediaPlayer.getCurrentPosition();
        progressBar.setProgress(0);
        timerHandler.postDelayed(timerRunnable, 0);

        //reset view
        pauseView();
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.pause();
                timePaused = mediaPlayer.getCurrentPosition();
                isPaused = true;
                //change display picture
                displayPic.setImageResource(R.drawable.player_stop);
                displayPic.setVisibility(View.VISIBLE);
                displayGif.setVisibility(View.INVISIBLE);

                //change button
                playView();
            }
        });
    }

    private void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlayer();
                displayPic.setVisibility(View.VISIBLE);
                displayGif.setVisibility(View.INVISIBLE);
            }

        });

        try {
            mediaPlayer.setDataSource(outputFile);
            mediaPlayer.prepare();

        } catch (Exception e) {
            // make something
        }
    }

    private void pauseView(){
        record.setVisibility(View.INVISIBLE);
        stop.setVisibility(View.VISIBLE);
        stop.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_baseline_pause_24));
    }

    private void playView(){
        stop.setVisibility(View.INVISIBLE);
        record.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_baseline_play_arrow_24));
        record.setVisibility(View.VISIBLE);
    }


    private void stopPlayer() {
        timerHandler.removeCallbacks(timerRunnable);
        timePaused = 0;

        //reset view
        playView();
    }
}