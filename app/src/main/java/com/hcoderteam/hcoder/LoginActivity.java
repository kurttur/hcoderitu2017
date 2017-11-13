package com.hcoderteam.hcoder;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private LinearLayout login_container;
    private AppCompatEditText login_username;
    private TextView login_error_text;
    private AppCompatButton login_button;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        login_container = (LinearLayout) findViewById(R.id.login_container);
        setLoginContainerMargin();

        login_username = (AppCompatEditText) findViewById(R.id.login_username);
        login_error_text = (TextView) findViewById(R.id.login_error_text);
        login_button = (AppCompatButton) findViewById(R.id.login_button);
        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                username = login_username.getText().toString();

                if (username.isEmpty()) {
                    login_error_text.setVisibility(View.VISIBLE);
                    login_error_text.setText("Please fill all fields");
                } else if (username.length() < 6) {
                    login_error_text.setVisibility(View.VISIBLE);
                    login_error_text.setText("Please enter at least 6 characters");
                } else {
                    login_error_text.setVisibility(View.GONE);

                    try {

                        String url = getString(R.string.server_url) + "/login";
                        String json_string = "{\"username\":\"" + username + "\"}";
                        JSONObject json = new JSONObject(json_string);

                        JsonObjectRequest login_request = new JsonObjectRequest(Request.Method.POST, url,
                                json, login_listener, login_error_listener);

                        Volley.newRequestQueue(getApplicationContext()).add(login_request);
                        login_button.setEnabled(false);

                        login_error_text.setText("Please Wait");
                        login_error_text.setVisibility(View.VISIBLE);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void setLoginContainerMargin() {

        //get screen sizes
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        // left, top, right, bottom
        lp.setMargins(0, (metrics.heightPixels)/5, 0, 0);
        login_container.setLayoutParams(lp);
    }

    private Response.Listener<JSONObject> login_listener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            login_error_text.setVisibility(View.GONE);
            login_button.setEnabled(true);

            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.putExtra("USERNAME", username);
            startActivity(i);
        }
    };

    private Response.ErrorListener login_error_listener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            login_error_text.setVisibility(View.VISIBLE);
            login_button.setEnabled(true);

            if (error instanceof NetworkError)
                login_error_text.setText("Connection Failure");
            else if (error instanceof ServerError)
                login_error_text.setText("Server Failure");
            else if (error instanceof AuthFailureError)
                login_error_text.setText("Username Already Taken");
            else if (error instanceof ParseError)
                login_error_text.setText("Server Failure");
            else if (error instanceof TimeoutError)
                login_error_text.setText("Timeout Error");
            else
                login_error_text.setText("Unkonown Error");
        }
    };
}
