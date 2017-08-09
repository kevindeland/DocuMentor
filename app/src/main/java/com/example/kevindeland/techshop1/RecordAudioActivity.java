package com.example.kevindeland.techshop1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by kevindeland on 6/21/17.
 */

public class RecordAudioActivity extends AppCompatActivity {

    private final String TAG = "MONTY";

    // data from past activities
    private String studentName;
    private byte[] imageByteArray;
    private String questionId;

    // Audio information
    private enum RecordingState  {
        TYPE_UNDEFINED, READY, IS_RECORDING, HAS_RECORDING, IS_PLAYING
    }

    private RecordingState recordingState = RecordingState.TYPE_UNDEFINED;

    private Button recordButton;
    private Button playButton;
    private Button saveButton;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String audioFileName = null;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    // requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio);

        audioFileName = getExternalCacheDir().getAbsolutePath();
        audioFileName += "/audiorecordtest.3gp";




        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        // Display image
        Bundle extras = getIntent().getExtras();
        imageByteArray = extras.getByteArray("image");
        studentName = extras.getString("studentName");

        Log.d(TAG, "student name = " + studentName);
        Log.d(TAG, "byteArray = " + imageByteArray.length);

        Bitmap bpm = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
        ImageView image = (ImageView) findViewById(R.id.imageView);

        image.setImageBitmap(bpm);

        // initialize buttons
        recordButton = (Button) findViewById(R.id.recordAndStop);
        recordButton.setEnabled(false);

        playButton = (Button) findViewById(R.id.play);
        playButton.setEnabled(false);

        saveButton = (Button) findViewById(R.id.save);
        saveButton.setEnabled(false);

    }


    protected void selectButton(View view) {
        Button button = (Button) view;

        Log.d(TAG, "button clicked: " + button.getText());

        questionId = ""+view.getTag();


        recordingState = RecordingState.READY;
        recordButton.setEnabled(true);

    }

    protected void clickRecordButton(View view) {

        switch(recordingState) {
            case TYPE_UNDEFINED:
                break; // impossible
            case READY:
                Log.d(TAG, "Record button clicked");
                recordingState = RecordingState.IS_RECORDING;
                recordButton.setText(R.string.stopRecord);
                // begin recording
                startRecording();

                break;
            case IS_RECORDING:
                Log.d(TAG, "StopRecord button clicked");
                recordingState = RecordingState.HAS_RECORDING;
                recordButton.setText(R.string.recordAudio);
                playButton.setEnabled(true);
                saveButton.setEnabled(true);
                // stop recording
                stopRecording();

                break;
            case HAS_RECORDING:
                Log.d(TAG, "Record button clicked");
                recordingState = RecordingState.IS_RECORDING;
                recordButton.setText(R.string.stopRecord);
                playButton.setEnabled(false);
                saveButton.setEnabled(false);
                // begin re-recording
                startRecording();

                break;
            case IS_PLAYING:
                break; // impossible
        }

    }

    protected void clickPlayButton(View view) {

        switch(recordingState) {
            case TYPE_UNDEFINED:
                break; // impossible
            case READY:
                break; // impossible
            case IS_RECORDING:
                break; // impossible
            case HAS_RECORDING:
                Log.d(TAG, "Play button clicked");
                recordingState = RecordingState.IS_PLAYING;
                playButton.setText(R.string.stopPlaying);
                recordButton.setEnabled(false);
                saveButton.setEnabled(false);
                // play recording
                startPlaying();

                break;
            case IS_PLAYING:
                Log.d(TAG, "Stop button clicked");
                recordingState = RecordingState.HAS_RECORDING;
                playButton.setText(R.string.playAudio);
                recordButton.setEnabled(true);
                saveButton.setEnabled(true);
                // stop playing
                stopPlaying();

                break;
        }
    }

    protected void clickSaveButton(View view) {

        switch(recordingState) {
            case TYPE_UNDEFINED:
                break; // impossible
            case READY:
                break; // impossible
            case IS_RECORDING:
                break; // impossible
            case HAS_RECORDING:
                saveThings();
                break;
            case IS_PLAYING:
                break; // impossible
        }
    }

    /**
     * Start recording
     */
    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(audioFileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();

    }

    /**
     * Stop Recording
     */
    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
    }

    /**
     * Start Playing
     */
    private void startPlaying() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFileName);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop Playing
     */
    private void stopPlaying() {
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void saveThings() {

        long saveId = System.currentTimeMillis() % 100000;

        String imageFilename = studentName + "_img_" + questionId + "_" + saveId + ".jpg";
        File imageFile = new File(this.getExternalFilesDir(null), imageFilename);
        FileOutputStream iFileOutput = null;
        try {
            iFileOutput = new FileOutputStream(imageFile.getPath());
            iFileOutput.write(imageByteArray);
            iFileOutput.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO save audio recording
        File src = new File(audioFileName);
        String aFilename = studentName + "_aud_" + questionId + "_" + saveId + ".3gp";
        File dst = new File(this.getExternalFilesDir(null), aFilename);

        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        openNewScreen();

    }

    private void openNewScreen() {
        Intent intent = new Intent(getApplicationContext(), ThankYouActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
