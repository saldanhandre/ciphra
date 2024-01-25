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
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(contourFloat, approxCurve, epsilon, true);
            // Draw the approximated contour for visualization
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
//            Imgproc.drawContours(coloredBinaryImage, Arrays.asList(points), -1, new Scalar(0, 0, 255), 2);
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
            Mat rotatedImage = coloredBinaryImage.clone(); // Clone the image for rotation
            // Draw the bounding rectangle
//            Imgproc.rectangle(coloredBinaryImage, rect.tl(), rect.br(), new Scalar(2, 82, 4), 2);

            if (rect.width > rect.height) {
                // Rotate the image 90 degrees clockwise
                Core.rotate(coloredBinaryImage, rotatedImage, Core.ROTATE_90_COUNTERCLOCKWISE);
                Rect rotatedRect = new Rect(rect.y, coloredBinaryImage.cols() - rect.x - rect.width, rect.height, rect.width);
                processRectangle(rotatedImage, rotatedRect);
                Core.rotate(rotatedImage, coloredBinaryImage, Core.ROTATE_90_CLOCKWISE);
            } else {
                processRectangle(coloredBinaryImage, rect);
            }
        }
    }

    private void processRectangle(Mat coloredBinaryImage, Rect rect) {
        // Rectangle is taller than wide, divide top and bottom
        Point divisionPoint1 = new Point(rect.x + rect.width / 2, rect.y);
        Point divisionPoint2 = new Point(rect.x + rect.width / 2, rect.y + rect.height);
        Imgproc.line(coloredBinaryImage, divisionPoint1, divisionPoint2, new Scalar(0, 255, 255), 2);

        // Create and draw smaller rectangle within the top half
        int quadrantHeight = rect.height / 3;
        Rect quadrantUnits = new Rect(rect.x + rect.width / 2, rect.y, rect.width / 2, quadrantHeight);
//        Imgproc.rectangle(coloredBinaryImage, quadrantUnits.tl(), quadrantUnits.br(), new Scalar(255, 0, 0), 2);
        drawSubQuadrantsUnits(coloredBinaryImage, quadrantUnits);
        blackPixelFinderRightToLeft(coloredBinaryImage, quadrantUnits);

        Rect quadrantTens = new Rect(rect.x, rect.y, rect.width / 2, quadrantHeight);
//        Imgproc.rectangle(coloredBinaryImage, quadrantTens.tl(), quadrantTens.br(), new Scalar(0, 255, 255), 2);
        drawSubQuadrantsTens(coloredBinaryImage, quadrantTens);
        blackPixelFinderLeftToRight(coloredBinaryImage, quadrantTens);

        Rect quadrantHundreds = new Rect(rect.x + rect.width / 2, rect.y + rect.height - quadrantHeight, rect.width / 2, quadrantHeight);
//        Imgproc.rectangle(coloredBinaryImage, quadrantHundreds.tl(), quadrantHundreds.br(), new Scalar(255,255, 0), 2);
        drawSubQuadrantsHundreds(coloredBinaryImage, quadrantHundreds);
        blackPixelFinderRightToLeft(coloredBinaryImage, quadrantHundreds);

        Rect quadrantThousands = new Rect(rect.x, rect.y + rect.height - quadrantHeight, rect.width / 2, quadrantHeight);
//        Imgproc.rectangle(coloredBinaryImage, quadrantThousands.tl(), quadrantThousands.br(), new Scalar(255, 0, 255), 2);
        drawSubQuadrantsThousands(coloredBinaryImage, quadrantThousands);
        blackPixelFinderLeftToRight(coloredBinaryImage, quadrantThousands);
    }


    private void blackPixelFinderRightToLeft(Mat originalImage, Rect rect) {
        // Clone original image to ensure contours are present
        Mat imageWithContours = originalImage.clone();

        // iterate through the rectangle in the cloned image to find the first contour line
        for (int x = rect.x+rect.width; x > rect.x; x--) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                double[] pixel = imageWithContours.get(y, x);

                if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0){
                    // Draw a vertical line at this position
                    Point lineStart = new Point(x, rect.y);
                    Point lineEnd = new Point(x, rect.y + rect.height);
                    Imgproc.line(originalImage, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                    return;
                }
            }
        }
    }

    private void blackPixelFinderLeftToRight(Mat originalImage, Rect rect) {
        // Clone original image to ensure contours are present
        Mat imageWithContours = originalImage.clone();

        // iterate through the rectangle in the cloned image to find the first contour line
        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                double[] pixel = imageWithContours.get(y, x);

                if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0){
                    // Draw a vertical line at this position
                    Point lineStart = new Point(x, rect.y);
                    Point lineEnd = new Point(x, rect.y + rect.height);
                    Imgproc.line(originalImage, lineStart, lineEnd, new Scalar(0, 0, 225), 1);
                    return;
                }
            }
        }
    }

    private void drawSubQuadrantsUnits(Mat coloredBinaryImage, Rect quadrant) {
        int subRectWidth = quadrant.width / 2;
        int subRectHeight = quadrant.height / 2;

        Rect subQuadrantUnits1 = new Rect(quadrant.x, quadrant.y, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantUnits1.tl(), subQuadrantUnits1.br(), new Scalar(255, 0, 0), 2);
        Rect subQuadrantUnits2 = new Rect(quadrant.x + subRectWidth, quadrant.y, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantUnits2.tl(), subQuadrantUnits2.br(), new Scalar(255, 0, 0), 2);
        Rect subQuadrantUnits3 = new Rect(quadrant.x, quadrant.y + subRectHeight, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantUnits3.tl(), subQuadrantUnits3.br(), new Scalar(255, 0, 0), 2);
        Rect subQuadrantUnits4 = new Rect(quadrant.x + subRectWidth, quadrant.y + subRectHeight, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantUnits4.tl(), subQuadrantUnits4.br(), new Scalar(255, 0, 0), 2);

        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantUnits1, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantUnits2, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantUnits3, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantUnits4, new Scalar(0, 0, 255));
    }

    private void drawSubQuadrantsTens(Mat coloredBinaryImage, Rect quadrant) {
        int subRectWidth = quadrant.width / 2;
        int subRectHeight = quadrant.height / 2;

        Rect subQuadrantTens1 = new Rect(quadrant.x + subRectWidth, quadrant.y, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantTens1.tl(), subQuadrantTens1.br(), new Scalar(0, 255, 255), 2);
        Rect subQuadrantTens2 = new Rect(quadrant.x, quadrant.y, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantTens2.tl(), subQuadrantTens2.br(), new Scalar(0, 255, 255), 2);
        Rect subQuadrantTens3 = new Rect(quadrant.x + subRectWidth, quadrant.y + subRectHeight, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantTens3.tl(), subQuadrantTens3.br(), new Scalar(0, 255, 255), 2);
        Rect subQuadrantTens4 = new Rect(quadrant.x, quadrant.y + subRectHeight, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantTens4.tl(), subQuadrantTens4.br(), new Scalar(0, 255, 255), 2);

        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantTens1, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantTens2, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantTens3, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantTens4, new Scalar(0, 0, 255));
    }

    private void drawSubQuadrantsHundreds(Mat coloredBinaryImage, Rect quadrant) {
        int subRectWidth = quadrant.width / 2;
        int subRectHeight = quadrant.height / 2;

        Rect subQuadrantHundreds1 = new Rect(quadrant.x, quadrant.y, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantHundreds1.tl(), subQuadrantHundreds1.br(), new Scalar(255,255, 0), 2);
        Rect subQuadrantHundreds2 = new Rect(quadrant.x + subRectWidth, quadrant.y, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantHundreds2.tl(), subQuadrantHundreds2.br(), new Scalar(255,255, 0), 2);
        Rect subQuadrantHundreds3 = new Rect(quadrant.x, quadrant.y + subRectHeight, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantHundreds3.tl(), subQuadrantHundreds3.br(), new Scalar(255,255, 0), 2);
        Rect subQuadrantHundreds4 = new Rect(quadrant.x + subRectWidth, quadrant.y + subRectHeight, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantHundreds4.tl(), subQuadrantHundreds4.br(), new Scalar(255,255, 0), 2);

        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantHundreds1, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantHundreds2, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantHundreds3, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantHundreds4, new Scalar(0, 0, 255));
    }

    private void drawSubQuadrantsThousands(Mat coloredBinaryImage, Rect quadrant) {
        int subRectWidth = quadrant.width / 2;
        int subRectHeight = quadrant.height / 2;

        Rect subQuadrantThousands1 = new Rect(quadrant.x + subRectWidth, quadrant.y, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantThousands1.tl(), subQuadrantThousands1.br(), new Scalar(255, 0, 255), 2);
        Rect subQuadrantThousands2 = new Rect(quadrant.x, quadrant.y, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantThousands2.tl(), subQuadrantThousands2.br(), new Scalar(255, 0, 255), 2);
        Rect subQuadrantThousands3 = new Rect(quadrant.x + subRectWidth, quadrant.y + subRectHeight, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantThousands3.tl(), subQuadrantThousands3.br(), new Scalar(255, 0, 255), 2);
        Rect subQuadrantThousands4 = new Rect(quadrant.x, quadrant.y + subRectHeight, subRectWidth, subRectHeight);
        Imgproc.rectangle(coloredBinaryImage, subQuadrantThousands4.tl(), subQuadrantThousands4.br(), new Scalar(255, 0, 255), 2);

        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantThousands1, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantThousands2, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantThousands3, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(coloredBinaryImage, subQuadrantThousands4, new Scalar(0, 0, 255));
    }

    private void analyzeAndLabelSubQuadrant(Mat coloredBinaryImage, Rect subQuadrant, Scalar textColor) {
        int blackPixelCount = 0;

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
        // label count of black pixels on the image near the subquadrant
        String label = String.valueOf(blackPixelCount);
        Point labelPoint = new Point(subQuadrant.x + 5, subQuadrant.y + 20); // Adjust as needed for positioning
        Imgproc.putText(coloredBinaryImage, label, labelPoint, Imgproc.FONT_HERSHEY_SIMPLEX, 1, textColor, 1);
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