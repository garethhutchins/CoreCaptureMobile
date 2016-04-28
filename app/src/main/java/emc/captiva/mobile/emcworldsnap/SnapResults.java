package emc.captiva.mobile.emcworldsnap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class SnapResults extends Activity {
    public String _ticket = "";
    private Context context;
    private String fileID;
    private String SnapFileName;
    private String contentType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //First get the Image View of the results
        Bundle bundle = getIntent().getExtras();
        String FileName = bundle.getString("FileName");
        String UIMString = bundle.getString("UIM");


        _ticket = bundle.getString("Ticket");
        fileID = bundle.getString("fileID");
        SnapFileName = bundle.getString("SnapFileName");
        contentType = bundle.getString("contentType");

        setContentView(R.layout.activity_snap_results);
        setPicture(FileName);
        try {
            JSONObject jsonObject = new JSONObject(UIMString);
            PrepareUIM(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Button _pdf = (Button) findViewById(R.id.btnPDF);

        _pdf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                onPDFClick(v);
            }
        });

        Button _finish = (Button) findViewById(R.id.btnFinish);

        _finish.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                onFinishClick(v);
            }
        });

    }
    private void setPicture (String FileName) {
        Log.d("File",Uri.parse(FileName).toString());
        LinearLayout scrollContainer = (LinearLayout)findViewById(R.id.scrollContainer);
        ScrollView scrollView = (ScrollView)scrollContainer.findViewById(R.id.scrollView);
        LinearLayout LinearResults = (LinearLayout)scrollView.findViewById(R.id.LinearResults);
        ImageView imageView = (ImageView)LinearResults.findViewById(R.id.imageView);
        //Set the picture
        File file = new File(FileName);
        Bitmap d = BitmapFactory.decodeFile(file.getAbsolutePath());
        int nh = (int) ( d.getHeight() * (512.0 / d.getWidth()) );
        Bitmap scaled = Bitmap.createScaledBitmap(d, 512, nh, true);
        imageView.setImageBitmap(scaled);

    }

    private void PrepareUIM(JSONObject uimObject) {
        try {
            Log.d("UIM",uimObject.toString());
            String documentType = uimObject.getString("docType");
            LinearLayout Container = (LinearLayout)findViewById(R.id.scrollContainer);
            ScrollView SView = (ScrollView)Container.findViewById(R.id.scrollView);
            LinearLayout LView = (LinearLayout) SView.findViewById(R.id.LinearResults);
            TextView TDocType = new TextView(this);
            TDocType.setText("Document Type");
            LView.addView(TDocType);
            EditText EDocType = new EditText(this);
            EDocType.setText(documentType);
            LView.addView(EDocType);
            JSONArray DocValues = uimObject.getJSONArray("nodeList");
            Integer N = DocValues.length();
            final TextView[] myTextViews = new TextView[N]; // create an empty array;
            final EditText[] myEditText = new EditText[N];

            for(int i = 0; i < DocValues.length(); i++){
                final TextView rowTextView = new TextView(this);
                final EditText rowEditText = new EditText(this);

                String name = DocValues.getJSONObject(i).getString("labelText");
                String Fieldtype = DocValues.getJSONObject(i).getString("indexFieldType");
                rowTextView.setText(name);
                LView.addView(rowTextView);
                myTextViews[i] = rowTextView;
                String value = DocValues.getJSONObject(i).getJSONArray("data").getJSONObject(0).getString("value");
                rowEditText.setText(value);
                Log.d("Field Type",Fieldtype);
                if (Fieldtype.equals("DateTime")) {
                    Log.d("Field Type",Fieldtype);
                    //remove the time characters
                    value = value.replace("T00:00:00.0000000","");
                    Log.d("New Date Value",value);
                    rowEditText.setText(value);
                    //rowEditText.setInputType(InputType.TYPE_DATETIME_VARIATION_DATE);
                    rowTextView.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
                }
                LView.addView(rowEditText);
                myEditText[i] = rowEditText;
                Log.d(name,value);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void onPDFClick(View view) {
        // Launch the preference settings activity.
        Log.d("Button","PDF Pressed");
        DownloadPDF DPDF = new DownloadPDF(this);
        DPDF._ticket = _ticket;
        DPDF.fileID = fileID;
        DPDF.contentType = contentType;
        DPDF.SnapFileName = SnapFileName;

        DPDF.execute();
    }

    public void onFinishClick(View view) {
        // Launch the preference settings activity.
        Log.d("Finish","Finish Clicked");
        Intent FScreen = new Intent(this,FirstScreen.class);
        startActivity(FScreen);
    }
}
