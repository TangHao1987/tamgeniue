// Abstract distance metric class

package org.thirdparty.lib.kdtree;

abstract class DistanceMetric {
    
    protected abstract double distance(double [] a, double [] b);
}
