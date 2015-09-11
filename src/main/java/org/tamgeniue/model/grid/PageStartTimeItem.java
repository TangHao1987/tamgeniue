package org.tamgeniue.model.grid;

import java.io.Serializable;

public class PageStartTimeItem implements Serializable{
    private static final long serialVersionUID = 2440219611035970010L;
    private int startTime;
    private int pageId;

    public PageStartTimeItem(int startTime,int pageId){
        this.startTime = startTime;
        this.pageId=pageId;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }
}
