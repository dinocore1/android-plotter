package com.devsmart.plotter;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.devsmart.BackgroundTask;

public abstract class GraphView extends View {

	public static enum Axis {
		X,
		Y
	}

	private static final String KEY_VIEWPORT = "viewport";

	private ExecutorService mDrawThread = Executors.newSingleThreadExecutor();

	private RectF mViewPort = new RectF();
	protected LinkedList<Series> mSeries = new LinkedList<Series>();

	private Bitmap mFrontBuffer;
	private Matrix mTransformMatrix = new Matrix();
	private Paint mDrawPaint = new Paint();
	private BackgroundDrawTask mBackgroundDrawTask;
	private GestureDetector mPanGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;

	//draw prefs
	protected boolean mDrawXAxis;
	protected boolean mDrawYAxis;
	protected int mAxisColor;
	protected Paint mAxisLabelPaint = new Paint();
	protected Rect mPlotMargins = new Rect();
	protected AxisLabelRenderer mAxisLabelRenderer;
	protected int mBackgroundColor;
	protected float mXAxisDevision;
	protected float mYAxisDevision;

	public GraphView(Context context) {
		super(context);
		init();
	}

	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		mPanGestureDetector = new GestureDetector(mSimpleGestureListener);
		mScaleGestureDetector = new ScaleGestureDetector(getContext(), mSimpleScaleGestureListener);
		mDrawPaint.setFilterBitmap(true);
		mViewPort.set(-1, -1, 1, 1);
		mTransformMatrix.reset();

