package org.tamgeniue.prediction;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tamgeniue.grid.Configuration;
import org.tamgeniue.model.grid.Grid;
import org.tamgeniue.model.grid.GridLeafTraHashItem;
import org.tamgeniue.model.roi.RoICell;
import org.tamgeniue.service.grid.GridService;
import org.tamgeniue.utl.AlgorithmUtil;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * the predictor class
 * @author workshop
 *
 */
@Component
public class Predictor {
	@Autowired
    GridService gridService;

    public StateGridFilter PathPrediction(
			ArrayList<Entry<Long, GridLeafTraHashItem>> startSet, Grid g,
			double p, double minDelta, double r) {
		//System.out.println("for debug:print path conf threashold P:"+p+" minDelta:"+minDelta+" startSet size:"+startSet.size());
		return PathPrediction(startSet,  g, p,  minDelta,  r, Integer.MAX_VALUE, null);
	}
	
	public  StateGridFilter PathPrediction(
			ArrayList<Entry<Long, GridLeafTraHashItem>> startSet, Grid g,
			double p, double minDelta, double r, int maxPathLen){
		
		return PathPrediction(
				startSet, g,
				p, minDelta, r,maxPathLen, null);
	}
	
	public  StateGridFilter PathPrediction(
			ArrayList<Entry<Long, GridLeafTraHashItem>> startSet, Grid g,
			double p, double minDelta, double r, ArrayList<StatesDendrogram> outSDList){
		
		return PathPrediction(
				startSet, g, p, minDelta, r,Integer.MAX_VALUE, outSDList);
	}


	private  StateGridFilter PathPrediction(ArrayList<Entry<Long, GridLeafTraHashItem>> startSet, Grid grid,
			double p, double minDelta, double r,int pathMaxLen, ArrayList<StatesDendrogram> outSDList){
		int time_k = 0;// count the future time
		StateGridFilter sgf = new StateGridFilter();//
		AgglomerativeCluster ac = new AgglomerativeCluster(grid);

		// first dendrogram got by query result
        long dtStart = System.currentTimeMillis();
		StatesDendrogram sd = ac.getDendrogram(startSet, r);
        long dtEnd = System.currentTimeMillis();
		Configuration.time_cluster+=dtEnd-dtStart;
		
		if(null!=outSDList){											 
		outSDList.add(sd);
		}
		
		time_k++;
		// prediction at time k==1
		dtStart = System.currentTimeMillis();
		GFStatesItem first = sgf.GenerateGFState(sd.getMacsTree(), p, time_k);
		dtEnd = System.currentTimeMillis();
		Configuration.time_hmm+=dtEnd-dtStart;
		if(null==first) {
			return sgf;
		}
		sgf.gfStates.addStatesItem(first);// add to states

		// while prediction probability is larger than p, continue to predict
		boolean stopWithBreak=false;
		while (sgf.getMaxDelta() >= minDelta) {
			
			// get relax micro state from grid
			dtStart = System.currentTimeMillis();
			ArrayList<RelaxMicroState> rf = gridService.forwardQueryMics(grid, sd.getMicsLevel());
			dtEnd = System.currentTimeMillis();
			Configuration.time_retrieve+=dtEnd-dtStart;
			//System.out.println("for debug time_k:"+time_k+" RelaxMicroStates size:"+rf.size());
			
			
			dtStart = System.currentTimeMillis();
			sd = ac.getDendrogramRelaxMics(rf, r);// get dendrogram
			dtEnd = System.currentTimeMillis();
			Configuration.time_cluster+=dtEnd-dtStart;

			
			if(null!=outSDList){
			outSDList.add(sd);
			}
			time_k++;
			
			
			dtStart = System.currentTimeMillis();
			GFStatesItem sec = sgf.GenerateGFState(sd.getMacsTree(), p, time_k);// Following prediction
			dtEnd = System.currentTimeMillis();
			Configuration.time_hmm+=dtEnd-dtStart;
			
			if((null==sec)||(time_k>=pathMaxLen)){
				stopWithBreak=true;
				break;
			}
			
			sgf.gfStates.addStatesItem(sec);
		}
		
		//System.out.println("for debug: stop with break"+stopWithBreak);
		//System.out.println("for debug: state number at last step:"+sgf.gfStates.getStateNum(time_k-1));
		return sgf;
	}
	
