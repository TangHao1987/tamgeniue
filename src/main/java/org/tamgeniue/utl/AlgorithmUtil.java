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
}
