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

    public Point getPt1() {
        return this.pt1;
    }

    public Point getPt2() {
        return this.pt2;
    }

    public Point getMiddlePoint() {
        double midX = (this.pt1.x + this.pt2.x) / 2;
        double midY = (this.pt1.y + this.pt2.y) / 2;
        return new Point(midX, midY);
    }

    public Point getIntersectionPoint(Line other) {
        double a1 = this.pt2.y - this.pt1.y;
        double b1 = this.pt1.x - this.pt2.x;
        double c1 = a1 * this.pt1.x + b1 * this.pt1.y;

        double a2 = other.pt2.y - other.pt1.y;
        double b2 = other.pt1.x - other.pt2.x;
        double c2 = a2 * other.pt1.x + b2 * other.pt1.y;

        double delta = a1 * b2 - a2 * b1;

        // If lines are parallel or coincident, delta will be 0; no intersection (or infinite intersections if coincident)
        if (delta == 0) {
            return null; // No intersection point or lines are coincident
        } else {
            double x = (b2 * c1 - b1 * c2) / delta;
            double y = (a1 * c2 - a2 * c1) / delta;
            return new Point(x, y);
        }
    }

    public Point getPerpendicularIntersectionPoint(Point p) {
        // Calculate slope of this line
        double slope = (this.pt2.y - this.pt1.y) / (this.pt2.x - this.pt1.x);

        // Calculate slope of the perpendicular line
        double perpSlope = -1 / slope;

        // Calculate the original line's y-intercept (b = y - mx)
        double originalIntercept = this.pt1.y - slope * this.pt1.x;

        // Calculate the perpendicular line's y-intercept
        double perpIntercept = p.y - perpSlope * p.x;

        // Solve for intersection
        // x = (perpIntercept - originalIntercept) / (slope - perpSlope)
        double x = (perpIntercept - originalIntercept) / (slope - perpSlope);
        double y = slope * x + originalIntercept;

        return new Point(x, y);
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
