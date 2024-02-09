package com.example.uidesign_cistercian;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImageDisplayActivity extends AppCompatActivity {

    private Mat matImage;
    private int imageHeight;
    private List<Integer> arabicResults = new ArrayList<>();

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

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        // Handle the Up button click here
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
        // Apply Gaussian Blur for noise reduction
        Imgproc.GaussianBlur(matImage, matImage, new Size(5, 5), 0);
        // Apply Binary Threshold
        Mat binaryImage = new Mat(); // keep copy of binary image for future processing
        Imgproc.threshold(matImage, binaryImage, 155, 255, Imgproc.THRESH_BINARY);
        // Apply Canny Edge Detection
        Mat edgeDetectedImage = new Mat();
        Imgproc.Canny(binaryImage, edgeDetectedImage, 100, 200);
        // Convert matImage to 3 channels
        Mat coloredBinaryImage = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC3);
        Imgproc.cvtColor(binaryImage, coloredBinaryImage, Imgproc.COLOR_GRAY2BGR);
        // Find Contours and approximate them
        findAndApproximateContours(edgeDetectedImage, coloredBinaryImage);
        // Convert processed Mat back to Bitmap
        Utils.matToBitmap(coloredBinaryImage, bitmap);

        // Update ImageView with the processed Bitmap
        runOnUiThread(() -> {
            ImageView imageView = findViewById(R.id.image_display_view);
            imageView.setImageBitmap(bitmap);
        });
    }

    private void findAndApproximateContours(Mat edgeDetectedImage, Mat coloredBinaryImage) {
        System.out.println("findAndApproximateContours");
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
            //Imgproc.drawContours(coloredBinaryImage, Arrays.asList(points), -1, new Scalar(0, 0, 255), 2);
            // Calculate bounding rectangle for each contour
            Rect boundingRect = Imgproc.boundingRect(contour);
            boundingRects.add(boundingRect);
        }

        // Filter out rectangles that are inside other rectangles
        List<Rect> filteredRects = filterRectangles(boundingRects, imageHeight, coloredBinaryImage);
        drawQuadrants(coloredBinaryImage, filteredRects);
    }

    private List<Rect> filterRectangles(List<Rect> rects, int imageHeight, Mat image) {

        List<Rect> sizeFilteredRects = new ArrayList<>(); // List of rectangles that pass filter 1
        List<Rect> uniqueFilteredRects = new ArrayList<>(); // List of rectangles that pass filters 1 and 2
        Set<String> uniqueRectSignatures = new HashSet<>(); // Signatures for filter 2
        List<Rect> finalFilteredRects = new ArrayList<>(); // Set of rectangles that pass filters 1, 2, and 3

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
        for (Rect rect : sizeFilteredRects) {
            Mat rotatedImage = image.clone(); // Clone the image for rotation

            if (rect.width > rect.height) {
                // Rotate the image 90 degrees clockwise
                Core.rotate(coloredBinaryImage, rotatedImage, Core.ROTATE_90_COUNTERCLOCKWISE);
                Rect rotatedRect = new Rect(rect.y, coloredBinaryImage.cols() - rect.x - rect.width, rect.height, rect.width);
                processRectangle(rotatedImage, rotatedRect);
                Core.rotate(rotatedImage, coloredBinaryImage, Core.ROTATE_90_CLOCKWISE);
            } else {
                //System.out.println("rectangle was NOT rotated");
                processRectangle(coloredBinaryImage, rect);
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
        //List<Double> percentages1stCheck = new ArrayList<>();
        //List<Double> percentages2ndCheck = new ArrayList<>();


        //System.out.println("drawQuadrants called");
        // Draw the bounding rectangles that passed the filter

        /*

        for (Rect rect : filteredRects) {
            // Find Stem
            Point divisionPoint1 = new Point(rect.x + rect.width / 2, rect.y);
            Point divisionPoint2 = new Point(rect.x + rect.width / 2, rect.y + rect.height);
            Line stemCandidate = new Line(divisionPoint1, divisionPoint2, new Scalar(255, 0, 0), 2);

                        int guideWidth = rect.width / 20;
                        int guideHeight = rect.height;
                        Point rectCenter = new Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0);

            // Calculate the rotation increment and start angle
            double angleIncrement = 1.0;
            double currentAngle = 0.0;
            boolean stemFound = false;

            // Rotate guide rectangle in small increments checking for the stem
            while (currentAngle < 360.0 && !stemFound) {
                // Calculate the new coordinates for the guide rectangle after rotation
                Point newCenter = rotatePoint(rectCenter, currentAngle, guideWidth, guideHeight);
                Rect guideRect = new Rect(
                        (int) (newCenter.x - guideWidth / 2.0),
                        (int) (newCenter.y - guideHeight / 2.0),
                        guideWidth,
                        guideHeight
                );
                if (currentAngle == 10.0 || currentAngle == 20.0 || currentAngle == 40.0 || currentAngle == 70.0 || currentAngle == 110.0 || currentAngle == 150.0 ||currentAngle == 180.0 || currentAngle == 250.0 || currentAngle == 300.0) {
                    stemCandidate.draw(coloredBinaryImage);
                }

                // Calculate the percentage of black pixels within the rotated guide rectangle
                double blackPixelPercentage = calculateBlackPixelPercentage(coloredBinaryImage, guideRect);

                if (blackPixelPercentage > 60) {
                    // If the stem is found, rotate the image once to process it
                    Mat rotatedImage = new Mat();
                    Mat rotationMatrix = Imgproc.getRotationMatrix2D(rectCenter, currentAngle, 1.0);
                    Imgproc.warpAffine(coloredBinaryImage, rotatedImage, rotationMatrix, coloredBinaryImage.size());

                    // Draw and process the rotated rectangle
                    Imgproc.rectangle(rotatedImage, guideRect.tl(), guideRect.br(), new Scalar(0, 255, 0), 2);
                    processRectangle(rotatedImage, rect);

                    // Optionally, rotate back if needed to show the original image orientation
                    Mat reverseRotationMatrix = Imgproc.getRotationMatrix2D(rectCenter, -currentAngle, 1.0); // This will reverse the rotation
                    Imgproc.warpAffine(rotatedImage, coloredBinaryImage, reverseRotationMatrix, coloredBinaryImage.size());
                    stemFound = true; // Exit the loop as stem is found
                }

                // Increment the angle for the next iteration
                currentAngle += angleIncrement;
            }
        }

         */


        /*

        for (Rect rect : filteredRects) {
            Rect guidelineRectStem = null;

            // Guideline Rectangle 1, to find Stem
            int guidelineRectStemWidth = rect.width / 45;
            int guidelineRectStemHeight = rect.height;
            guidelineRectStem = new Rect(rect.x + (rect.width / 2) - (guidelineRectStemWidth / 2), rect.y, guidelineRectStemWidth, guidelineRectStemHeight);
            Imgproc.rectangle(coloredBinaryImage, guidelineRectStem.tl(), guidelineRectStem.br(), new Scalar(150, 100, 100), 2);

            double percentage = 0;

            Mat subImage = coloredBinaryImage.submat(rect);
            Mat rotatedImage = new Mat();
            double angleIncrement = 1.0;
            double currentAngle = 0.0;

            while (currentAngle < 90.0) {
                percentage = calculateBlackPixelPercentage(rotatedImage, guidelineRectStem);
                Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(rect.width/2, rect.height/2), currentAngle, 1.0);
                Imgproc.warpAffine(subImage, rotatedImage, rotationMatrix, subImage.size());

                if (percentage >= 40) {
                    Imgproc.rectangle(coloredBinaryImage, guidelineRectStem.tl(), guidelineRectStem.br(), new Scalar(0, 255, 0), 2);
                    processRectangle(rotatedImage, new Rect(0, 0, rotatedImage.width(), rotatedImage.height()));
                    break;
                }
                currentAngle += angleIncrement;
            }
        }

         */


        for (Rect rect : filteredRects) {
            Map<Line, Double> percentages1stCheck = new HashMap<>();
            Map<Line, Double> percentages2ndCheck = new HashMap<>();
            Map<Line, Double> percentagesTop4 = new HashMap<>();

            Mat rotatedImage = coloredBinaryImage.clone(); // Clone the image for rotation
            // Draw the bounding rectangle
            Imgproc.rectangle(coloredBinaryImage, rect.tl(), rect.br(), new Scalar(2, 82, 4), 2);
            boolean stemFound = false;
            boolean firstCheckDone = false;
            boolean secondCheckDone = false;
            boolean similarPercentages = false;

            if(!stemFound) {
                for (int a = 0; a <= 40; a++) {
                    Point divisionPoint1 = new Point(rect.x, rect.y + a * (rect.height / 40.0));
                    Point divisionPoint2 = new Point(rect.x + rect.width, rect.y + rect.height - (a * (rect.height / 40.0)));
                    Line stemCandidate = new Line(divisionPoint1, divisionPoint2, new Scalar(255, 0, 0), 1);

                    double percentage = stemCandidate.getBlackPixelPercentage(coloredBinaryImage);
                    percentages1stCheck.put(stemCandidate, percentage);

                    if (percentage >= 90) {
                        stemCandidate.draw(coloredBinaryImage);
                        stemFound = true;
                        break;
                    }

                    //stemCandidate.draw(coloredBinaryImage);
                }
                firstCheckDone = true;
            }
            if(!stemFound && firstCheckDone) {
                for (int a = 0; a <= 40; a++) {
                    Point divisionPoint1 = new Point(rect.x + rect.width - (a * (rect.width / 40.0)), rect.y);
                    Point divisionPoint2 = new Point(rect.x + a * (rect.width / 40.0), rect.y + rect.height);
                    Line stemCandidate = new Line(divisionPoint1, divisionPoint2, new Scalar(200, 0, 100), 1);

                    double percentage = stemCandidate.getBlackPixelPercentage(coloredBinaryImage);
                    percentages2ndCheck.put(stemCandidate, percentage);

                    if (percentage >= 90) {
                        stemCandidate.draw(coloredBinaryImage);
                        stemFound = true;
                        break;
                    }

                    //stemCandidate.draw(coloredBinaryImage);
                }
                secondCheckDone = true;
            }
            if(!stemFound && firstCheckDone && secondCheckDone) {
                //Map.Entry<Line, Double> biggestPercent1stCheck = Collections.max(percentages1stCheck.entrySet(), Map.Entry.comparingByValue());
                //Map.Entry<Line, Double> biggestPercent2ndCheck = Collections.max(percentages2ndCheck.entrySet(), Map.Entry.comparingByValue());

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

                if (Math.abs(biggestPercent2ndCheck.getValue()-biggestPercent1stCheck.getValue()) <= 9) {
                    similarPercentages = true;
                }

                if (!percentagesTop4.isEmpty() && !similarPercentages) {
                    largestPercentage = top4List.get(0); // largest percentage
                    if (percentagesTop4.size() > 1) {
                        secondLargestPercentage = top4List.get(1); // second largest percentage
                    }
                }
                else if (!percentagesTop4.isEmpty() && similarPercentages) {
                    largestPercentage = biggestPercent1stCheck;
                    secondLargestPercentage = biggestPercent2ndCheck;
                }

                // draw the 2 largest percentages of the rectangle
                largestPercentage.getKey().draw(coloredBinaryImage);
                secondLargestPercentage.getKey().draw(coloredBinaryImage);

                Point intersectionPoint = new Point();
                intersectionPoint = largestPercentage.getKey().getIntersectionPoint(secondLargestPercentage.getKey());

                // Create the 2 candidates for Stem, by making lines between opposite corners of the 2 largestPercentage lines
                Line stemCandidate1 = new Line(largestPercentage.getKey().getPt1(), secondLargestPercentage.getKey().getPt2(), new Scalar(255, 50, 50), 2);
                Line stemCandidate2 = new Line(secondLargestPercentage.getKey().getPt1(), largestPercentage.getKey().getPt2(), new Scalar(255, 50, 50), 2);

                // draw the 2 stem candidates
                stemCandidate1.draw(coloredBinaryImage);
                stemCandidate2.draw(coloredBinaryImage);

                // Create a line that unites the intersection point with the candidates - stem guideline
                Line stemGuideline = new Line(stemCandidate1.getPerpendicularIntersectionPoint(intersectionPoint), stemCandidate2.getPerpendicularIntersectionPoint(intersectionPoint), new Scalar(0, 0, 255), 1);
                // draw it
                stemGuideline.draw(coloredBinaryImage);


            }





            if (rect.width > rect.height) {


                /*
                // Rotate the image 90 degrees clockwise
                //System.out.println("rectangle was rotated");
                Core.rotate(coloredBinaryImage, rotatedImage, Core.ROTATE_90_COUNTERCLOCKWISE);
                Rect rotatedRect = new Rect(rect.y, coloredBinaryImage.cols() - rect.x - rect.width, rect.height, rect.width);
                processRectangle(rotatedImage, rotatedRect);
                Core.rotate(rotatedImage, coloredBinaryImage, Core.ROTATE_90_CLOCKWISE);



                 */

            } else {
                //System.out.println("rectangle was NOT rotated");
                processRectangle(coloredBinaryImage, rect);
            }
        }
    }

    private Point rotatePoint(Point center, double angleDegrees, int width, int height) {
        double radians = Math.toRadians(angleDegrees);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        // This will rotate the point around the center of the rectangle
        double xNew = center.x + (width / 2.0) * cos - (height / 2.0) * sin;
        double yNew = center.y + (width / 2.0) * sin + (height / 2.0) * cos;
        return new Point(xNew, yNew);
    }

    private int processRectangle(Mat coloredBinaryImage, Rect rect) {
        int arabicResult = 0;
        // Find Stem
        Point divisionPoint1 = new Point(rect.x + rect.width / 2, rect.y);
        Point divisionPoint2 = new Point(rect.x + rect.width / 2, rect.y + rect.height);
        //Imgproc.line(coloredBinaryImage, divisionPoint1, divisionPoint2, new Scalar(0, 255, 255), 2);

        int quadrantHeight = 4 * (rect.height / 10);
        int quadrantWidth = rect.width / 2;

        Rect quadrantUnits = new Rect(rect.x + quadrantWidth, rect.y, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantUnits.tl(), quadrantUnits.br(), new Scalar(255, 0, 0), 2);
        int unitsDigit = resizingUnits(coloredBinaryImage, quadrantUnits);

        Rect quadrantTens = new Rect(rect.x, rect.y, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantTens.tl(), quadrantTens.br(), new Scalar(0, 255, 255), 2);
        int tensDigit = resizingTens(coloredBinaryImage, quadrantTens);

        Rect quadrantHundreds = new Rect(rect.x + quadrantWidth, rect.y + rect.height - quadrantHeight, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantHundreds.tl(), quadrantHundreds.br(), new Scalar(255,255, 0), 2);
        int hundredsDigit = resizingHundreds(coloredBinaryImage, quadrantHundreds);

        Rect quadrantThousands = new Rect(rect.x, rect.y + rect.height - quadrantHeight, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantThousands.tl(), quadrantThousands.br(), new Scalar(255, 0, 255), 2);
        int thousandsDigit = resizingThousands(coloredBinaryImage, quadrantThousands);

        arabicResult = thousandsDigit*1000 + hundredsDigit*100 + tensDigit*10 + unitsDigit;
        System.out.println("FINAL ARABIC RESULT IS " + arabicResult);

        arabicResults.add(arabicResult);
        updateResultsDisplay(); // Refresh the display with the new list of results
        return arabicResult;
    }

    // *******************************************************************************************************************

    // SubQuadrant class
    private static class SubQuadrant {
        Rect rect;
        double blackPixelPercentage;

        SubQuadrant(Rect rect, double blackPixelPercentage) {
            this.rect = rect;
            this.blackPixelPercentage = blackPixelPercentage;
        }

        public Rect getRect() {
            return rect;
        }

        public double getBlackPixelPercentage() {
            return blackPixelPercentage;
        }
    }

// *******************************************************************************************************************

    private int resizingUnits(Mat image, Rect rect) {
        int unitsDigitResult = 0;
        boolean firstLineDrawn = false;
        boolean pixel1Found = false, pixel2Found = false, pixel3Found = false, pixel4Found = false;
        int leftLimitX = -1, rightLimitX = -1, bottomLimitY = -1, topLimitY = -1;
        Rect guideline1Rect = null, guideline2Rect = null, guideline3Rect = null, guideline4Rect = null;
        int subQuadrantHeight, subQuadrantWidth;
        List<Rect> subQuadrantsUnits = new ArrayList<>();


        // Guideline Rectangle 1, to find leftLimitX
        int guideline1Width = rect.width / 2;
        int guideline1Height = rect.height / 15;
        guideline1Rect = new Rect(rect.x + (rect.width / 25), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
        //Imgproc.rectangle(image, guideline1Rect.tl(), guideline1Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline1Rect != null) {
            for (int x = guideline1Rect.x; x < guideline1Rect.x + guideline1Rect.width; x++) {
                for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Rect.height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                        pixel1Found = true;
                        leftLimitX = x + (guideline1Rect.width / 15);
                        Point lineStart = new Point(leftLimitX, rect.y);
                        Point lineEnd = new Point(leftLimitX, rect.y + rect.height);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                        firstLineDrawn = true;
                        break;
                    }
                }
                if (firstLineDrawn) break; // Exit the outer loop as well after drawing the 1st line
            }
            if (!pixel1Found) {
                leftLimitX = rect.x + (rect.width / 15);
                Point lineStart = new Point(leftLimitX, rect.y);
                Point lineEnd = new Point(leftLimitX, rect.y + rect.height);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 2, to find rightLimitX
        int guideline2Width = rect.width / 3;
        int guideline2Height = rect.height;
        guideline2Rect = new Rect(rect.x + (2 * (rect.width / 3)), rect.y, guideline2Width, guideline2Height);
        //Imgproc.rectangle(image, guideline2Rect.tl(), guideline2Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline2Rect != null) {
            for (int x = guideline2Rect.x + guideline2Rect.width; x > guideline2Rect.x; x--) {
                for (int y = guideline2Rect.y; y < guideline2Rect.y + guideline2Rect.height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel2Found = true;
                        rightLimitX = x;
                        Point lineStart = new Point(rightLimitX, rect.y);
                        Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                        break;
                    }
                }
                if (rightLimitX != -1)
                    break; // Exit the outer loop as well after drawing 2nd line
            }
            if (!pixel2Found) {
                rightLimitX = rect.x + rect.width;
                Point lineStart = new Point(rightLimitX, rect.y);
                Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 3, to find bottomLimitX
        int guideline3Width = rightLimitX - leftLimitX;
        int guideline3Height = 4 * (rect.height / 10);
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline3Rect = new Rect(leftLimitX + (guideline3Width/20), rect.y + rect.height - guideline3Height, guideline3Width, guideline3Height);
            //Imgproc.rectangle(image, guideline3Rect.tl(), guideline3Rect.br(), new Scalar(255, 150, 0), 2);
        }

        if (guideline3Rect != null) {
            for (int y = guideline3Rect.y + guideline3Height; y > guideline3Rect.y; y--) {
                for (int x = guideline3Rect.x; x < guideline3Rect.x + guideline3Width; x++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel3Found = true;
                        bottomLimitY = y;
                        Point lineStart = new Point(guideline3Rect.x, bottomLimitY);
                        Point lineEnd = new Point(guideline3Rect.x + guideline3Width, bottomLimitY);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the third line
                        break;
                    }
                }
                if (bottomLimitY != -1)
                    break; // Exit the outer loop as well after "drawing" the second line
            }
            if (!pixel3Found) {
                bottomLimitY = guideline3Rect.y + guideline3Height;
                Point lineStart = new Point(guideline3Rect.x, bottomLimitY);
                Point lineEnd = new Point(guideline3Rect.x + guideline3Width, bottomLimitY);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the third line
            }
        }


        // Guideline Rectangle 4, to find topLimitX
        int guideline4Width = rightLimitX - leftLimitX;
        int guideline4Height = 4 * (rect.height / 10);
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline4Rect = new Rect(leftLimitX, rect.y, guideline4Width, guideline4Height);
            //Imgproc.rectangle(image, guideline4Rect.tl(), guideline4Rect.br(), new Scalar(255, 150, 0), 2);
        }

        // iterate through the rectangle to find the first black pixel from the other side
        if (guideline4Rect != null) {
            for (int y = guideline4Rect.y; y < guideline4Height; y++) {
                for (int x = guideline4Rect.x; x < guideline4Rect.x + guideline4Width; x++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel4Found = true;
                        topLimitY = y;
                        Point lineStart = new Point(guideline4Rect.x, topLimitY);
                        Point lineEnd = new Point(guideline4Rect.x + guideline4Width, topLimitY);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the third line
                        break;
                    }
                }
                if (topLimitY != -1)
                    break; // Exit the outer loop as well after drawing the 4th line
            }
            if (!pixel4Found) {
                topLimitY = guideline4Rect.y;
                Point lineStart = new Point(guideline4Rect.x, topLimitY);
                Point lineEnd = new Point(guideline4Rect.x + guideline4Width, topLimitY);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the third line
            }
        }


        if (leftLimitX != -1 && rightLimitX != -1 && bottomLimitY != -1 && topLimitY != -1) {
            // Subdivide Resized Quadrant into Sub-quadrants
            subQuadrantHeight = (bottomLimitY - topLimitY) / 3;
            subQuadrantWidth = (rightLimitX - leftLimitX) / 3;

            Rect subQuadrantUnits1 = new Rect(leftLimitX, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits2 = new Rect(leftLimitX + subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits3 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits4 = new Rect(leftLimitX, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits5 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits6 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits7 = new Rect(leftLimitX, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits8 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantUnits9 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);

            subQuadrantsUnits.add(subQuadrantUnits1);
            subQuadrantsUnits.add(subQuadrantUnits2);
            subQuadrantsUnits.add(subQuadrantUnits3);
            subQuadrantsUnits.add(subQuadrantUnits4);
            subQuadrantsUnits.add(subQuadrantUnits5);
            subQuadrantsUnits.add(subQuadrantUnits6);
            subQuadrantsUnits.add(subQuadrantUnits7);
            subQuadrantsUnits.add(subQuadrantUnits8);
            subQuadrantsUnits.add(subQuadrantUnits9);

            //drawSubQuadrants(image, subQuadrantsUnits);
            unitsDigitResult = detectValidSubQuadrants(image, subQuadrantsUnits);
            //System.out.println("THE NUMBER IN UNITS IS " + unitsDigitResult);
        }
        return unitsDigitResult;
    }

// *******************************************************************************************************************

    private int resizingTens(Mat image, Rect rect) {
        int tensDigitResult = 0;
        boolean firstLineDrawn = false;
        boolean pixel1Found = false, pixel2Found = false, pixel3Found = false, pixel4Found = false;
        int rightLimitX = -1, leftLimitX = -1, bottomLimitY = -1, topLimitY = -1;
        Rect guideline1Rect = null, guideline2Rect = null, guideline3Rect = null, guideline4Rect = null;
        int subQuadrantHeight, subQuadrantWidth;
        List<Rect> subQuadrantsTens = new ArrayList<>();


        // Guideline Rectangle 1, to find rightLimitX
        int guideline1Width = rect.width / 2;
        int guideline1Height = rect.height / 15;
        guideline1Rect = new Rect(rect.x + (rect.width/2) - (rect.width/25), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
        //Imgproc.rectangle(image, guideline1Rect.tl(), guideline1Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline1Rect != null) {
            for (int x = guideline1Rect.x + guideline1Width; x > guideline1Rect.x; x--) {
                for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                        pixel1Found = true;
                        rightLimitX = x - (guideline1Rect.width / 15);
                        Point lineStart = new Point(rightLimitX, rect.y);
                        Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the first line
                        firstLineDrawn = true;
                        break;
                    }
                }
                if (firstLineDrawn) break; // Exit the outer loop as well after drawing first line
            }
            if (!pixel1Found) {
                rightLimitX = rect.x + rect.width - (rect.width/15);
                Point lineStart = new Point(rightLimitX, rect.y);
                Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 2, to find leftLimitX
        int guideline2Width = rect.width / 3;
        int guideline2Height = rect.height;
        guideline2Rect = new Rect(rect.x, rect.y, guideline2Width, guideline2Height);
        //Imgproc.rectangle(image, guideline2Rect.tl(), guideline2Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline2Rect != null) {
            for (int x = guideline2Rect.x; x < guideline2Rect.x + guideline2Width; x++) {
                for (int y = guideline2Rect.y; y < guideline2Rect.y + guideline2Height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel2Found = true;
                        leftLimitX = x;
                        Point lineStart = new Point(leftLimitX, rect.y);
                        Point lineEnd = new Point(leftLimitX, rect.y + rect.height);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the second line
                        break;
                    }
                }
                if (leftLimitX != -1)
                    break; // Exit the outer loop as well after drawing second line
            }
            if (!pixel2Found) {
                leftLimitX = guideline2Rect.x;
                Point lineStart = new Point(leftLimitX, guideline2Rect.y);
                Point lineEnd = new Point(leftLimitX, guideline2Rect.y + guideline2Height);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 3, to find bottomLimitX
        int guideline3Width = rightLimitX - leftLimitX;
        int guideline3Height = 4 * (rect.height / 10);
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline3Rect = new Rect(leftLimitX - (guideline3Width/20), rect.y + rect.height - guideline3Height, guideline3Width, guideline3Height);
            //Imgproc.rectangle(image, guideline3Rect.tl(), guideline3Rect.br(), new Scalar(255, 150, 0), 2);
        }

        if (guideline3Rect != null) {
            for (int y = guideline3Rect.y + guideline3Height; y > guideline3Rect.y; y--) {
                for (int x = guideline3Rect.x; x < guideline3Rect.x + guideline3Width; x++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel3Found = true;
                        bottomLimitY = y;
                        Point lineStart = new Point(guideline3Rect.x, bottomLimitY);
                        Point lineEnd = new Point(guideline3Rect.x + guideline3Width, bottomLimitY);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the third line
                        break;
                    }
                }
                if (bottomLimitY != -1)
                    break; // Exit the outer loop as well after "drawing" the second line
            }
            if (!pixel3Found) {
                bottomLimitY = guideline3Rect.y + guideline3Height;
                Point lineStart = new Point(guideline3Rect.x, bottomLimitY);
                Point lineEnd = new Point(guideline3Rect.x + guideline3Width, bottomLimitY);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the third line
            }
        }


        // Guideline Rectangle 4, to find topLimitX
        int guideline4Width = rightLimitX - leftLimitX;
        int guideline4Height = 4 * (rect.height / 10);
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline4Rect = new Rect(leftLimitX, rect.y, guideline4Width, guideline4Height);
            //Imgproc.rectangle(image, guideline4Rect.tl(), guideline4Rect.br(), new Scalar(255, 150, 0), 2);
        }

        if (guideline4Rect != null) {
            for (int y = guideline4Rect.y; y < guideline4Height; y++) {
                for (int x = guideline4Rect.x; x < guideline4Rect.x + guideline4Width; x++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel4Found = true;
                        topLimitY = y;
                        Point lineStart = new Point(guideline4Rect.x, topLimitY);
                        Point lineEnd = new Point(guideline4Rect.x + guideline4Width, topLimitY);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                        break;
                    }
                }
                if (topLimitY != -1)
                    break; // Exit the outer loop as well after drawing the 4th line
            }
            if (!pixel4Found) {
                topLimitY = guideline4Rect.y;
                Point lineStart = new Point(guideline4Rect.x, topLimitY);
                Point lineEnd = new Point(guideline4Rect.x + guideline4Width, topLimitY);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        if (rightLimitX != -1 && leftLimitX != -1 && bottomLimitY != -1 && topLimitY != -1) {
            // Subdivide Resized Quadrant into Sub-quadrants
            subQuadrantHeight = (bottomLimitY - topLimitY) / 3;
            subQuadrantWidth = (rightLimitX - leftLimitX) / 3;

            Rect subQuadrantTens1 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens2 = new Rect(leftLimitX + subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens3 = new Rect(leftLimitX, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens4 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens5 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens6 = new Rect(leftLimitX, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens7 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens8 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantTens9 = new Rect(leftLimitX, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);

            subQuadrantsTens.add(subQuadrantTens1);
            subQuadrantsTens.add(subQuadrantTens2);
            subQuadrantsTens.add(subQuadrantTens3);
            subQuadrantsTens.add(subQuadrantTens4);
            subQuadrantsTens.add(subQuadrantTens5);
            subQuadrantsTens.add(subQuadrantTens6);
            subQuadrantsTens.add(subQuadrantTens7);
            subQuadrantsTens.add(subQuadrantTens8);
            subQuadrantsTens.add(subQuadrantTens9);

            //drawSubQuadrants(image, subQuadrantsTens);
            tensDigitResult = detectValidSubQuadrants(image, subQuadrantsTens);
            //System.out.println("THE NUMBER IN TENS IS " + tensDigitResult);
        }
        return tensDigitResult;
    }

// *******************************************************************************************************************

    private int resizingHundreds(Mat image, Rect rect) {
        int hundredsDigitResult = 0;
        boolean firstLineDrawn = false;
        boolean pixel1Found = false, pixel2Found = false, pixel3Found = false, pixel4Found = false;
        int leftLimitX = -1, rightLimitX = -1, topLimitY = -1, bottomLimitY = -1;
        Rect guideline1Rect = null, guideline2Rect = null, guideline3Rect = null, guideline4Rect = null;
        int subQuadrantHeight, subQuadrantWidth;
        List<Rect> subQuadrantsHundreds = new ArrayList<>();


        // Guideline Rectangle 1, to find leftLimitX
        int guideline1Width = rect.width / 2;
        int guideline1Height = rect.height / 15;
        guideline1Rect = new Rect(rect.x + (rect.width / 25), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
        //Imgproc.rectangle(image, guideline1Rect.tl(), guideline1Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline1Rect != null) {
            for (int x = guideline1Rect.x; x < guideline1Rect.x + guideline1Rect.width; x++) {
                for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Rect.height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                        pixel1Found = true;
                        leftLimitX = x + (guideline1Rect.width / 15);
                        Point lineStart = new Point(leftLimitX, rect.y);
                        Point lineEnd = new Point(leftLimitX, rect.y + rect.height);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                        firstLineDrawn = true;
                        break;
                    }
                }
                if (firstLineDrawn) break; // Exit the outer loop as well after drawing the 1st line
            }
            if (!pixel1Found) {
                leftLimitX = rect.x + (rect.width / 15);
                Point lineStart = new Point(leftLimitX, rect.y);
                Point lineEnd = new Point(leftLimitX, rect.y + rect.height);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 2, to find rightLimitX
        int guideline2Width = rect.width / 3;
        int guideline2Height = rect.height;
        guideline2Rect = new Rect(rect.x + (2 * (rect.width / 3)), rect.y, guideline2Width, guideline2Height);
        //Imgproc.rectangle(image, guideline2Rect.tl(), guideline2Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline2Rect != null) {
            for (int x = guideline2Rect.x + guideline2Rect.width; x > guideline2Rect.x; x--) {
                for (int y = guideline2Rect.y; y < guideline2Rect.y + guideline2Rect.height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel2Found = true;
                        rightLimitX = x;
                        Point lineStart = new Point(rightLimitX, rect.y);
                        Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                        break;
                    }
                }
                if (rightLimitX != -1)
                    break; // Exit the outer loop as well after drawing 2nd line
            }
            if (!pixel2Found) {
                rightLimitX = rect.x + rect.width;
                Point lineStart = new Point(rightLimitX, rect.y);
                Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 3, to find topLimitX
        int guideline3Width = rightLimitX - leftLimitX;
        int guideline3Height = 4 * (rect.height / 10);
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline3Rect = new Rect(leftLimitX + (guideline3Width/20), rect.y, guideline3Width, guideline3Height);
            //Imgproc.rectangle(image, guideline3Rect.tl(), guideline3Rect.br(), new Scalar(255, 150, 0), 2);
        }

        if (guideline3Rect != null) {
            for (int y = guideline3Rect.y; y < guideline3Rect.y + guideline3Height; y++) {
                for (int x = guideline3Rect.x; x < guideline3Rect.x + guideline3Width; x++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel3Found = true;
                        topLimitY = y;
                        Point lineStart = new Point(guideline3Rect.x, topLimitY);
                        Point lineEnd = new Point(guideline3Rect.x + guideline3Width, topLimitY);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                        break;
                    }
                }
                if (topLimitY != -1) break; // Exit the outer loop as well after drawing the 3rd line
            }
            if (!pixel3Found) {
                topLimitY = guideline3Rect.y;
                Point lineStart = new Point(guideline3Rect.x, topLimitY);
                Point lineEnd = new Point(guideline3Rect.x + guideline3Width, topLimitY);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 4, to find bottomLimitX
        int guideline4Width = rightLimitX - leftLimitX;
        int guideline4Height = 4 * (rect.height / 10);
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline4Rect = new Rect(leftLimitX, rect.y + rect.height - guideline4Height, guideline4Width, guideline4Height);
            //Imgproc.rectangle(image, guideline4Rect.tl(), guideline4Rect.br(), new Scalar(255, 150, 0), 2);
        }

        if (guideline4Rect != null) {
            for (int y = guideline4Rect.y + guideline4Height; y > guideline4Rect.y; y--) {
                for (int x = guideline4Rect.x; x < guideline4Rect.x + guideline4Width; x++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel4Found = true;
                        bottomLimitY = y;
                        Point lineStart = new Point(guideline4Rect.x, bottomLimitY);
                        Point lineEnd = new Point(guideline4Rect.x + guideline4Width, bottomLimitY);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                        break;
                    }
                }
                if (bottomLimitY != -1)
                    break; // Exit the outer loop as well after drawing the 4th line
            }
            if (!pixel4Found) {
                bottomLimitY = guideline4Rect.y + guideline3Height;
                Point lineStart = new Point(guideline4Rect.x, bottomLimitY);
                Point lineEnd = new Point(guideline4Rect.x + guideline4Width, bottomLimitY);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        if (leftLimitX != -1 && rightLimitX != -1 && topLimitY != -1 && bottomLimitY != -1) {
            // Subdivide Resized Quadrant into Sub-quadrants
            subQuadrantHeight = (bottomLimitY - topLimitY) / 3;
            subQuadrantWidth = (rightLimitX - leftLimitX) / 3;

            Rect subQuadrantHundreds1 = new Rect(leftLimitX, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds2 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds3 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + 2 * subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds4 = new Rect(leftLimitX, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds5 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds6 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds7 = new Rect(leftLimitX, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds8 = new Rect(leftLimitX + subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantHundreds9 = new Rect(leftLimitX + 2 * subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);

            subQuadrantsHundreds.add(subQuadrantHundreds1);
            subQuadrantsHundreds.add(subQuadrantHundreds2);
            subQuadrantsHundreds.add(subQuadrantHundreds3);
            subQuadrantsHundreds.add(subQuadrantHundreds4);
            subQuadrantsHundreds.add(subQuadrantHundreds5);
            subQuadrantsHundreds.add(subQuadrantHundreds6);
            subQuadrantsHundreds.add(subQuadrantHundreds7);
            subQuadrantsHundreds.add(subQuadrantHundreds8);
            subQuadrantsHundreds.add(subQuadrantHundreds9);

            //drawSubQuadrants(image, subQuadrantsHundreds);
            hundredsDigitResult = detectValidSubQuadrants(image, subQuadrantsHundreds);
            //System.out.println("THE NUMBER IN HUNDREDS IS " + hundredsDigitResult);
        }
        return hundredsDigitResult;
    }

// *******************************************************************************************************************

    private int resizingThousands(Mat image, Rect rect) {
        int thousandsDigitResult = 0;
        boolean firstLineDrawn = false;
        boolean pixel1Found = false, pixel2Found = false, pixel3Found = false, pixel4Found = false;
        int leftLimitX = -1, rightLimitX = -1, bottomLimitY = -1, topLimitY = -1;
        Rect guideline1Rect = null, guideline2Rect = null, guideline3Rect = null, guideline4Rect = null;
        int subQuadrantHeight, subQuadrantWidth;
        List<Rect> subQuadrantsThousands = new ArrayList<>();

        // Guideline Rectangle 1, to find rightLimitX
        int guideline1Width = rect.width / 2;
        int guideline1Height = rect.height / 15;
        guideline1Rect = new Rect(rect.x + (rect.width/2) - (rect.width/25), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
        //Imgproc.rectangle(image, guideline1Rect.tl(), guideline1Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline1Rect != null) {
            for (int x = guideline1Rect.x + guideline1Width; x > guideline1Rect.x; x--) {
                for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                        pixel1Found = true;
                        rightLimitX = x - (guideline1Rect.width / 15);
                        Point lineStart = new Point(rightLimitX, rect.y);
                        Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the first line
                        firstLineDrawn = true;
                        break;
                    }
                }
                if (firstLineDrawn) break; // Exit the outer loop as well after drawing first line
            }
            if (!pixel1Found) {
                rightLimitX = rect.x + rect.width - (rect.width/15);
                Point lineStart = new Point(rightLimitX, rect.y);
                Point lineEnd = new Point(rightLimitX, rect.y + rect.height);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 2, to find leftLimitX
        int guideline2Width = rect.width / 3;
        int guideline2Height = rect.height;
        guideline2Rect = new Rect(rect.x, rect.y, guideline2Width, guideline2Height);
        //Imgproc.rectangle(image, guideline2Rect.tl(), guideline2Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline2Rect != null) {
            for (int x = guideline2Rect.x; x < guideline2Rect.x + guideline2Width; x++) {
                for (int y = guideline2Rect.y; y < guideline2Rect.y + guideline2Height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel2Found = true;
                        leftLimitX = x;
                        Point lineStart = new Point(leftLimitX, rect.y);
                        Point lineEnd = new Point(leftLimitX, rect.y + rect.height);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1); // Draw the second line
                        break;
                    }
                }
                if (leftLimitX != -1)
                    break; // Exit the outer loop as well after drawing second line
            }
            if (!pixel2Found) {
                leftLimitX = guideline2Rect.x;
                Point lineStart = new Point(leftLimitX, guideline2Rect.y);
                Point lineEnd = new Point(leftLimitX, guideline2Rect.y + guideline2Height);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 3, to find topLimitX
        int guideline3Width = rightLimitX - leftLimitX;
        int guideline3Height = 4 * (rect.height / 10);
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline3Rect = new Rect(leftLimitX - (guideline3Width/20), rect.y, guideline3Width, guideline3Height);
            //Imgproc.rectangle(image, guideline3Rect.tl(), guideline3Rect.br(), new Scalar(255, 150, 0), 2);
        }

        if (guideline3Rect != null) {
            for (int y = guideline3Rect.y; y < guideline3Rect.y + guideline3Height; y++) {
                for (int x = guideline3Rect.x; x < guideline3Rect.x + guideline3Width; x++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel3Found = true;
                        topLimitY = y;
                        Point lineStart = new Point(guideline3Rect.x, topLimitY);
                        Point lineEnd = new Point(guideline3Rect.x + guideline3Width, topLimitY);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                        break;
                    }
                }
                if (topLimitY != -1) break; // Exit the outer loop as well after drawing the 3rd line
            }
            if (!pixel3Found) {
                topLimitY = guideline3Rect.y;
                Point lineStart = new Point(guideline3Rect.x, topLimitY);
                Point lineEnd = new Point(guideline3Rect.x + guideline3Width, topLimitY);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        // Guideline Rectangle 4, to find bottomLimitX
        int guideline4Width = rightLimitX - leftLimitX;
        int guideline4Height = 4 * (rect.height / 10);
        if (rightLimitX != -1 && leftLimitX != -1) {
            guideline4Rect = new Rect(leftLimitX, rect.y + rect.height - guideline4Height, guideline4Width, guideline4Height);
            //Imgproc.rectangle(image, guideline4Rect.tl(), guideline4Rect.br(), new Scalar(255, 150, 0), 2);
        }

        if (guideline4Rect != null) {
            for (int y = guideline4Rect.y + guideline4Height; y > guideline4Rect.y; y--) {
                for (int x = guideline4Rect.x; x < guideline4Rect.x + guideline4Width; x++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        pixel4Found = true;
                        bottomLimitY = y;
                        Point lineStart = new Point(guideline4Rect.x, bottomLimitY);
                        Point lineEnd = new Point(guideline4Rect.x + guideline4Width, bottomLimitY);
                        //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                        break;
                    }
                }
                if (bottomLimitY != -1)
                    break; // Exit the outer loop as well after drawing the 4th line
            }
            if (!pixel4Found) {
                bottomLimitY = guideline4Rect.y + guideline3Height;
                Point lineStart = new Point(guideline4Rect.x, bottomLimitY);
                Point lineEnd = new Point(guideline4Rect.x + guideline4Width, bottomLimitY);
                //Imgproc.line(image, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
            }
        }


        if (rightLimitX != -1 && leftLimitX != -1 && topLimitY != -1 && bottomLimitY != -1) {
            // Subdivide Resized Quadrant into Sub-quadrants
            subQuadrantHeight = (bottomLimitY - topLimitY) / 3;
            subQuadrantWidth = (rightLimitX - leftLimitX) / 3;

            Rect subQuadrantThousands1 = new Rect(leftLimitX + 2*subQuadrantWidth, topLimitY + 2*subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands2 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + 2*subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands3 = new Rect(leftLimitX, topLimitY + 2*subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands4 = new Rect(leftLimitX + 2*subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands5 = new Rect(leftLimitX + subQuadrantWidth, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands6 = new Rect(leftLimitX, topLimitY + subQuadrantHeight, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands7 = new Rect(leftLimitX + 2*subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands8 = new Rect(leftLimitX + subQuadrantWidth, topLimitY, subQuadrantWidth, subQuadrantHeight);
            Rect subQuadrantThousands9 = new Rect(leftLimitX, topLimitY, subQuadrantWidth, subQuadrantHeight);

            subQuadrantsThousands.add(subQuadrantThousands1);
            subQuadrantsThousands.add(subQuadrantThousands2);
            subQuadrantsThousands.add(subQuadrantThousands3);
            subQuadrantsThousands.add(subQuadrantThousands4);
            subQuadrantsThousands.add(subQuadrantThousands5);
            subQuadrantsThousands.add(subQuadrantThousands6);
            subQuadrantsThousands.add(subQuadrantThousands7);
            subQuadrantsThousands.add(subQuadrantThousands8);
            subQuadrantsThousands.add(subQuadrantThousands9);

            //drawSubQuadrants(image, subQuadrantsThousands);
            thousandsDigitResult = detectValidSubQuadrants(image, subQuadrantsThousands);
            //System.out.println("THE NUMBER IN THOUSANDS IS " + thousandsDigitResult);
        }
        return thousandsDigitResult;
    }

// *******************************************************************************************************************

    private void drawSubQuadrants(Mat image, List<Rect> subQuadrants) {
        for(Rect subQuadrant : subQuadrants) {
            Imgproc.rectangle(image, subQuadrant.tl(), subQuadrant.br(), new Scalar(255, 0, 0), 2);
        }
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
            if (Math.abs(percentage - guideValue) <= 10) {
                //Imgproc.rectangle(image, subQuadrant.tl(), subQuadrant.br(), new Scalar(0, 255, 0), 2); // Using green for highlighting
                initiallyFlagged.add(i + 1);
            }
        }

        // Additional logic to flag based on certain rules
        Set<Integer> toCheck = new HashSet<>();
        if (initiallyFlagged.contains(3)) {
            toCheck.addAll(Arrays.asList(1, 2, 5, 6, 7, 9));
        }
        if (initiallyFlagged.contains(9)) {
            toCheck.addAll(Arrays.asList(1, 3, 5, 6, 7, 8));
        }
        if (initiallyFlagged.containsAll(Arrays.asList(3, 9))) {
            toCheck.addAll(Arrays.asList(1, 2, 6, 7, 8));
        }
        if (initiallyFlagged.containsAll(Arrays.asList(5, 7))) {
            toCheck.add(3);
        }
        if (initiallyFlagged.containsAll(Arrays.asList(1, 5))) {
            toCheck.add(9);
        }
        if (initiallyFlagged.containsAll(Arrays.asList(1, 6))) {
            toCheck.add(9);
        }
        if (initiallyFlagged.containsAll(Arrays.asList(6, 9))) {
            toCheck.add(1);
        }
        if (initiallyFlagged.contains(1)) {
            toCheck.addAll(Arrays.asList(2, 3));
        }
        if (initiallyFlagged.contains(7)) {
            toCheck.addAll(Arrays.asList(8, 9));
        }

        // Check and flag additional subquadrants based on the rule
        for (Integer index : toCheck) {
            int i = index - 1; // Adjusting back to 0-based indexing
            double percentage = percentages.get(i);
            if (percentage >= guideValue * 0.4) {
                initiallyFlagged.add(index); // Flagging based on the rule
            }
        }

        // Final flagging including both initially and additionally flagged subquadrants
        for (Integer index : initiallyFlagged) {
            int i = index - 1; // Adjusting back to 0-based indexing
            Rect subQuadrant = subQuadrants.get(i);
            // Flagging the subquadrant
            //Imgproc.rectangle(image, subQuadrant.tl(), subQuadrant.br(), new Scalar(0, 255, 0), 2);
        }

        // Interpretation of flagged subquadrants to number
        Integer detectedNumber = mapFlaggedSubQuadrantsToNumber(initiallyFlagged);
        //System.out.println("Detected Number: " + detectedNumber);

        return detectedNumber;
    }

    private double calculateBlackPixelPercentage(Mat coloredBinaryImage, Rect subQuadrant) {
        int blackPixelCount = 0;
        int totalPixels = subQuadrant.width * subQuadrant.height;

        // iterate through each pixel
        for (int y = subQuadrant.y; y < (subQuadrant.y + subQuadrant.height); y++) {
            for (int x = subQuadrant.x; x < (subQuadrant.x + subQuadrant.width); x++) {
                double[] pixel = coloredBinaryImage.get(y, x);
                // check if pixel is black
                if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                    blackPixelCount++;
                }
            }
        }

        double blackPixelPercentage = (double) blackPixelCount / totalPixels * 100;
        // label percentage of black pixels on the image near the subQuadrant
        String label = String.format("%.2f%%", blackPixelPercentage); // Formats the percentage to 2 decimal places
        Point labelPoint = new Point(subQuadrant.x + 5, subQuadrant.y + 20);
        //Imgproc.putText(coloredBinaryImage, label, labelPoint, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 255), 1);

        return (double) blackPixelCount / totalPixels * 100;
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

        // Check against each pattern
        if (flaggedSubQuadrants.equals(patternForNumber1)) {
            return 1; // Matches pattern for Number 1
        } else if (flaggedSubQuadrants.equals(patternForNumber2)) {
            return 2; // Matches pattern for Number 2
        } else if (flaggedSubQuadrants.equals(patternForNumber3)) {
            return 3; // Matches pattern for Number 3
        } else if (flaggedSubQuadrants.equals(patternForNumber4)) {
            return 4; // Matches pattern for Number 4
        } else if (flaggedSubQuadrants.equals(patternForNumber5)) {
            return 5; // Matches pattern for Number 5
        } else if (flaggedSubQuadrants.equals(patternForNumber6)) {
            return 6; // Matches pattern for Number 6
        } else if (flaggedSubQuadrants.equals(patternForNumber7)) {
            return 7; // Matches pattern for Number 7
        } else if (flaggedSubQuadrants.equals(patternForNumber8)) {
            return 8; // Matches pattern for Number 8
        } else if (flaggedSubQuadrants.equals(patternForNumber9)) {
            return 9; // Matches pattern for Number 9
        }
        return 0; // No pattern matches - it's 0
    }


//CHECK
    private void updateResultsDisplay() {
        TextView resultsView = findViewById(R.id.results);
        StringBuilder resultsText = new StringBuilder("Results:\n");
        for (int result : arabicResults) {
            if(arabicResults.size() > 1) {
                resultsText.append(result).append(", ");
            }
            else {
                resultsText.append(result).append("");
            }
        }
        resultsView.setText(resultsText.toString());
    }
}