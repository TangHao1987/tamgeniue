package org.tamgeniue.prediction;

import java.util.ArrayList;

public class TimeMacState {
	int id;
	ArrayList<ArrayList<MacroState>> macsTimes;
	ArrayList<Integer> traNum;//remeber to change traNum if add a state to the macsTimes
	ArrayList<Double[]> beta;
	
	
	
	
	public TimeMacState(){
		macsTimes=new ArrayList<>();
		macsTimes.add(new ArrayList<MacroState>());//first one is empty,start from 1
		
		traNum=new ArrayList<>();
		traNum.add(0);
		computeTraNum();
		
		beta=new ArrayList<>();
		beta.add(new Double[1]);
		computeBeta();
		
	}
	
	private void computeTraNum(){
		for(int i=1;i<macsTimes.size();i++){
			int num= compTraNum(i);
			traNum.add(num);
		}
	}
	
	private void computeBeta(){
		for(int i=1;i<macsTimes.size();i++){
			Double [] beta_i=this.compBeta_i(i);
			beta.add(beta_i);
		}
	}
	
	private int compTraNum(int k){
		int sum=0;
		for(int i=0;i<macsTimes.get(k).size();i++){
			sum+=macsTimes.get(k).get(i).roICellHashMap.size();
		}
		return sum;
	}
	
	private Double[] compBeta_i(int k){
		Double[] beta_i=new Double[macsTimes.get(k).size()];
		
		double C=0;
		for(int i=0;i<macsTimes.get(k).size();i++){
			double sumDensity=macsTimes.get(k).get(i).SumDensity;
			double n=macsTimes.get(k).get(i).n;
			beta_i[i]=macsTimes.get(k).get(i).SumDensity/n;
			C+=beta_i[i];
		}
		
		for(int i=0;i<macsTimes.get(k).size();i++){
			beta_i[i]/=C;
		}
		
		return beta_i;
	}
	public int getStateNum(int k){
		return macsTimes.get(k).size();
	}
	
	public MacroState getState(int k,int ki){
		return macsTimes.get(k).get(ki);
	}
	
	
	
	public int getTraNum(int k){
		return traNum.get(k);
	}
	
	public int getTraNumPerState(int k,int ki){
		return macsTimes.get(k).get(ki).roICellHashMap.size();
	}
	
	public double getBeta_k_j(int k,int j){
		return beta.get(k)[j];
	}
	public int getTimeLength(){
		return macsTimes.size();
	}
}
