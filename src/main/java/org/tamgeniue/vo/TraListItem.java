package org.tamgeniue.vo;

public class TraListItem {
    private int traId;
    private int timestamp;
    private int off;//the timestamp is guaranteed to be after the off

    public TraListItem(int inputTraId, int inOff, int inputTimestamp) {
        this.traId = inputTraId;
        this.timestamp = inputTimestamp;
        this.off = inOff;
    }

    public int getTraId() {
        return traId;
    }

    public void setTraId(int traId) {
        this.traId = traId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getOff() {
        return off;
    }

    public void setOff(int off) {
        this.off = off;
    }
}
