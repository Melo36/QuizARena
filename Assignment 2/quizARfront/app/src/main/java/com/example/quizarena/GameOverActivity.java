package com.example.quizarena;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class GameOverActivity extends AppCompatActivity {

    private FirebaseDatabase database;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference nickNameRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://quizarena-83dda-default-rtdb.europe-west1.firebasedatabase.app/");
        nickNameRef = database.getReference("nicknames/");
        Intent intent = getIntent();
        String[] resultArray = intent.getStringArrayExtra("resultArray");
        List<String> resultList = Arrays.asList(resultArray);
        ListView listView = findViewById(R.id.listView);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(GameOverActivity.this, R.layout.list_item_text, resultList);
        listView.setAdapter(adapter);

    }



}