	/**
	 * 
	 * @param followSet
	 *            : first input query set
	 * @param g
	 *            : grid map
	 * @param p
	 *            :for the generation of macro state
	 * @param minDelta
	 *            : for stopping condition of MAP
	 * @param r
	 *            : maximum radius of micro state
	 * @param inReuseSDs
	 *            : previous query result
	 * @param outSDList
	 *            : store the StatesDendrogram
	 * @return
	 */
	private StateGridFilter PathPredictionReuse(
			ArrayList<Entry<Long, GridLeafTraHashItem>> followSet, Grid g,
			double p, double minDelta, double r,
			ArrayList<StatesDendrogram> inReuseSDs,
			ArrayList<StatesDendrogram> outSDList) {

		int time_k = 0;// count the future time

		StateGridFilter sgf = new StateGridFilter();//
		AgglomerativeCluster ac = new AgglomerativeCluster(g);

		// first dendrogram got by query result. this one use direct generatio of micro states
		StatesDendrogram sd = ac.getDendrogram(followSet, r);
		if (null != outSDList) {
			outSDList.add(sd);
		}

		// prediction at time k==1
		time_k++;
		GFStatesItem first = sgf.GenerateGFState(sd.getMacsTree(), p, time_k);
		if(null==first){
			return sgf;
		}
		sgf.gfStates.addStatesItem(first);// add to states

		// while prediction probability is larger than p, continue to predict
		while (sgf.getMaxDelta() >= minDelta) {

			// get relax micro state from grid
			// ArrayList<RelaxMicroState> rf = g.forwardQueryMics(sd.micsLevel);
			time_k++;
			ArrayList<MicroState> reuseMics = null;
			if (null!=inReuseSDs&&time_k + 1 < inReuseSDs.size()) {
				
				// as reuse, should go ahead. In data generation, the time start from 1, where reuseSD start to 
				//store data from 0, therefore, time_k just go head
				reuseMics = inReuseSDs.get(time_k ).getMicsLevel();
			}

			ArrayList<MicroState> nextMics = forwardQeryMicsReuse(sd.getMicsLevel(),
					reuseMics, g, ac, r);

			sd = ac.getDendrogramMics(nextMics, r);// get dendrogram
			if (null != outSDList) {
				outSDList.add(sd);
			}
			GFStatesItem sec = sgf.GenerateGFState(sd.getMacsTree(), p, time_k);// follwing // prediction
			if(null==sec) break;
			sgf.gfStates.addStatesItem(sec);
		}
		return sgf;
	}
	
	/**
	 * query the next micro state by reuse previous result
	 * 
	 * @param inPreMics
	 *            : the previous micro state set, if without reuse, we use this
	 *            mics to query grid and get micro state
	 * @param inReuseMics
	 *            : the micro states queried by precious prediction process,
	 *            NOTED: inReuseMics are destroyed after being reused
	 * @param g
	 *            : grid
	 * @param inAC
	 *            : Agglomerative Cluster function
	 * @param r
	 *            : maximum bound of each micro state
	 */
	private ArrayList<MicroState> forwardQeryMicsReuse(
			ArrayList<MicroState> inPreMics, ArrayList<MicroState> inReuseMics,
			Grid g, AgglomerativeCluster inAC, double r) {
		
		// if there is nothing can be reused, query directly
		
		if (null == inReuseMics || 0>= inReuseMics.size()) {
			ArrayList<RelaxMicroState> rf = gridService.forwardQueryMics(g, inPreMics);
			return inAC.Releax2Mics(rf, r);// convert to MicroState
		}

			
			
		// build index, each (traId+time)-> MicroState
		HashMap<Long, MicroState> reuseMicsHash = hashTratimeMics(inReuseMics);

		for (MicroState preMic : inPreMics) {// for each previous Mics
			if (null != preMic.LTraCell && preMic.LTraCell.size() > 0) {
				// visit every trajectory id in each micro states
				Iterator<Long> preMicKeys = preMic.LTraCell.keySet().iterator();
				ArrayList<Long> preMicDelPoints=new ArrayList<Long>();
				while (preMicKeys.hasNext()) {
					Long keyItem = preMicKeys.next();// get traid+time
					int keyItemId = Configuration.getTraId(keyItem);
					int keyItemTime = Configuration.getTime(keyItem);
					Long keyNextTime = AlgorithmUtil.getKey(keyItemId,
                            keyItemTime + Configuration.T_Sample);// compute
																	// next time
					// find whether this traId+time has been stored in
					// reuseMicrostate
					MicroState keyHashMic = reuseMicsHash.get(keyNextTime);
					if (null != keyHashMic) {
						// delete thus traId+time, all the undeleted point in
						// preMics will be used to query grid
						//preMic.deletePoint(keyItem);
						preMicDelPoints.add(keyItem);
						
						// delete this traId+time, all the undeleted point in
						// this hash, will be deleted in reuse Mics, as they
						// are not part of the result of pre Mics
						reuseMicsHash.remove(keyNextTime);
					}
				}
				//delete at the same time, in order to reuse 
				for(Long preMicDelItem:preMicDelPoints){
					preMic.deletePoint(preMicDelItem);
				}
				//update density and minbound is not uesful for query
				//preMic.setMinBound();//set min Bound
				//preMic.updateDensity(g);//update density
			}
		}
		
		//filter the micro state whose size is zero
		ArrayList<MicroState> queryPreMics=new ArrayList<MicroState>();
		for(MicroState preMic:inPreMics){
			if(preMic.getSize()>0){
				queryPreMics.add(preMic);
			}
		}

		// get relax micro state from grid,quried by the rest points in preMics
		ArrayList<RelaxMicroState> rfRestPreMic = gridService.forwardQueryMics(g, queryPreMics);
		// convert to MicroState
		ArrayList<MicroState> queryMics = inAC.SplitMics(rfRestPreMic, r);

		// visit all the reuseMicHash, the elememts left in this hash are the
		// points that should not be part of
		// pre mics query result, thus, these points should be deleted from
		// reuse mics.
        // delete the points from state
        for (Entry<Long, MicroState> eLM : reuseMicsHash
                .entrySet()) {
            MicroState eLMS = eLM.getValue();
            Long eLMK = eLM.getKey();
            eLMS.deletePoint(eLMK);
        }

		//store the sum of micro states( query+ reuse)
		ArrayList<MicroState> sumMics=new ArrayList<MicroState>();
		// when delete point ,we donot update the density and the minbound, all
		// those work done by one scan
        for (MicroState reuseMic : inReuseMics) {
            if (reuseMic.getSize() >= 1) {
                reuseMic.updateDensity(g);
                reuseMic.setMinBound();
                sumMics.add(reuseMic);
            }
        }
				
		// query and reuse result merge together
		if(null!=queryMics){
			
		sumMics.addAll(queryMics);
		}

		// get the maximum large micro state

        return inAC.mergeMicroStates(sumMics, null, r);
	}
	
