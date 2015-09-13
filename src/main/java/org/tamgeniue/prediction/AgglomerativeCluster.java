package org.tamgeniue.prediction;


import org.tamgeniue.grid.Configuration;
import org.tamgeniue.model.grid.Grid;
import org.tamgeniue.model.grid.GridCell;
import org.tamgeniue.model.grid.GridLeafTraHashItem;
import org.thirdparty.lib.kdtree.KDTree;
import org.thirdparty.lib.kdtree.KeyDuplicateException;
import org.thirdparty.lib.kdtree.KeyMissingException;
import org.thirdparty.lib.kdtree.KeySizeException;

import java.util.*;
import java.util.Map.Entry;

public class AgglomerativeCluster {
	Grid g;
	public AgglomerativeCluster(Grid inG){
		g=inG;
	}

	/**
	 * 
	 * @param queryRes: a set of query result form grid
	 */
	public StatesDendrogram getDendrogram(ArrayList<Entry<Long, GridLeafTraHashItem>> queryRes,
			double r) {
		ArrayList<MicroState> mics = InitialMicroState(queryRes, r);
        return getDendrogramMics(mics, r);
	}
	
	/**
	 * get dendrogram from micro states directly
	 */
	public StatesDendrogram getDendrogramMics(ArrayList<MicroState> mics,double r){
		ArrayList<ArrayList<MacroState>> den= buildDendrogram(mics, r);
        return new StatesDendrogram(mics,den);
	}
	
	/**
	 * @param rMics: a set of relax micro state, which is query by a set of micro states
	 */
	public StatesDendrogram getDendrogramRelaxMics(ArrayList<RelaxMicroState> rMics,double r){
		ArrayList<MicroState> maxMics= Releax2Mics( rMics, r);
		ArrayList<ArrayList<MacroState>> den= buildDendrogram(maxMics, r);//build the dendrogram
        return new StatesDendrogram(maxMics,den);
	}
	
	/**
	 * convert RelaxMicroState into Micro State by split and merge operation
	 */
	public  ArrayList<MicroState> Releax2Mics(ArrayList<RelaxMicroState> rMics,double r){
		ArrayList<MicroState> initalMics=SplitMics(rMics, r);//split the relax micro state
        return mergeMicroStates(initalMics, null, r);
	}

	private ArrayList<ArrayList<MacroState>> buildDendrogram(
			ArrayList<MicroState> mics, double r) {

		ArrayList<ArrayList<MacroState>> res = new ArrayList<>();
		if(null==mics){
			return res;
		}

		// initialize the k-d tree. It is a quad-tree in fact
		KDTree<MacroState> kt = new KDTree<>(2);// build quad-tree for
															// nearest query
		HashMap<Integer, MacroState> macs = new HashMap<>();// stateId->
																				// macro_state
		invertPQTuple invertPQT = new invertPQTuple();// invert list, index the
														// tuple in priority
														// queue
		PriorityQueue<PQTuple> pqLow = new PriorityQueue<>(mics.size(),
				new ComparatorPQTuple());// lower level pq, if this queue is 0,
											// this level is finished
		PriorityQueue<PQTuple> pqHigh = new PriorityQueue<>(mics.size(),
				new ComparatorPQTuple());// high level pq

		initialStateUpward(mics, kt, macs, invertPQT, pqLow);// initialize all

		double R = r;
		do {
			R = Configuration.AlphaRadius * R;
			
			if(R>Configuration.MaxRadius) break;

			ArrayList<MacroState> level = StateUpward(kt, macs, invertPQT,
					pqLow, pqHigh, R);

			res.add(level);

			pqLow = pqHigh;
			int c = (pqLow.size() > 1) ? pqLow.size() : 2;// minimum of pq is 2
			pqHigh = new PriorityQueue<>(c, new ComparatorPQTuple());

		} while (pqLow.size() > 1);

		return res;
	}
	
	
	/**
	 * initialize the micro state. Given a set of cells, this function create a
	 * set of micro state. there are two steps step 1) construct the micro
	 * state,2) set the minimum bound for each micro state
	 * @return a set of micro state, which is store in ArrayList
	 */
	private ArrayList<MicroState> InitialMicroState(
			ArrayList<Entry<Long, GridLeafTraHashItem>> queryRes, double r) {
		ArrayList<MicroState> res = null;
		// initialize the k-d tree. It is a quad-tree in fact

        if (queryRes != null && queryRes.size() != 0) {
            try {
                // create initial micro states from points
                KDTree<MicroState> kdTree = new KDTree<>(2);
                ArrayList<MicroState> inMics = createMicroStateList(queryRes, kdTree);
                res = mergeMicroStates(inMics, kdTree, r);// merge thus micro states to maximum
                                                // micro states

            } catch (KeySizeException | KeyMissingException e) {
                e.printStackTrace();
            }
        }

        return res;// return result
	}
	
