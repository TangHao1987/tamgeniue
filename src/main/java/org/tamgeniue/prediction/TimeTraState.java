package org.tamgeniue.prediction;


import org.tamgeniue.model.grid.Grid;
import org.tamgeniue.model.roi.RoIState;
import org.tamgeniue.traStore.TraStoreListItem;

import java.awt.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * this class store all the information of historical trajectories,
 * 1. provide the operation and store of historical trajectories
 * 2. provide the operation and store of states
 * 
 * @author workshop
 *
 */
public class TimeTraState {

	
	// the time parameterized query result of historical trajectories. The index is delta time
	private ArrayList<ArrayList<TimeTraItem>> timeHisTraArray=null;

	//record the maximum coordination of historical trajectories ate each time stamp
	private Point[] timeHisMaxArray=null;
	//record the minimum coordination of historical trajectories ate each time stamp
	private Point[] timeHisMinArray=null;
	
	private double lat0;
	private double lng0;
	private double step;
	
	
	//discrete the space. 
	private ArrayList<ArrayList<RoIState>> timeHisStateArray=null;
	private ArrayList<ArrayList<HashSet<Integer>>> timeHisStateTraIdArray=null;
	private double[][] beta;
	
	private Grid grid;
	
	public TimeTraState(Grid inGrid,Hashtable<Integer, ArrayList<TraStoreListItem>> inQueryResult, double inLat0,
			double inLng0,double inStep){
	
		lat0=inLat0;
		lng0=inLng0;
		step=inStep;
		grid=inGrid;
		
		convert2TimeQueryRes(inQueryResult);
		timeQueryRes2RoiStateSet();//find states at each time 
		ClassifyTraIdByState();//classify each tra id into each states at each time
		ComputeBeta();//compute the beta array, which is a prori probability
	}
	
	
	/**
	 * convert the queryResult to timeQueryRes support all the trajectory have
	 * the same sample rate, and do not do interpolation process
	 */
	private void convert2TimeQueryRes(Hashtable<Integer, ArrayList<TraStoreListItem>> queryResult) {

		if (queryResult != null) {

			Hashtable<Integer,ArrayList<TimeTraItem>>  timeQueryRes = 
				new Hashtable<Integer, ArrayList<TimeTraItem>>();
			
			Hashtable<Integer,Point> timeQueryMax=new Hashtable<Integer,Point>();
			Hashtable<Integer,Point> timeQueryMin=new Hashtable<Integer,Point>();

			Set<Entry<Integer, ArrayList<TraStoreListItem>>> eq = queryResult
					.entrySet();

			Iterator<Entry<Integer, ArrayList<TraStoreListItem>>> itrEq = eq
					.iterator();

			while (itrEq.hasNext()) {// visit all the entry in queryResult
				Entry<Integer, ArrayList<TraStoreListItem>> itemSet = itrEq
						.next();

				ArrayList<TraStoreListItem> temp = itemSet.getValue();

				for (int i = 0; i < temp.size(); i++) {// put every entry
														// TraStoreListItem into
														// timeQueryRes
					
					// get the entry of timeQueryRes, for i-th time stamp
					ArrayList<TimeTraItem> timeItem = timeQueryRes.get(i);
					if (null == timeItem) {// if this item is empty, create it
						// and put it into timeQueryRes
						timeItem = new ArrayList<TimeTraItem>();
						timeQueryRes.put(i, timeItem);
					}
					
					//find the maximum 
					Point pMax=timeQueryMax.get(i);
					if(null==pMax){
						pMax=new Point(-1,-1);
						timeQueryMax.put(i, pMax);
					}
					
					//find the minimum
					Point pMin=timeQueryMin.get(i);
					if(null==pMin){
						pMin=new Point(Integer.MAX_VALUE,Integer.MAX_VALUE);
						timeQueryMin.put(i,pMin);
					}
					
					
					TimeTraItem timeTraItem = TraStoreListItem2TimeTraItem(
							temp.get(i), itemSet.getKey(), i);
					timeItem.add(timeTraItem);
					
					pMax.x= (pMax.x>timeTraItem.gridx) ? pMax.x:timeTraItem.gridx;
					pMax.y= (pMax.y>timeTraItem.gridy) ? pMax.y:timeTraItem.gridy;
					pMin.x= (pMin.x<timeTraItem.gridx) ? pMin.x:timeTraItem.gridx;
					pMin.y= (pMin.y<timeTraItem.gridy) ? pMin.y:timeTraItem.gridy;
					
				}
			}
			
		
			putHash2Array(timeQueryRes,timeQueryMax,timeQueryMin);
			
			
		}
	}
	
