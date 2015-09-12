package org.tamgeniue.service.prediction;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tamgeniue.grid.Configuration;
import org.tamgeniue.model.grid.Grid;
import org.tamgeniue.model.grid.GridLeafTraHashItem;
import org.tamgeniue.prediction.*;
import org.tamgeniue.service.grid.GridService;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Hao on 12/9/2015.
 */
@Service
public class PredictionService {
    @Autowired
    GridService gridService;
    @Value("${config.microState.proportion.down}")
    public double downProportion;//stop to go down to next level of the macro states
    @Value("${config.microState.proportion.map}")
    public double mapProportion;//terminate the MAP process
    @Value("${config.microState.max.radius}")
    public int maxRadius;

    public StateGridFilter predictPath(
            ArrayList<Map.Entry<Long, GridLeafTraHashItem>> startSet, Grid g) {
        //System.out.println("for debug:print path conf threashold P:"+p+" minDelta:"+minDelta+" startSet size:"+startSet.size());
        return predictPath(startSet, g, Integer.MAX_VALUE, null);
    }



    private  StateGridFilter predictPath(ArrayList<Map.Entry<Long, GridLeafTraHashItem>> startSet, Grid grid, int pathMaxLen, ArrayList<StatesDendrogram> outSDList){
        int time_k = 0;// count the future time
        StateGridFilter sgf = new StateGridFilter();//
        AgglomerativeCluster ac = new AgglomerativeCluster(grid);

        // first dendrogram got by query result
        long dtStart = System.currentTimeMillis();
        StatesDendrogram sd = ac.getDendrogram(startSet, maxRadius);
        long dtEnd = System.currentTimeMillis();
        Configuration.time_cluster+=dtEnd-dtStart;

        if(null!=outSDList){
            outSDList.add(sd);
        }

        time_k++;
        // prediction at time k==1
        dtStart = System.currentTimeMillis();
        GFStatesItem first = sgf.GenerateGFState(sd.getMacsTree(), downProportion, time_k);
        dtEnd = System.currentTimeMillis();
        Configuration.time_hmm+=dtEnd-dtStart;
        if(null==first) {
            return sgf;
        }
        sgf.gfStates.addStatesItem(first);// add to states

        // while prediction probability is larger than p, continue to predict
        boolean stopWithBreak=false;
        while (sgf.getMaxDelta() >= mapProportion) {

            // get relax micro state from grid
            dtStart = System.currentTimeMillis();
            ArrayList<RelaxMicroState> rf = gridService.forwardQueryMics(grid, sd.getMicsLevel());
            dtEnd = System.currentTimeMillis();
            Configuration.time_retrieve+=dtEnd-dtStart;
            //System.out.println("for debug time_k:"+time_k+" RelaxMicroStates size:"+rf.size());


            dtStart = System.currentTimeMillis();
            sd = ac.getDendrogramRelaxMics(rf, maxRadius);// get dendrogram
            dtEnd = System.currentTimeMillis();
            Configuration.time_cluster+=dtEnd-dtStart;


            if(null!=outSDList){
                outSDList.add(sd);
            }
            time_k++;


            dtStart = System.currentTimeMillis();
            GFStatesItem sec = sgf.GenerateGFState(sd.getMacsTree(), downProportion, time_k);// Following prediction
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
}
