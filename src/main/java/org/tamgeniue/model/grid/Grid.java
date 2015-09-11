package org.tamgeniue.model.grid;

import org.tamgeniue.grid.Configuration;
import org.tamgeniue.model.roi.RoIState;

import java.io.Serializable;

public class Grid implements Serializable {

    private static final long serialVersionUID = -9215443852086117712L;
    private GridCell root;//the root of grid(i.e. quad-tree)
    private int maxIndex;//the upper bound of the grid index,this stores the maximum possible cellx and celly coordinations in the grid
    private int curTime;
    private double[][] mask;//define the mask for density update

    private static final int GRID_MAX_LEVEL = 3;

    private QueryCache qc;
    public Grid() {
        qc = null;//as default, QueryCache is closed

        initialGrid();
    }

    public void closeQC() {
        qc = null;
    }

    public void openQC() {
        qc = new QueryCache();
    }

    public QueryCache getQc(){
        return qc;
    }

    public void initialGrid() {
        setDefaultMask();//set the mask for update density
        curTime = 0;
        int nodeCount = (int) Math.pow(2, Configuration.BITS_PER_GRID);
        maxIndex = (int) Math.pow(nodeCount, Configuration.MAX_LEVEL) - 1;//compute the maximum possible cellx and celly coordinations in the grid

        this.initialGridRootNode();//create the root of the grid(quad-tree)
        //threshold=this.EstimateThreshold(x1, y1, x2, y2)
    }


    /**
     * initialize the root of the grid. the root itself is a grid cell
     */
    private void initialGridRootNode() {
        root = new GridCell(GRID_MAX_LEVEL);
    }

    /**
     * get the cell with coordinate x y.
     */
    public GridCell getGridCell(int x, int y) {
        //return gridArray[x * width + y];
        if (x < 0 || y < 0 || x >= this.maxIndex || y >= this.maxIndex)//if out of boundary, just return null
            return null;

        int level_count = root.level;//start from root
        GridCell[][] array = root.gridArray;

        int level_x;
        int level_y;
        GridCell gc = null;
        while (level_count > 0) {//go down to the bottom, if there is empty cell, return immediately. this only visits the
            //the inner node, and ignore the leaf, therefore, level_count>0

            level_count--;

            level_x = x >> (level_count * Configuration.BITS_PER_GRID);//find the grid cell by high bit
            level_y = y >> (level_count * Configuration.BITS_PER_GRID);

            gc = array[level_x][level_y];

            if (gc == null) return null;
            else {
                array = gc.gridArray;

                x -= level_x << (level_count * Configuration.BITS_PER_GRID);//minus the high bits
                y -= level_y << (level_count * Configuration.BITS_PER_GRID);

            }
        }
        return gc;
    }

