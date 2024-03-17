package com.example.uidesign_cistercian;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ImageDisplayActivity extends AppCompatActivity {

    private Mat matImage;
    private FrameLayout imageOverlayLayout;
    private int imageHeight;
    private List<Integer> arabicResults = new ArrayList<>();
    private List<Rect> finalFilteredRects = new ArrayList<>();
    private List<Rect> foundRecsAfterCountours = new ArrayList<>();

    // COLOURS
    private final Scalar blue = new Scalar(0, 0, 255);
    private final Scalar red = new Scalar(255, 0, 0);
    private final Scalar green = new Scalar(0, 255, 0);
    private final Scalar pink = new Scalar(255, 0, 255);
    private final Scalar orange = new Scalar(255, 165, 0);
    private final Scalar black = new Scalar(0, 0, 0);
    private final Scalar white = new Scalar(255, 255, 255);
    private int lightBlue = Color.rgb(193, 230, 254);
    private int darkBlue = Color.rgb(0, 28, 52);


    // Load openCV library
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV");
            // Handle the failure here
        } else {
            Log.d("OpenCV", "OpenCV loaded successfully");
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // FrameLayout
        imageOverlayLayout = findViewById(R.id.imageOverlayLayout);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        ImageView imageView = findViewById(R.id.image_display_view);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey("imageUri")) {
                // Image is passed as a URI
                Uri imageUri = Uri.parse(extras.getString("imageUri"));
                imageView.setImageURI(imageUri);
                convertToGrayscale(imageUri);
            } else if (extras.containsKey("imageBitmap")) {
                // Image is passed as a byte array
                byte[] byteArray = extras.getByteArray("imageBitmap");
                Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                imageView.setImageBitmap(bmp);
                processImage(bmp);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Finish this activity and return to the parent activity (MainActivity)
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void convertToGrayscale(Uri imageUri) {
        try {
            // Convert Uri to Bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            processImage(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            showToast("File not found. Please try again.");
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Error processing the image. Please try again.");
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(ImageDisplayActivity.this, message, Toast.LENGTH_LONG).show());
    }

    private void processImage(Bitmap bitmap) {
        // Initialize matImage with the size of bitmap
        matImage = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
        imageHeight = matImage.rows();
        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, matImage);
        // Convert to Grayscale
        Imgproc.cvtColor(matImage, matImage, Imgproc.COLOR_RGB2GRAY);

        // Dilate the image
        Mat dilatedImage = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(210, 210));
        Imgproc.dilate(matImage, dilatedImage, element);

        // Apply Median Blur
        Mat bgImage = new Mat();
        Imgproc.medianBlur(dilatedImage, bgImage, 25);

        // Calculate the absolute difference
        Mat diffImage = new Mat();
        Core.absdiff(matImage, bgImage, diffImage);
        Core.bitwise_not(diffImage, diffImage); // Invert the difference

        // Normalize
        Mat normImage = new Mat();
        Core.normalize(diffImage, normImage, 0, 255, Core.NORM_MINMAX);

        // Threshold and Normalize
        Mat thrImage = new Mat();
        Imgproc.threshold(normImage, thrImage, 210, 255, Imgproc.THRESH_TRUNC);
        Core.normalize(thrImage, thrImage, 0, 255, Core.NORM_MINMAX);

        // Apply Gaussian Blur for noise reduction
