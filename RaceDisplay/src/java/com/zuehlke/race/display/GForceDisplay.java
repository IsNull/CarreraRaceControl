package com.zuehlke.race.display;

import processing.core.PApplet;
import processing.core.PFont;
import processing.serial.*;

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
    private Float[] acc;
    private Float[] gyr;
    private Float[] mag;

    void drawArrow(float headWidthFactor, float headLengthFactor) {
        float headWidth = headWidthFactor * 200.0f;
        float headLength = headLengthFactor * 200.0f;

        pushMatrix();

        // Draw base
        translate(0, 0, -100);
        box(100, 100, 200);

        // Draw pointer
        translate(-headWidth/2, -50, -100);
        beginShape(QUAD_STRIP);
        vertex(0, 0 ,0);
        vertex(0, 100, 0);
        vertex(headWidth, 0 ,0);
        vertex(headWidth, 100, 0);
        vertex(headWidth/2, 0, -headLength);
        vertex(headWidth/2, 100, -headLength);
        vertex(0, 0 ,0);
        vertex(0, 100, 0);
        endShape();
        beginShape(TRIANGLES);
        vertex(0, 0, 0);
        vertex(headWidth, 0, 0);
        vertex(headWidth/2, 0, -headLength);
        vertex(0, 100, 0);
        vertex(headWidth, 100, 0);
        vertex(headWidth/2, 100, -headLength);
        endShape();

        popMatrix();
    }

    void drawBoard() {
        pushMatrix();


        // Board body
        fill(255, 0, 0);
        box(250, 20, 400);

        // Forward-arrow
        pushMatrix();
        translate(0, 0, -200);
        scale(0.5f, 0.2f, 0.25f);
        fill(0, 255, 0);
        drawArrow(1.0f, 2.0f);
        popMatrix();

        popMatrix();
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
            acc=new Float[]{readFloat(serial),readFloat(serial),readFloat(serial)};
            mag=new Float[]{readFloat(serial),readFloat(serial),readFloat(serial)};
            gyr=new Float[]{readFloat(serial),readFloat(serial),readFloat(serial)};
        }

        // Draw board
        pushMatrix();
        translate(width/2, height/2, -350);
        drawBoard();
        popMatrix();

        textFont(font, 20);
        fill(255);
        textAlign(LEFT);

        // Output info text
        text("Point FTDI connector towards screen and press 'a' to align", 10, 25);

        // Output angles
        pushMatrix();
        translate(10, height - 10);
        textAlign(LEFT);
        DecimalFormat format = new DecimalFormat("####.00");
        text("Acc: " + format.format(acc[0]) + " -- " + format.format(acc[1])+ " -- " + format.format(acc[2]),0,0);
        text("Mag: " + format.format(mag[0]) + " -- " + format.format(mag[1])+ " -- " + format.format(mag[2]),0,-20);
        text("Gyr: " + format.format(gyr[0]) + " -- " + format.format(gyr[1])+ " -- " + format.format(gyr[2]),0,-40);
        popMatrix();
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
        }
    }



}
