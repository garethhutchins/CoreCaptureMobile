package otmobile;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static android.R.style.Theme_DeviceDefault_Light_Dialog;


/**
 * Created by hutchg on 08/04/2016.
 */
class PostToCoreCapture extends AsyncTask {
    private Context context;
    private ProgressDialog dialog;
    private String _ticket;
    public String FileName;
    private WeakReference<CoreCaptureResults> SnapResultsWeakReference;
    private String OriginalFileID;
    private String OriginalContentType;
    public String OriginalImage;

    private class loginRequest {
        public String username;
        public String password;
        public String subscriptionName;
        public String grant_type = "client_credentials";
    }
    private class loginResponse {
        returnStatus returnStatus;
        String access_token;
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
    private class ClassifyExtractPageRequest {
        public serviceProps serviceProps[] = new serviceProps[2];
        public requestItems requestItems[] = new requestItems[1];
    }
    private class PostFile {
        public String data;
        public String contentType;
    }
    private class PostFileResponse {
        public returnStatus returnStatus;
        public String id;
        public String contentType;
        public URI src;
        public String updated;
    }
    private RequestQueue queue;
    String filename;


    public PostToCoreCapture(Context context) {

        dialog = new ProgressDialog(context, Theme_DeviceDefault_Light_Dialog);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //dialog.setIndeterminate(true);
        //dialog.setIndeterminateDrawable(drawable);

        this.context = context;
        //Start the Request Queue
        HttpsTrustManager.allowAllSSL();
        queue = Volley.newRequestQueue(context);
        queue.start();
    }


