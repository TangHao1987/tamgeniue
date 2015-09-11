package org.tamgeniue.utl;

/**
 * Created by Hao on 11/9/2015.
 * Algorithm Utilities for some common logic using across whole app
 */
public class AlgorithmUtil {
    public static Long getKey(int traId,int time){
        Long key= (long) traId;
        key<<=32;
        key+=time;
        return key;
    }

    public static boolean equalDoubleArray(double[] a,double b[]){
        if(null==a||null==b) return false;
        if(a.length!=b.length) return false;
        for(int i=0;i<a.length;i++){
            if(a[i]!=b[i]) return false;
        }
        return true;
    }
}
