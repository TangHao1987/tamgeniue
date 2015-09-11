package org.tamgeniue.model.roi;

public class RoICell {
    private int roiX;
    private int roiY;
    private double density;

    public RoICell(int gridX, int gridY) {
        roiX = gridX;
        roiY = gridY;
        density = 0;
    }

    public RoICell(int gridX, int gridY, double inDensity) {
        roiX = gridX;
        roiY = gridY;
        density = inDensity;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof RoICell){
            RoICell cell = (RoICell)obj;
            if(cell.roiX == this.roiX && cell.roiY == this.roiY){
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return roiX << 16 + roiY;
    }

    @Override
    public String toString() {

        return "<" + roiX + "," + roiY + ">";

    }

    public int getRoiX() {
        return roiX;
    }

    public void setRoiX(int roiX) {
        this.roiX = roiX;
    }

    public int getRoiY() {
        return roiY;
    }

    public void setRoiY(int roiY) {
        this.roiY = roiY;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) {
        this.density = density;
    }
}
