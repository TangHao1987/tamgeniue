// Abstract distance metric class

package org.tamgeniue.utl.kdtree;

abstract class DistanceMetric {
    
    protected abstract double distance(double [] a, double [] b);
}
