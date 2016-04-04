package com.example.cantor.pruebamultiplayerv2;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.alljoyn.DaemonInit;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Cantor on 03/04/2016.
 */


public class LocalNetworkServices extends Service implements Observer{
    private BusAttachment mBus;
    //This is just for control purposes, it's not mandatory.
    //Initially, the bus is disconnected
    private BusAttachmentState mBusAttachmentState = BusAttachmentState.DISCONNECTED;
    private LobbyBusListener mBusListener = new LobbyBusListener();
    //This will be the prefix-name of our rooms, take into account that.
    private static final String NAME_PREFIX = "com.example.cantor.pruebamultiplayerv2";

    private LocalNetworkManager mManagerApplication = null;
    //This one works in the shadows. While we are using the application, this handler
    //listens the bus trying to find news (for example: someone has created a lobby,
    //lets put this lobby in the list)
    private BackgroundHandler mBackgroundHandler = null;
    //Session id that is used, the application uses this to talk with the remote devices
    private int mUserSessionId = -1;
    //
    private int mHostSessionId = -1;
    private UserInterface currentUserInterface = null;
    //PORT:
    private static final short CONTACT_PORT = 27;

    private User currentUser;
    private ConcurrentHashMap<String, UserInterface> currentLstUsers;


    public void onCreate() {
        startBusThread();
        mManagerApplication = (LocalNetworkManager)getApplication();
        mManagerApplication.addObserver(this);
        //TODO: test this
        //No sé si tenemos que hacer una lista de salas y usuarios en ellas o si se puede hacer directamente de usuarios
        //Y no hay choques (salgan users que no son de la sala que toca)
        currentUser = mManagerApplication.getUser();
        //currentLstUsers= mManagerApplication.getLstUsers();


        doConnect();
        doStartDiscovery();

        currentLstUsers = mManagerApplication.getLstUsers();
    }

    /**
     * Disconnects everything
     */
    public void onDestroy() {
        doStopDiscovery();
        doDisconnect();
        //StopBusThread
        mBackgroundHandler.exit();
        mManagerApplication.deleteObserver(this);
    }




    @Override
    public void update(Observable o, Object arg) {
        String qualifier = (String)arg;

        if (qualifier.equals(LocalNetworkManager.APPLICATION_QUIT_EVENT)) {
            Message message = mBackgroundHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mBackgroundHandler.sendMessage(message);
        }

        if (qualifier.equals(LocalNetworkManager.USE_JOIN_CHANNEL_EVENT)) {
            Message message = mBackgroundHandler.obtainMessage(HANDLE_USE_JOIN_CHANNEL_EVENT);
            mBackgroundHandler.sendMessage(message);
        }

        if (qualifier.equals(LocalNetworkManager.USE_LEAVE_CHANNEL_EVENT)) {
            Message message = mBackgroundHandler.obtainMessage(HANDLE_USE_LEAVE_CHANNEL_EVENT);
            mBackgroundHandler.sendMessage(message);
        }

        if (qualifier.equals(LocalNetworkManager.HOST_INIT_CHANNEL_EVENT)) {
            Message message = mBackgroundHandler.obtainMessage(HANDLE_HOST_INIT_CHANNEL_EVENT);
            mBackgroundHandler.sendMessage(message);
        }

        if (qualifier.equals(LocalNetworkManager.HOST_START_CHANNEL_EVENT)) {
            Message message = mBackgroundHandler.obtainMessage(HANDLE_HOST_START_CHANNEL_EVENT);
            mBackgroundHandler.sendMessage(message);
        }

        if (qualifier.equals(LocalNetworkManager.HOST_STOP_CHANNEL_EVENT)) {
            Message message = mBackgroundHandler.obtainMessage(HANDLE_HOST_STOP_CHANNEL_EVENT);
            mBackgroundHandler.sendMessage(message);
        }
    }

    //States our bus can be found:
    public static enum BusAttachmentState {
        DISCONNECTED,    /** The bus attachment is not connected to the AllJoyn bus */
        CONNECTED,        /** The  bus attachment is connected to the AllJoyn bus */
        DISCOVERING        /** The bus attachment is discovering remote attachments hosting chat channels */
    }


    /**
     * Connects to the bus
     */
    public void doConnect(){
        mBus = new BusAttachment(LocalNetworkManager.PACKAGE_NAME, BusAttachment.RemoteMessage.Receive);
        org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
        //assert(mBusAttachmentState == BusAttachmentState.DISCONNECTED);
        mBus.useOSLogging(true);
        mBus.setDebugLevel("ALLJOYN_JAVA", 7);
        mBus.registerBusListener(mBusListener);

        //currentUser = mManagerApplication.getUser();
        Status status = mBus.registerBusObject(currentUser, "/UserInterface");
        if (status != Status.OK ) {
            //TODO: error message  failed to register
            return;
        }

        status = mBus.connect();
        if (status != Status.OK) {
            //TODO: error message failed to connect
            return;
        }

        status = mBus.registerSignalHandlers(this);
        if (status != Status.OK) {
            //TODO: error message failed to register Handler
            return;
        }

        mBusAttachmentState = BusAttachmentState.CONNECTED;
    }

