package org.openpnp.model;

import org.openpnp.model.Board.Side;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class BoardPad extends AbstractModelObject {
    public enum Type {
        Paste,
        Ignore
    }
    
    @Attribute(required=false)
    private Type type = Type.Paste;

    @Attribute
    protected Side side = Side.Top;
    
    @Element
    protected Location location = new Location(LengthUnit.Millimeters);
    
    @Attribute(required=false)
    protected String name;
    
    @Element
    protected Pad pad;
    
    public BoardPad() {
        
    }
    
    public BoardPad(Pad pad, Location location) {
        setPad(pad);
        setLocation(location);
    }
    
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        Location oldValue = this.location;
        this.location = location;
        firePropertyChange("location", oldValue, location);
    }

    public Side getSide() {
        return side;
    }
    
    public void setSide(Side side) {
        Object oldValue = this.side;
        this.side = side;
        firePropertyChange("side", oldValue, side);
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        Object oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }
    
    public Pad getPad() {
        return pad;
    }
    
    public void setPad(Pad pad) {
        Object oldValue = pad;
        this.pad = pad;
        firePropertyChange("pad", oldValue, pad);
    }
}
