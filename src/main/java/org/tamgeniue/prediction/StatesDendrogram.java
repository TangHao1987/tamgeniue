package org.tamgeniue.prediction;

import java.util.ArrayList;

public class StatesDendrogram {
	
	ArrayList<MicroState> micsLevel;
	ArrayList<ArrayList<MacroState>> macsTree;
	
	
	public StatesDendrogram(ArrayList<MicroState> inMicsLevel,ArrayList<ArrayList<MacroState>> inMacsTree){
		micsLevel=inMicsLevel;
		macsTree=inMacsTree;
	}

}
