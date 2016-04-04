package com.example.cantor.pruebamultiplayerv2;

/**
 * Created by Cantor on 03/04/2016.
 */
public interface Observable {
    public void addObserver(Observer obs);
    public void deleteObserver(Observer obs);
}
