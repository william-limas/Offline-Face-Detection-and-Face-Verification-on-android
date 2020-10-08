/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.vision.demo.facedetector.FaceDetectorProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity demonstrating different image detector features with a still image from camera.
 */
@KeepName
public final class StillImageActivity2 extends AppCompatActivity {

    private static final String TAG = "StillImageActivity";

    private static final String OBJECT_DETECTION = "Object Detection";
    private static final String FACE_DETECTION = "Face Detection";

    private static final String SIZE_SCREEN = "w:screen"; // Match screen width
    private static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio
    private static final String SIZE_640_480 = "w:640"; // ~640*480 in a normal ratio

    private static final String KEY_IMAGE_URI = "com.google.mlkit.vision.demo.KEY_IMAGE_URI";
    private static final String KEY_SELECTED_SIZE = "com.google.mlkit.vision.demo.KEY_SELECTED_SIZE";
    private static final String FILE_NAME_TOP = "top.txt";
    private static final String FILE_NAME_LEFT = "left.txt";
    private static final String FILE_NAME_RIGHT = "right.txt";
    private static final String FILE_NAME_BOTTOM = "bottom.txt";
    private static final String FILE_NAME_SMILE = "smile.txt";
    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_CHOOSE_IMAGE = 1002;

    private ImageView preview;
    private GraphicOverlay graphicOverlay;
    private String selectedMode = OBJECT_DETECTION;
    private String selectedSize = SIZE_SCREEN;

    boolean isLandScape;

