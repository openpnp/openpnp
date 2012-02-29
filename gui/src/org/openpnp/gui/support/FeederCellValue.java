package org.openpnp.gui.support;

import org.openpnp.model.Configuration;
import org.openpnp.spi.Feeder;

public class FeederCellValue {
	private static Configuration configuration;
	private Feeder feeder;
	
	public static void setConfiguration(Configuration configuration) {
		FeederCellValue.configuration = configuration;
	}
	
	public FeederCellValue(Feeder feeder) {
		this.feeder = feeder;
	}
	
	public FeederCellValue(String value) {
		Feeder feeder = configuration.getMachine().getFeeder(value);
		if (feeder == null) {
			throw new NullPointerException();
		}
		this.feeder = feeder;
	}

	public Feeder getFeeder() {
		return feeder;
	}

	public void setFeeder(Feeder feeder) {
		this.feeder = feeder;
	}
	
	@Override
	public String toString() {
		return feeder == null ? "" : feeder.getId();
	}
}
