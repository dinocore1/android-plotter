package com.devsmart.plotter;

import android.graphics.RectF;

public class CoordinateSystem {

	AxisFunction mXAxisFunction;
	AxisFunction mYAxisFunction;
	CoordinateSystem mInverse;
	
	
	
	public CoordinateSystem copy() {
		CoordinateSystem retval = new CoordinateSystem();
		retval.mXAxisFunction = mXAxisFunction.copy();
		retval.mYAxisFunction = mYAxisFunction.copy();
		
		return retval;
	}

	public float xValue(float x) {
		return mXAxisFunction.value(x);
	}
	
	public float yValue(float y) {
		return mYAxisFunction.value(y);
	}
	
	public void mapRect(RectF dest, RectF src) {
		dest.left = mXAxisFunction.value(src.left);
		dest.right = mXAxisFunction.value(src.right);
		dest.bottom = mYAxisFunction.value(src.bottom);
		dest.top = mYAxisFunction.value(src.top);
	}
	
	public void mapRect(RectF rect) {
		rect.left = mXAxisFunction.value(rect.left);
		rect.right = mXAxisFunction.value(rect.right);
		rect.bottom = mYAxisFunction.value(rect.bottom);
		rect.top = mYAxisFunction.value(rect.top);
		
	}
	
	/**
	 * Apply this system to the array of 2D points, and write the transformed points back into the array
	 * @param pts
	 */
	public void mapPoints(float[] pts) {
		mapPoints(pts, pts);
		/*
		for(int x=0;x<pts.length;x=x+2){
			pts[x] = mXAxisFunction.value(pts[x]);
		}
		
		for(int y=1;y<pts.length;y=y+2){
			pts[y] = mYAxisFunction.value(pts[y]);
		}
		*/
		
	}
	
	public void mapPoints(float[] dest, float[] src) {
		for(int x=0;x<src.length;x=x+2){
			dest[x] = mXAxisFunction.value(src[x]);
		}
		
		for(int y=1;y<src.length;y=y+2){
			dest[y] = mYAxisFunction.value(src[y]);
		}
	}

	public CoordinateSystem getInverse() {
		if(mInverse == null){
			mInverse = new CoordinateSystem();
			mInverse.mXAxisFunction = mXAxisFunction.inverse();
			mInverse.mYAxisFunction = mYAxisFunction.inverse();
		}
		return mInverse;
	}
	
	public void interpolate(RectF from, RectF to){
		mXAxisFunction.interpolate(new float[]{from.left, from.right}, new float[]{to.left, to.right});
		mYAxisFunction.interpolate(new float[]{from.top, from.bottom}, new float[]{to.top, to.bottom});
	}
	
	public static CoordinateSystem createLinearSystem() {
		CoordinateSystem retval = new CoordinateSystem();
		retval.mXAxisFunction = new LinearFunction();
		retval.mYAxisFunction = new LinearFunction();
		
		return retval;
	}

	
}