	/**
	 * build index for (TraId+time)-> MicroState
	 */
	private HashMap<Long,MicroState> hashTratimeMics(ArrayList<MicroState> inReuseMics){
		
		HashMap<Long,MicroState> res=new HashMap<Long,MicroState>();
		if(null==inReuseMics) return null;
		
		for(MicroState ms:inReuseMics){//visit all micro states
			if(null!=ms.LTraCell&&ms.LTraCell.size()>0){
                for (Long keyItem : ms.LTraCell.keySet()) {
                    res.put(keyItem, ms);//put into hashmap
                }
			}
		}
		
		return res;
	}
	
	/**
	 * continuous prediction by reuse cache result
	 */
	public ArrayList<StateGridFilter> continuousPathPredictionRC(
			ArrayList<RoICell> inRecentPath, int backLen, Grid g, double p,
			double minDelta, double r){
		ArrayList<StateGridFilter> resSGF=new ArrayList<StateGridFilter>();
		g.openQC();
		ArrayList<RoICell> firstRef = new ArrayList<>(
				inRecentPath.subList(0, backLen));// index backLen is exclusive,
		
		// query the result from grid for first step
		ArrayList<Entry<Long, GridLeafTraHashItem>> firstRes = gridService.queryRangeTimeSeqCells(g, firstRef);

		// store the micro state, it will be reused by next time stamp
		ArrayList<StatesDendrogram> toReuseSDList = new ArrayList<StatesDendrogram>();
		// prediction path
		StateGridFilter firstGF = PathPrediction(firstRes, g, p, minDelta, r,
				null);//noted, we do not use toReuseSDList fore experiment currently, wait for forever...
		
		resSGF.add(firstGF);
		
		// start from time k, do prediction for eahc time step k
		for (int k = 1; k < inRecentPath.size() - backLen + 1; k++) {
			// for k to k+backLen-1, k is inclusive, and k+backLen is exclusive
			// get the recent trajectory, trace back trajectory
			ArrayList<RoICell> followRef = new ArrayList<>(
					inRecentPath.subList(k, k + backLen));
			ArrayList<Entry<Long, GridLeafTraHashItem>> followRes = gridService.queryRangeTimeSeqCells(g, followRef);// firstly, query result
					
			// store the micro state for this time,it will be reused by next
			// time stamp
			
			ArrayList<StatesDendrogram> toReusefollow = new ArrayList<StatesDendrogram>();
			// prediction
			StateGridFilter sgfFollow = this.PathPredictionReuse(followRes, g,
					p, minDelta, r, null, null);
			
			// reuse point to this time step micro states
			toReuseSDList = toReusefollow;
			resSGF.add(sgfFollow);
		}
		g.closeQC();//for debug
		return resSGF;
		
	}

