package emc.captiva.mobile.emcworldsnap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.view.View;

import java.util.HashMap;


import emc.captiva.mobile.sdk.CaptureImage;
import emc.captiva.mobile.sdk.CaptureWindow;
import emc.captiva.mobile.sdk.PictureCallback;


public class FirstScreen extends Activity implements PictureCallback {
    static String USE_QUADVIEW = "UseQuadView";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_screen);
        //License the Application
        CoreHelper.license(this);
    }

    public void onTakePicture(View view) {
        // Use a separate HashMap to hold non-TakePicture parameter values from preferences.
        HashMap<String, Object> appParams = new HashMap<>();
        // Obtain our picture parameters from the preferences. Only supported SDK keys should go into parameters.
        HashMap<String, Object> parameters = CoreHelper.getTakePictureParametersFromPrefs(this, appParams);

        // Get the preference for CaptureWindow
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(this);
        String capWndNone = CoreHelper.getStringResource(this, R.string.GPREF_CAPTURE_CUSTOM_OPTIONS_NONE);
        String capWndPref = gprefs.getString(CoreHelper.getStringResource(this, R.string.GPREF_CAPTURE_CUSTOM_OPTIONS), capWndNone);

        if (capWndPref.compareToIgnoreCase(capWndNone) != 0) {
            // Assign a custom CaptureWindow if specified by the prefs.
            CaptureWindow wnd = new CustomWindow(this, capWndPref, appParams);
            parameters.put(CaptureImage.PICTURE_CAPTUREWINDOW, wnd);
        }
        else if ((boolean) appParams.get(USE_QUADVIEW))
        {
            CaptureWindow wnd = new CustomWindow(this, capWndPref, appParams);
            parameters.put(CaptureImage.PICTURE_CAPTUREWINDOW, wnd);
        }

        // Launch the camera to take a picture.
        CaptureImage.takePicture(this, parameters);
    }

    public void onSettings(View view) {
        // Launch the preference settings activity.
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onPictureTaken(byte[] bytes) {

    }

    @Override
    public void onPictureCanceled(int i) {

    }

}