	private void putHash2Array(
			Hashtable<Integer, ArrayList<TimeTraItem>> timeQueryRes,
			Hashtable<Integer, Point> timeQueryMax,
			Hashtable<Integer, Point> timeQueryMin) {
		
		//initialize member variable
		timeHisTraArray = new ArrayList<ArrayList<TimeTraItem>>();
		for (int i = 0; i < timeQueryRes.size(); i++) {
			timeHisTraArray.add(null);
		}
		
		// put the result into ArrayList
		Set<Entry<Integer, ArrayList<TimeTraItem>>> timeQuerySet = timeQueryRes
				.entrySet();
		Iterator<Entry<Integer, ArrayList<TimeTraItem>>> timeQueryItr = timeQuerySet
				.iterator();
		while (timeQueryItr.hasNext()) {
			Entry<Integer, ArrayList<TimeTraItem>> eitr = timeQueryItr.next();
			timeHisTraArray.set(eitr.getKey(), eitr.getValue());
		}
		
		
		//put maximum  into array
		timeHisMaxArray=new Point[timeQueryMax.size()];
		Set<Entry<Integer,Point>> timeMaxSet=timeQueryMax.entrySet();
		Iterator<Entry<Integer,Point>> timeMaxItr=timeMaxSet.iterator();
		while(timeMaxItr.hasNext()){
			Entry<Integer,Point> maxItr=timeMaxItr.next();
			timeHisMaxArray[maxItr.getKey()]=maxItr.getValue();
		}
		
		//put minimum into array
		timeHisMinArray=new Point[timeQueryMin.size()];
		Set<Entry<Integer,Point>> timeMinSet=timeQueryMin.entrySet();
		Iterator<Entry<Integer,Point>> timeMinItr=timeMinSet.iterator();
		while(timeMinItr.hasNext()){
			Entry<Integer,Point> minItr=timeMinItr.next();
			timeHisMinArray[minItr.getKey()]=minItr.getValue();
		}
		
	}

	/**
	 * transforme the a TraStoreListItem to be a TimeTraItem. Addtional
	 * information of tra id and offset ( or timestampe delta is attached)
	 * 
	 * @param inItem
	 * @param inTraId
	 * @param delta
	 * @return
	 */
	private TimeTraItem TraStoreListItem2TimeTraItem(TraStoreListItem inItem,
			int inTraId, int delta) {

		TimeTraItem timeTraItem = new TimeTraItem();

		timeTraItem.timestamp = inItem.timestamp;

		Point gridPoint = transferToGrid(inItem.lat, inItem.lng);

		timeTraItem.gridx = gridPoint.x;
		timeTraItem.gridy = gridPoint.y;

		timeTraItem.traId = inTraId;
		timeTraItem.deltaTime = delta;

		return timeTraItem;
	}

	
	/**
	 * transfer a location in coordination system into the grid map.
	 * @param lat
	 * @param lng
	 * @return
	 */
	private Point transferToGrid(double lat, double lng) {
		double offx = lat - lat0;
		double offy = lng - lng0;

		int gridX = (int) (offx / step);
		int gridY = (int) (offy / step);

		return new Point(gridX, gridY);
	}

	
	/**
	 * convert the time query result into the state set.
	 * it is a discrete process, just get a set of states at each time step
	 */
	public void timeQueryRes2RoiStateSet(){
		if(null==timeHisTraArray) return;
		
		 timeHisStateArray=new ArrayList<ArrayList<RoIState>>();
		 
		 for(int i=0;i<timeHisTraArray.size();i++){
			 timeHisStateArray.add(null);
		 }
		 
		 for(int i=0;i<timeHisTraArray.size();i++){
						 
			 //ArrayList<RoIState> ttiState=grid.DiscreteStates(timeHisTraArray.get(i));
			 ArrayList<RoIState> ttiState=null;
			 timeHisStateArray.set(i, ttiState);	 
		 }

	}
	
	/**
	 * put each traid into different bins according to RoI state
	 */
	private void ClassifyTraIdByState(){
		 if(null==timeHisTraArray) return ;
		 if(null == timeHisStateArray) return;
		 timeHisStateTraIdArray=new ArrayList<ArrayList<HashSet<Integer>>>();
		
		 for(int k=0;k<timeHisTraArray.size();k++){
			 ArrayList<TimeTraItem> timeTraItem=timeHisTraArray.get(k);//  all trajectory id at time k
			 ArrayList<RoIState> timeStateItem=timeHisStateArray.get(k); //all  states at time k
			 ArrayList<HashSet<Integer>> timeStateTraIdItem
			 		=new ArrayList<HashSet<Integer>>();//create a list for classification at time k
			 																						
			 //create bin for each state at time k
			 for(int j=0;j<timeStateItem.size();j++){
				HashSet hashItem=new HashSet<Integer>();
				timeStateTraIdItem.add(hashItem);
			 }
			 
			 timeHisStateTraIdArray.add(timeStateTraIdItem);
			 
			 //put tra id into states
			 for(TimeTraItem ttItem:timeTraItem){
				 for(int i=0;i<timeStateItem.size();i++){
					 if(timeStateItem.get(i).contains(ttItem.gridx,ttItem.gridy)){
						 timeStateTraIdItem.get(i).add(ttItem.traId);
					 }
				 }//end for(int i=0;i<timeStateItem.size();i++)
			 }//end  for(TimeTraItem ttItem:timeTraItem)
			 
		 }//end  for(int k=0;k<timeHisTraArray.size();k++)
		
		
	}
	
