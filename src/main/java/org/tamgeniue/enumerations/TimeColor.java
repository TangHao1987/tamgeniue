package org.tamgeniue.enumerations;

import java.awt.*;

/**
 * Created by Hao on 7/9/2015.
 */
public enum TimeColor {
    RED(Color.RED),
    BLUE(Color.BLUE),
    GREEN(Color.GREEN),
    PINK(Color.PINK),
    CYAN(Color.CYAN),
    ORANGE(Color.ORANGE),
    MAGENTA(Color.MAGENTA),
    WHITE(Color.WHITE),
    DARK_GRAY(Color.DARK_GRAY);

    private TimeColor(Color color){
        this.color = color;
    }
    private Color color;

    public Color getColor() {
        return color;
    }

}
