package com.devsmart.plotter;

public interface AxisFunction {
	
	void interpolate(float[] x, float[] y);
	
	AxisFunction copy();

	/**
	 * translate screen coord --> graph coord 
	 * @param x
	 * @return
	 */
	float value(float x);
	
	/**
	 * return the inverse function
	 * @return
	 */
	AxisFunction inverse();
}
