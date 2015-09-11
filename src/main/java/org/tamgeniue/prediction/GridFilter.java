package org.tamgeniue.prediction;

import org.tamgeniue.model.grid.Grid;
import org.tamgeniue.model.roi.RoICell;
import org.tamgeniue.model.roi.RoIState;
import org.tamgeniue.traStore.TraStoreListItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

//import utl.fastbinomial.FastBinomial;

public class GridFilter {

    public TimeTraState timeTraState=null;
	
    private double[] p_z;
    
    private double w_kk[][];
    
    //for MAP computation
    private double delta_ki[][];//record the map
    private int psi_ki[][];//record the path
    private int MAPPath[];
    private double MAPPro;


    private static final double alphaJ_k_k_1=0.1;//the weight for J_k_k_1, while (1-alphaJ_k_k_1) is the weight of C_k_k_1


	/**
	 */
	public GridFilter(Grid inGrid,
			Hashtable<Integer, ArrayList<TraStoreListItem>> inQueryResult,
			double inLat0, double inLng0, double inStep) {

        timeTraState=new TimeTraState(inGrid, inQueryResult,
			 inLat0,  inLng0,  inStep);

		//long startComP_Z=System.currentTimeMillis(); //��ȡ��ʼʱ��
		compP_Z();
		//long endComP_Z=System.currentTimeMillis();
		//System.out.println("time for compP_Z: "+(endComP_Z-startComP_Z));
		
		
		
		//compW_kk();
		
		//comp_MAP();
		MAPPro=-1;
	}

	/**
	 * estimate the MAP path with the given future query time stamp 
	 */
	public double MAPEsitmation(){
		MAPPro= comp_MAP(0);
		return MAPPro;
	}
	
	public void GridEstimation(){
		compW_kk();
	}
	
	/**
	 * predict the MAP path with a give propability, 
	 * i.e. if the MAP probability is less than pro, we terminate the estimation of the path
	 */
	public double MAPEstimationThreshold(double pro){
		MAPPro= comp_MAP(pro);
		return MAPPro;
	}
	
	private double[][] P_X_k_X_k_1(int k){

		int k_1_num=0;
		
		if(1==k) k_1_num=1;
		else k_1_num=timeTraState.getStateNum(k-1);
		
		int k_num=timeTraState.getStateNum(k);
		
		double [][] p_k_k_1=new double [k_num][k_1_num];
		
		for(int i=0;i<k_num;i++){
			p_k_k_1[i]=new double[k_1_num];
			
			for(int j=0;j<k_1_num;j++){
				p_k_k_1[i][j]=Math.exp(alphaJ_k_k_1*J_XK_XK_1(k,i,j)+(1-alphaJ_k_k_1)*C_XK_XK_1(k,i,j))-1;
			}
		}
		
		////normalization
		double sum=0;
		for(int j=0;j<k_1_num;j++){
			sum=0;
			for(int i=0;i<k_num;i++){
				sum+=p_k_k_1[i][j]; 
			}
			
			for(int i=0;i<k_num;i++){
				p_k_k_1[i][j]/=sum;
			}
		}
		

		return p_k_k_1;
		
		
	}

	private double J_XK_XK_1(int k,int ki,int k_1j){
		
		if(1==k) return 0;
		
		RoIState x_k_i=timeTraState.getState(k, ki);
		RoIState x_k_1_j=timeTraState.getState(k-1,k_1j);
		
		HashSet<RoICell> union=new HashSet<RoICell>();
		
		union.addAll(x_k_i.getRoiSet());
		union.addAll(x_k_1_j.getRoiSet());
		
		double cat_inter=x_k_i.getSize()+x_k_1_j.getSize()-union.size();
		double cat_union=union.size();
		
		return (cat_inter/cat_union);
		
	//	return 0;
	}
	
	/**
	 */
	private double C_XK_XK_1(int k,int ki,int k_1j){
		
		if(1==k){
			double denominator=timeTraState.getTraNum(k);
			double numerator=timeTraState.getTraNumPerState(k, ki);
			
			return numerator/denominator;
		}
		
		HashSet<Integer> z_k_i=timeTraState.getStateTraIdBins(k,ki);
		HashSet<Integer> z_k_1_j=timeTraState.getStateTraIdBins(k-1,k_1j);
		
		HashSet<Integer> union=new HashSet<Integer>();
		
		union.addAll(z_k_i);
		union.addAll(z_k_1_j);
		
		double cat_inter=z_k_i.size()+z_k_1_j.size()-union.size();
		double cat_union=union.size();
		
		return (cat_inter/cat_union);
		
	}

