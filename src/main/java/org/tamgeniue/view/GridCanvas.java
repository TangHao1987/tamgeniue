package org.tamgeniue.view;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;

@Component("gridCanvas")
public class GridCanvas extends Canvas{

    @Value("0")
    private int x0;
    @Value("0")
    private int y0;
    @Value("${param.grid.divided}")
    private int x1;
    @Value("${param.grid.divided}")
    private int y1;

    private Graphics2D context;
    
    public void init(){
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    public void addNotify() {
        super.addNotify();
        context = (Graphics2D) this.getGraphics().create();
    } // End addNotify.

    public void paint(Graphics g) {

        g.setColor(Color.white);
        g.fillRect(0, 0, x1 - x0, y1 - y0);
//
//        for (int x = x0; x < x1; x++) {
//            for (int y = y0; y < y1; y++) {
//                GridCell gc = visGid.getGridCell(x, y);
//                if (gc != null) {
//                    double d = gc.density;
//                    int l = 255 - (int) (d / densityUnit);
//                    if (l < 0) l = 0;
//                    g.setColor(new Color(l, l, 255));
//                    g.drawLine(x - x0, y - y0, x - x0, y - y0);
//                    g.fillOval(x-x0,  y-y0, 20, 20);
//                }
//
//            }
//
//        }
//        if (traSet != null) {
//
//            // g.setColor(new Color(255,0,0));
//            // g.drawLine(0, 0, 50,50);
//            context.setColor(new Color(255, 0, 0));
//            Enumeration<ArrayList<TraStoreListItem>> traElements = traSet.elements();
//            Enumeration<Integer> traKeys = traSet.keys();
//            // context.drawLine((int)(0), (int)(0),
//            //	  (int)(200),(int)(200));
//            int colorSwitch = 0;
//            while (traElements.hasMoreElements() && traKeys.hasMoreElements()) {
//                ArrayList<TraStoreListItem> itemRes = traElements.nextElement();
//                int itemTraId = traKeys.nextElement();
//                // System.out.println("trajectory id is:"+itemTraId);
//
//                Color colorItem = timeColorSet[colorSwitch % timeColorNum];
//                context.setColor(colorItem);
//                for (int i = 0; i < itemRes.size(); i++) {
//                    TraStoreListItem offItem = itemRes.get(i);
//                    if (i == 0) {
//                        context.setColor(Color.BLACK);
//                        context.drawString("traid:" + itemTraId + " off_set:" + offItem.off,
//                                5 + (int) ((offItem.lat - lat0) / step), 5 + (int) ((offItem.lng - lng0) / step));
//                        context.setColor(colorItem);
//                    }
//
//                    //	  System.out.print("<lat:"+offItem.lat+" lng:"+offItem.lng+" time:"+offItem.timestamp+"> ");
//                    context.drawLine(2 + (int) ((offItem.lat - lat0) / step), 2 + (int) ((offItem.lng - lng0) / step),
//                            -2 + (int) ((offItem.lat - lat0) / step), -2 + (int) ((offItem.lng - lng0) / step));
//                    context.drawLine(2 + (int) ((offItem.lat - lat0) / step), -2 + (int) ((offItem.lng - lng0) / step),
//                            -2 + (int) ((offItem.lat - lat0) / step), 2 + (int) ((offItem.lng - lng0) / step));
//
//                }
//                colorSwitch++;
//                //  System.out.println();
//            }
//        } else if (null != timeTraState && null == MAPPath) {
//            context.setColor(new Color(255, 0, 0));
//
//            for (int k = 1; k < timeTraState.getTimeLength() + 1; k++) {
//                for (int i = 0; i < timeTraState.getStateNum(k); i++) {
//                    HashSet<RoICell> roiSet = timeTraState.getState(k, i).roiSet;
//                    for (RoICell rc : roiSet) {
//                        Color colorItem = timeColorSet[(k - 1) % timeColorNum];
//                        context.setColor(colorItem);
//                        context.drawLine(rc.roiX, rc.roiY, rc.roiX, rc.roiY);
//                    }
//                }
//            }
//        } else if (null != timeTraState) {
//            for (int i = 1; i < MAPPath.length; i++) {
//                Color colorItem = timeColorSet[(i - 1) % timeColorNum];
//                context.setColor(colorItem);
//                int x = (int) (timeTraState.getState(i, MAPPath[i]).getCenterX() + 0.5);
//                int y = (int) (timeTraState.getState(i, MAPPath[i]).getCenterY() + 0.5);
//                drawCircle(context, x, y);
//                //	System.out.println("time:"+i+" pos x:"+timeTraState.getState(i,MAPPath[i]).getCenterX()+" y:"+timeTraState.getState(i, MAPPath[i]).getCenterY());
//            }
//        } else if (null != traGrid) {
//            context.setColor(new Color(255, 0, 0));
//            for (Map.Entry<Long, GridLeafTraHashItem> item : traGrid) {
//                drawX(context, item.getValue().getCellX(), item.getValue().getCellY(), 0);
//            }
//        }
//
//        if (-1 != moveObjLat && -1 != moveObjLng) {
//            paintMoveObj(context, moveObjLat, moveObjLng);
//        }
    }
//
//    private void drawX(Graphics2D inContex, int x, int y, int r) {
//        inContex.drawLine(r + x, r + y,
//                -r + x, -r + y);
//        inContex.drawLine(r + x, -r + y,
//                -r + x, r + y);
//
//    }
//
//    private void drawCircle(Graphics inContext, int x, int y) {
//        inContext.fillOval(x, y, 7, 7);
//    }
//
//    private void paintMoveObj(Graphics2D inContext, double lat, double lng) {
//        int x = (int) ((lat) / step);
//        int y = (int) ((lng) / step);
//        drawXCircle(inContext, new Point(x, y));
//    }
//
//    private void drawXCircle(Graphics2D inContex, Point obj) {
//        drawXCircle(inContex, obj, 5);
//    }
//
//    private void drawXCircle(Graphics2D inContex, Point obj, int r) {
//        Stroke defaultStroke = inContex.getStroke();
//        Color defaultColor = inContex.getColor();
//        inContex.setColor(Color.BLACK);
//        inContex.setStroke(new BasicStroke(2));
//        inContex.drawOval(obj.x - r, obj.y - r, 2 * r, 2 * r);
//        drawX(inContex, obj.x, obj.y, 5);
//        inContex.setStroke(defaultStroke);
//        inContex.setColor(defaultColor);
//    }
//
//    protected void processMouseEvent(MouseEvent e) {
//        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
//            lastMouseX = e.getX();
//            lastMouseY = e.getY();
//            context.setColor(new Color(255, 0, 0));
//            context.drawString("x:" + lastMouseX + " y:" + lastMouseY, lastMouseX, lastMouseY);
//        }
//    }
//
//    protected void processMouseMotionEvent(MouseEvent event) {
//
//        if (event.getID() == MouseEvent.MOUSE_DRAGGED) {
//            int currentX = event.getX();
//            int currentY = event.getY();
//            context.drawLine(lastMouseX, lastMouseY, currentX, currentY);
//            lastMouseX = currentX;
//            lastMouseY = currentY;
//            repaint();
//        } // End if.
//        else if (event.getID() == MouseEvent.MOUSE_WHEEL) {
//            repaint();
//        }
//
//    } // End processMouseMotionEvent.

}
