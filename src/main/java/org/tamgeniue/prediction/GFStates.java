package org.tamgeniue.prediction;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * collect all the GFStateItem at all predictable time stamp
 * @author workshop
 *
 */
public class GFStates {
	
	ArrayList<GFStatesItem> gfsList;
	
	ArrayList<Double[]> delta;
	ArrayList<Integer[]> psi;
	double max_delta;
	
	public GFStates(){
		gfsList=new ArrayList<>();
		gfsList.add(new GFStatesItem());
		
		delta=new ArrayList<>();
		Double[]delta_0=new Double[1];
		delta_0[0]=1.;
		delta.add(delta_0);
		
		psi=new ArrayList<>();
		Integer[] psi_0=new Integer[1];
		psi_0[0]=0;
		psi.add(psi_0);
		
		max_delta=-1;
		
	}
	
	public double getMaxDelta(){
		return max_delta;
	}
	
	/**
	 * update the state by traIdCommSet
	 * @param traIdCommSet: a hashmap records the weights of different reference objects
	 */
	public void updateStatesContinuousWeights(HashMap<Integer,Integer> traIdCommSet){
		
	}
	
	/**
	 * @return: the current maximum value of MAP path
	 */
	public void addStatesItem(GFStatesItem gfi){
		gfsList.add(gfi);
		max_delta= MAPForward();
	}
	
	/**
	 * the recursion part of MAP
	 */
	private double MAPForward(){
		int k=gfsList.size()-1;
		int N_ks=getStateNum(k);
		GFStatesItem gfski=getStatesItem(k);
		
		
		Double delta_ki[]=new Double[N_ks];
		Integer psi_ki[ ]=new Integer[N_ks];
		
		delta.add(delta_ki);
		psi.add(psi_ki);
		
		assert(delta.size()==k+1);
		assert(psi.size()==k+1);
			
		double[] p_zk_xk= gfski.P_z_k_x_k;  //the observation likelihood function
		double[][] p_xk_xk_1=gfski.P_x_k_x_k_1;//the translation function
		
		double maxPro=-1;
		for(int i=0;i<N_ks;i++){
			double pro=MAPMaxDeltaPsiUpdate(k,i,p_xk_xk_1[i],p_zk_xk[i]);//update delta_kj and psi_kj
			if(pro>maxPro) maxPro=pro;
		}
		return maxPro;
	}
	
	
	/**
	 * update delta_kj and psi_kj
	 */
	public double MAPMaxDeltaPsiUpdate(int k,int ki,double p_x_kj_x_k_1[], double p_zk_xkj){
		double max=-1;
		int max_index=-1;
		int N_k_1s=getStateNum(k-1);
		//double[] item=new double[N_k_1s];
		double item=0;
		for(int j=0;j<N_k_1s;j++){
			item=delta.get(k-1)[j]*p_x_kj_x_k_1[j];
			if(item>max){
				max=item;
				max_index=j;
			}
		}
		
		delta.get(k)[ki]=max*p_zk_xkj;
		psi.get(k)[ki]=max_index;
		
		return max*p_zk_xkj;
	}

	public int[] MAPTraceBack(){
		
		//int T=timeTraState.getTimeLength();
		int seedIdx[]=new int[1];
	
		if(gfsList.size()<1) return null; 
		int T=gfsList.size()-1;
		
		int[] path=new int [T+1];//the one more position is left for virtual point at time t=0
		
		path[T]=argmaxIndex(this.delta.get(T));
		
		for(int t=T-1;t>=0;t--){
			path[t]=psi.get(t+1)[path[t+1]];
		}
		return path;
	}

	public ArrayList<MacroState> getMacroStatePath(){
		int[] path=MAPTraceBack();
		if(null==path) return null;
		ArrayList<MacroState> res=new ArrayList<MacroState>();
		
		res.add(new MacroState(-1));
		
		for(int i=1;i<gfsList.size();i++){
			res.add(gfsList.get(i).macs.get(path[i]));
		}
		return res;
	}
	
	private int argmaxIndex(Double[] a){
		double max=-1;
		int argmax=-1;
		for(int i=0;i<a.length;i++){
			if(max<a[i]){
				max=a[i];
				argmax=i;
			}
		}
		return argmax;
	}
	
	/**
	 * 
	 * @param k time stamp k
	 */
	public int getStateNum(int k){
		if(0==k) return 1;
		return gfsList.get(k).macs.size();
	}
	
	/**
	 * i-th state at time k
	 * @param ki: i-th state
	 */
	public MacroState  getState(int k,int ki){
		return gfsList.get(k).macs.get(ki);
	}
	
	/**
	 * get GFStateItem at time k
	 * @param k: time k
	 */
	public GFStatesItem getStatesItem(int k){
		return gfsList.get(k);
	}

}
