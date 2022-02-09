package com.app.picstalgia;

import android.graphics.Point;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import kotlin.math.MathKt;

public class Rectangle {
    private Point topLeft;
    private Point topRight;
    private Point bottomLeft;
    private Point bottomRight;

    public Rectangle(Point topLeft, Point topRight, Point bottomLeft, Point bottomRight) {
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
    }


    public Point topLeft() {
        return this.topLeft;
    }

    public Point topRight() {
        return this.topRight;
    }

    public Point bottomLeft() {
        return this.bottomLeft;
    }

    public Point bottomRight() {
        return this.bottomRight;
    }

    private float topWidth() {
        return this.distance(this.topLeft, this.topRight);
    }

    private float bottomWidth() {
        return this.distance(this.bottomLeft, this.bottomRight);
    }

    private float leftHeight() {
        return this.distance(this.topLeft, this.bottomLeft);
    }

    private float rightHeight() {
        return this.distance(this.topRight, this.bottomRight);
    }

    public List<Point> points() {
        ArrayList<Point> pnt = new ArrayList<>();
        pnt.add(this.topLeft);
        pnt.add(this.topRight);
        pnt.add(this.bottomLeft);
        pnt.add(this.bottomRight);

        return pnt;
    }

    public List<org.opencv.core.Point> points_opencv () {
        ArrayList<org.opencv.core.Point> pnt = new ArrayList<>();
        pnt.add(new org.opencv.core.Point(topLeft.x, topLeft.y));
        pnt.add(new org.opencv.core.Point(topRight.x, topRight.y));
        pnt.add(new org.opencv.core.Point(bottomLeft.x, bottomLeft.y));
        pnt.add(new org.opencv.core.Point(bottomRight.x, bottomRight.y));
        return pnt;
    }
    public float getHorizontalDistortionRatio() {
        if(this.leftHeight() > rightHeight()) {
            return leftHeight()/rightHeight();
        }
        return rightHeight()/leftHeight();
    }

    public float getVerticalDistortionRatio() {
        if(topWidth() > bottomWidth()) {
            return topWidth()/bottomWidth();
        }

        return bottomWidth()/topWidth();
    }

    public float getCircumferenceLength() {
        return topWidth() + bottomWidth() + leftHeight() + rightHeight();
    }

    public Rectangle scaled(float ratio) {
        Point tl = new Point(MathKt.roundToInt((float)this.topLeft.x * ratio ), MathKt.roundToInt((float)this.topLeft.y*ratio));
        Point tr = new Point(MathKt.roundToInt((float)this.topRight.x * ratio ), MathKt.roundToInt((float)this.topRight.y*ratio));
        Point bl = new Point(MathKt.roundToInt((float)this.bottomLeft.x * ratio ), MathKt.roundToInt((float)this.bottomLeft.y*ratio));
        Point br = new Point(MathKt.roundToInt((float)this.bottomRight.x * ratio ), MathKt.roundToInt((float)this.bottomRight.y*ratio));

        return new Rectangle(tl, tr, bl, br);
    }

    public boolean isApproximated(Rectangle rect1, Rectangle rect2, float distanceTolerance) {
        return distance(rect1.topLeft, rect2.topLeft) <= distanceTolerance
                && distance(rect1.topRight, rect2.topRight) <= distanceTolerance
                && distance(rect1.bottomLeft, rect2.bottomLeft) <= distanceTolerance
                && distance(rect1.bottomRight, rect2.bottomRight) <= distanceTolerance;
    }

    public float distance(Point start, Point end) {
        float diffX = (float)Math.abs(end.x - start.x);
        float diffY = (float)Math.abs(end.y - start.y);

        return (float)Math.sqrt((diffX * diffX + diffY * diffY));
    }

    public static Rectangle from(List<Point> points) {
        if(points.size() != 4) {
            return null;
        }
        points.sort(Comparator.comparing(point -> point.y));

        Point tl = points.get(0);
        Point tr = points.get(1);
        Point bl = points.get(2);
        Point br = points.get(3);

        if(points.get(0).x >= points.get(1).x ) {
            tl = points.get(1);
            tr = points.get(0);
        }

        if(points.get(2).x >= points.get(3).x) {
            bl = points.get(3);
            br = points.get(2);
        }

        return new Rectangle(tl, tr, bl, br);
    }
}
