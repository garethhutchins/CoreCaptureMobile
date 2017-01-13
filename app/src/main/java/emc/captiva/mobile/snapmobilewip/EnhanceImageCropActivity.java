/** -------------------------------------------------------------------------
 * Copyright 2013-2016 EMC Corporation.  All rights reserved.
 ---------------------------------------------------------------------------- */

package emc.captiva.mobile.snapmobilewip;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;

import emc.captiva.mobile.sdk.CaptureImage;

/**
 * This activity provides the ability to crop the image.
 */
public class EnhanceImageCropActivity extends Activity {	
	private final String _saveSavedSelectionKey = "SavedSelection";
			
	private ImageView _imageView = null;
	private CropView _cropView = null;
	private boolean _displayed = false;
	private float _widthScaleFactor = 1;
	private float _heightScaleFactor = 1;
	private Rect _savedSelectionRect = null;
	
	/**
	 * This is the crop submit button handler. It will apply the crop and finish the activity.
	 * @param view    The view for the control event.
	 */
	public void onCropSubmit(View view) {
		applyCrop();
		completeAndReturn(RESULT_OK);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		completeAndReturn(RESULT_CANCELED);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		// We handle the display and sizing here, because then we have a real view to work with.
		// If we haven't displayed the crop view, then display it.
		if (hasFocus && !_displayed) {
			_displayed = setImageAndCropView();		
		}
	}

	/*
	 * (non-Javadoc) 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(emc.captiva.mobile.snapmobilewip.R.layout.activity_enhance_image_crop);
		
		// Populate members.
		_imageView = (ImageView) findViewById(emc.captiva.mobile.snapmobilewip.R.id.ImageView);
		_cropView = (CropView) findViewById(emc.captiva.mobile.snapmobilewip.R.id.CropView);
		
		if (savedInstanceState != null) {
			// Restore selection rectangle coordinates
			int [] rectInts = savedInstanceState.getIntArray(_saveSavedSelectionKey);
			
			_savedSelectionRect = new Rect(rectInts[0], rectInts[1], rectInts[2], rectInts[3]);			
		}
	}

	/*
	 * (non-Javadoc) 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
    @SuppressWarnings("NullableProblems")
	@Override
	protected void onSaveInstanceState(Bundle state) {
		// Serialize the selection rectangle to be displayed after the state change.
		Rect selection = getSelection();
		
		state.putIntArray(_saveSavedSelectionKey, new int[] { selection.left, selection.top, selection.right, selection.bottom });
	}
	
	/**
	 * Finish the activity based on the result code.
	 * @param resultCode    The result code to pass back.
	 */
	private void completeAndReturn(int resultCode) {
	    // Pass back our result and finish.
	    Intent retData = new Intent();
        setResult(resultCode, retData);            
        finish();
	}
	
	/**
	 * Adjusts the an image rectangle for an image that is centered.
	 * @param rect   The image rectangle to adjust.
	 * @param add    If true will move the rectangle to the new centered position.
	 * If false it will move from a centered position back to an non-centered position.
	 * @return       The newly adjusted rectangle.
	 */
	private Rect adjustForCenter(Rect rect, boolean add) {
	    Rect img = _imageView.getDrawable().getBounds();
	    int xoffset = (_imageView.getWidth() - img.right) / 2;
        int yoffset = (_imageView.getHeight() - img.bottom) / 2;        
        if (add) {
            return new Rect(
                    Math.min(_imageView.getWidth(), rect.left + xoffset),
                    Math.min(_imageView.getHeight(), rect.top + yoffset),
                    Math.min(_imageView.getWidth(), rect.right + xoffset),
                    Math.min(_imageView.getHeight(), rect.bottom + yoffset));
        } else {
            return new Rect(
                    Math.max(0, rect.left - xoffset),
                    Math.max(0, rect.top - yoffset),
                    Math.max(0, rect.right - xoffset),
                    Math.max(0, rect.bottom - yoffset));
        }
	}
	
