package com.zuehlke.race.display;

import processing.core.PApplet;
import processing.core.PFont;
import processing.serial.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;

/******************************************************************************************
 * Based on Test Sketch for Razor AHRS v1.4.2
 * https://github.com/ptrbrtz/razor-9dof-ahrs
 ******************************************************************************************/

public class GForceDisplay extends PApplet {

    // IF THE SKETCH CRASHES OR HANGS ON STARTUP, MAKE SURE YOU ARE USING THE RIGHT SERIAL PORT:
// 1. Have a look at the Processing console output of this sketch.
// 2. Look for the serial port list and find the port you need (it's the same as in Arduino).
// 3. Set your port number here:
    final static int SERIAL_PORT_NUM = 0;
// 4. Try again.


    final static int SERIAL_PORT_BAUD_RATE = 57600;

    PFont font;
    Serial serial;


    boolean synched = false;
    private float[] acc = new float[3];
    private float[] gyr = new float[3];
    private float[] mag = new float[3];
    private float[] gyring = new float[100];
    private int g = 0;
    private short speed = 0;
    private int maxSpeed = 180;

    private static final boolean connect = true;

    private WebSocket ws;

    public GForceDisplay() {
        try {
            ws = new WebSocket(new URI("ws://192.168.174.1:9000"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    // Skip incoming serial stream data until token is found
    boolean readToken(Serial serial, String token) {
        // Wait until enough bytes are available
        if (serial.available() < token.length())
            return false;

        // Check if incoming bytes match token
        for (int i = 0; i < token.length(); i++) {
            if (serial.read() != token.charAt(i))
                return false;
        }

        return true;
    }

    // Global setup
    public void setup() {
        // Setup graphics
        size(1024, 768, OPENGL);
        smooth();
        noStroke();
        frameRate(50);

        // Load font
        font = loadFont("Univers-66.vlw");
        textFont(font);

        // Setup serial port I/O
        println("AVAILABLE SERIAL PORTS:");
        println(Serial.list());
        String portName = Serial.list()[SERIAL_PORT_NUM];
        println();
        println("HAVE A LOOK AT THE LIST ABOVE AND SET THE RIGHT SERIAL PORT NUMBER IN THE CODE!");
        println("  -> Using port " + SERIAL_PORT_NUM + ": " + portName);
        serial = new Serial(this, portName, SERIAL_PORT_BAUD_RATE);

        try {
            if (connect) ws.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void setupRazor() {
        println("Trying to setup and synch Razor...");

        // Set Razor output parameters
        serial.write("#oscb");  // Turn on binary output
        serial.write("#o1");  // Turn on continuous streaming output
        serial.write("#oe0"); // Disable error message output

        // Synch with Razor
        serial.clear();  // Clear input buffer up to here
        serial.write("#s00");  // Request synch token
    }

    float readFloat(Serial s) {
        // Convert from little endian (Razor) to big endian (Java) and interpret as float
        return Float.intBitsToFloat(s.read() + (s.read() << 8) + (s.read() << 16) + (s.read() << 24));
    }

    public void draw() {
        // Reset scene
        background(0);
        lights();

        // Sync with Razor
        if (!synched) {
            textAlign(CENTER);
            fill(255);
            text("Connecting to Razor...", width/2, height/2, -200);

            if (frameCount == 2)
                setupRazor();  // Set ouput params and request synch token
            else if (frameCount > 2)
                synched = readToken(serial, "#SYNCH00\r\n");  // Look for synch token
            return;
        }

        // Read angles from serial port
        while (serial.available() >= 36) {
            //Order is: acc x/y/z, mag x/y/z, gyr x/y/z.
            read(acc,3);
            read(mag,10);
            read(gyr,10);
            g = (g+1)%100;
            gyring[g] = gyr[2];
        }

        // Draw board
        pushMatrix();
        fill(0,255,0);
        rect(400,acc[0]>0?400:400-abs(acc[0]),50,50+abs(acc[0]));
        rect(acc[1]<0?400:400-abs(acc[1]),400,50+abs(acc[1]),50);

        fill(0,0,255);
        rect(800,gyr[0]>0?400:400-abs(gyr[0]),50,50+abs(gyr[0]));
        rect(850,gyr[1]>0?400:400-abs(gyr[1]),50,50+abs(gyr[1]));
        rect(900,gyr[2]>0?        400:400-abs(gyr[2]),50,50+abs(gyr[2]));
        popMatrix();


        //if (abs(gyring[g]-gyring[(g+100-5)%100])>4 && speed > 110) {
        //    speed = 110;
        //}else
        if (abs(acc[1])>40&& speed >150) {
            speed -= 40;
        }
        else if (abs(acc[1])>30&& speed >120) {
            speed -= 10;
        }
        else if (abs(acc[1])<=20 && speed <= maxSpeed)
        {
            speed += 10;
        }

        try {
            if (connect) ws.send("C1: "+speed);
        } catch (IOException e) {
            if (connect) try {
                ws.connect();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            e.printStackTrace();
        }


        textFont(font, 20);
        fill(255);
        textAlign(LEFT);

        // Output angles
        pushMatrix();
        translate(10, height - 10);
        textAlign(LEFT);
        text("Speeed: " + speed, 0, -60);
        DecimalFormat format = new DecimalFormat("####.00");
        text("Acc: " + format.format(acc[0]) + " -- " + format.format(acc[1])+ " -- " + format.format(acc[2]),0,0);
        text("Mag: " + format.format(mag[0]) + " -- " + format.format(mag[1])+ " -- " + format.format(mag[2]),0,-20);
        text("Gyr: " + format.format(gyr[0]) + " -- " + format.format(gyr[1])+ " -- " + format.format(gyr[2]),0,-40);
        popMatrix();
    }

    private void read(float[] val,float smoothing) {
        val[0] += (readFloat(serial)-val[0])/smoothing;
        val[1] += (readFloat(serial)-val[1])/smoothing;
        val[2] += (readFloat(serial)-val[2])/smoothing;
        //smoothedValue += timeSinceLastUpdate * (newValue - smoothedValue) / smoothing

    }

    public void keyPressed() {
        switch (key) {
            case '0':  // Turn Razor's continuous output stream off
                serial.write("#o0");
                println("Turn Razor's continuous output stream off");
                break;
            case '1':  // Turn Razor's continuous output stream on
                serial.write("#o1");
                println("Turn Razor's continuous output stream on");
                break;
            case 'f':  // Request one single yaw/pitch/roll frame from Razor (use when continuous streaming is off)
                serial.write("#f");
                break;
            case 'o':
                if (maxSpeed<=240)
                    maxSpeed += 10;
                break;
            case 'l':
                if (maxSpeed>=50)
                    maxSpeed -= 10;
                break;
        }
    }



}
