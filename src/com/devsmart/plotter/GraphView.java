package com.devsmart.plotter;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.devsmart.android.BackgroundTask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GraphView extends View
{
    public static void drawBitmap(Canvas c, int width, int height, List<DataRenderer> data, RectF viewport, CoordinateSystem coordinateSystem)
    {
        CoordinateSystem mCoordCopy = coordinateSystem.copy();
        mCoordCopy.interpolate(viewport, new RectF(0, 0, width, height));

        try
        {
            c.save();
            c.scale(1, -1);
            c.translate(0, -c.getHeight());

            for (DataRenderer r : data)
            {
                r.draw(c, viewport, mCoordCopy);
            }
        }
        finally
        {
            c.restore();
        }
    }

    private static final String KEY_VIEWPORT = "viewport";
    private static final String KEY_SUPERINSTANCE = "superinstance";
    private static final long mAnimationTime = 1000;

    private final GestureDetector.SimpleOnGestureListener mSimpleGestureListener = new GestureDetector.SimpleOnGestureListener()
    {
        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            mTransformMatrix.postScale(1.3f, 1.3f, e.getX(), e.getY());
            invalidate();
            updateViewport();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            mTransformMatrix.postTranslate(-distanceX, -distanceY);
            invalidate();
            updateViewport();
            return true;
        }
    };

    private final XYScaleGestureDetector.SimpleOnScaleGestureListener mSimpleScaleGestureListener = new XYScaleGestureDetector.SimpleOnScaleGestureListener()
    {
        @Override
        public boolean onScale(XYScaleGestureDetector detector)
        {
            mTransformMatrix.postScale(detector.getXScaleFactor(), detector.getYScaleFactor(), detector.getFocusX(), detector.getFocusY());
            invalidate();
            updateViewport();
            return true;
        }
    };

    private final ExecutorService mDrawThread = Executors.newSingleThreadExecutor();
    private final LinkedList<DataRenderer> mPlotData = new LinkedList<DataRenderer>();
    private final Matrix mTransformMatrix = new Matrix();
    private final Paint mDrawPaint = new Paint();
    private final Paint mAxisLabelPaint = new Paint();
    private final Rect mPlotMargins = new Rect();

    private CoordinateSystem mCoordinateSystem = CoordinateSystem.createLinearSystem();
    private RectF mViewPort = new RectF();
    private Bitmap mFrontBuffer;
    private BackgroundDrawTask mBackgroundDrawTask;
    private GestureDetector mPanGestureDetector;
    private XYScaleGestureDetector mScaleGestureDetector;
    private AxisRenderer mAxisRenderer;
    private Rect mGraphArea;
    private RectF mViewPortBounds;
    private Interpolator mAnimationInterpolator = null;
    private RectF mAnimationDest;
    private long mAnimationEndTime = 0;

    public GraphView(Context context)
    {
        super(context);
        mAxisRenderer = new SimpleAxisRenderer(getContext());
        init();
    }

    public GraphView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.GraphView, 0, 0);

        mAxisRenderer = new SimpleAxisRenderer(getContext());

        mAxisRenderer.setAxisColor(a.getInteger(R.styleable.GraphView_axisColor, Color.BLACK));
        mAxisRenderer.setLabelColor(a.getInteger(R.styleable.GraphView_axisColor, Color.DKGRAY));

        a.recycle();

        init();
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
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

    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        mGraphArea = mAxisRenderer.measureGraphArea(w, h);
        mCoordinateSystem.interpolate(mViewPort, new RectF(0, 0, mGraphArea.width(), mGraphArea.height()));

        drawFrame(mViewPort);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        cancelAnimation();

        final int action = MotionEventCompat.getActionMasked(event);
        switch (action)
        {
            case MotionEvent.ACTION_UP:
                doBoundsCheck();
                break;
        }

        boolean retval = mPanGestureDetector.onTouchEvent(event);
        retval |= mScaleGestureDetector.onTouchEvent(event);

        return retval;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        doAnimation();
        if (mFrontBuffer != null)
        {
            canvas.save();
            canvas.translate(mGraphArea.left, mGraphArea.top);
            canvas.drawBitmap(mFrontBuffer, mTransformMatrix, mDrawPaint);
            canvas.restore();
        }

        mAxisRenderer.drawAxis(canvas, getMeasuredWidth(), getMeasuredHeight(), getDisplayViewPort(), getCoordinateSystem());
    }

    public AxisRenderer getAxisRenderer()
    {
        return mAxisRenderer;
    }

    public void addSeries(DataRenderer series)
    {
        mPlotData.add(series);
        drawFrame(mViewPort);
    }

    public void removeSeries(DataRenderer series)
    {
        mPlotData.remove(series);
        drawFrame(mViewPort);
    }

    public void setViewportBounds(RectF bounds)
    {
        mViewPortBounds = bounds;
    }

    public void doBoundsCheck()
    {
        if (mViewPortBounds != null)
        {
            RectF newViewport = getDisplayViewPort();
            if (newViewport.width() > mViewPortBounds.width())
            {
                newViewport.left = mViewPortBounds.left;
                newViewport.right = mViewPortBounds.right;
            }
            if (newViewport.height() > mViewPortBounds.height())
            {
                newViewport.top = mViewPortBounds.top;
                newViewport.bottom = mViewPortBounds.bottom;
            }
            if (newViewport.left < mViewPortBounds.left)
            {
                newViewport.offset(mViewPortBounds.left - newViewport.left, 0);
            }
            if (newViewport.right > mViewPortBounds.right)
            {
                newViewport.offset(mViewPortBounds.right - newViewport.right, 0);
            }
            if (newViewport.bottom > mViewPortBounds.bottom)
            {
                newViewport.offset(0, mViewPortBounds.bottom - newViewport.bottom);
            }
            if (newViewport.top < mViewPortBounds.top)
            {
                newViewport.offset(0, mViewPortBounds.top - newViewport.top);
            }

            mAnimationInterpolator = new DecelerateInterpolator();
            mAnimationEndTime = System.currentTimeMillis() + mAnimationTime;
            mAnimationDest = newViewport;
            invalidate();
        }
    }

    public RectF getDisplayViewPort()
    {
        RectF rect = new RectF(0, 0, mGraphArea.width(), mGraphArea.height());

        Matrix m = new Matrix();
        mTransformMatrix.invert(m);
        m.postScale(1, -1);
        m.postTranslate(0, mGraphArea.height());
        m.mapRect(rect);

        mCoordinateSystem.getInverse().mapRect(rect);

        return rect;
    }

    public CoordinateSystem getCoordinateSystem()
    {
        CoordinateSystem retval = mCoordinateSystem.copy();
        retval.interpolate(getDisplayViewPort(), new RectF(0, 0, mGraphArea.width(), mGraphArea.height()));

        return retval;
    }

    public void setDisplayViewPort(RectF viewport)
    {
        mTransformMatrix.reset();
        mViewPort = new RectF(viewport);

        if (mGraphArea != null)
        {
            mCoordinateSystem.interpolate(mViewPort, new RectF(0, 0, mGraphArea.width(), mGraphArea.height()));
            drawFrame(viewport);
        }
    }

    public void updateViewport()
    {
        RectF newViewport = getDisplayViewPort();
        drawFrame(newViewport);
    }

    private void init()
    {
        mPanGestureDetector = new GestureDetector(mSimpleGestureListener);
        mScaleGestureDetector = new XYScaleGestureDetector(getContext(), mSimpleScaleGestureListener);
        mDrawPaint.setFilterBitmap(true);
        mViewPort.set(0, 0, 1, 1);
        mTransformMatrix.reset();

        //defaults
        mPlotMargins.set(20, 0, 0, 20);
        mAxisLabelPaint.setColor(Color.DKGRAY);
        mAxisLabelPaint.setTextSize(15.0f);
        mAxisLabelPaint.setAntiAlias(true);
    }

    private void doAnimation()
    {
        if (mAnimationInterpolator != null)
        {
            long now = System.currentTimeMillis();
            if (mAnimationEndTime <= now)
            {
                mAnimationInterpolator = null;
                drawFrame(mAnimationDest);
                return;
            }

            float done = 1 - ((float) (mAnimationEndTime - now) / mAnimationTime);
            done = mAnimationInterpolator.getInterpolation(done);
            RectF newViewport = new RectF();
            RectF currentViewport = getDisplayViewPort();
            newViewport.left = (1 - done) * currentViewport.left + done * mAnimationDest.left;
            newViewport.right = (1 - done) * currentViewport.right + done * mAnimationDest.right;
            newViewport.top = (1 - done) * currentViewport.top + done * mAnimationDest.top;
            newViewport.bottom = (1 - done) * currentViewport.bottom + done * mAnimationDest.bottom;
            drawFrame(newViewport);
        }
    }

    private void cancelAnimation()
    {
        mAnimationInterpolator = null;
    }

    private void drawFrame(final RectF viewport)
    {
        if (mBackgroundDrawTask != null)
        {
            mBackgroundDrawTask.mCanceled = true;
        }

        mBackgroundDrawTask = new BackgroundDrawTask(viewport);
        BackgroundTask.runBackgroundTask(mBackgroundDrawTask, mDrawThread);
    }

    private final class BackgroundDrawTask extends BackgroundTask
    {
        private int width;
        private int height;
        private Bitmap mDrawBuffer;
        private boolean mCanceled = false;
        private final RectF viewport;
        private ArrayList<DataRenderer> mData;
        private CoordinateSystem mCoordCopy;

        public BackgroundDrawTask(RectF view)
        {
            this.viewport = new RectF(view);
            if (mCoordinateSystem == null || mGraphArea == null)
            {
                mCanceled = true;
                return;
            }

            this.width = mGraphArea.width();
            this.height = mGraphArea.height();
            this.mCoordCopy = mCoordinateSystem.copy();
            this.mCoordCopy.interpolate(viewport, new RectF(0, 0, width, height));

            this.mData = new ArrayList<DataRenderer>(mPlotData);
        }

        @Override
        public void onBackground()
        {
            if (!mCanceled)
            {
                mDrawBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
                Canvas c = new Canvas(mDrawBuffer);

                try
                {
                    c.save();
                    c.scale(1, -1);
                    c.translate(0, -c.getHeight());

                    for (DataRenderer r : mData)
                    {
                        r.draw(c, viewport, mCoordCopy);
                    }
                }
                finally
                {
                    c.restore();
                }
            }
        }

        @Override
        public void onAfter()
        {
            if (!mCanceled)
            {
                mFrontBuffer = mDrawBuffer;
                mViewPort = viewport;
                mTransformMatrix.reset();
                mCoordinateSystem = mCoordCopy;
                invalidate();
            }
            else if (mDrawBuffer != null)
            {
                mDrawBuffer.recycle();
            }
            mDrawBuffer = null;
        }
    }
}