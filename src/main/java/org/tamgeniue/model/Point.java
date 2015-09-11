package org.tamgeniue.model;

import java.text.DecimalFormat;

/**
 * Define the class for a point in x-y coordinates
 * 
 * @author chensu
 * 
 */
public class Point {
	private double x;
    private double y;

	private static final DecimalFormat DB_DF = new DecimalFormat("0.00");

	public Point() {
	}

	public Point(double px, double py) {
		x = px;
		y = py;
	}

	public Point(Point p) {
		x = p.x;
		y = p.y;
	}

	public double distance(Point p2) {
		return Math.hypot(p2.x - x, p2.y - y);
	}
	
	public double distance(double x1,double y1){
		return distance(new Point(x1,y1));
	}

	public String toString() {
		return "(" + String.format("%1$-6s", DB_DF.format(x)) + ", "
				+ String.format("%1$-6s", DB_DF.format(y)) + ")";
	}

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
