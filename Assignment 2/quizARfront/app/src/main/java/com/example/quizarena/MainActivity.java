package com.example.quizarena;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.ArCoreApk;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    //TODO Vor jede Fehlermeldung, die wir auf der Konsole ausgeben hinzufügen
    //Beispiel in Zeile 48
    private static final String TAG = "MainActivity";
    public static final String ErrorPreFix = "QuizARena: ";
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase database;
    private DatabaseReference nicknamesRef;

    static MediaPlayer mediaPlayer;
    static boolean playMusic;

    public final static boolean TestingARCore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call superclass onCreate method
        super.onCreate(savedInstanceState);

        // Set the activity content from a layout source
        setContentView(R.layout.activity_main);

        // Game music
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 70, 0);
        mediaPlayer = MediaPlayer.create(this, R.raw.gamemusic);
        mediaPlayer.setLooping(true);
        //mediaPlayer.start();
        playMusic = true;

        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        while (availability.isTransient()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex)
            {
                System.err.println(ErrorPreFix + "Thread interrupted while waiting for ARCore to check compatibility");
            }
        }
        if (!availability.isSupported()) {
            switchToFailActivity();
        }
        // Get reference to database
        database = FirebaseDatabase.getInstance("https://quizarena-83dda-default-rtdb.europe-west1.firebasedatabase.app/");

        // Get a firebaseAuth instance
        firebaseAuth = FirebaseAuth.getInstance();

        // Get the current user from firebase
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            firebaseAuth.signOut(); // log the user out
        }

        Button questionsButton = findViewById(R.id.goToQuestionsButton);
        questionsButton.setOnClickListener(view -> switchToQuestionsActivity());

        // If the user is not logged in
        // Get button via id
        Button loginButton = findViewById(R.id.loginButton);
        Button registerButton = findViewById(R.id.registerButton);

        // Get EditText field values
        final EditText email = findViewById(R.id.emailLogin);
        final EditText password = findViewById(R.id.passwordLogin);

        // Send login information to firebase
        loginButton.setOnClickListener(view -> loginUser(email.getText().toString(), password.getText().toString()));

        // Switch to the register activity
        registerButton.setOnClickListener(view -> switchToRegisterActivity());
    }

    private void loginUser(String email, String password) {
        if(TestingARCore) {
            Intent intent = new Intent(MainActivity.this, GamePlayActivity.class);
            startActivity(intent);
            return;
        }
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, ErrorPreFix + "loginUser: User successfully logged in!");

                String uid = firebaseAuth.getUid();
                nicknamesRef = database.getReference("nicknames/" + uid);

                nicknamesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String nickname = dataSnapshot.getValue(String.class);
                        System.out.println("\n\n");
                        System.out.println(ErrorPreFix + "Nickname " + nickname);

                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // error - retry
                    }
                });
                switchToWelcomeActivity();
            } else {
                Log.d(TAG, ErrorPreFix + "loginUser: Failed to login!", task.getException());
                Toast.makeText(MainActivity.this, "Login failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchToWelcomeActivity() {
        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
        startActivity(intent);
    }

    private void switchToRegisterActivity() {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    private void switchToFailActivity() {
        Intent intent = new Intent(MainActivity.this, FailActivity.class);
        startActivity(intent);
    }
    
    private void switchToQuestionsActivity() {
        Intent intent = new Intent(MainActivity.this, MockGameplayActivity.class);
        startActivity(intent);
    }

}