    private void login() {
        String url;

        dialog.setMessage("Logging in to Core Capture");

        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(context);
        final loginRequest Login = new loginRequest();
        Login.username = gprefs.getString("Core Capture User","");
        Login.password = gprefs.getString("Core Capture Password","");
        Login.subscriptionName = gprefs.getString("Core Capture Subscription","");
        final String client = gprefs.getString("Core Capture Client","");
        final String secret = gprefs.getString("Core Capture Secret","");
        //Need to add a new setting to swtich data centers
        String Datacentre = gprefs.getString("Core Capture Data Centre","");
        if (Datacentre.equals("US")) {
            url = "https://otdsauth.ot2.opentext.com/oauth2/token";
        }
        else {
            url = "https://otdsauth.ot2.opentext.eu/oauth2/token";
        }

        Gson gson = new Gson();
        String Strjson = gson.toJson(Login);
        JSONObject JLogin = null;
        try {
            JLogin = new JSONObject(Strjson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        StringRequest  req = new StringRequest(Request.Method.POST, url,
                    new Listener<String>()
                    {
                        @Override
                        public void onResponse(String response) {
                            String StrResponse = "";
                            StrResponse = response.toString();
                            Gson gsonResponse = new Gson();
                            loginResponse LoginResponse = gsonResponse.fromJson(StrResponse, loginResponse.class);
                            _ticket = LoginResponse.access_token;
                            //Now call the Image Upload
                            UploadFile();
                        }

                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("Login Error",error.toString());
                    NetworkResponse response = error.networkResponse;

                    if(response != null && response.data != null){
                        String json = null;
                        json = new String(response.data);
                        JSONObject obj = null;
                        try {
                            obj = new JSONObject(json);
                            json = obj.getString("message");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(json != null) {
                            Log.d("Login Error",json);
                            ShowDialog("Login Error",json);
                            //Go back to Image Enhancement
                    }
                }

            }
           }

        ){
            @Override
            public String getBodyContentType()
            {
                return "application/x-www-form-urlencoded";
            }
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("grant_type", "password");
                params.put("username", Login.username);
                params.put("password", Login.password);
                params.put("subscriptionName",Login.subscriptionName);
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                String creds = String.format("%s:%s",client,secret);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                headers.put("Authorization", auth);
                return headers;
            }
        }
                ;

        queue.add(req);

    }
    private void UploadFile() {
        //This time we'll upload the file first so we can export the colour image
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(context);
        dialog.setMessage("Uploading Image");
        PostFile PF = new PostFile();
        //Convert the file to Base64
        InputStream inputStream;
        String encodedString = "";
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
            encodedString = Base64.encodeToString(bytes, Base64.DEFAULT);

        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        PF.contentType = mime;
        PF.data = encodedString;
        String url = "";
        String Datacentre = gprefs.getString("Core Capture Data Centre","");
        if (Datacentre.equals("US")) {
            url = "https://capture.ot2.opentext.com";
        }
        else {
            url = "https://capture.ot2.opentext.eu";
        }
        //Now make the post

        url = url + "/cp-rest/session/files";

        Gson gson = new Gson();
        String json = gson.toJson(PF);
        Log.d("JSONString",json);
        JSONObject JImage = null;
        try {
            JImage = new JSONObject(json);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest Imgreq = new JsonObjectRequest(url, JImage,
                new Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // handle response
                        Log.d("Upload OK",response.toString());
                        //Get the response back
                        try {
                            //Now pass the fileID to EnhanceImage
                            String FileID = response.getString("id");
                            OriginalFileID = FileID;
                            String contentType = response.getString("contentType");
                            OriginalContentType = contentType;
                            ProcessImage(FileID, contentType);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            //Look for error if the configuration isn't found
                            Log.d("Image Processing Error",response.toString());
                            try {
                                JSONObject JSONError = response.getJSONArray("resultItems").getJSONObject(0);
                                String ErrorMSG = JSONError.getString("errorMessage");
                                ShowDialog("Image Processing Error",ErrorMSG);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }

                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Login Error",error.toString());
                NetworkResponse response = error.networkResponse;
                if(response != null && response.data != null){
                    String json = null;
                    json = new String(response.data);
                    JSONObject obj = null;
                    try {
                        obj = new JSONObject(json);
                        json = obj.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(json != null) {
                        Log.d("Post File Error",json);
                        ShowDialog("Post File Error",json);
                        //Go back to Image Enhancement
                    }
                } }
        })

        {
            @Override
            public String getBodyContentType()
            {
                return "application/hal+json; charset=utf-8";
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization","Bearer " + _ticket);
                headers.put("Content-Type", "application/hal+json; charset=utf-8");
                return headers;
            }
        };
        int socketTimeout = 30000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        Imgreq.setRetryPolicy(policy);
        queue.add(Imgreq);

    }

    private void ProcessImage(String Fileid, String contentType)     {
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(context);
        ProcessImageRequest processImageRequest = new ProcessImageRequest();
        dialog.setMessage("Enhancing Image");
        //Set the Profile
        serviceProps Profile = new serviceProps();
        Profile.name = "Profile";
        Profile.value = gprefs.getString("Capture Profile","");
        processImageRequest.serviceProps[0] = Profile;

        //Set the Environment
        serviceProps Env = new serviceProps();
        Env.name = "Env";
        Env.value = gprefs.getString("Core Capture Environment","");;
        processImageRequest.serviceProps[1] = Env;

        //Set the request Item
        requestItems requestItem = new requestItems();
        requestItem.nodeID = 1;
        files uploadfile = new files();

        uploadfile.value = Fileid;
        uploadfile.name = filename;
        uploadfile.contentType = contentType;

        requestItem.files[0] = uploadfile;
        processImageRequest.requestItems[0] = requestItem;

        //Now do the posting

        String url = "";
        String Datacentre = gprefs.getString("Core Capture Data Centre","");
        if (Datacentre.equals("US")) {
            url = "https://capture.ot2.opentext.eu";
        }
        else {
            url = "https://capture.ot2.opentext.eu";
        }
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

        JsonObjectRequest Imgreq = new JsonObjectRequest(url, JImage,
                new Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // handle response
                        Log.d("Upload OK",response.toString());
                        //Get the response back
                        try {
                            JSONArray mainjsonArray= response.getJSONArray("resultItems").getJSONObject(0).getJSONArray("files");
                            String fileID = mainjsonArray.getJSONObject(0).getString("value");
                            String name = mainjsonArray.getJSONObject(0).getString("name");
                            String contentType = mainjsonArray.getJSONObject(0).getString("contentType");
                            Log.d("File ID",fileID);
                            //Now pass the fileID to ClassifyExtractPage
                            ClassifyExtract(name,fileID,contentType);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            //Look for error if the configuration isn't found
                            Log.d("Image Processing Error",response.toString());
                            try {
                                JSONObject JSONError = response.getJSONArray("resultItems").getJSONObject(0);
                                String ErrorMSG = JSONError.getString("errorMessage");
                                ShowDialog("Image Processing Error",ErrorMSG);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }

                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Login Error",error.toString());
                NetworkResponse response = error.networkResponse;
                if(response != null && response.data != null){
                    String json = null;
                    json = new String(response.data);
                    JSONObject obj = null;
                    try {
                        obj = new JSONObject(json);
                        json = obj.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(json != null) {
                        Log.d("Process Image Error",json);
                        ShowDialog("Process Image Error",json);
                        //Go back to Image Enhancement

                    }
                } }
        })

        {
            @Override
            public String getBodyContentType()
            {
                return "application/hal+json; charset=utf-8; charset=utf-8";
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization","Bearer " + _ticket);
                headers.put("Content-Type", "application/hal+json; charset=utf-8; charset=utf-8");
                return headers;
            }
        };
        int socketTimeout = 30000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        Imgreq.setRetryPolicy(policy);
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

    private void ClassifyExtract(final String name, final String fileID, final String contentType) {
        //Now call the Classify extract service
        //Change the dialog
        dialog.setMessage("Classifying & Extracting");
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(context);
        ClassifyExtractPageRequest ceRequest = new ClassifyExtractPageRequest();
        serviceProps Project = new serviceProps();
        //project
        Project.name = "Project";
        // not used anymore Project.value = gprefs.getString("Recognition Project","");
        Project.value = "Default";
        //env
        serviceProps Env = new serviceProps();
        Env.name = "Env";
        Env.value = gprefs.getString("Core Capture Environment","");;;
        ceRequest.serviceProps[0] = Project;
        ceRequest.serviceProps[1] = Env;
        requestItems requestItem = new requestItems();
        requestItem.nodeID = 1;
        files uploadfile = new files();

        uploadfile.value = fileID;
        uploadfile.name = name;
        uploadfile.contentType = contentType;

        requestItem.files[0] = uploadfile;
        ceRequest.requestItems[0] = requestItem;

        //Now do the posting
        String url = "";
        String Datacentre = gprefs.getString("Core Capture Data Centre","");
        if (Datacentre.equals("US")) {
            url = "https://capture.ot2.opentext.com";
        }
        else {
            url = "https://capture.ot2.opentext.eu";
        }
        url = url + "/cp-rest/session/services/classifyextractpage";

        Gson gson = new Gson();
        String json = gson.toJson(ceRequest);
        Log.d("JSONString",json);
        JSONObject JImage = null;
        try {
            JImage = new JSONObject(json);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest Classreq = new JsonObjectRequest(url, JImage,
                new Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // handle response
                        Log.d("Classify OK",response.toString());
                        Intent ViewSnapRestuls = new Intent(context,CoreCaptureResults.class);
                        ViewSnapRestuls.putExtra("UIM","");
                        try {
                            //First get the JSON Object for the UIM Data
                            JSONArray values = response.getJSONArray("resultItems").getJSONObject(0).getJSONArray("values");
                            for (int i =0; i < values.length();i++) {
                                JSONObject name = values.getJSONObject(i);
                                if (name.getString("name").equals("UimData")) {
                                    JSONObject uimObject =name.getJSONObject("value");
                                    Log.d("UIM Object",uimObject.toString());
                                    ViewSnapRestuls.putExtra("UIM",uimObject.toString());
                                }
                            }
                            ViewSnapRestuls.putExtra("FileName",FileName);
                            ViewSnapRestuls.putExtra("Ticket",_ticket);
                            ViewSnapRestuls.putExtra("fileID",OriginalFileID);
                            ViewSnapRestuls.putExtra("SnapFileName",name);
                            ViewSnapRestuls.putExtra("contentType",OriginalContentType);
                            ViewSnapRestuls.putExtra("fnURI",OriginalImage);
                            context.startActivity(ViewSnapRestuls);
                            Activity activity = (Activity) context;
                            activity.finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            //If it fails look for the error message
                            try {
                                String ErrorMessage= response.getJSONArray("resultItems").getJSONObject(0).getString("errorMessage");
                                ShowDialog("Error Reading File",ErrorMessage);
                                //G0 back to the main menu
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if(response != null && response.data != null){
                    String json = null;
                    json = new String(response.data);
                    JSONObject obj = null;
                    try {
                        obj = new JSONObject(json);
                        json = obj.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(json != null) {
                        Log.d("Classify Extract Error",json);
                        ShowDialog("Classify Extract Error",json);
                        //Go back to Image Enhancement

                    }
                } }
        })

        {
            @Override
            public String getBodyContentType()
            {
                return "application/hal+json; charset=utf-8; charset=utf-8";
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization","Bearer " + _ticket);
                headers.put("Content-Type", "application/hal+json; charset=utf-8; charset=utf-8");
                return headers;
            }
        };
        int socketTimeout = 30000;//30 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        Classreq.setRetryPolicy(policy);
        queue.add(Classreq);
    }


    @Override
    protected void onPostExecute(Object result) {
        //if (dialog.isShowing()) {
        //    dialog.dismiss();
       // }


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

    private void ShowDialog (String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(
                context).create();

        // Setting Dialog Title
        alertDialog.setTitle(title);

        // Setting Dialog Message
        alertDialog.setMessage(message);


        // Setting OK Button
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Write your code here to execute after dialog closed
                Intent intent = new Intent(context, FirstScreen.class);
                context.startActivity(intent);
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }
}
