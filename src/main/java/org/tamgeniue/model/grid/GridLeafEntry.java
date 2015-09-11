package org.tamgeniue.model.grid;

import org.tamgeniue.grid.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Hao on 9/9/2015.
 * Grid Leaf Entry Data model
 */
public class GridLeafEntry {
    //the hashmap to record the trajectory, which is resident in memory. when the size of
    //this map is larger than Configuration.capacityPerPage, we will write the whole map into a disk page
    private LinkedHashMap<Long,GridLeafTraHashItem> traHash=new LinkedHashMap<>();
    //record the start time of each disk pages
    private ArrayList<PageStartTimeItem> pageStartTimes=new ArrayList<>();

    //record the start time of current data(in memory)
    private int curPageStartTime=-1;
    //current update time
    private int curTime=-1;
    //count the total IO of this leaf
    private int countIO=0;

    public LinkedHashMap<Long, GridLeafTraHashItem> getTraHash() {
        return traHash;
    }

    public ArrayList<PageStartTimeItem> getPageStartTimes() {
        return pageStartTimes;
    }

    public void addPageStartTime(int id){
        PageStartTimeItem pstItem=new PageStartTimeItem(curPageStartTime,id);
        pageStartTimes.add(pstItem);
    }

    public int getOldPagesCount(){
        //the oldest page is in the front of the list, and the most recent page is in the tail of the list
        int duration=curTime-pageStartTimes.get(0).getStartTime();
        int count=0;

        //pay special attention to >=, if ==, still need to go ahead.
        while(duration>= Configuration.T_period){
            count++;
            if(count>=pageStartTimes.size()) break;
            duration=curTime-pageStartTimes.get(count).getStartTime()+1;
        }
        return count;
    }

    public int getCurPageStartTime() {
        return curPageStartTime;
    }

    public int getCountIO() {
        return countIO;
    }

    public void increaseCountIO() {
        this.countIO++;
    }

    /**
     * if there are updates, update the linkedhashmap, if linkedhashmap is too large, flush it into disk
     * time is the update time of traId, and cellx2 and celly2 is the next cells locaiton of traId after current update
     */
    public void append(int traId,int time,int cellx2,int celly2){
        curTime= time;
        GridLeafTraHashItem hashItem=new GridLeafTraHashItem(cellx2,celly2);
        traHash.put(getKey(traId, time), hashItem);

        if(traHash.size()==1){
            curPageStartTime= time;
        }
    }


    private static Long getKey(int traId,int time){
        Long key= (long) traId;
        key<<=32;
        key+=time;
        return key;
    }
}
