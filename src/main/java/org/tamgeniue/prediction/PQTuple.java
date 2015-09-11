package org.tamgeniue.prediction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public class PQTuple {
	double d;
	int host;
	int nbs;
	
	//host-> nbs; nbs is te nearest neighbor of host
	public PQTuple(int inHost,int inNeighbor,double inDist){
		host=inHost;
		nbs=inNeighbor;
		d=inDist;
	}
	
	public static void main(String args[]){
		PQTuple t1=new PQTuple(1,2,0.5);
		PQTuple t2=new PQTuple(3,4,0.6);
		
		PriorityQueue<PQTuple> pq=new PriorityQueue<PQTuple>(4,new ComparatorPQTuple());
		
		pq.add(t1);
		pq.add(t2);
		
		pq.remove(t1);
		System.out.println(pq.size());
		
	}
};

//compare distance
class ComparatorPQTuple implements Comparator<PQTuple>{

	@Override
	public int compare(PQTuple arg0, PQTuple arg1) {
		// TODO Auto-generated method stub
		if(arg0.d<arg1.d) return -1;
		if(arg0.d==arg1.d) return 0;
		if(arg0.d>arg1.d) return 1;
		return 0;
	}
	
};

class invertPQTuple{
	HashMap<Integer,ArrayList<PQTuple>> invertNbs=new HashMap<Integer,ArrayList<PQTuple>>();//record neighborhood, all the PQTuple, whose nbs is "key:nbs"
	HashMap<Integer,PQTuple> invertHost=new HashMap<Integer,PQTuple>();//record host, all the PQTuple, whose host is "key:host"
	
	public void addNbsTuple(int id,PQTuple t){
		ArrayList<PQTuple> al=invertNbs.get(id);
		if(null==al){
			al=new ArrayList<PQTuple>();
			al.add(t);
			invertNbs.put(id, al);
		}else{
			al.add(t);
		}
	}
	
	public void addHostTuple(int id, PQTuple t){
		PQTuple al=invertHost.get(id);
		invertHost.put(id, t);
	}
	
	public ArrayList<PQTuple> getNbsTuples(int id){
		return invertNbs.get(id);
	}
	
	public PQTuple getHostTuples(int id){
		return invertHost.get(id);
	}
	
	public void deleteNbsId(int id){
		invertNbs.remove(id);
	}
	public void deleteHostId(int id){
		invertHost.remove(id);
	}
	
}
