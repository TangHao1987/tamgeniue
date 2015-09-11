package org.tamgeniue.prediction;

import java.util.ArrayList;

public class StatesDendrogram {
	
	private ArrayList<MicroState> micsLevel;
    private ArrayList<ArrayList<MacroState>> macsTree;
	
	
	public StatesDendrogram(ArrayList<MicroState> inMicsLevel,ArrayList<ArrayList<MacroState>> inMacsTree){
		micsLevel=inMicsLevel;
		macsTree=inMacsTree;
	}

    public ArrayList<MicroState> getMicsLevel() {
        return micsLevel;
    }

    public ArrayList<ArrayList<MacroState>> getMacsTree() {
        return macsTree;
    }
}
