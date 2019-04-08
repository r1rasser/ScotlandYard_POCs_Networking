package com.example.poc_sockets;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private Server server;
    private String serviceName = "com.se2.poc.sockets.";
    private String logTag = "se2_poc";
    private String serviceType = "_http._tcp.";
    private String localEndpointName;
    private boolean isAdvertising = false;
    private boolean isDiscovering = false;
    private NsdManager nsdManager;
    private Client client;
    private TextView tvResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    private void stop() {
        if (isAdvertising) {
            isAdvertising = false;
            nsdManager.unregisterService(registrationListener);
            setText("stopped advertising");
        }
        if (isDiscovering) {
            isDiscovering = false;
            nsdManager.stopServiceDiscovery(discoveryListener);
            setText("stopped discovering");
        }
    }

    private void setText(final String str) {
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                tvResponse.setText(str);
            }
        });
    }

    NsdManager.RegistrationListener registrationListener = new NsdManager.RegistrationListener() {

        @Override
        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            Log.d(logTag, "service registered");
            setText("started advertising");
            serviceName = NsdServiceInfo.getServiceName();
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Registration failed! Put debugging code here to determine why.
            Log.d(logTag, "registration failed. errorcode = "+errorCode);
            setText("advertising failed");
            isAdvertising = false;
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo arg0) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            isAdvertising = false;
            setText("stopped advertising");
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Unregistration failed. Put debugging code here to determine why.
            Log.d(logTag, "unregistration failed. errorcode = "+errorCode);
            setText("stoping advertising failed");
        }
    };

    private void sendPayload() {
        TextView tv = findViewById(R.id.message);
        String str = localEndpointName+": "+tv.getText().toString();
        if (client != null) {
            client.send(str);
        }
        if (server != null) {
            server.send(str);
        }
    }

    public void startAdvertising() {
        server = new Server(this);

        TextView tv = findViewById(R.id.name);
        localEndpointName = tv.getText().toString();

        isAdvertising = true;
        Log.d(logTag, ""+server.getPort());
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(serviceName+localEndpointName);
        serviceInfo.setServiceType(serviceType);
        serviceInfo.setPort(server.getPort());
        nsdManager = (NsdManager)this.getSystemService(Context.NSD_SERVICE);

        nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    public void startDiscovery() {
        isDiscovering = true;
        client = new Client(this);

        TextView tv = findViewById(R.id.name);
        localEndpointName = tv.getText().toString();

        Log.d(logTag, "started discovering");

        nsdManager = (NsdManager)this.getSystemService(Context.NSD_SERVICE);
        nsdManager.discoverServices(
                serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);

    }

    NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(logTag, "Service discovery started");
            setText("started discovering");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found! Do something with it.
            Log.d(logTag, "Service discovery success" + service);

            if (!service.getServiceType().equals(serviceType)) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d(logTag, "Unknown Service Type: " + service.getServiceType());
            } else if (service.getServiceName().equals(serviceName)) {
                // The name of the service tells the user what they'd be
                // connecting to. It could be "Bob's Chat App".
                Log.d(logTag, "Same machine: " + serviceName);
            } else if (service.getServiceName().contains(serviceName)){
                nsdManager.resolveService(service, resolveListener);
                setText("connecting");
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(logTag, "service lost: " + service);
            setText("disconnected");
            isDiscovering = false;
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(logTag, "Discovery stopped: " + serviceType);
            setText("stopped discovering");
            isDiscovering = false;
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(logTag, "Discovery failed: Error code:" + errorCode);
            setText("discovering failed");
            nsdManager.stopServiceDiscovery(this);
            isDiscovering = false;
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(logTag, "Discovery failed: Error code:" + errorCode);
            setText("stoping discovering failed");
            nsdManager.stopServiceDiscovery(this);
            isDiscovering = false;
        }
    };

    NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(logTag, "Resolve failed: " + errorCode);
                setText("connecting failed");
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(logTag, "Resolve Succeeded. " + serviceInfo);
                setText("connected");

                if (serviceInfo.getServiceName().equals(serviceName)) {
                    Log.d(logTag, "Same IP.");
                    return;
                }

                if (client != null) {
                    client.setAddress(serviceInfo.getHost());
                    client.setPort(serviceInfo.getPort());
                    client.connect();
                    client.start();
                }
                if (server != null) {
                    server.setClientAdress(serviceInfo.getHost());
                    server.setClientPort(serviceInfo.getPort());
                }
            }
        };


}