	/**
	 * given a set of micro states, and merge them into a set of maximum micro
	 * states
	 * @param inkt
	 *            : if inkt is  null( inMics are stored in inkt at the same
	 *            time), create a new k-d tree, and store each state into k-d
	 *            tree
	 */
    public ArrayList<MicroState> mergeMicroStates(ArrayList<MicroState> microstateList, KDTree<MicroState> inkt, double r) {
        if (null == microstateList) {
            return null;
        }
        ArrayList<MicroState> res;
        try {
            HashMap<Integer, MicroState> idMsMap = new HashMap<>();
            if (null == inkt) {// if inkt is null, create a new k-d tree to store
                // all micro states
                inkt = new KDTree<>(2);
                for (MicroState inMic : microstateList) {
                    insertKDTree(inMic, idMsMap, inkt);
                }
            }
            for (MicroState inMic : microstateList) {
                idMsMap.put(inMic.id, inMic);
            }

            assert (idMsMap.size() == inkt.size());

            while (idMsMap.size() > 0) {
                // get one micro state from hashmap
                Collection<MicroState> col = idMsMap.values();
                Iterator<MicroState> msIterator = col.iterator();
                MicroState currentMs = msIterator.next();
                // if it cannot be expanded, it will not be added into the map
                msIterator.remove();
                // ml.remove(col_e.id);

                // query all the possible micro states
                List<MicroState> msSets = inkt.nearestEuclidean(currentMs.getCenter(),
                        2 * r - currentMs.minBound);
                if ((msSets == null) || (msSets.size() == 0)) {
                    continue;
                }

                // visit all the possible nearest neighbors by iterator
                MicroState[] microStateArray = msSets.toArray(new MicroState[1]);

                for (int j = microStateArray.length - 1; j >= 0; j--) {
                    MicroState nbs = microStateArray[j];
                    if (nbs.id == currentMs.id) {
                        continue;
                    }
                    // if true, this state is expanded
                    if (nbs.getDisCenter(currentMs) - nbs.minBound - currentMs.minBound <= 2 * r) {
                        // delete this micro state, as it will be changed
                        // col_e has been removed from hashmap ml
                        inkt.delete(currentMs.getCenter());
                        // delete nbs, as it will be absorbed
                        inkt.delete(nbs.getCenter());
                        // remove it from hashmap ml
                        idMsMap.remove(nbs.id);
                        idMsMap.remove(currentMs.id);

                        MicroState ms = new MicroState(Configuration.getStateId(),
                                currentMs, nbs);
                        insertKDTree(ms, idMsMap, inkt);
                        break;
                    }

                }
            }
        } catch (KeySizeException | KeyMissingException e) {
            e.printStackTrace();
        }

        if (inkt.size() >= Configuration.minNumPerMic) {
            res = inkt.toArrayListValue();// to arrayList
            return res;
        } else {
            return null;
        }
    }

