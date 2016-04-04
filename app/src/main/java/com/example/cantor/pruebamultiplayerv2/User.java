package com.example.cantor.pruebamultiplayerv2;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;

/**
 * Created by Cantor on 03/04/2016.
 */
public class User implements UserInterface, BusObject {
    private boolean isHost;
    private String name;

    public User(){
        isHost = false;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
    }


    public String getName() throws BusException {
        return name;
    }


    public void setName(String name) throws BusException {
        this.name = name;
    }
}
