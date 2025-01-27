package com.example.login;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.OkHttpClient;

public class Part4Prob extends AppCompatActivity {
    // countdown
    TextView textViewCountDown;
    private static final long COUNTDOWN_IN_MILLIS = 30000;
    private long timeLeftInMillis;
    CountDownTimer countDownTimer;

    // DB
    private TestDBHelper mTestDBHelper;

    // upload video
    private StorageReference mStorage;
    private ProgressDialog mProgress;
    private OkHttpClient okHttpClient;
    private String idByANDROID_ID;

    // POST
    String getTime;

    //Recording & Playing
    MediaPlayer player;
    MediaRecorder audioRecorder;
    Uri audiouri;
    ParcelFileDescriptor file;

    private String outputFile = null;
    private String outputUri = null;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_part4_prob);

        // 안드로이드폰 ID
        idByANDROID_ID = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        // initialize DB
        mTestDBHelper = new TestDBHelper(Part4Prob.this);

        // 현재 날짜
        long now = System.currentTimeMillis();
        Date mDate = new Date(now);
        SimpleDateFormat simpleDate = new SimpleDateFormat("_yyyyMMdd_hhmmss");
        getTime = simpleDate.format(mDate);

        permissionCheck();

        ImageButton startRecord =findViewById(R.id.start_recording);
        ImageButton stopRecord = findViewById(R.id.stop_recording);
        ImageButton playRecord = findViewById(R.id.play_recording);
        Button next=findViewById(R.id.btn_next);

        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordAudio();
                stopRecord.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), android.R.color.holo_red_dark));
            }
        });

        stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAudio();
                playRecord.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), android.R.color.holo_red_dark));
            }
        });

        playRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file != null) playAudio();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                Intent intent = new Intent(Part4Prob.this, Part5Prob.class);
                startActivity(intent);
                finish();
            }
        });

        Button submit=findViewById(R.id.btn_submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //submitfile();
            }
        });


        // for Countdown
        textViewCountDown = findViewById(R.id.tv_countdown);
        timeLeftInMillis = COUNTDOWN_IN_MILLIS;
        startCountDown();
    }

    private void recordAudio() {

        // firebase에 저장되는 파일이름
        outputUri = "No4_"+idByANDROID_ID+Part1Prob.getTime+"_test.mp3";

        ContentValues values = new ContentValues(4);
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, outputUri);
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3");
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/TOKIC/");

        // 안드로이드 local 경로 ?
        outputFile = "/sdcard/Music/TOKIC/" + outputUri;
        //outputFile = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + outputUri;
        audiouri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

        try {
            file = getContentResolver().openFileDescriptor(audiouri, "w");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (file != null) {
            audioRecorder = new MediaRecorder();
            audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
            audioRecorder.setOutputFile(file.getFileDescriptor());
            audioRecorder.setAudioChannels(1);
            try {
                audioRecorder.prepare();
                audioRecorder.start();
                Toast.makeText(this, "녹음 시작됨.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void stopAudio() {
        if (audioRecorder != null) {
            audioRecorder.stop();
            audioRecorder.release();
            audioRecorder = null;
            Toast.makeText(this, "녹음 중지됨.", Toast.LENGTH_SHORT).show();

        }
    }

    private void playAudio() {
        try {
            closePlayer();

            player = new MediaPlayer();
            player.setDataSource(file.getFileDescriptor());
            player.prepare();
            player.start();

            Toast.makeText(this, "재생 시작됨.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    public void permissionCheck(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void startCountDown(){
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountDownText();
                Toast.makeText(Part4Prob.this, "Time Over", Toast.LENGTH_LONG).show();
                stopAudio();
                Intent intent = new Intent(Part4Prob.this, Part5Prob.class);
                startActivity(intent);
                finish();
            }
        }.start();
    }

    private void updateCountDownText(){
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeFormatted = String.format(Locale.getDefault(),"%02d:%02d", minutes,seconds);

        textViewCountDown.setText(timeFormatted);

        // Start Audio Recording
        if(seconds == 20) {
          //  Toast.makeText(Part4Prob.this, "응답을 시작하세요!", Toast.LENGTH_SHORT).show();
           // recordAudio();
        }
    }

    private void submitfile() {

        Uri file = Uri.fromFile(new File(outputFile));
        StorageReference riversRef = storageRef.child("User/"+idByANDROID_ID+'/'+file.getLastPathSegment());
        UploadTask uploadTask = riversRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(Part4Prob.this, "업로드 실패", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(Part4Prob.this, "업로드 성공", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
