package com.devsmart.plotter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.math.BigDecimal;

public class SimpleAxisRenderer implements AxisRenderer {

	int numDivisions = 5;
	boolean mDrawXAxis = true;
	boolean mDrawYAxis = true;
	int mAxisColor = Color.BLACK;
	Rect mPlotMargins = new Rect(20, 0, 0, 20);
	public Paint mAxisLabelPaint = new Paint();
	public Paint mAxisTickPaint = new Paint();
	String mXAxisLabel = "Wavelength";
	String mYAxisLabel = "Intensity";
	
	DisplayMetrics mDisplayMetrics;
	
	public SimpleAxisRenderer(Context context) {
		mDisplayMetrics = context.getResources().getDisplayMetrics();

        mAxisLabelPaint.setColor(Color.BLACK);
        mAxisLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15, mDisplayMetrics));
        mAxisLabelPaint.setAntiAlias(true);

        mAxisTickPaint.setColor(Color.DKGRAY);
        mAxisTickPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, mDisplayMetrics));
        mAxisTickPaint.setAntiAlias(true);

    }

	public SimpleAxisRenderer(GraphView graphview) {
		mDisplayMetrics = graphview.getContext().getResources().getDisplayMetrics();
		
		mAxisLabelPaint.setColor(Color.BLACK);
		mAxisLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15, mDisplayMetrics));
		mAxisLabelPaint.setAntiAlias(true);
		
		mAxisTickPaint.setColor(Color.DKGRAY);
		mAxisTickPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, mDisplayMetrics));
		mAxisTickPaint.setAntiAlias(true);
		
	}

    @Override
    public void setAxisColor(int color) {
        mAxisLabelPaint.setColor(color);
        mAxisTickPaint.setColor(color);
    }

    @Override
    public void setYAxisLabel(String label){
        mYAxisLabel = label;
    }

    @Override
    public void setXAxisLabel(String label) {
        mXAxisLabel = label;
    }
	
	private float[] mYAxis;
	private float[] mXAxis;
	float[] points = new float[4];
	Rect bounds = new Rect();
	RectF boundsf = new RectF();
	Rect graphArea = new Rect();
	
	protected void calcBounds(final int canvasWidth, final int canvasHeight) {
		mAxisLabelPaint.getTextBounds("1", 0, 1, bounds);
		float axisLabelHeight = bounds.height();
		
		mAxisTickPaint.getTextBounds("1", 0, 1, bounds);
		float tickLabelHeight = bounds.height();
		
		float axisLabelBoundery = axisLabelHeight + tickLabelHeight + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, mDisplayMetrics);
		
		
		float height = canvasHeight - axisLabelBoundery - mPlotMargins.height();
		
		mYAxis = new float[]{axisLabelBoundery + mPlotMargins.left, mPlotMargins.top, 
									axisLabelBoundery + mPlotMargins.left, mPlotMargins.top + height};
		
		mXAxis = new float[]{axisLabelBoundery + mPlotMargins.left, canvasHeight - axisLabelBoundery - mPlotMargins.bottom,
										canvasWidth - mPlotMargins.right, canvasHeight - axisLabelBoundery - mPlotMargins.bottom};
	}
	
	
	Matrix m = new Matrix();
	
	@Override
	public void drawAxis(Canvas canvas, final int canvasWidth, final int canvasHeight, RectF viewPort, CoordinateSystem coordSystem) {
		
		measureGraphArea(canvasWidth, canvasHeight);
		
		m.setRectToRect(new RectF(0,0,graphArea.width(),graphArea.height()), new RectF(graphArea), ScaleToFit.FILL);
		m.postScale(1, -1);
		m.postTranslate(0, graphArea.height());
		
		//Debug axis display
		//canvas.drawText(viewPort.toString(), 50, 50, mAxisTickPaint);
		
		if(mDrawXAxis) {
			
			//draw axis
			canvas.drawLines(mXAxis, mAxisTickPaint);
			
			//draw label
			mAxisLabelPaint.getTextBounds(mXAxisLabel, 0, mXAxisLabel.length(), bounds);
			float y = canvasHeight - mPlotMargins.bottom + bounds.bottom + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, mDisplayMetrics);
			canvas.drawText(mXAxisLabel, (mXAxis[2]-mXAxis[0])/2 - bounds.width()/2 + mXAxis[0], y, mAxisLabelPaint);
			
			//draw ticks
			final float dist = viewPort.width() / numDivisions;
			float xPoint = (float) (dist * Math.floor(viewPort.left / dist));
			while(xPoint < viewPort.right+dist){
				points[0] = xPoint;
				points[1] = 0;
				points[2] = xPoint;
				points[3] = 0;
				coordSystem.mapPoints(points);
				m.mapPoints(points);
				points[1] = mXAxis[1];
				points[3] = mXAxis[1] - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, mDisplayMetrics);
				
				if(points[0] >= mXAxis[0]) {
					canvas.drawLines(points, mAxisTickPaint);
					
					String label = getTickLabel(xPoint);
					mAxisTickPaint.getTextBounds(label, 0, label.length(), bounds);
					
			
					canvas.drawText(label,
							points[0]-bounds.width()/2,
							points[1] + bounds.height() + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, mDisplayMetrics),
							mAxisTickPaint);
				}

				
				

				xPoint += dist;

			}
		}
		
		if(mDrawYAxis){
			
			
			
			//draw Y axis
			canvas.drawLines(mYAxis, mAxisTickPaint);
		
			//draw label
			mAxisLabelPaint.getTextBounds(mYAxisLabel, 0, mYAxisLabel.length(), bounds);
			canvas.save();
			points[0] = mPlotMargins.left;
			points[1] = (mYAxis[3] - mYAxis[1])/2 + bounds.width()/2;
			canvas.rotate(-90, points[0], points[1]);
			canvas.drawText(mYAxisLabel, points[0], points[1], mAxisLabelPaint);
			canvas.restore();

			final float dist = viewPort.height() / numDivisions;
			float yPoint = 	(float) (dist *  Math.floor(viewPort.top / dist));
			while(yPoint < viewPort.bottom+dist){
				points[0] = 0;
				points[1] = yPoint;
				points[2] = 0;
				points[3] = yPoint;
				coordSystem.mapPoints(points);
				m.mapPoints(points);
				points[0] = mYAxis[0];
				points[2] = mYAxis[0] + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, mDisplayMetrics);
				
				if(points[1] <= mYAxis[3]) {
					canvas.drawLines(points, mAxisTickPaint);
	
					String label = getTickLabel(yPoint);
					mAxisTickPaint.getTextBounds(label, 0, label.length(), bounds);
					canvas.save();
					points[2] = points[0]-bounds.height()/2-TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, mDisplayMetrics);
					points[3] = points[1]+bounds.width()/2;
					canvas.rotate(-90, points[2], points[3]);
					canvas.drawText(label,
							points[2], points[3],
							mAxisTickPaint);
					canvas.restore();
				}

				yPoint += dist;
			}

		}

	}
	
	protected String getTickLabel(float value) {
		return String.format("%g", value);
		//return String.valueOf(MathUtils.round(value, 1));
	}



	@Override
	public Rect measureGraphArea(int screenWidth, int screenHeight) {
		calcBounds(screenWidth, screenHeight);
		
		graphArea.left = (int) Math.floor(mXAxis[0]);
		graphArea.right = (int) Math.ceil(mXAxis[2]);
		graphArea.top = (int) Math.floor(mYAxis[1]);
		graphArea.bottom = (int) Math.ceil(mYAxis[3]);
		return graphArea;
	}
	


}
