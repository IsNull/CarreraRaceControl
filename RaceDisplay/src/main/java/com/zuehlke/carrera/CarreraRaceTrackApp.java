package com.zuehlke.carrera;

import com.zuehlke.carrera.model.RazorAPI;
import com.zuehlke.carrera.model.SensorData;
import com.zuehlke.carrera.model.SpeedControl;
import com.zuehlke.carrera.model.racetrack.RaceTrackAPI;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by wgiersche on 29.09.2014.
 */
public class CarreraRaceTrackApp {

    public static void main(String[] args) {
        new CarreraRaceTrackApp();
    }

    private static final String backendUrl = "http://comupter:8080/ws/rest/raceTrack";

    private final RazorAPI razor;
    private final RaceTrackAPI raceTrack;
    private Timer speedPoller;
    private Timer sensorPusher;


    public CarreraRaceTrackApp(){

        razor = new RazorAPI();
        raceTrack = new RaceTrackAPI();

        System.out.println("Completed successfully :-)");
        startTicker();
    }

    private void startTicker(){
        speedPoller.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pollSpeedControl();
            }
        }, 0, 100);

        sensorPusher.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pushSensorData();
            }
        }, 0, 100);
    }


    private void pollSpeedControl(){
        SpeedControl speedControl = new RestTemplate().getForObject(backendUrl + "/speed", SpeedControl.class);
        if(speedControl != null){
            int speed = (int)Math.floor( speedControl.getPower() * 10d ) ;
            raceTrack.setCarSpeed(speed);
        }
    }

    private void pushSensorData(){
        SensorData data = razor.readSensorData();
        new RestTemplate().postForEntity(backendUrl + "/sensor", data, Object.class);
    }

}