	/**
	 * * Note that in algorithm, k starts from 1, i.e. k=1,2,3...., however, in the implementation, k starts from 0,
	 * therefore, each k is minus by 1
	 * @param k is the time stamp
	 * @param i is the i-th element state at tmie k
	 */
	public RoIState getState(int k,int i){
		return timeHisStateArray.get(k-1).get(i);
	}
	
	/**
	 * return the number of states at time k
	 * * Note that in algorithm, k starts from 1, i.e. k=1,2,3...., however, in the implementation, k starts from 0,
	 * therefore, each k is minus by 1
	 * @param k
	 * @return
	 */
	public int getStateNum(int k){
		if(0==k) return 1;
		return timeHisStateArray.get(k-1).size();
	}
	
	/**
	 * return the number of trajectories at time k
	 *  Note that in algorithm, k starts from 1, i.e. k=1,2,3...., however, in the implementation, k starts from 0,
	 * therefore, each k is minus by 1
	 * @param k
	 * @return
	 */
	public int getTraNum(int k){
		return timeHisTraArray.get(k-1).size();
	}
	
	/**
	 * * Note that in algorithm, k starts from 1, i.e. k=1,2,3...., however, in the implementation, k starts from 0,
	 * therefore, each k is minus by 1
	 * @param k
	 * @param i
	 * @return
	 */
	public HashSet<Integer> getStateTraIdBins(int k,int i){
		return timeHisStateTraIdArray.get(k-1).get(i);
	}
	
	/**
	 * return the number of tra id at each state at time k
	 *  Note that in algorithm, k starts from 1, i.e. k=1,2,3...., however, in the implementation, k starts from 0,
	 * therefore, each k is minus by 1
	 * @param k
	 * @param ki
	 * @return
	 */
	public int getTraNumPerState(int k,int ki){
		return timeHisStateTraIdArray.get(k-1).get(ki).size();
	}
	
	/**
	 * 
	 */
	private void ComputeBeta(){
		beta=new double[timeHisStateArray.size()][];		
		for(int k=0;k<beta.length;k++){
			beta[k]=computeBetaArray(k);
		}
	}
	
	/**
	 * 
	 * @param k
	 * @return
	 */
	private double[] computeBetaArray(int k){
		ArrayList<RoIState> roiStates=timeHisStateArray.get(k);
		double beta[]=new double[roiStates.size()];
		
		double denominator=0;
		for(int i=0;i<beta.length;i++){
			beta[i]=roiStates.get(i).getAvgDensity();
			denominator+=beta[i];
		}
		
		for(int i=0;i<beta.length;i++){
			beta[i]/=denominator;
		}
		return beta;
	}
	
	/**
	 * return the priori probability that a tra id will put in each state
	 * Note that in algorithm, k starts from 1, i.e. k=1,2,3...., however, in the implementation, k starts from 0,
	 * therefore, each k is minus by 1
	 * @param k
	 * @return
	 */
	public double[] getBetaArray(int k){
		
		return beta[k-1];
	}
	
	/**
	 * 
	 * @return the priori probability that a tra id will put at state j at time k
	 * Note that in algorithm, k starts from 1, i.e. k=1,2,3...., however, in the implementation, k starts from 0,
	 * therefore, each k is minus by 1param k
	 * @param j
	 * @return
	 */
	public double getBeta_k_j(int k,int j){
		return beta[k-1][j];
	}
	
	/**
	 * return a set of states at time k
	 * Note that in algorithm, k starts from 1, i.e. k=1,2,3...., however, in the implementation, k starts from 0,
	 * therefore, each k is minus by 1
	 * @param k
	 * @return
	 */
	public ArrayList<RoIState> getStateSet(int k){
		return timeHisStateArray.get(k-1);
	}
	
	public int getTimeLength(){
		return timeHisStateArray.size();
	}
	
	/**
	 * for debug
	 */
	public void visitRoIStateSet(){
		if(null!=timeHisStateArray){
			System.out.println("visit the states at each time TimeTraState:TimeHisStateArray");
			for(int i=0;i<timeHisStateArray.size();i++){
				System.out.println("state++++++++++++++++++++++++++++++++++++++++++++++");
				System.out.println("time stampe is:"+i);
				for(RoIState rs:timeHisStateArray.get(i)){
					System.out.println("RoIState is:"+rs.toString());
				}
			}
		}
	}
	
	/**
	 * for debug
	 */
	public void visitTimeQueryRes(){
		
		if(null!=timeHisTraArray){
			System.out.println("vist the TimeTraState:TimeHisTraArray");
			for(int i=0;i<timeHisTraArray.size();i++){
				System.out.println("=======================================");
				System.out.println("time stampe is:"+i);
				for(TimeTraItem ti:timeHisTraArray.get(i)){
					System.out.println("TimeTraItem timestamp="+ti.timestamp+" deltaTime="+ti.deltaTime
							+" gridX="+ti.gridx+" gridY="+ti.gridy+" tra id="+ti.traId);
				}
			}
		}
	}
	
}
