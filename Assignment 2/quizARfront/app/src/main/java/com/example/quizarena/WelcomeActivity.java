package com.example.quizarena;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase database;
    private DatabaseReference nicknamesRef;
    private String nickname;

    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);



        // Colour the AR in "Quiz Arena"
        TextView textView = findViewById(R.id.textView);
        String text = "QUIZ ARENA";

        SpannableString ss = new SpannableString(text);
        ForegroundColorSpan fcsRed = new ForegroundColorSpan(Color.RED);

        ss.setSpan(fcsRed, 5,7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);

        // Get a firebaseAuth instance
        firebaseAuth = FirebaseAuth.getInstance();

        database = FirebaseDatabase.getInstance("https://quizarena-83dda-default-rtdb.europe-west1.firebasedatabase.app/");
        // database = FirebaseDatabase.getInstance("https://quizarena-83dda-default-rtdb.europe-west1.firebasedatabase.app/%22);

        addUserNicknameToWelcomeText();

        // Get buttons
        Button logoutButton = findViewById(R.id.logoutButton);
        Button joinLobbyButton = findViewById(R.id.joinLobbyButton);
        Button optionsButton = findViewById(R.id.optionsButton);
        //Button multiplayerTestButton = findViewById(R.id.multiplayerTestButton);

        logoutButton.setOnClickListener(view -> logoutUser());
        joinLobbyButton.setOnClickListener(view -> switchToJoinLobbyActivity());
        optionsButton.setOnClickListener(view -> switchToOptionsMenu());
        //multiplayerTestButton.setOnClickListener(view -> switchToMulitplayerTestActivity());

    }

    // this is how you get the data from te database
    private void addUserNicknameToWelcomeText() {
        // Get welcome textview
        TextView welcomeText = findViewById(R.id.welcomeText);

        String uid = firebaseAuth.getUid();
        nicknamesRef = database.getReference("nicknames/" + uid);

        nicknamesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                nickname = dataSnapshot.getValue(String.class);
                welcomeText.setText(welcomeText.getText() + " " + nickname);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // error - retry
            }
        });
    }

    private void switchToOptionsMenu() {
        Intent intent = new Intent(WelcomeActivity.this, OptionsActivity.class);
        startActivity(intent);
    }

    private void logoutUser() {
        // show user a dialog, that he is about to log out and make him confirm this action
        AlertDialog alertDialog = new AlertDialog.Builder(WelcomeActivity.this).create();
        alertDialog.setTitle("Logout");
        alertDialog.setMessage("Do you want to logout?");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Logout", (dialogInterface, i) -> {
            firebaseAuth.signOut();
            dialogInterface.dismiss();
            // and then switch back to main activity
            switchToMainActivity();
        });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        alertDialog.show();
    }

    private void switchToMainActivity() {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void switchToJoinLobbyActivity() {
        Intent intent = new Intent(WelcomeActivity.this, joinLobbyActivity.class);
        intent.putExtra("nickname", nickname);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        logoutUser();
    }
}