	/**
	 * given a set of query result, initialize them into a set of micro states.
	 * Most of time, just initialize each point as a state however, some points
	 * are within the same cells are merge
     *
	 * @param kdTree: if outkt is not null, the micro states also are stored into k-d tree
	 * @throws KeySizeException
	 * @throws KeyMissingException
	 */
	private ArrayList<MicroState> createMicroStateList(
            ArrayList<Entry< Long, GridLeafTraHashItem>> queryRes, KDTree<MicroState> kdTree) throws KeySizeException,
			KeyMissingException {
        for (Entry<Long, GridLeafTraHashItem> queryRe : queryRes) {
            int x = queryRe.getValue().getCellX();
            int y = queryRe.getValue().getCellY();
            GridCell cell = g.getGridCell(x, y);
            MicroState microState = new MicroState(Configuration.getStateId());
            microState.addPoint(x, y, cell.density, queryRe);
            insertKDTree(microState, null, kdTree);
        }
        return kdTree.size()>2?kdTree.toArrayListValue():null;
	}

	/**
	 * split the relax micro state into a ast of micro states
	 */
	public ArrayList<MicroState> SplitMics(ArrayList<RelaxMicroState> rMics,double r){
		
		if(null==rMics||0==rMics.size()){
			return null;
		}
		
		ArrayList<MicroState> res=new ArrayList<>();//store result
		
		for(RelaxMicroState rmic:rMics){
			ArrayList<MicroState> rmicResItem=rmic.generateMics(r, g);//each relax mics are splited by themselves
			if(null!=rmicResItem){
				res.addAll(rmicResItem);
			}
		}
		
		return res;
	}
	
	/**
	 * go to up level to get good state
	 */
	public ArrayList<MacroState> StateUpward(KDTree<MacroState> kt,HashMap<Integer, MacroState> macs,
			invertPQTuple invertPQT,PriorityQueue<PQTuple> pqLow,PriorityQueue<PQTuple> pqHigh,
			double R) {

			while (pqLow.size() > 0) {//if there are state can be merged
				PQTuple closest = findMergable(macs, pqLow, pqHigh, R);
				if (null == closest)//cannot find any mergable state,null is possible
					break;

				MacroState H = Agglomeration(macs, closest, kt);//generate new larger state
				assert(null!=H);
				if(kt.size()>1){//if the pqLow is smaller or equal to 1, we do not need to do reflection
				Reflection(H, closest, macs, invertPQT, kt, pqLow, pqHigh);
				}else{
					break;
				}

			}

		return 	kt.toArrayListValue();//to arrayList
		

	}
	
	/**
	 * the update of priority queue
	 */
	private void Reflection(MacroState H, PQTuple inClosest,
			HashMap<Integer, MacroState> inMacs, invertPQTuple inInvertPQT,
			KDTree<MacroState> inkt, PriorityQueue<PQTuple> inoutPqLow,
			PriorityQueue<PQTuple> inPqHigh) {

		inoutPqLow.remove(inClosest);// remove host itself
		inInvertPQT.deleteHostId(inClosest.host);//remove the host from index
		
		//if any macro state point to host
		ArrayList<PQTuple> hTuples = inInvertPQT.getNbsTuples(inClosest.host);
	
		// remove all point to host
		if (null != hTuples) {
			for (PQTuple item : hTuples) {
				if (!inoutPqLow.remove(item)) {//remove from low, if not in low, remove from high
					inPqHigh.remove(item);
				}

				MacroState itemHost = inMacs.get(item.host);
				if(null!=itemHost){//itemHost may be null, as it may be deleted
					findNewTuple( itemHost, inkt,
							 inoutPqLow, inInvertPQT);
				}
			}
			inInvertPQT.deleteNbsId(inClosest.host);//delete as host has been deleted
		}
		
		//find tuple, whose host is nbs, as nbs is disappeared
		PQTuple nbsHost = inInvertPQT.getHostTuples(inClosest.nbs);
		if (null != nbsHost) {
			if (!inoutPqLow.remove(nbsHost)) {
				inPqHigh.remove(nbsHost);
			}
			inInvertPQT.deleteHostId(inClosest.nbs);
		}

		ArrayList<PQTuple> nbsTuples = inInvertPQT.getNbsTuples(inClosest.nbs);//find all tuple points to nbs, as nbs is disappeared

		// remove all point to nbs
		if (null != nbsTuples) {
			for (PQTuple item : nbsTuples) {
				if (!inoutPqLow.remove(item)) {
					    inPqHigh.remove(item);
				}

				MacroState itemHost = inMacs.get(item.host);
				if(null!=itemHost){//itemHost may be null, as it may be deleted
					findNewTuple( itemHost, inkt,
							 inoutPqLow, inInvertPQT);
				}
			}
			
			inInvertPQT.deleteNbsId(inClosest.nbs);
		}

		//add new macro state H into PQ
		MacroState HNbs = this.getClosestMac(H, inkt);
		PQTuple HPqt = new PQTuple(H.id, HNbs.id, H.getDisCenter(HNbs));
		inoutPqLow.add(HPqt);
		inInvertPQT.addHostTuple(H.id, HPqt);
		inInvertPQT.addNbsTuple(HNbs.id, HPqt);

	}
	
