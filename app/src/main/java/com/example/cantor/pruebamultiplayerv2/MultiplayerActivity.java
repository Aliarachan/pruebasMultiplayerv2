package com.example.cantor.pruebamultiplayerv2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MultiplayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);
    }

    public void onClick(View v){
        switch(v.getId()){
            case R.id.buttonCreateLobby:
                Intent batCreateIntent = new Intent (this, CreateLobbyActivity.class);
                batCreateIntent.putExtra("tmpID", 0);
                startActivity(batCreateIntent);
                break;
            case R.id.buttonJoinLobby:
                Intent batJoinIntent = new Intent(this, JoinLobbyActivity.class);
                startActivity(batJoinIntent);
                break;
        }
    }
}
