package org.tamgeniue.dataloader;


import org.springframework.stereotype.Component;

import java.sql.*;

@Component
public class SQLiteDriver {
	private Connection conn;
    private ResultSet rs;
	
	/**
	 * Open DB, togather with closeDB and exeSQL
	 */
	public void openDB(String db){
		try {
			Class.forName("org.sqlite.JDBC");
		
		String conStr="jdbc:sqlite:"+db;
		 conn = DriverManager.getConnection(conStr);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
    }
	
	/**
	 * close DB, with openDB and exeSQL
	 */
	public void closeDB(){
		try {
            conn.close();
            rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * with opendb and closedb
	 */
	public void exeSQL(String sql){
		try{
		 Statement stat = conn.createStatement(); 
		
		 rs = stat.executeQuery(sql);
		}catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public double[] getMaxMinNum(String db,String table,String timeStart,String timeEnd){
		double[] res=new double[5];
		try {
			openDB( db);
			 String sql="select min(lat),min(lng),max(lat),max(lng),count(distinct  id) from "+table
			 + " where time>time(\""+timeStart+"\") and time<time(\""+timeEnd+"\")";
			 exeSQL(sql);
			 while(rs.next()){
				 res[0]=rs.getDouble(1);
				 res[1]=rs.getDouble(2);
				 res[2]=rs.getDouble(3);
				 res[3]=rs.getDouble(4);
				 res[4]=rs.getInt(5);
			 }
			 closeDB();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	
	//important, by time order
	public void loadWxgVideoTraDB(String table, int timeStart,int timeEnd){
		String sql= "select * from "+
		 table+ " where t>"+timeStart+" and t<"+timeEnd+" order by t asc";
		exeSQL(sql);
	}
	
	//by time order
	public void loadBBFOld(String table, int timeStart, int timeEnd){
		String sql= "select id,seq,x,y,t from "+
		 table+ " where t>"+timeStart+" and t<"+timeEnd+" order by t asc";
		exeSQL(sql);
	}
	
	public int[] getMITTraStartEndId(String db,String table, int timeStart, int timeEnd){
		int[] res= new int[2];
		try {
			openDB(db);
			 String sql="select min(id),max(id) from "+table
			 + " where t>"+timeStart+" and t<"+timeEnd+"";
			 
			 exeSQL(sql);
			
			 while(rs.next()){
				 res[0]=rs.getInt(1);
				 res[1]=rs.getInt(2);
			 }
			 closeDB();
		} catch (SQLException e) {
			res=null;
			e.printStackTrace();
		}
		return res;
	}
	
	public int getSeconds(String str){
		String[] resStr=str.split(":|,");
		int h=Integer.parseInt(resStr[0]);
		int m=Integer.parseInt(resStr[1]);
		int s=Integer.parseInt(resStr[2]);
		
		return h*3600+m*60+s;
	}

    public ResultSet getRs() {
        return rs;
    }

}
