package org.tamgeniue.prediction;

public class MacroState extends State{
	
	public MacroState(int id){
		super(id);
	}
	
	public MacroState(int id,State ms1,State ms2){
		super(id,ms1,ms2);
	}
	
	public MacroState(int id, State ms1){
		super(id,ms1);
	}
	
	
	/**
	 * merge two state
	 */
	public void addMacroState(State ms){
		super.addState(ms);
	}
	
	public double getRadius(){
		double sx=super.SSX-LSX*LSX/n;
		double sy=super.SSY-LSY*LSY/n;
		
		double r=Math.sqrt((sx+sy)/n);
		if(0==r) r=1;
		return r;
	}
	
	public static double getRadius(State si,State sj){
		
		double sLX=si.LSX+sj.LSX;
		double sLY=si.LSY+sj.LSY;
		
		double sSSX=si.SSX+sj.SSX;
		double sSSY=si.SSY+sj.SSY;
		
		int sn=si.n+sj.n;
		
		double x=sSSX-sLX*sLX/sn;
		double y=sSSY-sLY*sLY/sn;
		
		return Math.sqrt((x+y)/sn);
		
	}
	
	public double getRadius(State si){
		return MacroState.getRadius(this,si );
	}
	
	
	public static void main(String[] args){
		
		MacroState ms=new MacroState(1);
		
		MicroState m1=new MicroState(1);
		m1.addPoint(-2, 1, 0, null);
		MicroState m2=new MicroState(2);
		m2.addPoint(2, 1, 0, null);
		
		ms.addMacroState(m1);
		ms.addMacroState(m2);
		
		System.out.println(ms.getRadius()+" "+ms.getCenter()[0]+" "+ms.getCenter()[1]);
		System.out.println(getRadius(m1, m2));
		
		MacroState ms1=new MacroState(1);
		ms1.addMacroState(m1);
		System.out.print(ms1.getRadius(m2));
		
	}

}
