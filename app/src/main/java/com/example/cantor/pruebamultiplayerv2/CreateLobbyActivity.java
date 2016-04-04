package com.example.cantor.pruebamultiplayerv2;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

public class CreateLobbyActivity extends AppCompatActivity implements Observer{

    private LocalNetworkManager managerApplication = null;
    private int currentUserId;
    private boolean confirmed = false;
    private EditText editTextRoomName;
    private Button confirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_lobby);

        managerApplication = (LocalNetworkManager)getApplication();
        managerApplication.checkin();
        managerApplication.addObserver(this);

        editTextRoomName = (EditText) findViewById(R.id.editTextLobbyName);
        confirmButton = (Button)findViewById(R.id.buttonConfirm);

        Bundle b = getIntent().getExtras();
        currentUserId = b.getInt("tmpID");
        if (currentUserId == 1){
            String batName = b.getString("roomName");
            confirmedRoom(batName);
            confirmButton.setFocusable(false);
        }

        managerApplication.setmHandlerActivities(mHandler);
        //Para cuando entra un usuario y ya hay otros antes:
        //refreshUsersList();
        managerApplication.refreshLobbies();

    }

    public void onClick(View v){
        switch(v.getId()){
            case R.id.buttonConfirm:
                if (!confirmed) {
                    String batName = editTextRoomName.getText().toString();
                    managerApplication.initRoom(batName);
                    confirmedRoom(batName);
                } else{
                    //Intent hacia el gameplay
                }
                //refreshUsersList();
                managerApplication.refreshLobbies();
                break;
            case R.id.buttonGoBack:
                //TODO: de momento lo dejo asi
                goBack();
                break;
            case R.id.buttonRefresh:
                refreshUsersList();
        }
    }

    private void confirmedRoom(String roomName){
        confirmed = true;
        editTextRoomName.setFocusable(false);
        editTextRoomName.setText(roomName);
        confirmButton.setText("PlayStart!");
    }

    public void onBackPressed(){
        goBack();
    }

    private void goBack(){
        if (currentUserId == 0){
            managerApplication.closeRoom();
        } else {
            managerApplication.userLeaveChannel();
            this.finish();
        }
    }

    private void closeRoom(){
        if (currentUserId == 0){
            Toast toast = Toast.makeText(CreateLobbyActivity.this, "Host closing room", Toast.LENGTH_SHORT);
            toast.show();
            managerApplication.hostStopChannel();
            this.finish();
        } else {
            managerApplication.userLeaveChannel();
            Toast toast = Toast.makeText(CreateLobbyActivity.this, "Lobby closed by its host", Toast.LENGTH_SHORT);
            toast.show();
            this.finish();
        }
    }

    public synchronized void update(Observable o, Object arg) {
        String qualifier = (String)arg;

        if (qualifier.equals(LocalNetworkManager.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(LocalNetworkManager.REFRESH_LOBBIES_AND_PLAYERS)){
            Message message = mHandler.obtainMessage(HANDLE_REFRESH_LOBBIES_AND_PLAYERS);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(LocalNetworkManager.HOST_CLOSE_ROOM)){
            Message message = mHandler.obtainMessage(HANDLE_CLOSE_ROOM);
            mHandler.sendMessage(message);
        }
    }

    private void refreshUsersList(){
        ListView batList = (ListView)findViewById(R.id.listViewPlayers);
        ArrayAdapter<String> channelListAdapter = new ArrayAdapter<String>(this, android.R.layout.test_list_item);
        batList.setAdapter(channelListAdapter);
        //Encontramos los nombres de los users que han entrado
        List<String> names = managerApplication.getUsersName();
        for (String name : names) {
            channelListAdapter.add(name);
        }
        channelListAdapter.notifyDataSetChanged();
    }

    private static final int HANDLE_APPLICATION_QUIT_EVENT = 2;
    private static final int HANDLE_REFRESH_LOBBIES_AND_PLAYERS = 3;
    private static final int HANDLE_CLOSE_ROOM = 5;

    /**
     * El handler se suele definir aqui de forma implicita
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_APPLICATION_QUIT_EVENT:
                    goBack();
                    break;
                case HANDLE_REFRESH_LOBBIES_AND_PLAYERS:
                    refreshUsersList();
                    break;
                case HANDLE_CLOSE_ROOM:
                    closeRoom();
                default:
                    break;
            }
        }
    };

}
