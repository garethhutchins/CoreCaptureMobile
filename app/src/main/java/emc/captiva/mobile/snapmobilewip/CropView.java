/** -------------------------------------------------------------------------
 * Copyright 2013-2016 EMC Corporation.  All rights reserved.
 ---------------------------------------------------------------------------- */

package emc.captiva.mobile.snapmobilewip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * This class provides cropping rectangle UI.
 */
public class CropView extends View {
	private final float TOUCH_RADIUS = 35.0f;
	private final int BOUNDS_COLOR = 0x7F00FF00; // Bounding box color, with transparent alpha.
	private final int TOUCH_COLOR = 0x7F0000FF; // Normal Circle color.
	private final int SELECTED_COLOR = 0x7FFF0000; // Selected circle color.
	
	private Point[] _bounds; // topLeft, bottomRight
	private Paint _boundsPaint;	
	private Point _selected;
	private Paint _touchPaint;
	private Paint _selectedPaint;
	private Rect _bitmapRect;
	private float _touchRadius;
	
	/**
	 * Constructor.
	 * @param context    The context for this object.
     * @param attributes The attributes given to the control 
     */
	public CropView(Context context, AttributeSet attributes) {
		super(context, attributes);
		
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		DisplayMetrics displayMetrics = new DisplayMetrics();
		
		// Use device independent sizes for the touch circles.  
		display.getMetrics(displayMetrics);
		_touchRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TOUCH_RADIUS, displayMetrics);
		
		// Build our paints.
		_boundsPaint = new Paint();		
		_boundsPaint.setColor(BOUNDS_COLOR);		
		_touchPaint = new Paint();
		_touchPaint.setColor(TOUCH_COLOR);		
		_selectedPaint = new Paint();
		_selectedPaint.setColor(SELECTED_COLOR);
	}	
	
	/* (non-Javadoc)
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	public boolean onTouchEvent(MotionEvent event) {
	    
	    // Handle the adjustments to the crop rectangle.
		switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
			    
			    // Set our selected circle to none.
				_selected = null;
				
				// For each corner in our bounding rectangle determine if the action touched our circle.
				// We allow a larger touch than the actual visible circle to allow ease-of-use.
				// If it is within our circle, then select it by changing its color.
				for (Point p : _bounds) {
					if (Math.abs(p.x - event.getX()) <= (_touchRadius) && Math.abs(p.y - event.getY()) <= (_touchRadius)) {
						_selected = p;
						invalidate();
						break;
					}
				}
				
				break;
			}
			
			case MotionEvent.ACTION_MOVE: {
			    
			    // If a circle was selected, then prepare to change the bounding box size. 
				if (_selected != null) {
				    
				    // Grab the coordinate for the move event.
					int x = (int)event.getX();
					int y = (int)event.getY();
					
					// Grab the width and height for the crop window.
					int left = getBitmapRect().left;
					int top = getBitmapRect().top;
					int right = getBitmapRect().right;
					int bottom = getBitmapRect().bottom;
					
					// Ensure that we are within the bounds of our crop window.
					x = x < left ? left : x;
					y = y < top ? top : y;
					x = x > right ? right : x;
					y = y > bottom ? bottom: y;

					// Set the selected coordinate and invalidate so that we update the UI.
					_selected.x = x;
					_selected.y = y;
					invalidate();
				}
				
				break;
			}
			
			case MotionEvent.ACTION_UP:	{

			    // De-select the circle and invalidate so that we update the UI.
				_selected = null;
				invalidate();
				break;
			}
		}
						
		return true;
	}	
	
	/**
	 * This sets the bitmap for which the crop rectangle will use. We
	 * use this to set the bounds of the box.
	 * @param bitmapRect    The bitmap rectangle.
	 */
	void setBitmapRect(Rect bitmapRect) {
	    if (bitmapRect == null || bitmapRect.isEmpty()) {
	        _bitmapRect = new Rect(0, 0, this.getWidth(), this.getHeight());
	    } else {
	        _bitmapRect = getValidRectInView(bitmapRect);
	    }
	}
	
	/**
	 * This provides safe access for the bitmap rectangle.
	 * @return    Return the bitmap rectangle.
	 */
	Rect getBitmapRect() {
        if (_bitmapRect == null || _bitmapRect.isEmpty()) {
            _bitmapRect = new Rect(0, 0, this.getWidth(), this.getHeight());
        } 

        return _bitmapRect;
    }
	
	/**
	 * Return the selection rectangle.
	 * @return    The selection rectangle.
	 */
	Rect getSelection() {
	    
	    // Prevent the selection from being inverted and ensure we take the visible rectangle as the crop rectangle.
		Point topLeft = new Point(Math.min(_bounds[0].x, _bounds[1].x),Math.min(_bounds[0].y, _bounds[1].y)); 
		Point bottomRight = new Point(Math.max(_bounds[0].x, _bounds[1].x),Math.max(_bounds[0].y, _bounds[1].y));	  
		Rect cropRect = new Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
		
		// Before return our selection bounding box, ensure it is within our view.
		Rect validRect = getValidRectInView(cropRect);
		return validRect;
	}

	/**
	 * Reset the rectangle minus our offset.
	 */
	void resetSelection() {
		_bounds[0] = new Point(getBitmapRect().left, getBitmapRect().top);						
		_bounds[1] = new Point(getBitmapRect().right, getBitmapRect().bottom);			
		invalidate();		
	}
	
	/**
	 * Set the selection rectangle.
	 * @param r    The rectangle to use for the selection.
	 */
	void setSelection(Rect r) {
	    
	    // Check to make sure we have a valid bounds. 
		if (_bounds == null) {
			_bounds = new Point[2];
		}

		// Set the crop rectangle.
		if (r == null || r.isEmpty()) {
		    // The rectangle given is invalid, so reset the rectangle.
		    resetSelection();
		}
		else
		{
		    // Apply the new selection rectangle to be our bounding box.
			Rect intersectRect = getValidRectInView(r);
			_bounds[0] = new Point(intersectRect.left, intersectRect.top);						
			_bounds[1] = new Point(intersectRect.right, intersectRect.bottom);
			invalidate();
		}		
	}
	
	/**
	 * Ensure the rectangle passed-in is within the bounds of the view.
	 * @param cropRect    The crop rectangle to use.
	 * @return            A valid rectangle for the view. 
	 */
	Rect getValidRectInView(Rect cropRect) {
		Rect viewRect = new Rect(0, 0, this.getWidth(), this.getHeight());
		int left   = Math.max( cropRect.left, viewRect.left);
		int right  = Math.min( cropRect.right, viewRect.right);
		int top    = Math.max( cropRect.top, viewRect.top );
		int bottom = Math.min( cropRect.bottom, viewRect.bottom);
		Rect validRect = new Rect( left, top, right, bottom);
		return validRect;
	}
	
	/* (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		// If the bounds hasn't been set, then set our default rectangle.
		if (_bounds == null) {
			_bounds = new Point[2];
			_bounds[0] = new Point(0, 0);						
			_bounds[1] = new Point(canvas.getWidth(), canvas.getHeight());			
		}

		// Draw the rectangle and the circle at upper-left and lower-right.
		canvas.drawRect(_bounds[0].x, _bounds[0].y, _bounds[1].x, _bounds[1].y, _boundsPaint);
		for (Point p : _bounds) {
			canvas.drawCircle(p.x, p.y, _touchRadius, _touchPaint);
		}
		
		// If a circle is selected, then paint it with our selection color. 		
		if (_selected != null) {
			canvas.drawCircle(_selected.x, _selected.y, _touchRadius, _selectedPaint);
		}
	}
}
