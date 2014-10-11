package com.zuehlke.carrera;

import com.zuehlke.carrera.model.RazorAPI;
import com.zuehlke.carrera.model.SensorEvent;
import com.zuehlke.carrera.model.SpeedControl;
import com.zuehlke.carrera.model.racetrack.RaceTrackAPI;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Connects to the Backend-Server and allows the race track to be
 * remote controlled.
 */
public class CarreraRaceTrackApp {

    public static void main(String[] args) {
        new CarreraRaceTrackApp();
    }
    // http://zrhn1772:8080/ws/rest/raceTrack/sensor
    private static final String backendUrl = "http://localhost:8080/ws/rest/relay";

    //private static final String microRelayURL = "http://192.168.1.79:8080/relay";
    //private static final String backendUrl = "http://relay2.beta.swisscloud.io/ws/rest/relay";
    // ping test http://zrhn1772:8080/ws/rest/myBot/ping

    private static final long sensorDataInterval = 20;
    private static final long speedControlInterval = 20;

    private final RazorAPI razor;
    private final RaceTrackAPI raceTrack;
    private Timer speedPoller;
    private Timer sensorPusher;

    Client client = ClientBuilder.newClient()
            .register(JacksonFeature.class);
            //.register(LoggingFilter.class); // Un-comment this to log REST request/response content

    public CarreraRaceTrackApp(){

        razor = new RazorAPI("COM5");
        raceTrack = new RaceTrackAPI();

        System.out.println("Starting periodic poling...");
        startTicker();
    }

    private void startTicker(){

        speedPoller = new Timer();
        sensorPusher = new Timer();

        speedPoller.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
               pollSpeedControl();
            }
        }, 0, speedControlInterval);

        sensorPusher.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pushSensorData();
            }
        }, 0, sensorDataInterval);
    }

    private void pollSpeedControl(){

        int speed = 0;
        try {
            SpeedControl speedControl =
                    client.target(backendUrl + "/speed")
                            .request(MediaType.APPLICATION_JSON)
                            .get(SpeedControl.class);

            if (speedControl != null) {
                speed = (int) Math.floor(speedControl.getPower());
                //System.out.println("Racetrack setting speed to: " + speed);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        raceTrack.setCarSpeed(speed);
    }

    private long MAX_LATENCY = 10;
    private long timecheck = 0;

    private void pushSensorData() {
        try {
            SensorEvent data = razor.readSensorData();

            //System.out.println("Got sensor Data: " + data);
            Response response =
                    client.target(backendUrl + "/sensor")
                            .request(MediaType.APPLICATION_JSON)
                            .post(Entity.json(data), Response.class);

            //System.out.println("" + data.getTimeStamp() + " - " + data.getAcc()[1]);
            if (response.getStatus() >= 400) {
                // HTTP ERROR
                System.err.println("POSTING SENSOR DATA FAILED : " + response.getStatus() + "(" + response.getStatusInfo() + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
