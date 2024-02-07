package com.example.uidesign_cistercian;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Line {
    public Point pt1;
    public Point pt2;
    public Scalar color;
    public int thickness;

    public Line(Point pt1, Point pt2, Scalar color, int thickness) {
        this.pt1 = pt1;
        this.pt2 = pt2;
        this.color = color;
        this.thickness = thickness;
    }

    // Method to draw this line on a given Mat image
    public void draw(Mat image) {
        Imgproc.line(image, this.pt1, this.pt2, this.color, this.thickness);
    }

    // Method to calculate the largest black pixel stream that is uninterrupted along the line, result in percentage
    public double getBlackPixelPercentage(Mat image) {
        int maxBlackPixelStreak = 0;
        int currentStreak = 0;
        int totalPixels = 0;

        int dx = Math.abs((int)pt2.x - (int)pt1.x);
        int dy = Math.abs((int)pt2.y - (int)pt1.y);

        int sx = (int)pt1.x < (int)pt2.x ? 1 : -1;
        int sy = (int)pt1.y < (int)pt2.y ? 1 : -1;

        int err = dx - dy;
        int e2;

        int currentX = (int)pt1.x;
        int currentY = (int)pt1.y;

        while (true) {
            if(currentX >= 0 && currentX < image.cols() && currentY >= 0 && currentY < image.rows()) {
                double[] pixel = image.get(currentY, currentX);
                if (pixel != null && pixel.length > 0 && pixel[0] == 0) { // Assuming single-channel (grayscale) image for simplicity
                    currentStreak++; // Increment current streak of black pixels
                    if (currentStreak > maxBlackPixelStreak) {
                        maxBlackPixelStreak = currentStreak; // Update max streak if current is larger
                    }
                } else {
                    currentStreak = 0; // Reset current streak if pixel is not black
                }
            }
            totalPixels++;

            if (currentX == (int)pt2.x && currentY == (int)pt2.y) {
                break;
            }

            e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                currentX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currentY += sy;
            }
        }

        // Calculate percentage of the largest black pixel streak relative to the total pixels in the line
        return totalPixels > 0 ? (double) maxBlackPixelStreak / totalPixels * 100 : 0;
    }
}