	private double P_X_ki_Z_k(int k, int ki){
		
		double denominator=timeTraState.getTraNum(k);
		double numerator=timeTraState.getTraNumPerState(k, ki);
		
		return numerator/denominator;
	}
	

	private double Psi_L(int L,double [] beta){
		double sum=0;
		for(int i=L;i<beta.length;i++){
			sum+=beta[i];
		}
		return sum;
	}

	private int Gamma_k_L(int k,int L){
		int sum=0;
		
		for(int i=L;i<timeTraState.getStateNum(k);i++){
			sum+=timeTraState.getTraNumPerState(k, i);
		}
		return sum;
	}
	

	
	/**
	 * we do not compute compP_Z(), but consider it as a constant
	 */
	private void compP_Z(){
		p_z=new double[timeTraState.getTimeLength()+1];
		
		//k start from 1

		for(int k=1;k<p_z.length;k++){
			p_z[k]=1;
		}
	}
	
	/**
	 * k starts from 1
	 */
	private double getP_Z_K(int k){
		return p_z[k];
	}
	
	/**
	 */
	private double P_X_ki(int k,int i){
		return timeTraState.getBeta_k_j(k, i);
	}
	
	/**
	 * compute p(z_k|x_ki), return an array, and n is the number of states
	 */
	private double[] P_Z_k_X_ki(int k,int n){
		double [] res=new double[n];
		double sum=0;
		for(int i=0;i<n;i++){
			res[i]=P_Z_k_X_kiPropto(k,i);
			sum+=res[i];
		}
		
		for(int i=0;i<n;i++){
			res[i]/=sum;
		}
		
		return res;
	}
	
	/**
	 * Compute p(z_k|x_ki), without normalization
	 */
	public double P_Z_k_X_kiPropto(int k, int i){
		
		double p_xki_zk=this.P_X_ki_Z_k(k, i);
		double p_z_k=this.getP_Z_K(k);
		double p_x_ki=this.P_X_ki(k, i);
		
		return p_xki_zk*p_z_k/p_x_ki;
	}
	
	
	/**
	 * k starts from 1
	 */
	public void compW_kk(){
		w_kk=new double[timeTraState.getTimeLength()+1][];
		
		w_kk[0]=new double[1];
		w_kk[0][0]=1;
		
		for(int k=1;k<timeTraState.getTimeLength()+1;k++){
			comW_kkItem(k);
		}
		
	}

    private void comW_kkItem(int k){
		w_kk[k]=new double[timeTraState.getStateNum(k)];
		
		double[] w_k_k_1=W_k_k_1Item(k);
		
		int N_ks=this.timeTraState.getStateNum(k);
		double denominator=0;
		double[] p_Z_k_X_k=this.P_Z_k_X_ki(k, N_ks);
		for(int j=0;j<N_ks;j++){
			double item=w_k_k_1[j]*p_Z_k_X_k[j];
			w_kk[k][j]=item;
			denominator+=item;
		}
		
		for(int i=0;i<N_ks;i++){
			//double numerator=w_k_k_1[i]*P_Z_k_X_ki(k,i);
			w_kk[k][i]/=denominator;
		}	
	}
		
	private double W_k_k_1_i_Item(int k,int i,double [][] p_x_k_x_k_1){
		
		int n_k_1_s;
		if(1==k) n_k_1_s=1;
		else n_k_1_s=timeTraState.getStateNum(k-1);
		
		double sum=0;
		
		for(int j=0;j<n_k_1_s;j++){
			double item=w_kk[k-1][j]*p_x_k_x_k_1[i][j];
			sum+=item;
		}
		return sum;
	}
	
	private double[] W_k_k_1Item(int k){
		double[] w_k_k_1=new double[timeTraState.getStateNum(k)];
		
		double[][] p_x_k_x_k_1=this.P_X_k_X_k_1(k);
		
		for(int i=0;i<timeTraState.getStateNum(k);i++){
			w_k_k_1[i]=W_k_k_1_i_Item(k,i,p_x_k_x_k_1);
		}
		
		return w_k_k_1;
		
	}
	
	
	//+++for MAP computation
	
