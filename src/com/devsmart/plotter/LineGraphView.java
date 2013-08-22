package com.devsmart.plotter;

import java.util.Iterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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

		//clear the canvas
		canvas.drawColor(mBackgroundColor);
		
		//drawAxis(canvas, viewPort);

		Matrix matrix = getViewportToScreenMatrix(new RectF(0,0,canvas.getWidth(), canvas.getHeight()), viewPort); 

		RectF pixel = new RectF();

		RectF pixelBin = new RectF();
		float xBinWidth = viewPort.width()/canvas.getWidth();
		pixelBin.left = viewPort.left;
		pixelBin.right = pixelBin.left+xBinWidth;
		pixelBin.bottom = Float.POSITIVE_INFINITY;
		pixelBin.top = Float.NEGATIVE_INFINITY;

		float[] lastpoint = new float[2];
		float[] output = new float[2];
		for(Series series : mSeries){
			Iterator<float[]> it = series.createIterator();
			while(it.hasNext()){
				float[] point = it.next();
				
				if(point[0] < viewPort.left || point[0] > viewPort.right){
					continue;
				}


				while(point[0] >= pixelBin.right){

					if(pixelBin.bottom != Float.POSITIVE_INFINITY){
						//draw pixel
						matrix.mapRect(pixel, pixelBin);
						
						/*
						if(pixel.height() > 0){
							canvas.drawRect(pixel, mPointPaint);
						} else {
							canvas.drawPoint(pixel.left, pixel.top, mPointPaint);
						}
						*/
						
						canvas.drawLine(lastpoint[0], lastpoint[1], pixel.left, pixel.top, mPointPaint);
						
						lastpoint[0] = pixel.left;
						lastpoint[1] = pixel.top;
					}

					pixelBin.offset(xBinWidth, 0);
					pixelBin.bottom = Float.POSITIVE_INFINITY;
					pixelBin.top = Float.NEGATIVE_INFINITY;
				}

				pixelBin.bottom = Math.min(pixelBin.bottom, point[1]);
				pixelBin.top = Math.max(pixelBin.top, point[1]);

				/*


				if(point[0] >= viewPort.left &&
					point[0] <= viewPort.right //&&
					//point[1] >= viewPort.top &&
					//point[1] <= viewPort.bottom
					){
					matrix.mapPoints(output, point);
					drawPointData(output, canvas);

					canvas.drawLine(lastpoint[0], lastpoint[1], output[0], output[1], mPointPaint);

					lastpoint[0] = output[0];
					lastpoint[1] = output[1];
				}
				 */
			}
		}

		//draw pixel
		matrix.mapRect(pixel, pixelBin);
		canvas.drawRect(pixel, mPointPaint);

	}


	private void drawPointData(float[] point, Canvas canvas) {
		canvas.drawPoints(point, mPointPaint);

	}




}
