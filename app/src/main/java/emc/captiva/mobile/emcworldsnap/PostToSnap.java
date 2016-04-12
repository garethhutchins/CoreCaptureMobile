package emc.captiva.mobile.emcworldsnap;


import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;


import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;



import com.android.volley.Response;

import com.android.volley.VolleyError;

import com.android.volley.toolbox.JsonObjectRequest;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by hutchg on 08/04/2016.
 */
public class PostToSnap extends AsyncTask {
    private Context context;
    private ProgressDialog dialog;
    private String _ticket;
    public String FileName;
    private class loginRequest {
        public String culture = "en-US";
        public String licenseKey = context.getResources().getString(R.string.licenseKey);
        public String deviceId = "Captiva Mobile Demo";
        public String applicationId = context.getResources().getString(R.string.applicationId);
        public String username;
        public String password;
    }
    private class loginResponse {
        returnStatus returnStatus;
        String ticket;
    }
    private class returnStatus{
        public Integer status;
        public String code;
        public String message;
        public String server;
    }
    private class ProcessImageRequest {
        public serviceProps serviceProps[] = new serviceProps[2];
        public requestItems requestItems[] = new requestItems[1];
    }
    private class serviceProps {
        public String name;
        public String value;
    }
    private class requestItems {
        public Integer nodeID;
        public files files[] = new files[1];
    }
    private class files {
        public String name;
        public String value;
        public String contentType;
    }
    private class filesResponse {
        String name;
        String value;
        String src;
        String contentType;
    }


    public PostToSnap(Context context) {
        dialog = new ProgressDialog(context);
        this.context = context;
    }


    private void login() {
        String url;
        String User;
        String Password;

        HttpsTrustManager.allowAllSSL();
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(context);
        loginRequest Login = new loginRequest();
        Login.username = gprefs.getString("Snap User","");
        Login.password = gprefs.getString("Snap Password","");
        url = gprefs.getString("Snap URL", "");
        url = url + "/cp-rest/Session";
        Gson gson = new Gson();
        String json = gson.toJson(Login);
        JSONObject JLogin = null;
        try {
            JLogin = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestQueue queue = Volley.newRequestQueue(context);

        queue.start();
        JsonObjectRequest req = new JsonObjectRequest(url, JLogin,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            String StrResponse = "";
                            StrResponse = response.toString();
                            Gson gsonResponse = new Gson();
                            loginResponse LoginResponse = gsonResponse.fromJson(StrResponse, loginResponse.class);
                            _ticket = LoginResponse.ticket;
                            //Now call the Image Upload
                            ProcessImage();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String StrResponse = "";
                    StrResponse = error.toString();
                }
            });

        queue.add(req);

    }

    private void ProcessImage() {
        ProcessImageRequest processImageRequest = new ProcessImageRequest();

        //Set the Profile
        serviceProps Profile = new serviceProps();
        Profile.name = "Profile";
        Profile.value = "PID";
        processImageRequest.serviceProps[0] = Profile;

        //Set the Environment
        serviceProps Env = new serviceProps();
        Env.name = "Env";
        Env.value = "D";
        processImageRequest.serviceProps[1] = Env;

        //Set the request Item
        requestItems requestItem = new requestItems();
        requestItem.nodeID = 1;
        files uploadfile = new files();

        InputStream inputStream;
        String encodedString = "";
        String filename = "";
        String mime = "";


        try {
            inputStream = new FileInputStream(FileName);
            byte[] bytes;
            File file = new File(FileName);
            filename = file.getName();
            Uri uri = Uri.fromFile(file);

            mime = getMimeType(uri);
            byte[] buffer = new byte[(int) file.length()];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            bytes = output.toByteArray();
            //String str = new String(bytes, "UTF-8");

            encodedString = Base64.encodeToString(bytes, Base64.DEFAULT);
            //encodedString = str;

        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        uploadfile.value = encodedString;
        uploadfile.name = filename;
        uploadfile.contentType = mime;

        requestItem.files[0] = uploadfile;
        processImageRequest.requestItems[0] = requestItem;

        //Now do the posting
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(context);
        String url = gprefs.getString("Snap URL", "");
        url = url + "/cp-rest/session/services/processimage";

        Gson gson = new Gson();
        String json = gson.toJson(processImageRequest);
        Log.d("JSONString",json);
        JSONObject JImage = null;
        try {
            JImage = new JSONObject(json);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        HttpsTrustManager.allowAllSSL();

        RequestQueue queue = Volley.newRequestQueue(context);

        queue.start();

        JsonObjectRequest Imgreq = new JsonObjectRequest(url, JImage,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // handle response
                        Log.d("Upload OK",response.toString());
                        //Get the response back
                        try {
                            JSONArray mainjsonArray= response.getJSONArray("resultItems").getJSONObject(0).getJSONArray("files");
                            String fileID = mainjsonArray.getJSONObject(0).getString("value");
                            Log.d("File ID",fileID);
                            //Now pass the fileID to ClassifyExtractPage
                            ClassifyExtract(fileID);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // handle error
                Log.d("Upload Error",error.toString());
                Integer i = error.networkResponse.statusCode;
                Log.d("Upload Error Message",i.toString());

            }
        })

        {
            @Override
            public String getBodyContentType()
            {
                return "application/vnd.emc.captiva+json; charset=utf-8";
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("CPTV-TICKET", _ticket);
                headers.put("Content-Type", "application/vnd.emc.captiva+json; charset=utf-8");
                return headers;
            }
        };
        queue.add(Imgreq);
    }

    @Override
    protected void onPreExecute() {
        dialog.setMessage("Posting to Snap");
        dialog.show();
        super.onPreExecute();
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        login();

        return null;
    }

    private void ClassifyExtract(String FileID) {
        //Now call the Classify extract service

    }

    @Override
    protected void onPostExecute(Object result) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }

    }

    public String getMimeType(Uri uri) {
        String mimeType = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getApplicationContext().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }
}
