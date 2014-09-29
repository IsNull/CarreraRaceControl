package com.zuehlke.carrera.model;

import java.util.Arrays;

/**
 * Created by paba on 29.09.2014.
 */
public class SensorData {

    /**
     * Returns a empty sensor data
     */
    public static final SensorData Empty = new SensorData(new float[3], new float[3], new float[3]);

    private final float[] acc;
    private final float[] gyr;
    private final float[] mag;

    public SensorData(float[] acc, float[] gyr, float[] mag){
        this.acc = Arrays.copyOf(acc, acc.length);
        this.gyr = Arrays.copyOf(gyr, gyr.length);
        this.mag = Arrays.copyOf(mag, mag.length);
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
}