    /**
     * set the mask
     */
    public void setMask(double[][] m) {
        mask = m;
        double maskSum = 0;
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                maskSum += mask[i][j];
            }

        }

        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                mask[i][j] /= maskSum;
            }
        }
    }

    /**
     * the default of mask, which is an estimation of Gaussian Distribution
     */
    private void setDefaultMask() {
        double[][] mask = new double[][]{
                new double[]{1, 2, 1},
                new double[]{2, 8, 2},
                new double[]{1, 2, 1}

        };
        setMask(mask);
    }

    public void increaseDensityDirect(int x, int y, double delta) {
        //if out of boundary, return immediately
        if (x < 0 || y < 0 || x >= this.maxIndex || y >= this.maxIndex)
            return;

        // similar code with function of public GridCell getGridCell(int x, int y), by create new cell
        //right away when find a null grid cell
        int level_count = root.level;
        GridCell[][] array = root.gridArray;

        int level_x;
        int level_y;
        GridCell gc = null;
        while (level_count > 0) {// go down to the bottom, only visits the inner node, therefore, level_count > 0

            level_count--;

            level_x = x >> (level_count * Configuration.BITS_PER_GRID);
            level_y = y >> (level_count * Configuration.BITS_PER_GRID);

            gc = array[level_x][level_y];

            if (gc == null) {
                array[level_x][level_y] = new GridCell(level_count);//create new cells
                gc = array[level_x][level_y];
            }

            array = gc.gridArray;

            x -= level_x << (level_count * Configuration.BITS_PER_GRID);
            y -= level_y << (level_count * Configuration.BITS_PER_GRID);

            //	level_count--;

        }

        assert gc != null;
        gc.density += delta;
    }


    /**
     * update the density of such cell, with mask operation, i.e., we update 9 cells at the same time
     */
    public void updateDensityMask(int x, int y) {

        for (int s = -1; s <= 1; s++) {
            for (int t = -1; t <= 1; t++) {
                increaseDensityDirect(x + s, y + t, mask[1 + s][1 + t]);
            }
        }
    }


    /**
     * a point stay here, therefore, the next location of moving object in GridLeafEntry is still the same cells
     */
    private void updatePointAndDensity(int cellx1, int celly1, int t1, int inTraId) {
        updateDensityMask(cellx1, celly1);
        GridCell gc = this.getGridCell(cellx1, celly1);
        if (gc != null) {
            gc.getGridLeafEntry().append(inTraId, t1, cellx1, celly1);

        }

    }

    /**
     * append the line to current cell, and record the next cell of this moving object,
     * note that the time for cellx2, celly2 is not necessary, as it can be computed by t2=t1+Configuration.T_sample
     */
    private void updatePointAndNext(int cellx1, int celly1, int t1, int cellx2, int celly2, int inTraId) {
        GridCell gc = this.getGridCell(cellx1, celly1);
        if (gc == null) {
            return;
        }
        gc.getGridLeafEntry().append(inTraId, t1, cellx2, celly2);
    }

    /**
     * update the density of line, from (x1,y1) to (x2,y2)
     */
    public void updateLineTra(int cellx1, int celly1, int t1, int cellx2, int celly2, int t2, int traId) {
        int k;
        double x, y, dx, dy;
        int x0, y0;
        curTime = t2;
        k = Math.abs(cellx2 - cellx1);
        if (Math.abs(celly2 - celly1) > k) k = Math.abs(celly2 - celly1);

        if (k == 0) {
            //stay in the same grid cells, without moving
            updatePointAndDensity(cellx1, celly1, t1, traId);
            return;
        }

        //DDA
        dx = (double) (cellx2 - cellx1) / k;
        dy = (double) (celly2 - celly1) / k;

        x = (double) (cellx1);
        y = (double) (celly1);

        for (int i = 0; i < k; i++) {
            x0 = (int) (x + 0.5);
            y0 = (int) (y + 0.5);
            updateDensityMask(x0, y0);

            //updateTra(x0,y0,traId,off1,t0); //we maybe do not consider such interpolation of trajectory, only increase the density

            x += dx;
            y += dy;
        }

        //update with point, only the start and end point
        updatePointAndNext(cellx1, celly1, t1, cellx2, celly2, traId);
    }


    /**
     * find constraint region of interest
     */
    public RoIState findConstraintRoI(int gridX, int gridY, int crX, int crY, double threshold) {

        int crXMin = gridX - crX;
        if (crXMin < 0) crXMin = 0;

        int crXMax = gridX + crX;
        if (crXMax > maxIndex) crXMax = maxIndex;

        int crYMin = gridY - crY;
        if (crYMin < 0) crYMin = 0;

        int crYMax = gridY + crY;
        if (crYMax > maxIndex) crYMax = maxIndex;

        RoIState roiState = new RoIState();
        //roiSet.clear();

        recursive_findRoI(gridX, gridY, crXMin, crXMax, crYMin, crYMax, threshold, roiState);

        return roiState;

    }

    private void recursive_findRoI(int gridX, int gridY,
                                   int crXMin, int crXMax, int crYMin, int crYMax, double threshold, RoIState roiState) {

        if (gridX > crXMax || gridX < crXMin) return;
        if (gridY > crYMax || gridY < crYMin) return;

        if (roiState.contains(gridX, gridY)) return;

        GridCell gc = getGridCell(gridX, gridY);
        if (gc == null || gc.density < threshold) {
            return;
        } else {
            roiState.addRoICell(gridX, gridY, gc.density);
        }
        //recursive
        recursive_findRoI(gridX, gridY + 1, crXMin, crXMax, crYMin, crYMax, threshold, roiState);
        recursive_findRoI(gridX, gridY - 1, crXMin, crXMax, crYMin, crYMax, threshold, roiState);
        recursive_findRoI(gridX - 1, gridY, crXMin, crXMax, crYMin, crYMax, threshold, roiState);
        recursive_findRoI(gridX + 1, gridY, crXMin, crXMax, crYMin, crYMax, threshold, roiState);
        recursive_findRoI(gridX + 1, gridY + 1, crXMin, crXMax, crYMin, crYMax, threshold, roiState);
        recursive_findRoI(gridX + 1, gridY - 1, crXMin, crXMax, crYMin, crYMax, threshold, roiState);
        recursive_findRoI(gridX - 1, gridY + 1, crXMin, crXMax, crYMin, crYMax, threshold, roiState);
        recursive_findRoI(gridX - 1, gridY - 1, crXMin, crXMax, crYMin, crYMax, threshold, roiState);

    }

    public int getCurTime() {
        return curTime;
    }