	/**
	 * find a new clost tuple for a host, and update the corresponding DS
	 */
	private void findNewTuple(MacroState itemHost, KDTree<MacroState> inkt,
			PriorityQueue<PQTuple> inoutPqLow, invertPQTuple inInvertPQT) {
		
		MacroState itemNbs = this.getClosestMac(itemHost, inkt);
		PQTuple itemPqt = new PQTuple(itemHost.id, itemNbs.id,
				itemHost.getDisCenter(itemNbs));
		inoutPqLow.add(itemPqt);//add to lowPQ
		
		inInvertPQT.addHostTuple(itemHost.id, itemPqt);//add to invert index
		inInvertPQT.addNbsTuple(itemNbs.id, itemPqt);//add to invert index
		
		

	}
	
	/**
	 * find the nearest macro state of host
	 */
	private MacroState getClosestMac(MacroState host, KDTree<MacroState> inkt) {

		List<MacroState> stj;
		try {
			stj = inkt.nearest(host.getCenter(), 2);// find 2, exception itself
			assert (stj != null);
			MacroState a[] = new MacroState[1];
			MacroState stjA[] = stj.toArray(a);
			
			int ni=getNearestState( stjA, host);
			return stjA[ni];
		} catch (KeySizeException | IllegalArgumentException e) {
			e.printStackTrace();
		}

        return null;
	}
	
//	private MicroState getClosestMic(MicroState host,KDTree<MicroState> inkt){
//		List<MicroState> stj;
//		try{
//			stj=inkt.nearest(host.getCenter(), 2);
//			assert(stj!=null);
//			MicroState a[]=new MicroState[1];
//			MicroState stjA[]=stj.toArray(a);
//			int ni=getNearestState(stjA,host);
//			return stjA[ni];
//		}catch (KeySizeException | IllegalArgumentException e) {
//			e.printStackTrace();
//		}
//        return null;
//	}
	
	private int getNearestState(State[] stjA,State host){
		for (int k = stjA.length - 1; k >= 0; k--) {
			State nbs = stjA[k];
			if(nbs.id==host.id) continue;//skip itself

			return k;
		}
		
		return -1;
	}

	private MacroState Agglomeration(HashMap<Integer, MacroState> inMacs,
			PQTuple inClosest, KDTree<MacroState> inkt) {

		MacroState host = inMacs.get(inClosest.host);
		MacroState nbs = inMacs.get(inClosest.nbs);

		try {
			//remove two smaller macro state from k-d tree and hashmap
			inkt.delete(host.getCenter());
			inkt.delete(nbs.getCenter());
			inMacs.remove(nbs.id);
			inMacs.remove(host.id);

			//generate new larger macro state
			MacroState com=new MacroState(Configuration.getStateId(),host,nbs);
			
			//insert into hashmap and k-d tree
			KDTreeHashInsertMac(com,inMacs,inkt);
			//inMacs.put(com.id, com);
			//inkt.replaceInsert(com.getCenter(), com);

			return com;

		} catch (KeySizeException | KeyMissingException e) {
			e.printStackTrace();
		}

        return null;

	}
	
