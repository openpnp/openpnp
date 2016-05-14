package org.openpnp.spi;

public interface HeadMountable extends Movable {
    /**
     * Gets the Head that this HeadMountable is attached to. If it is not attached to a Head this
     * method returns null.
     * 
     * @return
     */
    Head getHead();

    /**
     * Set the Head that this HeadMountable is attached to. Called by the Head when the
     * HeadMountable is added to it.
     */
    void setHead(Head head);
}
