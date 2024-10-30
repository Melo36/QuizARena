package com.example.quizarena;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    // tag that represents current activity
    private static final String TAG = "RegisterActivity";
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase database;
    private DatabaseReference nicknameRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Get Button via id (which we defined in activity_main.xml file)
        Button backToLoginButton = findViewById(R.id.backToLoginButton);
        Button registerButton = findViewById(R.id.registerButton);


        // get instance of Realtime database
        database = FirebaseDatabase.getInstance("https://quizarena-83dda-default-rtdb.europe-west1.firebasedatabase.app/");


        // Get a firebaseAuth instance
        firebaseAuth = FirebaseAuth.getInstance(); // this instance represents the current connection from the current user to the firebase backend

        // we cant save the nickname in firebase (we wil save it in mongodb)
        // get the user info from the EditText fields via id
        final EditText nickname = findViewById(R.id.nickname);
        final EditText email = findViewById(R.id.emailRegister);
        final EditText password = findViewById(R.id.passwordRegister);

        // Switch to the register activity
        backToLoginButton.setOnClickListener(view -> switchToMainActivity());

        registerButton.setOnClickListener(view -> registerUser(email.getText().toString(), password.getText().toString(), nickname.getText().toString()));

    }

    private void registerUser(String email, String password, String nickname) {
        // Register the user with firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()) {
                Log.d(TAG, "registerUser: User successfully registered in firebase.");
                //registerUserInSpringBackend(firebaseAuth.getCurrentUser().getUid(), nickname);
                addNicknameToFirebaseRTDB(firebaseAuth.getCurrentUser().getUid(), nickname);

                Toast.makeText(RegisterActivity.this, "Registration successfull!", Toast.LENGTH_SHORT).show();

            } else {
                Log.d(TAG, "registerUser: Failed to register user", task.getException());
                // if it failed -> we need to let user know that the regristration failed
                Toast.makeText(RegisterActivity.this, "Registration failed!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void addNicknameToFirebaseRTDB(String firebaseUID, String nickname) {
        // write nickname to friebase realtime database -> to use it later for lobbyName creation / specify who is host etc.
        nicknameRef = database.getReference("nicknames");
        nicknameRef.child(firebaseUID).setValue(nickname);
    }

    private void switchToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void switchToWelcomeActivity() {
        Intent intent = new Intent(RegisterActivity.this, WelcomeActivity.class);
        startActivity(intent);
    }

}