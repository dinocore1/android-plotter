package com.devsmart.plotter;

import java.util.Iterator;

import android.graphics.RectF;


public interface Series {

	public Iterator<float[]> createIterator();
}
