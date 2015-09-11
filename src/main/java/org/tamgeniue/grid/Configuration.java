package org.tamgeniue.grid;

import java.util.HashMap;

public class Configuration {


    //PARAMETERS
    public static final int GRID_DIVIDED =2048;//maximum number of grids at each axes

    //LATITUDE/LONGITUDE
    public static final double MIN_LATITUDE =292.0;
    public static final double MIN_LONGITUDE =3935.0;
    public static final double MAX_LATITUDE =23056.0;
    public static final double MAX_LONGITUDE =30851.0;

    //the support of reference trajectories. How many minimum trajectories are required
    public static final int TraSupport=1;
    //for time profiling
	public static int time_retrieve = 0;
	public static double time_cluster = 0;
	public static double time_hmm = 0;
	//end for time profiling
	
	
	public static int BITS_PER_GRID=4;
	public static int MAX_LEVEL=3;

	
	public static int T_Sample=1;//the sample time interval for each update
	public static int T_period=1000;//the period of trajectories which we think they are not old
	
	public static int BrinkConstraintRoI=3; 
	public static double BrinkThreshold=0.1;

	
	public static double cellRadius=Math.sqrt(2)/2;//use to compute the micro state, each cell has a radius sqrt(2)/2
	public static double MicroStateRadius=4*cellRadius;
	
	public static int minNumPerMic=1;//the minimum number of tra id at each micro state, 
	
	

	
	//define parameter related with MacroState and MicroState 
	public static double ProDown=0.9;//stop to go down to next level of the macro states
	public static double MAPPro=0.0001;//terminate the MAP process
	public static int MaxRadius=40;
	public static double MaxStateDis=40;
	public static double AlphaRadius=2;//should be larger than 1
	
	public static double AlphaScore=0.5;
	
	//for continuous prediction
	public static boolean doSelfCorrection=false;
	public static HashMap<Integer,Integer> lifetimeMap=new HashMap<Integer,Integer>();//entry <traId,lifetime>
	public static int lossCount=0;//with debugging purpose
	public static int fullcount=0;//with debugging purpose
	public static double doSelfParameter=3;
	

	public static int CapacityPerPage=135000;
	public static int PageSize=4096000;
	public static String GridFile="GridDiskBuffer";
	
	
	public static int getTraId(Long key){
        return (int)(key>>32);
	}
	
	public static int getTime(Long key){
		long id2=key>>32;
		id2<<=32;	
		long timeLong=key-id2;
        return (int)(timeLong);
	}
	

	
	public static int hitCount=0;

	private static int stateId=0;
	
	public static int getStateId(){
		return stateId++;
	}

	
}
