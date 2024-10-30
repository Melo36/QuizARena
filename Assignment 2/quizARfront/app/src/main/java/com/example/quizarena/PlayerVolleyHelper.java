package com.example.quizarena;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class PlayerVolleyHelper {
    public RequestQueue requestQueue;
    private final String baseURL = "http://192.168.178.31:8080";

    public PlayerVolleyHelper(Context context) {
        // the context represents the current activity
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public void getPlayerByFirebaseUID(String firebaseUID, Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener) {
        // Rest API endpoint
        String url = baseURL + "/player/" + firebaseUID;

        // Create the GET request
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, url, null, responseListener, errorListener);
        requestQueue.add(objectRequest);
    }

    public void getPlayerNicknameByFirebaseUID(String firebaseUID, Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener) {
        // Rest API endpoint
        String url = baseURL + "/player/nickname/" + firebaseUID;

        // Create the GET request
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, url, null, responseListener, errorListener);
        requestQueue.add(objectRequest);
    }


    // post method for creating a user
    public void postRegisterNewUser(JSONObject jsonObject, Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener) {
        // the response listener will wait until there is a response from the backend
        // Rest API endpoint
        String url = baseURL + "/player";

        // Create the POST request
        JsonObjectRequest objRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject, responseListener, errorListener);
        requestQueue.add(objRequest);
    }


}

