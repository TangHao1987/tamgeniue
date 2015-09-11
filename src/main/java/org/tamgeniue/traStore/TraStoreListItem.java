package org.tamgeniue.traStore;

public class TraStoreListItem {
public double lat;//corresponding to x
public double lng;//corresponding to y
public int timestamp;
public int off;

public TraStoreListItem(double inLat,double inLng,int inTime){
	lat=inLat;
	lng=inLng;
	timestamp=inTime;
}
}
