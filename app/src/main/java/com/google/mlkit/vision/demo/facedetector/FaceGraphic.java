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

package com.google.mlkit.vision.demo.facedetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import android.widget.Toast;

import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.GraphicOverlay.Graphic;
import com.google.mlkit.vision.demo.StillImageActivity;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
public class FaceGraphic extends Graphic {
    private float left;
    private float top;
    private float right;
    private float bottom;
    private float x;
    private float y;
    private static final String FILE_NAME_TOP = "top.txt";
    private static final String FILE_NAME_LEFT = "left.txt";
    private static final String FILE_NAME_RIGHT = "right.txt";
    private static final String FILE_NAME_BOTTOM = "bottom.txt";
    private static final String FILE_NAME_SMILE = "smile.txt";
    private static final float FACE_POSITION_RADIUS = 4.0f;
    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float ID_Y_OFFSET = 40.0f;
    private static final float ID_X_OFFSET = -40.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private static final int NUM_COLORS = 10;
    private static final int[][] COLORS = new int[][]{
            // {Text color, background color}
            {Color.BLACK, Color.WHITE},
            {Color.WHITE, Color.MAGENTA},
            {Color.BLACK, Color.LTGRAY},
            {Color.WHITE, Color.RED},
            {Color.WHITE, Color.BLUE},
            {Color.WHITE, Color.DKGRAY},
            {Color.BLACK, Color.CYAN},
            {Color.BLACK, Color.YELLOW},
            {Color.WHITE, Color.BLACK},
            {Color.BLACK, Color.GREEN}
    };


    private Paint facePositionPaint;
    private Paint[] idPaints;
    private Paint[] boxPaints;
    private Paint[] labelPaints;

    private volatile Face face;

    public FaceGraphic(GraphicOverlay overlay, Face face) {
        super(overlay);

        this.face = face;
        final int selectedColor = Color.WHITE;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        int numColors = COLORS.length;
        idPaints = new Paint[numColors];
        boxPaints = new Paint[numColors];
        labelPaints = new Paint[numColors];
        for (int i = 0; i < numColors; i++) {
            idPaints[i] = new Paint();
            idPaints[i].setColor(COLORS[i][0] /* text color */);
            idPaints[i].setTextSize(ID_TEXT_SIZE);

            boxPaints[i] = new Paint();
            boxPaints[i].setColor(COLORS[i][1] /* background color */);
            boxPaints[i].setStyle(Paint.Style.STROKE);
            boxPaints[i].setStrokeWidth(BOX_STROKE_WIDTH);

            labelPaints[i] = new Paint();
            labelPaints[i].setColor(COLORS[i][1]  /* background color */);
            labelPaints[i].setStyle(Paint.Style.FILL);
        }
    }

    public FaceGraphic() {

    }
    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override

    public void draw(Canvas canvas) {
        Face face = this.face;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        x = translateX(face.getBoundingBox().centerX());
        y = translateY(face.getBoundingBox().centerY());
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);

        // Calculate positions.
        left = x - scale(face.getBoundingBox().width() / 2.0f);
        top = y - scale(face.getBoundingBox().height() / 2.0f);
        right = x + scale(face.getBoundingBox().width() / 2.0f);
        bottom = y + scale(face.getBoundingBox().height() / 2.0f);
        float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
        float yLabelOffset = -lineHeight;

