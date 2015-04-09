package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class FunctionRenderer implements DataRenderer
{
    public interface GraphFunction
    {
        double value(double x);
    }

    private double[] mSampleLocations;
    private GraphFunction mFunction;
    private double mSampleRate = 2;
    protected final Paint mPointPaint = new Paint();
    float[] points = new float[6];
    float[] drawPoints = new float[4];
    float[] lastPoint = new float[2];
    double[] yMinMax = new double[2];

    public FunctionRenderer(GraphFunction f, double[] sampleLocations, int color)
    {
        mFunction = f;
        mSampleLocations = sampleLocations;
        mPointPaint.setColor(color);
        mPointPaint.setStrokeWidth(2.0f);
        mPointPaint.setAntiAlias(true);
        mPointPaint.setStyle(Paint.Style.STROKE);
    }

    public FunctionRenderer(GraphFunction f, double samplerate, int color)
    {
        mFunction = f;
        mSampleRate = samplerate;
        mPointPaint.setColor(color);
        mPointPaint.setStrokeWidth(2.0f);
        mPointPaint.setAntiAlias(true);
        mPointPaint.setStyle(Paint.Style.STROKE);
    }

    private void drawFixedSample(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem)
    {
        //reset min max
        yMinMax[0] = Double.MAX_VALUE;
        yMinMax[1] = Double.MIN_VALUE;

        final double pixelWidth = viewPort.width() / (double) canvas.getWidth();
        final double stepWidth = Math.min(pixelWidth, 1.0 / mSampleRate);

        Path p = new Path();

        points[0] = viewPort.left;
        points[1] = (float) mFunction.value(viewPort.left);
        coordSystem.mapPoints(points);
        p.moveTo(points[0], points[1]);

        double startPix = viewPort.left;
        for (double x = startPix; x < viewPort.right; x += stepWidth)
        {
            final double y = mFunction.value(x);
            yMinMax[0] = Math.min(yMinMax[0], y);
            yMinMax[1] = Math.max(yMinMax[1], y);

            if (x >= startPix + pixelWidth)
            {

                //min
                points[0] = (float) startPix;
                points[1] = (float) yMinMax[0];

                //max
                points[2] = (float) startPix;
                points[3] = (float) yMinMax[1];

                coordSystem.mapPoints(points);

                p.lineTo(points[0], points[1]);
                p.lineTo(points[2], points[3]);

                //reset min max
                yMinMax[0] = Double.MAX_VALUE;
                yMinMax[1] = Double.MIN_VALUE;

                startPix = x;
            }
        }

        points[0] = viewPort.right;
        points[1] = (float) mFunction.value(viewPort.right);
        coordSystem.mapPoints(points);
        p.lineTo(points[0], points[1]);

        canvas.drawPath(p, mPointPaint);
    }

    private class MinMax
    {
        double min;
        double max;

        public MinMax()
        {
            reset();
        }

        public void reset()
        {
            min = Double.MAX_VALUE;
            max = Double.MIN_VALUE;
        }

        public void add(float value)
        {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
    }

    private void drawAtSampleLocations(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem)
    {

        Paint pointPaint = new Paint();
        pointPaint.setColor(Color.RED);

        int sampleindex = 0;
        for (int i = 0; i < mSampleLocations.length; i++)
        {
            if (mSampleLocations[i] >= viewPort.left)
            {
                sampleindex = i;
                if (i > 0)
                {
                    sampleindex = i - 1;
                }
                break;
            }
        }

        //reset min max
        yMinMax[0] = Double.MAX_VALUE;
        yMinMax[1] = Double.MIN_VALUE;
        MinMax xminmax = new MinMax();
        MinMax yminmax = new MinMax();

        final double pixelWidth = viewPort.width() / (double) canvas.getWidth();

        Path p = new Path();
        Path p2 = new Path();

        points[0] = (float) mSampleLocations[sampleindex];
        points[1] = (float) mFunction.value(points[0]);
        xminmax.add(points[0]);
        yminmax.add(points[1]);

        coordSystem.mapPoints(points);
        p.lineTo(points[0], points[1]);
        p2.moveTo(points[0], points[1]);
        lastPoint[0] = points[0];
        lastPoint[1] = points[1];

        double startPix = mSampleLocations[sampleindex];
        double x = 0;
        while (true)
        {
            if (sampleindex >= mSampleLocations.length - 1 || (x = mSampleLocations[sampleindex++ + 1]) > viewPort.right)
            {
                break;
            }

            final double y = mFunction.value(x);


            points[0] = (float) x;
            points[1] = (float) y;
            coordSystem.mapPoints(points);

            canvas.drawCircle(points[0], points[1], 3.0f, pointPaint);

            p.lineTo((lastPoint[0] + points[0]) / 2, lastPoint[1]);
            p.lineTo((lastPoint[0] + points[0]) / 2, points[1]);

            //p.lineTo(points[0], lastPoint[1]);
            //p.lineTo(points[0], points[1]);
            lastPoint[0] = points[0];
            lastPoint[1] = points[1];

            /*
            yMinMax[0] = Math.min(yMinMax[0], y);
            yMinMax[1] = Math.max(yMinMax[1], y);

            if(x >= startPix+pixelWidth){

                //min
                points[0] = (float) x;
                points[1] = (float) yMinMax[0];

                //max
                points[2] = (float) x;
                points[3] = (float) yMinMax[1];

                xminmax.add(points[0]);
                yminmax.add(points[1]);
                xminmax.add(points[2]);
                yminmax.add(points[3]);

                coordSystem.mapPoints(points);

                p.quadTo((lastPoint[0]+points[0])/2, coordSystem.yValue((lastPoint[0]+points[0])/2), points[0], points[1]);
                lastPoint[0] = points[0];
                lastPoint[1] = points[1];

                p.quadTo((lastPoint[0]+points[2])/2, coordSystem.yValue((lastPoint[0]+points[2])/2), points[2], points[3]);
                lastPoint[0] = points[2];
                lastPoint[1] = points[3];

                //reset min max
                yMinMax[0] = Double.MAX_VALUE;
                yMinMax[1] = Double.MIN_VALUE;

                startPix = x;
            }
            */
        }

        p.lineTo(canvas.getWidth(), lastPoint[1]);

        points[0] = viewPort.right;
        points[1] = (float) mFunction.value(viewPort.right);
        xminmax.add(points[0]);
        yminmax.add(points[1]);
        coordSystem.mapPoints(points);
        //p.lineTo(points[0], points[1]);
        //p.quadTo((lastPoint[0] + points[0]) / 2, (lastPoint[1] + points[1]) / 2, points[0], points[1]);


        p.lineTo(canvas.getWidth(), 0);

        //p.lineTo(xminmax.max, yminmax.min);
        //p.lineTo(xminmax.min, yminmax.min);


        p.close();

        canvas.drawPath(p, mPointPaint);


        //reset min max
        yMinMax[0] = Double.MAX_VALUE;
        yMinMax[1] = Double.MIN_VALUE;
        final double stepWidth = Math.min(pixelWidth, 1.0 / mSampleRate);
        startPix = viewPort.left;
        for (x = startPix; x < viewPort.right; x += stepWidth)
        {
            final double y = mFunction.value(x);
            yMinMax[0] = Math.min(yMinMax[0], y);
            yMinMax[1] = Math.max(yMinMax[1], y);

            if (x >= startPix + pixelWidth)
            {

                //min
                points[0] = (float) startPix;
                points[1] = (float) yMinMax[0];

                //max
                points[2] = (float) startPix;
                points[3] = (float) yMinMax[1];

                coordSystem.mapPoints(points);

                p2.lineTo(points[0], points[1]);
                p2.lineTo(points[2], points[3]);

                //reset min max
                yMinMax[0] = Double.MAX_VALUE;
                yMinMax[1] = Double.MIN_VALUE;

                startPix = x;
            }
        }

        Paint p2Paint = new Paint();
        p2Paint.setStyle(Paint.Style.STROKE);
        p2Paint.setColor(Color.WHITE);
        p2Paint.setStrokeWidth(2.0f);
        p2Paint.setAntiAlias(true);
        canvas.drawPath(p2, p2Paint);
    }

    @Override
    public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem)
    {
        if (mSampleLocations == null)
        {
            drawFixedSample(canvas, viewPort, coordSystem);
        }
        else
        {
            drawAtSampleLocations(canvas, viewPort, coordSystem);
        }
    }

    @Override
    public void setPaintColor(int color)
    {
        mPointPaint.setColor(color);
    }
}