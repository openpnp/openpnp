package org.openpnp.model;

import java.io.Serializable;

/**
 * Created by matt on 22/07/2017.
 */
public abstract class Curve implements Serializable
{
    protected Point from;
    protected Point to;

    public Point getFrom()
    {
        return from;
    }

    public void setFrom(Point from)
    {
        this.from = from;
    }

    public Point getTo()
    {
        return to;
    }

    public void setTo(Point to)
    {
        this.to = to;
    }

    public abstract Curve reverse();

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Curve curve = (Curve) o;

        if (from != null ? !from.equals(curve.from) : curve.from != null) return false;
        if (to != null ? !to.equals(curve.to) : curve.to != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }
}