package org.tamgeniue.storagemanager;

import java.util.ArrayList;

/**
 * Created by Hao on 6/9/2015.
 *
 */
public class PageEntry {
    private int length = 0;
    private ArrayList<Integer> pages = new ArrayList<Integer>();

    public void setLength(int length){
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public ArrayList<Integer> getPages() {
        return pages;
    }
}
