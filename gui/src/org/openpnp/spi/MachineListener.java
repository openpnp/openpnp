package org.openpnp.spi;

/**
 * Provides a set of callbacks called by a Machine to notify listeners of 
 * asynchronous state changes in the Machine.
 * @author jason
 */
public interface MachineListener {
	void machineHeadActivity(Machine machine, Head head);
	
	void machineStarted(Machine machine);
	
	void machineStartFailed(Machine machine, String reason);
	
	void machineStopped(Machine machine, String reason);
	
	void machineStopFailed(Machine machine, String reason);
}
