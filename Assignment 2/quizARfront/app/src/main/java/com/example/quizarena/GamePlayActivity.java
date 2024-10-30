package com.example.quizarena;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnsupportedConfigurationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.net.URL;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class GamePlayActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener
{
    private static final int cameraCode = 432;
    private boolean requestInstall = true;
    private Session session;
    private Thread updateLoop;
    private ArFragment arFragment;
    private Renderable model;
    private View overlay;
    //Die beiden vielleicht an ARUpdate weitergeben?
    private FirebaseDatabase database;
    private DatabaseReference messageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_play);
        layoutCreate();
        checkCamera();
        overlay = findViewById(R.id.overlayContainer);

        getSupportFragmentManager().addFragmentOnAttachListener(this);

        /*
        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }
         */

        System.err.println(MainActivity.ErrorPreFix + "onCreate before loading model");

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        //"haus3.gltf"
        //"https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"
        ModelRenderable.builder().setSource(this, Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(r -> model = r)
                .exceptionally(throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage((throwable.getMessage())).show();
                    System.err.println(MainActivity.ErrorPreFix + "Failed to load model");
                    return null;
                });
    }

    @Override
    protected void onPause() {
        System.err.println(MainActivity.ErrorPreFix + "onpause");

        super.onPause();
        if(session != null)
        {
            updateLoop.interrupt();
            session.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.err.println(MainActivity.ErrorPreFix + "onresume");
        if(ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA_SERVICE) == PERMISSION_DENIED)
            return;
        System.err.println(MainActivity.ErrorPreFix + "After camera permission");
        try {
            if(requestInstall)
            {
                if (ArCoreApk.getInstance().requestInstall(this, true) == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                    requestInstall = false;
                    return;
                }
                requestInstall = false;
            } else {
                ArCoreApk.getInstance().requestInstall(this, false);
            }

            System.err.println(MainActivity.ErrorPreFix + "Before session == 0");

            if(session == null)
            {
                System.err.println(MainActivity.ErrorPreFix + "after session == 0");

                //TODO Check config support
                session = new Session(this);
                updateLoop = new ARUpdate(session);
                Config config = new Config(session);
                CameraConfigFilter filter = new CameraConfigFilter(session);
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    //Maybe needed?
                    filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.REQUIRE_AND_USE));
                    config.setDepthMode(Config.DepthMode.AUTOMATIC);
                }
                //filter.setStereoCameraUsage(EnumSet.of(CameraConfig.StereoCameraUsage.REQUIRE_AND_USE)); Maybe needed?
                filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
                filter.setFacingDirection(CameraConfig.FacingDirection.BACK);
                config.setUpdateMode(Config.UpdateMode.BLOCKING);
                config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
                config.setDepthMode(Config.DepthMode.AUTOMATIC);
                config.setFocusMode(Config.FocusMode.AUTO);
                config.setGeospatialMode(Config.GeospatialMode.DISABLED);
                config.setInstantPlacementMode(Config.InstantPlacementMode.LOCAL_Y_UP);
                config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
                config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
                try {
                    session.setCameraConfig(session.getSupportedCameraConfigs(filter).get(0));
                    session.configure(config);
                } catch (IndexOutOfBoundsException e) {
                    System.err.println(MainActivity.ErrorPreFix + "Camera Configuration unsupported");
                    Intent intent = new Intent(GamePlayActivity.this, FailActivity.class);
                    startActivity(intent);
                } catch (UnsupportedConfigurationException e) {
                    System.err.println(MainActivity.ErrorPreFix + "Device Configuration unsupported");
                    Intent intent = new Intent(GamePlayActivity.this, FailActivity.class);
                    startActivity(intent);
                }
            }
            session.resume();
            updateLoop.start();
        } catch (Exception e) {
            Toast.makeText(GamePlayActivity.this, MainActivity.ErrorPreFix + "Download ARCore pls", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCamera() {
        if(ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA_SERVICE) == PERMISSION_DENIED)
        {
            requestPermissions(new String[] {Manifest.permission.CAMERA}, cameraCode);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case cameraCode:
                if (grantResults.length > 0) {
                    if(grantResults[0] == PERMISSION_DENIED)
                    {
                        System.out.println("Camera permission not granted");
                        Toast.makeText(GamePlayActivity.this, "Camera permission necessary for AR", Toast.LENGTH_SHORT).show();
                        checkCamera();
                    } else onResume();
                }
        }
    }

    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        if (model == null) {
            Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        System.err.println(MainActivity.ErrorPreFix + "Player tapped the plane");

        // Create the Anchor.
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.setParent(anchorNode);
        node.setRenderable(this.model);
        node.select();

        System.err.println(MainActivity.ErrorPreFix + "onTapPlane finished");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(session != null)
            session.close();
        //TODO Firebase Kram vielleicht auch schließen?
        //TODO CloudAnchor löschen wenn Lobby geschlossen wird
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnTapArPlaneListener(this);
        }
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String finalCorrectAnswer;
    private Hashtable<Integer, String> hashtable = new Hashtable<>();

    private Button category1Button;
    private Button category2Button;
    private Button category3Button;

    private Button aButton;
    private Button bButton;
    private Button cButton;
    private Button dButton;

    private TextView questionText;
    private TextView categoryText;

    private void layoutCreate() {
        //Button getQuestionButton = findViewById(R.id.getQuestionButton);
        category1Button = findViewById(R.id.category1);
        category2Button = findViewById(R.id.category2);
        category3Button = findViewById(R.id.category3);

        questionText = findViewById(R.id.QuestionText);
        categoryText = findViewById(R.id.categoryText);

        //getQuestionButton.setOnClickListener(view -> displayQuestion());

        aButton=findViewById(R.id.A);
        bButton=findViewById(R.id.B);
        cButton=findViewById(R.id.C);
        dButton=findViewById(R.id.D);

        aButton.setOnClickListener(view -> answerChosen(aButton,finalCorrectAnswer));
        bButton.setOnClickListener(view -> answerChosen(bButton,finalCorrectAnswer));
        cButton.setOnClickListener(view -> answerChosen(cButton,finalCorrectAnswer));
        dButton.setOnClickListener(view -> answerChosen(dButton,finalCorrectAnswer));

        hashtable.put(9, "General Knowledge");
        hashtable.put(10, "Books");
        hashtable.put(11, "Film");
        hashtable.put(12, "Music");
        hashtable.put(13, "Musicals & Theatres");
        hashtable.put(14, "Television");
        hashtable.put(15, "Video Games");
        hashtable.put(16, "Board Games");
        hashtable.put(17, "Science & Nature");
        hashtable.put(18, "Computers");
        hashtable.put(19, "Mathematics");
        hashtable.put(20, "Mythology");
        hashtable.put(21, "Sports");
        hashtable.put(22, "Geography");
        hashtable.put(23, "History");
        hashtable.put(24, "Politics");
        hashtable.put(25, "Art");
        hashtable.put(26, "Celebrities");
        hashtable.put(27, "Animals");
        hashtable.put(28, "Vehicles");
        hashtable.put(29, "Comic");
        hashtable.put(30, "Gadgets");
        hashtable.put(31, "Anime & Manga");
        hashtable.put(32, "Cartoon & Animations");

        categorySelection();
    }

    private void chooseCategory(int category) {
        category1Button.setVisibility(View.GONE);
        category2Button.setVisibility(View.GONE);
        category3Button.setVisibility(View.GONE);
        categoryText.setVisibility(View.GONE);

        aButton.setVisibility(View.VISIBLE);
        bButton.setVisibility(View.VISIBLE);
        cButton.setVisibility(View.VISIBLE);
        dButton.setVisibility(View.VISIBLE);
        questionText.setVisibility(View.VISIBLE);

        displayQuestion(category);
    }

    private void categorySelection()
    {
        int one = ThreadLocalRandom.current().nextInt(9, 32 + 1);
        int two = ThreadLocalRandom.current().nextInt(9, 32 + 1);
        int three = ThreadLocalRandom.current().nextInt(9, 32 + 1);
        while(one==two||two==three||one==three)
        {
            one = ThreadLocalRandom.current().nextInt(9, 32 + 1);
            two = ThreadLocalRandom.current().nextInt(9, 32 + 1);
            three = ThreadLocalRandom.current().nextInt(9, 32 + 1);
        }
        final int finOne = one;
        final int finTwo = two;
        final int finThree = three;

        category1Button.setText(hashtable.get(one));
        category2Button.setText(hashtable.get(two));
        category3Button.setText(hashtable.get(three));

        category1Button.setOnClickListener(view -> chooseCategory(finOne));
        category2Button.setOnClickListener(view -> chooseCategory(finTwo));
        category3Button.setOnClickListener(view -> chooseCategory(finThree));
    }

    private String getQuestion(int category, String difficulty, String type)  {
        // gets question and answer posibilities from database
        StringBuffer sb = new StringBuffer();
        Thread t = new Thread(() -> {
            try {
                String urlText = "https://opentdb.com/api.php?amount=1&category=" + category + "&difficulty=" + difficulty + "&type=" + type;
                URL url;
                url = new URL(urlText);
                Scanner sc = new Scanner(url.openStream());
                while (sc.hasNext()) {
                    sb.append(sc.next()).append(" ");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String result = sb.toString();
        result = result.replaceAll("<[^>]*>", "");
        return result;
    }

    private void displayQuestion(int category)
    {
        int dif = ThreadLocalRandom.current().nextInt(0, 2);
        String difficulty = dif == 0 ? "medium" : "easy";
        // gets the question and answers and filters the string
        String reply = getQuestion(category,"medium", "multiple");
        System.out.println(reply);
        reply = reply.substring(reply.indexOf("question"));
        reply = reply.replace("\"", "");
        reply = reply.replace("{", "");
        reply = reply.replace("}", "");
        reply = reply.replace("[", "");
        reply = reply.replace("]", "");
        reply = reply.replace(":", ",");
        reply = reply.replace("&#039;","'");
        reply = reply.replace("\\","");
        reply = reply.replace("&amp;"," & ");
        reply = reply.replace("&rsquo;","");
        reply = reply.replace("&eacute;","");
        reply = reply.replace("&ldquo;","\"");
        reply = reply.replace("&rdquo;","\"");
        reply = reply.replace("&ouml;","ö");
        //reply = reply.replace(",", " ");
        reply = reply.replace("&quot;", "\"");
        String[] arr = reply.split(",");

        // questions and possibilities
        String question = "";
        String correctAnswer="";
        String B = "";
        String C = "";
        String D = "";

        // initiates the button texts
        for (int i = 1; i < arr.length; i++) {
            if (arr[i].equals("correct_answer")) {
                correctAnswer = arr[i + 1];
                B = arr[i + 3];
                C = arr[i + 4];
                D = arr[i + 5];
                break;
            }
            question += arr[i];
        }
        finalCorrectAnswer=correctAnswer;

        // sets the texts for buttons
        TextView questionText = findViewById(R.id.QuestionText);
        Button aButton=findViewById(R.id.A);
        Button bButton=findViewById(R.id.B);
        Button cButton=findViewById(R.id.C);
        Button dButton=findViewById(R.id.D);
        aButton.setText(correctAnswer);
        bButton.setText(B);
        cButton.setText(C);
        dButton.setText(D);
        questionText.setText(question);
    }

    private void answerChosen(Button button, String correctAnswer)
    {
        aButton.setVisibility(View.GONE);
        bButton.setVisibility(View.GONE);
        cButton.setVisibility(View.GONE);
        dButton.setVisibility(View.GONE);
        questionText.setVisibility(View.GONE);

        category1Button.setVisibility(View.VISIBLE);
        category2Button.setVisibility(View.VISIBLE);
        category3Button.setVisibility(View.VISIBLE);
        categoryText.setVisibility(View.VISIBLE);

        categorySelection();

        //TODO Logik für richtige Antwort
    }
}