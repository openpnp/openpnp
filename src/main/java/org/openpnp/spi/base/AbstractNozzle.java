package org.openpnp.spi.base;

import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.openpnp.gui.support.Icons;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public abstract class AbstractNozzle extends AbstractModelObject implements Nozzle {
    @ElementList(required = false)
    protected IdentifiableList<NozzleTip> nozzleTips = new IdentifiableList<>();

    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    protected Head head;
    
    protected Part part;

    public AbstractNozzle() {
        this.id = Configuration.createId("NOZ");
        this.name = getClass().getSimpleName();
    }

    @Override
    public List<NozzleTip> getNozzleTips() {
        return Collections.unmodifiableList(nozzleTips);
    }

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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        firePropertyChange("name", null, name);
    }
    
    @Override
    public Part getPart() {
        return part;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return Icons.captureTool;
    }

    @Override
    public void addNozzleTip(NozzleTip nozzleTip) throws Exception {
        nozzleTips.add(nozzleTip);
        fireIndexedPropertyChange("nozzleTips", nozzleTips.size() - 1, null, nozzleTip);
    }

    @Override
    public void removeNozzleTip(NozzleTip nozzleTip) {
        int index = nozzleTips.indexOf(nozzleTip);
        if (nozzleTips.remove(nozzleTip)) {
            fireIndexedPropertyChange("nozzleTips", index, nozzleTip, null);
        }
    }
}
