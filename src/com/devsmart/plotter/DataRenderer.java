package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.RectF;

public interface DataRenderer {
	
	public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem);

}
