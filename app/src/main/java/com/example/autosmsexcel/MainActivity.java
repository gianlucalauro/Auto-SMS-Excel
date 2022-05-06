package com.example.autosmsexcel;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.MessageFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    List<String[]> csvData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyPermissions();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            verifyStoragePermission();

        Button sendSMSButton = findViewById(R.id.sendSMS);
        Button openFileButton = findViewById(R.id.openFile);
        EditText editText = findViewById(R.id.editTextTextMultiLine);

        editText.setText(readText());

        sendSMSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verifyPermissions();
                String text = editText.getText().toString();
                if (!TextUtils.isEmpty(text))
                    try {
                        for (String[] data : csvData) {
                            sendSMS(data[0], generateMessage(text, data[1]));
                        }
                        if (!csvData.isEmpty()) {
                            saveText(text);
                            Toast.makeText(getApplicationContext(), "Messagi mandati",
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Errore:\n".concat(e.getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
            }
        });

        openFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFile();
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void verifyStoragePermission() {
        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", this.getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    public void verifyPermissions() {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.SEND_SMS
        };
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void sendSMS(String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String generateMessage(String text, String code) {
        return MessageFormat.format(text, code);
    }

    public void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        activityResultLauncher.launch(intent);

    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        String path = new File(data.getData().getPath()).getAbsolutePath();
                        String fullPath = Environment.getExternalStorageDirectory() + "/" + path.substring(path.lastIndexOf(":") + 1);

                        MainActivity.this.csvData = getStringFromExtraFile(fullPath);

                    }
                }
            }
    );

    public void saveText(String content) {
        File path = getApplicationContext().getFilesDir();
        try {
            FileOutputStream writer = new FileOutputStream(new File(path, "savedText.txt"));
            writer.write(content.getBytes());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readText() {
        File path = getApplicationContext().getFilesDir();
        File file = new File(path, "savedText.txt");
        int length = (int) file.length();

        byte[] bytes = new byte[length];


        try {
            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String(bytes);
    }

    private List<String[]> getStringFromExtraFile(String fullPath) {
        List<String[]> resultList = new ArrayList<>();

        File file = new File(fullPath);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String returned;
            while ((returned = bufferedReader.readLine()) != null) {
                String[] cols = returned.split(",");
                resultList.add(cols);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextView textView = findViewById(R.id.selectedFile);
        textView.setText("File selezionato: " + fullPath.substring(fullPath.lastIndexOf("/") + 1));

        return resultList;
    }


}