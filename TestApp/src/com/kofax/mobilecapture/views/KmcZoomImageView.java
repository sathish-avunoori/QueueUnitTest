package com.kofax.mobilecapture.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import com.kofax.mobilecapture.utilities.Globals;

public class KmcZoomImageView extends ImageView
{
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private static final int CLICK = 3;
    private int mode = NONE;


    private float minScale = 1f;
    private float maxScale = 4f;
    private float[] matrixValues;


    private float XSpace, YSpace;
    private float width, height;
    private float ChangeScale = 1f;
    private float right, bottom;
    private float origWidth, origHeight, bmWidth, bmHeight;

    private PointF lastPoint = new PointF();
    private PointF startPoint = new PointF();
    private Matrix matrix = new Matrix();

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector gestureDetector;
    private Handler mZoomHandler;

    public KmcZoomImageView(Context context, AttributeSet attr)
    {
        super(context, attr);
        //super.setClickable(true);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context,new GestureListener());
        matrix.setTranslate(1f, 1f);
        matrixValues = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener(new OnTouchListener()
        {

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                mScaleDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);
                matrix.getValues(matrixValues);
                float x = matrixValues[Matrix.MTRANS_X];
                float y = matrixValues[Matrix.MTRANS_Y];
                PointF currentPoint = new PointF(event.getX(), event.getY());

                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        lastPoint.set(event.getX(), event.getY());
                        startPoint.set(lastPoint);
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        lastPoint.set(event.getX(), event.getY());
                        startPoint.set(lastPoint);
                        mode = ZOOM;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == ZOOM || (mode == DRAG && ChangeScale > minScale))
                        {
                            float deltaX = currentPoint.x - lastPoint.x;
                            float deltaY = currentPoint.y - lastPoint.y;
                            float scaleWidth = Math.round(origWidth * ChangeScale);
                            float scaleHeight = Math.round(origHeight * ChangeScale);
                            if (scaleWidth < width)
                            {
                                deltaX = 0;
                                if (y + deltaY > 0)
                                    deltaY = -y;
                                else if (y + deltaY < -bottom)
                                    deltaY = -(y + bottom);
                            }
                            else if (scaleHeight < height)
                            {
                                deltaY = 0;
                                if (x + deltaX > 0)
                                    deltaX = -x;
                                else if (x + deltaX < -right)
                                    deltaX = -(x + right);
                            }
                            else
                            {
                                if (x + deltaX > 0)
                                    deltaX = -x;
                                else if (x + deltaX < -right)
                                    deltaX = -(x + right);

                                if (y + deltaY > 0)
                                    deltaY = -y;
                                else if (y + deltaY < -bottom)
                                    deltaY = -(y + bottom);
                            }

                            matrix.postTranslate(deltaX, deltaY);
                            if(deltaX == 0 || deltaY == 0){
                                // gestureDetector.onTouchEvent(event);
                            }
                            lastPoint.set(currentPoint.x, currentPoint.y);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        mode = NONE;
                        int xDiff = (int) Math.abs(currentPoint.x - startPoint.x);
                        int yDiff = (int) Math.abs(currentPoint.y - startPoint.y);
                        if (xDiff < CLICK && yDiff < CLICK) {
                            performClick();
                            if(mZoomHandler != null) {
                                Message msg = new Message();
                                msg.what = Globals.Messages.MESSAGE_IMAGE_FLIP.ordinal();
                                msg.arg1 = 3;
                                mZoomHandler.sendMessage(msg);
                            }
                        }
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                }
                if(getDrawable() != null){
                    setImageMatrix(matrix);
                    invalidate();
                }
                return true;
            }

        });
    }

    @Override
    public void setImageBitmap(Bitmap bm)
    {
        super.setImageBitmap(bm);
        bmWidth = bm.getWidth();
        bmHeight = bm.getHeight();
    }

    public void setHandler(Handler handler){
        mZoomHandler = handler;
    }

    public void setMaxZoom(float x)
    {
        maxScale = x;
    }


    private class GestureListener extends SimpleOnGestureListener  {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                //If X axis difference value is more than Y axis then gesture swipe left or right
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD
                            && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        float[] values = null;
                    	
                    	Matrix mat = getImageMatrix();
                    	values = new float[9];
                    	mat.getValues(values);                    	                    	
                    	if((values[Matrix.MSCALE_X] <= 1) && (values[Matrix.MSCALE_Y] <= 1)){
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                    	}
                    	values = null;
                    }
                    result = true;

                } //If Y axis difference value is more than X axis then gesture swipe top or bottom
                else if (Math.abs(diffY) > SWIPE_THRESHOLD
                        && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                }
                result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }

        public void onSwipeRight() {
            Message msg = new Message();
            msg.what = Globals.Messages.MESSAGE_IMAGE_FLIP.ordinal();
            msg.arg1 = 1;
            mZoomHandler.sendMessage(msg);
            Log.d(VIEW_LOG_TAG, "on right");
        }

        public void onSwipeLeft() {
            Log.d(VIEW_LOG_TAG, "on left");
            Message msg = new Message();
            msg.what = Globals.Messages.MESSAGE_IMAGE_FLIP.ordinal();
            msg.arg1 = 2;
            mZoomHandler.sendMessage(msg);
        }

        public void onSwipeTop() {
        }

        public void onSwipeBottom() {
        }
    }





    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            float mScaleFactor = detector.getScaleFactor();
            float origScale = ChangeScale;
            ChangeScale *= mScaleFactor;
            if (ChangeScale > maxScale)
            {
                ChangeScale = maxScale;
                mScaleFactor = maxScale / origScale;
            }
            else if (ChangeScale < minScale)
            {
                ChangeScale = minScale;
                mScaleFactor = minScale / origScale;
            }
            right = width * ChangeScale - width - (2 * XSpace * ChangeScale);
            bottom = height * ChangeScale - height - (2 * YSpace * ChangeScale);
            if (origWidth * ChangeScale <= width || origHeight * ChangeScale <= height)
            {
                matrix.postScale(mScaleFactor, mScaleFactor, width / 2, height / 2);
                if (mScaleFactor < 1)
                {
                    matrix.getValues(matrixValues);
                    float x = matrixValues[Matrix.MTRANS_X];
                    float y = matrixValues[Matrix.MTRANS_Y];
                    if (mScaleFactor < 1)
                    {
                        if (Math.round(origWidth * ChangeScale) < width)
                        {
                            if (y < -bottom)
                                matrix.postTranslate(0, -(y + bottom));
                            else if (y > 0)
                                matrix.postTranslate(0, -y);
                        }
                        else
                        {
                            if (x < -right)
                                matrix.postTranslate(-(x + right), 0);
                            else if (x > 0)
                                matrix.postTranslate(-x, 0);
                        }
                    }
                }
            }
            else
            {
                matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
                matrix.getValues(matrixValues);
                float x = matrixValues[Matrix.MTRANS_X];
                float y = matrixValues[Matrix.MTRANS_Y];
                if (mScaleFactor < 1) {
                    if (x < -right)
                        matrix.postTranslate(-(x + right), 0);
                    else if (x > 0)
                        matrix.postTranslate(-x, 0);
                    if (y < -bottom)
                        matrix.postTranslate(0, -(y + bottom));
                    else if (y > 0)
                        matrix.postTranslate(0, -y);
                }
            }
            return true;
        }
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        try {
            float scale;
            float scaleX =  width / bmWidth;
            float scaleY = height / bmHeight;
            scale = Math.min(scaleX, scaleY);
            matrix.setScale(scale, scale);
                setImageMatrix(matrix);

            ChangeScale = 1f;


            YSpace = height - (scale * bmHeight);
            XSpace = width - (scale * bmWidth);
            YSpace /= 2;
            XSpace /= 2;

            matrix.postTranslate(XSpace, YSpace);

            origWidth = width - 2 * XSpace;
            origHeight = height - 2 * YSpace;
            right = width * ChangeScale - width - (2 * XSpace * ChangeScale);
            bottom = height * ChangeScale - height - (2 * YSpace * ChangeScale);
                setImageMatrix(matrix);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
