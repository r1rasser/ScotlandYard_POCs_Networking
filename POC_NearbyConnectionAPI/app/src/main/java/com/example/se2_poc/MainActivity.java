package com.example.se2_poc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private ConnectionsClient mConnectionsClient;
    private String localEndpointName;
    private String logTag = "poc_se2";
    private String appTag = "com.se2.poc";
    private boolean mIsAdvertising = false;
    private boolean misDiscovering = false;
    private boolean mIsConnecting = false;
    private boolean misConnected = false;
    private Endpoint mConnection;
    private TextView tvResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mConnectionsClient = Nearby.getConnectionsClient(this);

        Button sendApp = findViewById(R.id.create);
        sendApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAdvertising();
            }
        });

        TextView tv = findViewById(R.id.name);
        localEndpointName = tv.getText().toString();

        Button sendApp2 = findViewById(R.id.search);
        sendApp2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDiscovery();
            }
        });

        Button sendApp3 = findViewById(R.id.stop);
        sendApp3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop();
            }
        });

        Button sendApp4 = findViewById(R.id.send);
        sendApp4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendPayload();
            }
        });

        tvResponse = findViewById(R.id.response);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, getRequiredPermissions())) {
            if (!hasPermissions(this, getRequiredPermissions())) {
                if (Build.VERSION.SDK_INT < 23) {
                    ActivityCompat.requestPermissions(
                            this, getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
                } else {
                    requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
                }
            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    private void stop() {
        if (mIsAdvertising) {
            mIsAdvertising = false;
            mConnectionsClient.stopAdvertising();
            tvResponse.setText("stopped advertising");
        }
        if (misDiscovering) {
            misDiscovering = false;
            mConnectionsClient.stopDiscovery();
            tvResponse.setText("stopped discovery");
        }
    }

    private void startAdvertising() {
        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(Strategy.P2P_STAR);
        mIsAdvertising = true;
        TextView tv = findViewById(R.id.name);
        localEndpointName = tv.getText().toString();

        mConnectionsClient
                .startAdvertising(
                        localEndpointName,
                        appTag,
                        mConnectionLifecycleCallback,
                        advertisingOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                Log.d(logTag, "Now advertising endpoint");
                                tvResponse.setText("started advertising");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                Log.d(logTag,"startAdvertising() failed.", e);
                                tvResponse.setText("advertising failed");
                            }
                        });
    }

    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    Log.d(logTag, String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));
                    tvResponse.setText(new String(payload.asBytes(), StandardCharsets.UTF_8));
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    Log.d(logTag,
                            String.format(
                                    "onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
                }
            };

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.d(logTag,
                            String.format(
                                    "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                                    endpointId, connectionInfo.getEndpointName()));
                    tvResponse.setText("connecting");
                    mConnection = new Endpoint(endpointId, connectionInfo.getEndpointName());
                    mConnectionsClient
                            .acceptConnection(mConnection.getId(), mPayloadCallback)
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(logTag, "acceptConnection() failed.", e);
                                            tvResponse.setText("connection failed");
                                        }
                                    });
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    Log.d(logTag, String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));

                    if (!result.getStatus().isSuccess()) {
                        Log.d(logTag,
                                String.format(
                                        "Connection failed. Received status %s.",
                                        result.toString()));
                        tvResponse.setText("connection failed");
                        return;
                    }
                    Log.d(logTag, String.format("connectedToEndpoint(endpoint=%s)", endpointId));
                    tvResponse.setText("connected");
                    misConnected = true;
                }

                @Override
                public void onDisconnected(String endpointId) {
                    if (!misConnected) {
                        Log.d(logTag, "Unexpected disconnection from endpoint " + endpointId);
                        return;
                    }
                    misConnected = false;
                    Log.d(logTag, String.format("disconnectedFromEndpoint(endpoint=%s)", endpointId));
                    tvResponse.setText("disconnected");
                }
            };


    private void startDiscovery() {
        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
        discoveryOptions.setStrategy(Strategy.P2P_STAR);
        misDiscovering = true;

        TextView tv = findViewById(R.id.name);
        localEndpointName = tv.getText().toString();

        mConnectionsClient
                .startDiscovery(
                        appTag,
                        new EndpointDiscoveryCallback() {
                            @Override
                            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                                Log.d(logTag,
                                        String.format(
                                                "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                                endpointId, info.getServiceId(), info.getEndpointName()));
                                tvResponse.setText("endpoint found");
                                if (info.getServiceId().equals(appTag)) {
                                    Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                                    connectToEndpoint(endpoint);
                                }
                            }

                            @Override
                            public void onEndpointLost(String endpointId) {
                                Log.d(logTag, String.format("onEndpointLost(endpointId=%s)", endpointId));
                                tvResponse.setText("endpoint lost");
                            }
                        },
                        discoveryOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                Log.d(logTag, "started discovery");
                                tvResponse.setText("started discovering");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(logTag,"startDiscovering() failed.", e);
                                mIsAdvertising = false;
                                tvResponse.setText("discovering failed");
                            }
                        });
    }

    protected void connectToEndpoint(final Endpoint endpoint) {
        Log.d(logTag, "Sending a connection request to endpoint " + endpoint);
        mIsConnecting = true;

        mConnectionsClient
                .requestConnection(localEndpointName, endpoint.getId(), mConnectionLifecycleCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(logTag,"requestConnection() failed.", e);
                                tvResponse.setText("connecting failed");
                                mIsConnecting = false;
                            }
                        });
    }

    private void sendPayload() {
        TextView tv = findViewById(R.id.message);
        String str = localEndpointName+": "+tv.getText().toString();
        Payload payload = Payload.fromBytes(str.getBytes());
        if (!misConnected) {
            TextView tv2 = findViewById(R.id.response);
            tv2.setText("disconnected");
            return;
        }
        mConnectionsClient
                .sendPayload(mConnection.getId(), payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(logTag, "sendPayload() failed.", e);
                                tvResponse.setText("sending failed");
                            }
                        });
    }

    /** Represents a device we can talk to. */
    protected static class Endpoint {
        @NonNull private final String id;
        @NonNull private final String name;

        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }
}
