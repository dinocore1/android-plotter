package com.devsmart.plotter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

public class LineGraphView extends GraphView {

	protected Paint mPointPaint = new Paint();

	public LineGraphView(Context context) {
		super(context);
		init();
	}
	
	public LineGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	protected void init() {
		mPointPaint.setColor(Color.GREEN);
		mPointPaint.setStrokeWidth(2.0f);
	}

	protected void drawGraph(Canvas canvas, RectF viewPort){

		float[] points;

		final int canvasWidth = canvas.getWidth();
		final int canvasHeight = canvas.getHeight();

		Matrix matrix = getViewportToScreenMatrix(canvasWidth, canvasHeight, viewPort); 
		
		//clear the canvas
		canvas.drawColor(mBackgroundColor);

		Paint axisPaint = new Paint();
		axisPaint.setColor(mAxisColor);
		axisPaint.setStrokeWidth(2);

		if(mDrawXAxis){
			//draw X axis
			points = new float[]{
					viewPort.left, 0,
					viewPort.right, 0
			};
			matrix.mapPoints(points);
			canvas.drawLines(points, axisPaint);
			
			points[0] = viewPort.left;
			points[1] = 0;
			matrix.mapPoints(points);
			canvas.drawText(String.valueOf(viewPort.left), points[0], points[1], mAxisLabelPaint);
			
			points[0] = viewPort.right;
			points[1] = 0;
			matrix.mapPoints(points);
			canvas.drawText(String.valueOf(viewPort.right), points[0], points[1], mAxisLabelPaint);
		}

		if(mDrawYAxis){
			//draw Y axis
			points = new float[]{
					0, viewPort.top,
					0, viewPort.bottom
			};
			matrix.mapPoints(points);
			canvas.drawLines(points, axisPaint);
			
			points[0] = 0;
			points[1] = viewPort.top;
			matrix.mapPoints(points);
			canvas.drawText(String.valueOf(viewPort.top), points[0], points[1], mAxisLabelPaint);
			
			points[0] = 0;
			points[1] = viewPort.bottom;
			matrix.mapPoints(points);
			canvas.drawText(String.valueOf(viewPort.bottom), points[0], points[1], mAxisLabelPaint);
		}
		
		points = new float[2];
		for(Series series : mSeries){
			for(int i=0;i<series.getLength();i++){
				series.getPoint(i, points);
				matrix.mapPoints(points);
				drawPointData(points, canvas);
			}
		}
		
	}
	

	private void drawPointData(float[] point, Canvas canvas) {
		canvas.drawPoints(point, mPointPaint);
		
	}

	


}
