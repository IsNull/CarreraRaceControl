package com.zuehlke.carrera.console;

import com.zuehlke.carrera.model.WebSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by wgiersche on 27.09.2014.
 */
public class RaceConsole {

    public static void main ( String [] args ) throws URISyntaxException, IOException {
        if ( args.length != 0 ) {
            System.exit(0);
        }

        WebSocket ws = new WebSocket(new URI(System.getProperty("url","ws://127.0.0.1:9000")));

        ws.connect();

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
                    String command = "C1: " + cmd;
                    System.out.println("Sending '" + command + "' to race control!");
                    ws.send(command);
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        ws.close();
        System.out.println("Race-Console has been ended.");


    }
}
