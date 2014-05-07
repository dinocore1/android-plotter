package com.devsmart.plotter.graph;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.devsmart.plotter.DataRenderer;
import com.devsmart.plotter.Function;
import com.devsmart.plotter.MultivariateFunction;

public class FunctionRenderer implements DataRenderer {

    public Function mGraphFunction;
    public Paint mPathPaint;

    public FunctionRenderer(Function graphFunction, int lineColor, float lineWidth) {
        mGraphFunction = graphFunction;
        mPathPaint = new Paint();
        mPathPaint.setColor(lineColor);
        mPathPaint.setStrokeWidth(lineWidth);
        mPathPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas, RectF viewPort, MultivariateFunction toScreen) {

        float[] point = new float[2];
        point[0] = viewPort.right;
        float left = toScreen.value(point)[0];
        point[0] = viewPort.left;
        float right = toScreen.value(point)[0];

        final float pixelWidth = viewPort.width() / Math.abs(right-left);
        final float stepWidth = pixelWidth;

        Path p = new Path();

        float x = viewPort.left;
        float y = mGraphFunction.value(x);

        point[0] = x;
        point[1] = y;
        point = toScreen.value(point);
        p.moveTo(point[0], point[1]);

        for(x=x+stepWidth; x<=viewPort.right; x+=stepWidth) {
            y = mGraphFunction.value(x);

            point[0] = x;
            point[1] = y;
            point = toScreen.value(point);
            p.lineTo(point[0], point[1]);

        }

        canvas.drawPath(p, mPathPaint);

    }
}
