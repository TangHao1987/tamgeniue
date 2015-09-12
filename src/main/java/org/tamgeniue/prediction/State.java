package org.tamgeniue.prediction;


import org.tamgeniue.grid.Configuration;
import org.tamgeniue.model.roi.RoICell;

import java.util.*;
import java.util.Map.Entry;

public class State {

	int id;//each micro state has one id
	
	public int n;//the number of points, (roiCells.size())
	//public double minBound;//the distance to farthest point
	public HashSet<RoICell> roiCells;//record all the cells( or points)
	public HashMap<Integer,RoICell> roICellHashMap;//traId and its corresponding cells
	
	public double SumDensity;//the sum of density of all cells
	public double locationSumX;//x-the sum the position of all cells
	public double locationSumY;//y-..
	public double squareSumX;//x-the square sum of the position of all cells
	public double squareSumY;//y-..
	
	
	public State(int inId){
	
		id=inId;
		
		roiCells =new HashSet<>();
		roICellHashMap = new HashMap<>();
		
		n=0;		
		SumDensity=0;
		locationSumX =0;
		locationSumY =0;
		squareSumX =0;
		squareSumY =0;
	}
	
	public ArrayList<Integer> getLTArray(){
		Set<Integer> LTKeys= roICellHashMap.keySet();
        return new ArrayList<>(LTKeys);
	}
	
	public State(int inId,State ms1,State ms2){
		id=inId;
		
		roiCells =new HashSet<>();
		//LT=new ArrayList<Integer>();
		roICellHashMap = new HashMap<>();
		
		roiCells.addAll(ms1.roiCells);
		roiCells.addAll(ms2.roiCells);
		//LT.addAll(ms1.LT);
		//LT.addAll(ms2.LT);
		mergeLT(roICellHashMap,ms1.roICellHashMap);
		mergeLT(roICellHashMap,ms2.roICellHashMap);
		
		n=ms1.n+ms2.n;
		
		//sum of position
		locationSumX =ms1.locationSumX +ms2.locationSumX;
		locationSumY =ms1.locationSumY +ms2.locationSumY;
		
		//sum of square position
		squareSumX =ms1.squareSumX +ms2.squareSumX;
		squareSumY =ms1.squareSumY +ms2.squareSumY;
		
		//sum of density
		SumDensity=ms1.SumDensity+ms2.SumDensity;
	}
	
	public State(int inId, State ms1){
		id=inId;
		
		roiCells =new HashSet<>();
		roICellHashMap =new HashMap<>();
		roiCells.addAll(ms1.roiCells);
		//LT.addAll(ms1.LT);
		mergeLT(roICellHashMap,ms1.roICellHashMap);
		
		n=ms1.n;
		
		//sum of position
		locationSumX =ms1.locationSumX;
		locationSumY =ms1.locationSumY;
		
		//sum of square position
		squareSumX =ms1.squareSumX;
		squareSumY =ms1.squareSumY;
		
		//sum of density
		SumDensity=ms1.SumDensity;
	}
	
	/**
	 * get the current center of
	 */
	public double[] getCenter(){
		double[] c=new double[2];
		if(!Configuration.doSelfCorrection){
		c[0]= locationSumX /n;
		c[1]= locationSumY /n;
		return c;
		}else{
			return  getCenterWithLifetime();
		
		}
		
	}
	
	public double getStateTraLifetime(){
		if(!Configuration.doSelfCorrection){
			return roICellHashMap.size();
		}
		else{
			double ltSum=0;
			Set<Entry<Integer,RoICell>> LTSet= roICellHashMap.entrySet();
            for (Entry<Integer, RoICell> LTSetItem : LTSet) {
                //Integer lt=null;
                Integer lt = Configuration.lifetimeMap.get(LTSetItem.getKey());//get the lifetime
                Configuration.fullcount++;//for debug
                if (null == lt) {//if empty, just 1
                    ltSum += 1;
                    Configuration.lossCount++;//for debug
                } else {

                    double ltWeight = Math.pow(Configuration.doSelfParameter, lt - 1);//Exponentially increase
                    //double ltWeight=lt;
                    ltSum += ltWeight;
                }
            }
			return ltSum;
		}
	}
	
	/**
	 * get center with lifetime
	 */
	public double[] getCenterWithLifetime(){
		
			double[] c=new double[2];
			double LSXlt=0;
			double LSYlt=0;
			double ltSum=0;
			
			Set<Entry<Integer,RoICell>> LTSet= roICellHashMap.entrySet();
        for (Entry<Integer, RoICell> LTSetItem : LTSet) {
            //Integer lt=null;
            Integer lt = Configuration.lifetimeMap.get(LTSetItem.getKey());//get the lifetime
            Configuration.fullcount++;//for debug
            if (null == lt) {//if empty, just 1
                LSXlt += LTSetItem.getValue().getRoiX();
                LSYlt += LTSetItem.getValue().getRoiY();
                ltSum += 1;
                Configuration.lossCount++;//for debug
            } else {

                double ltWeight = Math.pow(Configuration.doSelfParameter, lt - 1);//Exponentially increase
                //double ltWeight=lt;
                LSXlt += LTSetItem.getValue().getRoiX() * ltWeight;
                LSYlt += LTSetItem.getValue().getRoiY() * ltWeight;
                ltSum += ltWeight;
            }
        }
			c[0]=LSXlt/ltSum;
			c[1]=LSYlt/ltSum;
			return c;
	
		
		
	}
	
	//Add Hashmap B into A
	private void mergeLT(HashMap<Integer,RoICell> LTA,HashMap<Integer,RoICell> LTB){
		if(null==LTB){
			return;
		}
        for (Entry<Integer, RoICell> LTBItem : LTB.entrySet()) {
            LTA.put(LTBItem.getKey(), LTBItem.getValue());
        }
	}
	
	/**
	 * the distance from center to a point
	 */
	public double getDisCenter(double txi, double tyi) {
		// TODO Auto-generated method stub
		double c[]=getCenter();
		double p[]={txi,tyi};
        return Math.sqrt((c[0]-p[0])*(c[0]-p[0])+(c[1]-p[1])*(c[1]-p[1]));
	}
	
	/**
	 * the distance from center to a center of the other micro state
	 */
	public double getDisCenter(State ms){
		double d;
	
		double c[]=getCenter();
		double p[]=ms.getCenter();
		d=Math.sqrt((c[0]-p[0])*(c[0]-p[0])+(c[1]-p[1])*(c[1]-p[1]));
		
		return d;
	}

	/**
	 * merge two state
	 */
	public void addState(State ms){
		//sum of position
		locationSumX +=ms.locationSumX;
		locationSumY +=ms.locationSumY;
		
		//sum of square position
		squareSumX +=ms.squareSumX;
		squareSumY +=ms.squareSumY;
		
		//sum of density
		SumDensity+=ms.SumDensity;
		
		roiCells.addAll(ms.roiCells);
		//LT.addAll(ms.LT);
		mergeLT(roICellHashMap,ms.roICellHashMap);
		
		n+=ms.n;
	}

	public int getSize(){
		return n;
	}
	
	
}
