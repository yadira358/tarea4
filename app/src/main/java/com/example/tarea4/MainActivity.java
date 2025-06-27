package com.example.videocaptureapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;

    private VideoView videoView;
    private Button buttonRecordVideo;
    private Button buttonSave; // For SQLite or confirming save
    private Uri currentVideoUri; // To hold the URI of the captured video

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        buttonRecordVideo = findViewById(R.id.buttonRecordVideo);
        buttonSave = findViewById(R.id.buttonSave);

        // Add MediaController for video playback controls
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        buttonRecordVideo.setOnClickListener(v -> checkPermissionsAndRecordVideo());
        buttonSave.setOnClickListener(v -> saveVideoMetadataToSqlite()); // Placeholder for SQLite

        // Initial permission check (you might do this on app start or before an action)
        checkPermissions();
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE // Will be ignored on API 29+ if using MediaStore correctly
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void checkPermissionsAndRecordVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)) { // For API 29+, WRITE_EXTERNAL_STORAGE might not be needed for MediaStore
            dispatchTakeVideoIntent();
        } else {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, proceed with recording
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                // You might automatically start recording here or just enable the button
            } else {
                Toast.makeText(this, "Permissions denied. Cannot record video.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the video should go
            File videoFile = null;
            try {
                videoFile = createVideoFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Error creating video file", Toast.LENGTH_SHORT).show();
                return;
            }
            // Continue only if the File was successfully created
            if (videoFile != null) {
                Uri videoUri = FileProvider.getUriForFile(this,
                        "com.example.videocaptureapp.fileprovider", // Replace with your package name and fileprovider authority
                        videoFile);
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                currentVideoUri = videoUri; // Store for later use
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private File createVideoFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoFileName = "MP4_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES); // App-specific storage
        // Or getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) for public storage (requires WRITE_EXTERNAL_STORAGE on older APIs)
        File video = File.createTempFile(
                videoFileName,  /* prefix */
                ".mp4",         /* suffix */
                storageDir      /* directory */
        );
        return video;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            // The video is already saved at the URI we provided.
            // data.getData() might be null or contain a content URI if the camera app handles saving itself.
            // We'll use our pre-created `currentVideoUri`

            if (currentVideoUri != null) {
                videoView.setVideoURI(currentVideoUri);
                videoView.start();
                Toast.makeText(this, "Video captured and loaded: " + currentVideoUri.getPath(), Toast.LENGTH_LONG).show();

                // If you want to make it visible in gallery immediately, you might need to scan it
                // MediaScannerConnection.scanFile(this, new String[]{currentVideoUri.getPath()}, null, null);
            } else if (data != null && data.getData() != null) {
                // Some camera apps return a URI in data.getData()
                Uri videoFromCameraApp = data.getData();
                videoView.setVideoURI(videoFromCameraApp);
                videoView.start();
                currentVideoUri = videoFromCameraApp; // Update current URI
                Toast.makeText(this, "Video captured and loaded (from data): " + videoFromCameraApp.getPath(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Video recording cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveVideoMetadataToSqlite() {
        // This is where you would implement the SQLite logic for the extra point.
        // You'll need to create a SQLiteOpenHelper class, a database, and a table.
        // Then, insert details like currentVideoUri.toString() (or its path),
        // a timestamp, etc., into your database.
        if (currentVideoUri != null) {
            // Example:
            // MyDatabaseHelper dbHelper = new MyDatabaseHelper(this);
            // SQLiteDatabase db = dbHelper.getWritableDatabase();
            // ContentValues values = new ContentValues();
            // values.put("video_path", currentVideoUri.toString());
            // values.put("timestamp", System.currentTimeMillis());
            // long newRowId = db.insert("videos_table", null, values);
            // db.close();
            Toast.makeText(this, "Simulating save to SQLite for video: " + currentVideoUri.toString(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No video to save to SQLite.", Toast.LENGTH_SHORT).show();
        }
    }

    // You will need a FileProvider setup in AndroidManifest.xml and a file_paths.xml
    // For example:
    /*
    In AndroidManifest.xml inside <application> tag:
    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="com.example.videocaptureapp.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths"/>
    </provider>
    */

    /*
    Create res/xml/file_paths.xml:
    <?xml version="1.0" encoding="utf-8"?>
    <paths>
        <external-path name="my_videos" path="Android/data/com.example.videocaptureapp/files/Movies" />
    </paths>
    */
}