    /**
     * Disconnects from the bus
     * @return
     */
    private boolean doDisconnect() {
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
        //TODO: see if this works
        mBus.unregisterBusObject(currentUser);
        mBus.unregisterBusListener(mBusListener);
        mBus.disconnect();
        mBusAttachmentState = BusAttachmentState.DISCONNECTED;
        return true;
    }

    /**
     * Starts looking for rooms
     */
    private void doStartDiscovery() {
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
        //Looks for all rooms that starts with our prefix
        //This is important because if you share the network with other applications
        //It could cause to detect rooms that don't belong in our game.
        Status status = mBus.findAdvertisedName(NAME_PREFIX);
        if (status == Status.OK) {
            mBusAttachmentState = BusAttachmentState.DISCOVERING;
            return;
        } else {
            //TODO: Unable to start finding advertised names
            return;
        }
    }

    /**
     * Cancels looking for rooms
     */
    private void doStopDiscovery() {
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED);
        mBus.cancelFindAdvertisedName(NAME_PREFIX);
        mBusAttachmentState = BusAttachmentState.CONNECTED;
    }

    private void doJoinSession() {

        if (currentUser.isHost()) {
            /**
            try {
                currentUser.setName("R");
                currentLstUsers.put("R", currentUserInterface);
            } catch (BusException e) {
                e.printStackTrace();
            }
             **/
            //return;
        }


        String wellKnownName = NAME_PREFIX + "." + mManagerApplication.getCurrentLobbyName();

        /*
         * Since we can act as the host of a channel, we know what the other
         * side is expecting to see.
         */
        short contactPort = CONTACT_PORT;
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
        Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

        SignalEmitter emitter = new SignalEmitter(currentUser, mUserSessionId, SignalEmitter.GlobalBroadcast.Off);
        currentUserInterface = emitter.getInterface(UserInterface.class);
        Status status = mBus.joinSession(wellKnownName, contactPort, sessionId, sessionOpts, new SessionListener() {
            //When the last chat participant leaves and we have an empty chat and we cry because we don't have friends.
            public void sessionLost(int sessionId, int reason) {
                //TODO: chat session has been lost, error?
            }
            /**
            public void sessionMemberAdded(int sessionId, String uniqueName){
                int size = currentLstUsers.size();

                 switch(size){
                     case 0:
                         try {
                             currentUser.setName("R");
                             currentLstUsers.put("R", currentUserInterface);
                         } catch (BusException e) {
                             e.printStackTrace();
                         }
                         break;
                 case 1:
                     try {
                         currentUser.setName("G");
                         currentLstUsers.put("G", currentUserInterface);
                     } catch (BusException e) {
                         e.printStackTrace();
                     }
                 break;
                 case 2:
                     try {
                         currentUser.setName("B");
                         currentLstUsers.put("B", currentUserInterface);
                     } catch (BusException e) {
                     e.printStackTrace();
                     }
                     break;
                 case 3:
                     try {
                         currentUser.setName("Y");
                         currentLstUsers.put("Y", currentUserInterface);
                     } catch (BusException e) {
                     e.printStackTrace();
                     }
                     break;
                 default:
                 //TODO: think about it
                 break;

                 }
            }**/
        });

        int size = currentLstUsers.size();

        switch(size) {
            case 0:
                try {
                    currentUser.setName("R");
                    currentLstUsers.put("R", currentUserInterface);
                } catch (BusException e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                try {
                    currentUser.setName("G");
                    currentLstUsers.put("G", currentUserInterface);
                } catch (BusException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    currentUser.setName("B");
                    currentLstUsers.put("B", currentUserInterface);
                } catch (BusException e) {
                    e.printStackTrace();
                }
                break;
            case 3:
                try {
                    currentUser.setName("Y");
                    currentLstUsers.put("Y", currentUserInterface);
                } catch (BusException e) {
                    e.printStackTrace();
                }
                break;
            default:
                //TODO: think about it
                break;
        }

        if (status == Status.OK) {
            mUserSessionId = sessionId.value;
        } else {
            //TODO: error message unable to join chat session
            return;
        }
    }

    /**
     * A client user leaves the channel. A host user just puts the sessionId to -1.
     */
    private void doLeaveSession(){
        if (currentUser.isHost()) {
            mBus.leaveSession(mUserSessionId);
        }
        mUserSessionId = -1;
    }

    /**
     * Advertising the current channel.
     * You can find this channel with the method doDiscover
     */
    private void doAdvertise() {
        String wellKnownName = NAME_PREFIX + "." + mManagerApplication.getCurrentLobbyName();
        Status status = mBus.advertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);

        if (status != Status.OK) {
            //TODO: error message unable to advertise channel name
        }
    }


    /**
     * Cancel the advertising of the current channel.
     * THe discover method won't be able to find the channel.
     */
    private void doCancelAdvertise() {
        String wellKnownName = NAME_PREFIX + "." + mManagerApplication.getCurrentLobbyName();
        Status status = mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);

        if (status != Status.OK) {
            //TODO: error message in case the bus couldn't cancel the advertisement
            return;
        }
    }

    /**
     * Reserves the name of the lobby
     */
    private void doRequestName() {
        //Bus must be connected
        int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
        assert (stateRelation >= 0);
        //Name we want to request
        //TODO: before calling this, we have to call the setName for Application.
        String wellKnownName = NAME_PREFIX + "." + mManagerApplication.getCurrentLobbyName();
        Status status = mBus.requestName(wellKnownName, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE);
        if (status != Status.OK) {
            //TODO: error message unable to get wellKnownName
        }
    }


    /**
     * Releases the name of the channel. Releases your body. Releases your soul.
     * Bust must be connected, we must have a channel name.
     */
    private void doReleaseName() {
        //The bus must be connected
        int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
        assert (stateRelation >= 0);
        assert(mBusAttachmentState == BusAttachmentState.CONNECTED || mBusAttachmentState == BusAttachmentState.DISCOVERING);
        //Name we want to release:
        String wellKnownName = NAME_PREFIX + "." + mManagerApplication.getCurrentLobbyName();
        mBus.releaseName(wellKnownName);
    }


    /**
     * LETS START THINGS
     * Creates a server, joins it.
     */
    private void doBindSession() {
        Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);

        Status status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
            public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                //If you have the correct contact port, you are welcome, pal.
                if (sessionPort == CONTACT_PORT) {
                    return true;
                }
                return false;
            }

            public void sessionJoined(short sessionPort, int id, String joiner) {
                mHostSessionId = id;
                SignalEmitter emitter = new SignalEmitter(currentUser, id, SignalEmitter.GlobalBroadcast.Off);
                currentUserInterface = emitter.getInterface(UserInterface.class);
            }
        });

        if (status != Status.OK) {
            //TODO: error message can't bind this tutututtu
        }
    }


    /**
     * Server off, everything off, everybody eventually dies.
     */
    private void doUnbindSession() {
        mBus.unbindSessionPort(CONTACT_PORT);
        currentUserInterface = null;
    }




    /**
     * Creates a thread and connects our handler to it, so it can work.
     */
    private void startBusThread() {
        HandlerThread busThread = new HandlerThread("BackgroundHandler");
        busThread.start();
        mBackgroundHandler = new BackgroundHandler(busThread.getLooper());
    }



    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_USE_JOIN_CHANNEL_EVENT = 1;
    private static final int HANDLE_USE_LEAVE_CHANNEL_EVENT = 2;
    private static final int HANDLE_HOST_INIT_CHANNEL_EVENT = 3;
    private static final int HANDLE_HOST_START_CHANNEL_EVENT = 4;
    private static final int HANDLE_HOST_STOP_CHANNEL_EVENT = 5;
    private static final int HANDLE_OUTBOUND_CHANGED_EVENT = 6;

    private class BackgroundHandler extends Handler{
        //This is mandatory:
        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        public void exit(){
            getLooper().quit();
        }
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_APPLICATION_QUIT_EVENT:
                {
                    doLeaveSession();
                    doCancelAdvertise();
                    doUnbindSession();
                    doReleaseName();
                    exit();
                    stopSelf();
                }
                break;
                case HANDLE_USE_JOIN_CHANNEL_EVENT:
                {
                    doJoinSession();
                }
                break;
                case HANDLE_USE_LEAVE_CHANNEL_EVENT:
                {
                    doLeaveSession();
                }
                break;
                case HANDLE_HOST_START_CHANNEL_EVENT:
                {
                    doRequestName();
                    doBindSession();
                    doAdvertise();
                    mManagerApplication.refreshLobbies();
                }
                break;
                case HANDLE_HOST_STOP_CHANNEL_EVENT:
                {
                    doCancelAdvertise();
                    doUnbindSession();
                    doReleaseName();
                    //exit();
                    mManagerApplication.refreshLobbies();

                }
                break;
                default:
                    break;
            }
        }


    }

    private class LobbyBusListener extends BusListener {
        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            LocalNetworkManager application = (LocalNetworkManager)getApplication();
            application.addFoundLobby(name);
        }


        public void lostAdvertisedName(String name, short transport, String namePrefix) {
            LocalNetworkManager application = (LocalNetworkManager)getApplication();
            application.removeFoundLobby(name);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static {
        System.loadLibrary("alljoyn_java");
    }


}
