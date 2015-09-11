package org.tamgeniue.service.grid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tamgeniue.grid.*;
import org.tamgeniue.model.grid.*;
import org.tamgeniue.model.roi.RoICell;
import org.tamgeniue.model.roi.RoIState;
import org.tamgeniue.prediction.MicroState;
import org.tamgeniue.prediction.RelaxMicroState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class GridService {

    @Autowired
    private GridLeafEntryService gridLeafEntryService;


    /**
     * query all the trajectories with a RoI, the RoI is found by a seed cell(gridX,gridY) with a constraint crX and crY.
     */
    public ArrayList<Map.Entry<Long,GridLeafTraHashItem>> queryRangeTimePerCell(Grid g, int  gridX,int gridY,
                                                                                int crX,int crY,int timeRange,double threshold){

        RoIState roiState=g.findConstraintRoI(gridX, gridY, crX, crY, threshold);//find RoI

        return this.queryByRoITimeRange(g, roiState, timeRange);
    }

    /**query all the trajectories that pass a set of RoI, and the order sequence of the trajectories are roiArray[0]->roiArray[1]->roiArray[2]->.....
     * return traId from roiArray[0]->roiArray[1]->roiArray[2]->.....
     */
    public ArrayList<Map.Entry<Long,GridLeafTraHashItem>> queryByMultiRoITimeRange(Grid g, ArrayList<RoIState> roiArray){

        return queryByMultiRoITimeRange(g, roiArray,Configuration.T_period);
    }


    /**query all the trajectories that pass a set of RoI, and the order sequence of the trajectories are roiArray[0]->roiArray[1]->roiArray[2]->.....
     * return traId from roiArray[0]->roiArray[1]->roiArray[2]->.....
     */
    public ArrayList<Map.Entry<Long,GridLeafTraHashItem>> queryByMultiRoITimeRange(Grid g, ArrayList<RoIState> roiArray,int timeRange){

        //query all the trajectories at state 0
        ArrayList<Map.Entry<Long,GridLeafTraHashItem>> roiQA=this.queryByRoITimeRange(g, roiArray.get(0), timeRange);

        if(roiArray.size()==1) return roiQA;//if there is only one RoI, return result immediately.
        else{

            HashMap<Integer,Map.Entry<Long,GridLeafTraHashItem>> resHash=
                    new HashMap<Integer,Map.Entry<Long,GridLeafTraHashItem>>();

            //add the trajectories of first state into hashmap
            for(Map.Entry<Long,GridLeafTraHashItem> AItem:roiQA ){
                int AItemTraId=Configuration.getTraId(AItem.getKey());//get tra id
                int AItemTime=Configuration.getTime(AItem.getKey());
                resHash.put(AItemTraId, AItem);
            }

            //first state is computed again
            for(int i=1;i<roiArray.size();i++){
                ArrayList<Map.Entry<Long,GridLeafTraHashItem>> BItem=this.queryByRoITimeRange(g, roiArray.get(i), timeRange);
                //direction is  resHash->B
                resHash=IntersectMultiRoIQuery(resHash, BItem);
            }


            //only keep the values
            return new  ArrayList<Map.Entry<Long,GridLeafTraHashItem>>(resHash.values());
        }
    }
    /**
     * query all the trajectories that pass a cell (cellX,cellY)
     */
    public ArrayList<Map.Entry<Long,GridLeafTraHashItem>> queryByCellTimeRange(Grid g, int cellx,int celly,int timeRange){
        GridCell gc=g.getGridCell(cellx, celly);//find the corresponding leaf of this cell
        if(gc==null) return null;//if cell is empty, return null
        int st=g.getCurTime() -timeRange;
        GridLeafEntry entry =  gc.getGridLeafEntry();
        return gridLeafEntryService.queryTimeRangeForward(entry, st); //return result of leaf query
    }

    public ArrayList<Map.Entry<Long,GridLeafTraHashItem>> queryByRoITimeRange(Grid g, RoIState roiState,int timeRange){

        RoICell[] roiCells=roiState.toArray();//convert to array of RoICell

        //store the result
        ArrayList<Map.Entry<Long,GridLeafTraHashItem>> res=new ArrayList<Map.Entry<Long,GridLeafTraHashItem>>();

        for (RoICell roiCell : roiCells) {
            //find corresponding leaf( grid cell)
            GridCell gc = g.getGridCell(roiCell.getRoiX(), roiCell.getRoiY());

            //query and add to result
            if (null != gc) {
                int st = g.getCurTime() - timeRange;
                GridLeafEntry entry =  gc.getGridLeafEntry();
                res.addAll(gridLeafEntryService.queryTimeRangeForward(entry, st)); //return result of leaf query
            }
        }
        return res;//return result
    }

    /**Given a sequence of cells, then infer a sequence of RoI. Find all trajectories that pass the sequence of RoI
     * cellArray:cellArray[0]->cellArray[1]->cellArray[2]->...
     * @return   ArrayList<Entry<key,value>>  key-- traId+time;  value:(cell_x,cell_y)
     */
    public ArrayList<Map.Entry<Long,GridLeafTraHashItem>> queryRangeTimeSeqCells(Grid g, ArrayList<RoICell> cellArray,
                                                                                 int crX,int crY, int timeRange,double threshold){

        ArrayList<RoIState> roiArray=new ArrayList<>();
        for(RoICell c:cellArray){
            RoIState roi=g.findConstraintRoI(c.getRoiX(), c.getRoiY(), crX, crY, threshold);
            if(roi.getSize()<1) continue;
            roiArray.add(roi);// a sequence of RoI
        }
        if(roiArray.size()>=1){
            ArrayList<Map.Entry<Long,GridLeafTraHashItem>> res=this.queryByMultiRoITimeRange(g, roiArray, timeRange);//get trajectories
            putQCClearTime(g, res);

            return res;
        }else{
            return null;
        }

    }



    /**
     * given a list of micro state, query the grid, and get a list of query
     * result for each micro state
     *
     * @param mics
     */
    public ArrayList<RelaxMicroState> forwardQueryMics(Grid g,
            ArrayList<MicroState> mics) {
        if (null == mics||0==mics.size())
            return null;

        ArrayList<RelaxMicroState> res = new ArrayList<RelaxMicroState>();//

        for (MicroState micItem : mics) {

            RelaxMicroState micRelax = new RelaxMicroState();
            res.add(micRelax);

            // visit every cells in this micro state
            for (Map.Entry<RoICell, ArrayList<Long>> cellMicItem : (Iterable<Map.Entry<RoICell, ArrayList<Long>>>) micItem.Ltratime.entrySet()) {
                // get cell and all the tra id in this cell
                ArrayList<Long> storeQ = new ArrayList<Long>();
                //query from QueryCache firstly
                ArrayList<Map.Entry<Long, GridLeafTraHashItem>> cRes = queryQC(g, cellMicItem.getValue(), storeQ);
                if (null == cRes) {//if return null, all the data still is in storeQ
                    storeQ = cellMicItem.getValue();

                } else {
                    micRelax.addLtratime(cRes);
                }

                if (0 == storeQ.size()) {
                    continue;
                }

                // query grid cell
                GridCell gcMicItem = g.getGridCell(cellMicItem.getKey().getRoiX(), cellMicItem.getKey().getRoiY());

                // query the next cell position for all tra id in this cell
                GridLeafEntry entry = gcMicItem.getGridLeafEntry();
                ArrayList<Map.Entry<Long, GridLeafTraHashItem>> gcMicItemRes = gridLeafEntryService.queryTraSet(entry, cellMicItem.getValue());

                putQC(g, gcMicItemRes);

                micRelax.addLtratime(gcMicItemRes);// add to result
                //micRelax.Ltratime.addAll(gcMicItemRes);// add to result
            }

        }

        return res;
    }


    /**
     * Given a sequence of cells, then infer a sequence of RoI. Find all trajectories that pass the sequence of RoI
     * cellArray:cellArray[0]->cellArray[1]->cellArray[2]->...
     */
    public ArrayList<Map.Entry<Long,GridLeafTraHashItem>> queryRangeTimeSeqCells(Grid g, ArrayList<RoICell> cellArray){
        return queryRangeTimeSeqCells(g, cellArray,
                Configuration.BrinkConstraintRoI, Configuration.BrinkConstraintRoI, Configuration.T_period,Configuration.BrinkThreshold);
    }




    /**
     * insert into querycache, and also set the expire time
     */
    private void putQCClearTime(Grid g, ArrayList<Map.Entry<Long, GridLeafTraHashItem>> seqQres){
        QueryCache qc = g.getQc();
        if(null==qc||null==seqQres||0==seqQres.size()) return;

        for(Map.Entry<Long,GridLeafTraHashItem> ei:seqQres){
            int traId=Configuration.getTraId(ei.getKey());
            int time=Configuration.getTime(ei.getKey());
            qc.setCacheExpire(traId, time);
            qc.insert(traId, time, ei);


        }
    }

    /**
     * put it into querycache
     */
    private void putQC(Grid g, ArrayList<Map.Entry<Long, GridLeafTraHashItem>> gcQres){
        QueryCache qc = g.getQc();
        if(null==qc||null==gcQres||0==gcQres.size()) return;

        for(Map.Entry<Long,GridLeafTraHashItem> ei:gcQres){
            int traId=Configuration.getTraId(ei.getKey());
            int time=Configuration.getTime(ei.getKey());
            qc.insert(traId, time, ei);
        }
    }

    /**
     * query by QueryCache, the result is put in return, and outQ is used to store all the query that is not hit in cache
     */
    private ArrayList<Map.Entry<Long,GridLeafTraHashItem>> queryQC(Grid g, ArrayList<Long> q,ArrayList<Long> outQ){
        QueryCache qc = g.getQc();
        if(null==qc||qc.size()<=0) return null;
        if(null==q||q.size()<=0) return null;//return if no result
        ArrayList<Map.Entry<Long,GridLeafTraHashItem>> res=new ArrayList<Map.Entry<Long,GridLeafTraHashItem>>();//store result

        //visit every query
        for(Long item:q){
            int traId=Configuration.getTraId(item);
            int time=Configuration.getTime(item)+Configuration.T_Sample;

            Map.Entry<Long,GridLeafTraHashItem> eitem=qc.hitCache(traId, time);//get query result

            if(null!=eitem){
                res.add(eitem);//res
            } else{
                outQ.add(item);//this should be queried from storage
            }
        }

        return res;

    }

    /**
     * find the intersection of A -> B, there is a time order for those trajectories.
     * @param A
     * @param B
     */
    public HashMap<Integer,Map.Entry<Long,GridLeafTraHashItem>> IntersectMultiRoIQuery(
            HashMap<Integer,Map.Entry<Long,GridLeafTraHashItem>>  A, ArrayList<Map.Entry<Long,GridLeafTraHashItem>>  B){

        HashMap<Integer,Map.Entry<Long,GridLeafTraHashItem>> res=new HashMap<Integer, Map.Entry<Long,GridLeafTraHashItem>> ();

        for(Map.Entry<Long,GridLeafTraHashItem> BItem:B){
            int BItemTraId=Configuration.getTraId(BItem.getKey());//get tra id

            Map.Entry<Long,GridLeafTraHashItem> AItem=res.get(BItemTraId);//test whether traId is in Res, which means it is in both A and B
            if(null==AItem){
                AItem=A.get(BItemTraId);//test whether traId is in HashMap A
            }
            if(null!=AItem){//if traId is in A, or in (A and B)
                int BItemTime=Configuration.getTime(BItem.getKey());//get the time of traId at RoI state B
                int AItemTime=Configuration.getTime(AItem.getKey());//get the time of traId at RoI state A
                if(BItemTime>=AItemTime){//only add most recent traId item
                    res.put(BItemTraId, BItem);
                }
            }
        }
        return res;
    }

}
