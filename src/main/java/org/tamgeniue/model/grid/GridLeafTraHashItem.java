package org.tamgeniue.model.grid;

import java.io.Serializable;

public class GridLeafTraHashItem implements Serializable{

	private static final long serialVersionUID = 3041983006808082903L;
	private int cellX;
	private int cellY;
	
	public GridLeafTraHashItem(int inCellX,int inCellY){
		cellX=inCellX;
		cellY=inCellY;
	}
	public void setCellLoc(int inCellX,int inCellY){
		cellX=inCellX;
		cellY=inCellY;
	}
	public int getCellX(){
		return cellX;
	}
	public int getCellY(){
		return cellY;
	}
	
}
