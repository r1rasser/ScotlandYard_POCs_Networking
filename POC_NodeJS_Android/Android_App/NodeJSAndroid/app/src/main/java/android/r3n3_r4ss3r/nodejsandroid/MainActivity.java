package android.r3n3_r4ss3r.nodejsandroid;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    private EditText messagebox;
    private TextView response;
    private Socket mSocket;
    private Emitter.Listener onMessageReceive = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String message;
                    try {
                        message = data.getString("message");
                    } catch (JSONException e) {
                        return;
                    }

                    // add the message to view
                    Toast t = Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG);
                    t.show();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            mSocket = IO.socket("http://" + getString(R.string.server_host) + ":3000");
            mSocket.connect();
            mSocket.on("messageRec", onMessageReceive);
        } catch (URISyntaxException e) {
        }
        messagebox = findViewById(R.id.messagebox);
        response = findViewById(R.id.response);
    }

    public void sendMessage(View view) {
        String message = messagebox.getText().toString();
        mSocket.emit("message", message);
        messagebox.setText("");
    }

    public void requestToServer(View view) {
        String url = "http://" + getString(R.string.server_host) + ":3000/";
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest loginRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String res = null;
                        try {
                            res = response.getString("response");
                        } catch (JSONException e) {

                        }
                        MainActivity.this.response.setText(res);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
        requestQueue.add(loginRequest);
    }
}
