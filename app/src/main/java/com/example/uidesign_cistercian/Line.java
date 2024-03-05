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
        if (pt1 == null && pt2 == null) {
            throw new IllegalArgumentException("Point objects pt1 and pt2 are null.");
        } else if (pt1 == null && pt2 != null) {
            throw new IllegalArgumentException("Point pt1 is null.");
        } else if (pt1 != null && pt2 == null) {
            throw new IllegalArgumentException("Point pt2 is null.");
        }
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

    public double getSmallestAngleFromVertical() {
        // Calculate the differences in the x and y coordinates
        double dx = pt2.x - pt1.x;
        double dy = pt2.y - pt1.y;

        // Calculate the angle of the line with respect to the horizontal axis in radians
        double angleRadians = Math.atan2(dy, dx);

        // Convert the angle to degrees
        double angleDegrees = Math.toDegrees(angleRadians);

        // Calculate the angle's complement with respect to the vertical axis
        double angleFromVertical = 90 - Math.abs(angleDegrees);

        // Check the slope direction
        // If the slope is negative (dx and dy have opposite signs), adjust the angle
        if (dx * dy > 0) { // Negative slope: dx and dy have opposite signs
            angleFromVertical = 360 - Math.abs(angleFromVertical);
        }

        // Ensure the angle is within the [0, 360] range
        angleFromVertical = angleFromVertical % 360;

        return angleFromVertical;
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
    public double getUninterruptedBlackPixelPercentage(Mat image) {
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
                if (pixel != null && pixel.length > 0 && pixel[0] < 10 && pixel[1] < 10 && pixel[2] < 10) { // assuming single-channel image for simplicity OU NAO
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

    public double getBlackPixelPercentage(Mat image) {
        int totalBlackPixels = 0;
        int totalPixels = 0;

        int dx = Math.abs((int)pt2.x - (int)pt1.x);
        int dy = Math.abs((int)pt2.y - (int)pt1.y);

        int sx = (int)pt1.x < (int)pt2.x ? 1 : -1;
        int sy = (int)pt1.y < (int)pt2.y ? 1 : -1;

        int err = dx - dy;

        int currentX = (int)pt1.x;
        int currentY = (int)pt1.y;

        while (true) {
            if (currentX >= 0 && currentX < image.cols() && currentY >= 0 && currentY < image.rows()) {
                double[] pixel = image.get(currentY, currentX);
                // Check if pixel is black in a 3-channel image
                if (pixel != null && pixel.length == 3 && pixel[0] <= 5 && pixel[1] <= 5 && pixel[2] <= 5) {
                    totalBlackPixels++; // Increment total black pixels count
                }
            }
            totalPixels++;

            if (currentX == (int)pt2.x && currentY == (int)pt2.y) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                currentX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currentY += sy;
            }
        }

        // Calculate and return the percentage of the line covered by black pixels
        return totalPixels > 0 ? (double) totalBlackPixels / totalPixels * 100.0 : 0;
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

        // If either startBlackPixel or endBlackPixel is null, return the midpoint of the line
        if (startBlackPixel == null || endBlackPixel == null) {
            return getMiddlePoint();
        } else {
            // Create line to connect start and end black pixels. This line goes from one side of the stem to the other (the short way)
            Line stemWidth = new Line(startBlackPixel, endBlackPixel, new Scalar(255, 50, 50), 2);
            // Get the middle point of that line
            Point middlePoint = stemWidth.getMiddlePoint();
            return middlePoint;
        }
    }

    // This method returns a line that's perpendicular to this line, and crosses a specific point
    // It'll be used to find the stem, after we have the stem width line
    public Line getStemLine(Point crossingPoint) {
        // Calculate the slope of the original line
        double slope = (this.pt2.y - this.pt1.y) / (this.pt2.x - this.pt1.x);

        // Calculate the slope of the perpendicular line
        double perpSlope = -1 / slope;

        // Assuming we want to draw a line of finite length, choose an arbitrary length (delta) for the line ends relative to the crossing point
        double delta = 10; // Adjust this value based on your image size or requirements

        // Calculate two points on the perpendicular line, using the slope and a fixed delta from the crossing point
        double x1 = crossingPoint.x - delta;
        double y1 = crossingPoint.y + perpSlope * (x1 - crossingPoint.x);

        double x2 = crossingPoint.x + delta;
        double y2 = crossingPoint.y + perpSlope * (x2 - crossingPoint.x);

        Line line = new Line(new Point(x1, y1), new Point(x2, y2), new Scalar(0, 0, 255), 2);
        return line;
    }


    // this method trims the line so that it starts and ends in a black pixel.
    // it is used to trim the stem candidates for accuracy.
    public Line trimToBlackPixels(Mat image) {
        // Define starting and ending points for search
        Point startSearchPoint = this.pt1;
        Point endSearchPoint = this.pt2;
        Point middlePoint = this.getMiddlePoint();

        // Initialize variables to hold the first black pixels found from each end
        Point firstBlackPixelFromStart = null;
        Point firstBlackPixelFromEnd = null;

        // Initialize a boolean to indicate if the middle point has been reached
        boolean reachedMiddleFromStart = false;
        boolean reachedMiddleFromEnd = false;

        // Set up for Bresenham's line algorithm
        int dx = Math.abs((int)(pt2.x - pt1.x));
        int dy = -Math.abs((int)(pt2.y - pt1.y));
        int sx = pt1.x < pt2.x ? 1 : -1;
        int sy = pt1.y < pt2.y ? 1 : -1;
        int err = dx + dy;

        // Start from pt1, moving towards pt2
        int x = (int)startSearchPoint.x;
        int y = (int)startSearchPoint.y;
        while (!reachedMiddleFromStart) {
            if (image.get(y, x)[0] == 0) { // Assuming black pixel check for single-channel image
                firstBlackPixelFromStart = new Point(x, y);
                break;
            }
            if (x == (int)middlePoint.x && y == (int)middlePoint.y) {
                reachedMiddleFromStart = true;
            }

            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x += sx; }
            if (e2 <= dx) { err += dx; y += sy; }
        }

        // Reset for search from pt2 to pt1
        err = dx + dy;
        x = (int)endSearchPoint.x;
        y = (int)endSearchPoint.y;
        while (!reachedMiddleFromEnd) {
            if (image.get(y, x)[0] == 0) { // Assuming black pixel check for single-channel image
                firstBlackPixelFromEnd = new Point(x, y);
                break;
            }
            if (x == (int)middlePoint.x && y == (int)middlePoint.y) {
                reachedMiddleFromEnd = true;
            }

            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x -= sx; }
            if (e2 <= dx) { err += dx; y -= sy; }
        }

        // If black pixels were found before reaching the middle from both ends, create a new line
        if (firstBlackPixelFromStart != null && firstBlackPixelFromEnd != null) {
            return new Line(firstBlackPixelFromStart, firstBlackPixelFromEnd, this.color, this.thickness);
        } else {
            // If no black pixel is found from either end before the middle, keep the original line
            return this;
        }
    }

    public Line rearrangeLine(Mat image) {
        Point midPoint = getMiddlePoint();
        Point oneThirdPoint = new Point(pt1.x + (pt2.x - pt1.x) / 3, pt1.y + (pt2.y - pt1.y) / 3);
        Point twoThirdsPoint = new Point(pt1.x + 2 * (pt2.x - pt1.x) / 3, pt1.y + 2 * (pt2.y - pt1.y) / 3);
        double lineLength = getLength();
        double searchDistance = lineLength / 4; // Search distance in the perpendicular direction

        Point[] resultPoints = new Point[6];

        // Process each third of the line
        resultPoints[0] = findAdjustedPoint(image, pt1, oneThirdPoint, searchDistance, true);
        resultPoints[1] = findAdjustedPoint(image, oneThirdPoint, pt1, searchDistance, false);
        resultPoints[2] = findAdjustedPoint(image, oneThirdPoint, twoThirdsPoint, searchDistance, true);
        resultPoints[3] = findAdjustedPoint(image, twoThirdsPoint, oneThirdPoint, searchDistance, false);
        resultPoints[4] = findAdjustedPoint(image, twoThirdsPoint, pt2, searchDistance, true);
        resultPoints[5] = findAdjustedPoint(image, pt2, twoThirdsPoint, searchDistance, false);

//        System.out.println("                         resultPoints[0] = " + resultPoints[0]);
//        System.out.println("                         resultPoints[1] = " + resultPoints[1]);
//        System.out.println("                         resultPoints[2] = " + resultPoints[2]);
//        System.out.println("                         resultPoints[3] = " + resultPoints[3]);
//        System.out.println("                         resultPoints[4] = " + resultPoints[4]);
//        System.out.println("                         resultPoints[5] = " + resultPoints[5]);

        Line lineOfBestFit = findLineOfBestFit(resultPoints);

        // Calculate the trimmed line so it's the same length as the original and centered at the same midpoint
        Line trimmedAndCenteredLine = trimAndCenterLine(lineOfBestFit, lineLength, midPoint);

        return trimmedAndCenteredLine;
    }

    private Line trimAndCenterLine(Line line, double targetLength, Point targetMidPoint) {
        double angle = Math.atan2(line.pt2.y - line.pt1.y, line.pt2.x - line.pt1.x);

        // Calculate new endpoints based on target length and angle
        Point newPt1 = new Point(targetMidPoint.x - Math.cos(angle) * targetLength / 2,
                targetMidPoint.y - Math.sin(angle) * targetLength / 2);
        Point newPt2 = new Point(targetMidPoint.x + Math.cos(angle) * targetLength / 2,
                targetMidPoint.y + Math.sin(angle) * targetLength / 2);

        return new Line(newPt1, newPt2, line.color, line.thickness);
    }

    private Point findAdjustedPoint(Mat image, Point start, Point end, double searchDistance, boolean forward) {
        Point whitePixelStart = findWhitePixelStreamStart(image, start, end, forward);
        //System.out.println("whitePixelStart = " + whitePixelStart);

        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double length = Math.sqrt(dx * dx + dy * dy);

        // Calculate perpendicular direction unit vectors
        double perpDx = dy / length;
        double perpDy = -dx / length;

        // Try analyzing each perpendicular side separately
        Point result = analyzePerpendicularSide(image, whitePixelStart, perpDx, perpDy, searchDistance, true);
        if (result == null) { // If no suitable point is found on the first side, check the other side
            result = analyzePerpendicularSide(image, whitePixelStart, perpDx, perpDy, searchDistance, false);
        }

        // If no black pixel is found in both directions, return the initial white pixel start point
        if (result == null) {
            System.out.println("No black pixel found perpendicularly, returning initial white pixel");
            return whitePixelStart;
        } else {
            return result;
        }
    }


    private Point analyzePerpendicularSide(Mat image, Point start, double perpDx, double perpDy, double searchDistance, boolean firstSide) {
        Point currentPoint = new Point(start.x, start.y);
        Point firstBlackPixelFound = null;
        Point lastWhitePixelAfterBlack = null;
        int sign = firstSide ? -1 : 1; // Determine direction of analysis

        int consecutiveBlackPixels = 0; // Track consecutive black pixels
        double distance;

        // Start searching for black pixels
        for (distance = 0; distance <= searchDistance; distance += Math.sqrt(perpDx * perpDx + perpDy * perpDy)) {
            double[] pixel = image.get((int) currentPoint.y, (int) currentPoint.x);

            if (pixel != null && pixel[0] < 10 && pixel[1] < 10 && pixel[2] < 10) { // Black pixel
                consecutiveBlackPixels++;
                if (consecutiveBlackPixels == 5) {
                    // When the 5th consecutive black pixel is found, mark this position.
                    firstBlackPixelFound = new Point(currentPoint.x, currentPoint.y);
                    break; // Exit the loop after finding the 5th consecutive black pixel.
                }
            } else {
                consecutiveBlackPixels = 0; // Reset on non-black pixel
            }

            // Update current point for next iteration
            currentPoint.x += sign * perpDx;
            currentPoint.y += sign * perpDy;
        }

        // Continue searching for a white pixel after finding black sequence
        if (firstBlackPixelFound != null) {
            for (; distance <= searchDistance; distance += Math.sqrt(perpDx * perpDx + perpDy * perpDy)) {
                double[] pixel = image.get((int) currentPoint.y, (int) currentPoint.x);
                if (pixel != null && pixel[0] > 245 && pixel[1] > 245 && pixel[2] > 245) { // White pixel
                    lastWhitePixelAfterBlack = new Point(currentPoint.x, currentPoint.y);
                    break; // Found white pixel, exit loop
                }
                // Update current point for next iteration
                currentPoint.x += sign * perpDx;
                currentPoint.y += sign * perpDy;
            }
        }

        // Calculate and return the midpoint or relevant point based on findings
        if (lastWhitePixelAfterBlack != null) {
            Line guideline = new Line(firstBlackPixelFound, lastWhitePixelAfterBlack, new Scalar(0, 255, 0), 1);
            guideline.draw(image);
            return guideline.findMiddleBlackPixel(image);
        } else if (firstBlackPixelFound != null) {
            return firstBlackPixelFound; // Return start of black sequence if no white found
        }

        // No sequence found, return null
        return null;
    }

    private Point findWhitePixelStreamStart(Mat image, Point start, Point end, boolean forward) {
        // This method iterate from start to end (or end to start based on 'forward') to find a stream of 5 white pixels
        int streamLength = 0;
        Point currentPoint = new Point(start.x, start.y);

        // Calculate line properties
        double deltaX = end.x - start.x;
        double deltaY = end.y - start.y;
        double steps = Math.max(Math.abs(deltaX), Math.abs(deltaY));
        double xIncrement = deltaX / steps;
        double yIncrement = deltaY / steps;

        // Iterate over the line
        for (int i = 0; i <= steps; i++) {
            // Get the current pixel
            double[] pixel = image.get((int)currentPoint.y, (int)currentPoint.x);
            if (pixel != null && pixel.length >= 3 && pixel[0] > 245 && pixel[1] > 245 && pixel[2] > 245) { // Check if the pixel is white
                streamLength++;
                if (streamLength == 1) { // Mark the start of a potential white pixel stream
                    start = new Point(currentPoint.x, currentPoint.y);
                }
                if (streamLength == 10) { // Found a stream of 5 white pixels
                    end = new Point(currentPoint.x, currentPoint.y);
                    return end;
                }
            } else {
                streamLength = 0; // Reset if the current pixel is not white
            }

            // Move to the next pixel
            if (forward) {
                currentPoint.x += xIncrement;
                currentPoint.y += yIncrement;
            } else {
                currentPoint.x -= xIncrement;
                currentPoint.y -= yIncrement;
            }
        }

        // If no stream of 5 white pixels is found, return the initial iteration point
        return start;
    }


    public Line findLineOfBestFit(Point[] points) {
        if (points == null || points.length < 2) {
            throw new IllegalArgumentException("At least two points are required.");
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = points.length;

        for (Point p : points) {
            sumX += p.x;
            sumY += p.y;
            sumXY += p.x * p.y;
            sumX2 += p.x * p.x;
        }

        double xMean = sumX / n;
        double yMean = sumY / n;

        // Calculate the slope (m) and y-intercept (b) of the line
        double m = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double b = yMean - m * xMean;

        // Determine two points on the line for the minimum and maximum x values
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        for (Point p : points) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
        }

        // Calculate corresponding y values on the line for minX and maxX
        Point startPoint = new Point(minX, m * minX + b);
        Point endPoint = new Point(maxX, m * maxX + b);

        // Create and return the line of best fit
        return new Line(startPoint, endPoint, new Scalar(255, 0, 255), 1);
    }
}
