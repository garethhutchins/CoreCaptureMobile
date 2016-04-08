/** -------------------------------------------------------------------------
 * Copyright 2013-2016 EMC Corporation.  All rights reserved.
 ---------------------------------------------------------------------------- */

package emc.captiva.mobile.sdksampleapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Map;

import emc.captiva.mobile.sdk.CaptureImage;
import emc.captiva.mobile.sdk.CaptureWindow;
import emc.captiva.mobile.sdk.ContinuousCaptureCallback;

/**
 * A custom capture window.
 */
public class CustomWindow extends CaptureWindow {

    private final static String TAG = CustomWindow.class.getSimpleName();
    private enum OverlayMode
    {
        Replace,
        Extend,
        None
    }
    private OverlayMode _overlayMode = OverlayMode.None;
    private TextView _customLabel = null;
    private CustomView _view = null;
    private QuadView _quadView = null;
    private View _flashView = null;
    private boolean _useMotion = false;

    /**
     * Constructor for our custom window.
     * @param context This is the context for the activity.
     * @param pref The value of the preference used for take picture.
     */
    public CustomWindow(Context context, String pref, Map<String, Object> appParams) {
        // We could have retrieved the preferences here, but since we just did that,
        // we will just pass it in. So here, we determine from our preference whether
        // we are extending the existing SDK overlay or whether we are creating a brand new overlay.
        // Get the actual string representation for the "replace" mode to see if it matches the
        // preference that was actually specified. If not, then we don't do replace only extend.
        String newPref = CoreHelper.getStringResource(context, R.string.GPREF_CAPTURE_CUSTOM_OPTIONS_REPLACE);
        if (pref != null && pref.compareToIgnoreCase(newPref) == 0) {
            // We are in replace mode.
            _overlayMode = OverlayMode.Replace;
        }

        newPref = CoreHelper.getStringResource(context, R.string.GPREF_CAPTURE_CUSTOM_OPTIONS_EXTEND);
        if (pref != null && pref.compareToIgnoreCase(newPref) == 0) {
            // We are in replace mode.
            _overlayMode = OverlayMode.Extend;
        }

        _useMotion = appParams != null && ((boolean) appParams.get(MainActivity.USE_MOTION));
    }

    // Show the half-transparent white full-layout view for a quarter of a second.
    public void flashScreen() {
        _flashView.setVisibility(View.VISIBLE);
        _flashView.postDelayed(new Runnable() {
            @Override
            public void run() {
                _flashView.setVisibility(View.INVISIBLE);
            }
        }, 250);
    }

    boolean useMotion()
    {
        return _useMotion;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {

        // Set up the shutter flash view, which is a 50% transparent white full screen view.
        _flashView = new View(getActivity());
        _flashView.setVisibility(View.INVISIBLE);
        _flashView.setBackgroundColor(Color.parseColor("#CCFFFFFF"));
        _flashView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Get either the default overlay view or a new view depending on
        // whether our preference wants us to create a new view. If we are extending
        // then we just add a label in the middle of the window. If we are creating a
        // new view, then we display a leveling bubble in the upper-right corner with
        // a capture button at the bottom.
        if (_overlayMode == OverlayMode.Replace) {
            // Add new custom view to replace the SDK overlay.
            _view = new CustomView(this);
            setView(_view, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            _quadView = new QuadView(this.getActivity());
            RelativeLayout.LayoutParams qparams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            _quadView.setLayoutParams(qparams);
            _view.addView(_quadView);

            _view.addView(_flashView);

        } else {
            // We will have the SDK add its default overlay first.
            super.onCreate(savedInstanceState);

            // Add the label to our existing view.
            RelativeLayout view = (RelativeLayout) getView();

            if (_overlayMode == OverlayMode.Extend) {
                // Add a label to this to display accelerometer information.
                _customLabel = new TextView(this.getActivity());
                _customLabel.setAlpha(0.75f);
                _customLabel.setTextColor(Color.WHITE);
                _customLabel.setBackgroundColor(Color.GRAY);
                _customLabel.setPadding(5, 5, 5, 5);
                _customLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17.0f);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.CENTER_IN_PARENT);
                _customLabel.setLayoutParams(params);
                view.addView(_customLabel);
            }

            _quadView = new QuadView(this.getActivity());
            RelativeLayout.LayoutParams qparams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            _quadView.setLayoutParams(qparams);
            view.addView(_quadView);

            view.addView(_flashView);
        }
    }