	/**
	 * make continuous prediction from 0 to len-backLen
	 * Reuse micros: of previous micro states
	 * 
	 * @param inRecentPath
	 *            :a pre-defind prediction path
	 * @param backLen
	 *            :back trace path
	 * @param g
	 *            :grid map
	 * @param p
	 *            :probability to control the state generation
	 * @param minDelta
	 *            :min probability for MAP
	 * @param r
	 *            :maximum distance threshold for micro state
	 * @return ArrayList<StateGridFilter>, the generated StateGridFilter for at
	 *          each time 0<=k<len-backLen
	 */
	public ArrayList<StateGridFilter> OLDcontinuousPathPredictionRM(
			ArrayList<RoICell> inRecentPath, int backLen, Grid g, double p,
			double minDelta, double r) {

		// result to return
		ArrayList<StateGridFilter> resSGF = new ArrayList<StateGridFilter>();

		ArrayList<RoICell> firstRef = new ArrayList<>(
				inRecentPath.subList(0, backLen));// index backLen is exclusive,
		// query the result from grid for first step
		ArrayList<Entry<Long, GridLeafTraHashItem>> firstRes = gridService.queryRangeTimeSeqCells(g, firstRef);

		// store the micro state, it will be reused by next time stamp
		ArrayList<StatesDendrogram> toReuseSDList = new ArrayList<StatesDendrogram>();
		// prediction path
		StateGridFilter firstGF = PathPrediction(firstRes, g, p, minDelta, r,
				toReuseSDList);
		resSGF.add(firstGF);

		// start from time k, do prediction for eahc time step k
		for (int k = 1; k < inRecentPath.size() - backLen + 1; k++) {
			// for k to k+backLen-1, k is inclusive, and k+backLen is exclusive
			// get the recent trajectory, trace back trajectory
			ArrayList<RoICell> followRef = new ArrayList<RoICell>(
					inRecentPath.subList(k, k + backLen));

			ArrayList<Entry<Long, GridLeafTraHashItem>> followRes = gridService
					.queryRangeTimeSeqCells(g, followRef);// firstly, query result

			// store the micro state for this time,it will be reused by next
			// time stamp
			ArrayList<StatesDendrogram> toReusefollow = new ArrayList<StatesDendrogram>();
			// prediction
			StateGridFilter sgfFollow = this.PathPredictionReuse(followRes, g,
					p, minDelta, r, toReuseSDList, toReusefollow);
			
		
			
			
			// reuse point to this time step micro states
			toReuseSDList = toReusefollow;
			resSGF.add(sgfFollow);
		}
		return resSGF;
	}

	public StateGridFilter DirectPathPrediction(ArrayList<RoICell> inRecentPath,Grid g, double p,
			double minDelta, double r){
		ArrayList<Entry<Long, GridLeafTraHashItem>>  queryFirst=gridService.queryRangeTimeSeqCells(g, inRecentPath);
		if(null==queryFirst||queryFirst.size()==0) return null;
        return PathPrediction(queryFirst, g,p,minDelta,r);
	}
	
	
	private HashMap<Integer, ArrayList<RoICell>> LoadTestCase(String testFile){
		
		HashMap<Integer,ArrayList<RoICell>> res=new HashMap<Integer,ArrayList<RoICell>>();
		try{
		FileInputStream in=new FileInputStream(testFile);
		DataInputStream ds=new DataInputStream(in);
		BufferedReader br=new BufferedReader(new InputStreamReader(ds));
		
		String str="";
		ArrayList<RoICell> tra=null;
		int id=-1;
		while((str=br.readLine())!=null){
			if(str.contains("#")){
				String idStr[]=str.split("#");
				id=Integer.parseInt(idStr[1]);
				tra=new ArrayList<>();
				if(-1 != id){
					res.put(id, tra);
				}
			
			}else{
				String locStr[]=str.split(" ");
				if(locStr.length>=2){
				int x=Integer.parseInt(locStr[0]);
				int y=Integer.parseInt(locStr[1]);
				
				RoICell rc=new RoICell(x,y);
                    assert tra != null;
                    tra.add(rc);
				}
			}
		}
		
		
		}catch(Exception e){
			e.printStackTrace();
		}
		return res;
		
		
		
	}
	
	
	

	public void printSampleList(HashMap<Integer,ArrayList<RoICell>> inList){

        for (Entry<Integer, ArrayList<RoICell>> item : inList.entrySet()) {
            System.out.println("id:" + item.getKey());

            ArrayList<RoICell> rcList = item.getValue();

            for (RoICell rc : rcList) {
                System.out.print(" x:" + rc.getRoiX() + " y:" + rc.getRoiY() + " ");
            }
            System.out.println("");
        }
		
	}
	
		
	
	public static void main(String args[]){
	
	}
	

	
}


