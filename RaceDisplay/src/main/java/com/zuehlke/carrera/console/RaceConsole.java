package com.zuehlke.carrera.console;

import com.zuehlke.carrera.model.racetrack.RaceTrackAPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/**
 * Created by wgiersche on 27.09.2014.
 */
public class RaceConsole {

    public static void main ( String [] args ) throws URISyntaxException, IOException {

        RaceTrackAPI raceTrackAPI = new RaceTrackAPI();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        boolean continuing = true;

        while ( continuing ) {
            try {
                System.out.print("Enter new speed: ");
                String cmd = br.readLine();
                if (cmd != null && cmd.equals("q")) {
                    System.out.println("Ending race console...");
                    continuing = false;
                } else {
                    raceTrackAPI.setCarSpeed(Integer.parseInt(cmd));
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        raceTrackAPI.disconnectSpeedControl();
        System.out.println("Race-Console has been ended.");


    }
}
