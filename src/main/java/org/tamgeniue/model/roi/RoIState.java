package org.tamgeniue.model.roi;

import java.util.HashSet;
import java.util.Iterator;

public class RoIState {
    /**
	 * RoIState record a set of RoICell and related properties
	 */
	
	private HashSet<RoICell> roiSet;
	private double avgDensity;
	private double centerX;
    private double centerY;
	public RoIState(){
		roiSet=new HashSet<>();
	}

	public boolean contains(int gridX,int gridY){
        RoICell rc=new RoICell(gridX,gridY);
        return contains(rc);
    }
	
	public boolean contains(RoICell rc){
		return roiSet.contains(rc);
	}

	public void addRoICell(int gridX,int gridY,double density){
		avgDensity=(avgDensity*roiSet.size()+density)/(roiSet.size()+1);
		centerX=(centerX*roiSet.size()+gridX)/(roiSet.size()+1);
		centerY=(centerY*roiSet.size()+gridY)/(roiSet.size()+1);
		
		RoICell rc=new RoICell(gridX,gridY,density);
		roiSet.add(rc);
	}

	public void addRoICell(RoICell roiCell){
        addRoICell(roiCell.getRoiX(),roiCell.getRoiY(),roiCell.getDensity());
    }
	
	public void unionRoIState(RoIState inRoIState){
		
		int sumSize=roiSet.size()+inRoIState.getSize();
		
		int sizeThis=this.getSize();
		int sizeIn=inRoIState.getSize();
		
		avgDensity=(avgDensity*sizeThis+inRoIState.avgDensity*sizeIn)/sumSize;
		
		centerX=(centerX*sizeThis+inRoIState.getCenterX()*sizeIn)/(sumSize);
		centerY=(centerY*sizeThis+inRoIState.getCenterY()*sizeIn)/sumSize;
		
		roiSet.addAll(inRoIState.roiSet);
	}
	

	public int getSize(){
		return roiSet.size();
	}

	public double getAvgDensity(){
		return this.avgDensity;
	}

	public double getCenterX(){
		return this.centerX;
	}
	
	public double getCenterY(){
		return this.centerY;
	}
	
	
	
	@Override
	public String toString(){
		
		Iterator<RoICell> enu=roiSet.iterator();
		String str="";
		
		while(enu.hasNext()){
			RoICell roiCell=enu.next();
			str+=" "+roiCell.toString();
		}
		
		return str;
	}

	public RoICell[] toArray(){
		RoICell[] a=new RoICell[1];
		return roiSet.toArray(a);
	
	}
	
	public static void main(String[] args){
		RoIState roistate=new RoIState();
		roistate.addRoICell(1, 1, 0.5);
		roistate.addRoICell(3, 4, 0.8);
		roistate.addRoICell(5, 6, 0.8);
		
		RoICell xy[]=roistate.toArray();
		
		for(int i=0;i<roistate.getSize();i++){
			System.out.println("x:"+xy[i].getRoiX()+" y:"+xy[i].getRoiY());
		}
	}

    public HashSet<RoICell> getRoiSet() {
        return roiSet;
    }
	
}
