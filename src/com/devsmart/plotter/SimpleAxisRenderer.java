package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.devsmart.MathUtils;

public class SimpleAxisRenderer implements AxisRenderer {

	int numDivisions = 5;
	boolean mDrawXAxis = true;
	boolean mDrawYAxis = true;
	int mAxisColor = Color.BLACK;
	Rect mPlotMargins = new Rect(20, 0, 0, 20);
	Paint mAxisLabelPaint = new Paint();
	Paint mAxisTickPaint = new Paint();
	String mXAxisLabel = "Wavelength";
	String mYAxisLabel = "Intensity";
	
	DisplayMetrics mDisplayMetrics;
	
	public SimpleAxisRenderer(GraphView graphview) {
		mDisplayMetrics = graphview.getContext().getResources().getDisplayMetrics();
		
		mAxisLabelPaint.setColor(Color.BLACK);
		mAxisLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15, mDisplayMetrics));
		mAxisLabelPaint.setAntiAlias(true);
		
		mAxisTickPaint.setColor(Color.DKGRAY);
		mAxisTickPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, mDisplayMetrics));
		mAxisTickPaint.setAntiAlias(true);
		
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
	
	
	
	@Override
	public void drawAxis(Canvas canvas, final int canvasWidth, final int canvasHeight, RectF viewPort) {
		
		calcBounds(canvasWidth, canvasHeight);
		
		Paint axisPaint = new Paint();
		axisPaint.setColor(mAxisColor);
		axisPaint.setStrokeWidth(2);

		boundsf.set(0,0,canvasWidth, canvasHeight);
		Matrix matrix = GraphView.getViewportToScreenMatrix(boundsf, viewPort);
		
		
		if(mDrawXAxis) {
			
			//draw axis
			canvas.drawLines(mXAxis, axisPaint);
			
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
				matrix.mapPoints(points);
				points[1] = mXAxis[1];
				points[3] = mXAxis[1] - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, mDisplayMetrics);
				
				if(points[0] >= mXAxis[0]) {
					canvas.drawLines(points, axisPaint);
					
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
			canvas.drawLines(mYAxis, axisPaint);
		
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
				matrix.mapPoints(points);
				points[0] = mYAxis[0];
				points[2] = mYAxis[0] + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, mDisplayMetrics);
				
				if(points[1] <= mYAxis[3]) {
					canvas.drawLines(points, axisPaint);
	
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
		return String.valueOf(MathUtils.round(value, 1));
		//return String.valueOf(value);
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
