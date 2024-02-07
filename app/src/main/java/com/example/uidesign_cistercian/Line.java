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
}
