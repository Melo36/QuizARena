package com.example.quizarena;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ARUpdate extends Thread {
    private final Session session;
    private FirebaseDatabase database;
    private DatabaseReference messageRef;
    private int counter = 0;

    public ARUpdate(Session s) {
        session = s;
    }

    public ARUpdate(Session s, FirebaseDatabase f, DatabaseReference r) {
        session = s;
        database = f;
        messageRef = r;
    }


    @Override
    public void run() {
        System.err.println(MainActivity.ErrorPreFix + "Thread gestartet");

        while (true)
        {
            //Game engine loop
            receiveInformation();
            updateFrame();
            if(++counter % 10 == 0)
                System.err.println(MainActivity.ErrorPreFix + "10 update loops later");

        }


    }

    private void updateFrame()
    {
        try {
            Frame f = session.update();

        } catch (Exception e) {
            System.err.println(MainActivity.ErrorPreFix + "Runtime Error during session update loop");
            e.printStackTrace();
        }
    }

    private void receiveInformation() {
        //TODO synchronize with server
    }
}
