package org.tamgeniue.prediction;

import org.tamgeniue.grid.Configuration;
import org.tamgeniue.model.roi.RoICell;

import java.util.ArrayList;
import java.util.HashSet;

public class StateGridFilter {

	private static final double alphaJ_k_k_1=0.1;//the weight for J_k_k_1, while (1-alphaJ_k_k_1) is the weight of C_k_k_1

	public GFStates gfStates;
	 
	 public StateGridFilter(){
		 gfStates=new GFStates();
		
	 }

	 public double getMaxDelta(){
		 return gfStates.getMaxDelta();
	 }
	 
	 public GFStatesItem GenerateGFState(ArrayList<ArrayList<MacroState>> dendrogram,double p,int k) {
		int level=dendrogram.size();
		if(0==level) {
			//System.out.println("for debug: level is 0");
			return null;
		}
		
		GFStatesItem preGFS=null;
		
		//for debug
		//System.out.println("time off:"+k+"+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		
		double maxStateScore=-1;
		
		while(level>0){
		level--;
		
		GFStatesItem gfs=new GFStatesItem();
		
		gfs.addMacStates(dendrogram.get(level));
		
		double[][] p_x_k_x_k_1=getP_X_k_X_k_1(gfs,k);
		double[] p_z_k_x_k=getP_Z_k_X_ki(gfs);
		gfs.addGFSat(p_x_k_x_k_1, p_z_k_x_k);
		
		double[] W_kk=comW_kkItem(gfs,k);
		gfs.addGFW(W_kk);
		
		//for debug
		//System.out.println("level:"+level);
		//System.out.println(gfs.toSring());
		//end for debug
		if(gfs.statCondition(p)){
			double score=gfs.getScore();
			if(score>maxStateScore){
				maxStateScore=score;
				preGFS=gfs;
			}
		}else{
			break;
		}
		}
		assert(null!=preGFS);
		return preGFS;
	}

	private double[] comW_kkItem(GFStatesItem ls,int k){
		double[] w_kk=new double[ls.getStateNum()];
		
		double[] w_k_k_1=getW_k_k_1Item(ls,k);
		
		int N_ks=ls.getStateNum();
		double denominator=0;
		double[] p_Z_k_X_k=ls.P_z_k_x_k;
		for(int j=0;j<N_ks;j++){
			double item=w_k_k_1[j]*p_Z_k_X_k[j];
			w_kk[j]=item;
			denominator+=item;
		}
		
		for(int i=0;i<N_ks;i++){
			//double numerator=w_k_k_1[i]*P_Z_k_X_ki(k,i);
			w_kk[i]/=denominator;
		}	
		
		return w_kk;
	}

	private double getW_k_k_1_i_Item(GFStatesItem ls,int k,int i){
		
		int n_k_1_s;
		double[] w_k_1;
		if(1==k) {
			n_k_1_s=1;
			w_k_1=new double[1];
			w_k_1[0]=1;
		}
		else{ 
			n_k_1_s=gfStates.getStateNum(k-1);
			w_k_1=gfStates.getStatesItem(k-1).w;
		}
		
		
		
		double  [][] p_x_k_x_k_1=ls.P_x_k_x_k_1;
		double sum=0;
		
		for(int j=0;j<n_k_1_s;j++){
			double item=w_k_1[j]*p_x_k_x_k_1[i][j];
			sum+=item;
		}
		return sum;
	}
	
	private double[] getW_k_k_1Item(GFStatesItem ls,int k){
		double[] w_k_k_1=new double[ls.getStateNum()];
		
		for(int i=0;i<ls.getStateNum();i++){
			w_k_k_1[i]=getW_k_k_1_i_Item(ls,k,i);
		}
		
		return w_k_k_1;
	}
	
	
	
	/**
	 * compute p(z_k|x_ki), return an array, and n is the number of states
	 */
	private double[] getP_Z_k_X_ki(GFStatesItem ls){
		double [] res=new double[ls.getStateNum()];
		double sum=0;
		for(int i=0;i<ls.getStateNum();i++){
			res[i]=getP_Z_k_X_kiPropto(ls,i);
			sum+=res[i];
		}
		
		for(int i=0;i<ls.getStateNum();i++){
			res[i]/=sum;
		}
		
		return res;
	}
	
	
	/**
	 * Compute p(z_k|x_ki), without normalization
	 */
	public double getP_Z_k_X_kiPropto(GFStatesItem ls,int i){
		
		double p_xki_zk=this.getP_X_ki_Z_k(ls, i);
		double p_z_k=1;
		double p_x_ki=this.getP_X_ki(ls, i);
		
		return p_xki_zk*p_z_k/p_x_ki;
	}
	
	
	private double getP_X_ki(GFStatesItem ls,int i){
		return ls.beta[i];
	}
	
	private double getP_X_ki_Z_k(GFStatesItem ls, int ki){
		if(!Configuration.doSelfCorrection){
		double denominator=ls.getTraSumNum();
		double numerator=ls.getTraNumPerState( ki);
		
		return numerator/denominator;
		} else{
			return getP_X_ki_Z_k_withLifetime(ls,ki);
		}
	}
	