	/**
	 * /insert into hashmap and k-d tree
	 * @throws KeySizeException
	 * @throws KeyMissingException
	 */
	public void KDTreeHashInsertMac(MacroState ms,
			HashMap<Integer, MacroState> inMacs, KDTree<MacroState> inkt)
			throws KeySizeException, KeyMissingException {

		MacroState insertMac = ms;
		try {
			if(null!=inkt){
			inkt.insert(insertMac.getCenter(), insertMac);
			}
		} catch (KeyDuplicateException e) {
			MacroState ms0 = inkt.search(ms.getCenter());
			inkt.delete(ms0.getCenter());
			insertMac = new MacroState(Configuration.getStateId(), ms, ms0);
			try {
				inkt.insert(insertMac.getCenter(), insertMac);
			} catch (KeyDuplicateException e1) {
				e1.printStackTrace();
			}
		}
		if(null!=inMacs){
		inMacs.put(insertMac.id, insertMac);
		}

	}

    public MicroState insertKDTree(MicroState microState,HashMap<Integer, MicroState> idMicroStateMap, KDTree<MicroState> kdTree)
			throws KeySizeException, KeyMissingException {
		try {
			if(kdTree != null){
			    kdTree.insert(microState.getCenter(), microState);
			}
		} catch (KeyDuplicateException e) {
			MicroState ms0 = kdTree.search(microState.getCenter());
			kdTree.delete(ms0.getCenter());
			if(idMicroStateMap != null){
                //TODO: break point test to see if possible to move below statement to outside
				idMicroStateMap.remove(ms0.id);
			}
            microState = new MicroState(Configuration.getStateId(), microState, ms0);
			try {
                kdTree.insert(microState.getCenter(), microState);
            } catch (KeyDuplicateException e1) {
				e1.printStackTrace();
			}
		}
        return microState;
	}
	
	/**
	 * find a mergable state
	 * @param inMacs: state_id->macro state
	 * @param inPqLow:
	 * @param outPqHigh:
	 * @param inR:  radius threshold
	 */
	private PQTuple findMergable(HashMap<Integer, MacroState> inMacs,
			PriorityQueue<PQTuple> inPqLow, PriorityQueue<PQTuple> outPqHigh,
			double inR) {

		while (inPqLow.size() > 1) {
			PQTuple pqt = inPqLow.poll();
			MacroState host = inMacs.get(pqt.host);
			MacroState nbs = inMacs.get(pqt.nbs);

			double sr = MacroState.getRadius(host, nbs);
			double cr =host.getDisCenter(nbs);
			if (sr > inR||cr>Configuration.MaxStateDis) {//remove this state into high
				outPqHigh.add(pqt);
			} else {
				return pqt;
			}
		}
		return null;
	}
	