		//defaults
		mDrawXAxis = true;
		mXAxisDevision = 1.0f;
		mDrawYAxis = true;
		mYAxisDevision = 1.0f;
		mPlotMargins.set(10, 10, 10, 10);
		mAxisColor = Color.DKGRAY;
		mAxisLabelPaint.setColor(Color.DKGRAY);
		mAxisLabelPaint.setTextSize(20.0f);
		mAxisLabelPaint.setAntiAlias(true);
		mBackgroundColor = Color.WHITE;
		mAxisLabelRenderer = new AxisLabelRenderer() {
			
			@SuppressLint("DefaultLocale")
			@Override
			public String renderAxisLabel(Axis axis, float value) {
				return String.format("%.3f", value);
			}
		};

	}
	
	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle retval = new Bundle();
		
		float[] viewportvalues = new float[4];
		viewportvalues[0] = mViewPort.left;
		viewportvalues[1] = mViewPort.top;
		viewportvalues[2] = mViewPort.right;
		viewportvalues[3] = mViewPort.bottom;
		retval.putFloatArray(KEY_VIEWPORT, viewportvalues);
		return retval;
	}
	
	protected void onRestoreInstanceState (Parcelable state) {
		Bundle bundle = (Bundle) state;
		float[] viewportvalues = bundle.getFloatArray(KEY_VIEWPORT);
		mViewPort.left = viewportvalues[0];
		mViewPort.top = viewportvalues[1];
		mViewPort.right = viewportvalues[2];
		mViewPort.bottom = viewportvalues[3];
		drawFrame(mViewPort);
	}

	public void addSeries(Series series) {
		mSeries.add(series);
		drawFrame(mViewPort);
	}

	public void removeSeries(Series series) {
		mSeries.remove(series);
		drawFrame(mViewPort);
	}

	protected Matrix getViewportToScreenMatrix(
			final int canvasWidth, final int canvasHeight,
			RectF viewPort){

		Matrix matrix = new Matrix();
		matrix.setRectToRect(viewPort, 
				//new RectF(mPlotMargins.left, mPlotMargins.top, canvasWidth-mPlotMargins.right, canvasHeight-mPlotMargins.bottom),
				new RectF(0,0,canvasWidth,canvasHeight),
				ScaleToFit.FILL);

		matrix.postScale(1, -1);
		matrix.postTranslate(0, canvasHeight);

		return matrix;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		drawFrame(mViewPort);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		final int action = MotionEventCompat.getActionMasked(event);
		switch(action){
		case MotionEvent.ACTION_UP:
			updateViewport();
			break;
		}

		boolean retval = mPanGestureDetector.onTouchEvent(event);
		retval |= mScaleGestureDetector.onTouchEvent(event);
		return retval;
	}

	protected void updateViewport(){
		RectF newViewport = getDisplayViewPort();
		drawFrame(newViewport);
	}

	public RectF getDisplayViewPort(){
		Matrix m = new Matrix();
		mTransformMatrix.invert(m);

		RectF screen = new RectF(0,0, getMeasuredWidth(), getMeasuredHeight());
		m.mapRect(screen);

		Matrix viewPortTransform = getViewportToScreenMatrix(getMeasuredWidth(), getMeasuredHeight(), mViewPort);
		Matrix screenToViewPort = new Matrix();
		viewPortTransform.invert(screenToViewPort);

		screenToViewPort.mapRect(screen);
		return screen;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(mFrontBuffer != null){
			canvas.drawBitmap(mFrontBuffer, mTransformMatrix, mDrawPaint);
		}
	}

	protected abstract void drawGraph(Canvas canvas, RectF viewPort);


	private void drawFrame(final RectF viewport) {
		if(mBackgroundDrawTask != null){
			mBackgroundDrawTask.mCanceled = true;
		}
		mBackgroundDrawTask = new BackgroundDrawTask(getMeasuredWidth(), getMeasuredHeight(), viewport);
		BackgroundTask.runBackgroundTask(mBackgroundDrawTask, mDrawThread);
	}

	private class BackgroundDrawTask extends BackgroundTask {

		private int width;
		private int height;
		private Bitmap mDrawBuffer;
		private boolean mCanceled = false;
		private final RectF viewport;

		public BackgroundDrawTask(int width, int height, RectF viewport){
			this.width = width;
			this.height = height;
			this.viewport = new RectF(viewport);
		}

		@Override
		public void onBackground() {
			if(!mCanceled){
				mDrawBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
				drawGraph(new Canvas(mDrawBuffer), viewport);
			}
		}

		@Override
		public void onFinished() {
			if(!mCanceled){
				mFrontBuffer = mDrawBuffer;
				mViewPort = viewport;
				mTransformMatrix.reset();
				invalidate();
				//mBackgroundDrawTask = null;
			}
		}


	}

	private GestureDetector.SimpleOnGestureListener mSimpleGestureListener = new GestureDetector.SimpleOnGestureListener(){

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			mTransformMatrix.postTranslate(-distanceX, -distanceY);
			invalidate();
			updateViewport();
			return true;
		}

	};

	private ScaleGestureDetector.SimpleOnScaleGestureListener mSimpleScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener(){

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float scale = detector.getScaleFactor();
			mTransformMatrix.postScale(scale, scale, detector.getFocusX(), detector.getFocusY());
			invalidate();
			updateViewport();
			return true;

		}

	};

	protected void drawAxis(Canvas canvas, RectF viewPort) {

		DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
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

			float xPoint = 	(float) (mXAxisDevision *  Math.floor(viewPort.left / mXAxisDevision));
			while(xPoint < viewPort.right+mXAxisDevision/2){
				if(xPoint != 0.0f){
					points[0] = xPoint;
					points[1] = 0;
					points[2] = xPoint;
					points[3] = 0;
					matrix.mapPoints(points);
					points[1] -= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
					points[3] += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
					canvas.drawLines(points, axisPaint);

					String label = mAxisLabelRenderer.renderAxisLabel(Axis.X, xPoint);
					float textWidth = mAxisLabelPaint.measureText(label);
					canvas.drawText(label,
							points[0]-textWidth/2,
							points[1] - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics),
							mAxisLabelPaint);

				}
				xPoint += mXAxisDevision;

			}


		}

		Rect bounds = new Rect();
		if(mDrawYAxis){
			//draw Y axis
			points = new float[]{
					0, viewPort.top,
					0, viewPort.bottom
			};
			matrix.mapPoints(points);
			canvas.drawLines(points, axisPaint);

			float yPoint = 	(float) (mYAxisDevision *  Math.floor(viewPort.top / mYAxisDevision));
			while(yPoint < viewPort.bottom+mYAxisDevision/2){
				if(yPoint != 0.0f){
					points[0] = 0;
					points[1] = yPoint;
					points[2] = 0;
					points[3] = yPoint;
					matrix.mapPoints(points);
					points[0] -= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
					points[2] += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
					canvas.drawLines(points, axisPaint);

					String label = mAxisLabelRenderer.renderAxisLabel(Axis.Y, yPoint);
					mAxisLabelPaint.getTextBounds(label, 0, label.length(), bounds);
					float textWidth = mAxisLabelPaint.measureText(label);
					canvas.drawText(label,
							points[0]-textWidth-TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics),
							points[1]+bounds.height()/2,
							mAxisLabelPaint);
				}
				yPoint += mYAxisDevision;
			}

		}




	}
	
	public void autoScaleDomainAndRange() {
		float[] point = new float[2];
		RectF viewPort = new RectF();
		viewPort.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
				Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
		for(Series series : mSeries){
			for(int i=0;i<series.getLength();i++){
				series.getPoint(i, point);
				viewPort.left = Math.min(viewPort.left, point[0]);
				viewPort.right = Math.max(viewPort.right, point[0]);
				viewPort.top = Math.min(viewPort.top, point[1]);
				viewPort.bottom = Math.max(viewPort.bottom, point[1]);
			}
		}
		drawFrame(viewPort);
	}



}
