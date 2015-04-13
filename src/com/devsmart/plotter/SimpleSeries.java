package com.devsmart.plotter;

import java.util.ArrayList;
import java.util.Iterator;

public class SimpleSeries implements Series {

    public ArrayList<float[]> mData = new ArrayList<float[]>();

    @Override
    public Iterator<float[]> createIterator() {
        return mData.iterator();
    }
}