	/**
	 * This function handles displaying the image and the crop view.
	 * @return   Returns true if the image and crop view was displayed, otherwise false.
	 */
	private boolean setImageAndCropView() {
	    
	    // Set the image and crop view. First obtain a rect size for a scaled image 
        // that will fit this view. Again, you do not have to scale the image if you expect
	    // your devices to have sufficient memory to handle your images. We will be demonstrating
	    // scaling the image here. Consequently, we have to store the scale factor for the width
	    // and height to use when converting back and forth between the image in the SDK and the 
	    // image being used by the screen for cropping.
        Rect rect = CoreHelper.calcImageSizePx(_imageView, 0);
        if (rect.width() > 0 && rect.height() > 0) {
            
            // Store our scale factors.
            Map<String, Object> properties = CaptureImage.getImageProperties();
            float imageWidth = (Integer)properties.get(CaptureImage.IMAGE_PROPERTY_WIDTH);
            float imageHeight = (Integer)properties.get(CaptureImage.IMAGE_PROPERTY_HEIGHT);
            _widthScaleFactor = ((float)rect.width()) / imageWidth;
            _heightScaleFactor = ((float)rect.height()) / imageHeight;            
            
            // Get a scaled image for this size and assign it to our image view.
            Bitmap bitmap = CaptureImage.getImageForDisplay(rect.width(), rect.height(), null);
            _imageView.setImageBitmap(bitmap);
            
            // Set an adjusted rect for centering the bitmap into the view that crop will use.
            Rect adj = adjustForCenter(rect, true);
            _cropView.setBitmapRect(adj);
            
            Rect cropRect;
            
            if (_savedSelectionRect != null) {
            	// Restore previous selection
            	cropRect = _savedSelectionRect;
            	_savedSelectionRect = null;
            } else {
                // Obtain an auto calculated crop rectangle to start with using no threshold
                // or padding and set that as our selection. 
            	cropRect = CaptureImage.getAutoCropRect(0, 0);
            }
            
            if (cropRect != null && !cropRect.isEmpty()) {
                // It gave us a crop rectangle. Change the crop rectangle given by the SDK to the scaled image we have
                // by applying our scale factor.
                Rect rectDips = new Rect(
                    (int)((float)cropRect.left * _widthScaleFactor), 
                    (int)((float)cropRect.top * _heightScaleFactor), 
                    (int)((float)cropRect.right * _widthScaleFactor), 
                    (int)((float)cropRect.bottom * _heightScaleFactor));
                adj = adjustForCenter(rectDips, true);
                _cropView.setSelection(adj);
                _cropView.bringToFront();
            } else {
                // We didn't get a crop rectangle, so just provide one (minus 50 pixels).
                _cropView.setSelection(new Rect(0, 0, Math.max(1, rect.width() - 50), Math.max(1, rect.height() - 50))); 
                _cropView.bringToFront();
            }
            
            // We have displayed the view so store this so we don't do this again.
            return true;
        }
		
		return false;
	}	
	
	/**
	 * Get the selection in image coordinates.
	 * @return Rect representing the selection.
	 */
	private Rect getSelection() {
	    // Here we get the selection that was made and if it is valid and not empty we
	    // convert it back to the SDK sizing by dividing our scale factor.
		Rect rectDips = _cropView.getSelection();
		
	    // Adjust for being centered.
	    Rect adj = adjustForCenter(rectDips, false);
	    
	    // Apply scale factor to convert back to SDK sizing.
	    return new Rect(
                (int)((float)adj.left / _widthScaleFactor),
                (int)((float)adj.top / _heightScaleFactor),
                (int)((float)adj.right / _widthScaleFactor),
                (int)((float)adj.bottom / _heightScaleFactor));
	}

	/**
	 * Here we apply the crop selection.
	 */
	private void applyCrop() {	    		    
	    // Build our parameters for the SDK crop operation.
	    HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(CaptureImage.FILTER_PARAM_CROP_RECTANGLE, getSelection());
        // Apply the crop.
        CaptureImage.applyFilters(new String[] { CaptureImage.FILTER_CROP }, parameters);
	}
}
