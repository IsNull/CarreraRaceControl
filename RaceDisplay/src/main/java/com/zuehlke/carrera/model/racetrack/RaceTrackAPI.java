package com.zuehlke.carrera.model.racetrack;

import com.zuehlke.carrera.model.WebSocket;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Manages the local race-track
 *
 * Created by paba on 29.09.2014.
 */
public class RaceTrackAPI {

    private String speedControlURL = "ws://127.0.0.1:9000";
    private WebSocket wsSpeedControl = null;

    public RaceTrackAPI(){
        connectSpeedControl();
    }

    /**
     * Sets the car speed
     */
    public synchronized void setCarSpeed(int speed){

        if(!wsSpeedControl.isConnected()){
            System.out.println("Can not send speed-control since we are not connected. Reconnecting...");
            connectSpeedControl(); // Reconnect?

            if(!wsSpeedControl.isConnected()){
                System.out.println("Reconnect failed. Discarding car speed.");
            }
        }

        String cmd = speed + "";
        String command = "C1: " + cmd;
        //System.out.println("Sending '" + command + "' to race control!");
        try {
            wsSpeedControl.send(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private synchronized void connectSpeedControl(){
        try {
            wsSpeedControl = new WebSocket(speedControlURL);
            wsSpeedControl.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void disconnectSpeedControl(){
        if(wsSpeedControl != null){
            try {
                wsSpeedControl.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