        // Decide color based on face ID
        int colorID = (face.getTrackingId() == null)
                ? 0 : Math.abs(face.getTrackingId() % NUM_COLORS);
        float righteyeopen = face.getRightEyeOpenProbability();
        float lefteyeopen = face.getLeftEyeOpenProbability();
//        float smiling = face.getSmilingProbability();
        reset:
//        if(face.getTrackingId() != 0){
//            Toast.makeText(getApplicationContext(),"Harap memotret satu wajah saja",Toast.LENGTH_LONG).show();
//            break reset;
//        }
        if (righteyeopen<0.5 || lefteyeopen < 0.5){
            Toast.makeText(getApplicationContext(),"Harap membuka kedua mata",Toast.LENGTH_LONG).show();
        }else{
//            Toast.makeText(getApplicationContext(),"detected face "+face.getTrackingId(),Toast.LENGTH_LONG).show();

            // Calculate width and height of label box
            float textWidth = idPaints[colorID].measureText("ID: " + face.getTrackingId());
            if (face.getSmilingProbability() != null) {
                yLabelOffset -= lineHeight;
                textWidth = Math.max(textWidth, idPaints[colorID].measureText(
                        String.format(Locale.US, "Happiness: %.2f", face.getSmilingProbability())));
            }
            if (face.getLeftEyeOpenProbability() != null) {
                yLabelOffset -= lineHeight;
                textWidth = Math.max(textWidth, idPaints[colorID].measureText(
                        String.format(Locale.US, "Left eye: %.2f", face.getLeftEyeOpenProbability())));
            }
            if (face.getRightEyeOpenProbability() != null) {
                yLabelOffset -= lineHeight;
                textWidth = Math.max(textWidth, idPaints[colorID].measureText(
                        String.format(Locale.US, "Right eye: %.2f", face.getLeftEyeOpenProbability())));
            }

            // Draw labels
            canvas.drawRect(left - BOX_STROKE_WIDTH,
                    top + yLabelOffset,
                    left + textWidth + (2 * BOX_STROKE_WIDTH),
                    top,
                    labelPaints[colorID]);
            yLabelOffset += ID_TEXT_SIZE;
            canvas.drawRect(left, top, right, bottom, boxPaints[colorID]);
            canvas.drawText("ID: " + face.getTrackingId(), left, top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;

            // Draws all face contours.
            for (FaceContour contour : face.getAllContours()) {
                for (PointF point : contour.getPoints()) {
                    canvas.drawCircle(
                            translateX(point.x), translateY(point.y), FACE_POSITION_RADIUS, facePositionPaint);
                }
            }

            // Draws smiling and left/right eye open probabilities.
            if (face.getSmilingProbability() != null) {
                canvas.drawText(
                        "Smiling: " + String.format(Locale.US, "%.2f", face.getSmilingProbability()),
                        left,
                        top + yLabelOffset,
                        idPaints[colorID]);
                yLabelOffset += lineHeight;
//            FileOutputStream smile_txt = null;
//            String left_string = String.valueOf(face.getSmilingProbability());
//            try{
//                smile_txt = getApplicationContext().openFileOutput(FILE_NAME_SMILE, MODE_PRIVATE);
//                smile_txt.write(left_string.getBytes());
//            }catch (FileNotFoundException e){
//                e.printStackTrace();
//            }catch (IOException e){
//                e.printStackTrace();
//            }finally {
//                if(smile_txt!=null){
//                    try {
//                        smile_txt.close();
//                    }catch (IOException e){
//                        e.printStackTrace();
//                    }
//                }
//            }
            }

            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
            if (leftEye != null && face.getLeftEyeOpenProbability() != null) {
                canvas.drawText(
                        "Left eye open: " + String.format(Locale.US, "%.2f", face.getLeftEyeOpenProbability()),
                        translateX(leftEye.getPosition().x) + ID_X_OFFSET,
                        translateY(leftEye.getPosition().y) + ID_Y_OFFSET,
                        idPaints[colorID]);
            } else if (leftEye != null && face.getLeftEyeOpenProbability() == null) {
                canvas.drawText(
                        "Left eye",
                        left,
                        top + yLabelOffset,
                        idPaints[colorID]);
                yLabelOffset += lineHeight;
            } else if (leftEye == null && face.getLeftEyeOpenProbability() != null) {
                canvas.drawText(
                        "Left eye open: " + String.format(Locale.US, "%.2f", face.getLeftEyeOpenProbability()),
                        left,
                        top + yLabelOffset,
                        idPaints[colorID]);
                yLabelOffset += lineHeight;
            }

            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
            if (rightEye != null && face.getRightEyeOpenProbability() != null) {
                canvas.drawText(
                        "Right eye open: " + String.format(Locale.US, "%.2f", face.getRightEyeOpenProbability()),
                        translateX(rightEye.getPosition().x) + ID_X_OFFSET,
                        translateY(rightEye.getPosition().y) + ID_Y_OFFSET,
                        idPaints[colorID]);
            } else if (rightEye != null && face.getRightEyeOpenProbability() == null) {
                canvas.drawText(
                        "Right eye",
                        left,
                        top + yLabelOffset,
                        idPaints[colorID]);
                yLabelOffset += lineHeight;
            } else if (rightEye == null && face.getRightEyeOpenProbability() != null) {
                canvas.drawText(
                        "Right eye open: " + String.format(Locale.US, "%.2f", face.getRightEyeOpenProbability()),
                        left,
                        top + yLabelOffset,
                        idPaints[colorID]);
            }

            // Draw facial landmarks
            drawFaceLandmark(canvas, FaceLandmark.LEFT_EYE);
            drawFaceLandmark(canvas, FaceLandmark.RIGHT_EYE);
            drawFaceLandmark(canvas, FaceLandmark.LEFT_CHEEK);
            drawFaceLandmark(canvas, FaceLandmark.RIGHT_CHEEK);

            FileOutputStream fos = null;
            int intTop = (int) top;
            String top_string = String.valueOf(intTop);
            try{
                fos = getApplicationContext().openFileOutput(FILE_NAME_TOP, MODE_PRIVATE);
                fos.write(top_string.getBytes());
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                if(fos!=null){
                    try {
                        fos.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }

            FileOutputStream fos_left = null;
            int intLeft = (int) left;
            String left_string = String.valueOf(intLeft);
            try{
                fos_left = getApplicationContext().openFileOutput(FILE_NAME_LEFT, MODE_PRIVATE);
                fos_left.write(left_string.getBytes());
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                if(fos_left!=null){
                    try {
                        fos_left.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }

            FileOutputStream fos_right = null;
            int intRight = (int) right;
            String right_string = String.valueOf(intRight);
            try{
                fos_right = getApplicationContext().openFileOutput(FILE_NAME_RIGHT, MODE_PRIVATE);
                fos_right.write(right_string.getBytes());
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                if(fos_right!=null){
                    try {
                        fos_right.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }

            FileOutputStream fos_bottom = null;
            int intBottom = (int) bottom;
            String bottom_string = String.valueOf(intBottom);
            try{
                fos_bottom = getApplicationContext().openFileOutput(FILE_NAME_BOTTOM, MODE_PRIVATE);
                fos_bottom.write(bottom_string.getBytes());
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                if(fos_bottom!=null){
                    try {
                        fos_bottom.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }

//        setTop(top);
//        setBottom(bottom);
//        setLeft(left);
//        setRight(right);
            Log.d("STATE2","top : " + intTop + " bottom : " + intBottom + " left : " + intLeft + " right : " + intRight);
        }
    }

    private void drawFaceLandmark(Canvas canvas, @LandmarkType int landmarkType) {
        FaceLandmark faceLandmark = face.getLandmark(landmarkType);
        if (faceLandmark != null) {
            canvas.drawCircle(
                    translateX(faceLandmark.getPosition().x),
                    translateY(faceLandmark.getPosition().y),
                    FACE_POSITION_RADIUS,
                    facePositionPaint);
        }
    }
}
