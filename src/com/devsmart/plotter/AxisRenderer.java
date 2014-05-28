package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public interface AxisRenderer {
	
	/**
	 * Measure the area where the graph data will be drawn. Return a Rect 
	 * in screen coordinates.
	 * @param screenWidth
	 * @param screenHeight
	 * @return
	 */
	Rect measureGraphArea(int screenWidth, int screenHeight);
	
	/**
	 * Draw the axis on the canvas. 
	 * @param canvas
	 * @param canvasWidth
	 * @param canvasHeight
	 * @param viewport
	 * @return
	 */
	void drawAxis(Canvas canvas, int canvasWidth, int canvasHeight, RectF viewport, CoordinateSystem coordSystem);

    void setXAxisLabel(String label);
    void setYAxisLabel(String label);

}
