package org.tamgeniue.domain;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tamgeniue.model.dataloading.MoveObjCache;

import java.sql.*;

@Component
public class BbfDAO {
    @Value("${config.db.path}")
    String db;
    @Value("${config.db.table.name}")
    String table;
	private Connection conn;
    private ResultSet rs;

    public void loadData(MoveObjCache moveObjCache, int startTime,int endTime){
        openDB();
        String sql= "select id,seq,x,y,t from "+
                table+ " where t>"+startTime+" and t<"+endTime+" order by t asc";
        exeSQL(sql);
        try {
            while (rs.next()) {
                int t=rs.getInt("t");
                int id=rs.getInt("id");
                int x=rs.getInt("x");
                int y=rs.getInt("y");
                moveObjCache.update(id, x, y, t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();
    }

	public double[] getMaxMinNum(String timeStart,String timeEnd){
		double[] res=new double[5];
		try {
			openDB();
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
	
	public int[] getMITTraStartEndId(int timeStart, int timeEnd){
		int[] res= new int[2];
		try {
			openDB();
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

    /**
     * Open DB, togather with closeDB and exeSQL
     */
    private void openDB(){
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
    private void closeDB(){
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
    private void exeSQL(String sql){
        try{
            Statement stat = conn.createStatement();

            rs = stat.executeQuery(sql);
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }



}
