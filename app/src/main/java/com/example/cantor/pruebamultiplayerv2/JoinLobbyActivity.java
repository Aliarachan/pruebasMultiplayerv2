package com.example.cantor.pruebamultiplayerv2;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

public class JoinLobbyActivity extends AppCompatActivity implements Observer {
    private LocalNetworkManager managerApplication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_lobby);

        managerApplication = (LocalNetworkManager)getApplication();
        managerApplication.checkin();
        managerApplication.addObserver(this);

        final ListView batList = (ListView)findViewById(R.id.listViewLobbies);
        managerApplication.setmHandlerActivities(mHandler);

        batList.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = batList.getItemAtPosition(position).toString();
                managerApplication.userJoinSession(name);
                Intent batIntent = new Intent(JoinLobbyActivity.this, CreateLobbyActivity.class);
                batIntent.putExtra("tmpID", 1);
                batIntent.putExtra("roomName", name);
                startActivity(batIntent);
            }
        });
        //A lo mejor al entrar ya hay salas creadas
        refreshLobbies();

    }

    public synchronized void update(Observable o, Object arg) {
        String qualifier = (String)arg;
        if(qualifier.equals(LocalNetworkManager.REFRESH_LOBBIES_AND_PLAYERS)){
            Message message = mHandler.obtainMessage(HANDLE_REFRESH_LOBBIES_AND_PLAYERS);
            mHandler.sendMessage(message);
        }

    }

    private void refreshLobbies() {
        ListView batList = (ListView)findViewById(R.id.listViewLobbies);
        ArrayAdapter<String> channelListAdapter = new ArrayAdapter<String>(this, android.R.layout.test_list_item);
        channelListAdapter.clear();
        batList.setAdapter(channelListAdapter);
        //Encontramos los nombres de las salas que hemos creado
        List<String> channels = managerApplication.getmLobbies();
        for (String channel : channels) {
            //Buscamos el ultimo punto
            //Hace un nombre compuesto con el package y mas cosas
            //Estan separados por puntos y despues del ultimo esta el nombre de la sala
            int lastDot = channel.lastIndexOf('.');
            //Si no hay punto es que no es un nombre de la sala y pasamos de ello
            if (lastDot < 0) {
                continue;
            }
            //Cogemos las siguientes posiciones despues del ultimo punto
            channelListAdapter.add(channel.substring(lastDot + 1));
        }
        channelListAdapter.notifyDataSetChanged();
    }


    private static final int HANDLE_REFRESH_LOBBIES_AND_PLAYERS = 2;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_REFRESH_LOBBIES_AND_PLAYERS:
                    refreshLobbies();
                default:
                    break;
            }
        }
    };
}
