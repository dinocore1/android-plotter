package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.RectF;

public interface AxisRenderer {
	
	void drawAxis(Canvas canvas, int canvasWidth, int canvasHeight, RectF viewport);

}
