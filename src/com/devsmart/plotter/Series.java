package com.devsmart.plotter;


public interface Series {

	int getLength();
	
	void getPoint(int index, float[] point);
}
