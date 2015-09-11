package org.tamgeniue.prediction;

import org.tamgeniue.grid.*;
import org.tamgeniue.model.grid.Grid;
import org.tamgeniue.model.grid.GridCell;
import org.tamgeniue.model.grid.GridLeafTraHashItem;
import org.tamgeniue.model.roi.RoICell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class MicroState extends State{
	public HashMap<RoICell,ArrayList<Long>> Ltratime;
	public HashMap<Long,RoICell> LTraCell;//RoIcell is the next location of (traId+time); i.e. traId+time----> RoICell

	public double minBound;//the distance to farthest point

	public MicroState(int inId){
	
		super(inId);
		Ltratime=new HashMap<>();
		LTraCell=new HashMap<>();
		minBound=-1;
	}
	
	public MicroState(int inId,MicroState ms){
		super(inId,ms);

		Ltratime=new HashMap<>();
		Ltratime.putAll(ms.Ltratime);
		
		LTraCell=new HashMap<>();
		LTraCell.putAll(ms.LTraCell);
	
		minBound=ms.minBound;
	}
	
	public MicroState(int inId,MicroState ms1,MicroState ms2){
		super(inId,ms1,ms2);
		
		Ltratime= new HashMap<>();
		Ltratime.putAll(ms1.Ltratime);//put a into res
		addLtratimeHash(Ltratime,ms2.Ltratime);
		LTraCell=new HashMap<>();
		LTraCell.putAll(ms1.LTraCell);
		LTraCell.putAll(ms2.LTraCell);
		
		
		this.setMinBound();
	}
	
	/**
	 * merge ltt into b
	 */
	private void addLtratimeHash(
			HashMap<RoICell, ArrayList<Long>> ltt,
			HashMap<RoICell, ArrayList<Long>> b) {
        for (Entry<RoICell, ArrayList<Long>> bItem : b.entrySet()) {
            ArrayList<Long> resItem = ltt.get(bItem.getKey());
            if (null == resItem) {
                resItem = new ArrayList<>();//if empty, create a new one
                ltt.put(bItem.getKey(), resItem);
            }
            resItem.addAll(bItem.getValue());
        }
	}

	/**
	 * and <rc,e> into Ltratime hashmap
	 */
	private void addLtratimeHash(HashMap<RoICell, ArrayList<Long>> ltt,RoICell rc,Long e){
		ArrayList<Long> lttItem=ltt.get(rc);//get array of Long
		if(null==lttItem){//if null, creat new one and put it into hashmap
			lttItem=new ArrayList<>();
			ltt.put(rc, lttItem);
		}
		lttItem.add(e);
	}
	
	/**
	 * delete <rc,e> from Ltratime HashMap
	 */
	private void deleteLtratimeHash(HashMap<RoICell,ArrayList<Long>> ltt,RoICell rc,Long e){
		ArrayList<Long> res=ltt.get(rc);
		res.remove(e);
	}
	/**
	 * add one cell into the microstate
	 */
	public void addPoint(int x,int y,double den,Entry<Long,GridLeafTraHashItem> e){
	
		addPoint( x, y,  den, e, true);
	}

	public void addPoint(int x,int y, double den, Entry<Long,GridLeafTraHashItem>e, boolean setBound){
		//sum of position
		LSX+=x;
		LSY+=y;
		
		//sum of square
		SSX+=x*x;
		SSY+=y*y;
		
		//sum of density
		SumDensity+=den;
		
		//add to LC
		
		RoICell rc=new RoICell(x,y);
		LC.add(rc);
		if(null!=e){
		int key= Configuration.getTraId(e.getKey());
		//LT.add(key);
		LT.put(key, rc);
		
		addLtratimeHash(Ltratime,rc,e.getKey());
		
		LTraCell.put(e.getKey(),rc);
		}

		n+=1;//size ++
		if(setBound){
		setMinBound();
		}
	}
	
	/**
	 * delete a point from this micro state
	 */
	public void deletePoint(Long point){
				
		RoICell rc=LTraCell.remove(point);//remove this traId, and get RoICell
		assert(rc!=null);
		
		deleteLtratimeHash(Ltratime,rc,point);//remove from Ltratime
		Integer traId=Configuration.getTraId(point);
		LT.remove(traId);//remove from LT
		LC.remove(rc);//remove from LC
		
		
		int x=rc.getRoiX();
		int y=rc.getRoiY();
		
		LSX-=x;
		LSY-=y;
		
		SSX-=x*x;
		SSY-=y*y;

		//GridCell gc=g.getGridCell(x, y);//process later, and once
		//double den=gc.density;
		//SumDensity-=den;

		n-=1;

		//if(setBound){//process latter,and once
		//	setMinBound();
		//}

		
	}
	
	/**
	 * merge two state
	 */
	public void addMicroState(MicroState ms){
		
		super.addState(ms);
		///LC.addAll(ms.LC);
		//LT.addAll(ms.LT);
		//Ltra.putAll(ms.Ltra);
		addLtratimeHash(Ltratime,ms.Ltratime);
		LTraCell.putAll(ms.LTraCell);
		//sum size
		//set minimum bound, if we add up state, this is not necessary
		setMinBound();
	}
	
	/**
	 * update the density of this state by one scan
	 */
	public void updateDensity(Grid g){
		SumDensity=0;
		//visit every RoI and get the density
        for (RoICell lcvItem : LTraCell.values()) {
            GridCell gc = g.getGridCell(lcvItem.getRoiX(), lcvItem.getRoiY());
            if (null != gc) {
                SumDensity += gc.density;
            }
        }
	}
	
	/**
	 * set the minimum bound for each micro state
	 */
	public void setMinBound(){
		//if there is only one cell, the radius is sqrt(2)/2
		if(LC.size()==1){
			minBound=Configuration.cellRadius;
			return ;
		}
		
		double c[]=getCenter();//get center
		double max=-1;
		double temp;
		
		//visit all element
        for (RoICell rc : LC) {
            double p[] = {rc.getRoiX(), rc.getRoiY()};
            temp = (c[0] - p[0]) * (c[0] - p[0]) + (c[1] - p[1]) * (c[1] - p[1]);//distance
            if (temp > max) {//find maximum distance
                max = temp;
            }
        }
		minBound=Math.sqrt(max);
	}

	public String toLCString(){
		
		String str="";
		for(RoICell rc:LC){
			str+=rc.toString();
		}
		
		return str;
	}

	public int getSize(){
		return n;
	}
	
}