	/**
	 * the computation of MAP
	 */
	private double comp_MAP(double pro){
		//the following two array record the back trace. therefore, each 
		//array store T+1 number of state, i.e. at t=T+1, it record the state of
		//moving object at T
		delta_ki=new double[timeTraState.getTimeLength()+2][];
		psi_ki=new int[timeTraState.getTimeLength()+2][];
		
		delta_ki[0]=new double[1];
		delta_ki[0][0]=1;
		delta_ki[timeTraState.getTimeLength()+1]=new double[1];//the maximum likelihood
		
		psi_ki[0]=new int[1];
		psi_ki[0][0]=0;
		psi_ki[timeTraState.getTimeLength()+1]=new int[1];//the MAP state at time t=T
		
		
		double kPro=1;//record the probability of MAP path at time k
		int k=0;
		for(k=1;k<timeTraState.getTimeLength()+1;k++){
			kPro=MAPRecursion(k);//the recursion part of MAP, the return value is the maximum probablity for the MAP path at current time
			if(kPro<pro) break;
		}
		
		double maxPro[]=new double[1];
		MAPPath=MAPTraceBack(k-1,maxPro);
		return maxPro[0];
		
	}
	
	
	/**
	 * the recursion part of MAP
	 * @return return the maximum probability of the path at current timestamp k
	 */
	public double MAPRecursion(int k){
		
		int N_ks=this.timeTraState.getStateNum(k);
		delta_ki[k]=new double[N_ks];
		psi_ki[k]=new int[N_ks];
			
		double[] p_zk_xk=this.P_Z_k_X_ki(k, N_ks);//the observation likelihood function
		double[][] p_xk_xk_1=P_X_k_X_k_1(k);//the translation function
		
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
		int N_k_1s=timeTraState.getStateNum(k-1);
		//double[] item=new double[N_k_1s];
		double item=0;
		for(int j=0;j<N_k_1s;j++){
			item=delta_ki[k-1][j]*p_x_kj_x_k_1[j];
			if(item>max){
				max=item;
				max_index=j;
			}
		}
		
		delta_ki[k][ki]=max*p_zk_xkj;
		psi_ki[k][ki]=max_index;
		return delta_ki[k][ki];
	}
	
	/**
	 * 
	 * @param T  the termination time stamp, 
	 * @param outMaxIdx  the maximum state id, this is a return result, it is array with one element, in order to change the parameter value of function
	 * @param outmaxStatePro  the maximum state path probability at this time T, it is array with one element
	 */
	private void MAPTermination(int T,int[] outMaxIdx,double[] outmaxStatePro){
		
		//int T=timeTraState.getTimeLength();//the longest time stamp
		int N_ks=timeTraState.getStateNum(T);//the number of states at time t=T
		
		double max=-1;
		int max_index=-1;
		
		for(int i=0;i<N_ks;i++){
			if(max<delta_ki[T][i]){
				max=delta_ki[T][i];
				max_index=i;
			}
		}
		delta_ki[T+1][0]=max;
		psi_ki[T+1][0]=max_index;
		
		outMaxIdx[0]=max_index;
		outmaxStatePro[0]=max;
	}
	
	public int[] MAPTraceBack(int T, double[] outMaxPro){
		
		//int T=timeTraState.getTimeLength();
		int seedIdx[]=new int[1];
		double maxStatePro[]=new double[1];
		
		MAPTermination(T,seedIdx,maxStatePro);
		
		outMaxPro[0]=maxStatePro[0];
		
		int[] path=new int [T+1];//the one more position is left for virtual point at time t=0
		path[T]=seedIdx[0];
		
		for(int t=T-1;t>=0;t--){
			path[t]=psi_ki[t+1][path[t+1]];
		}
		return path;
	}
	
	/**
	 * for debug
	 */
	public void visitTimeTraState(){
		timeTraState.visitTimeQueryRes();
		timeTraState.visitRoIStateSet();
	}
	
	public void visitW_kk(){
		System.out.println("visit w_kk");
		for(int k=0;k<this.timeTraState.getTimeLength()+1;k++){
			System.out.println("w_kk["+k+"]// at time "+k);
			for(int i=0;i<w_kk[k].length;i++){
				System.out.print(" w_kk["+k+"]"+"["+i+"]:"+ w_kk[k][i]);
			}
			System.out.println();
		}
	}
	
	public void visitMAPPath(){
		System.out.println("MAP path with probability:"+MAPPro);
		for(int i=1;i<MAPPath.length;i++){
			System.out.println("time:"+i+" pos x:"+timeTraState.getState(i,MAPPath[i]).getCenterX()+" y:"+timeTraState.getState(i, MAPPath[i]).getCenterY());
		}
	}
	
	public int[] getMAPPath(){
		return MAPPath;
	}
	
}