    private Uri imageUri;
    private int imageMaxWidth;
    private int imageMaxHeight;
    private VisionImageProcessor imageProcessor;
    public Bitmap imageBitmap;
    public Bitmap resizedBitmap;
    String top_str;
    String left_str;
    String right_str;
    String bottom_str;
    String smile_str;
    int bottom;
    int top;
    int left;
    int right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_still_image);

        findViewById(R.id.select_image_button)
                .setOnClickListener(
                        view -> {
                            // Menu for selecting either: a) take new photo b) select from existing
                            PopupMenu popup = new PopupMenu(StillImageActivity2.this, view);
                            popup.setOnMenuItemClickListener(
                                    menuItem -> {
                                        int itemId = menuItem.getItemId();
                                        if (itemId == R.id.select_images_from_local) {
                                            startChooseImageIntentForResult();
                                            return true;
                                        }
//                                        else if (itemId == R.id.take_photo_using_camera) {
//                                            startCameraIntentForResult();
//                                            return true;
//                                        }
                                        return false;
                                    });
                            MenuInflater inflater = popup.getMenuInflater();
                            inflater.inflate(R.menu.camera_button_menu, popup.getMenu());
                            popup.show();
                        });
        preview = findViewById(R.id.preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);

        populateFeatureSelector();
        populateSizeSelector();

        isLandScape =
                (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
            selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE);
        }

        View rootView = findViewById(R.id.root);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                imageMaxWidth = rootView.getWidth();
                imageMaxHeight = rootView.getHeight() - findViewById(R.id.control).getHeight();
                if (SIZE_SCREEN.equals(selectedSize)) {
                    tryReloadAndDetectInImage();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        createImageProcessor();
        tryReloadAndDetectInImage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.still_image_menu, menu);
        return true;
    }

    private void populateFeatureSelector() {
        Spinner featureSpinner = findViewById(R.id.feature_selector);
        List<String> options = new ArrayList<>();
        options.add(FACE_DETECTION);


        // Creating adapter for featureSpinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        featureSpinner.setAdapter(dataAdapter);
        featureSpinner.setOnItemSelectedListener(
                new OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                        selectedMode = parentView.getItemAtPosition(pos).toString();
                        createImageProcessor();
                        tryReloadAndDetectInImage();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
    }

    private void populateSizeSelector() {
        Spinner sizeSpinner = findViewById(R.id.size_selector);
        List<String> options = new ArrayList<>();
        options.add(SIZE_SCREEN);
        options.add(SIZE_1024_768);
        options.add(SIZE_640_480);

        // Creating adapter for featureSpinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        sizeSpinner.setAdapter(dataAdapter);
        sizeSpinner.setOnItemSelectedListener(
                new OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                        selectedSize = parentView.getItemAtPosition(pos).toString();
                        createImageProcessor();
                        tryReloadAndDetectInImage();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_IMAGE_URI, imageUri);
        outState.putString(KEY_SELECTED_SIZE, selectedSize);
    }

    private void startCameraIntentForResult() {
        // Clean up last time's image
        imageUri = null;
        preview.setImageBitmap(null);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void startChooseImageIntentForResult() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            tryReloadAndDetectInImage();
        } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data.getData();
            tryReloadAndDetectInImage();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void tryReloadAndDetectInImage() {
        Log.d(TAG, "Try reload and detect image");
        try {
            if (imageUri == null) {
                return;
            }

            if (SIZE_SCREEN.equals(selectedSize) && imageMaxWidth == 0) {
                // UI layout has not finished yet, will reload once it's ready.
                return;
            }

            imageBitmap = BitmapUtils.getBitmapFromContentUri(getContentResolver(), imageUri);
            if (imageBitmap == null) {
                return;
            }

            // Clear the overlay first
            graphicOverlay.clear();

            // Get the dimensions of the image view
            Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

            // Determine how much to scale down the image
            float scaleFactor =
                    Math.max(
                            (float) imageBitmap.getWidth() / (float) targetedSize.first,
                            (float) imageBitmap.getHeight() / (float) targetedSize.second);

            resizedBitmap =
                    Bitmap.createScaledBitmap(
                            imageBitmap,
                            (int) (imageBitmap.getWidth() / scaleFactor),
                            (int) (imageBitmap.getHeight() / scaleFactor),
                            true);

            preview.setImageBitmap(resizedBitmap);

            if (imageProcessor != null) {
                graphicOverlay.setImageSourceInfo(
                        resizedBitmap.getWidth(), resizedBitmap.getHeight(), /* isFlipped= */ false);
                imageProcessor.processBitmap(resizedBitmap, graphicOverlay);
            } else {
                Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error retrieving saved image");
            imageUri = null;
        }
    }

    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;

        switch (selectedSize) {
            case SIZE_SCREEN:
                targetWidth = imageMaxWidth;
                targetHeight = imageMaxHeight;
                break;
            case SIZE_640_480:
                targetWidth = isLandScape ? 640 : 480;
                targetHeight = isLandScape ? 480 : 640;
                break;
            case SIZE_1024_768:
                targetWidth = isLandScape ? 1024 : 768;
                targetHeight = isLandScape ? 768 : 1024;
                break;
            default:
                throw new IllegalStateException("Unknown size");
        }

        return new Pair<>(targetWidth, targetHeight);
    }

    private void createImageProcessor() {
        try {
            switch (selectedMode) {
                case FACE_DETECTION:
                    imageProcessor = new FaceDetectorProcessor(this);

//                    FileInputStream fis_smile = null;
//                    try{
//                        fis_smile = openFileInput(FILE_NAME_SMILE);
//                        InputStreamReader isr = new InputStreamReader(fis_smile);
//                        BufferedReader br = new BufferedReader(isr);
//                        StringBuilder sb = new StringBuilder();
//
//                        String text;
//
//                        while ((text=br.readLine())!=null){
//                            sb.append(text);
//                        }
//                        smile_str = sb.toString();
//                    }catch (FileNotFoundException e){
//                        e.printStackTrace();
//                    }catch (IOException e){
//                        e.printStackTrace();
//                    }finally{
//                        if (fis_smile != null){
//                            try{
//                                fis_smile.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//
//                    float smile = Float.parseFloat(smile_str);
//                    Log.d("STATE2", "smile : "+ smile);

//                    if(smile>0.5){
//                        imageProcessor = new FaceDetectorProcessor(this);
//                    }else{
//                        Toast.makeText(getApplicationContext(),"Harap senyum, foto hanya terdeteksi jika senyum",Toast.LENGTH_LONG).show();
//                    }
                    break;
                default:
                    Log.e(TAG, "Unknown selectedMode: " + selectedMode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + selectedMode, e);
            Toast.makeText(
                    getApplicationContext(),
                    "Can not create image processor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }


    public void Crop(View view) {
        //FaceGraphic fg1 = new FaceGraphic();
        //        fg1.draw(canvas);
        FileInputStream fis = null;
        try{
            fis = openFileInput(FILE_NAME_TOP);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();

            String text;

            while ((text=br.readLine())!=null){
                sb.append(text);
            }
            top_str = sb.toString();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally{
            if (fis != null){
                try{
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        FileInputStream fis_left = null;
        try{
            fis_left = openFileInput(FILE_NAME_LEFT);
            InputStreamReader isr = new InputStreamReader(fis_left);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();

            String text;

            while ((text=br.readLine())!=null){
                sb.append(text);
            }
            left_str = sb.toString();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally{
            if (fis_left != null){
                try{
                    fis_left.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        FileInputStream fis_right = null;
        try{
            fis_right = openFileInput(FILE_NAME_RIGHT);
            InputStreamReader isr = new InputStreamReader(fis_right);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();

            String text;

            while ((text=br.readLine())!=null){
                sb.append(text);
            }
            right_str = sb.toString();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally{
            if (fis_right != null){
                try{
                    fis_right.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        FileInputStream fis_bottom = null;
        try{
            fis_bottom = openFileInput(FILE_NAME_BOTTOM);
            InputStreamReader isr = new InputStreamReader(fis_bottom);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();

            String text;

            while ((text=br.readLine())!=null){
                sb.append(text);
            }
            bottom_str = sb.toString();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally{
            if (fis_bottom != null){
                try{
                    fis_bottom.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        top = Integer.parseInt(top_str);
        bottom = Integer.parseInt(bottom_str);
        left = Integer.parseInt(left_str);
        right = Integer.parseInt(right_str);

//        Log.d("STATE2", "top : "+ top + " bottom : "+ bottom +" left : "+ left +" right : "+ right );

        Bitmap new_bitmap = Bitmap.createBitmap(resizedBitmap,left,top,right-left,bottom-top);
        Bitmap new_bitmap_resized = Bitmap.createScaledBitmap(new_bitmap,160,160,true);

        saveToInternalStorage(new_bitmap_resized, 2 );
        preview.setImageBitmap(new_bitmap_resized);
        Log.d("STATE2", "top : "+ top + " bottom : "+ bottom +" left : "+ left +" right : "+ right );

        Intent intent = new Intent(StillImageActivity2.this, VerifActivity2.class);
        intent.putExtra("BitmapImage", new_bitmap_resized);
        startActivity(intent);
    }



    private void saveToInternalStorage(Bitmap bitmapImage, int counter){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        // path to /data/data/yourapp/app_imageDir
        File MyDirectory = cw.getDir("imageDir", Context.MODE_PRIVATE);

        // Create imageDir
        File MyPath = new File(MyDirectory,"Image" + counter + ".jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(MyPath);

            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //return MyDirectory.getAbsolutePath();
    }
}
