package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

public interface DataRenderer {

    /**
     * Draw the graph to the screen.
     * @param canvas
     * @param viewPort The area of the graph to draw. Values graph coordinate system.
     * @param toScreen function used to map graph coordinates to screen coordinates
     */
    void draw(Canvas canvas, RectF viewPort, MultivariateFunction toScreen);
}
