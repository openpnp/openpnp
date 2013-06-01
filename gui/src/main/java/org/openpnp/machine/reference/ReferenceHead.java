/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceHeadConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractHead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceHead extends AbstractHead {
    private final static Logger logger = LoggerFactory
            .getLogger(ReferenceHead.class);

    private ReferenceMachine machine;
    private ReferenceDriver driver;

    public ReferenceHead() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration)
                    throws Exception {
                machine = (ReferenceMachine) configuration.getMachine();
                driver = machine.getDriver();
            }
        });
    }

    @Override
    public void home() throws Exception {
		logger.debug("{}.home()", getId());
        driver.home(this);
        machine.fireMachineHeadActivity(this);
    }

	@Override
    public Wizard getConfigurationWizard() {
        return new ReferenceHeadConfigurationWizard(this);
    }

	@Override
	public Nozzle getNozzle(String id) {
		Nozzle result = super.getNozzle(id);
		logger.debug("{}.getNozzle({}) => {}", new Object[]{getId(), id, result});
		return result;
	}

	@Override
	public Actuator getActuator(String id) {
		Actuator result = super.getActuator(id);
		logger.debug("{}.getActuator({}) => {}", new Object[]{getId(), id, result});
		return result;
	}

	@Override
	public Camera getCamera(String id) {
		Camera result = super.getCamera(id);
		logger.debug("{}.getCamera({}) => {}", new Object[]{getId(), id, result});
		return result;
	}

	@Override
	public void addCamera(Camera camera) throws Exception {
		logger.debug("{}.addCamera({})", getId(), id);
		super.addCamera(camera);
	}

	@Override
	public void removeCamera(Camera camera) {
		logger.debug("{}.removeCamera({})", getId(), id);
		super.removeCamera(camera);
	}

	@Override
	public void moveToSafeZ(double speed) throws Exception {
		logger.debug("{}.moveToSafeZ({})", getId(), speed);
		super.moveToSafeZ(speed);
	}

	@Override
	public String toString() {
		return getId();
	}
}