	/**
	 * upward initialize
	 * @param mics  record all micro states
	 * @param outkt index the macro state for find nearest neighbor
	 * @param outMacs record macro_state_id->macro_state
	 * @param outInvertPQT index pq
	 * @param outPQ pq
	 */
	private void initialStateUpward(ArrayList<MicroState> mics,
			KDTree<MacroState> outkt, HashMap<Integer, MacroState> outMacs,
			invertPQTuple outInvertPQT, PriorityQueue<PQTuple> outPQ) {
		try {
			//create macro state for each micro state
            for (MicroState mic : mics) {
                MacroState mac = new MacroState(Configuration.getStateId(), mic);//generate new state id
                this.KDTreeHashInsertMac(mac, outMacs, outkt);//insert into map and k-d tree
                //outkt.replaceInsert(mac.getCenter(), mac);//add it into k-d tree
                //outMacs.put(mac.id, mac);
            }

			Collection<MacroState> values = outMacs.values();//all macro state
			
			if(values.size() > 2){
			for (MacroState mac : values) {
				
				findNewTuple( mac, outkt,
						outPQ, outInvertPQT);

			}
			}
		} catch (KeySizeException | KeyMissingException e) {
			e.printStackTrace();
		}
    }
//
//	public static StatesDendrogram  testCase1(){
//		Grid g=null;
//		AgglomerativeCluster ac=new AgglomerativeCluster(g);
//
//		ArrayList<Entry<Long,GridLeafTraHashItem>> queryRes=new ArrayList<Entry<Long,GridLeafTraHashItem>>();
//
//		HashMap<Long,GridLeafTraHashItem> hm=new HashMap<Long,GridLeafTraHashItem>();
//
//		GridLeafTraHashItem gti[]=new GridLeafTraHashItem[27];
//		gti[0]=new GridLeafTraHashItem(2,2);
//		gti[1]=new GridLeafTraHashItem(2,3);
//		gti[2]=new GridLeafTraHashItem(2,4);
//		gti[3]=new GridLeafTraHashItem(2,5);
//		gti[4]=new GridLeafTraHashItem(3,2);
//		gti[5]=new GridLeafTraHashItem(3,3);
//		gti[6]=new GridLeafTraHashItem(3,4);
//		gti[7]=new GridLeafTraHashItem(3,5);
//		gti[8]=new GridLeafTraHashItem(4,2);
//		gti[9]=new GridLeafTraHashItem(4,3);
//		gti[10]=new GridLeafTraHashItem(4,4);
//		gti[11]=new GridLeafTraHashItem(4,5);
//		gti[12]=new GridLeafTraHashItem(5,2);
//		gti[13]=new GridLeafTraHashItem(5,3);
//		gti[14]=new GridLeafTraHashItem(5,4);
//		gti[15]=new GridLeafTraHashItem(5,5);
//
//		gti[16]=new GridLeafTraHashItem(9,3);
//		gti[17]=new GridLeafTraHashItem(10,3);
//		gti[18]=new GridLeafTraHashItem(10,4);
//		gti[19]=new GridLeafTraHashItem(10,5);
//		gti[20]=new GridLeafTraHashItem(10,6);
//		gti[21]=new GridLeafTraHashItem(10,7);
//		gti[22]=new GridLeafTraHashItem(9,7);
//
//
//		gti[23]=new GridLeafTraHashItem(3,7);
//		gti[24]=new GridLeafTraHashItem(4,8);
//		gti[25]=new GridLeafTraHashItem(5,9);
//		gti[26]=new GridLeafTraHashItem(6,10);
//
//		double sumx=0,sumy=0;
//
//		for(int i=0;i<27;i++){
//			Long key= AlgorithmUtil.getKey(i, i);
//			hm.put(key, gti[i]);
//		}
//
//		int start=16,end=26;
//
//		for(int i=start;i<=end;i++){
//			sumx+=gti[i].getCellX();
//			sumy+=gti[i].getCellY();
//		}
//		sumx/=(end-start+1);
//		sumy/=(end-start+1);
//
//		double radius=0;
//		for(int i=start;i<=end;i++){
//			double x=gti[i].getCellX();
//			double y=gti[i].getCellY();
//
//			double r=(x-sumx)*(x-sumx)+(y-sumy)*(y-sumy);
//
//			radius+=r;
//		}
//		ArrayList<Entry<Long,GridLeafTraHashItem>> query=new ArrayList<Entry<Long,GridLeafTraHashItem>>(hm.entrySet());
//		//System.out.println(res.size());
//		query=new ArrayList<Entry<Long,GridLeafTraHashItem>>(hm.entrySet());
//
//		return ac.getDendrogram(query, 2.58/4);
//
//	}
//
//
//	public static StatesDendrogram  testCase2(){
//		Grid g=null;
//		AgglomerativeCluster ac=new AgglomerativeCluster(g);
//
//		ArrayList<Entry<Long,GridLeafTraHashItem>> queryRes=new ArrayList<Entry<Long,GridLeafTraHashItem>>();
//
//		HashMap<Long,GridLeafTraHashItem> hm=new HashMap<Long,GridLeafTraHashItem>();
//
//		GridLeafTraHashItem gti[]=new GridLeafTraHashItem[27];
//		gti[0]=new GridLeafTraHashItem(2,2);
//		gti[1]=new GridLeafTraHashItem(2,3);
//		gti[2]=new GridLeafTraHashItem(2,4);
//		gti[3]=new GridLeafTraHashItem(2,5);
//		gti[4]=new GridLeafTraHashItem(3,2);
//		gti[5]=new GridLeafTraHashItem(3,3);
//		gti[6]=new GridLeafTraHashItem(3,4);
//		gti[7]=new GridLeafTraHashItem(3,5);
//		gti[8]=new GridLeafTraHashItem(4,2);
//		gti[9]=new GridLeafTraHashItem(4,3);
//		gti[10]=new GridLeafTraHashItem(4,4);
//		gti[11]=new GridLeafTraHashItem(4,5);
//		gti[12]=new GridLeafTraHashItem(5,2);
//		gti[13]=new GridLeafTraHashItem(5,3);
//		gti[14]=new GridLeafTraHashItem(5,4);
//		gti[15]=new GridLeafTraHashItem(5,5);
//
//		gti[16]=new GridLeafTraHashItem(9,1);
//		gti[17]=new GridLeafTraHashItem(10,1);
//		gti[18]=new GridLeafTraHashItem(10,2);
//		gti[19]=new GridLeafTraHashItem(10,3);
//
//		gti[20]=new GridLeafTraHashItem(10,7);
//		gti[21]=new GridLeafTraHashItem(10,8);
//		gti[22]=new GridLeafTraHashItem(9,8);
//
//
//		gti[23]=new GridLeafTraHashItem(3,7);
//		gti[24]=new GridLeafTraHashItem(4,8);
//		gti[25]=new GridLeafTraHashItem(5,9);
//		gti[26]=new GridLeafTraHashItem(6,10);
//
//		double sumx=0,sumy=0;
//
//		for(int i=0;i<27;i++){
//			Long key=AlgorithmUtil.getKey(i, i);
//			hm.put(key, gti[i]);
//
//		}
//
//		int start=16,end=26;
//
//		for(int i=start;i<=end;i++){
//			sumx+=gti[i].getCellX();
//			sumy+=gti[i].getCellY();
//		}
//		sumx/=(end-start+1);
//		sumy/=(end-start+1);
//
//		double radius=0;
//		for(int i=start;i<=end;i++){
//			double x=gti[i].getCellX();
//			double y=gti[i].getCellY();
//
//			double r=(x-sumx)*(x-sumx)+(y-sumy)*(y-sumy);
//
//			radius+=r;
//		}
//		ArrayList<Entry<Long,GridLeafTraHashItem>> query=new ArrayList<Entry<Long,GridLeafTraHashItem>>(hm.entrySet());
//		//System.out.println(res.size());
//		query=new ArrayList<Entry<Long,GridLeafTraHashItem>>(hm.entrySet());
//
//		return ac.getDendrogram(query, 2.58/4);
//
//	}
//
//	public static void main(String args[]){
//		Grid g=null;
//		AgglomerativeCluster ac=new AgglomerativeCluster(g);
//
//		ArrayList<Entry<Long,GridLeafTraHashItem>> queryRes=new ArrayList<Entry<Long,GridLeafTraHashItem>>();
//
//		HashMap<Long,GridLeafTraHashItem> hm=new HashMap<Long,GridLeafTraHashItem>();
//
//		GridLeafTraHashItem gti[]=new GridLeafTraHashItem[27];
//		gti[0]=new GridLeafTraHashItem(2,2);
//		gti[1]=new GridLeafTraHashItem(2,3);
//		gti[2]=new GridLeafTraHashItem(2,4);
//		gti[3]=new GridLeafTraHashItem(2,5);
//		gti[4]=new GridLeafTraHashItem(3,2);
//		gti[5]=new GridLeafTraHashItem(3,3);
//		gti[6]=new GridLeafTraHashItem(3,4);
//		gti[7]=new GridLeafTraHashItem(3,5);
//		gti[8]=new GridLeafTraHashItem(4,2);
//		gti[9]=new GridLeafTraHashItem(4,3);
//		gti[10]=new GridLeafTraHashItem(4,4);
//		gti[11]=new GridLeafTraHashItem(4,5);
//		gti[12]=new GridLeafTraHashItem(5,2);
//		gti[13]=new GridLeafTraHashItem(5,3);
//		gti[14]=new GridLeafTraHashItem(5,4);
//		gti[15]=new GridLeafTraHashItem(5,5);
//
//		gti[16]=new GridLeafTraHashItem(9,3);
//		gti[17]=new GridLeafTraHashItem(10,3);
//		gti[18]=new GridLeafTraHashItem(10,4);
//		gti[19]=new GridLeafTraHashItem(10,5);
//		gti[20]=new GridLeafTraHashItem(10,6);
//		gti[21]=new GridLeafTraHashItem(10,7);
//		gti[22]=new GridLeafTraHashItem(9,7);
//
//
//		gti[23]=new GridLeafTraHashItem(3,7);
//		gti[24]=new GridLeafTraHashItem(4,8);
//		gti[25]=new GridLeafTraHashItem(5,9);
//		gti[26]=new GridLeafTraHashItem(6,10);
//
//		double sumx=0,sumy=0;
//
//		for(int i=0;i<27;i++){
//			Long key=AlgorithmUtil.getKey(Configuration.getStateId(), i);
//			hm.put(key, gti[i]);
//
//		}
//
//		int start=16,end=26;
//
//		for(int i=start;i<=end;i++){
//			sumx+=gti[i].getCellX();
//			sumy+=gti[i].getCellY();
//		}
//		sumx/=(end-start+1);
//		sumy/=(end-start+1);
//
//		double radius=0;
//		for(int i=start;i<=end;i++){
//			double x=gti[i].getCellX();
//			double y=gti[i].getCellY();
//
//			double r=(x-sumx)*(x-sumx)+(y-sumy)*(y-sumy);
//
//			radius+=r;
//		}
//
//		System.out.println(""+Math.sqrt(radius/27));
//
//
//		ArrayList<Entry<Long,GridLeafTraHashItem>> query=new ArrayList<Entry<Long,GridLeafTraHashItem>>(hm.entrySet());
//
//		ArrayList<MicroState> res=ac.InitialMicroState(query, Math.sqrt(2)*2);
//		System.out.println(res.get(0).getDisCenter(res.get(1)));
//		System.out.println(res.get(1).getDisCenter(res.get(2)));
//
//		System.out.println(res.get(0).getDisCenter(res.get(2)));
//
//		System.out.println(MacroState.getRadius(res.get(0), res.get(1)));
//		System.out.println(MacroState.getRadius(res.get(1), res.get(2)));
//
//		//System.out.println(res.size());
//		query=new ArrayList<Entry<Long,GridLeafTraHashItem>>(hm.entrySet());
//
//		//ArrayList<ArrayList<MacroState>> macRes=ac.getDendrogram(query, 2.58/2);
//		StatesDendrogram sd=ac.getDendrogram(query, 2.59/4);
//		System.out.println(sd.getMacsTree().size());
//	}
//
}
