package com.techleadafrica.click2chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hbb20.CountryCodePicker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String BASE_URL = "https://wa.me/";
    private TextInputEditText message_field, phone_field;
    private CountryCodePicker cpp;
    private String country_code;
    private static final int CALL_LOG_PERMISSIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE); //comment this line if you need to show Title.
        setContentView(R.layout.activity_main);
        initComponent();
        sendMessage();
    }


    private void initComponent() {
        phone_field = findViewById(R.id.phone_number);
        message_field = findViewById(R.id.message);
        cpp = findViewById(R.id.ccp);

        cpp.setOnCountryChangeListener(
                new CountryCodePicker.OnCountryChangeListener() {
                    @Override
                    public void onCountrySelected() {
                        country_code = cpp.getSelectedCountryCode();
                    }
                }
        );
        //message_field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    }

    private void sendMessage() {
        ((AppCompatButton) findViewById(R.id.bt_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ((FloatingActionButton) findViewById(R.id.bt_submit)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = message_field.getText().toString();
                if (phone_field.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "Phone field can not be empty", Toast.LENGTH_SHORT).show();
                } else {
                    String phone = country_code + phone_field.getText().toString();
                    String encoded_message = "";

                    //Encoding message that can be transferred in the url
                    try {
                        encoded_message = URLEncoder.encode(message, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Toast.makeText(MainActivity.this, "We're sorry\nMessage format can't be sent", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    String url = BASE_URL + phone + "/?text=" + encoded_message;
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    //i.setData(Uri.parse(url));
                    PackageManager packageManager = getPackageManager();
                    List<ResolveInfo> activities = packageManager.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);

                    //check if WhatsApp is installed
                    if (activities.size() > 0) {
                        startActivity(i);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Uri contactUri = data.getData();
                // We only need the NUMBER column, because there will be only one row in the result
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

                // Perform the query on the contact to get the NUMBER column
                // We don't need a selection or sort order (there's only one result for the given URI)
                // CAUTION: The query() method should be called from a separate thread to avoid blocking
                // your app's UI thread. (For simplicity of the sample, this code doesn't do that.)
                // Consider using <code><a href="/reference/android/content/CursorLoader.html">CursorLoader</a></code> to perform the query.
                Cursor cursor = getContentResolver()
                        .query(contactUri, projection, null, null, null);
                cursor.moveToFirst();

                // Retrieve the phone number from the NUMBER column
                int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(column);
                phone_field.setText(number);
            }
        }
    }

    public void pickPhone(View v) {
        if(!checkForPermissions()){
            return;
        }
        String[] callLogFields = {CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE};
        String ORDER = CallLog.Calls.DATE + " DESC";
        String WHERE = CallLog.Calls._ID + " IN (SELECT " + CallLog.Calls._ID + " FROM calls GROUP BY " + CallLog.Calls.NUMBER + ")";



        @SuppressLint("MissingPermission") Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, callLogFields, WHERE, null, ORDER);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);



        if(cursor == null || !cursor.moveToFirst()) return;
        final List<Map<String, String>> data = new ArrayList<>();
        do
        {
            long time = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            Date resultdate = new Date(time);
            String date = sdf.format(resultdate);
            String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
            String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));

            Map<String, String> map = new HashMap<>(4);
            map.put("number", number);
            map.put("name", name);
            map.put("visible_name", name == null ? number : name);
            map.put("date", date);
            data.add(map);
        } while (cursor.moveToNext());
        if(!cursor.isClosed()) cursor.close();

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int item) {
                String number = data.get(item).get("number");
                phone_field.setText(number);
            }
        };

        SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_2,
                new String[] {"visible_name", "date"},
                new int[] {android.R.id.text1,
                        android.R.id.text2,
                });
        dialogBuilder.setAdapter(adapter, listener);
        dialogBuilder.setTitle("Choose from Call Log");
        dialogBuilder.create().show();
    }

    private boolean checkForPermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG ) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, CALL_LOG_PERMISSIONS);
        }
        else {
            return true;
        }
        return false;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CALL_LOG_PERMISSIONS){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                return;
            }
            else {
                new AlertDialog.Builder(this)
                        .setTitle("Call log permissions denied")
                        .setMessage("You will only be able to access call logs after granting permissions")
                        .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkForPermissions();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            }
        }
    }


}
