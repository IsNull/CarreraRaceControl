package com.zuehlke.carrera.model;

import processing.core.PApplet;
import processing.serial.Serial;

import java.util.Date;

/**
 * Created by paba on 29.09.2014.
 */
public class RazorAPI {

    private final static int SERIAL_PORT_BAUD_RATE = 57600;
    private final static String SYNC_TOKEN = "#SYNCH00\r\n";

    private Serial serial;

    // Value buffer
    private final float[] acc = new float[3];
    private final float[] gyr = new float[3];
    private final float[] mag = new float[3];



    public RazorAPI(){
        String portName = "COM5";
        serial = new Serial(new PApplet(){}, portName, SERIAL_PORT_BAUD_RATE);
    }


    public void setupRazor() {
        System.out.println("Trying to setup and sync Razor...");

        // Set Razor output parameters
        serial.write("#oscb");  // Turn on binary output
        serial.write("#o1");  // Turn on continuous streaming output
        serial.write("#oe0"); // Disable error message output

        // Synch with Razor
        serial.clear();  // Clear input buffer up to here
        serial.write("#s00");  // Request synch token
    }


    /**
     * Turn Razor's continuous output stream on
     */
    public void enableContinousOutput(){
        serial.write("#o1");
    }

    /**
     * Turn Razor's continuous output stream off
     */
    public void disableContinousOutput(){
        serial.write("#o0");
    }

    /**
     * Request one single yaw/pitch/roll frame from Razor (use when continuous streaming is off)
     */
    public void requestSinglePitch(){
        serial.write("#f");
    }

    /**
     * Skip incoming serial stream data until token is found
     *
     * @return
     */
    public boolean syncToken() {

        // Wait until enough bytes are available
        if (serial.available() < SYNC_TOKEN.length())
            return false;

        // Check if incoming bytes match token
        for (int i = 0; i < SYNC_TOKEN.length(); i++) {
            if (serial.read() != SYNC_TOKEN.charAt(i))
                return false;
        }
        return true;
    }

    public SensorEvent readSensorData(){
        readSerialData();
        return new SensorEvent( acc, mag, gyr, new Date().getTime() );
    }

    private void readSerialData() {
        while (serial.available() >= 36) { // TODO: Necessary!!?
            //Order is: acc x/y/z, mag x/y/z, gyr x/y/z.
            this.read(acc, 3);
            this.read(mag, 10);
            this.read(gyr, 10);
        }
    }


    /**
     * Read 3 floating point values from the serial bus.
     * @param val
     * @param smoothing
     */
    private void read(float[] val, float smoothing) {
        val[0] += (readFloat(serial) - val[0]) / smoothing;
        val[1] += (readFloat(serial) - val[1]) / smoothing;
        val[2] += (readFloat(serial) - val[2]) / smoothing;
    }


    /**
     * Convert from little endian (Razor) to big endian (Java) and interpret as float
     * @param s
     * @return
     */
    private float readFloat(Serial s) {
        return Float.intBitsToFloat(s.read() + (s.read() << 8) + (s.read() << 16) + (s.read() << 24));
    }


}
