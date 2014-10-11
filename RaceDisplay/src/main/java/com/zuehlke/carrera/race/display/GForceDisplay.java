package com.zuehlke.carrera.race.display;

import com.zuehlke.carrera.model.PointXY;
import com.zuehlke.carrera.model.RazorAPI;
import com.zuehlke.carrera.model.SensorEvent;
import processing.core.PApplet;
import processing.core.PFont;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Properties;

/**
 * ***************************************************************************************
 * Based on Test Sketch for Razor AHRS v1.4.2
 * https://github.com/ptrbrtz/razor-9dof-ahrs
 * ****************************************************************************************
 */

public class GForceDisplay extends PApplet {


    private static final boolean SHOW_GRAVYTY_TRIAL = true;

    // IF THE SKETCH CRASHES OR HANGS ON STARTUP, MAKE SURE YOU ARE USING THE RIGHT SERIAL PORT:
    // 1. Have a look at the Processing console output of this sketch.
    // 2. Look for the serial port list and find the port you need (it's the same as in Arduino).
    // 3. Set your port number here:
    // 4. Try again.

    private final RazorAPI razor = new RazorAPI("COM5");


    private PFont font;

    boolean synched = false;

    private int accThreshold1;
    private int accThreshold2;
    private int accThreshold3;
    private float gyrThreshold;

    private SensorEvent data = SensorEvent.Empty;

    private final LinkedList<PointXY> trailAcc = new LinkedList<>();
    private static final int maxTrailLength = 60;
    private PointXY latestAcc = new PointXY(0,0);



    /**
     *
     * @throws URISyntaxException
     */
    public GForceDisplay() throws URISyntaxException {
        println("hello");

        Properties prop = new Properties();
        InputStream input = null;

        try {

            String propertyPath = System.getProperty("config","/config.properties");

            System.out.println("Loading properties from " + propertyPath);

            input = getClass().getResourceAsStream(propertyPath); //new FileInputStream(propertyPath);
            // get the property value and print it out
            prop.load(input);
            accThreshold1 = Integer.parseInt(prop.getProperty("accThreshold1"));
            accThreshold2 = Integer.parseInt(prop.getProperty("accThreshold2"));
            accThreshold3 = Integer.parseInt(prop.getProperty("accThreshold3"));
            gyrThreshold = Float.parseFloat(prop.getProperty("gyrThreshold"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }



    // Global setup
    @Override
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
    }



    @Override
    public void draw() {
        // Reset scene
        background(0);
        lights();
        if (checkSyncWithRazor()) return;

        float scaleFactor = min((height / 768f), (width / 1024f));
        float diameter = 50 * scaleFactor;

        readSerialData(scaleFactor);

        drawGravityTrail(diameter);
        drawGravityCross(scaleFactor, diameter);
        drawGravityThreshold(accThreshold1, scaleFactor, diameter);
        drawGravityThreshold(accThreshold2, scaleFactor, diameter);
        drawGravityThreshold(accThreshold3, scaleFactor, diameter);

        //drawSpeedoMeter(scaleFactor);
        drawGyro(scaleFactor);

        drawTextualData();
    }


    private void readSerialData(float scaleFactor) {

        data = razor.readSensorData();


        PointXY p = new PointXY(width / 2 + data.getAcc()[1] * scaleFactor, height / 2 - data.getAcc()[0] * scaleFactor);
        trailAcc.addFirst(p);
        // If trailAcc is too 'long' remove the oldest points
        while (trailAcc.size() > maxTrailLength)
            trailAcc.removeLast();
        latestAcc = p;
    }



    @Override
    public void keyPressed() {
        switch (key) {
            case '0':  // Turn Razor's continuous output stream off
                razor.disableContinousOutput();
                println("Turn Razor's continuous output stream off");
                break;
            case '1':  // Turn Razor's continuous output stream on
                razor.enableContinousOutput();
                println("Turn Razor's continuous output stream on");
                break;
            case 'f':
                razor.requestSinglePitch();
                break;
        }
    }




    private void drawGyro(float scaleFactor) {
        stroke(255);
        fill(50);
        strokeWeight(4*scaleFactor);

        pushMatrix();
        translate(width - 180 * scaleFactor, 180 * scaleFactor);
        pushMatrix();
        strokeWeight(4 * scaleFactor);

        float gyroZ =  data.getGyr()[2];
        float gyroVal = (gyroZ+100) / 180f * 10;

        rotate(radians(gyroVal));
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

    private void drawTextualData() {
        textFont(font, 20);
        fill(255);
        textAlign(LEFT);

        // Output angles
        pushMatrix();
        translate(10, height - 10);
        textAlign(LEFT);
        DecimalFormat format = new DecimalFormat("####.00");
        text("Acc: " + format.format(data.getAcc()[0]) + " -- " + format.format(data.getAcc()[1]) + " -- " + format.format(data.getAcc()[2]), 0, 0);
        text("Mag: " + format.format(data.getMag()[0]) + " -- " + format.format(data.getMag()[1]) + " -- " + format.format(data.getMag()[2]), 0, -20);
        text("Gyr: " + format.format(data.getGyr()[0]) + " -- " + format.format(data.getGyr()[1]) + " -- " + format.format(data.getGyr()[2]), 0, -40);
        popMatrix();
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
                razor.setupRazor();  // Set ouput params and request synch token
            } else if (frameCount > 2) {
                text("...waiting for Sync token", width / 2, height / 2 + 50, -200);
                synched = razor.syncToken();  // Look for synch token
            }
            return true;
        }
        return false;
    }


    private void drawGravityTrail(float diameter) {

        if(SHOW_GRAVYTY_TRIAL){
            // Draw some of the previous points and fade em out
            if (trailAcc.size() >= 2) {
                noStroke();
                for (int i = trailAcc.size() - 1; i > 0; i--) {
                    PointXY currPoint = trailAcc.get(i);
                    float smallDiameter = diameter * ((1f * (trailAcc.size() - i)) / (2f * trailAcc.size()));
                    fill(0, (1f * (trailAcc.size() - i)) / (1f * trailAcc.size()) * 255, 0);
                    ellipse(currPoint.x, currPoint.y, smallDiameter, smallDiameter);
                }
            }
        }

        if(latestAcc != null){
            noStroke();
            fill(0, 255, 0);
            ellipse(latestAcc.x, latestAcc.y, diameter, diameter);
        }
    }


}
