package com.devsmart.plotter;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class FunctionRenderer2 implements DataRenderer {

    public interface GraphFunction {
        double value(double x);
    }

    private final GraphFunction mFunction;
    protected final Paint mPaint = new Paint();


    public FunctionRenderer2(GraphFunction f, int color){
        mFunction = f;
        mPaint.setColor(color);
        mPaint.setStrokeWidth(2.0f);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    public static boolean isRealNumber(float f) {
        return !Float.isNaN(f) && !Float.isInfinite(f);
    }

    @Override
    public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {

        float[] points = new float[2];
        final double pixelWidth = viewPort.width() / (double)canvas.getWidth();

        Path p = new Path();
        for(double x=viewPort.left;x<=viewPort.right;x+=pixelWidth){
            final double y = mFunction.value(x);

            points[0] = (float)x;
            points[1] = (float)y;

            coordSystem.mapPoints(points);
            if(isRealNumber(points[0]) && isRealNumber(points[1])) {
                if (x == viewPort.left) {
                    p.moveTo(points[0], points[1]);
                } else {
                    p.lineTo(points[0], points[1]);
                }
            }
        }

        canvas.drawPath(p, mPaint);
    }

    @Override
    public void setPaintColor(int color)
    {
        mPaint.setColor(color);
    }
}
