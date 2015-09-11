package org.tamgeniue.model.grid;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * note that, the key and time are current traid and time, and the location is the next location of current time
 * @author workshop
 *
 */
public class QueryCache {
	//first level trajectory Id, second level, time stamp.
	HashMap<Integer,CacheHashMap> cache;
	int hitCount;
	
	public QueryCache(){
		cache=new HashMap<>();
		hitCount=0;
	}

	public void insert(int traId,int time,Entry<Long, GridLeafTraHashItem> rc){
		CacheHashMap ci=cache.get(traId);
		if(null==ci){
			ci=new CacheHashMap();
			cache.put(traId,ci);
		}
		ci.put(time, rc);
	}

	public Entry<Long, GridLeafTraHashItem> hitCache(int traId,int time){
		CacheHashMap ci=cache.get(traId);
		if(null==ci) return null;
		Entry<Long, GridLeafTraHashItem> rc=ci.get(time);
		if(null!=rc){
			hitCount++;
		}
		return rc;
	}
	
	public void setCacheExpire(int traId,int time){
		CacheHashMap ci=cache.get(traId);
		if(null!=ci&&ci.size()>0){
			ci.setClearTime(time);
		}
	}
	
	public int size(){
		return cache.size();
	}
	
	public void clear(){
		cache.clear();
	}
	
	class CacheHashMap  extends LinkedHashMap<Integer, Entry<Long, GridLeafTraHashItem>>{
		private int clearTime=0;
		@Override
		public boolean removeEldestEntry(Entry<Integer, Entry<Long, GridLeafTraHashItem>> eldest) {
		        super.removeEldestEntry(eldest);
                return eldest.getKey() < clearTime;
        }
		
		public void setClearTime(int time){
			clearTime=time;
			
		}
		
		
		
	}
	
	public static void main(String[] args){
		QueryCache x=new QueryCache();
		CacheHashMap xm=x.new CacheHashMap();
		xm.put(1,null);
		xm.put(2,null);
		xm.setClearTime(5);
		xm.put(3, null);
		xm.put(4, null);
		HashMap<Integer,Entry<Long, GridLeafTraHashItem>> l=new HashMap<Integer,Entry<Long, GridLeafTraHashItem>>();
		l.put(5,null);
		l.put(6, null);
		xm.putAll(l);
		System.out.println(xm.toString());
	}

}
