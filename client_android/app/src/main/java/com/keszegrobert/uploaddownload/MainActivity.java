package com.keszegrobert.uploaddownload;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity{

    private static final int RESULT_LOAD_IMAGE = 1;
    private static final String SERVER_ADDRESS = "http://192.168.0.33:8282/";
    ImageView imToUpload, imDownloaded;
    Button btUploadImage, btDownloadImage;
    EditText etUploadImageName, etDonwloadImageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imToUpload = (ImageView)findViewById(R.id.imageToUpload);
        imToUpload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.d("Robi", "image button");
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
            }
        });

        imDownloaded = (ImageView)findViewById(R.id.imageDownloaded);
        btUploadImage = (Button)findViewById(R.id.btUploadImage);
        btUploadImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.d("Robi", "upload button");
                Bitmap image = ((BitmapDrawable)imToUpload.getDrawable()).getBitmap();
                new UploadImage(image, etUploadImageName.getText().toString()).execute();
            }
        });

        btDownloadImage = (Button)findViewById(R.id.btDownloadImage);
        btDownloadImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.d("Robi", "download button");
                new DownloadImage(etDonwloadImageName.getText().toString()).execute();
            }
        });

        etUploadImageName = (EditText)findViewById(R.id.etUploadName);
        etDonwloadImageName = (EditText)findViewById(R.id.etDownloadName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null){
            Uri imageUri = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imToUpload.setImageBitmap(bitmap);
        }
    }


    private class UploadImage extends AsyncTask<Void,Void,Void> {
        Bitmap image;
        String name;
        int respCode;

        public UploadImage(Bitmap image, String name){
            this.image = image;
            this.name = name;
            Log.d("Robi", "The name of the picture will be: " + name);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            String encodedImage = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);

            HttpURLConnection connection = null;
            DataOutputStream outputStream = null;
            InputStream inputStream = null;

            String twoHyphens = "--";
            String boundary =  "*****"+Long.toString(System.currentTimeMillis())+"*****";
            String lineEnd = "\r\n";

            String filefield = "file";
            try {
                URL url = new URL(SERVER_ADDRESS);
                connection = (HttpURLConnection) url.openConnection();

                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                //connection.setChunkedStreamingMode(1024);

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);
                //connection.setRequestProperty("Content-Length", encodedImage.length());

                outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\""+filefield+"\"; filename=\"" +name+ ".jpg\"" + lineEnd);
                outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
                outputStream.writeBytes("Content-Transfer-Encoding: base64" + lineEnd);
                outputStream.writeBytes(lineEnd);
                Log.d("Robi", encodedImage);

                outputStream.writeBytes(encodedImage);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                this.respCode = connection.getResponseCode();
                if (this.respCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                        Log.d("Robi", inputLine);
                    }

                    inputStream.close();
                    connection.disconnect();
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            /*switch (respCode) {
                case 200:
                    //all went ok - read response
                    Toast.makeText(getApplicationContext(), "Image Uploaded", Toast.LENGTH_SHORT);
                    break;
                default:
                    Toast.makeText(getApplicationContext(), "Data was corrupted", Toast.LENGTH_SHORT);

            }*/
        }
    }

    private class DownloadImage extends AsyncTask<Void,Void,Bitmap>{
        String name;

        public DownloadImage(String name){
            this.name = name;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(SERVER_ADDRESS +  name);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                return BitmapFactory.decodeStream(inputStream);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null){
                imDownloaded.setImageBitmap(bitmap);
            }

        }
    }

}
