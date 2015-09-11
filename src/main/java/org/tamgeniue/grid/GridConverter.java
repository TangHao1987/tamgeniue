package org.tamgeniue.grid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("gridConverter")
public class GridConverter {
    @Value("${config.latitude.min}")
    private double refLatitude;
    @Value("${config.longitude.min}")
    private double refLongitude;

    private double step;

    public void setStep(double step) {
        this.step = step;
    }

    public java.awt.Point getPoint(double lat, double lng){
        double offx=lat-refLatitude;
        double offy=lng-refLongitude;

        int gridX=(int)(offx/(step));
        int gridY=(int)(offy/(step));

        return new java.awt.Point(gridX,gridY);
    }

}
