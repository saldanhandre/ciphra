package com.example.uidesign_cistercian;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
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
import java.util.List;

public class ImageDisplayActivity extends AppCompatActivity {

    private Mat matImage;

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
            ;
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(contourFloat, approxCurve, epsilon, true);
            // Draw the approximated contour for visualization
//            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
//            Imgproc.drawContours(coloredBinaryImage, Arrays.asList(points), -1, new Scalar(0, 69, 181), 2);
            // Calculate bounding rectangle for each contour
            Rect boundingRect = Imgproc.boundingRect(contour);
            boundingRects.add(boundingRect);
        }

        // Filter out rectangles that are inside other rectangles
        List<Rect> filteredRects = filterContainedRectangles(boundingRects);
        drawQuadrants(coloredBinaryImage, filteredRects);
    }

    private void drawQuadrants(Mat coloredBinaryImage, List<Rect> filteredRects) {
        // Draw the bounding rectangles that passed the filter
        for (Rect rect : filteredRects) {
            // Draw the bounding rectangle
//            Imgproc.rectangle(coloredBinaryImage, rect.tl(), rect.br(), new Scalar(2, 82, 4), 2);

            // Determine the longest side of the rectangle and draw the division line
            if (rect.width > rect.height) {
                // Rectangle is wider than tall, divide left and right
                Point divisionPoint1 = new Point(rect.x, rect.y + rect.height / 2);
                Point divisionPoint2 = new Point(rect.x + rect.width, rect.y + rect.height / 2);
                Imgproc.line(coloredBinaryImage, divisionPoint1, divisionPoint2, new Scalar(0, 255, 255), 2);

                // Create and draw smaller rectangle within the left half
                int thirdWidth = rect.width / 3;
                Rect smallRect1 = new Rect(rect.x, rect.y, thirdWidth, rect.height / 2);
                Imgproc.rectangle(coloredBinaryImage, smallRect1.tl(), smallRect1.br(), new Scalar(255, 0, 0), 2);
                Rect smallRect2 = new Rect(rect.x , rect.y + rect.height / 2, thirdWidth, rect.height / 2);
                Imgproc.rectangle(coloredBinaryImage, smallRect2.tl(), smallRect2.br(), new Scalar(0, 255, 255), 2);
                Rect smallRect3 = new Rect(rect.x + rect.width, rect.y, -thirdWidth, rect.height / 2);
                Imgproc.rectangle(coloredBinaryImage, smallRect3.tl(), smallRect3.br(), new Scalar(255,255, 0), 2);
                Rect smallRect4 = new Rect(rect.x + rect.width, rect.y + rect.height / 2, -thirdWidth, rect.height / 2);
                Imgproc.rectangle(coloredBinaryImage, smallRect4.tl(), smallRect4.br(), new Scalar(255, 0, 255), 2);

            } else {
                // Rectangle is taller than wide, divide top and bottom
                Point divisionPoint1 = new Point(rect.x + rect.width / 2, rect.y);
                Point divisionPoint2 = new Point(rect.x + rect.width / 2, rect.y + rect.height);
                Imgproc.line(coloredBinaryImage, divisionPoint1, divisionPoint2, new Scalar(0, 255, 255), 2);

                // Create and draw smaller rectangle within the top half
                int thirdHeight = rect.height / 3;
                Rect smallRect1 = new Rect(rect.x + rect.width / 2, rect.y, rect.width / 2, thirdHeight);
                Imgproc.rectangle(coloredBinaryImage, smallRect1.tl(), smallRect1.br(), new Scalar(255, 0, 0), 2);
                Rect smallRect2 = new Rect(rect.x, rect.y, rect.width / 2, thirdHeight);
                Imgproc.rectangle(coloredBinaryImage, smallRect2.tl(), smallRect2.br(), new Scalar(0, 255, 255), 2);
                Rect smallRect3 = new Rect(rect.x + rect.width / 2, rect.y + rect.height, rect.width / 2, -thirdHeight);
                Imgproc.rectangle(coloredBinaryImage, smallRect3.tl(), smallRect3.br(), new Scalar(255,255, 0), 2);
                Rect smallRect4 = new Rect(rect.x, rect.y + rect.height, rect.width / 2, -thirdHeight);
                Imgproc.rectangle(coloredBinaryImage, smallRect4.tl(), smallRect4.br(), new Scalar(255, 0, 255), 2);
            }
        }
    }

    private List<Rect> filterContainedRectangles(List<Rect> rects) {
        List<Rect> filteredRects = new ArrayList<>();
        for (Rect rect1 : rects) {
            boolean isContained = false;
            for (Rect rect2 : rects) {
                if (rect1 != rect2 && rect2.contains(rect1.tl()) && rect2.contains(rect1.br())) {
                    isContained = true;
                    break;
                }
            }
            if (!isContained) {
                filteredRects.add(rect1);
            }
        }
        return filteredRects;
    }


    private void detectCorners(Mat image, MatOfPoint2f contour) {

        int maxCorners = 12; // Maximum number of corners to detect
        double qualityLevel = 0.10; // Quality level for corner detection
        double minDistance = 10; // Minimum distance between corners

        MatOfPoint corners = new MatOfPoint();

        // Convert the contour Mat to an 8-bit single channel, as required by goodFeaturesToTrack
        Mat mask = new Mat(image.size(), CvType.CV_8UC1, new Scalar(0));
        MatOfPoint intContour = new MatOfPoint(contour.toArray());
        Imgproc.fillPoly(mask, Arrays.asList(intContour), new Scalar(255));

        // Detect corners
        Imgproc.goodFeaturesToTrack(mask, corners, maxCorners, qualityLevel, minDistance, new Mat(), 3, true, 0.04);

        // Convert corners MatOfPoint to List<Point> for drawing
        List<Point> cornerPoints = corners.toList();
        // Draw circles on each corner
        for (Point corner : cornerPoints) {
            Imgproc.circle(image, corner, 5, new Scalar(255, 0, 0), -1); // Red color for corners
        }
    }


}