//
//	 public static void main(String[] args){
//		 testQueryGrid();
//	 }
//
//		 public static void testGeneral(){
//
//				try{
//				//IStorageManager diskfile = new DiskStorageManager(ps);
//
//			    ArrayList<Point> queryStore=new ArrayList<Point>();
//				Grid grid=new Grid();
//
//				Random rd=new Random(2);
//
//				int x=0,y=0;
//
//				int recordTraId=5;
//				int recordTime=-1;
//				int recordNextX=-1;
//				int recordNextY=-1;
//
//				int recordPageNextX=-1;
//				int recordPageNextY=-1;
//				int recordStopTime=10000-350;
//				int recordPageTime=-1;
//
//
//
//				long startLoad=System.currentTimeMillis();
//				for(int j=0;j<10000000;j++){
//					int cx=rd.nextInt(128);
//					int cy=rd.nextInt(128);
//
//					int oldx=x;
//					int oldy=y;
//
//					if(queryStore.size()<1000000)
//					queryStore.add(new Point(oldx,oldy));
//
//					x=(int)Math.pow(-1,cx)*rd.nextInt(128);
//					y=(int)Math.pow(-1, cy)*rd.nextInt(128);
//					int id=rd.nextInt(10);
//					if(x<0) x=-x; if(y<0) y=-y;
//					grid.updateLineTra(oldx, oldy, j, x, y, j+4, id);
//					//System.out.println("time is:"+j+" cellx:"+x+" celly:"+y);
//
//				}
//
//
//
//				long endLoad=System.currentTimeMillis();
//				System.out.println("load time is:"+(endLoad-startLoad));
//
//				long start1=System.currentTimeMillis();
//				for(Point p:queryStore){
//					grid.queryByCellTimeRange((int)p.getX(),(int)p.getY(),10000);
//				}
//				long end1=System.currentTimeMillis();
//				System.out.println("total time is:"+(double)(end1-start1));
//				System.out.println("hit data stream to hash:"+Configuration.hitCount);
//
//
//				}catch(Exception e){
//					e.printStackTrace();
//				}
//
//			}
//
//		 public static void testQueryGrid(){
//
//				try{
//				Grid grid=new Grid();
//
//				Random rd=new Random(2);
//
//				int x=0,y=0;
//				int count=0;
//				int testX,testY;
//				testX=16;
//				testY=4;
//				int dur=1000000;
//
//				int xr=32,yr=32;
//
//				int map[][] =new int[xr][];
//				for(int i=0;i<map.length;i++){
//					map[i]=new int[yr];
//					for(int j=0;j<map[i].length;j++){
//						map[i][j]=0;
//					}
//				}
//
//				for(int j=0;j<dur/2;j++){
//					int cx=rd.nextInt(4);
//					int cy=rd.nextInt(4);
//
//					cx=(int)Math.pow(-1, cx)*cx;;
//					cy=(int)Math.pow(-1, cy)*cy;
//
//					int oldx=x;
//					int oldy=y;
//					map[oldx][oldy]++;
//					x+=cx;
//					y+=cy;
//
//					if(x<0||x>=xr) x-=2*cx;
//					if(y<0||y>=yr) y-=2*cy;
//
//					int id=rd.nextInt(10);
//					if(oldx==testX&&oldy==testY){
//						count++;
//					}
//					grid.updateLineTra(oldx, oldy, j, x, y, j+1, id);
//					//System.out.println("time is:"+j+" cellx:"+x+" celly:"+y);
//
//				}
//				for(int i=0;i<64;i++){
//					if(i>=11&&i<=13){
//						grid.updateLineTra(i-3, i-3, dur+i, i+1, i+1, dur+i+1, 11);
//						//	grid.updateLineTra(i, i, dur+i, i+1, i+1, dur+i+1, 11);
//					} else{
//					grid.updateLineTra(i, i, dur+i, i+1, i+1, dur+i+1, 11);
//					}
//				}
//
//				for(int j=dur/2;j<dur;j++){
//					int cx=rd.nextInt(4);
//					int cy=rd.nextInt(4);
//
//					cx=(int)Math.pow(-1, cx)*cx;;
//					cy=(int)Math.pow(-1, cy)*cy;
//
//					int oldx=x;
//					int oldy=y;
//					map[oldx][oldy]++;
//					x+=cx;
//					y+=cy;
//
//					if(x<0||x>=xr) x-=2*cx;
//					if(y<0||y>=yr) y-=2*cy;
//
//					int id=rd.nextInt(10);
//					if(oldx==testX&&oldy==testY){
//						count++;
//					}
//					grid.updateLineTra(oldx, oldy, j, x, y, j+1, id);
//					//System.out.println("time is:"+j+" cellx:"+x+" celly:"+y);
//
//				}
//
//
//				//test-1   grid.queryByCellTimeRange
//				//	ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryByCellTimeRange(testX,testY,100000);
//
//				//test-2  grid.queryByRoITimeRange
//				/*RoIState roiState=new RoIState();
//				for(int rx=15;rx<=17;rx++){
//					for(int ry=15;ry<=17;ry++){
//						roiState.addRoICell(rx,ry,0);
//					}
//				}
//				ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryByRoITimeRange(roiState, dur/2+500);
//				for(int i=0;i<res.size();i++){
//					System.out.println("triaId:"+Configuration.getTraId(res.get(i).getKey())+" time:"+Configuration.getTime(res.get(i).getKey())+
//							" x:"+res.get(i).getValue().getCellX()+" y:"+res.get(i).getValue().getCellY());
//				}*/
//
//
//				//test-3  queryByMultiRoITimeRange
//			/*	ArrayList<RoIState> queryRoIs=new ArrayList<RoIState>();
//
//				for(int k=8;k<=16;k+=4){
//					RoIState roiState=new RoIState();
//
//					for(int rx=k-2;rx<=k+2;rx++){
//						for(int ry=k-2;ry<=k+2;ry++){
//							roiState.addRoICell(rx, ry, 0);
//						}
//					}
//					queryRoIs.add(roiState);
//				}
//
//				ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryByMultiRoITimeRange(queryRoIs, dur/2+500);
//				for(int i=0;i<res.size();i++){
//					System.out.println("triaId:"+Configuration.getTraId(res.get(i).getKey())+" time:"+Configuration.getTime(res.get(i).getKey())+
//							" x:"+res.get(i).getValue().getCellX()+" y:"+res.get(i).getValue().getCellY());
//				}
//			 */
//
//			//test 4-- queryRangeTimePerCell
//				ArrayList<RoICell> cells=new ArrayList<RoICell>();
//				for(int k=8;k<=16;k+=4){
//					RoICell rc=new RoICell(k,k);
//					cells.add(rc);
//				}
//
//				//ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryRangeTimePerCell(16, 16, 2, 2, dur/2+500, 1);
//				ArrayList<Entry<Long, GridLeafTraHashItem>>  res=grid.queryRangeTimeSeqCells(cells, 2, 2, dur/2+500, 1);
//                    for (Entry<Long, GridLeafTraHashItem> re : res) {
//                        System.out.println("triaId:" + Configuration.getTraId(re.getKey()) + " time:" + Configuration.getTime(re.getKey()) +
//                                " x:" + re.getValue().getCellX() + " y:" + re.getValue().getCellY());
//                    }
//
//
//			    System.out.println("count:"+count);
//
//			    for(int i=0;i<xr;i++){
//			    	for(int j=0;j<yr;j++){
//			    		System.out.print(map[i][j]+ " ");
//			    	}
//			    	System.out.println();
//			    }
//
//				}catch(Exception e){
//					e.printStackTrace();
//				}
//
//		 }
}



