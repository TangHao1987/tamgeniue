package org.tamgeniue.dataloader;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tamgeniue.grid.Configuration;
import org.tamgeniue.grid.GridConverter;
import org.tamgeniue.model.grid.Grid;
import org.tamgeniue.model.roi.RoICell;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

@Component
public class DataLoader {
	double step = 0;// 4X4, four level of grid, approximate one meter width for

    private int sampleNum;
    private int sampleLen;
    private int sampleStartTime;
    private int sampleEndTime;

    private int[] sampleIdx;
	private HashMap<Integer, ArrayList<RoICell>> sampleGridHash = new HashMap<>();//in grid coordination
    @Autowired
    private MoveObjCache moveObjCache;

    @Autowired
    private GridConverter gridConverter;

    @Autowired
    private SQLiteDriver sqLiteDriver;

    public DataLoader() {
    }

    public DataLoader(int inSampleNum, int inSampleLen,
                      int inSampleStartTime, int inSampleEndTime) {
        sampleNum = inSampleNum;
        sampleLen = inSampleLen;
        sampleStartTime=inSampleStartTime;
        sampleEndTime=inSampleEndTime;
    }


    public Grid loadDataToGrid(String db, String table, int timeStart, int timeEnd){
        calibrateParams();
        setGridSampleIds( db, table, timeStart, timeEnd);
        gridConverter.setStep(step);
        return createGrid(db, table, timeStart, timeEnd);
    }

	public HashMap<Integer, ArrayList<RoICell>> getGridSampleList() {
		return this.sampleGridHash;
	}

	private void calibrateParams() {
		double xScale = Configuration.MAX_LATITUDE - Configuration.MIN_LATITUDE;
		double yScale = Configuration.MAX_LONGITUDE - Configuration.MIN_LONGITUDE;
		double maxScale = (xScale > yScale) ? xScale : yScale;
		int divided = Configuration.GRID_DIVIDED;
        step = maxScale / divided;
	}

	private void setGridSampleIds(String db,String table,int timeStart,int timeEnd) {
        int [] idse = sqLiteDriver.getMITTraStartEndId(db, table, timeStart, timeEnd);

        int startId=idse[0];
        int endId=idse[1];
        int resIdx = 0;

        sampleIdx = new int[sampleNum];

        int count = startId;
        assert (endId-startId)> sampleNum:"!((endId-startId)>sampleNum)";
        int interval = (endId-startId)/ sampleNum;

        while(count < endId){
            if (count % interval == 1 && resIdx < sampleIdx.length) {
                sampleIdx[resIdx] = count;
                resIdx++;
            }
            count++;
        }

	}

	private Grid createGrid(String db, String table, int startTime,
                            int endTime) {

		Grid g = new Grid();
        moveObjCache.init(g);

		if (this.sampleNum > 0){
			moveObjCache.setSample(this.sampleIdx, sampleLen, sampleStartTime, sampleEndTime);
		}

        sqLiteDriver.openDB(db);
        sqLiteDriver.loadBBFOld(table, startTime, endTime);
		

		try {
			while (sqLiteDriver.getRs().next()) {
				MovingObject mo = this.ParseMovingObject(sqLiteDriver.getRs());
	    		moveObjCache.update(mo.id, mo.x, mo.y, mo.t);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

  	if(null!=this.sampleGridHash){
  		this.prosSampleResList(moveObjCache.getGridSampleHashList());
  	}
        sqLiteDriver.closeDB();

		return g;
	}
	

	private void prosSampleResList(HashMap<Integer, ArrayList<RoICell>> sgh) {
        for (Entry<Integer, ArrayList<RoICell>> item : sgh.entrySet()) {
            if (item.getValue().size() >= this.sampleLen) {
                this.sampleGridHash.put(item.getKey(), item.getValue());
            }
        }
	}

	 private class MovingObject {
			private int id = -1;
			int seq = -1;
			int t = -1;
			double x = -1;
			double y = -1;
		}
	 
	 private MovingObject ParseMovingObject(ResultSet sqlRs){
		 MovingObject mo=new MovingObject();
		 try{
		 mo.t=sqlRs.getInt("t");
		 mo.id=sqlRs.getInt("id");

		 mo.x=sqlRs.getInt("x");
		 mo.y=sqlRs.getInt("y");
		 mo.seq=sqlRs.getInt("seq");
		 }catch(Exception e){
			 e.printStackTrace();
		 }
		 return mo;
	 }
}
