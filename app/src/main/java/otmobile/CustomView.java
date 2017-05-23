/** -------------------------------------------------------------------------
 * Copyright 2013-2016 EMC Corporation.  All rights reserved.
 ---------------------------------------------------------------------------- */
package otmobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import emc.captiva.mobile.sdk.CaptureWindow;
import emc.captiva.mobile.sdk.PictureCallback;

/**
 * This represents a custom view that will be used to fully replace the
 * SDK's overlay if needed.
 */
class CustomView extends RelativeLayout {
    private final static String TAG = CustomView.class.getSimpleName();
    private CustomWindow _capWnd = null;
    private Paint _green = new Paint();
    private Paint _white = new Paint();
    private Paint _yellow = new Paint();
    private RectF _bubbleRect = new RectF(0, 0, 0, 0);
    private PointF _bubblePos = new PointF(0, 0);
    private int _strokeWidth = 10;
    private Display _display = null;
    private Rect _cropRect = new Rect(0, 0, 0, 0);
    private LinearLayout _buttonBar = null;
    private boolean _cropRectSet = false;

    /**
     * Context based constructor to avoid 'missing constructor used for tools' validation warning.
     * @param context The current context.
     */
    CustomView(Context context) {
        super(context);
    }

    /**
     * Constructor for CustomView.
     * @param capWnd We pass in our instance of CaptureWindow so that
     *               we can retrieve some state from it.
     */
    CustomView(CustomWindow capWnd) {
        // Use the Context based constructor instead of super to avoid 'symbol not used' validation warning.
        this(capWnd.getActivity());

        // Setup state.
        Context context = capWnd.getActivity();
        _capWnd = capWnd;
        setWillNotDraw(false); // This allows us to draw.

        // We use the window service to get display orientations of the window itself.
        _display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        // Setup the paint
        _green.setColor(Color.GREEN);
        _green.setStrokeWidth(_strokeWidth);
        _green.setAlpha((int) (255 * .75f));

        _white.setColor(Color.WHITE);
        _white.setStrokeWidth(_strokeWidth);
        _white.setAlpha((int) (255 * .75f));

        _yellow.setColor(Color.YELLOW);
        _yellow.setStrokeWidth(5);
        _yellow.setStyle(Paint.Style.STROKE);

        // Add a label and a button to our view.
        addButtons();
	}

    /**
     * This is called when the view is detached from a window. At this point
     * it no longer has a surface for drawing.
     */
    @Override
    protected void onDetachedFromWindow () {
        super.onDetachedFromWindow();

        // The view will be detached from the window, so perform any
        // cleanup here.
    }

    /**
     * If you wanted to perform some drawing on the overlay, you would do it here.
     * @param canvas The canvas to use for drawing.
     */
    @SuppressWarnings("SuspiciousNameCombination")
	@Override
	protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if ( _capWnd.useMotion()) {
            // Compute size of bubble. Stoke width's are centered on the line,
            // but we are taking the whole width to give a bit of space around the circle.
            float bubbleSize = Math.min(canvas.getWidth(), canvas.getHeight()) * .20f;
            _bubbleRect.left = canvas.getWidth() - (int) bubbleSize - _strokeWidth;
            _bubbleRect.top = _strokeWidth;
            _bubbleRect.right = canvas.getWidth() - _strokeWidth;
            _bubbleRect.bottom = _strokeWidth + (int) bubbleSize;

            // If we are fairly near the center of the circle then color it green.
            Paint pt = _white;
            if (_bubblePos.x <= _bubbleRect.centerX() + 5 && _bubblePos.x >= _bubbleRect.centerX() - 5 &&
                    _bubblePos.y <= _bubbleRect.centerY() + 5 && _bubblePos.y >= _bubbleRect.centerY() - 5) {
                pt = _green;
            }

            // Draw bubble.
            pt.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(_bubbleRect.left + (int) bubbleSize / 2, _bubbleRect.top + (int) bubbleSize / 2, (int) bubbleSize / 2, pt);
            pt.setStyle(Paint.Style.FILL);
            canvas.drawCircle(_bubblePos.x, _bubblePos.y, bubbleSize / 10, pt);
        }

        // Set and get back a valid crop rectangle for this preview and configured aspect ratio.
        if (!_cropRectSet) {

            _cropRect.left = 0;
            _cropRect.top = 0;
            if (getDisplayOrient() == 1) {
                // PORTRAIT
                _cropRect.right = canvas.getWidth();
                _cropRect.bottom = canvas.getHeight() - _buttonBar.getHeight();
            } else {
                // LANDSCAPE
                _cropRect.right = canvas.getWidth() - _buttonBar.getWidth();
                _cropRect.bottom = canvas.getHeight();
            }

            _cropRect = _capWnd.setCropZoneForPreview(_cropRect);
            _cropRectSet = true; // We did our calculations.
        }

