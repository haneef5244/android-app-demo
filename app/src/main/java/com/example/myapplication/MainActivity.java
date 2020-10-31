package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.obj.ModuleReq;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
  private static final int PERMISSION_REQUEST_CODE = 1;
  private static final int REQUEST_GALLERY = 200;
  private String moduleName;
  private String majorType;
  private String mainType;
  private String filePath;
  private String email;
  private LinearLayout parentLayout;
  private int check = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    parentLayout = (LinearLayout) findViewById(R.id.parent_layout);
    Spinner moduleNameSpinner = findViewById(R.id.moduleNameSpinner);
    ArrayAdapter<CharSequence> moduleNameAdapter = ArrayAdapter.createFromResource(this, R.array.modules, android.R.layout.simple_spinner_item);
    moduleNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    moduleNameSpinner.setAdapter(moduleNameAdapter);
    moduleNameSpinner.setOnItemSelectedListener(this);

    Spinner majorTypeSpinner = findViewById(R.id.majorTypeSpinner);
    ArrayAdapter<CharSequence> majorTypeAdapter = ArrayAdapter.createFromResource(this, R.array.majors, android.R.layout.simple_spinner_item);
    majorTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    majorTypeSpinner.setAdapter(majorTypeAdapter);
    majorTypeSpinner.setOnItemSelectedListener(this);

    Spinner mainTypeSpinner = findViewById(R.id.mainTypeSpinner);
    ArrayAdapter<CharSequence> mainTypeAdapter = ArrayAdapter.createFromResource(this, R.array.mains, android.R.layout.simple_spinner_item);
    mainTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mainTypeSpinner.setAdapter(mainTypeAdapter);
    mainTypeSpinner.setOnItemSelectedListener(this);

    Button uploadButton = findViewById(R.id.uploadButton);
    uploadButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (Build.VERSION.SDK_INT >= 23) {
          if (checkPermission()) {
            filePicker();
          } else {
            requestPermission();
          }
        } else {
          filePicker();
        }
      }
    });

    Button uploadToServerButton = findViewById(R.id.uploadToServerButton);
    uploadToServerButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        sendRequestToServer();
      }
    });
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    switch (parent.getId()) {
      case R.id.moduleNameSpinner:
        moduleName = parent.getItemAtPosition(position).toString();
        System.out.println("moduleNameSpinner selected is");

      case R.id.majorTypeSpinner:
        majorType = parent.getItemAtPosition(position).toString();
        System.out.println("majorTypeSpinner selected is");
      case R.id.mainTypeSpinner:
        mainType = parent.getItemAtPosition(position).toString();
        System.out.println("mainTypeSpinner selected is");
      default:
        System.out.println("none");
    }
    if (check++ > 1) {
      System.out.println("Child count = " + parentLayout.getChildCount());
      LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      final View rowView = inflater.inflate(R.layout.text_view_layout, null);
      final TextView name = (TextView) rowView.findViewById(R.id.text_view);
      name.setText(String.format("Text View %d", parentLayout.getChildCount() + 1));
      parentLayout.addView(rowView, parentLayout.getChildCount());
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }

  private void requestPermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
      Toast.makeText(MainActivity.this, "Please provide permission to upload file", Toast.LENGTH_SHORT).show();
    } else {
      ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }
  }

  private boolean checkPermission() {
    int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
    if (result == PackageManager.PERMISSION_GRANTED) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case PERMISSION_REQUEST_CODE:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(MainActivity.this, "Permission Successful", Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(MainActivity.this, "Permission Failed", Toast.LENGTH_SHORT).show();
        }
    }
  }

  private void filePicker() {
    Toast.makeText(MainActivity.this, "File picker call", Toast.LENGTH_SHORT).show();
    Intent openGallery = new Intent(Intent.ACTION_PICK);
    openGallery.setType("image/*");
    startActivityForResult(openGallery, REQUEST_GALLERY);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
      String filePath = getRealPathFromUri(data.getData(), MainActivity.this);
      Log.d("File Path : ", " " + filePath);
      this.filePath = filePath;
      File file = new File(filePath);
    }
  }

  public String getRealPathFromUri(Uri uri, Activity activity) {
    Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
    if (cursor == null) {
      return uri.getPath();
    } else {
      cursor.moveToFirst();
      int id = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
      return cursor.getString(id);
    }
  }

  public void sendRequestToServer() {
    EditText text = findViewById(R.id.editTextTextEmailAddress);
    email = text.getText().toString();
    if (!moduleName.isEmpty() && !majorType.isEmpty() && !mainType.isEmpty() && !filePath.isEmpty() && !email.isEmpty()) {

      ModuleReq req = new ModuleReq();
      req.setEmail(email);
      req.setMainType(mainType);
      req.setMajorType(majorType);
      req.setModuleName(moduleName);
      String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
      String ext = FilenameUtils.getExtension(filePath);
      File file = new File(filePath);
      Integer fileSizeInMB = Integer.parseInt(String.valueOf(file.length() / 1024)) / 1000;
      if (fileSizeInMB > 10) {
        Toast.makeText(MainActivity.this, "File size exceeds 10MB limit.", Toast.LENGTH_SHORT).show();
        filePath = "";
        return;
      }

      //to upload in-memory bytes use ByteArrayResource instead
      Resource r = new FileSystemResource(file);
      try {
        MultipartFile multipartFile = new MockMultipartFile(filename, new FileInputStream(new File(filePath)));
        /////////////////////////
        MultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);//Main request's headers

        HttpHeaders requestHeadersAttachment = new HttpHeaders();
        requestHeadersAttachment.setContentType(MediaType.IMAGE_PNG);// extract mediatype from file extension
        HttpEntity<Resource> attachmentPart;
        ByteArrayResource fileAsResource = new ByteArrayResource(multipartFile.getBytes()) {
          @Override
          public String getFilename() {
            return multipartFile.getOriginalFilename();
          }
        };
        attachmentPart = new HttpEntity<>(r, requestHeadersAttachment);

        multipartRequest.set("File1", attachmentPart);

        ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders requestHeadersJSON = new HttpHeaders();
        requestHeadersJSON.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntityJSON = new HttpEntity<>(objectMapper.writeValueAsString(req), requestHeadersJSON);

        multipartRequest.set("request", requestEntityJSON);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(multipartRequest, requestHeaders);//final request
        RestTemplate restTemplate = new RestTemplate();
        if (android.os.Build.VERSION.SDK_INT > 9) {
          StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
          StrictMode.setThreadPolicy(policy);
        }
        // back end system is deployed in Heroku, since free plan is used, response will be abit slow
        restTemplate.postForLocation("https://module-demo-api.herokuapp.com/module/register", requestEntity);
        Toast.makeText(MainActivity.this, "File successfully submitted. Please check your email", Toast.LENGTH_SHORT).show();
      } catch (IOException e) {
        e.printStackTrace();
      }


    } else {
      Toast.makeText(MainActivity.this, "Please input all fields", Toast.LENGTH_SHORT).show();
    }
  }
}
