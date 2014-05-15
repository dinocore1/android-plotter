package com.devsmart.plotter;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class OversampleFunctionRenderer implements DataRenderer {


    private final FunctionRenderer2.GraphFunction mFunction;
    private double[] mSamplePoints;
    private double mSampleRate;
    protected Paint mPaint = new Paint();

    public OversampleFunctionRenderer(FunctionRenderer2.GraphFunction f, double[] samplePoints, int color) {
        mFunction = f;
        mSamplePoints = samplePoints;
        mPaint.setColor(color);
        mPaint.setStrokeWidth(2.0f);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    public OversampleFunctionRenderer(FunctionRenderer2.GraphFunction f, double sampleRate, int color){
        mFunction = f;
        mSampleRate = sampleRate;
        mPaint.setColor(color);
        mPaint.setStrokeWidth(2.0f);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {
        if(mSamplePoints != null){
            drawUsingSamplePoints(canvas, viewPort, coordSystem);
        } else {
            drawUsingSamplerate(canvas, viewPort, coordSystem);
        }
    }

    private void drawUsingSamplerate(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {
        float[] points = new float[2];
        final double pixelWidth = viewPort.width() / (double)canvas.getWidth();

        Path p = new Path();
        for(double x=viewPort.left;x<=viewPort.right;x+=mSampleRate){
            final double y = mFunction.value(x);


            points[0] = (float)x;
            points[1] = (float)y;

            coordSystem.mapPoints(points);
            if(FunctionRenderer2.isRealNumber(points[0]) && FunctionRenderer2.isRealNumber(points[1])) {
                if (x == viewPort.left) {
                    p.moveTo(points[0], points[1]);
                } else {
                    p.lineTo(points[0], points[1]);
                }
            }
        }

        canvas.drawPath(p, mPaint);
    }

    private void drawUsingSamplePoints(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem) {
        float[] points = new float[2];
        final double pixelWidth = viewPort.width() / (double)canvas.getWidth();

        Path p = new Path();
        for(int i=0;i<mSamplePoints.length;i++){
            double x = mSamplePoints[i];
            final double y = mFunction.value(x);

            points[0] = (float)x;
            points[1] = (float)y;

            coordSystem.mapPoints(points);
            if(FunctionRenderer2.isRealNumber(points[0]) && FunctionRenderer2.isRealNumber(points[1])) {
                if (x == viewPort.left) {
                    p.moveTo(points[0], points[1]);
                } else {
                    p.lineTo(points[0], points[1]);
                }
            }
        }

        canvas.drawPath(p, mPaint);
    }
}
