package com.example.quizarena;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;

public class OptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        // Volume Slider
        final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        SeekBar volControl = (SeekBar) findViewById(R.id.seekBarVolume);
        volControl.setMax(maxVolume);
        volControl.setProgress(curVolume);
        volControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
            }
        });

        Button musicOnOffButton = findViewById(R.id.musicOnOffButton);
        musicOnOffButton.setOnClickListener(view -> toggleMusic());

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> super.onBackPressed());

    }


    private void toggleMusic() {

        Button musicOnOffButton = findViewById(R.id.musicOnOffButton);
        if (MainActivity.playMusic) {
            MainActivity.playMusic = false;
            // pause Music -> set the static MediaPlayer from WelcomeActivity to false
            MainActivity.mediaPlayer.pause();
            musicOnOffButton.setText("Music: Off");

        } else {
            MainActivity.playMusic = true;
            MainActivity.mediaPlayer.start();
            // pause Music -> set the static MediaPlayer from WelcomeActivity to true
            musicOnOffButton.setText("Music: On");
        }


    }


}