    @Override
    public void onShowCaptureMode(int mode) {
        // If we are not creating a new overlay, but extending the
        // existing SDK overlay, then hide our label if we are
        // capturing.
        if (_overlayMode == OverlayMode.Extend ) {
            if (mode == CAPTURE_MODE_IDLE) {
                // Display our label.
                _customLabel.setVisibility(View.VISIBLE);
            } else {
                // Hide our label.
                _customLabel.setVisibility(View.GONE);
            }
        }

        // Call base class so that it can adjust UI for the SDK's overlay.
        super.onShowCaptureMode(mode);
    }

    /**
     * This function is called whenever there is an SDK sensor event. The default implementation
     * provides the behavior for the SDK overlay only.
     * @param sensor The ID of the SDK sensor. The ID for the SDK sensors are
     * <dl>
     *		<dt>LIGHT</dt><dd>1</dd>
     *		<dt>MOVEMENT</dt><dd>2</dd>
     *		<dt>LEVEL</dt><dd>3</dd>
     *		<dt>FOCUS</dt><dd>4</dd>
     * </dl>
     * @param valid True if the sensor is within successful valid parameters, otherwise false.
     * What is considered a successful valid parameters are those specified when the
     * takePicture(...) function was called.
     * @param event Full sensor event data. This will be null if the hardware does not
     *              provide corresponding sensor data.
     * @param data  Quality corners if a quadrilateral is detected by the quality sensor.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onSensorChange(int sensor, boolean valid, SensorEvent event, Object data) {
        super.onSensorChange(sensor, valid, event, data);

        // Depending on mode, set the label to the motion sensor value.
        if (sensor == CaptureWindow.MOVEMENT && event != null && event.values.length == 3) {
            if (_customLabel != null) {  // This is only created in Extend mode
                String s = String.format("x:%.2f\ny:%.2f\nz:%.2f", event.values[0], event.values[1], event.values[2]);
                _customLabel.setText(s);
            } else if (_view != null && useMotion()) { // This is only created in Replace mode
                    _view.updateBubble(event.values[0], event.values[1]);
            }
        }

        if (sensor != CaptureWindow.QUALITY && !valid)
        {
            _quadView.setVisibility(View.INVISIBLE);
        }

        if (sensor == CaptureWindow.QUALITY)
        {
            Map<String, Object> val = (Map<String, Object>) data ;
            PointF[] quadPoints = (PointF[]) val.get(ContinuousCaptureCallback.CAPTURE_QUALITY_CORNERS);
            if (quadPoints != null) {
                String temp = "";
                for (PointF quadPoint : quadPoints) {
                    temp += String.format("(%.0f, %.0f),", quadPoint.x, quadPoint.y);
                }
                Log.i(TAG, "PointFs - " + temp);

                // Scale image quad points to view quad points
                PointF[] vQuadPoints = new PointF[quadPoints.length];
                for (int i = 0; i < quadPoints.length; i++)
                {
                    vQuadPoints[i] = CaptureImage.imagePointToView(quadPoints[i]);
                }

                temp = "";
                for (int i = 0; i < quadPoints.length; i++)
                {
                    temp += String.format("(%.0f, %.0f),", vQuadPoints[i].x, vQuadPoints[i].y);
                }
                Log.i(TAG, "vPointFs - " + temp);

                _quadView.setQuadCorners(vQuadPoints, valid);

            }
        }
    }
}
