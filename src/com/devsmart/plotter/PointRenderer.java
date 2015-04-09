package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;


public class PointRenderer implements DataRenderer {

    private float[] mPoints;
    private final Paint mPaint = new Paint();

    public PointRenderer(float[] points, int color) {
        mPoints = points;
        mPaint.setColor(color);
        mPaint.setStrokeWidth(5.0f);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {

        float[] point = new float[2];
        for(int i=0;i<mPoints.length;i=i+2){
            if(viewPort.contains(mPoints[i], mPoints[i+1])){
                point[0] = mPoints[i];
                point[1] = mPoints[i+1];

                coordSystem.mapPoints(point);
                canvas.drawCircle(point[0], point[1], 2.0f, mPaint);
            }
        }
    }

    @Override
    public void setPaintColor(int color)
    {
        mPaint.setColor(color);
    }
}
