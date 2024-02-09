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

    public double getLength() {
        double dx = pt2.x - pt1.x; // Difference in x-coordinates
        double dy = pt2.y - pt1.y; // Difference in y-coordinates
        return Math.sqrt(dx * dx + dy * dy); // Distance formula
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
                if (pixel != null && pixel.length > 0 && pixel[0] == 0) { // assuming single-channel image for simplicity
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

    public Point findMiddleBlackPixel(Mat image) {
        Point startBlackPixel = null;
        Point endBlackPixel = null;

        // Bresenham's line algorithm setup
        int dx = Math.abs((int)pt2.x - (int)pt1.x);
        int dy = -Math.abs((int)pt2.y - (int)pt1.y);

        int sx = pt1.x < pt2.x ? 1 : -1;
        int sy = pt1.y < pt2.y ? 1 : -1;

        int err = dx + dy, e2; // error value e_xy

        int currentX = (int)pt1.x;
        int currentY = (int)pt1.y;

        // Iterating from pt1 to pt2 to find the first black pixel
        while (true) {
            if (image.get(currentY, currentX)[0] == 0) { // Check if the pixel is black
                startBlackPixel = new Point(currentX, currentY);
                break;
            }
            if (currentX == (int)pt2.x && currentY == (int)pt2.y) break;
            e2 = 2 * err;
            if (e2 >= dy) { err += dy; currentX += sx; }
            if (e2 <= dx) { err += dx; currentY += sy; }
        }

        // Resetting error for iteration from pt2 to pt1
        err = dx + dy;

        currentX = (int)pt2.x;
        currentY = (int)pt2.y;

        // Iterating from pt2 to pt1 to find the first black pixel
        while (true) {
            if (image.get(currentY, currentX)[0] == 0) { // Check if the pixel is black
                endBlackPixel = new Point(currentX, currentY);
                break;
            }
            if (currentX == (int)pt1.x && currentY == (int)pt1.y) break;
            e2 = 2 * err;
            if (e2 >= dy) { err += dy; currentX -= sx; }
            if (e2 <= dx) { err += dx; currentY -= sy; }
        }

        // Create line to connect start and end black pixels. This line goes from one side of the stem to the other (the short way)
        Line stemWidth = new Line(startBlackPixel, endBlackPixel, new Scalar(255, 50, 50), 2);
        // Get the middle point of that line
        Point middlePoint = stemWidth.getMiddlePoint();

        return middlePoint;
    }

    // This method returns a line that's perpendicular to this line, and crosses a specific point
    // It'll be used to find the stem, after we have the stem width line
    public Line getStemLine(Mat image, Point crossingPoint) {
        // Calculate the slope of the original line
        double slope = (this.pt2.y - this.pt1.y) / (this.pt2.x - this.pt1.x);

        // Calculate the slope of the perpendicular line
        double perpSlope = -1 / slope;

        // Assuming we want to draw a line of finite length, choose an arbitrary length (delta) for the line ends relative to the crossing point
        double delta = 200; // Adjust this value based on your image size or requirements

        // Calculate two points on the perpendicular line, using the slope and a fixed delta from the crossing point
        double x1 = crossingPoint.x - delta;
        double y1 = crossingPoint.y + perpSlope * (x1 - crossingPoint.x);

        double x2 = crossingPoint.x + delta;
        double y2 = crossingPoint.y + perpSlope * (x2 - crossingPoint.x);

        Line line = new Line(new Point(x1, y1), new Point(x2, y2), new Scalar(0, 0, 255), 2);
        return line;
    }
}