//        Imgproc.GaussianBlur(thrImage, thrImage, new Size(9, 9), 0);

        Imgproc.erode(thrImage, thrImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(4, 4)));


        // Apply Binary Threshold
        Mat binaryImage = new Mat(); // keep copy of binary image for future processing
        Imgproc.threshold(thrImage, binaryImage, 105, 255, Imgproc.THRESH_BINARY);

        // Apply Canny Edge Detection
        Mat edgeDetectedImage = new Mat();
        Imgproc.Canny(binaryImage, edgeDetectedImage, 140, 170);

        // Convert matImage to 3 channels
        Mat coloredBinaryImage = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC3);
        Imgproc.cvtColor(binaryImage, coloredBinaryImage, Imgproc.COLOR_GRAY2BGR);

        // Find Contours in the edge detected image
        foundRecsAfterCountours = findAndApproximateContours(edgeDetectedImage, coloredBinaryImage);

        // Draw the Quadrants
        drawQuadrants(coloredBinaryImage, foundRecsAfterCountours);

        // Convert processed Mat back to Bitmap
        Utils.matToBitmap(coloredBinaryImage, bitmap);

        // Update ImageView with the processed Bitmap
        runOnUiThread(() -> {
            ImageView imageView = findViewById(R.id.image_display_view);
            imageView.setImageBitmap(bitmap);
        });
    }

    private List<Rect> findAndApproximateContours(Mat edgeDetectedImage, Mat coloredBinaryImage) {
        //System.out.println("findAndApproximateContours");
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edgeDetectedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // List to hold all bounding rectangles
        List<Rect> boundingRects = new ArrayList<>();

        // Iterate over all detected contours
        for (MatOfPoint contour : contours) {
            // Convert contour to a different format
            MatOfPoint2f contourFloat = new MatOfPoint2f(contour.toArray());
            // Approximate the contour to a polygon
            double epsilon = 0.0045 * Imgproc.arcLength(contourFloat, true);
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(contourFloat, approxCurve, epsilon, true);
            // Draw the approximated contour for visualization
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
            //Imgproc.drawContours(coloredBinaryImage, Arrays.asList(points), -1, blue, 2);
            // Calculate bounding rectangle for each contour
            Rect boundingRect = Imgproc.boundingRect(contour);
            boundingRects.add(boundingRect);
        }

        // Filter out rectangles that are inside other rectangles
        List<Rect> foundRecs = filterRectangles(boundingRects, imageHeight, coloredBinaryImage);

        return foundRecs;
    }

    private List<Rect> filterRectangles(List<Rect> rects, int imageHeight, Mat image) {

        List<Rect> sizeFilteredRects = new ArrayList<>(); // List of rectangles that pass filter 1
        List<Rect> uniqueFilteredRects = new ArrayList<>(); // List of rectangles that pass filters 1 and 2
        Set<String> uniqueRectSignatures = new HashSet<>(); // Signatures for filter 2
        //List<Rect> finalFilteredRects = new ArrayList<>(); // Set of rectangles that pass filters 1, 2, and 3 (already initialized in the beginning)

        // Filter 1 - Filter out rectangles that are too small
        double minHeightThreshold = 0.15 * imageHeight; // Minimum height of a rectangle
        for (Rect rect : rects) {
            if (rect.height >= minHeightThreshold) {
                sizeFilteredRects.add(rect);
                System.out.println("rectangle added as not small with height = " + rect.height + " (img height = " + imageHeight + ")");
            }
        }

        /*
        // Extra filter -  filter out rectangles that dont have a stem
        NOTE FROM PAST SELF: PARA SEGMENTS QUE ESTAO SEPARADOS DA MAIN STEM, VER SE UMA GRANDE MAIORIA DA PERCENTAGEM DOS PIXELS
        ]E PRETA, SE FOR VERDADE E SE O COMPRIMENTO FOR MENOS DE METADE DO COMPRIMENTO TO MAIOR RECT (SE PELO MENOS 50% DOS RECTS TIVEREM
        O MESMO GRANDE COMPRIMENTO OU SIOMILAR), ]E PORQUE ]E UM SEGMENTO SEPARADO, ENTAO TEM DE SER ENCONTRAR A STEM DELE.
        for (Rect rect : sizeFilteredRects) {
            Mat rotatedImage = image.clone(); // Clone the image for rotation

            if (rect.width > rect.height) {
                // Rotate the image 90 degrees clockwise
                Core.rotate(coloredBinaryImage, rotatedImage, Core.ROTATE_90_COUNTERCLOCKWISE);
                Rect rotatedRect = new Rect(rect.y, coloredBinaryImage.cols() - rect.x - rect.width, rect.height, rect.width);
                processCipher(rotatedImage, rotatedRect);
                Core.rotate(rotatedImage, coloredBinaryImage, Core.ROTATE_90_CLOCKWISE);
            } else {
                //System.out.println("rectangle was NOT rotated");
                processCipher(coloredBinaryImage, rect);
            }

        }

         */

        // Filter 2 - Filter out duplicates based on a unique signature of each rectangle
        for (Rect rect : sizeFilteredRects) {
            String signature = rect.tl().toString() + "-" + rect.br().toString(); // Create a unique signature
            if (uniqueRectSignatures.add(signature)) {
                // If the signature is added successfully, it's not a duplicate
                System.out.println("rectangle added as not duplicate with height = " + rect.height + " and tl = " + rect.tl() + " and br = " + rect.br());
                uniqueFilteredRects.add(rect);
            }
        }

        // Filter 3 - Filter out rectangles that are contained within others
        for (int i = 0; i < uniqueFilteredRects.size(); i++) {
            Rect rect1 = uniqueFilteredRects.get(i);
            boolean isContained = false;
            for (int j = 0; j < uniqueFilteredRects.size(); j++) {
                if (i == j) continue; // Skip comparing the rectangle with itself
                Rect rect2 = uniqueFilteredRects.get(j);

                if (rect2.contains(rect1.tl()) && rect2.contains(rect1.br())) {
                    // If rect1 is fully contained within rect2
                    isContained = true;
                    break;
                }
            }
            if (!isContained) {
                // If rect1 is not contained within any other rectangle, add it to the final list
                finalFilteredRects.add(rect1);
                System.out.println("rectangle added as not contained");
            }
        }

        return finalFilteredRects;
    }



    private void drawQuadrants(Mat coloredBinaryImage, List<Rect> filteredRects) {
        for (Rect rect : filteredRects) {

            // Draw rectangle for debugging
            drawRectangle(coloredBinaryImage, rect, red, 2);

            // Calculate the center point of the rectangle
            Point center = new Point(rect.tl().x, rect.tl().y);

            // Create a TextView dynamically
            TextView textView = new TextView(this);

            //drawRectangle(coloredBinaryImage, rect, green, 1);
            Line stem = findStem(coloredBinaryImage, rect);
            //stem.draw(coloredBinaryImage);

            int numberResult = 0;

            double angle = stem.getSmallestAngleFromVertical(); // Rotation angle in degrees

            // Perform the rotation with the new dimensions
            Mat rotatedImage = cloneAndCropImageWithPadding(coloredBinaryImage, rect, -angle);
            // Convert to Grayscale
            //Imgproc.cvtColor(rotatedImage, rotatedImage, Imgproc.COLOR_RGB2GRAY);

            // Get the bounding rect
            Rect boundingRect = expandUntilNoBlack(rotatedImage);
            //drawRectangle(rotatedImage, boundingRect, orange, 1);

            // resize the rect to fit exactly the cipher
            //System.out.println("Going to call resizedRect");
            Rect resizedRect = resizeRectangle(rotatedImage, boundingRect, true, true, true, true);
            //drawRectangle(rotatedImage, resizedRect, blue, 1);
            //System.out.println("resizedRect.tl = " + resizedRect.tl() + ", resizedRect.br = " + resizedRect.br());

            // process the cipher and get number
            //System.out.println("Going to call processCipher");
            numberResult = processCipher(rotatedImage, resizedRect);

            Bitmap bitmapImage = Bitmap.createBitmap(rotatedImage.cols(), rotatedImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rotatedImage, bitmapImage);

//            ImageView imageView = findViewById(R.id.image_display_view_provisorio);
//            imageView.setImageBitmap(bitmapImage);

            if(numberResult > 0) {
                arabicResults.add(numberResult);
//                updateResultsDisplay();
                // Add result to history
                ConversionHistoryManager.getInstance(getApplicationContext()).addConversion(numberResult);
            }

            // Set text and other properties for the TextView
            textView.setText(String.valueOf(numberResult));
            textView.setTextColor(darkBlue);
            textView.setBackgroundColor(lightBlue);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20); // text size

            // Measure the TextView to get its dimensions
            textView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = textView.getMeasuredWidth();
            int height = textView.getMeasuredHeight();

            // Create layout params to set position
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            params.leftMargin = (int) (center.x - width / 2); // Adjust horizontal position
            params.topMargin = (int) (center.y - height / 2); // Adjust vertical position

            // Add the TextView to the layout
            imageOverlayLayout.addView(textView, params);
        }
    }

    public Rect expandUntilNoBlack(Mat image) {
        //System.out.println("expandUntilNoBlack called");
        int centerX = image.width() / 2;
        int centerY = image.height() / 2;
        int stepSize = 20; // Increment size
        boolean foundBlack = false;

        // Initial checking area with a starting size
        int width = 60;
        int height = 60;
        int maxTries = 60; // Maximum number of tries to prevent infinite loops
        int tries = 0; // Counter for the number of tries

        do {
            foundBlack = false; // Reset for each iteration
            // Define the checking rectangle
            Rect checkingArea = new Rect(Math.max(0, centerX - width / 2), Math.max(0, centerY - height / 2), width, height);

            // Check for black pixels only on the edges of the checking area
            // Top edge
            for (int col = checkingArea.x; col < checkingArea.x + checkingArea.width; col++) {
                if (isPixelBlack(image, checkingArea.y, col)) {
                    foundBlack = true;
                    break;
                }
            }
            // Bottom edge
            if (!foundBlack) {
                for (int col = checkingArea.x; col < checkingArea.x + checkingArea.width; col++) {
                    if (isPixelBlack(image, checkingArea.y + checkingArea.height - 1, col)) {
                        foundBlack = true;
                        break;
                    }
                }
            }
            // Left edge
            if (!foundBlack) {
                for (int row = checkingArea.y; row < checkingArea.y + checkingArea.height; row++) {
                    if (isPixelBlack(image, row, checkingArea.x)) {
                        foundBlack = true;
                        break;
                    }
                }
            }
            // Right edge
            if (!foundBlack) {
                for (int row = checkingArea.y; row < checkingArea.y + checkingArea.height; row++) {
                    if (isPixelBlack(image, row, checkingArea.x + checkingArea.width - 1)) {
                        foundBlack = true;
                        break;
                    }
                }
            }

            if (foundBlack) {
                // Increase the checking area by the stepSize
                if (tries % 5 == 0) {
                    //System.out.println("Rectangle number " + tries);
                }
                width += stepSize;
                height += stepSize;
                tries++; // Increment the tries counter
            }
        } while (foundBlack && tries < maxTries); // Repeat if a black pixel was found on the edges and tries are below max

        if (tries >= maxTries) {
            System.out.println("Max tries reached, possibly stuck in a loop");
        }

        // Log the number of tries it took to find the rectangle
        System.out.println("Rectangle found at try number: " + tries);

        // Draw the final rectangle
        Rect boundingRect = new Rect(Math.max(0, centerX - width / 2), Math.max(0, centerY - height / 2), width, height);
        return boundingRect;
    }

    private boolean isPixelBlack(Mat image, int row, int col) {
        boolean check = false;
        if (row >= 0 && row < image.height() && col >= 0 && col < image.width()) {
            double[] pixel = image.get(row, col);
            check = pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0; // check if pixel is black
        }
        return check;
    }


    public Rect resizeRectangle(Mat image, Rect rect, boolean resizeTop, boolean resizeBottom, boolean resizeLeft, boolean resizeRight) {
        int rectHeight = rect.height;
        int rectWidth = rect.width;
        // Initialize limits to the rectangle's current boundaries
        int topLimitY = rect.y, bottomLimitY = rect.y + rectHeight - 1;
        int leftLimitX = rect.x, rightLimitX = rect.x + rectWidth - 1;

        System.out.println("rect.x = " + rect.x + ", rect.y = " + rect.y);

        // Additional checks to prevent accessing pixels outside the image
        int maxY = image.rows() - 1;
        int maxX = image.cols() - 1;

        // Variables to control the resizing loop
        boolean foundBlackPixel;
        int maxIterations = 100; // Set a maximum number of iterations to prevent infinite loops
        int iterations = 0;

        boolean topLimitFound = false, bottomLimitFound = false, leftLimitFound = false, rightLimitFound = false;

        //top
        if (resizeTop && topLimitY > 0) {
            //System.out.println("Going to resize the top");
            for (int y = rect.y; y < rect.y + rectHeight; y++) {
                for (int x = rect.x; x < rect.x + rectWidth; x++) {
                    if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                        double[] pixel = image.get(y, x);
                        if (pixel[0] < 10 && pixel[1] < 10 && pixel[2] < 10) {
                            topLimitFound = true;
                            topLimitY = y;
                            //System.out.println("topLimitY = " + topLimitY);
                            Point lineStart = new Point(rect.x, topLimitY);
                            Point lineEnd = new Point(rect.x + rectWidth, topLimitY);
                            //Imgproc.line(image, lineStart, lineEnd, green, 2);
                            break;
                        }
                    }
                }
                if (topLimitFound)
                    break; // Exit the outer loop as well after getting the top limit
            }
            if (!topLimitFound) {
                topLimitY = rect.y;
                Point lineStart = new Point(rect.x, topLimitY);
                Point lineEnd = new Point(rect.x + rectWidth, topLimitY);
                //Imgproc.line(image, lineStart, lineEnd, red, 2);
            }
        }
        if (resizeBottom && bottomLimitY > 0) { //bottom
            //System.out.println("Going to resize the bottom");
            for (int y = rect.y + rectHeight; y > rect.y; y--) {
                for (int x = rect.x; x < rect.x + rectWidth; x++) {
                    if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                        double[] pixel = image.get(y, x);
                        if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                            bottomLimitFound = true;
                            bottomLimitY = y;
                            //System.out.println("bottomLimitY = " + bottomLimitY);
                            Point lineStart = new Point(rect.x, bottomLimitY);
                            Point lineEnd = new Point(rect.x + rectWidth, bottomLimitY);
                            //Imgproc.line(image, lineStart, lineEnd, blue, 1);
                            break;
                        }
                    }
                }
                if (bottomLimitFound)
                    break; // Exit the outer loop as well after getting the bottom limit
            }
            if (!bottomLimitFound) {
                bottomLimitY = rect.y;
                Point lineStart = new Point(rect.x, bottomLimitY);
                Point lineEnd = new Point(rect.x + rectWidth, bottomLimitY);
                //Imgproc.line(image, lineStart, lineEnd, blue, 1);
            }
        }
        if (resizeLeft && leftLimitX > 0) { //left
            //System.out.println("Going to resize the left");
            for (int x = rect.x; x < rectWidth; x++) {
                for (int y = rect.y; y < rect.y + rectHeight; y++) {
                    if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                        double[] pixel = image.get(y, x);
                        if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                            leftLimitFound = true;
                            leftLimitX = x;
                            //System.out.println("leftLimitX = " + leftLimitX);
                            Point lineStart = new Point(leftLimitX, rect.y);
                            Point lineEnd = new Point(leftLimitX, rect.y + rectHeight);
                            //Imgproc.line(image, lineStart, lineEnd, blue, 1);
                            break;
                        }
                    }
                }
                if (leftLimitFound)
                    break; // Exit the outer loop as well after drawing 2nd line
            }
            if (!leftLimitFound) {
                leftLimitX = rect.x + rectWidth;
                Point lineStart = new Point(leftLimitX, rect.y);
                Point lineEnd = new Point(leftLimitX, rect.y + rectHeight);
                //Imgproc.line(image, lineStart, lineEnd, blue, 1);
            }
        }
        if (resizeRight && rightLimitX > 0) { //right
            //System.out.println("Going to resize the right");
            for (int x = rect.x + rectWidth; x > rect.x; x--) {
                for (int y = rect.y; y < rect.y + rectHeight; y++) {
                    if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                        double[] pixel = image.get(y, x);
                        if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                            rightLimitFound = true;
                            rightLimitX = x;
                            //System.out.println("rightLimitX = " + rightLimitX);
                            Point lineStart = new Point(rightLimitX, rect.y);
                            Point lineEnd = new Point(rightLimitX, rect.y + rectHeight);
                            //Imgproc.line(image, lineStart, lineEnd, blue, 1);
                            break;
                        }
                    }
                }
                if (rightLimitFound)
                    break; // Exit the outer loop as well after drawing 2nd line
            }
            if (!rightLimitFound) {
                rightLimitX = rect.x + rectWidth;
                Point lineStart = new Point(rightLimitX, rect.y);
                Point lineEnd = new Point(rightLimitX, rect.y + rectHeight);
                //Imgproc.line(image, lineStart, lineEnd, blue, 1);
            }
        }

        Rect finalRect = new Rect(leftLimitX, topLimitY, rightLimitX - leftLimitX, bottomLimitY - topLimitY);
        //drawRectangle(image, finalRect, orange, 1);
        //System.out.println("Created resize rect");

        return finalRect;
    }




    private Line findStem(Mat image, Rect rect) {

        Map<Line, Double> percentages1stCheck = new HashMap<>();
        Map<Line, Double> percentages2ndCheck = new HashMap<>();
        Map<Line, Double> percentagesTop4 = new HashMap<>();

        Point rectMiddlePoint = new Point(rect.x + rect.width / 2.00, rect.y + rect.height / 2.00);

        // Draw the bounding rectangle
        //drawRectangle(image, rect, green, 1);

        boolean stemFound = false;
        boolean firstCheckDone = false;
        boolean secondCheckDone = false;
        boolean similarPercentages = false;
        int dividerInt = 70;
        double dividerDouble = 70.0; // these 2 need to have the same value

        Line stem = null;

        if (!stemFound) {
            for (int a = 0; a <= dividerInt; a++) {
                Point p1 = new Point(rect.x, rect.y + a * (rect.height / dividerDouble));
                Point p2 = new Point(rect.x + rect.width, rect.y + rect.height - (a * (rect.height / dividerDouble)));
                Point divisionPoint1 = adjustPointToBounds(image, p1);
                Point divisionPoint2 = adjustPointToBounds(image, p2);
                if (divisionPoint1 != null && divisionPoint2 != null) {
                    stem = new Line(divisionPoint1, divisionPoint2, red, 1);
                } else {
                    System.out.println("Print");
                }

                double percentage = stem.getUninterruptedBlackPixelPercentage(image);
                percentages1stCheck.put(stem, percentage);

                if (percentage >= 90) {
                    //stem.draw(image);
                    stemFound = true;
                    System.out.println("Stem Found at 1st check");
                    break;
                }
            }
            firstCheckDone = true;
        }
        if (!stemFound && firstCheckDone) {
            for (int a = 0; a <= dividerInt; a++) {
                Point p1 = new Point(rect.x + rect.width - (a * (rect.width / dividerDouble)), rect.y);
                Point p2 = new Point(rect.x + a * (rect.width / dividerDouble), rect.y + rect.height);
                Point divisionPoint1 = adjustPointToBounds(image, p1);
                Point divisionPoint2 = adjustPointToBounds(image, p2);
                if (divisionPoint1 != null && divisionPoint2 != null) {
                    stem = new Line(divisionPoint1, divisionPoint2, red, 1);
                } else {
                    System.out.println("Print");
                }

                double percentage = stem.getUninterruptedBlackPixelPercentage(image);
                percentages2ndCheck.put(stem, percentage);

                if (percentage >= 90) {
                    //stem.draw(image);
                    stemFound = true;
                    System.out.println("Stem Found at 2nd check");
                    break;
                }

                //stemCandidate.draw(coloredBinaryImage);
            }
            secondCheckDone = true;
        }
        if (!stemFound && firstCheckDone && secondCheckDone) {
            // For 1st check
            // create list with the map entries of the percentages
            List<Map.Entry<Line, Double>> sortedEntries1 = new ArrayList<>(percentages1stCheck.entrySet());
            // Sort the list in descending order
            sortedEntries1.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            // Initialize the largest percentages of the 1st Check
            Map.Entry<Line, Double> biggestPercent1stCheck = null;
            Map.Entry<Line, Double> secondBiggestPercent1stCheck = null;

            if (!sortedEntries1.isEmpty()) {
                biggestPercent1stCheck = sortedEntries1.get(0); // largest percentage in the 1st check
                if (sortedEntries1.size() > 1) {
                    secondBiggestPercent1stCheck = sortedEntries1.get(1); // second largest percentage in the 1st check
                }
            }

            // Add the largest 2 percentages to the Top 4 Map
            percentagesTop4.put(biggestPercent1stCheck.getKey(), biggestPercent1stCheck.getValue());
            percentagesTop4.put(secondBiggestPercent1stCheck.getKey(), secondBiggestPercent1stCheck.getValue());

            // draw the 2 largest percentages of the check 1
            //biggestPercent1stCheck.getKey().draw(coloredBinaryImage);
            //secondBiggestPercent1stCheck.getKey().draw(coloredBinaryImage);

            // For 2nd check
            // create list with the map entries of the percentages
            List<Map.Entry<Line, Double>> sortedEntries2 = new ArrayList<>(percentages2ndCheck.entrySet());
            // Sort the list in descending order
            sortedEntries2.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            // Initialize the largest percentages of the 2nd Check
            Map.Entry<Line, Double> biggestPercent2ndCheck = null;
            Map.Entry<Line, Double> secondBiggestPercent2ndCheck = null;

            if (!sortedEntries2.isEmpty()) {
                biggestPercent2ndCheck = sortedEntries2.get(0); // largest percentage in the 2nd check
                if (sortedEntries2.size() > 1) {
                    secondBiggestPercent2ndCheck = sortedEntries2.get(1); // second largest percentage in the 2nd check
                }
            }

            // Add the largest 2 percentages to the Top 4 Map
            percentagesTop4.put(biggestPercent2ndCheck.getKey(), biggestPercent2ndCheck.getValue());
            percentagesTop4.put(secondBiggestPercent2ndCheck.getKey(), secondBiggestPercent2ndCheck.getValue());

            // draw the 2 largest percentages of the check 2
            //biggestPercent2ndCheck.getKey().draw(coloredBinaryImage);
            //secondBiggestPercent2ndCheck.getKey().draw(coloredBinaryImage);

            // create list with the map entries of the top 4 percentages
            List<Map.Entry<Line, Double>> top4List = new ArrayList<>(percentagesTop4.entrySet());
            // Sort the list in descending order
            top4List.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            // Initialize the largest percentages
            Map.Entry<Line, Double> largestPercentage = null;
            Map.Entry<Line, Double> secondLargestPercentage = null;

            if (Math.abs(biggestPercent2ndCheck.getValue() - biggestPercent1stCheck.getValue()) <= 9) {
                similarPercentages = true;
            }

            if (!percentagesTop4.isEmpty() && !similarPercentages) {
                largestPercentage = top4List.get(0); // largest percentage
                if (percentagesTop4.size() > 1) {
                    secondLargestPercentage = top4List.get(1); // second largest percentage
                }
            } else if (!percentagesTop4.isEmpty() && similarPercentages) {
                largestPercentage = biggestPercent1stCheck;
                secondLargestPercentage = biggestPercent2ndCheck;
            }

            // draw the 2 largest percentages of the rectangle
            //largestPercentage.getKey().draw(image);
            //secondLargestPercentage.getKey().draw(image);

            Point intersectionPoint = rectMiddlePoint;
            //intersectionPoint = largestPercentage.getKey().getIntersectionPoint(secondLargestPercentage.getKey());

            // Create the 2 candidates for Stem, by making lines between opposite corners of the 2 largestPercentage lines
            Point largestP1 = largestPercentage.getKey().getPt1();
            Point largestPercentageP1 = adjustPointToBounds(image, largestP1);

            Point largestP2 = largestPercentage.getKey().getPt2();
            Point largestPercentageP2 = adjustPointToBounds(image, largestP2);
            Point secondLargestP1 = secondLargestPercentage.getKey().getPt1();
            Point secondLargestPercentageP1 = adjustPointToBounds(image, secondLargestP1);

            Point secondLargestP2 = secondLargestPercentage.getKey().getPt2();
            Point secondLargestPercentageP2 = adjustPointToBounds(image, secondLargestP2);

            Line stemCandidate1 = null, stemCandidate2 = null;

            if (largestPercentageP1 != null && largestPercentageP2 != null && secondLargestPercentageP1 != null && secondLargestPercentageP2 != null) {
                stemCandidate1 = new Line(largestPercentageP1, secondLargestPercentageP2, new Scalar(255, 50, 50), 2);
                stemCandidate2 = new Line(secondLargestPercentageP1, largestPercentageP2, new Scalar(255, 50, 50), 2);
                if (stemCandidate1.getLength() < largestPercentage.getKey().getLength()) {
                    stemCandidate1 = new Line(largestPercentageP1, secondLargestPercentageP1, new Scalar(255, 50, 50), 2);
                    stemCandidate2 = new Line(secondLargestPercentageP2, largestPercentageP2, new Scalar(255, 50, 50), 2);
                }
            } else {
                System.out.println("Print");
            }

            // draw the 2 stem candidates
            //stemCandidate1.draw(image);
            //stemCandidate2.draw(image);

            // Create a line that unites the intersection point with the candidates - stem guideline
            Line stemGuideline = null;
            if (stemCandidate1.getPerpendicularIntersectionPoint(intersectionPoint) != null && stemCandidate2.getPerpendicularIntersectionPoint(intersectionPoint) != null) {
                stemGuideline = new Line(stemCandidate1.getPerpendicularIntersectionPoint(intersectionPoint), stemCandidate2.getPerpendicularIntersectionPoint(intersectionPoint), blue, 1);
            } else {
                System.out.println("Adjusted points");
                Point p1 = adjustPointToBounds(image, stemCandidate1.getPerpendicularIntersectionPoint(intersectionPoint));
                Point p2 = adjustPointToBounds(image, stemCandidate1.getPerpendicularIntersectionPoint(intersectionPoint));
                stemGuideline = new Line(p1, p2, blue, 1);

            }
            // draw it
            if (stemGuideline != null && stemGuideline.getPt1() != null && stemGuideline.getPt2() != null) {
                //stemGuideline.draw(image);
                //System.out.println("stemGuideline drawn");
            } else {
                System.out.println("stemGuideline not drawn");
            }

            Point stemMiddlePoint = stemGuideline.findMiddleBlackPixel(image);
            stem = stemGuideline.getStemLine(stemMiddlePoint);
            //stem.draw(image);
            System.out.println("Stem Found at 3rd check");
        }
        return stem;
    }

    // Method to calculate the percentage of pixels in a line between two points on a Mat image
    public double getUninterruptedBlackPixelPercentage(Mat image, Point point1, Point point2) {
        if (image.empty()) {
            throw new IllegalArgumentException("Image is empty");
        }

        //Imgproc.line(image, point1, point2, green, 1);

        // Convert points to integers (assuming the points are at pixel locations)
        int x0 = (int) point1.x;
        int y0 = (int) point1.y;
        int x1 = (int) point2.x;
        int y1 = (int) point2.y;

        int deltaX = Math.abs(x1 - x0);
        int deltaY = Math.abs(y1 - y0);
        int x, y, end;
        int xIncrement = (x0 < x1) ? 1 : -1;
        int yIncrement = (y0 < y1) ? 1 : -1;
        int err = deltaX - deltaY;
        int e2;

        int totalPixels = 0;
        int currentStreamLength = 0;
        int longestStreamLength = 0;

        x = x0;
        y = y0;
        end = deltaX > deltaY ? deltaX : deltaY; // Use the larger difference as the end condition

        for(int i = 0; i <= end; i++) {
            totalPixels++;

            if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                double[] pixel = image.get(y, x);
                if (pixel != null && pixel[0] == 0) { // Checking if the pixel is black
                    currentStreamLength++;
                    if (currentStreamLength > longestStreamLength) {
                        longestStreamLength = currentStreamLength; // Update the longest stream if current is longer
                    }
                } else {
                    currentStreamLength = 0; // Reset current stream length if pixel is not black
                }
            }

            if (x == x1 && y == y1) {
                break;
            }

            e2 = 2 * err;
            if (e2 > -deltaY) {
                err -= deltaY;
                x += xIncrement;
            }
            if (e2 < deltaX) {
                err += deltaX;
                y += yIncrement;
            }
        }

        if (totalPixels == 0) {
            return 0.0; // To avoid division by zero
        }

        // Calculate the percentage of the longest uninterrupted stream of black pixels
        return (double) longestStreamLength / totalPixels * 100.0;
    }

    public static Mat cloneAndCropImageWithPadding(Mat src, Rect rect, double angle) {
        // Clone the source image to preserve the original
        Mat clonedSrc = src.clone();

        // Crop the cloned image by the specified rectangle
        Mat croppedImage = clonedSrc.submat(rect).clone(); // Clone to detach from original image

        // Define the padding dynamically based on the dimensions of the cropped image
        // This padding1 is the padding before rotation, so that the image doesn't go out of bounds
        int padding1 = Math.max(croppedImage.width(), croppedImage.height()) / 3;

        // Calculate new size with padding
        int paddedWidth1 = croppedImage.width() + 2 * padding1;
        int paddedHeight1 = croppedImage.height() + 2 * padding1;

        // Create a new image with the padded size
        Mat paddedImage = new Mat(paddedHeight1, paddedWidth1, croppedImage.type(), new Scalar(255, 255, 255));

        // Determine the ROI within the padded image where the cropped image will be placed
        int roiX1 = padding1;
        int roiY1 = padding1;

        // Place the cropped image in the center of the padded image
        croppedImage.copyTo(paddedImage.submat(roiY1, roiY1 + croppedImage.height(), roiX1, roiX1 + croppedImage.width()));

        // Calculate the center of the original rectangle within the padded image
        Point center = new Point(roiX1 + croppedImage.width() / 2.0, roiY1 + croppedImage.height() / 2.0);

        // Get the rotation matrix for the specified angle around the rectangle's center in the padded image
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, -angle, 1.0);

        // Determine the size of the rotated image (could be the same as padded image size to keep everything)
        Size rotatedSize = new Size(paddedImage.width(), paddedImage.height());

        // Perform the rotation
        Mat rotatedImage = new Mat();
        Imgproc.warpAffine(paddedImage, rotatedImage, rotationMatrix, rotatedSize);

        /*
        // pad again - padding2
        int padding2 = 30;
        // Calculate new size with padding
        int paddedWidth2 = rotatedImage.width() + 2 * padding2;
        int paddedHeight2 = rotatedImage.height() + 2 * padding2;
        // Create a new image with the padded size
        Mat finalImage = new Mat(paddedHeight2, paddedWidth2, rotatedImage.type(), new Scalar(255, 255, 255));
        // Determine the ROI within the padded image where the cropped image will be placed
        int roiX2 = padding2;
        int roiY2 = padding2;
        // Place the cropped image in the center of the padded image
        rotatedImage.copyTo(finalImage.submat(roiY2, roiY2 + rotatedImage.height(), roiX2, roiX2 + rotatedImage.width()));

         */

        return rotatedImage;
    }

    private Point adjustPointToBounds(Mat image, Point point) {
        double x = point.x;
        double y = point.y;

        // Adjust x if it's out of bounds
        if (x < 0) x = 0;
        else if (x >= image.cols()) x = image.cols() - 1;

        // Adjust y if it's out of bounds
        if (y < 0) y = 0;
        else if (y >= image.rows()) y = image.rows() - 1;

        return new Point(x, y);
    }

