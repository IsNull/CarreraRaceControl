package com.zuehlke.carrera.model;

import java.util.Arrays;

/**
 * Immutable (almost) implementation of sensor data.
 *
 * Created by paba on 29.09.2014.
 */
public class SensorEvent {
    /**
     * Returns a empty sensor data
     */
    public static final SensorEvent Empty = createEmptyCarSensor();

    /**
     * Creates a new, empty Car-Sensor Event
     * @return
     */
    public static SensorEvent createEmptyCarSensor(){
        return new SensorEvent(new float[3], new float[3], new float[3], 0);
    }

    private SensorEventType type;
    private long timeStamp;
    private float[] acc;
    private float[] gyr;
    private float[] mag;

    /**
     * Empty constructor for serialisation
     */
    protected SensorEvent(){
    }

    /**
     * Creates a new RaceTrackEvent
     * @param type the type of this event
     */
    public SensorEvent(SensorEventType type, long timeStamp){
        this.type = type;
        this.timeStamp = timeStamp;
    }

    /**
     * Creates a Sensor-Event of Car-Sensor Data
     * @param acc
     * @param gyr
     * @param mag
     * @param timeStamp
     */
    public SensorEvent(float[] acc, float[] gyr, float[] mag, long timeStamp){
        this.acc = Arrays.copyOf(acc, acc.length);
        this.gyr = Arrays.copyOf(gyr, gyr.length);
        this.mag = Arrays.copyOf(mag, mag.length);
        this.timeStamp = timeStamp;
        this.type = SensorEventType.CAR_SENSOR_DATA;
    }

    /**
     * Gets the X, Y, Z Acceleration
     * @return
     */
    public float[] getAcc(){
        return acc;
    }

    /**
     * Gets the X, Y, Z Gyro Data
     * @return
     */
    public float[] getGyr(){
        return gyr;
    }

    /**
     * Gets the X, Y, Z Magnitude
     * @return
     */
    public float[] getMag(){
        return mag;
    }

    /**
     * Get the time-stamp when this event happened
     * @return
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Get the event type
     * @return
     */
    public SensorEventType getType() {
        return type;
    }


    @Override
    public String toString() {
        return "SensorEvent{" +
                "type=" + type +
                ", timeStamp=" + timeStamp +
                ", acc=" + Arrays.toString(acc) +
                ", gyr=" + Arrays.toString(gyr) +
                ", mag=" + Arrays.toString(mag) +
                '}';
    }
}
