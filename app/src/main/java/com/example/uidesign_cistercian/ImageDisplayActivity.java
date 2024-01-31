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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageDisplayActivity extends AppCompatActivity {

    private Mat matImage;
    private int imageHeight;

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
        List<Rect> filteredRects = filterRectangles(boundingRects, imageHeight);
        drawQuadrants(coloredBinaryImage, filteredRects);
    }

    private void drawQuadrants(Mat coloredBinaryImage, List<Rect> filteredRects) {
        System.out.println("drawQuadrants called");
        // Draw the bounding rectangles that passed the filter

        for (Rect rect : filteredRects) {
            Mat rotatedImage = coloredBinaryImage.clone(); // Clone the image for rotation
            System.out.println("clones rotated");
            // Draw the bounding rectangle
            //Imgproc.rectangle(coloredBinaryImage, rect.tl(), rect.br(), new Scalar(2, 82, 4), 2);

            if (rect.width > rect.height) {
                // Rotate the image 90 degrees clockwise
                System.out.println("rectangle was rotated");
                Core.rotate(coloredBinaryImage, rotatedImage, Core.ROTATE_90_COUNTERCLOCKWISE);
                Rect rotatedRect = new Rect(rect.y, coloredBinaryImage.cols() - rect.x - rect.width, rect.height, rect.width);
                processRectangle(rotatedImage, rotatedRect);
                Core.rotate(rotatedImage, coloredBinaryImage, Core.ROTATE_90_CLOCKWISE);
            } else {
                System.out.println("rectangle was NOT rotated");
                processRectangle(coloredBinaryImage, rect);
            }
        }
    }

    private void processRectangle(Mat coloredBinaryImage, Rect rect) {
        // Find Stem
        Point divisionPoint1 = new Point(rect.x + rect.width / 2, rect.y);
        Point divisionPoint2 = new Point(rect.x + rect.width / 2, rect.y + rect.height);
        //Imgproc.line(coloredBinaryImage, divisionPoint1, divisionPoint2, new Scalar(0, 255, 255), 2);

        int quadrantHeight = 4 * (rect.height / 10);
        int quadrantWidth = rect.width / 2;

        Rect quadrantUnits = new Rect(rect.x + quadrantWidth, rect.y, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantUnits.tl(), quadrantUnits.br(), new Scalar(255, 0, 0), 2);
        resizingUnits(coloredBinaryImage, quadrantUnits);

        Rect quadrantTens = new Rect(rect.x, rect.y, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantTens.tl(), quadrantTens.br(), new Scalar(0, 255, 255), 2);
        resizingTens(coloredBinaryImage, quadrantTens);

        Rect quadrantHundreds = new Rect(rect.x + quadrantWidth, rect.y + rect.height - quadrantHeight, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantHundreds.tl(), quadrantHundreds.br(), new Scalar(255,255, 0), 2);
        resizingHundreds(coloredBinaryImage, quadrantHundreds);

        Rect quadrantThousands = new Rect(rect.x, rect.y + rect.height - quadrantHeight, quadrantWidth, quadrantHeight);
        //Imgproc.rectangle(coloredBinaryImage, quadrantThousands.tl(), quadrantThousands.br(), new Scalar(255, 0, 255), 2);
        resizingThousands(coloredBinaryImage, quadrantThousands);
    }

