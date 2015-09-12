package org.tamgeniue.service.grid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.tamgeniue.grid.Configuration;
import org.tamgeniue.model.grid.GridLeafEntry;
import org.tamgeniue.model.grid.GridLeafTraHashItem;
import org.tamgeniue.model.grid.PageStartTimeItem;
import org.thirdparty.lib.storagemanager.IStorageManager;
import org.tamgeniue.utl.AlgorithmUtil;
import org.tamgeniue.utl.serialization.BinarySerializer;

import java.io.IOException;
import java.util.*;

@Service
public class GridLeafEntryService {
    @Autowired
    @Qualifier("diskStorageManager")
    IStorageManager diskStorageManager;

    private static int PAGE_ID_CUR=-10;
    private static int PAGE_ID_NIL=-1;

    public int getSizeOf(GridLeafEntry gridLeafEntry) {
        int s = -1;
        try {
            byte[] traHashByte;
            traHashByte = BinarySerializer.getByteSerialize(gridLeafEntry.getTraHash());
            int traHashSizeof = traHashByte.length;
            byte[] pageStartTimesByte = BinarySerializer.getByteSerialize(gridLeafEntry.getPageStartTimes());
            int pageStartTimesSizeof = pageStartTimesByte.length;
            int otherSize = 4 + 4 + 4 + 8;
            s = traHashSizeof + pageStartTimesSizeof + otherSize;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    public void flush(GridLeafEntry gridLeafEntry) {
        LinkedHashMap<Long, GridLeafTraHashItem> traHash = gridLeafEntry.getTraHash();
        if (traHash.size() >= Configuration.CapacityPerPage) {
            try {
                byte[] data = BinarySerializer.getByteSerialize(traHash);
                int id = diskStorageManager.storeByteArray(IStorageManager.NewPage, data);
                gridLeafEntry.increaseCountIO();

                if (gridLeafEntry.getPageStartTimes().size() >= 1) {
                    removeOldPages(gridLeafEntry);//too old disk page are discarded
                }
                gridLeafEntry.addPageStartTime(id);

                traHash.clear();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * if some pages are too old, remove them
     */
    private void removeOldPages(GridLeafEntry gridLeafEntry) {
        ArrayList<PageStartTimeItem> pageStartTimes = gridLeafEntry.getPageStartTimes();
        int count = gridLeafEntry.getOldPagesCount();
        //erase the data at the tail after shifting
        for (int i = 0; i < count - 1; i++) {
            pageStartTimes.remove(pageStartTimes.size() - 1);
        }

        //all previous data need to be erased.
        for (int i = 0; i < count - 1; i++) {
            diskStorageManager.deleteByteArray(pageStartTimes.get(i).getPageId());
        }

        //shifting, move all the data Item to the front of the list
        for (int j = count - 1; j >= 0 && j < pageStartTimes.size(); j++) {
            pageStartTimes.set(j - count + 1, pageStartTimes.get(j));
        }

        //erase the data at the tail after shifting
        for (int i = 0; i < count - 1; i++) {
            pageStartTimes.remove(pageStartTimes.size() - 1);
        }
    }

    /**
     * query recent trajectories point start or larter than forwardTiemStamp
     * this is down(towards old data) query
     */
    public ArrayList<Map.Entry<Long,GridLeafTraHashItem>> queryTimeRangeForward(GridLeafEntry gridLeafEntry,int forwardTimeStamp){
        //store the result
        ArrayList<Map.Entry<Long,GridLeafTraHashItem>> res
                =new ArrayList<>();

        //if queryTime is smaller than pageQueryStartTime, we need to read one more page
        int pageQueryStartTime=gridLeafEntry.getCurPageStartTime();
        List<PageStartTimeItem> pageStartTimes = gridLeafEntry.getPageStartTimes();
        //query result in traHash
        queryTimeRangePerTraHash(gridLeafEntry.getTraHash(), forwardTimeStamp,res);

        try{

            LinkedHashMap<Long,GridLeafTraHashItem> pageHashTra=null;
            //the most recent page is in the tail of pageStartTimes
            int idx=pageStartTimes.size()-1;

            //if(idx<0) return res;//

            //queryTime<pageQueryStartTime: if queryTime is smaller than pageQueryStartTime, we need to read one more page
            // idx>=0: there is no pages in disk, return immediately
            while(forwardTimeStamp <pageQueryStartTime&&idx>=0){
                //get item
                PageStartTimeItem pstItem=pageStartTimes.get(idx);
                assert( pstItem!=null ) :"GridLeafEntry: wrong pageStartTimes ";
                idx--;

                pageQueryStartTime=pstItem.getStartTime();//update time
                pageHashTra= loadPageHashTra(gridLeafEntry, pstItem.getPageId());//read the hashmap from disk

                assert( pageHashTra!=null ) :"GridLeafEntry: load traHash from page "+pstItem.getPageId()+" failed";

                //query result in pageHashTra
                queryTimeRangePerTraHash(pageHashTra, forwardTimeStamp, res);

            };
        }catch(Exception e){
            e.printStackTrace();
        }

        return res;
    }

    /**
     * query each traHash by queryTime, all points that are more recent(with larger timestamp) than inQueryTime
     *  are included in query result
     */
    private void queryTimeRangePerTraHash(LinkedHashMap<Long,GridLeafTraHashItem> inTraLinkedHash,int inQueryTime,
                                          ArrayList<Map.Entry<Long,GridLeafTraHashItem>> outRes){
        //visit the linkedhashmap by reverse order, more recent traId has more larger timestamp
        ListIterator<Map.Entry<Long,GridLeafTraHashItem>> list
                = new ArrayList<> (inTraLinkedHash.entrySet()).listIterator(inTraLinkedHash.size());

        while(list.hasPrevious()){
            Map.Entry<Long,GridLeafTraHashItem> item=list.previous();
            int itemTime=Configuration.getTime(item.getKey());
            //the more recent traId, the larger timestamp
            if(itemTime>=inQueryTime){
                outRes.add(item);
            }
        }
    }

    /**
     * load hashmap from page(id) into memory
     */
    private LinkedHashMap<Long,GridLeafTraHashItem> loadPageHashTra(GridLeafEntry gridLeafEntry, int id){
        try{
            byte[] data=this.diskStorageManager.loadByteArray(id);
            gridLeafEntry.increaseCountIO();
            return (LinkedHashMap<Long,GridLeafTraHashItem>)BinarySerializer.getByteDeserialize(data);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }


    /**
     * query the next cell of traId and time in current cell
     */
    public GridLeafTraHashItem queryTraIdTime(GridLeafEntry gridLeafEntry, int inTraId,int inTraTime){

        if(inTraTime>=gridLeafEntry.getCurPageStartTime()){//in case, we do not need to load disk page
            return queryTraIdTimePerTraHash(gridLeafEntry.getTraHash(), inTraId, inTraTime);
        } else{
            int pageId=getPageIdByTime(gridLeafEntry, inTraTime);//get the corresponding page id
            if(PAGE_ID_NIL==pageId) return null;

            LinkedHashMap<Long,GridLeafTraHashItem> pageTraHash=loadPageHashTra(gridLeafEntry, pageId);//load the hashmap from disk page
            return queryTraIdTimePerTraHash(pageTraHash, inTraId, inTraTime);//get query result
        }

    }

    /**
     * query a set of tra from this single cell
     */
    public ArrayList<Map.Entry<Long, GridLeafTraHashItem>> queryTraSet(GridLeafEntry gridLeafEntry,
            ArrayList<Long> inTraSet) {
        // store result
        HashMap<Long, GridLeafTraHashItem> resHash = new HashMap<>();
        // collect the entry according their page id
        HashMap<Integer, ArrayList<Long>> collect = new HashMap<>();
        if (null == inTraSet)
            return null;

        //divide the tra by their page id
        for (Long traItem : inTraSet) {
            int timeItem = Configuration.getTime(traItem)
                    + Configuration.T_Sample;//time + T_Smaple to get next time
            int itemPageId = getPageIdByTime(gridLeafEntry, timeItem);//get page id, if page id is PAGE_ID_CUR, it is stored in current tra hash

            //put this tra into collect hashmap
            ArrayList<Long> itemLongArray = collect.get(itemPageId);
            if (null == itemLongArray) {
                itemLongArray = new ArrayList<>();
                collect.put(itemPageId, itemLongArray);
            }
            itemLongArray.add(traItem);// put tra_id in the same page together
        }

        for (Map.Entry<Integer, ArrayList<Long>> pageItem : collect.entrySet()) {
            LinkedHashMap<Long, GridLeafTraHashItem> pageTraHash;

            if (PAGE_ID_CUR == pageItem.getKey()) {//return current traHash
                pageTraHash = gridLeafEntry.getTraHash();
            } else if (PAGE_ID_NIL == pageItem.getKey()) {
                pageTraHash = null;
            } else {
                pageTraHash = loadPageHashTra(gridLeafEntry, pageItem.getKey());//load from disk
            }

            //visit every item in this pageTraHash
            if (pageTraHash != null) {//if trahash cannot be found, ignore it
                for (Long key : pageItem.getValue()) {
                    int keyTraId = Configuration.getTraId(key);//id
                    int keyTime = Configuration.getTime(key)
                            + Configuration.T_Sample;//cur+T_Sample
                    GridLeafTraHashItem glItem = queryTraIdTimePerTraHash(
                            pageTraHash, keyTraId, keyTime);//get item
                    if (null != glItem) {//the end of trajectory, do not get result
                        Long newKey = AlgorithmUtil.getKey(keyTraId, keyTime);

                        resHash.put(newKey, glItem);
                    }
                }
            }
        }

        return new ArrayList<>(
                resHash.entrySet());
    }


    /**
     * query a item specified by a traId and time
     */
    private GridLeafTraHashItem queryTraIdTimePerTraHash(LinkedHashMap<Long,GridLeafTraHashItem> inTraLinkedHash,
                                                         int traId,int traTime){
        Long key= AlgorithmUtil.getKey(traId, traTime);
        return inTraLinkedHash.get(key);

    }

    /**
     *
     * @return if return PAGE_ID_CUR, current hashmap is the storage location for thus time, else, return the id for this page
     */
    private int getPageIdByTime(GridLeafEntry gridLeafEntry, int traTime){

        if(traTime>=gridLeafEntry.getCurPageStartTime()){//in case, we do not need to load disk page
            return PAGE_ID_CUR;
        }

        int pageIdIdx=this.binarySearch(traTime, gridLeafEntry.getPageStartTimes());

        if(pageIdIdx== PAGE_ID_NIL) return PAGE_ID_NIL;

        return gridLeafEntry.getPageStartTimes().get(pageIdIdx).getPageId();
    }


    private int binarySearch(int traTime,ArrayList<PageStartTimeItem> array){
        int startIdx=0,endIdx=array.size()-1,midIdx;
        if(0==array.size()) return PAGE_ID_NIL;
        int midTime,startTime,endTime;

        startTime=array.get(0).getStartTime();
        if(traTime<startTime) return PAGE_ID_NIL;


        endTime=array.get(endIdx).getStartTime();
        if (traTime>=endTime) return endIdx;

        while(endIdx-startIdx>1){
            midIdx=(startIdx+endIdx)>>1;
            midTime=array.get(midIdx).getStartTime();

            if(traTime>=midTime) startIdx=midIdx;
            else if(traTime<midTime) endIdx=midIdx;
        }

        return startIdx;

    }
}
