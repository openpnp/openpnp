package org.openpnp.model;

public class Line extends Curve
{
    private Double angleToX = null;

    public Line(Point from, Point to)
    {
        this.from = from;
        this.to = to;

    }

    public Line reverse()
    {
        return new Line(to, from);
    }

    public double angleToX()
    {
        if (angleToX == null)
        {
            Point vector = to.subtract(from);
            angleToX = Math.atan2(vector.getY(), vector.getX());
            if (Math.abs(angleToX - Math.PI) < Math.PI / 360)
                angleToX = -Math.PI;
        }
        return angleToX;
    }

    public double length()
    {
        return -1; // from.distanceTo(to)
    }

    @Override
    public String toString()
    {
        return "Line{" +
                "from=" + getFrom() +
                ", to=" + getTo() +
                ", ang=" + angleToX() +
                '}';
    }
}