// *******************************************************************************************************************

    private void resizingUnits(Mat image, Rect rect) {
        boolean firstLineDrawn = false;
        boolean pixel1Found = false, pixel2Found = false, pixel3Found = false, pixel4Found = false;
        int leftLimitX = -1, rightLimitX = -1, bottomLimitY = -1, topLimitY = -1;
        Rect guideline1Rect = null, guideline2Rect = null, guideline3Rect = null, guideline4Rect = null;
        int subQuadrantHeight, subQuadrantWidth;


        // Guideline Rectangle 1, to find leftLimitX
        int guideline1Width = rect.width / 2;
        int guideline1Height = rect.height / 15;
        guideline1Rect = new Rect(rect.x + (rect.width / 30), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
        //Imgproc.rectangle(image, guideline1Rect.tl(), guideline1Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline1Rect != null) {
            for (int x = guideline1Rect.x; x < guideline1Rect.x + guideline1Rect.width; x++) {
                for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Rect.height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                        pixel1Found = true;
                        leftLimitX = x + (guideline1Rect.width / 35);
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
                leftLimitX = rect.x + (rect.width / 30);
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
            guideline3Rect = new Rect(leftLimitX, rect.y + rect.height - guideline3Height, guideline3Width, guideline3Height);
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

            drawSubQuadrants(image, subQuadrantUnits1, subQuadrantUnits2, subQuadrantUnits3, subQuadrantUnits4, subQuadrantUnits5, subQuadrantUnits6, subQuadrantUnits7, subQuadrantUnits8, subQuadrantUnits9);
            labelSubQuadrants(image, subQuadrantUnits1, subQuadrantUnits2, subQuadrantUnits3, subQuadrantUnits4, subQuadrantUnits5, subQuadrantUnits6, subQuadrantUnits7, subQuadrantUnits8, subQuadrantUnits9);
        }
    }

// *******************************************************************************************************************

    private void resizingTens(Mat image, Rect rect) {
        boolean firstLineDrawn = false;
        boolean pixel1Found = false, pixel2Found = false, pixel3Found = false, pixel4Found = false;
        int rightLimitX = -1, leftLimitX = -1, bottomLimitY = -1, topLimitY = -1;
        Rect guideline1Rect = null, guideline2Rect = null, guideline3Rect = null, guideline4Rect = null;
        int subQuadrantHeight, subQuadrantWidth;


        // Guideline Rectangle 1, to find rightLimitX
        int guideline1Width = rect.width / 2;
        int guideline1Height = rect.height / 15;
        guideline1Rect = new Rect(rect.x + (rect.width/2) - (rect.width/30), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
        //Imgproc.rectangle(image, guideline1Rect.tl(), guideline1Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline1Rect != null) {
            for (int x = guideline1Rect.x + guideline1Width; x > guideline1Rect.x; x--) {
                for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                        pixel1Found = true;
                        rightLimitX = x - (guideline1Rect.width / 35);
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
                rightLimitX = rect.x + rect.width - (rect.width/30);
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
            guideline3Rect = new Rect(leftLimitX, rect.y + rect.height - guideline3Height, guideline3Width, guideline3Height);
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

            drawSubQuadrants(image, subQuadrantTens1, subQuadrantTens2, subQuadrantTens3, subQuadrantTens4, subQuadrantTens5, subQuadrantTens6, subQuadrantTens7, subQuadrantTens8, subQuadrantTens9);
            labelSubQuadrants(image, subQuadrantTens1, subQuadrantTens2, subQuadrantTens3, subQuadrantTens4, subQuadrantTens5, subQuadrantTens6, subQuadrantTens7, subQuadrantTens8, subQuadrantTens9);
        }
    }

// *******************************************************************************************************************

    private void resizingHundreds(Mat image, Rect rect) {
        boolean firstLineDrawn = false;
        boolean pixel1Found = false, pixel2Found = false, pixel3Found = false, pixel4Found = false;
        int leftLimitX = -1, rightLimitX = -1, topLimitY = -1, bottomLimitY = -1;
        Rect guideline1Rect = null, guideline2Rect = null, guideline3Rect = null, guideline4Rect = null;
        int subQuadrantHeight, subQuadrantWidth;


        // Guideline Rectangle 1, to find leftLimitX
        int guideline1Width = rect.width / 2;
        int guideline1Height = rect.height / 15;
        guideline1Rect = new Rect(rect.x + (rect.width / 30), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
        //Imgproc.rectangle(image, guideline1Rect.tl(), guideline1Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline1Rect != null) {
            for (int x = guideline1Rect.x; x < guideline1Rect.x + guideline1Rect.width; x++) {
                for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Rect.height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                        pixel1Found = true;
                        leftLimitX = x + (guideline1Rect.width / 35);
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
                leftLimitX = rect.x + (rect.width / 30);
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
            guideline3Rect = new Rect(leftLimitX, rect.y, guideline3Width, guideline3Height);
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

            drawSubQuadrants(image, subQuadrantHundreds1, subQuadrantHundreds2, subQuadrantHundreds3, subQuadrantHundreds4, subQuadrantHundreds5, subQuadrantHundreds6, subQuadrantHundreds7, subQuadrantHundreds8, subQuadrantHundreds9);
            labelSubQuadrants(image, subQuadrantHundreds1, subQuadrantHundreds2, subQuadrantHundreds3, subQuadrantHundreds4, subQuadrantHundreds5, subQuadrantHundreds6, subQuadrantHundreds7, subQuadrantHundreds8, subQuadrantHundreds9);
        }
    }

// *******************************************************************************************************************

    private void resizingThousands(Mat image, Rect rect) {
        boolean firstLineDrawn = false;
        boolean pixel1Found = false, pixel2Found = false, pixel3Found = false, pixel4Found = false;
        int leftLimitX = -1, rightLimitX = -1, bottomLimitY = -1, topLimitY = -1;
        Rect guideline1Rect = null, guideline2Rect = null, guideline3Rect = null, guideline4Rect = null;
        int subQuadrantHeight, subQuadrantWidth;

        // Guideline Rectangle 1, to find rightLimitX
        int guideline1Width = rect.width / 2;
        int guideline1Height = rect.height / 15;
        guideline1Rect = new Rect(rect.x + (rect.width/2) - (rect.width/30), rect.y + (rect.height / 2) - (guideline1Height / 2), guideline1Width, guideline1Height);
        //Imgproc.rectangle(image, guideline1Rect.tl(), guideline1Rect.br(), new Scalar(150, 100, 100), 2);

        if (guideline1Rect != null) {
            for (int x = guideline1Rect.x + guideline1Width; x > guideline1Rect.x; x--) {
                for (int y = guideline1Rect.y; y < guideline1Rect.y + guideline1Height; y++) {
                    double[] pixel = image.get(y, x);
                    if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255 && !firstLineDrawn) {
                        pixel1Found = true;
                        rightLimitX = x - (guideline1Rect.width / 35);
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
                rightLimitX = rect.x + rect.width - (rect.width/30);
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
            guideline3Rect = new Rect(leftLimitX, rect.y, guideline3Width, guideline3Height);
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

            drawSubQuadrants(image, subQuadrantThousands1, subQuadrantThousands2, subQuadrantThousands3, subQuadrantThousands4, subQuadrantThousands5, subQuadrantThousands6, subQuadrantThousands7, subQuadrantThousands8, subQuadrantThousands9);
            labelSubQuadrants(image, subQuadrantThousands1, subQuadrantThousands2, subQuadrantThousands3, subQuadrantThousands4, subQuadrantThousands5, subQuadrantThousands6, subQuadrantThousands7, subQuadrantThousands8, subQuadrantThousands9);
        }
    }

// *******************************************************************************************************************

    private void drawSubQuadrants(Mat image, Rect r1, Rect r2, Rect r3, Rect r4, Rect r5, Rect r6, Rect r7, Rect r8, Rect r9) {
        Imgproc.rectangle(image, r1.tl(), r1.br(), new Scalar(255, 0, 0), 2);
        Imgproc.rectangle(image, r2.tl(), r2.br(), new Scalar(255, 0, 0), 2);
        Imgproc.rectangle(image, r3.tl(), r3.br(), new Scalar(255, 0, 0), 2);
        Imgproc.rectangle(image, r4.tl(), r4.br(), new Scalar(255, 0, 0), 2);
        Imgproc.rectangle(image, r5.tl(), r5.br(), new Scalar(255, 0, 0), 2);
        Imgproc.rectangle(image, r6.tl(), r6.br(), new Scalar(255, 0, 0), 2);
        Imgproc.rectangle(image, r7.tl(), r7.br(), new Scalar(255, 0, 0), 2);
        Imgproc.rectangle(image, r8.tl(), r8.br(), new Scalar(255, 0, 0), 2);
        Imgproc.rectangle(image, r9.tl(), r9.br(), new Scalar(255, 0, 0), 2);
    }

    private void labelSubQuadrants(Mat image, Rect r1, Rect r2, Rect r3, Rect r4, Rect r5, Rect r6, Rect r7, Rect r8, Rect r9) {
        analyzeAndLabelSubQuadrant(image, r1, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(image, r2, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(image, r3, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(image, r4, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(image, r5, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(image, r6, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(image, r7, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(image, r8, new Scalar(0, 0, 255));
        analyzeAndLabelSubQuadrant(image, r9, new Scalar(0, 0, 255));
    }

    private void analyzeAndLabelSubQuadrant(Mat coloredBinaryImage, Rect subQuadrant, Scalar textColor) {
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
        // Calculate the percentage of black pixels
        double blackPixelPercentage = (double) blackPixelCount / totalPixels * 100;

        // label percentage of black pixels on the image near the subquadrant
        String label = String.format("%.2f%%", blackPixelPercentage); // Formats the percentage to 2 decimal places
        Point labelPoint = new Point(subQuadrant.x + 5, subQuadrant.y + 20);
        Imgproc.putText(coloredBinaryImage, label, labelPoint, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, textColor, 1);
    }

    private List<Rect> filterRectangles(List<Rect> rects, int imageHeight) {

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
}