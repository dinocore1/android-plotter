package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.RectF;

public interface DataRenderer {

    /**
     * Draw the graph to the screen.
     * @param canvas
     * @param viewPort The area of the graph to draw. Values are in non-screen units.
     * @param coordanateSystem
     */
    void draw(Canvas canvas, RectF viewPort, CoordanateSystem coordanateSystem);
}
