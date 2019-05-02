package com.signalR.signalrdemo;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.Credentials;
import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.http.Request;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler2;
import microsoft.aspnet.signalr.client.transport.ClientTransport;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class MainActivity extends AppCompatActivity {
    HubConnection hubConnection;
    HubProxy hubProxy;
    Handler handler = new Handler();
    List<Client> clientList;
    List<String> clientsNames;
    Client sendTo;
    Context context;

    EditText etxtUserName, etxtMessage, etxtSendMessage;
    Button btnConnectDisconnect, btnSend;
    Spinner spUsers;

    String TAG = "MainActivity";

    String bearerToken = "Bearer token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etxtUserName = findViewById(R.id.etxtUserName);
        etxtMessage = findViewById(R.id.etxtMessage);
        etxtSendMessage = findViewById(R.id.etxtSendMessage);
        btnConnectDisconnect = findViewById(R.id.btnConnectDisconnect);
        btnSend = findViewById(R.id.btnSend);
        spUsers = findViewById(R.id.spUsers);

        context = this;

        spUsers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    sendTo = clientList.get(position - 1);
                    btnSend.setEnabled(true);
                } else {
                    sendTo = null;
                    btnSend.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnConnectDisconnect.getText().toString().trim().equalsIgnoreCase("Connect")) {
                    connect();
                } else {
                    disconnect();
                }
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!etxtUserName.getText().toString().trim().equals("") && sendTo != null) {
                    hubProxy.invoke("SendMessage", new Object[]{etxtSendMessage.getText().toString().trim(),
                            sendTo.connectionID
                    });
                }
            }
        });
    }

    /**
     * Signalr
     */
    void connect() {
        Platform.loadPlatformComponent(new AndroidPlatformComponent());
        Credentials credentials = new Credentials() {
            @Override
            public void prepareRequest(Request request) {
                request.addHeader("Auth", bearerToken);
            }
        };
        hubConnection = new HubConnection("http://.../");
        hubConnection.setCredentials(credentials);
        hubConnection.connected(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "connect() --> Connected to: " + hubConnection.getUrl());
                        btnConnectDisconnect.setText("Disconnect");
                    }
                });
            }
        });
        hubProxy = hubConnection.createHubProxy("hubName");
        ClientTransport clientTransport = new ServerSentEventsTransport(hubConnection.getLogger());
        SignalRFuture<Void> signalRFuture = hubConnection.start(clientTransport);

        hubProxy.on("getNotification", new SubscriptionHandler1<Object>() {
            @Override
            public void run(Object s) {
                Log.d(TAG, "Json response: " + s);

                try {

                    JSONArray jsonArray = new JSONArray(s);
                    clientList = new ArrayList<>();
                    clientsNames = new ArrayList<>();
                    clientsNames.add("Choose");
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject jk = jsonArray.getJSONObject(i);
                        String uname = jk.getString("username");
                        String conid = jk.getString("connectionID");
                        Client k = new Client(uname, conid);
                        clientList.add(k);
                        clientsNames.add(uname);
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
                            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                            spUsers.setAdapter(adapter);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, Object.class);

        hubProxy.on("SendMessage", new SubscriptionHandler2<String, String>() {
            @Override
            public void run(final String s, final String s2) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        etxtMessage.setText(etxtMessage.getText() + "\n" + s2 + " : " + s);
                    }
                });
            }
        }, String.class, String.class);
        try {
            signalRFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


    /**
     * Reinit
     */
    void disconnect() {
        hubConnection.stop();
        btnConnectDisconnect.setText("Connect");
        clientList.clear();
        spUsers.setAdapter(null);
        btnSend.setEnabled(false);
        sendTo = null;
    }
}
