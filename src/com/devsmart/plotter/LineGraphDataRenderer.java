package com.devsmart.plotter;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

import com.devsmart.PeekableIterator;

public class LineGraphDataRenderer implements DataRenderer {

	protected Paint mPointPaint = new Paint();
	protected Series mSeries;

	public LineGraphDataRenderer(Series series, int color) {
		mSeries = series;
		mPointPaint.setColor(color);
		mPointPaint.setStrokeWidth(2.0f);
	}


	RectF pixel = new RectF();
	RectF pixelBin = new RectF();
	
	public void draw(Canvas canvas, RectF viewPort, CoordinateSystem coordSystem){

		try {
		canvas.save();
		canvas.scale(1, -1);
		canvas.translate(0, -canvas.getHeight());

		final float xBinWidth = viewPort.width()/canvas.getWidth();
		pixelBin.left = viewPort.left-xBinWidth;
		pixelBin.right = viewPort.left;
		pixelBin.bottom = Float.POSITIVE_INFINITY;
		pixelBin.top = Float.NEGATIVE_INFINITY;

		float[] lastpoint = new float[]{Float.NaN, Float.NaN};
		
			PeekableIterator<float[]> it = new PeekableIterator<float[]>(mSeries.createIterator());
			
			
			//findPixelBinLessThan(pixelBin, it);
			while(it.hasNext()){
				float[] point = it.next();
				lastpoint[0] = point[0];
				lastpoint[1] = point[1];
				if(it.peek()[0] > viewPort.left){
					break;
				}
			}
			
			coordSystem.mapPoints(lastpoint);
			
			boolean findOneMore = false;
			while(it.hasNext()){
				pixelBin.offset(xBinWidth, 0);
				pixelBin.bottom = Float.POSITIVE_INFINITY;
				pixelBin.top = Float.NEGATIVE_INFINITY;
				
				if(fillPixelBin(pixelBin, it)){
					//draw pixel
					coordSystem.mapRect(pixel, pixelBin);
					canvas.drawLine(lastpoint[0], lastpoint[1], pixel.left, pixel.top, mPointPaint);
					lastpoint[0] = pixel.left;
					lastpoint[1] = pixel.top;
					if(findOneMore) {
						break;
					}
				}
				if(it.peek()[0] > viewPort.right){
					findOneMore = true;
				}
				
			}
		} finally {
			canvas.restore();
		}
	}
	
	private boolean fillPixelBin(RectF pixelBin, PeekableIterator<float[]> it) {
		boolean retval = false;
		float[] point;
		while(it.hasNext()){
			point = it.peek();
			if(point[0] > pixelBin.right) {
				break;
			}
			
			if(point[0] >= pixelBin.left) {
				pixelBin.bottom = Math.min(pixelBin.bottom, point[1]);
				pixelBin.top = Math.max(pixelBin.top, point[1]);
				retval = true;
			}
			it.next();
		}
		return retval;
	}


}
