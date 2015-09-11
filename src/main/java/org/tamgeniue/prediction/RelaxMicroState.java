package org.tamgeniue.prediction;


import org.tamgeniue.grid.Configuration;
import org.tamgeniue.model.grid.Grid;
import org.tamgeniue.model.grid.GridLeafTraHashItem;

import java.util.ArrayList;
import java.util.Map.Entry;

public class RelaxMicroState {

	private ArrayList<Entry<Long,GridLeafTraHashItem>> Ltratime;
	
	private double LSX;//x-the sum the position of all cells
	private double LSY;//y-..
	private int n;
	
	public RelaxMicroState(){
		
		Ltratime=new ArrayList<>();
		
		LSX=0;
		LSY=0;
		n=0;
	}
	

	public void addLtratime(ArrayList<Entry<Long,GridLeafTraHashItem>> in){
		Ltratime.addAll(in);
		
		for(Entry<Long,GridLeafTraHashItem> ein:in){
			LSX+=ein.getValue().getCellX();
			LSY+=ein.getValue().getCellY();
		}
		n+=in.size();
		
	}

	private void minusLS(double x,double y){
		LSX-=x;
		LSY-=y;
		n--;
	}

	public double[] getCenter(){
		double[] c=new double[2];
		
		c[0]=LSX/n;
		c[1]=LSY/n;
		
		return c;
	}
	public double getDisCenter(double txi, double tyi) {
		// TODO Auto-generated method stub
		double c[]=getCenter();
		double p[]={txi,tyi};
        return Math.sqrt((c[0]-p[0])*(c[0]-p[0])+(c[1]-p[1])*(c[1]-p[1]));
	}

	public ArrayList<MicroState> generateMics(double r,Grid inG){
		if(null==this.Ltratime||0==n){
			return null;
		}
		
		ArrayList<MicroState> res=new ArrayList<>();
		
		MicroState micDomain=new MicroState(Configuration.getStateId());//store the master state
		
		//visit all points
	    for(Entry<Long,GridLeafTraHashItem> en:Ltratime){
	    	int enx=en.getValue().getCellX();
	    	int eny=en.getValue().getCellY();
	    	double den=inG.getGridCell(enx, eny).density;//get density
	    	
	    	if(this.getDisCenter(enx, eny)>r){//split this point as a new state
	    		MicroState enMic=new MicroState(Configuration.getStateId());
	    		enMic.addPoint(enx, eny, den, en);
	    	
	    		minusLS(enx,eny);	
	    		
	    		res.add(enMic);
	    	}else{
	    		micDomain.addPoint(enx, eny, den, en,false);//keep them as a new state
	    	}
	    }
	    micDomain.setMinBound();
	    res.add(micDomain);
		
		return res;
	}
}