// *******************************************************************************************************************
    private int processCipher(Mat image, Rect rect) {
        int quadrantWidth = rect.width / 2;
        int quadrantHeight = 4 * (rect.height / 10);
        int arabicResult = 0;
        Line rectMiddleLine = null, guidelineTop = null, guidelineBottom = null;

        // Get the middle line of the rectangle, so that the quadrants are more exact
        Point middle1 = new Point(rect.x + rect.width/2.0, rect.y);
        Point middle2 = new Point(rect.x + rect.width/2.0, rect.y + rect.height);
        Point middlePoint1 = adjustPointToBounds(image, middle1);
        Point middlePoint2 = adjustPointToBounds(image, middle2);
        if (middlePoint1 != null && middlePoint2 != null) {
            rectMiddleLine = new Line(middlePoint1, middlePoint2, new Scalar(0, 255, 0), 1);
        } else {
            System.out.println("Print");
        }

        //rectMiddleLine.draw(image);

        // Create guideline rect for top and bottom sub stem
        int guideRectWidth = rect.width / 6;

        // Guideline for top substem
        Point top1 = new Point(rect.x + rect.width/2.0 - guideRectWidth/2.0, rect.y + quadrantHeight/2.0);
        Point top2 = new Point(rect.x + rect.width/2.0 + guideRectWidth/2.0, rect.y + quadrantHeight/2.0);
        Point point1Top = adjustPointToBounds(image, top1);
        Point point2Top = adjustPointToBounds(image, top2);
        if (point1Top != null && point2Top != null) {
            guidelineTop = new Line(point1Top, point2Top, new Scalar(0, 255, 0), 1);
        } else {
            System.out.println("Print");
        }
        // this finds the exact middle point inside of the stem, so that the calculations of the quadrants is exact
        Point pointTop = guidelineTop.findMiddleBlackPixel(image);
        Point secondPoint = new Point(pointTop.x, rect.y);
        Line line = new Line(pointTop, secondPoint, pink, 1);
        //line.draw(image);
        int subStemXTop = (int) pointTop.x;


        // Guideline for bottom substem
        Point bottom1 = new Point(rect.x + rect.width/2.0 - guideRectWidth/2.0, rect.y + rect.height - quadrantHeight/2.0);
        Point bottom2 = new Point(rect.x + rect.width/2.0 + guideRectWidth/2.0, rect.y + rect.height - quadrantHeight/2.0);
        Point point1Bottom = adjustPointToBounds(image, bottom1);
        Point point2Bottom = adjustPointToBounds(image, bottom2);
        if (point1Bottom != null && point2Bottom != null) {
            guidelineBottom = new Line(point1Bottom, point2Bottom, new Scalar(0, 255, 0), 1);
        } else {
            System.out.println("Print");
        }

        Point pointBottom = guidelineBottom.findMiddleBlackPixel(image);
        int subStemXBottom = (int) pointBottom.x;




        Rect quadrantUnits = new Rect(subStemXTop, rect.y, quadrantWidth, quadrantHeight);
        //drawRectangle(image, quadrantUnits, green, 1);
        int unitsValue = findUnitsValue(image, quadrantUnits);

        Rect quadrantTens = new Rect(rect.x, rect.y, subStemXTop - rect.x, quadrantHeight);
        //drawRectangle(image, quadrantTens, green, 1);
        int tensValue = findTensValue(image, quadrantTens);

        Rect quadrantHundreds = new Rect(subStemXBottom, rect.y + rect.height - quadrantHeight, quadrantWidth, quadrantHeight);
        //drawRectangle(image, quadrantHundreds, green, 1);
        int hundredsValue = findHundredsValue(image, quadrantHundreds);

        Rect quadrantThousands = new Rect(rect.x, rect.y + rect.height - quadrantHeight, subStemXTop - rect.x, quadrantHeight);
        //drawRectangle(image, quadrantThousands, green, 1);
        int thousandsValue = findThousandsValue(image, quadrantThousands);

        arabicResult = thousandsValue + hundredsValue + tensValue + unitsValue;
        System.out.println("ARABIC RESULT IS " + arabicResult);
        System.out.println(thousandsValue + " + " + hundredsValue + " + " + tensValue + " + " + unitsValue + " = " + arabicResult);

        //arabicResults.add(arabicResult);
        //updateResultsDisplay(); // Refresh the display with the new list of

        return arabicResult;
    }

