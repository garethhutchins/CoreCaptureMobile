/** -------------------------------------------------------------------------
 * Copyright 2013-2016 EMC Corporation.  All rights reserved.
 ---------------------------------------------------------------------------- */

package emc.captiva.mobile.emcworldsnap;

import java.util.Map;
import java.util.HashMap;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import emc.captiva.mobile.emcworldsnap.R;
import emc.captiva.mobile.sdk.CaptureException;
import emc.captiva.mobile.sdk.CaptureImage;

/**
 * This activity displays the device and image properties.
 */
public class ImageInfoActivity extends Activity {	
	private TextView _deviceIDLabel = null;
	private TextView _widthLabel = null;
	private TextView _heightLabel = null;
	private TextView _channelsLabel = null;
	private TextView _bitsPerPixLabel = null;
	private TextView _versionLabel = null;
	
	/**
	 * This is the back button handler.
	 * @param view    The view for the control event.
	 */
	public void onBackButton(View view) {
	    // Finish the activity and return to the previous.
        finish();
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_imageinfo);
		
		// Retrieve the controls.
		_deviceIDLabel = (TextView) findViewById(R.id.DeviceIDLabel);

		// Get the image properties.
		Map<String, Object> properties = CaptureImage.getImageProperties();
		int imageWidth = (Integer)properties.get(CaptureImage.IMAGE_PROPERTY_WIDTH);
		int imageHeight = (Integer)properties.get(CaptureImage.IMAGE_PROPERTY_HEIGHT);
		int channels = (Integer)properties.get(CaptureImage.IMAGE_PROPERTY_CHANNELS);
		int bitsPerPix = (Integer)properties.get(CaptureImage.IMAGE_PROPERTY_BITSPERPIXEL);

        int glare = CaptureImage.measureQuality(new HashMap<String, Object>() { { put(CaptureImage.MEASURE_GLARE, 1); }});
        int quadrilateral = CaptureImage.measureQuality(new HashMap<String, Object>() { { put(CaptureImage.MEASURE_QUADRILATERAL, 1); }});
        int perspective = CaptureImage.measureQuality(new HashMap<String, Object>() {
            {
                put(CaptureImage.MEASURE_PERSPECTIVE, 1);
            }
        });
        Point[] quadCropPoints = CaptureImage.getQuadrilateralCropCorners();

        HashMap allAssessments = new HashMap<String, Object>() {
            {
                put(CaptureImage.MEASURE_GLARE, 1);
                put(CaptureImage.MEASURE_QUADRILATERAL, 1);
                put(CaptureImage.MEASURE_PERSPECTIVE, 1);
            }
        };

        int totalQuality = CaptureImage.measureQuality(allAssessments);

        CaptureException error = (CaptureException) getIntent().getSerializableExtra("FilterError");
        String lastErrorMsg = error != null ? String.format("Error code %d: ", error.getCode()) + error.getLocalizedMessage() : "No error";

		// Assign the properties to the controls.
		_deviceIDLabel.setText( getString(R.string.ImageInfo_DeviceID) + " " + CaptureImage.getDeviceId() + "\n\n" +

		                        getString(R.string.ImageInfo_Width) +  " " + Integer.toString(imageWidth) + "\n" +
		                        getString(R.string.ImageInfo_Height) +  " " + Integer.toString(imageHeight)  + "\n" +
		                        getString(R.string.ImageInfo_Channels) +  " " + Integer.toString(channels)  + "\n" +
		                        getString(R.string.ImageInfo_BitsPerPixel) +  " " + Integer.toString(bitsPerPix)  + "\n\n" +

		                        getString(R.string.ImageInfo_Version) + " " + CaptureImage.getVersion()  + "\n\n" +

                                getString(R.string.ImageInfo_Glare) + " " + glare  + "\n" +
                                getString(R.string.ImageInfo_Quadrilateral) + " " + quadrilateral + "\n" +
                                getString(R.string.ImageInfo_Perspective) + " " + perspective  + "\n" +
                                getString(R.string.ImageInfo_TotalQuality) + " " + totalQuality + "\n" +

                                "Quad Points: " +
                                (quadCropPoints.length > 0 ? "\n" +
                                    "  top left: " + Integer.toString(quadCropPoints[0].x) + ", " + Integer.toString(quadCropPoints[0].y) + "\n" +
                                     "  top right: " + Integer.toString(quadCropPoints[1].x) + ", " + Integer.toString(quadCropPoints[1].y) +"\n" +
                                     "  bottom right: " + Integer.toString(quadCropPoints[2].x) + ", " + Integer.toString(quadCropPoints[2].y) +"\n" +
                                     "  bottom left: " + Integer.toString(quadCropPoints[3].x) + ", " + Integer.toString(quadCropPoints[3].y) :
                                    "No valid quad points detected") +"\n" +
                                getString(R.string.ImageInfo_LastError) + " " + lastErrorMsg
        );
	}
}
