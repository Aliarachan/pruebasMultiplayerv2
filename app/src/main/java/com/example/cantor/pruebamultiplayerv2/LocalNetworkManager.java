package com.example.cantor.pruebamultiplayerv2;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;

import org.alljoyn.bus.BusException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Cantor on 03/04/2016.
 */
public class LocalNetworkManager extends Application  implements Observable{
    ComponentName mRunningService = null;
    public static String PACKAGE_NAME;
    //All objects that are observing outsider events.
    private List<Observer> mObservers = new ArrayList<Observer>();
    private List<String> mLobbies = new ArrayList<String>();



    private static User user;
    private static ConcurrentHashMap<String, UserInterface> lstUsers;
    private String currentLobbyName = null;
    private Handler mHandlerActivities;



    public void onCreate() {
        user = new User();
        lstUsers = new ConcurrentHashMap<String, UserInterface>();
        PACKAGE_NAME = getApplicationContext().getPackageName();
        Intent intent = new Intent(this, LocalNetworkServices.class);
        mRunningService = startService(intent);
    }

    public void checkin() {
        if (mRunningService == null) {
            Intent intent = new Intent(this, LocalNetworkServices.class);
            mRunningService = startService(intent);
            if (mRunningService == null) {
                //TODO: fatal_error impossible to create services
            }
        }
    }

    public static User getUser() {
        return user;
    }

    public static void setUser(User user) {
        LocalNetworkManager.user = user;
    }

    public static ConcurrentHashMap<String, UserInterface> getLstUsers() {
        return lstUsers;
    }

    public static void setLstUsers(ConcurrentHashMap<String, UserInterface> lstUsers) {
        LocalNetworkManager.lstUsers = lstUsers;
    }

    public String getCurrentLobbyName() {
        return currentLobbyName;
    }

    public void setCurrentLobbyName(String currentLobbyName) {
        this.currentLobbyName = currentLobbyName;
    }

    public Handler getmHandlerActivities() {
        return mHandlerActivities;
    }

    public void setmHandlerActivities(Handler mHandlerActivities) {
        this.mHandlerActivities = mHandlerActivities;
    }

    public List<String> getmLobbies() {
        return mLobbies;
    }

    public ArrayList getUsersName(){
        ArrayList<String> tmp = new ArrayList<String>();
        ArrayList<UserInterface> tmp2 = new ArrayList<>(lstUsers.values());
        for (UserInterface batUser: tmp2){
            try {
                tmp.add(batUser.getName());
            } catch (BusException e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    public static final String APPLICATION_QUIT_EVENT = "APPLICATION_QUIT_EVENT";
    public static final String USE_JOIN_CHANNEL_EVENT = "USE_JOIN_CHANNEL_EVENT";
    public static final String USE_LEAVE_CHANNEL_EVENT = "USE_LEAVE_CHANNEL_EVENT";
    public static final String HOST_INIT_CHANNEL_EVENT = "HOST_INIT_CHANNEL_EVENT";
    public static final String HOST_START_CHANNEL_EVENT = "HOST_START_CHANNEL_EVENT";
    public static final String HOST_STOP_CHANNEL_EVENT = "HOST_STOP_CHANNEL_EVENT";
    public static final String REFRESH_LOBBIES_AND_PLAYERS = "REFRESH_LOBBIES_AND_PLAYERS";
    public static final String HOST_CLOSE_ROOM = "HOST CLOSE_ROOM";


    /**
     * Adds a given observer to our list, if it is not already in it.
     * @param obs Observer
     */
    public synchronized void addObserver(Observer obs) {
        //If it isn't already in the list
        if (mObservers.indexOf(obs) < 0) {
            //add it
            mObservers.add(obs);
        }
    }

    /**
     * Removes a given observer from out list.
     * @param obs observer
     */
    public synchronized void deleteObserver(Observer obs) {
        mObservers.remove(obs);
    }

    /**
     * For every observer that we have, tell them to update their info.
     * @param arg
     */
    private void notifyObservers(Object arg) {
        for (Observer obs : mObservers) {
            obs.update(this, arg);
        }
    }

    /**
     * Adds a lobby to our list. First, if we had found it before, remove it.
     * @param channel
     */
    public synchronized void addFoundLobby(String channel) {
        removeFoundLobby(channel);
        mLobbies.add(channel);
    }


    /**
     * Removes a lobby from our list.
     * @param channel
     */
    public synchronized void removeFoundLobby(String channel) {
        for (Iterator<String> i = mLobbies.iterator(); i.hasNext();) {
            String string = i.next();
            if (string.equals(channel)) {
                i.remove();
            }
        }
    }

    public synchronized void initRoom(String name){
        currentLobbyName = name;
        user.setIsHost(true);
        notifyObservers(HOST_START_CHANNEL_EVENT);
        notifyObservers(USE_JOIN_CHANNEL_EVENT);

    }

    public synchronized void refreshLobbies(){
        notifyObservers(REFRESH_LOBBIES_AND_PLAYERS);
    }

    public synchronized void userLeaveChannel() {
        currentLobbyName = null;
        notifyObservers(USE_LEAVE_CHANNEL_EVENT);
    }

    public synchronized void userJoinSession(String name){
        currentLobbyName = name;
        notifyObservers(USE_JOIN_CHANNEL_EVENT);
    }


    public synchronized void closeRoom(){
        notifyObservers(HOST_CLOSE_ROOM);
    }

    public synchronized void hostStopChannel() {
        notifyObservers(HOST_STOP_CHANNEL_EVENT);
    }

}