	private double getP_X_ki_Z_k_withLifetime(GFStatesItem ls, int ki){
		double denominator=ls.getTraSumLifetime();
		double numerator=ls.getTraLifetimePerState(ki);
		return numerator/denominator;
	}

	//+++++++++++++++++++++++++++++++++++++++++++
	private double[][] getP_X_k_X_k_1(GFStatesItem ls,int k){
		int k_1_num;
		if(1==k) k_1_num=1;
		else {
			k_1_num=gfStates.getStateNum(k-1);
		}
		
		int k_num=ls.getStateNum();
		
		double [][] p_k_k_1=new double[k_num][k_1_num];
		
		for(int i=0;i<k_num;i++){
			p_k_k_1[i]=new double[k_1_num];
			
			for(int j=0;j<k_1_num;j++){
				double J_X=getJ_XK_XK_1(ls,k,i,j);
				double C_X=getC_XK_XK_1(ls, k, i, j);
				p_k_k_1[i][j]=Math.exp(alphaJ_k_k_1*J_X+(1-alphaJ_k_k_1)*C_X)-1;
			}
		}
		
		//normalize
		for(int j=0;j<k_1_num;j++){
			double sum=0;
			for(int i=0;i<k_num;i++){
				sum+=p_k_k_1[i][j];
			}
			for(int i=0;i<k_num;i++){
				if(0==sum){
					p_k_k_1[i][j]=0;
				}else{
				p_k_k_1[i][j]/=sum;
				}
			}
		}
		
		
		return p_k_k_1;
	}

	private double getJ_XK_XK_1(GFStatesItem ls,int k,int ki,int k_1j){
		
		if(1==k) return 0;
		
		MacroState x_k_i=ls.getState(ki);
		MacroState x_k_1_j=gfStates.getState(k-1,k_1j);
		
		HashSet<RoICell> union=new HashSet<>();
		
		
		union.addAll(x_k_i.LC);
		union.addAll(x_k_1_j.LC);
		
		double cat_inter=x_k_i.LC.size()+x_k_1_j.LC.size()-union.size();
		double cat_union=union.size();
		
		return (cat_inter/cat_union);
		
	//	return 0;
	}
	
    private double getC_XK_XK_1(GFStatesItem ls,int k,int ki,int k_1j){
		
		if(1==k){
			double denominator=ls.getTraSumNum();
			double numerator=ls.getTraNumPerState(ki);
			
			return numerator/denominator;
		}
		
		//ArrayList<Integer> z_k_i=ls.getState(ki).getLTArray().LT;
		//ArrayList<Integer> z_k_1_j=gfStates.getState(k-1, k_1j).LT;
		
		ArrayList<Integer> z_k_i=ls.getState(ki).getLTArray();
		ArrayList<Integer> z_k_1_j=gfStates.getState(k-1, k_1j).getLTArray();
		
		HashSet<Integer> union=new HashSet<>();
		
		union.addAll(z_k_i);
		union.addAll(z_k_1_j);
		
		double cat_inter=z_k_i.size()+z_k_1_j.size()-union.size();
		double cat_union=union.size();
		
		return (cat_inter/cat_union);
		
	}
	
	public static void testCase1(){
		
		StatesDendrogram sd=AgglomerativeCluster.testCase1();
		ArrayList<ArrayList<MacroState>> acRes1=sd.macsTree;
		StateGridFilter sGF=new StateGridFilter();
		GFStatesItem first= sGF.GenerateGFState(acRes1,0.5,1);
		System.out.println(first.macs.size());
		
		System.out.println(first.toSring());
	}
	
	public static void  testCase2(){
		StatesDendrogram sd1=AgglomerativeCluster.testCase1();
		ArrayList<ArrayList<MacroState>> acRes1=sd1.macsTree;
		StateGridFilter sGF=new StateGridFilter();
		GFStatesItem first= sGF.GenerateGFState(acRes1,0.5,1);
		sGF.gfStates.addStatesItem(first);
		//System.out.println(first.macs.size());
		
		StatesDendrogram sd2=AgglomerativeCluster.testCase2();
		ArrayList<ArrayList<MacroState>> acRes2=sd2.macsTree;
		//System.out.println(acRes2.get(1).size());
		
		GFStatesItem sec= sGF.GenerateGFState(acRes2,0.8,2);
		
	     sGF.gfStates.addStatesItem(sec);
		
		System.out.println("MAP:"+sGF.gfStates.max_delta);
		
		int[] path=sGF.gfStates.MAPTraceBack();
		
		System.out.println("path");
        for (int aPath : path) {
            System.out.println(aPath);
        }
		
		ArrayList<MacroState> mp=sGF.gfStates.getMacroStatePath();
		System.out.println("Macro state path");
		for(int i=1;i<mp.size();i++){
			System.out.println(mp.get(i).getCenter()[0]+" "+mp.get(i).getCenter()[1]);
		}
		
		System.out.println("first");
		System.out.println(first.toSring());
		
		System.out.println("second");
		System.out.println(sec.toSring());
	}
	//====================================================
	
	
	
	public static void main(String args[]){
		testCase1();
		testCase2();
	}
}



