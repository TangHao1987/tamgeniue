package org.tamgeniue.service.dataloading;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tamgeniue.model.dataloading.MoveObjCache;
import org.tamgeniue.domain.BbfDAO;
import org.tamgeniue.grid.GridConverter;
import org.tamgeniue.model.grid.Grid;
import org.tamgeniue.model.roi.RoICell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

@Component
public class DataLoadingService {
    @Value("${config.sample.number}")
    private int SAMPLE_NUM =1000;
    @Value("${config.sample.length}")
    private int SAMPLE_LENGTH =10;
    @Value("${config.sample.time.start}")
    private int START_TIME =0;
    @Value("${config.sample.time.end}")
    private int END_TIME =15;

    //PARAMETERS
    @Value("${config.sample.grid.divided}")
    private int GRID_DIVIDED =2048;

    //LATITUDE/LONGITUDE
    @Value("${config.latitude.min}")
    private double MIN_LATITUDE;
    @Value("${config.longitude.min}")
    private double MIN_LONGITUDE;
    @Value("${config.latitude.max}")
    private double MAX_LATITUDE;
    @Value("${config.longitude.max}")
    private double MAX_LONGITUDE;


    private int[] sampleIdx;
	private HashMap<Integer, ArrayList<RoICell>> sampleGridHash = new HashMap<>();//in grid coordination
    @Autowired
    private MoveObjCache moveObjCache;

    @Autowired
    private GridConverter gridConverter;

    @Autowired
    private BbfDAO sqLiteDriver;

    public DataLoadingService() {

    }

    public Grid loadDataToGrid(int startTime, int endTime){
        calculateStep();
        setGridSampleIds(startTime, endTime);
        gridConverter.setStep(calculateStep());
        Grid g = new Grid();
        moveObjCache.init(g);
        if (this.SAMPLE_NUM > 0){
            moveObjCache.setSample(this.sampleIdx, SAMPLE_LENGTH, START_TIME, END_TIME);
        }
        sqLiteDriver.loadData(moveObjCache, startTime, endTime);
        this.prosSampleResList(moveObjCache.getGridSampleHashList());
        return g;
    }

	private double calculateStep() {
		double xScale = MAX_LATITUDE - MIN_LATITUDE;
		double yScale =MAX_LONGITUDE - MIN_LONGITUDE;
		double maxScale = (xScale > yScale) ? xScale : yScale;
		int divided = GRID_DIVIDED;
        return maxScale / divided;
	}

	private void setGridSampleIds(int timeStart,int timeEnd) {
        int [] idse = sqLiteDriver.getMITTraStartEndId(timeStart, timeEnd);

        int startId=idse[0];
        int endId=idse[1];
        int resIdx = 0;

        sampleIdx = new int[SAMPLE_NUM];

        int count = startId;
        assert (endId-startId)> SAMPLE_NUM :"!((endId-startId)>SAMPLE_NUM)";
        int interval = (endId-startId)/ SAMPLE_NUM;

        while(count < endId){
            if (count % interval == 1 && resIdx < sampleIdx.length) {
                sampleIdx[resIdx] = count;
                resIdx++;
            }
            count++;
        }

	}

	private void prosSampleResList(HashMap<Integer, ArrayList<RoICell>> sgh) {
        for (Entry<Integer, ArrayList<RoICell>> item : sgh.entrySet()) {
            if (item.getValue().size() >= this.SAMPLE_LENGTH) {
                this.sampleGridHash.put(item.getKey(), item.getValue());
            }
        }
	}

    public HashMap<Integer, ArrayList<RoICell>> getGridSampleList() {
        return this.sampleGridHash;
    }


}
