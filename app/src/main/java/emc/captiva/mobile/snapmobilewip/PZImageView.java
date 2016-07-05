/** -------------------------------------------------------------------------
 * Copyright 2013-2016 EMC Corporation.  All rights reserved.
 ---------------------------------------------------------------------------- */

package emc.captiva.mobile.snapmobilewip;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/*
 * ImageView to Pan and Zoom
 * */
public class PZImageView extends ImageView {
    boolean _preventGesture = false;
    private float _scaleFactor = 1;
    private float _focusX = 0;
    private float _focusY = 0;
    private float _focusXPanAdjustment = 0;
    private float _focusYPanAdjustment = 0;
    private float _totfocusXPanAdjustment = 0;
    private float _totfocusYPanAdjustment = 0;
    private GestureDetector _gestureDetector;
	private ScaleGestureDetector _scaleDetector;
    
    /**
     * Constructor
     * @param context    The context for this object.
     * @param attributes The attributes given to the control 
     */
    public PZImageView(Context context, AttributeSet attributes) {
        super(context, attributes);
        _gestureDetector = new GestureDetector(context, new GestureListener());
        _scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }
    
    /* (non-Javadoc)
     * @see android.widget.ImageView#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {        
        // Scale or pan the canvas depending on the scale factor and focus adjustments.
        // Don't allow the focus coord adjustments to move beyond the image.
        int viewWidth = getWidth();
        int viewHeight = getHeight();                 
        Drawable drawable = getDrawable();
        
        if (drawable == null || drawable instanceof BitmapDrawable == false) {
        	return;
        }
        
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();                
        
        if (bitmap == null) {
        	return;
        }
        
        int deltaW = Math.max(0, (viewWidth - bitmap.getWidth()) / 2); // force range
        int deltaH = Math.max(0, (viewHeight - bitmap.getHeight()) / 2);
        
        _focusX = _focusX + _focusXPanAdjustment;
        _focusY = _focusY + _focusYPanAdjustment;
        _focusX = Math.max(deltaW, _focusX);
        _focusY = Math.max(deltaH, _focusY);
        _focusX = Math.min(_focusX, viewWidth - deltaW);
        _focusY = Math.min(_focusY, viewHeight - deltaH);
        
        canvas.save();
        canvas.scale(_scaleFactor, _scaleFactor, _focusX, _focusY);
        super.onDraw(canvas);
        canvas.restore();
    }    
    
    /* (non-Javadoc)
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        
        // Pass all touch events to our gesture detectors if we are allowed.
        if (_preventGesture == false && !_gestureDetector.onTouchEvent(event)) {            
            _scaleDetector.onTouchEvent(event);
        }       
        
        return true;
    }
    
    /**
     * Provides handling of general simple gestures. For our scenario we are just handling the 
     * double tab and the pan (aka scroll).
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        
        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDown(android.view.MotionEvent)
         */
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        
        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // We set the new focus coord and also toggle the zoom level either full in or out.
            _focusX = e.getX();
            _focusY = e.getY();
            _focusXPanAdjustment = 0;
            _focusYPanAdjustment = 0; 
            _totfocusXPanAdjustment = 0;
            _totfocusYPanAdjustment = 0;
            if (_scaleFactor == 1) {
                _scaleFactor = 5;
            } else {
                _scaleFactor = 1;
            }
            
            invalidate();
            return true;
        }
        
        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Don't allow panning if the image is completely visible and there is no zoom (i.e. scale factor is 1-to-1).
            if (_scaleFactor == 1) {
                _focusXPanAdjustment = 0;
                _focusYPanAdjustment = 0;   
                _totfocusXPanAdjustment = 0;
                _totfocusYPanAdjustment = 0;                
            } else {
                _focusXPanAdjustment = distanceX;
                _focusYPanAdjustment = distanceY;
                if (_focusXPanAdjustment != 0) {
                    _focusXPanAdjustment = _focusXPanAdjustment / _scaleFactor;
                }
                
                if (_focusYPanAdjustment != 0) {
                    _focusYPanAdjustment = _focusYPanAdjustment / _scaleFactor;
                }
                
                // Keep track of total adjustments for focus changes during scaling.
                _totfocusXPanAdjustment += _focusXPanAdjustment;
                _totfocusYPanAdjustment += _focusYPanAdjustment;
                
                // Rebuild UI.
                invalidate();
            } 
            
            return super.onScroll(e1, e2, distanceX, distanceY);
        }        
    }
    
    /**
     * Here we handle the scaling of the image.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        
        /* (non-Javadoc)
         * @see android.view.ScaleGestureDetector.SimpleOnScaleGestureListener#onScale(android.view.ScaleGestureDetector)
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            
            if (detector.isInProgress() && detector.getPreviousSpan() > 0f) {
                // Don't allow less than 1-to-1 or more than 5x zoom. Scaling will set a new focus center also.
                _scaleFactor *= detector.getScaleFactor();
                _scaleFactor = Math.max(1, Math.min(_scaleFactor, 5));
                
                // We just use a rough adjustment here to set a new focus 
                // by adjusting based on the amount of panning we have done so far.
                _focusX = _scaleDetector.getFocusX();
                _focusY = _scaleDetector.getFocusY();
                _focusXPanAdjustment = _totfocusXPanAdjustment;
                _focusYPanAdjustment = _totfocusYPanAdjustment;
                
                // Rebuild UI
                invalidate();
            }
            
            return true;
        }
    }
}