        // Draw the crop rectangle.
        if (!_cropRect.isEmpty()) {
            canvas.drawRect(_cropRect, _yellow);
        }
    }

    /**
     * An internal method for updating the bubble with the accelerometer data.
     * @param x The accelerometer data for X axis.
     * @param y The accelerometer data for Y axis.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    void updateBubble(float x, float y) {
        float xPos = 0, yPos = 0, positionUnits, rotation = getDisplayOrient();

        // We have to have our circle to get started, if we don't have this then we do nothing.
        if (!_bubbleRect.isEmpty()) {

            // Adjust for orientation as the numbers may need to be flipped.
            // Only one type of portrait is supported on the camera preview.
            // For landscape we have two types, left up, or right up and the camera preview
            // will change. We ignore others as bottom-up is not a supported camera preview.
            // So, we have to factor this into our formula.
            if (rotation == 1) {
                // PORTRAIT
                xPos = x;
                yPos = y * -1; // Positive should move up, negative moves down.
            } else if (rotation == 2) {
                // LANDSCAPE-RIGHTUP
                xPos = y * -1;
                yPos = x * -1;
            } else if (rotation == 3) {
                // LANDSCAPE-LEFTUP
                xPos = y;
                yPos = x;
            }

            // We are providing +/- 100 positions horizontally and vertically in our bubble.
            // Adjust the positions to be one of those positions. So, we take the diameter
            // of the bubble, divide by 200 to give us 100 positions for each pos or neg
            // radius direction. Then we take our accelerometer data (+/- 10) and multiple by 10
            // to give range up to +/- 100. Then we position it from the center by adjusting the
            // for each position.
            positionUnits = _bubbleRect.width() / 200;
            xPos = (positionUnits * 10 * xPos) + _bubbleRect.centerX();
            yPos = (positionUnits * 10 * yPos) + _bubbleRect.centerY();

            // Ensure we didn't overflow outside our circle.
            xPos = Math.max(Math.min(xPos, _bubbleRect.right), _bubbleRect.left);
            yPos = Math.max(Math.min(yPos, _bubbleRect.bottom), _bubbleRect.top);

            // Update the position of the bubble only if we need to do it.
            if (_bubblePos.x != xPos || _bubblePos.y != yPos) {
                _bubblePos.x = xPos;
                _bubblePos.y = yPos;
                invalidate((int)_bubbleRect.left - _strokeWidth,
                        (int)_bubbleRect.top - _strokeWidth,
                        (int)_bubbleRect.right + _strokeWidth,
                        (int)_bubbleRect.bottom + _strokeWidth);
            }
        }
    }

    /**
     * A method to create a button.
     */
    private void addButtons() {
        final CaptureWindow wnd = _capWnd;
        int rotation = getDisplayOrient();

        // Create a simple cancel button.
        Button cancelBtn = new Button(_capWnd.getActivity());
        cancelBtn.setText("Cancel");
        cancelBtn.setAlpha(0.75f);

        // Wire up a click handler to call CaptureWindow's beginTakePicture().
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wnd.cancelPicture(PictureCallback.REASON_CANCEL);
            }
        });

        // Create a simple button that says "Capture" and adjusts its alpha so image shows through.
        Button captureBtn = new Button(_capWnd.getActivity());
        captureBtn.setText("Capture");
        captureBtn.setAlpha(0.75f);

        // Wire up a click handler to call CaptureWindow's beginTakePicture().
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    wnd.beginTakePicture();
                } catch (Exception ex)
                {
                    Log.w(TAG, ex.getMessage());
                }
            }
        });

        // Put the buttons at the bottom centered.
        _buttonBar = new LinearLayout(_capWnd.getActivity());
        RelativeLayout.LayoutParams captureLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (rotation == 1) {
            // PORTRAIT
            _buttonBar.setOrientation(LinearLayout.HORIZONTAL); // For the linear layout container of our buttons.
            captureLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            captureLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        } else {
            // LANDSCAPE
            _buttonBar.setOrientation(LinearLayout.VERTICAL); // For the linear layout container of our buttons.
            captureLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            captureLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        }

        // Set the layout information into our container.
        _buttonBar.setLayoutParams(captureLayoutParams);
        _buttonBar.addView(cancelBtn);
        _buttonBar.addView(captureBtn);
        addView(_buttonBar);
    }

    /**
     * Determine the orientation of the window.
     * @return The rotation will either be 1=portrait, 2=landscape, 3=reverse-landscape,
     * 4=reverse-portrait.
     */
    private int getDisplayOrient() {
        // Orientation listeners give degrees, but we need to determine the current position of the window,
        // in order to do this, we use the window service.
        final int rotationState = _display.getRotation();
        switch (rotationState) {
            case Surface.ROTATION_0:
                return 1; // Portrait
            case Surface.ROTATION_90:
                return 2; // Landscape, right-side up.
            case Surface.ROTATION_270:
                return 3; // Reverse landscape, left-side up.
            default:
                return 4; // Reverse portrait, bottom-up.
        }
    }
}
