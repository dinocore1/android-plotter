package com.devsmart.plotter;

import java.util.ArrayList;
import java.util.Iterator;

import android.graphics.RectF;

public class SimpleSeries implements Series {
	
	protected ArrayList<float[]> mData = new ArrayList<float[]>();

	@Override
	public Iterator<float[]> createIterator() {
		return mData.iterator();
	}

	

}
