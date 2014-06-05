package com.zuehlke.race.display;

import processing.core.PApplet;
import processing.core.PFont;
import processing.serial.Serial;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.LinkedList;

/**
 * ***************************************************************************************
 * Based on Test Sketch for Razor AHRS v1.4.2
 * https://github.com/ptrbrtz/razor-9dof-ahrs
 * ****************************************************************************************
 */

public class GForceDisplay extends PApplet {

    // IF THE SKETCH CRASHES OR HANGS ON STARTUP, MAKE SURE YOU ARE USING THE RIGHT SERIAL PORT:
// 1. Have a look at the Processing console output of this sketch.
// 2. Look for the serial port list and find the port you need (it's the same as in Arduino).
// 3. Set your port number here:
    private final static int SERIAL_PORT_NUM = 0;
// 4. Try again.

    private static final boolean CONNECT_TO_RACE_CONTROL = true;
    private final static int SERIAL_PORT_BAUD_RATE = 57600;

    private PFont font;
    private Serial serial;


    boolean synched = false;
    private float[] acc = new float[3];
    private float[] gyr = new float[3];
    private float[] mag = new float[3];
    private int speed = 0;
    private int maxSpeed = 180;
    private int accThreshold1 = 60;
    private int accThreshold2 = 40;
    private int accThreshold3 = 30;
    private int speedLevel1 = 150;
    private int speedLevel2 = 120;
    private float gyrThreshold = 8f;


    private WebSocket ws;

    LinkedList<PointXY> trail = new LinkedList<>();
    int maxTrailLength = 60;

