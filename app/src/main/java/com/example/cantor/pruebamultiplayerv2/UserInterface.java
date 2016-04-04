package com.example.cantor.pruebamultiplayerv2;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusProperty;

/**
 * Created by Cantor on 03/04/2016.
 */
@BusInterface(name = "com.example.cantor.pruebamultiplayerv2")
public interface UserInterface {

    @BusProperty(annotation = BusProperty.ANNOTATE_EMIT_CHANGED_SIGNAL)
    public String getName() throws BusException;

    @BusProperty
    public void setName(String name) throws BusException;
}
