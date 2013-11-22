package com.devsmart.plotter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import android.view.View;
import android.widget.ZoomButtonsController;

import com.devsmart.BackgroundTask;

public class GraphView extends View {

	public static enum Axis {
		X,
		Y
	}

	private static final String KEY_VIEWPORT = "viewport";
	private static final String KEY_SUPERINSTANCE = "superinstance";
	
	private CoordinateSystem mCoordinateSystem = CoordinateSystem.createLinearSystem();

	private ExecutorService mDrawThread = Executors.newSingleThreadExecutor();

	private RectF mViewPort = new RectF();
	protected LinkedList<DataRenderer> mPlotData = new LinkedList<DataRenderer>();

	private Bitmap mFrontBuffer;
	private Matrix mTransformMatrix = new Matrix();
	private Paint mDrawPaint = new Paint();
	private BackgroundDrawTask mBackgroundDrawTask;
	private GestureDetector mPanGestureDetector;
	private XYScaleGestureDetector mScaleGestureDetector;

	//draw prefs
	protected boolean mDrawXAxis;
	protected boolean mDrawYAxis;
	protected int mAxisColor;
	protected Paint mAxisLabelPaint = new Paint();
	protected Rect mPlotMargins = new Rect();
	protected int mBackgroundColor;
	public float mXAxisDevision;
	public float mYAxisDevision;
	protected int mXAxisMargin;
	protected int mYAxisMargin;
	
	protected AxisRenderer mAxisRenderer;

	private ZoomButtonsController mZoomControls;
	private Rect mGraphArea;

	public GraphView(Context context) {
		super(context);
		init();
	}

	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		mAxisRenderer = new SimpleAxisRenderer(this);
		mPanGestureDetector = new GestureDetector(mSimpleGestureListener);
		mScaleGestureDetector = new XYScaleGestureDetector(getContext(), mSimpleScaleGestureListener);
		mDrawPaint.setFilterBitmap(true);
		mViewPort.set(0, 0, 1, 1);
		mTransformMatrix.reset();

		//defaults
		mDrawXAxis = true;
		mXAxisDevision = 1.0f;
		mDrawYAxis = true;
		mYAxisDevision = 1.0f;
		mPlotMargins.set(20, 0, 0, 20);
		mAxisColor = Color.DKGRAY;
		mAxisLabelPaint.setColor(Color.DKGRAY);
		mAxisLabelPaint.setTextSize(15.0f);
		mAxisLabelPaint.setAntiAlias(true);
		mBackgroundColor = Color.WHITE;

