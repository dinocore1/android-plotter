package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.RectF;

public interface AxisRenderer {
	
	void drawAxis(Canvas canvas, RectF viewport, GraphView graphview);

}