    public GForceDisplay() {
        try {
            ws = new WebSocket(new URI("ws://127.0.0.1:9000"));
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
        if (frame != null) {
            frame.setResizable(true);
        }
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
            if (CONNECT_TO_RACE_CONTROL) ws.connect();
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
        if (checkSyncWithRazor()) return;

        float scaleFactor = min((height / 768f), (width / 1024f));
        float diameter = 50 * scaleFactor;

        readSerialData(scaleFactor);
        autoPilot();

        drawGravityTrail(diameter);
        drawGravityCross(scaleFactor, diameter);
        drawGravityThreshold(accThreshold1, scaleFactor, diameter);
        drawGravityThreshold(accThreshold2, scaleFactor, diameter);
        drawGravityThreshold(accThreshold3, scaleFactor, diameter);

        drawSpeedoMeter(scaleFactor);
        drawGyro(scaleFactor);

        drawTextualData();
    }

    private void drawGyro(float scaleFactor) {
        stroke(255);
        fill(50);
        strokeWeight(4*scaleFactor);

        pushMatrix();
        translate(width - 180 * scaleFactor, 180 * scaleFactor);
        pushMatrix();
        strokeWeight(4 * scaleFactor);
        rotate(radians(gyr[2] / 180f));
        line(0, 0, 0, -150 * scaleFactor);
        popMatrix();
        pushMatrix();
        strokeWeight(1 * scaleFactor);
        rotate(radians(-gyrThreshold));
        line(0, 0, 0, -150 * scaleFactor);
        popMatrix();
        pushMatrix();
        strokeWeight(1*scaleFactor);
        rotate(radians(gyrThreshold));
        line(0, 0, 0, -150*scaleFactor);
        popMatrix();
        popMatrix();

    }

    private void drawSpeedoMeter(float scaleFactor) {
        stroke(255);
        fill(50);
        rect(scaleFactor*5,scaleFactor*5,50*scaleFactor,400*scaleFactor,5*scaleFactor);
        fill(200,50,0);
        rect(scaleFactor*5,scaleFactor*5+(1f-speed/255f)*(400*scaleFactor),50*scaleFactor,(speed/255f)*(400*scaleFactor),0,0,5*scaleFactor,5*scaleFactor);
    }

    private void drawTextualData() {
        textFont(font, 20);
        fill(255);
        textAlign(LEFT);

        // Output angles
        pushMatrix();
        translate(10, height - 10);
        textAlign(LEFT);
        text("Max Speed: " + maxSpeed, 0, -80);
        text("Speed: " + speed, 0, -60);
        DecimalFormat format = new DecimalFormat("####.00");
        text("Acc: " + format.format(acc[0]) + " -- " + format.format(acc[1]) + " -- " + format.format(acc[2]), 0, 0);
        text("Mag: " + format.format(mag[0]) + " -- " + format.format(mag[1]) + " -- " + format.format(mag[2]), 0, -20);
        text("Gyr: " + format.format(gyr[0]) + " -- " + format.format(gyr[1]) + " -- " + format.format(gyr[2]), 0, -40);
        popMatrix();
    }

    private void autoPilot() {
        if (abs(gyr[2])/180f > gyrThreshold && speed > speedLevel1) {
            speed = 110;
        }else
        if (abs(acc[1]) > accThreshold1 && speed > speedLevel1) {
            speed -= 40;
        } else if (abs(acc[1]) > accThreshold2 && speed > speedLevel2) {
            speed -= 10;
        } else if (abs(acc[1]) <= accThreshold3) {
            speed += 10;
            if (speed > maxSpeed)
                speed = maxSpeed;
        }

        try {
            if (CONNECT_TO_RACE_CONTROL) ws.send("C1: " + speed);
        } catch (IOException e) {
            if (CONNECT_TO_RACE_CONTROL) try {
                ws.connect();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            e.printStackTrace();
        }
    }

    private void drawGravityThreshold(float accThreshold, float scaleFactor, float diameter) {
        // gravity border
        stroke(128);
        strokeWeight(2 * scaleFactor);
        float size = accThreshold*scaleFactor*2;
        line(width / 2 + diameter/2 + accThreshold*scaleFactor, height / 2 - size*scaleFactor, width / 2  + diameter/2 + accThreshold*scaleFactor, height / 2 + size*scaleFactor);
        line(width / 2 - diameter/2 - accThreshold*scaleFactor, height / 2 - size*scaleFactor, width / 2  - diameter/2 - accThreshold*scaleFactor, height / 2 + size*scaleFactor);
    }

    private void drawGravityCross(float scaleFactor, float diameter) {
        // gravity cross
        stroke(128);
        strokeWeight(2 * scaleFactor);
        line(width / 2 - diameter / 2, height / 2, width / 2 + diameter / 2, height / 2);
        line(width / 2, height / 2 - diameter / 2, width / 2, height / 2 + diameter / 2);
    }

    private boolean checkSyncWithRazor() {
        // Sync with Razor
        if (!synched) {
            textAlign(CENTER);
            fill(255);

            text("Connecting to Razor...", width / 2, height / 2, -200);
            if (frameCount == 2) {
                setupRazor();  // Set ouput params and request synch token
            } else if (frameCount > 2) {
                text("...waiting for Sync token", width / 2, height / 2 + 50, -200);
                synched = readToken(serial, "#SYNCH00\r\n");  // Look for synch token
            }
            return true;
        }
        return false;
    }

    private void readSerialData(float scaleFactor) {
        while (serial.available() >= 36) {
            //Order is: acc x/y/z, mag x/y/z, gyr x/y/z.
            read(acc, 3);
            read(mag, 10);
            read(gyr, 10);


            PointXY p = new PointXY(width / 2 + acc[1] * scaleFactor, height / 2 - acc[0] * scaleFactor);
            trail.addFirst(p);
            // If trail is too 'long' remove the oldest points
            while (trail.size() > maxTrailLength)
                trail.removeLast();
        }
    }

    private void drawGravityTrail(float diameter) {
        if (trail.size() >= 2) {
            noStroke();
            for (int i = trail.size() - 1; i > 0; i--) {
                PointXY currPoint = trail.get(i);
                float smallDiameter = diameter * ((1f * (trail.size() - i)) / (2f * trail.size()));
                fill(0, (1f * (trail.size() - i)) / (1f * trail.size()) * 255, 0);
                ellipse(currPoint.x, currPoint.y, smallDiameter, smallDiameter);
            }
            fill(0, 255, 0);
            ellipse(trail.get(0).x, trail.get(0).y, diameter, diameter);
        }
    }

    private void read(float[] val, float smoothing) {
        val[0] += (readFloat(serial) - val[0]) / smoothing;
        val[1] += (readFloat(serial) - val[1]) / smoothing;
        val[2] += (readFloat(serial) - val[2]) / smoothing;
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
                if (maxSpeed <= 240)
                    maxSpeed += 10;
                break;
            case 'l':
                if (maxSpeed >= 50)
                    maxSpeed -= 10;
                break;
            case 's':
                maxSpeed = 10;
                speed = 0;
                break;
        }
    }


}
