package com.zuehlke.carrera;

import com.zuehlke.carrera.race.display.GForceDisplay;
import processing.core.PApplet;

public class SensorDisplayApp {

    public static void main(String[] args) {
        PApplet.main(new String[]{GForceDisplay.class.getCanonicalName()});
    }
}
