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
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.FrameLayout;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ImageDisplayActivity extends AppCompatActivity {

    private Mat matImage;
    private int imageHeight;
    private List<Integer> arabicResults = new ArrayList<>();
    private List<Rect> finalFilteredRects = new ArrayList<>();
    private List<Rect> foundRecsAfterCountours = new ArrayList<>();

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



    private void drawQuadrants(Mat coloredBinaryImage, List<Rect> filteredRects) { //NAME FINDQUADRANTS IN THE FUTURE
        //System.out.println("drawQuadrants called");
        // Draw the bounding rectangles that passed the filter



        for (Rect rect : filteredRects) {
            Mat rotatedImage = coloredBinaryImage.clone(); // Clone the image for rotation
            int numberResult = 0;

            // Draw the bounding rectangle
            //Imgproc.rectangle(coloredBinaryImage, rect.tl(), rect.br(), new Scalar(2, 82, 4), 2);

            if (rect.width > rect.height) {
                // Rotate the image 90 degrees clockwise
                /*
                Core.rotate(coloredBinaryImage, rotatedImage, Core.ROTATE_90_CLOCKWISE);
                Rect rotatedRect = new Rect(rect.y, coloredBinaryImage.cols() - rect.x - rect.width, rect.height, rect.width);
                numberResult = processCipher(rotatedImage, rotatedRect);
                Core.rotate(rotatedImage, coloredBinaryImage, Core.ROTATE_90_COUNTERCLOCKWISE);

                 */


                // Perform the rotation with the new dimensions
                Mat provisorio = cloneAndCropImageWithPadding(coloredBinaryImage, rect, 48);
                Imgproc.cvtColor(provisorio, provisorio, Imgproc.COLOR_BGR2RGB);

                // Convert the rotated Mat to a Bitmap
                Bitmap provisorioBitmap = Bitmap.createBitmap(provisorio.cols(), provisorio.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(provisorio, provisorioBitmap);

                // Set the Bitmap to the ImageView
                ImageView imageView = findViewById(R.id.image_display_view_provisorio);
                //imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView.setImageBitmap(provisorioBitmap);

            } else {
                //System.out.println("rectangle was NOT rotated");
                numberResult = processCipher(coloredBinaryImage, rect);
            }
            arabicResults.add(numberResult);
            displayResultsOnImageOverlay(coloredBinaryImage, finalFilteredRects, arabicResults);
        }





        /*

        for (Rect rect : filteredRects) {
            Mat rotatedImage = coloredBinaryImage.clone(); // Clone the image for rotation
            int numberResult = 0;

            Point rectCenter = new Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0);
            double angle = rect.width > rect.height ? 90 : 0; // Determine if we need to rotate
            double scale = 1.0; // Keep the original scale

            // Get the rotation matrix for rotating the image around its center
            Mat rotationMatrix = Imgproc.getRotationMatrix2D(rectCenter, angle, scale);
            // Rotate the image
            Imgproc.warpAffine(coloredBinaryImage, rotatedImage, rotationMatrix, coloredBinaryImage.size());

            // After rotation, the rectangle's coordinates need to be updated or recalculated

            if (angle != 0) { // Since angle is always 90 degrees clockwise in this scenario
                // Calculate new coordinates of the rectangle after a 90-degree clockwise rotation
                int newX = rect.y;
                int newY = coloredBinaryImage.cols() - rect.x - rect.width;
                int newWidth = rect.height;
                int newHeight = rect.width;

                // Create a new Rect object with these dimensions
                Rect rotatedRect = new Rect(newX, newY, newWidth, newHeight);

                // Process the rectangle as needed
                numberResult = processCipher(rotatedImage, rotatedRect);
            } else {
                numberResult = processCipher(rotatedImage, rect);
            }
            arabicResults.add(numberResult);
            displayResultsOnImageOverlay(coloredBinaryImage, finalFilteredRects, arabicResults);
        }

         */




        // ***************** TRY TO MAKE IT DETECT DIAGONAL CIPHERS ************************



        for (Rect rect : filteredRects) {
            Line stem = findStem(coloredBinaryImage, rect);
            stem.draw(coloredBinaryImage);



            // ***************** TRY NUMBER 4 ************************



            Rect roi = new Rect(rect.x, rect.y, rect.width, rect.height); // Rectangle specifying the ROI
            Mat cipherImage = new Mat(coloredBinaryImage, roi); // Extracting the ROI from the image

            double angle = stem.getSmallestAngleFromVertical(); // Rotation angle in degrees
            System.out.println("THIS IS THE ANGLE: " + angle);

            // Calculate the center of the cipher image
            Point center = new Point(cipherImage.width() / 2.0, cipherImage.height() / 2.0);

            // Calculate new dimensions for the rotatedSize based on the angle
            double angleRad = Math.toRadians(angle);
            double cosAngle = Math.abs(Math.cos(angleRad));
            double sinAngle = Math.abs(Math.sin(angleRad));
            int newWidth = (int) Math.ceil(cipherImage.width() * cosAngle + cipherImage.height() * sinAngle);
            int newHeight = (int) Math.ceil(cipherImage.width() * sinAngle + cipherImage.height() * cosAngle);
            Size rotatedSize = new Size(newWidth, newHeight);

            // Adjust the rotation matrix for the new center (since the image size changes, the center might shift)
            Point newCenter = new Point(newWidth / 2.0, newHeight / 2.0);
            Mat rotationMatrix = Imgproc.getRotationMatrix2D(newCenter, angle, 1.0);

            // Perform the rotation with the new dimensions
            Mat rotatedCipher = new Mat();
            Imgproc.warpAffine(cipherImage, rotatedCipher, rotationMatrix, rotatedSize, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(255, 0, 0));

            // Convert the rotated Mat to a Bitmap
            Bitmap bitmapImage = Bitmap.createBitmap(rotatedCipher.cols(), rotatedCipher.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rotatedCipher, bitmapImage);

            // Set the Bitmap to the ImageView
            ImageView imageView = findViewById(R.id.image_display_view_provisorio);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setImageBitmap(bitmapImage);



            // ***************** TRY NUMBER 3 ************************

            /*


            double xLeftLimit = 0, xRightLimit = 0, yTopLimit = 0, yBottomLimit = 0;
            double angle = -stem.getSmallestAngleFromVertical(); // Rotation angle in degrees
            System.out.println("the angle is " + angle);

            List<Point> rotatedRectCorners = rotateRectangle(rect, angle);
                        // Print the coordinates of the rotated corners
                        for (Point corner : rotatedRectCorners) {
                            System.out.println("after (" + corner + ")");
                        }

            /*
            if (angle <= 0) {
                xLeftLimit = rotatedRectCorners.get(0).x;
                yTopLimit = rotatedRectCorners.get(1).y;
                xRightLimit = rotatedRectCorners.get(2).x;
                yBottomLimit = rotatedRectCorners.get(3).y;
            } else {
                yTopLimit = rotatedRectCorners.get(0).y;
                xRightLimit = rotatedRectCorners.get(1).x;
                yBottomLimit = rotatedRectCorners.get(2).y;
                xLeftLimit = rotatedRectCorners.get(3).x;
            }

             */
            /*

            if (angle <= 0) {
                xLeftLimit = rect.y + rect.height;
                yTopLimit = rect.tl();
                xRightLimit = rect.x + rect.width;
                yBottomLimit = rect.x + rect.width + rect.height;
            } else {
                yTopLimit = rotatedRectCorners.get(0).y;
                xRightLimit = rotatedRectCorners.get(1).x;
                yBottomLimit = rotatedRectCorners.get(2).y;
                xLeftLimit = rotatedRectCorners.get(3).x;
            }

            Mat rotatedImage = rotateImage(coloredBinaryImage, angle, rectMiddlePoint);
            Mat croppedImage = cropImage(rotatedImage, yTopLimit, yBottomLimit, xLeftLimit, xRightLimit);

            int guidelineRectWidth = croppedImage.cols() / 50;
            int guidelineRectHeight = croppedImage.cols() / 2;
            int topLimitY = 0, bottomLimitY = 0;
            Line topLine = null;
            Line bottomLine = null;

            // Define a
            Rect guidelineRectTop = new Rect((int)rectMiddlePoint.x - guidelineRectWidth/2, 0, guidelineRectWidth, guidelineRectHeight);
            // iterate through the rectangle to find the first black pixel from the other side
            for (int y = guidelineRectTop.y; y < guidelineRectHeight; y++) {
                for (int x = guidelineRectTop.x; x < guidelineRectTop.x + guidelineRectWidth; x++) {
                    double[] pixel = croppedImage.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        topLimitY = y;
                        topLine = new Line(new Point(0,topLimitY), new Point(croppedImage.cols()-1,topLimitY), new Scalar(0, 255, 0), 1);
                        break;
                    }
                }
                if (topLimitY != -1)
                    break; // Exit the outer loop as well after defining the top line
            }

            Rect guidelineRectBottom = new Rect((int)rectMiddlePoint.x - guidelineRectWidth/2, (int)rectMiddlePoint.y, guidelineRectWidth, guidelineRectHeight - guidelineRectHeight/2);
            for (int y = guidelineRectBottom.y + guidelineRectHeight; y > guidelineRectBottom.y; y--) {
                for (int x = guidelineRectBottom.x; x < guidelineRectBottom.x + guidelineRectWidth; x++) {
                    double[] pixel = croppedImage.get(y, x);
                    if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                        bottomLimitY = y;
                        bottomLine = new Line(new Point(0,bottomLimitY), new Point(croppedImage.cols()-1,bottomLimitY), new Scalar(0, 255, 0), 1);
                        break;
                    }
                }
                if (bottomLimitY != -1)
                    break; // Exit the outer loop as well after "drawing" the second line
            }

            int cypherRectangleHeight = bottomLimitY - topLimitY;
            int cypherRectangleWidth = 2*(cypherRectangleHeight/3);

            Rect cypherRectangle = new Rect((int)rectMiddlePoint.x - cypherRectangleWidth/2, topLimitY, cypherRectangleWidth, cypherRectangleHeight);

            */
        }
    }

    private Line findStem(Mat image, Rect rect) {
        Map<Line, Double> percentages1stCheck = new HashMap<>();
        Map<Line, Double> percentages2ndCheck = new HashMap<>();
        Map<Line, Double> percentagesTop4 = new HashMap<>();

        Point rectMiddlePoint = new Point(rect.x + rect.width / 2.00, rect.y + rect.height / 2.00);

        // Draw the bounding rectangle
        Imgproc.rectangle(image, rect.tl(), rect.br(), new Scalar(2, 82, 4), 2);

        boolean stemFound = false;
        boolean firstCheckDone = false;
        boolean secondCheckDone = false;
        boolean similarPercentages = false;

        Line stem = null;

        if (!stemFound) {
            for (int a = 0; a <= 40; a++) {
                Point divisionPoint1 = new Point(rect.x, rect.y + a * (rect.height / 40.0));
                Point divisionPoint2 = new Point(rect.x + rect.width, rect.y + rect.height - (a * (rect.height / 40.0)));
                stem = new Line(divisionPoint1, divisionPoint2, new Scalar(0, 0, 255), 1);

                double percentage = stem.getBlackPixelPercentage(image);
                percentages1stCheck.put(stem, percentage);

                if (percentage >= 90) {
                    //stem.draw(image);
                    stemFound = true;
                    break;
                }
            }
            firstCheckDone = true;
        }
        if (!stemFound && firstCheckDone) {
            for (int a = 0; a <= 40; a++) {
                Point divisionPoint1 = new Point(rect.x + rect.width - (a * (rect.width / 40.0)), rect.y);
                Point divisionPoint2 = new Point(rect.x + a * (rect.width / 40.0), rect.y + rect.height);
                stem = new Line(divisionPoint1, divisionPoint2, new Scalar(0, 0, 255), 1);

                double percentage = stem.getBlackPixelPercentage(image);
                percentages2ndCheck.put(stem, percentage);

                if (percentage >= 90) {
                    //stem.draw(image);
                    stemFound = true;
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
            //largestPercentage.getKey().draw(coloredBinaryImage);
            //secondLargestPercentage.getKey().draw(coloredBinaryImage);

            Point intersectionPoint = new Point();
            intersectionPoint = largestPercentage.getKey().getIntersectionPoint(secondLargestPercentage.getKey());

            // Create the 2 candidates for Stem, by making lines between opposite corners of the 2 largestPercentage lines
            Line stemCandidate1 = new Line(largestPercentage.getKey().getPt1(), secondLargestPercentage.getKey().getPt2(), new Scalar(255, 50, 50), 2);
            Line stemCandidate2 = new Line(secondLargestPercentage.getKey().getPt1(), largestPercentage.getKey().getPt2(), new Scalar(255, 50, 50), 2);
            if (stemCandidate1.getLength() < largestPercentage.getKey().getLength()) {
                stemCandidate1 = new Line(largestPercentage.getKey().getPt1(), secondLargestPercentage.getKey().getPt1(), new Scalar(255, 50, 50), 2);
                stemCandidate2 = new Line(secondLargestPercentage.getKey().getPt2(), largestPercentage.getKey().getPt2(), new Scalar(255, 50, 50), 2);
            }

            // draw the 2 stem candidates
            // stemCandidate1.draw(coloredBinaryImage);
            // stemCandidate2.draw(coloredBinaryImage);

            // Create a line that unites the intersection point with the candidates - stem guideline
            Line stemGuideline = new Line(stemCandidate1.getPerpendicularIntersectionPoint(intersectionPoint), stemCandidate2.getPerpendicularIntersectionPoint(intersectionPoint), new Scalar(0, 0, 255), 1);
            // draw it
            //stemGuideline.draw(coloredBinaryImage);

            Point stemMiddlePoint = stemGuideline.findMiddleBlackPixel(image);
            stem = stemGuideline.getStemLine(image, stemMiddlePoint);
            //stem.draw(image);
        }
        return stem;
    }

    public static Mat cloneAndCropImageWithPadding(Mat src, Rect rect, double angle) {
        // Clone the source image to preserve the original
        Mat clonedSrc = src.clone();

        // Crop the cloned image by the specified rectangle
        Mat croppedImage = clonedSrc.submat(rect).clone(); // Clone to detach from original image

        // Define the padding dynamically based on the dimensions of the cropped image
        int padding = Math.max(croppedImage.width(), croppedImage.height()) / 2;

        // Calculate new size with padding
        int paddedWidth = croppedImage.width() + 2 * padding;
        int paddedHeight = croppedImage.height() + 2 * padding;

        // Create a new image with the padded size
        Mat paddedImage = new Mat(paddedHeight, paddedWidth, croppedImage.type(), new Scalar(255, 255, 255));

        // Determine the ROI within the padded image where the cropped image will be placed
        int roiX = padding;
        int roiY = padding;

        // Place the cropped image in the center of the padded image
        croppedImage.copyTo(paddedImage.submat(roiY, roiY + croppedImage.height(), roiX, roiX + croppedImage.width()));

        // Calculate the center of the original rectangle within the padded image
        Point center = new Point(roiX + croppedImage.width() / 2.0, roiY + croppedImage.height() / 2.0);

        // Get the rotation matrix for the specified angle around the rectangle's center in the padded image
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, -angle, 1.0);

        // Determine the size of the rotated image (could be the same as padded image size to keep everything)
        Size rotatedSize = new Size(paddedImage.width(), paddedImage.height());

        // Perform the rotation
        Mat rotatedImage = new Mat();
        Imgproc.warpAffine(paddedImage, rotatedImage, rotationMatrix, rotatedSize);

        return rotatedImage;
    }

    public Mat rotateImage(Mat src, double angle, Point center) {
        // Get the rotation matrix for the specified angle
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);

        // Determine the size of the new image to ensure it fits the entire rotated image
        double absCos = Math.abs(rotationMatrix.get(0, 0)[0]);
        double absSin = Math.abs(rotationMatrix.get(0, 1)[0]);
        int newWidth = (int) (src.height() * absSin + src.width() * absCos);
        int newHeight = (int) (src.height() * absCos + src.width() * absSin);

        // Adjust the rotation matrix to take into account translation
        rotationMatrix.put(0, 2, rotationMatrix.get(0, 2)[0] + (newWidth / 2) - center.x);
        rotationMatrix.put(1, 2, rotationMatrix.get(1, 2)[0] + (newHeight / 2) - center.y);

        // Perform the rotation
        Mat dst = new Mat();
        Imgproc.warpAffine(src, dst, rotationMatrix, new Size(newWidth, newHeight));

        return dst;
    }

    public List<Point> rotateRectangle(Rect rectangle, double angleDegrees) {
        // Calculate the center of the rectangle
        double centerX = rectangle.x + rectangle.width / 2.0;
        double centerY = rectangle.y + rectangle.height / 2.0;

        // Convert angle from degrees to radians
        double angleRadians = Math.toRadians(angleDegrees);

        // Pre-calculate sine and cosine of the rotation angle
        double cosAngle = Math.cos(angleRadians);
        double sinAngle = Math.sin(angleRadians);

        // Define the original corners of the rectangle
        List<Point> originalCorners = new ArrayList<>();
        originalCorners.add(new Point(rectangle.x, rectangle.y)); // Top-left
        originalCorners.add(new Point(rectangle.x + rectangle.width, rectangle.y)); // Top-right
        originalCorners.add(new Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height)); // Bottom-right
        originalCorners.add(new Point(rectangle.x, rectangle.y + rectangle.height)); // Bottom-left

        // Calculate the new corners after rotation
        List<Point> rotatedCorners = new ArrayList<>();
        for (Point corner : originalCorners) {
            // Translate point to origin (center of rectangle)
            double translatedX = corner.x - centerX;
            double translatedY = corner.y - centerY;

            // Rotate point
            double rotatedX = translatedX * cosAngle - translatedY * sinAngle;
            double rotatedY = translatedX * sinAngle + translatedY * cosAngle;

            // Translate point back
            rotatedX += centerX;
            rotatedY += centerY;

            // Add rotated corner to the list
            rotatedCorners.add(new Point(rotatedX, rotatedY));
        }

        return rotatedCorners;
    }


    public Mat cropImage(Mat originalImage, double top, double bottom, double left, double right) {
        // Ensure the crop coordinates are within the image bounds and are integers
        int x = (int) Math.max(left, 0);
        int y = (int) Math.max(top, 0);
        int width = (int) Math.min(right - left, originalImage.cols() - x);
        int height = (int) Math.min(bottom - top, originalImage.rows() - y);

        // Define the rectangle for cropping
        Rect cropRect = new Rect(x, y, width, height);

        // Crop the image
        Mat croppedImage = new Mat(originalImage, cropRect);

        return croppedImage;
    }

    private Rect findSingleCipherBoundingRectInRotatedImage(Mat originalImage, double angle, Point rotationCenter) {
        // Rotate the image
        Mat rotatedImage = new Mat();
        Size imageSize = originalImage.size();
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(rotationCenter, angle, 1.0);
        Imgproc.warpAffine(originalImage, rotatedImage, rotationMatrix, imageSize, Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS);

        // Now find contours in the rotated image
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(rotatedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Assuming the target is the largest contour
        Rect boundingRect = null;
        double maxArea = 0;
        for (MatOfPoint contour : contours) {
            double contourArea = Imgproc.contourArea(contour);
            if (contourArea > maxArea) {
                maxArea = contourArea;
                boundingRect = Imgproc.boundingRect(contour);
            }
        }

        // boundingRect is now aligned with the rotated image
        return boundingRect;
    }



    private Rect findBoundingRectangleWithRadiusExpansion(Mat image, Point startPoint, int stepSize, Mat visualizationImage) {
        // Initialize variables to define the search area, starting from the startPoint
        int left = (int) startPoint.x;
        int right = (int) startPoint.x;
        int top = (int) startPoint.y;
        int bottom = (int) startPoint.y;

        boolean foundBlackPixel = true;
        Scalar boxColor = new Scalar(0, 255, 0); // color for visualization boxes

        // Continue expanding the search area as long as black pixels are found
        while (foundBlackPixel) {
            foundBlackPixel = false; // Reset flag for each expansion step

            // Temporarily store the expanded bounds
            int newLeft = Math.max(left - stepSize, 0);
            int newRight = Math.min(right + stepSize, image.cols() - 1);
            int newTop = Math.max(top - stepSize, 0);
            int newBottom = Math.min(bottom + stepSize, image.rows() - 1);

            // Draw the current bounding box for visualization
            Imgproc.rectangle(visualizationImage, new Point(newLeft, newTop), new Point(newRight, newBottom), boxColor, 2);

            // Scan the expanded area for black pixels
            for (int x = newLeft; x <= newRight; x++) {
                for (int y = newTop; y <= newBottom; y++) {
                    // Only check the perimeter of the expanded area to reduce calculations
                    if (x == newLeft || x == newRight || y == newTop || y == newBottom) {
                        double[] pixelValue = image.get(y, x);
                        if (pixelValue != null && pixelValue[0] == 0) { // Assuming a binary image where black pixels have a value of 0
                            System.out.println("Pixel Found");
                            // Update the search area bounds if a black pixel is found
                            left = newLeft;
                            right = newRight;
                            top = newTop;
                            bottom = newBottom;
                            foundBlackPixel = true;
                            break; // Exit the loop early if a black pixel is found
                        }
                    }
                }
                if (foundBlackPixel) break; // Exit the outer loop early if a black pixel is found
            }
        }

        // Calculate the width and height of the bounding rectangle
        int width = right - left + 1;
        int height = bottom - top + 1;

        // Create and return the bounding rectangle
        return new Rect(left, top, width, height);
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

    private int processCipher(Mat coloredBinaryImage, Rect rect) {
        int arabicResult = 0;
        // Find Stem
        Point divisionPoint1 = new Point(rect.x + rect.width / 2, rect.y);
        Point divisionPoint2 = new Point(rect.x + rect.width / 2, rect.y + rect.height);
        Imgproc.line(coloredBinaryImage, divisionPoint1, divisionPoint2, new Scalar(0, 255, 255), 2);

        int quadrantHeight = 4 * (rect.height / 10);
        int quadrantWidth = rect.width / 2;

        Rect quadrantUnits = new Rect(rect.x + quadrantWidth, rect.y, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantUnits.tl(), quadrantUnits.br(), new Scalar(255, 0, 0), 2);
        int unitsValue = findUnitsValue(coloredBinaryImage, quadrantUnits);

        Rect quadrantTens = new Rect(rect.x, rect.y, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantTens.tl(), quadrantTens.br(), new Scalar(0, 255, 255), 2);
        int tensValue = findTensValue(coloredBinaryImage, quadrantTens);

        Rect quadrantHundreds = new Rect(rect.x + quadrantWidth, rect.y + rect.height - quadrantHeight, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantHundreds.tl(), quadrantHundreds.br(), new Scalar(255,255, 0), 2);
        int hundredsValue = findHundredsValue(coloredBinaryImage, quadrantHundreds);

        Rect quadrantThousands = new Rect(rect.x, rect.y + rect.height - quadrantHeight, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantThousands.tl(), quadrantThousands.br(), new Scalar(255, 0, 255), 2);
        int thousandsValue = findThousandsValue(coloredBinaryImage, quadrantThousands);

        arabicResult = thousandsValue + hundredsValue + tensValue + unitsValue;
        System.out.println("ARABIC RESULT IS " + arabicResult);
        System.out.println(thousandsValue + " + " + hundredsValue + " + " + tensValue + " + " + unitsValue + " = " + arabicResult);

        //arabicResults.add(arabicResult);
        //updateResultsDisplay(); // Refresh the display with the new list of

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

    private int findUnitsValue(Mat image, Rect rect) {
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

            drawSubQuadrants(image, subQuadrantsUnits);
            unitsDigitResult = detectValidSubQuadrants(image, subQuadrantsUnits);
            //System.out.println("THE NUMBER IN UNITS IS " + unitsDigitResult);
        }
        return unitsDigitResult;
    }

// *******************************************************************************************************************

    private int findTensValue(Mat image, Rect rect) {
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

            drawSubQuadrants(image, subQuadrantsTens);
            tensDigitResult = detectValidSubQuadrants(image, subQuadrantsTens);
            //System.out.println("THE NUMBER IN TENS IS " + tensDigitResult);
        }
        return tensDigitResult * 10;
    }

// *******************************************************************************************************************

    private int findHundredsValue(Mat image, Rect rect) {
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

            drawSubQuadrants(image, subQuadrantsHundreds);
            hundredsDigitResult = detectValidSubQuadrants(image, subQuadrantsHundreds);
            //System.out.println("THE NUMBER IN HUNDREDS IS " + hundredsDigitResult);
        }
        return hundredsDigitResult * 100;
    }

// *******************************************************************************************************************

    private int findThousandsValue(Mat image, Rect rect) {
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

            drawSubQuadrants(image, subQuadrantsThousands);
            thousandsDigitResult = detectValidSubQuadrants(image, subQuadrantsThousands);
            //System.out.println("THE NUMBER IN THOUSANDS IS " + thousandsDigitResult);
        }
        return thousandsDigitResult * 1000;
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
//        Set<Integer> patternForNumber0 = new HashSet<>(Collections.emptyList());
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
//        if (flaggedSubQuadrants.equals(patternForNumber0)) {
//            return 0; // Matches pattern for Number 0
//        }
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