		mZoomControls = new ZoomButtonsController(this);
		mZoomControls.setAutoDismissed(true);
		mZoomControls.setOnZoomListener(mZoomButtonListener);

	}
	


	@Override
	protected Parcelable onSaveInstanceState() {
		
		Bundle retval = new Bundle();
		retval.putParcelable(KEY_SUPERINSTANCE, super.onSaveInstanceState());

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
		super.onRestoreInstanceState(bundle.getParcelable(KEY_SUPERINSTANCE));

		float[] viewportvalues = bundle.getFloatArray(KEY_VIEWPORT);
		mViewPort.left = viewportvalues[0];
		mViewPort.top = viewportvalues[1];
		mViewPort.right = viewportvalues[2];
		mViewPort.bottom = viewportvalues[3];
		drawFrame(mViewPort);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mZoomControls.setVisible(false);
	}
	
	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if(visibility != View.VISIBLE){
			mZoomControls.setVisible(false);
		}
	}

	public void addSeries(DataRenderer series) {
		mPlotData.add(series);
		drawFrame(mViewPort);
	}

	public void removeSeries(DataRenderer series) {
		mPlotData.remove(series);
		drawFrame(mViewPort);
	}



	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		mGraphArea = mAxisRenderer.measureGraphArea(w, h);
		mCoordinateSystem.interpolate(mViewPort, new RectF(0,0,mGraphArea.width(), mGraphArea.height()));
		
		drawFrame(mViewPort);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		mZoomControls.setVisible(true);

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
		RectF rect = new RectF(0,0,mGraphArea.width(), mGraphArea.height());
		
		Matrix m = new Matrix();
		mTransformMatrix.invert(m);
		m.postScale(1, -1);
		m.postTranslate(0, mGraphArea.height());
		m.mapRect(rect);
		
		mCoordinateSystem.getInverse().mapRect(rect);
		
		return rect;
	}
	
	public CoordinateSystem getCoordinateSystem() {
		CoordinateSystem retval = mCoordinateSystem.copy();
		retval.interpolate(getDisplayViewPort(), new RectF(0,0,mGraphArea.width(),mGraphArea.height()));
		return retval;
	}
	
	public void setDisplayViewPort(RectF viewport) {
		mTransformMatrix.reset();
		mViewPort = new RectF(viewport);
		mCoordinateSystem.interpolate(mViewPort, new RectF(0,0,mGraphArea.width(), mGraphArea.height()));
		drawFrame(viewport);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		if(mFrontBuffer != null){
			canvas.save();
			canvas.translate(mGraphArea.left, mGraphArea.top);
			canvas.drawBitmap(mFrontBuffer, mTransformMatrix, mDrawPaint);
			canvas.restore();
		}
		mAxisRenderer.drawAxis(canvas, getMeasuredWidth(), getMeasuredHeight(), getDisplayViewPort(), getCoordinateSystem());
	}

	private void drawFrame(final RectF viewport) {
		if(mBackgroundDrawTask != null){
			mBackgroundDrawTask.mCanceled = true;
		}
		mBackgroundDrawTask = new BackgroundDrawTask(viewport);
		BackgroundTask.runBackgroundTask(mBackgroundDrawTask, mDrawThread);
	}

	private class BackgroundDrawTask extends BackgroundTask {

		private int width;
		private int height;
		private Bitmap mDrawBuffer;
		private boolean mCanceled = false;
		private final RectF viewport;
		private ArrayList<DataRenderer> mData;
		private CoordinateSystem mCoordCopy;

		public BackgroundDrawTask(RectF view){
			
			this.viewport = new RectF(view);
			if(mCoordinateSystem == null || mGraphArea == null){
				mCanceled = true;
				return;
			}
			
			this.width = mGraphArea.width();
			this.height = mGraphArea.height();
			this.mCoordCopy = mCoordinateSystem.copy();
			this.mCoordCopy.interpolate(viewport, new RectF(0,0,width,height));
			
			
			this.mData = new ArrayList<DataRenderer>(mPlotData);
		}

		@Override
		public void onBackground() {
			if(!mCanceled){
				mDrawBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
				Canvas c = new Canvas(mDrawBuffer);
				
				try {
					c.save();
					c.scale(1, -1);
					c.translate(0, -c.getHeight());
					
					for(DataRenderer r : mData){
						r.draw(c, viewport, mCoordCopy);
					}
				}finally {
					c.restore();
				}
				
			}
		}

		@Override
		public void onAfter() {
			if(!mCanceled){
				mFrontBuffer = mDrawBuffer;
				mViewPort = viewport;
				mTransformMatrix.reset();
				mCoordinateSystem = mCoordCopy;
				invalidate();
			} else if(mDrawBuffer != null) {
				mDrawBuffer.recycle();
			}
		}


	}

	private GestureDetector.SimpleOnGestureListener mSimpleGestureListener = new GestureDetector.SimpleOnGestureListener(){

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}



		@Override
		public boolean onDoubleTap(MotionEvent e) {
			//autoScaleDomainAndRange();
			mTransformMatrix.postScale(1.3f, 1.3f, e.getX(), e.getY());
			invalidate();
			updateViewport();
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

	private XYScaleGestureDetector.SimpleOnScaleGestureListener mSimpleScaleGestureListener = new XYScaleGestureDetector.SimpleOnScaleGestureListener(){

		@Override
		public boolean onScale(XYScaleGestureDetector detector) {
			//float scale = detector.getScaleFactor();
			
			mTransformMatrix.postScale(detector.getXScaleFactor(), detector.getYScaleFactor(), detector.getFocusX(), detector.getFocusY());
			invalidate();
			updateViewport();
			return true;

		}

	};
	
	public static double roundToSignificantFigures(double num, int n) {
	    if(num == 0) {
	        return 0;
	    }

	    final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
	    final int power = n - (int) d;

	    final double magnitude = Math.pow(10, power);
	    final long shifted = Math.round(num*magnitude);
	    return shifted/magnitude;
	}
	
	public static RectF getSeriesLimits(Series series) {
		RectF retval = new RectF();
		retval.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
				Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
		
		Iterator<float[]> it = series.createIterator();
		while(it.hasNext()){
			float[] point = it.next();
			retval.left = Math.min(retval.left, point[0]);
			retval.right = Math.max(retval.right, point[0]);
			retval.top = Math.min(retval.top, point[1]);
			retval.bottom = Math.max(retval.bottom, point[1]);
		}
		
		return retval;
	}

	public void autoScaleDomainAndRange() {


		/*
		BackgroundTask.runBackgroundTask(new BackgroundTask() {

			RectF viewport = new RectF();

			@Override
			public void onBackground() {
				viewport.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
						Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
				for(Series series : mSeries){
					Iterator<float[]> it = series.createIterator();
					while(it.hasNext()){
						float[] point = it.next();
						viewport.left = Math.min(viewport.left, point[0]);
						viewport.right = Math.max(viewport.right, point[0]);
						viewport.top = Math.min(viewport.top, point[1]);
						viewport.bottom = Math.max(viewport.bottom, point[1]);
					}
				}
				
				RectF screen = new RectF(mPlotMargins.left, mPlotMargins.top, getMeasuredWidth(),getMeasuredHeight()-mPlotMargins.height());
				Matrix matrix = new Matrix();
				getViewportToScreenMatrix(screen, viewport).invert(matrix);
				matrix.mapRect(viewport, new RectF(0,0,getMeasuredWidth(), getMeasuredHeight()));
				

			}

			@Override
			public void onAfter() {
				drawFrame(viewport);
			}

		}, mDrawThread);
		*/

	}

	public void zoomInCenter() {
		float scale = 1.3f;
		mTransformMatrix.postScale(scale, scale, getMeasuredWidth()/2, getMeasuredHeight()/2);
		invalidate();
		updateViewport();
	}

	public void zoomOutCenter() {
		float scale = 0.7f;
		mTransformMatrix.postScale(scale, scale, getMeasuredWidth()/2, getMeasuredHeight()/2);
		invalidate();
		updateViewport();
	}

	private ZoomButtonsController.OnZoomListener mZoomButtonListener = new ZoomButtonsController.OnZoomListener(){

		@Override
		public void onVisibilityChanged(boolean visible) {}

		@Override
		public void onZoom(boolean zoomIn) {
			if(zoomIn) {
				zoomInCenter();
			} else {
				zoomOutCenter();
			}

		}

	};



}
