package com.example.quizarena;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// MP_Functionality = Main3Activity in tutorial
//TODO: Klasse löschen (war nur zum testen)
public class MP_Functionality extends AppCompatActivity {

    Button button;
    String playerName = "";
    String lobbyName = "";
    String role = "";
    String message = "";

    FirebaseDatabase database;
    DatabaseReference messageRef;

    FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp_functionality);

        firebaseAuth.getInstance().getUid();


        button = findViewById(R.id.button);
        button.setEnabled(false);

        database = FirebaseDatabase.getInstance("https://quizarena-83dda-default-rtdb.europe-west1.firebasedatabase.app/");
        SharedPreferences preferences = getSharedPreferences("PREFS", 0);
        playerName = preferences.getString("playerName", "");

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            lobbyName = extras.getString("lobbyName");
            if(lobbyName.equals(firebaseAuth.getInstance().getUid())) {
                role = "host";
            } else {
                role = "guest";
            }



        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send message
                button.setEnabled(false);
                message = role + ":Poked!";
                messageRef.setValue(message);
            }
        });

        // listen for incoming messages
        messageRef = database.getReference("lobbies/" + lobbyName + "/message");
        message = role + ":Poked!";
        messageRef.setValue(message);
        addLobbyEventListener();
    }


    private void addLobbyEventListener() {
        messageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // message recieved
                if(role.equals("host")) {
                    if(dataSnapshot.getValue(String.class).contains("guest:")) {
                        button.setEnabled(true);
                        Toast.makeText(MP_Functionality.this, "" + dataSnapshot.getValue(String.class).replace("guest", ""), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if(dataSnapshot.getValue(String.class).contains("host:")) {
                        button.setEnabled(true);
                        Toast.makeText(MP_Functionality.this, "" + dataSnapshot.getValue(String.class).replace("host", ""), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // error - retry
                messageRef.setValue(message);
            }
        });
    }

}