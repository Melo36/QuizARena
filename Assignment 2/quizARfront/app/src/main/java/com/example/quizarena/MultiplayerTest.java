package com.example.quizarena;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.data.DataBufferSafeParcelable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MultiplayerTest extends AppCompatActivity {

    ListView listView;
    Button button;

    List<String> lobbyList;

    String lobbyName = "";
    String name_of_lobby = "";

    FirebaseDatabase database;
    DatabaseReference lobbyRef;
    DatabaseReference lobbiesRef;
    FirebaseAuth firebaseAuth;

    // TODO: delete this class

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_test);

        database = FirebaseDatabase.getInstance("https://quizarena-83dda-default-rtdb.europe-west1.firebasedatabase.app/");

        firebaseAuth = FirebaseAuth.getInstance();

        // Get the current user from firebase
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        listView = findViewById(R.id.listView);
        button = findViewById(R.id.createLobbyButton);

        // all existing available rooms
        lobbyList = new ArrayList<>();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create lobby and add yourself as player1
                button.setText("CREATING LOBBY");
                button.setEnabled(false);
                createLobby();

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // join an existing lobby and add yourself as player2
                lobbyName = lobbyList.get(position);
                lobbyRef = database.getReference("lobbies/" + lobbyName);
                //lobbyRef = database.getReference("lobbies/" + lobbyName + "/player2");
                lobbyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int pos = 2;
                        while (pos < 5) {
                            if (snapshot.hasChild("/players/player" + pos)) {
                                pos++;
                            } else {
                                addLobbyEventListener();
                                lobbyRef = database.getReference("lobbies/" + lobbyName + "/players/player" + pos);
                                lobbyRef.setValue(firebaseAuth.getCurrentUser().getUid());
                                break;
                            }
                        }
                        if(pos == 5){
                            Toast.makeText(MultiplayerTest.this, "Lobby already full!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


            }
        });

        // show if new lobby is available
        addLobbiesEventListener();
    }


    // hier wird die LObby kreiert
    private void addLobbyEventListener() {
        lobbyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // join the lobby
                button.setText("CREATE LOBBY");
                button.setEnabled(true);
                Intent intent = new Intent(getApplicationContext(), MP_Functionality.class);
                intent.putExtra("lobbyName", lobbyName); // to determine who is host in GamePlayActivity
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // error
                button.setText("CREATE LOBBY");
                button.setEnabled(true);
                Toast.makeText(MultiplayerTest.this, "Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // hier werden die Lobbies angezeigt in der Liste
    // A DataSnapshot instance contains data from a Firebase Database location. Any time you read Database data, you receive the data as a DataSnapshot.
    private void addLobbiesEventListener() {
        lobbiesRef = database.getReference("lobbies");
        lobbiesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // show list of lobbies
                lobbyList.clear();
                Iterable<DataSnapshot> lobbies = dataSnapshot.getChildren();
                for (DataSnapshot snapshot : lobbies) {
                    lobbyList.add(snapshot.getKey());

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MultiplayerTest.this, android.R.layout.simple_list_item_1, lobbyList);
                    listView.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // error - nothing
            }
        });
    }

    private String createLobby() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Name of lobby:");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        alertDialog.setView(input);

        // Set up the buttons
        alertDialog.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //name_of_lobby = input.getText().toString();
                lobbyName = input.getText().toString();

                // set this user to the host -> as this user created the lobby
                DatabaseReference hostRef = database.getReference("lobbies/" + lobbyName + "/host");
                hostRef.setValue(firebaseAuth.getUid()); // set  host to current firebaseUID

                lobbyRef = database.getReference("lobbies/" + lobbyName + "/players/player1");
                addLobbyEventListener();
                lobbyRef.setValue(firebaseAuth.getUid());

            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
        return name_of_lobby;

    }

}








