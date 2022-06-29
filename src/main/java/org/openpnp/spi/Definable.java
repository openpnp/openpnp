package org.openpnp.spi;

public interface Definable {
    public Definable getDefinedBy();
    
    public void setDefinedBy(Definable definable);
    
    public boolean isDefinedBy(Definable definable);
}
