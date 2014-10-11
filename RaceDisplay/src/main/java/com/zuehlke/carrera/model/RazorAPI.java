package com.zuehlke.carrera.model;

import processing.core.PApplet;
import processing.serial.Serial;

import java.util.Date;
import java.util.Random;

public class RazorAPI {

    private final static int SERIAL_PORT_BAUD_RATE = 57600;
    private final static String SYNC_TOKEN = "#SYNCH00\r\n";
    private final float ERR_THRESHOLD = 1000;

    private Serial serial;

    // Value buffer
    private final float[] acc = new float[3];
    private final float[] gyr = new float[3];
    private final float[] mag = new float[3];

    private Random random = new Random();

    public RazorAPI(String portName){
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
    public synchronized void enableContinousOutput(){
        serial.write("#o1");
    }

    /**
     * Turn Razor's continuous output stream off
     */
    public synchronized void disableContinousOutput(){
        serial.write("#o0");
    }

    /**
     * Request one single yaw/pitch/roll frame from Razor (use when continuous streaming is off)
     */
    public synchronized void requestSinglePitch(){
        serial.write("#f");
    }

    /**
     * Skip incoming serial stream data until token is found
     *
     * @return sync successful (true)
     */
    public synchronized boolean syncToken() {

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

    public synchronized void sync() {
        serial.clear();
        byte[] syncBytes = new byte[2];
        random.nextBytes(syncBytes);
        String syncId = new String(syncBytes);
        System.out.println("Syncing with token: " + syncId);
        serial.write("#s"+syncId);

        String token = "#SYNCH"+syncId;

        for (int i = 0; i<= 10; i++) {
            String line = readLine();
            if (line!=null && line.contains(token)) {
                System.out.println("Syncing was successful");
                return;
            }
        }
        System.err.println("Syncing ABORTED after multiple tries");
    }

    private String readLine() {
        String read;
        int exitCounter = 0;
        while ((read = serial.readStringUntil('\n')) == null && exitCounter++<30) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.err.println("interrupted");
            }
        }
        return read;
    }

    public synchronized SensorEvent readSensorData(){
        boolean needsSync = readSerialData();
        if (needsSync) {
            sync();
        }
        return new SensorEvent( acc, mag, gyr, new Date().getTime() );
    }

    private boolean readSerialData() {
        boolean needsSync = false;
        while (serial.available() >= 36) { // float is 4bytes -> 4bytes * 3 (axis) * 3 (sensor values) => 36 bytes
            //Order is: acc x/y/z, mag x/y/z, gyr x/y/z.
            needsSync |= this.read(acc, 3) ||
                         this.read(mag, 10) ||
                         this.read(gyr, 10);
        }
        return needsSync;
    }


    /**
     * Read 3 floating point values from the serial bus.
     * @return possible sync issues
     */
    private boolean read(float[] val, float smoothing) {
        float x = readFloat(serial);
        float y = readFloat(serial);
        float z = readFloat(serial);

        if (x > ERR_THRESHOLD || x < -ERR_THRESHOLD
                || y > ERR_THRESHOLD || y < -ERR_THRESHOLD
                || z > ERR_THRESHOLD || z < -ERR_THRESHOLD) {
            return true;
        }

        val[0] += (x - val[0]) / smoothing;
        val[1] += (y - val[1]) / smoothing;
        val[2] += (z - val[2]) / smoothing;

        return false;
    }


    /**
     * Convert from little endian (Razor) to big endian (Java) and interpret as float
     */
    private float readFloat(Serial s) {
        return Float.intBitsToFloat(s.read() + (s.read() << 8) + (s.read() << 16) + (s.read() << 24));
    }


}
