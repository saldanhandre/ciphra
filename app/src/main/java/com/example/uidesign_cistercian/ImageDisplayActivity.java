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
        Imgproc.threshold(matImage, matImage, 155, 255, Imgproc.THRESH_BINARY);
        // Apply Canny Edge Detection
        Imgproc.Canny(matImage, matImage, 100, 200);

        // Find Contours and approximate them
        findAndApproximateContours(matImage);
        // Convert processed Mat back to Bitmap
        Utils.matToBitmap(matImage, bitmap);

        // Update ImageView with the processed Bitmap
        runOnUiThread(() -> {
            ImageView imageView = findViewById(R.id.image_display_view);
            imageView.setImageBitmap(bitmap);
        });
    }

    private void findAndApproximateContours(Mat image) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(image, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // List to hold all bounding rectangles
        List<Rect> boundingRects = new ArrayList<>();

        // Iterate over all detected contours
        for (MatOfPoint contour : contours) {
            // Convert contour to a different format
            MatOfPoint2f contourFloat = new MatOfPoint2f(contour.toArray());

            // Calculate the perimeter of the contour
            double perimeter = Imgproc.arcLength(contourFloat, true);

            // Approximate the contour to a polygon
            double epsilon = 0.005 * perimeter;
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(contourFloat, approxCurve, epsilon, true);

            // Detect corners
            detectCorners(image, approxCurve);

            // Draw the approximated contour for visualization
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
            Imgproc.drawContours(image, Collections.singletonList(points), -1, new Scalar(255, 0, 0), 2);

            // Calculate bounding rectangle for each contour
            Rect boundingRect = Imgproc.boundingRect(contour);
            boundingRects.add(boundingRect);
        }

        // Filter out rectangles that are inside other rectangles
        List<Rect> filteredRects = filterContainedRectangles(boundingRects);

        // Draw the bounding rectangles that passed the filter
        for (Rect rect : filteredRects) {
            // Draw the bounding rectangle
            Imgproc.rectangle(image, rect.tl(), rect.br(), new Scalar(255, 0, 0), 2);
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
        double qualityLevel = 0.01; // Quality level for corner detection
        double minDistance = 10; // Minimum distance between corners

        MatOfPoint corners = new MatOfPoint();
        Mat img = new Mat();

        // Detect corners
        Imgproc.goodFeaturesToTrack(img, corners, maxCorners, qualityLevel, minDistance, new Mat(), 3, false, 0.04);

        // Draw circles at detected corner points
        List<Point> cornerPoints = corners.toList();
        for (Point corner : cornerPoints) {
            Imgproc.circle(image, corner, 5, new Scalar(255, 0, 0), -1); // Red color for corners
        }
    }


}