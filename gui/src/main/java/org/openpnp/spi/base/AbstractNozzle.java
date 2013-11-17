package org.openpnp.spi.base;

import java.util.Collections;
import java.util.List;

import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public abstract class AbstractNozzle implements Nozzle {
    @ElementList(required=false)
    	protected IdentifiableList<NozzleTip> nozzletips = new IdentifiableList<NozzleTip>();

    @Override
    public List<NozzleTip> getNozzleTips() {
        return Collections.unmodifiableList(nozzletips);
    }
	
	@Attribute
    protected String id;
    
    protected Head head;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Head getHead() {
        return head;
    }

    @Override
    public void setHead(Head head) {
        this.head = head;
    }

}
