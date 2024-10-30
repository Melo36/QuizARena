package com.example.quizarena;

import static java.lang.Thread.sleep;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.ThreadLocalRandom;

public class MockGameplayActivity extends AppCompatActivity {
    private boolean isHost;
    private int player;
    private Punktestand punktestand;
    private boolean[] playersDone;
    private String[] playerAnswers;
    private String currentCorrectAnswer; // TODO Punkte zählen
    private FirebaseDatabase database;
    private DatabaseReference lobbyRef;
    private FirebaseAuth firebaseAuth;
    private String Uid;
    private RequestQueue mQueue;
    private String nickname;
    private boolean alternate;

    private Button aButton;
    private Button bButton;
    private Button cButton;
    private Button dButton;
    private Button startRoundButton;
    private TextView questionText;
    private Button skillButton;
    private TextView skilLView;

    private int playerCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mock_activity);

        database = FirebaseDatabase.getInstance("https://quizarena-83dda-default-rtdb.europe-west1.firebasedatabase.app/");
        firebaseAuth = FirebaseAuth.getInstance();
        Uid = firebaseAuth.getCurrentUser().getUid();
        mQueue = Volley.newRequestQueue(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            lobbyRef = database.getReference("lobbies/" + extras.getString("lobbyName"));
            nickname = extras.getString("nickname");
        }

        questionText = findViewById(R.id.QuestionText);
        aButton = findViewById(R.id.A);
        bButton = findViewById(R.id.B);
        cButton = findViewById(R.id.C);
        dButton = findViewById(R.id.D);
        skilLView = findViewById(R.id.SkillText);
        aButton.setOnClickListener(view -> answerChosen(aButton.getText().toString()));
        bButton.setOnClickListener(view -> answerChosen(bButton.getText().toString()));
        cButton.setOnClickListener(view -> answerChosen(cButton.getText().toString()));
        dButton.setOnClickListener(view -> answerChosen(dButton.getText().toString()));
        skillButton = findViewById(R.id.skillButton);
        skillButton.setOnClickListener(view -> machAuge());
        System.err.println(MainActivity.ErrorPreFix + "Mock Oncreate");
        startRoundButton = findViewById(R.id.startRound);
        startRoundButton.setVisibility(View.VISIBLE);
        startRoundButton.setOnClickListener(view -> startGame()); //TODO HOST MUSS ALS LETZTER STARTGAME KLICKEN!!!
    }

    private void startGame() {
        startRoundButton.setVisibility(View.GONE);
        lobbyRef.child("/Auge").addValueEventListener(new ValueEventListener() {
            boolean firstTime = true;
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(firstTime)
                {
                    firstTime = false;
                } else {
                    String auge = snapshot.getValue().toString();
                    Toast.makeText(MockGameplayActivity.this, auge.substring(1) + " hat Auge gemacht, der ehrenlose", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        lobbyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                while (snapshot.hasChild("/players/player" + (playerCount + 1))) {
                    playerCount++;
                }
                playerCount--;
                System.err.println(MainActivity.ErrorPreFix + "playercount:" + (playerCount + 1));
                if (snapshot.child("/host").getValue().toString().equals(Uid)) {
                    isHost = true;
                    player = 0;
                    playersDone = new boolean[playerCount + 1];
                    playerAnswers = new String[playerCount + 1];
                    for (int i = 1; i <= playerCount; i++) {
                        int finalI = i;
                        lobbyRef.child("/answers/" + i).setValue("");
                        lobbyRef.child("/answers/" + i).addValueEventListener(new ValueEventListener() {
                            final int index = finalI;
                            boolean firstTime = true;

                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                System.err.println(MainActivity.ErrorPreFix + "Data with index " + index + " has changed");
                                playerAnswers[index] = (String) snapshot.getValue();
                                if (firstTime) {
                                    playersDone[index] = false;
                                    firstTime = false;
                                } else {
                                    playersDone[index] = true;
                                    answerReceived();
                                }
                            }

                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }
                    punktestand = new Punktestand(playerCount + 3);
                    getQuestion();
                } else {
                    isHost = false;
                    for (int i = 1; i <= playerCount + 1; i++) {
                        String id = snapshot.child("/players/player" + i).getValue().toString();
                        if (id.equals(Uid)) {
                            player = i - 1;
                        }
                    }
                    System.err.println(MainActivity.ErrorPreFix + "I am player: " + player);
                    lobbyRef.child("/punktestand").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            HashMap<String, Object> hashMap = (HashMap<String, Object>) snapshot.getValue();
                            if (hashMap == null) {
                                System.err.println(MainActivity.ErrorPreFix + "Skipped first Punktestand because null");
                                return;
                            }
                            HashMap<String, Object> question = (HashMap<String, Object>) hashMap.get("nextQuestion");
                            Question question2 = new Question((String) question.get("question"), (String) question.get("a"), (String) question.get("b"), (String) question.get("c"),
                                    (String) question.get("d"), (String) question.get("category"));
                            punktestand = new Punktestand((Long) hashMap.get("roundsLeft"));
                            punktestand.nextQuestion = question2;
                            if (punktestand.roundsLeft < 1) {
                                for (int i = 0; i <= playerCount; i++) {
                                    punktestand.setPunkt(i, (Long) hashMap.get("punkt" + (i + 1)));
                                }
                                gameOver();
                            } else {
                                showQuestion();
                            }
                        }

                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showQuestion() {
        questionText.setText(punktestand.nextQuestion.getQuestion());
        aButton.setText(punktestand.nextQuestion.getA());
        bButton.setText(punktestand.nextQuestion.getB());
        cButton.setText(punktestand.nextQuestion.getC());
        dButton.setText(punktestand.nextQuestion.getD());
        aButton.setVisibility(View.VISIBLE);
        bButton.setVisibility(View.VISIBLE);
        cButton.setVisibility(View.VISIBLE);
        dButton.setVisibility(View.VISIBLE);
        skillButton.setVisibility(View.VISIBLE);
        questionText.setVisibility(View.VISIBLE);
        skilLView.setVisibility(View.VISIBLE);
    }

    private void answerChosen(String answer) {
        System.err.println(MainActivity.ErrorPreFix + "Answer chosen");
        aButton.setVisibility(View.GONE);
        bButton.setVisibility(View.GONE);
        cButton.setVisibility(View.GONE);
        dButton.setVisibility(View.GONE);
        questionText.setVisibility(View.GONE);
        skillButton.setVisibility(View.GONE);
        skilLView.setVisibility(View.GONE);
        if (isHost) {
            playerAnswers[0] = answer;
            playersDone[0] = true;
            answerReceived();
        } else {
            System.err.println(MainActivity.ErrorPreFix + "Sending answer as player " + player);
            lobbyRef.child("/answers/" + player).setValue(answer);
        }
    }

    private void answerReceived() {
        System.err.println(MainActivity.ErrorPreFix + "Answer received");
        System.err.println(MainActivity.ErrorPreFix + Arrays.toString(playersDone));
        for (boolean b : playersDone) {
            if (!b) {
                return;
            }
        }
        punktestand.roundsLeft--;
        Arrays.fill(playersDone, false);
        for(int i = 0; i <= playerCount; i++)
        {
            if(currentCorrectAnswer.equals(playerAnswers[i]))
            {
                punktestand.addPunkt(i, 10);
            }
        }
        System.err.println(MainActivity.ErrorPreFix + "About to get new Question");
        getQuestion();
    }

    private void getQuestion() {
        int category = ThreadLocalRandom.current().nextInt(9, 32 + 1);
        String url = "https://opentdb.com/api.php?amount=1&category=" + category + "&type=multiple";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject jsonObject = response.getJSONArray("results").getJSONObject(0);
                        String question = Html.fromHtml(jsonObject.getString("question"), 0).toString();
                        TextView questionText = findViewById(R.id.QuestionText);
                        questionText.setText(Html.fromHtml(question, 0));

                        String A = currentCorrectAnswer = Html.fromHtml(jsonObject.getString("correct_answer"), 0).toString();


                        JSONArray incAnswers = jsonObject.getJSONArray("incorrect_answers");
                        String B = Html.fromHtml(incAnswers.getString(0), 0).toString();
                        String C = Html.fromHtml(incAnswers.getString(1), 0).toString();
                        String D = Html.fromHtml(incAnswers.getString(2), 0).toString();

                        Hashtable<Integer, String> hash = new Hashtable<>();
                        hash.put(0, A);
                        hash.put(1, B);
                        hash.put(2, C);
                        hash.put(3, D);
                        ArrayList<Integer> list = new ArrayList<>();
                        for (int i = 0; i < 4; i++) {
                            list.add(i);
                        }
                        Collections.shuffle(list);
                        punktestand.nextQuestion = new Question(question, hash.get(list.get(0)), hash.get(list.get(1)),
                                hash.get(list.get(2)), hash.get(list.get(3)), jsonObject.getString("category"));
                        lobbyRef.child("/punktestand").setValue(punktestand);
                        if (punktestand.roundsLeft < 1) {
                            gameOver();
                            return;
                        }
                        showQuestion();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                Throwable::printStackTrace
        );
        mQueue.add(request);
    }

    private void gameOver() {
        long[] punkteArray = new long[playerCount + 1];
        String[] nicknames = new String[playerCount + 1];
        String[] uids = new String[playerCount + 1];
        DatabaseReference nicknameRef = database.getReference("nicknames");
        for (int i = 0; i <= playerCount; i++) {
            punkteArray[i] = punktestand.punkte(i);
        }
        lobbyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (int i = 0; i <= playerCount; i++) {
                    uids[i] = snapshot.child("/players/player" + (i + 1)).getValue().toString();
                }
                nicknameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (int i = 0; i <= playerCount; i++) {
                            HashMap<String, Object> nickMap = (HashMap<String, Object>) snapshot.getValue();
                            nicknames[i] = nickMap.get(uids[i]).toString();
                        }
                        String[] resultString = new String[playerCount+1];
                        for (int i = 0; i <= playerCount; i++) {
                            resultString[i] = nicknames[i] + ": " + punkteArray[i];
                        }
                        for (int n = playerCount; n > 0; n--) {
                            for (int i = 0; i < n; i++) {
                                if(punkteArray[i] < punkteArray[i+1]){
                                    long temp = punkteArray[i];
                                    punkteArray[i] = punkteArray[i+1];
                                    punkteArray[i+1] = temp;
                                    String temp2 = resultString[i];
                                    resultString[i] = resultString[i+1];
                                    resultString[i+1] = temp2;
                                }
                            }
                        }
                        resultString[0] +=  " \uD83D\uDC51";
                        Intent intent = new Intent(MockGameplayActivity.this, GameOverActivity.class);

                        intent.putExtra("resultArray", resultString);
                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void machAuge() {
        char c = alternate ? '0' : '1';
        lobbyRef.child("/Auge").setValue(c + nickname);
        alternate = !alternate;
    }
}