// *******************************************************************************************************************

    private int findUnitsValue(Mat image, Rect rect) {
        List<Rect> subQuadrantsUnits = new ArrayList<>();

        int unitsResultBySubQuadrants = 0;
        int unitsResultBySegments = 0;

        int[] limitsLeftRight = getLimitsLeftRight(image, rect, true);
        int[] limitsTopBottom = getLimitsTopBottom(image, rect, limitsLeftRight[0], limitsLeftRight[1]);

        int leftLimitX = limitsLeftRight[0];
        int rightLimitX = limitsLeftRight[1];
        int topLimitY = limitsTopBottom[0];
        int bottomLimitY = limitsTopBottom[1];

        if (leftLimitX != -1 && rightLimitX != -1 && bottomLimitY != -1 && topLimitY != -1) {
            // Get the result from the subquadrants
            int subQuadrantHeight = (bottomLimitY - topLimitY) / 3;
            int subQuadrantWidth = (rightLimitX - leftLimitX) / 3;

            Rect subQuadrantUnits1 = new Rect(leftLimitX, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits2 = new Rect(leftLimitX + subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits3 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits4 = new Rect(leftLimitX, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits5 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits6 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits7 = new Rect(leftLimitX, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits8 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits9 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);

            subQuadrantsUnits.addAll(Arrays.asList(subQuadrantUnits1, subQuadrantUnits2, subQuadrantUnits3, subQuadrantUnits4, subQuadrantUnits5, subQuadrantUnits6, subQuadrantUnits7, subQuadrantUnits8, subQuadrantUnits9));

            unitsResultBySubQuadrants = detectValidSubQuadrants(image, subQuadrantsUnits);
            //drawSubQuadrants(image, subQuadrantsUnits);
            
            // Get the result from the segments
            Point point1 = new Point(leftLimitX + subQuadrantWidth/4.0, topLimitY + subQuadrantHeight/4.0);
            Point point2 = new Point(rightLimitX - subQuadrantWidth/4.0, topLimitY + subQuadrantHeight/4.0);
            Point point3 = new Point(leftLimitX + subQuadrantWidth/4.0, bottomLimitY - subQuadrantHeight/4.0);
            Point point4 = new Point(rightLimitX - subQuadrantWidth/4.0, bottomLimitY - subQuadrantHeight/4.0);

            List<Line> segmentsUnits = getSegmentsFromPoints(point1, point2, point3, point4);

            unitsResultBySegments = detectValidSegments(image, segmentsUnits);
            //drawSegments(image, segmentsUnits);
            
            boolean sameResult = unitsResultBySegments == unitsResultBySubQuadrants;
            System.out.println("Digit Units (" + sameResult + ") - Segments: " + unitsResultBySegments + ", Rects: " + unitsResultBySubQuadrants);
        }
        return unitsResultBySegments;
    }

    private int findTensValue(Mat image, Rect rect) {
        List<Rect> subQuadrantsTens = new ArrayList<>();

        int tensResultBySubQuadrants = 0;
        int tensResultBySegments = 0;

        int[] limitsLeftRight = getLimitsLeftRight(image, rect, false);
        int[] limitsTopBottom = getLimitsTopBottom(image, rect, limitsLeftRight[0], limitsLeftRight[1]);

        int leftLimitX = limitsLeftRight[0];
        int rightLimitX = limitsLeftRight[1];
        int topLimitY = limitsTopBottom[0];
        int bottomLimitY = limitsTopBottom[1];


        if (rightLimitX != -1 && leftLimitX != -1 && bottomLimitY != -1 && topLimitY != -1) {
            // Get the result from the subquadrants
            int subQuadrantHeight = (bottomLimitY - topLimitY) / 3;
            int subQuadrantWidth = (rightLimitX - leftLimitX) / 3;

            Rect subQuadrantTens1 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens2 = new Rect(leftLimitX + subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens3 = new Rect(leftLimitX, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens4 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens5 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens6 = new Rect(leftLimitX, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens7 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens8 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens9 = new Rect(leftLimitX, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);

            subQuadrantsTens.addAll(Arrays.asList(subQuadrantTens1, subQuadrantTens2, subQuadrantTens3, subQuadrantTens4, subQuadrantTens5, subQuadrantTens6, subQuadrantTens7, subQuadrantTens8, subQuadrantTens9));
            
            tensResultBySubQuadrants = detectValidSubQuadrants(image, subQuadrantsTens);
            //drawSubQuadrants(image, subQuadrantsTens);
            
            // Get the result from the segments
            Point point1 = new Point(rightLimitX - subQuadrantWidth/4.0, topLimitY + subQuadrantHeight/4.0);
            Point point2 = new Point(leftLimitX + subQuadrantWidth/4.0, topLimitY + subQuadrantHeight/4.0);
            Point point3 = new Point(rightLimitX - subQuadrantWidth/4.0, bottomLimitY - subQuadrantHeight/4.0);
            Point point4 = new Point(leftLimitX + subQuadrantWidth/4.0, bottomLimitY - subQuadrantHeight/4.0);

            List<Line> segmentsTens = getSegmentsFromPoints(point1, point2, point3, point4);

            tensResultBySegments = detectValidSegments(image, segmentsTens);
            //drawSegments(image, segmentsTens);

            boolean sameResult = tensResultBySegments == tensResultBySubQuadrants;
            System.out.println("Digit Tens (" + sameResult + ") - Segments: " + tensResultBySegments + ", Rects: " + tensResultBySubQuadrants);
        }
        return tensResultBySegments * 10;
    }

    private int findHundredsValue(Mat image, Rect rect) {
        List<Rect> subQuadrantsHundreds = new ArrayList<>();

        int hundredsResultBySubQuadrants = 0;
        int hundredsResultBySegments = 0;

        int[] limitsLeftRight = getLimitsLeftRight(image, rect, true);
        int[] limitsTopBottom = getLimitsTopBottom(image, rect, limitsLeftRight[0], limitsLeftRight[1]);

        int leftLimitX = limitsLeftRight[0];
        int rightLimitX = limitsLeftRight[1];
        int topLimitY = limitsTopBottom[0];
        int bottomLimitY = limitsTopBottom[1];

        if (leftLimitX != -1 && rightLimitX != -1 && topLimitY != -1 && bottomLimitY != -1) {
            // Get the result from the subquadrants
            int subQuadrantHeight = (bottomLimitY - topLimitY) / 3;
            int subQuadrantWidth = (rightLimitX - leftLimitX) / 3;

            Rect subQuadrantHundreds1 = new Rect(leftLimitX, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds2 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds3 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds4 = new Rect(leftLimitX, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds5 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds6 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds7 = new Rect(leftLimitX, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds8 = new Rect(leftLimitX + subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds9 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);

            subQuadrantsHundreds.addAll(Arrays.asList(subQuadrantHundreds1, subQuadrantHundreds2, subQuadrantHundreds3, subQuadrantHundreds4, subQuadrantHundreds5, subQuadrantHundreds6, subQuadrantHundreds7, subQuadrantHundreds8, subQuadrantHundreds9));

            hundredsResultBySubQuadrants = detectValidSubQuadrants(image, subQuadrantsHundreds);
            //drawSubQuadrants(image, subQuadrantsHundreds);

            // Get the result from the segments
            Point point1 = new Point(leftLimitX + subQuadrantWidth/4.0, bottomLimitY - subQuadrantHeight/4.0);
            Point point2 = new Point(rightLimitX - subQuadrantWidth/4.0, bottomLimitY - subQuadrantHeight/4.0);
            Point point3 = new Point(leftLimitX + subQuadrantWidth/4.0, topLimitY + subQuadrantHeight/4.0);
            Point point4 = new Point(rightLimitX - subQuadrantWidth/4.0, topLimitY + subQuadrantHeight/4.0);

            List<Line> segmentsHundreds = getSegmentsFromPoints(point1, point2, point3, point4);

            hundredsResultBySegments = detectValidSegments(image, segmentsHundreds);
            //drawSegments(image, segmentsHundreds);

            boolean sameResult = hundredsResultBySegments == hundredsResultBySubQuadrants;
            System.out.println("Digit Hundreds (" + sameResult + ") - Segments: " + hundredsResultBySegments + ", Rects: " + hundredsResultBySubQuadrants);
        }
        return hundredsResultBySegments * 100;
    }

    private int findThousandsValue(Mat image, Rect rect) {
        List<Rect> subQuadrantsThousands = new ArrayList<>();

        int thousandsResultBySubQuadrants = 0;
        int thousandsResultBySegments = 0;

        int[] limitsLeftRight = getLimitsLeftRight(image, rect, false);
        int[] limitsTopBottom = getLimitsTopBottom(image, rect, limitsLeftRight[0], limitsLeftRight[1]);

        int leftLimitX = limitsLeftRight[0];
        int rightLimitX = limitsLeftRight[1];
        int topLimitY = limitsTopBottom[0];
        int bottomLimitY = limitsTopBottom[1];

        if (rightLimitX != -1 && leftLimitX != -1 && topLimitY != -1 && bottomLimitY != -1) {
            // Get the result from the subquadrants
            int subQuadrantHeight = (bottomLimitY - topLimitY) / 3;
            int subQuadrantWidth = (rightLimitX - leftLimitX) / 3;

            Rect subQuadrantThousands1 = new Rect(leftLimitX + 2*subQuadrantWidth, topLimitY + 2*subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands2 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + 2*subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands3 = new Rect(leftLimitX, topLimitY + 2*subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands4 = new Rect(leftLimitX + 2*subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands5 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands6 = new Rect(leftLimitX, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands7 = new Rect(leftLimitX + 2*subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands8 = new Rect(leftLimitX + subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands9 = new Rect(leftLimitX, topLimitY, subQuadrantWidth, subQuadrantHeight);

            subQuadrantsThousands.addAll(Arrays.asList(subQuadrantThousands1, subQuadrantThousands2, subQuadrantThousands3, subQuadrantThousands4, subQuadrantThousands5, subQuadrantThousands6, subQuadrantThousands7, subQuadrantThousands8, subQuadrantThousands9));

            thousandsResultBySubQuadrants = detectValidSubQuadrants(image, subQuadrantsThousands);
            //drawSubQuadrants(image, subQuadrantsThousands);
            
            // Get the result from the segments
            Point point1 = new Point(rightLimitX - subQuadrantWidth/4.0, bottomLimitY - subQuadrantHeight/4.0);
            Point point2 = new Point(leftLimitX + subQuadrantWidth/4.0, bottomLimitY - subQuadrantHeight/4.0);
            Point point3 = new Point(rightLimitX - subQuadrantWidth/4.0, topLimitY + subQuadrantHeight/4.0);
            Point point4 = new Point(leftLimitX + subQuadrantWidth/4.0, topLimitY + subQuadrantHeight/4.0);

            List<Line> segmentsThousands = getSegmentsFromPoints(point1, point2, point3, point4);

            thousandsResultBySegments = detectValidSegments(image, segmentsThousands);
            //drawSegments(image, segmentsThousands);

            boolean sameResult = thousandsResultBySegments == thousandsResultBySubQuadrants;
            System.out.println("Digit Thousands (" + sameResult + ") - Segments: " + thousandsResultBySegments + ", Rects: " + thousandsResultBySubQuadrants);
        }
        return thousandsResultBySegments * 1000;
    }

// *******************************************************************************************************************

    private int[] getLimitsLeftRight(Mat image, Rect rect, boolean is_stem_on_left_side) {
        boolean firstLineDrawn = false;
        boolean pixel1Found = false, pixel2Found = false;
        int leftLimitX = -1, rightLimitX = -1;
        Rect guideline1Rect = null, guideline2Rect = null;

        if (is_stem_on_left_side) { // stem is on the left side (Quadrants of the units and hundreds)
            // Guideline Rectangle 1, to find leftLimitX
            int guideline1Width = rect.width / 2;
            int guideline1Height = rect.height / 15;
            guideline1Rect = new Rect(rect.x + (rect.width / 15), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
            //drawRectangle(image, guideline1Rect, orange, 1);

            if (guideline1Rect != null) {
                for (int x = guideline1Rect.x; x < guideline1Rect.x + guideline1Rect.width; x++) {
                    for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Rect.height; y++) {
                        if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                            double[] pixel = image.get(y, x);
                            if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                                pixel1Found = true;
                                leftLimitX = x + (guideline1Rect.width / 20);
                                Point lineStart = new Point(leftLimitX, rect.y);
                                Point lineEnd = new Point(leftLimitX, rect.y + rect.height);
                                Line leftLimit = new Line(lineStart, lineEnd, pink, 1);
                                //leftLimit.draw(image); // draw the line
                                firstLineDrawn = true;
                                break;
                            }
                        }
                    }
                    if (firstLineDrawn) break; // Exit the outer loop as well after drawing the 1st line
                }
                if (!pixel1Found) {
                    leftLimitX = rect.x + (rect.width / 15);
                    Point lineStart = new Point(leftLimitX, rect.y);
                    Point lineEnd = new Point(leftLimitX, rect.y + rect.height);
                    Line leftLimit = new Line(lineStart, lineEnd, pink, 1);
                    //leftLimit.draw(image); // draw the line
                }
            }

            // Guideline Rectangle 2, to find rightLimitX
            int guideline2Width = rect.width / 2;
            int guideline2Height = rect.height;
            guideline2Rect = new Rect(rect.x + (2 * (rect.width / 3)) + rect.width/10, rect.y, guideline2Width, guideline2Height);
            //drawRectangle(image, guideline2Rect, orange, 1);

            if (guideline2Rect != null) {
                for (int x = guideline2Rect.x + guideline2Rect.width; x > guideline2Rect.x; x--) {
                    for (int y = guideline2Rect.y; y < guideline2Rect.y + guideline2Rect.height; y++) {
                        if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                            double[] pixel = image.get(y, x);
                            if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                                pixel2Found = true;
                                rightLimitX = x + guideline2Rect.width / 10;
                                Point lineStart = new Point(rightLimitX, rect.y);
                                Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                                Line rightLimit = new Line(lineStart, lineEnd, pink, 1);
                                //rightLimit.draw(image); // draw the line
                                break;
                            }
                        }
                    }
                    if (rightLimitX != -1)
                        break; // Exit the outer loop as well after drawing 2nd line
                }
                if (!pixel2Found) {
                    rightLimitX = rect.x + rect.width;
                    Point lineStart = new Point(rightLimitX, rect.y);
                    Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                    Line rightLimit = new Line(lineStart, lineEnd, pink, 1);
                    //rightLimit.draw(image); // draw the line
                }
            }
        } else { // stem is on the right side (Quadrants of the tens and tousands)
            // Guideline Rectangle 1, to find rightLimitX
            int guideline1Width = rect.width / 2;
            int guideline1Height = rect.height / 15;
            guideline1Rect = new Rect(rect.x + (rect.width/2) - (rect.width/25), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
            //drawRectangle(image, guideline1Rect, orange, 1);

            if (guideline1Rect != null) {
                for (int x = guideline1Rect.x + guideline1Width; x > guideline1Rect.x; x--) {
                    for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Height; y++) {
                        if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                            double[] pixel = image.get(y, x);
                            if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                                pixel1Found = true;
                                rightLimitX = x - (guideline1Rect.width / 25);
                                Point lineStart = new Point(rightLimitX, rect.y);
                                Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                                Line rightLimit = new Line(lineStart, lineEnd, pink, 1);
                                //rightLimit.draw(image); // draw the line
                                firstLineDrawn = true;
                                break;
                            }
                        }
                    }
                    if (firstLineDrawn) break; // Exit the outer loop as well after drawing first line
                }
                if (!pixel1Found) {
                    rightLimitX = rect.x + rect.width - (rect.width/15);
                    Point lineStart = new Point(rightLimitX, rect.y);
                    Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                    Line rightLimit = new Line(lineStart, lineEnd, pink, 1);
                    //rightLimit.draw(image); // draw the line
                }
            }


            // Guideline Rectangle 2, to find leftLimitX
            int guideline2Width = rect.width / 2;
            int guideline2Height = rect.height;
            guideline2Rect = new Rect(rect.x - rect.width/10, rect.y, guideline2Width, guideline2Height);
            //drawRectangle(image, guideline2Rect, orange, 1);

            if (guideline2Rect != null) {
                for (int x = guideline2Rect.x; x < guideline2Rect.x + guideline2Width; x++) {
                    for (int y = guideline2Rect.y; y < guideline2Rect.y + guideline2Height; y++) {
                        if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                            double[] pixel = image.get(y, x);
                            if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                                pixel2Found = true;
                                leftLimitX = x;
                                Point lineStart = new Point(leftLimitX, rect.y);
                                Point lineEnd = new Point(leftLimitX, rect.y + rect.height);
                                Line leftLimit = new Line(lineStart, lineEnd, pink, 1);
                                //leftLimit.draw(image); // draw the line
                                break;
                            }
                        }
                    }
                    if (leftLimitX != -1)
                        break; // Exit the outer loop as well after drawing second line
                }
                if (!pixel2Found) {
                    leftLimitX = guideline2Rect.x;
                    Point lineStart = new Point(leftLimitX, guideline2Rect.y);
                    Point lineEnd = new Point(leftLimitX, guideline2Rect.y + guideline2Height);
                    Line leftLimit = new Line(lineStart, lineEnd, pink, 1);
                    //leftLimit.draw(image); // draw the line
                }
            }
        }
        return new int[]{leftLimitX, rightLimitX};
    }

    private int[] getLimitsTopBottom(Mat image, Rect rect, int leftLimitX, int rightLimitX) {
        boolean pixel3Found = false, pixel4Found = false;
        int topLimitY = -1, bottomLimitY = -1;
        Rect guideline3Rect = null, guideline4Rect = null;
        int offsetFromStem = rect.width / 15;

        // Guideline Rectangle 3, to find topLimitX
        int guideline3Width = rightLimitX - leftLimitX - 2 * offsetFromStem;
        int guideline3Height = rect.height / 2;
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline3Rect = new Rect(leftLimitX + offsetFromStem, rect.y - rect.height/20, guideline3Width, guideline3Height);
            //drawRectangle(image, guideline3Rect, orange, 1);
        }

        if (guideline3Rect != null) {
            for (int y = guideline3Rect.y; y < guideline3Rect.y + guideline3Height; y++) {
                for (int x = guideline3Rect.x; x < guideline3Rect.x + guideline3Width; x++) {
                    if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                        double[] pixel = image.get(y, x);
                        if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                            pixel3Found = true;
                            topLimitY = y;
                            Point lineStart = new Point(guideline3Rect.x, topLimitY);
                            Point lineEnd = new Point(guideline3Rect.x + guideline3Width, topLimitY);
                            Line topLimit = new Line(lineStart, lineEnd, pink, 1);
                            //topLimit.draw(image); // draw the line
                            break;
                        }
                    }
                }
                if (topLimitY != -1) break; // Exit the outer loop as well after drawing the 3rd line
            }
            if (!pixel3Found) {
                topLimitY = guideline3Rect.y;
                Point lineStart = new Point(guideline3Rect.x, topLimitY);
                Point lineEnd = new Point(guideline3Rect.x + guideline3Width, topLimitY);
                Line topLimit = new Line(lineStart, lineEnd, pink, 1);
                //topLimit.draw(image); // draw the line
            }
        }

        // Guideline Rectangle 4, to find bottomLimitX
        int guideline4Width = rightLimitX - leftLimitX - 2 * offsetFromStem;
        int guideline4Height = 10 * (rect.height / 20);
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline4Rect = new Rect(leftLimitX + offsetFromStem, rect.y + rect.height - guideline4Height + rect.height/20, guideline4Width, guideline4Height);
            //drawRectangle(image, guideline4Rect, orange, 1);
        }

        if (guideline4Rect != null) {
            for (int y = guideline4Rect.y + guideline4Height; y > guideline4Rect.y; y--) {
                for (int x = guideline4Rect.x; x < guideline4Rect.x + guideline4Width; x++) {
                    if (x >= 0 && x < image.width() && y >= 0 && y < image.height()) {
                        double[] pixel = image.get(y, x);
                        if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                            pixel4Found = true;
                            bottomLimitY = y;
                            Point lineStart = new Point(guideline4Rect.x, bottomLimitY);
                            Point lineEnd = new Point(guideline4Rect.x + guideline4Width, bottomLimitY);
                            Line bottomLimit = new Line(lineStart, lineEnd, pink, 1);
                            //bottomLimit.draw(image); // draw the line
                            break;
                        }
                    }
                }
                if (bottomLimitY != -1)
                    break; // Exit the outer loop as well after drawing the 4th line
            }
            if (!pixel4Found) {
                bottomLimitY = guideline4Rect.y + guideline3Height - guideline3Height/5;
                Point lineStart = new Point(guideline4Rect.x, bottomLimitY);
                Point lineEnd = new Point(guideline4Rect.x + guideline4Width, bottomLimitY);
                Line bottomLimit = new Line(lineStart, lineEnd, pink, 1);
                //bottomLimit.draw(image); // draw the line
            }
        }

        return new int[]{topLimitY, bottomLimitY};
    }

    public List<Line> getSegmentsFromPoints(Point p1, Point p2, Point p3, Point p4) {
        Line segment1 = new Line(p1, p2, blue, 1);
        Line segment2 = new Line(p3, p4, blue, 1);
        Line segment3 = new Line(p1, p4, blue, 1);
        Line segment4 = new Line(p3, p2, blue, 1);
        Line segment5 = new Line(p2, p4, blue, 1);

        return Arrays.asList(segment1, segment2, segment3, segment4, segment5);
    }

    private void drawSubQuadrants(Mat image, List<Rect> subQuadrants) {
        for(Rect subQuadrant : subQuadrants) {
            drawRectangle(image, subQuadrant, red, 1);
        }
    }

    private void drawSegments(Mat image, List<Line> lines) {
        for(Line line : lines) {
            line.draw(image);
        }
    }

    private void drawRectangle(Mat image, Rect rect, Scalar color, int thickness) {
        Imgproc.rectangle(image, rect.tl(), rect.br(), color, thickness);
    }

    private Integer detectValidSegments(Mat image, List<Line> segments) {
        Set<Integer> flaggedSegments = new HashSet<>(); // Track flagged segments

        // Initially flagging subQuadrants within 10% of the guide value
        for (int i = 0; i < segments.size(); i++) {
            Line segment = segments.get(i);
            //System.out.println("Segment " + (i + 1) + " has percentage: " + segment.getBlackPixelPercentage(image));
            if (segment.getBlackPixelPercentage(image) > 50.0) {
                flaggedSegments.add(i + 1);
                System.out.println("Seg. " + (i + 1) + " FLAGGED (" + segment.getBlackPixelPercentage(image) +  "%)");
            }
//            else if (segment.getBlackPixelPercentage(image) > 30.0 && segment.getBlackPixelPercentage(image) <= 50) {
//                Line rearrangedSegment = segment.rearrangeLine(image);
//                rearrangedSegment.draw(image);
//                if (rearrangedSegment.getBlackPixelPercentage(image) > 50.0) {
//                    flaggedSegments.add(i + 1);
//                    System.out.println("Seg. " + (i + 1) + " rearranged & FLAGGED (%: " + rearrangedSegment.getBlackPixelPercentage(image) +  "), old %: " + segment.getBlackPixelPercentage(image));
//                } else {
//                    System.out.println("Seg. " + (i + 1) + " rearranged & NOT flagged (%: " + rearrangedSegment.getBlackPixelPercentage(image) +  "), old %: " + segment.getBlackPixelPercentage(image));
//                }
//            }
            else {
                System.out.println("Seg. " + (i + 1) + " NOT flagged (" + segment.getBlackPixelPercentage(image) +  "%)");
            }
        }

        int detectedNumber = mapFlaggedSegmentsToNumber(flaggedSegments);
        //System.out.println("Detected Number TESTING UNITS: " + detectedNumber);

        return detectedNumber;
    }

    private Integer detectValidSubQuadrants(Mat image, List<Rect> subQuadrants) {
        List<Double> percentages = new ArrayList<>();
        Set<Integer> initiallyFlagged = new HashSet<>(); // Track initially flagged subQuadrants

        for(Rect subQuadrant : subQuadrants) {
            double percentage = calculateBlackPixelPercentage(image, subQuadrant);
            percentages.add(percentage);
        }
        // Finding the guide value
        double guideValue = Collections.max(percentages);

        // Check if guideValue is below the minimum threshold
        if (guideValue < 7) {
            System.out.println("All subquadrants are empty or almost empty, skipping flagging.");
            return 0;
        }

        // Initially flagging subQuadrants within 10% of the guide value
        for (int i = 0; i < subQuadrants.size(); i++) {
            Rect subQuadrant = subQuadrants.get(i);
            double percentage = percentages.get(i);
            // Check if the percentage is within 10% of the guide value
            if (Math.abs(percentage - guideValue) <= 30) {
                //drawRectangle(image, subQuadrant, green, 1); // Using green for highlighting
                initiallyFlagged.add(i + 1);
            }
        }

        // Additional logic to flag based on certain rules
        Set<Integer> toCheck = new HashSet<>();
        if (initiallyFlagged.size() == 1 && initiallyFlagged.contains(1)) {
            toCheck.addAll(Arrays.asList(2, 3, 5, 9));}
        if (initiallyFlagged.size() == 1 && initiallyFlagged.contains(3)) {
            toCheck.addAll(Arrays.asList(1, 2, 5, 6, 7, 9));}
        if (initiallyFlagged.size() == 1 && initiallyFlagged.contains(7)) {
            toCheck.addAll(Arrays.asList(3, 5));}
        if (initiallyFlagged.size() == 1 && initiallyFlagged.contains(9)) {
            toCheck.addAll(Arrays.asList(1, 3, 5, 6, 7, 8));}
        if (initiallyFlagged.size() == 2 && initiallyFlagged.containsAll(Arrays.asList(1, 5))) {
            toCheck.add(9);}
        if (initiallyFlagged.size() == 2 && initiallyFlagged.containsAll(Arrays.asList(1, 6))) {
            toCheck.add(9);}
        if (initiallyFlagged.size() == 2 && initiallyFlagged.containsAll(Arrays.asList(3, 9))) {
            toCheck.addAll(Arrays.asList(1, 2, 6, 7, 8));}
        if (initiallyFlagged.size() == 2 && initiallyFlagged.containsAll(Arrays.asList(5, 7))) {
            toCheck.add(3);}
        if (initiallyFlagged.size() == 2 && initiallyFlagged.containsAll(Arrays.asList(6, 9))) {
            toCheck.add(1);}
        
        // Check and flag additional subquadrants based on the rule
        for (Integer index : toCheck) {
            int i = index - 1; // Adjusting back to 0-based indexing
            double percentage = percentages.get(i);
            if (percentage >= guideValue * 0.20) {
                initiallyFlagged.add(index); // Flagging based on the rule
            }
        }

        // Final flagging including both initially and additionally flagged subquadrants
        for (Integer index : initiallyFlagged) {
            int i = index - 1; // Adjusting back to 0-based indexing
            Rect subQuadrant = subQuadrants.get(i);
            // Flagging the subquadrant
            //drawRectangle(image, subQuadrant, green, 1);
        }

        // Interpretation of flagged subquadrants to number
        int detectedNumber = mapFlaggedSubQuadrantsToNumber(initiallyFlagged);
        //System.out.println("Detected Number: " + detectedNumber);

        return detectedNumber;
    }

    private int mapFlaggedSegmentsToNumber(Set<Integer> flaggedSegments) {
        Set<Integer> patternForNumber1 = new HashSet<>(Collections.singletonList(1));
        Set<Integer> patternForNumber2 = new HashSet<>(Collections.singletonList(2));
        Set<Integer> patternForNumber3 = new HashSet<>(Collections.singletonList(3));
        Set<Integer> patternForNumber4 = new HashSet<>(Collections.singletonList(4));
        Set<Integer> patternForNumber5 = new HashSet<>(Arrays.asList(1, 4));
        Set<Integer> patternForNumber6 = new HashSet<>(Collections.singletonList(5));
        Set<Integer> patternForNumber7 = new HashSet<>(Arrays.asList(1, 5));
        Set<Integer> patternForNumber8 = new HashSet<>(Arrays.asList(2, 5));
        Set<Integer> patternForNumber9a = new HashSet<>(Arrays.asList(1, 2, 5));
        Set<Integer> patternForNumber9b = new HashSet<>(Arrays.asList(1, 2));

        if (flaggedSegments.size() <= 3) {
            if (flaggedSegments.equals(patternForNumber9a)) {
                return 9; // Matches pattern for Number 9
            } else if (flaggedSegments.size() <= 2) {
                if (flaggedSegments.equals(patternForNumber5)) {
                    return 5; // Matches pattern for Number 5
                } else if (flaggedSegments.equals(patternForNumber7)) {
                    return 7; // Matches pattern for Number 7
                } else if (flaggedSegments.equals(patternForNumber8)) {
                    return 8; // Matches pattern for Number 8
                } else if (flaggedSegments.equals(patternForNumber9b)) {
                    return 9; // Matches pattern for Number 9
                } else if (flaggedSegments.size() <= 1) {
                    if (flaggedSegments.equals(patternForNumber1)) {
                        return 1; // Matches pattern for Number 1
                    } else if (flaggedSegments.equals(patternForNumber2)) {
                        return 2; // Matches pattern for Number 2
                    } else if (flaggedSegments.equals(patternForNumber3)) {
                        return 3; // Matches pattern for Number 3
                    } else if (flaggedSegments.equals(patternForNumber4)) {
                        return 4; // Matches pattern for Number 4
                    } else if (flaggedSegments.equals(patternForNumber6)) {
                        return 6; // Matches pattern for Number 6
                    } else if (flaggedSegments.size() == 0) {
                        return 0;
                    }
                }
            }
        }
        return -1;
    }

    private int mapFlaggedSubQuadrantsToNumber(Set<Integer> flaggedSubQuadrants) {
        // Define patterns corresponding to numbers
        Set<Integer> patternForNumber1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> patternForNumber2 = new HashSet<>(Arrays.asList(7, 8, 9));
        Set<Integer> patternForNumber3 = new HashSet<>(Arrays.asList(1, 5, 9));
        Set<Integer> patternForNumber4 = new HashSet<>(Arrays.asList(3, 5, 7));
        Set<Integer> patternForNumber5 = new HashSet<>(Arrays.asList(1, 2, 3, 5, 7));
        Set<Integer> patternForNumber6 = new HashSet<>(Arrays.asList(3, 6, 9));
        Set<Integer> patternForNumber7 = new HashSet<>(Arrays.asList(1, 2, 3, 6, 9));
        Set<Integer> patternForNumber8 = new HashSet<>(Arrays.asList(3, 6, 7, 8, 9));
        Set<Integer> patternForNumber9 = new HashSet<>(Arrays.asList(1, 2, 3, 6, 7, 8, 9));

        if (flaggedSubQuadrants.equals(patternForNumber1)) { return 1; // Matches pattern for Number 1
        } else if (flaggedSubQuadrants.equals(patternForNumber2)) { return 2; // Matches pattern for Number 2
        } else if (flaggedSubQuadrants.equals(patternForNumber3)) { return 3; // Matches pattern for Number 3
        } else if (flaggedSubQuadrants.equals(patternForNumber4)) { return 4; // Matches pattern for Number 4
        } else if (flaggedSubQuadrants.equals(patternForNumber5)) { return 5; // Matches pattern for Number 5
        } else if (flaggedSubQuadrants.equals(patternForNumber6)) { return 6; // Matches pattern for Number 6
        } else if (flaggedSubQuadrants.equals(patternForNumber7)) { return 7; // Matches pattern for Number 7
        } else if (flaggedSubQuadrants.equals(patternForNumber8)) { return 8; // Matches pattern for Number 8
        } else if (flaggedSubQuadrants.equals(patternForNumber9)) { return 9; // Matches pattern for Number 9
        }
        return 0; // No pattern matches - it's 0
    }

    private double calculateBlackPixelPercentage(Mat coloredBinaryImage, Rect subQuadrant) {
        int blackPixelCount = 0;
        int totalPixels = subQuadrant.width * subQuadrant.height;

        // iterate through each pixel
        for (int y = subQuadrant.y; y < (subQuadrant.y + subQuadrant.height); y++) {
            for (int x = subQuadrant.x; x < (subQuadrant.x + subQuadrant.width); x++) {
                if (x >= 0 && x < coloredBinaryImage.width() && y >= 0 && y < coloredBinaryImage.height()) {
                    double[] pixel = coloredBinaryImage.get(y, x); //
                    // check if pixel is black
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        blackPixelCount++;
                    }
                }

            }
        }

        double blackPixelPercentage = (double) blackPixelCount / totalPixels * 100;
        // label percentage of black pixels on the image near the subQuadrant
        String label = String.format("%.2f%%", blackPixelPercentage); // Formats the percentage to 2 decimal places
        Point labelPoint = new Point(subQuadrant.x + 5, subQuadrant.y + 20);
        //Imgproc.putText(coloredBinaryImage, label, labelPoint, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, blue, 1);

        return (double) blackPixelCount / totalPixels * 100;
    }

//CHECK
//    private void updateResultsDisplay() {
//        TextView resultsView = findViewById(R.id.results);
//        StringBuilder resultsText = new StringBuilder("Results:\n");
//        for (int result : arabicResults) {
//            if(arabicResults.size() > 1) {
//                resultsText.append(result).append(", ");
//            }
//            else {
//                resultsText.append(result).append("");
//            }
//        }
//        resultsView.setText(resultsText.toString());
//    }

    private void displayResultsOnImageOverlay(Mat imageMat, List<Rect> cipherRects, List<Integer> results) {
        FrameLayout layout = findViewById(R.id.imageOverlayLayout);

        // Assuming ImageView is the first child and clear previous TextViews
        layout.removeViews(1, layout.getChildCount() - 1);

        for (int i = 0; i < cipherRects.size(); i++) {
            final Rect rect = cipherRects.get(i);
            int result = results.get(i);

            final TextView resultView = new TextView(this);
            resultView.setText(String.valueOf(result));
            resultView.setTextColor(Color.RED); // Text color
            resultView.setTextSize(20); // Text size
            resultView.setGravity(Gravity.CENTER);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);

            // Calculate center position of the TextView
            // Assume getImageScale() correctly adjusts for any scaling applied by the ImageView
            float scale = getImageScale(imageMat, layout);
            int centerX = (int) ((rect.x + rect.width / 2.0f) * scale);
            int centerY = (int) ((rect.y + rect.height / 2.0f) * scale);

            // Initially, place TextView in the approximate center
            params.leftMargin = centerX;
            params.topMargin = centerY;

            layout.addView(resultView, params);

            // Dynamically adjust TextView position after it's been added to the layout
            resultView.post(() -> {
                // Adjust params based on actual TextView size
                params.leftMargin = centerX - resultView.getWidth() / 2;
                params.topMargin = centerY - resultView.getHeight() / 2;
                resultView.setLayoutParams(params);
            });
        }
    }

    /**
     * Calculate scaling factor between Mat dimensions and ImageView dimensions
     * if ImageView is scaled. Adjust this method to match your actual scaling logic.
     */
    private float getImageScale(Mat imageMat, FrameLayout layout) {
        ImageView imageView = layout.findViewById(R.id.image_display_view);
        float widthScale = (float) imageView.getWidth() / imageMat.cols();
        float heightScale = (float) imageView.getHeight() / imageMat.rows();
        // Use either widthScale or heightScale depending on your scaling strategy
        return widthScale; // or heightScale
    }
}