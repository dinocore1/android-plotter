package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.RectF;

public interface DataRenderer {
    void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem);

    void setPaintColor(int color);
}