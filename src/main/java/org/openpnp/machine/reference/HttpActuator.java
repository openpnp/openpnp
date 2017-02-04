/*
 * Copyright (C) 2017 Sebastian Pichelhofer <sp@apertus.org> based on reference by Jason von Nieda
 * <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.HttpActuatorConfigurationWizard;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;

public class HttpActuator extends ReferenceActuator implements ReferenceHeadMountable {

    @Element(required = false)
    protected String onUrl = "";

    @Element(required = false)
    protected String offUrl = "";

    @Element(required = false)
    protected boolean on = false;

    public HttpActuator() {}

    @Override
    public void actuate(boolean on) throws Exception {
        Logger.debug("{}.actuate({})", getName(), on);
        // getDriver().actuate(this, on);
        URL obj = null;
        if (this.on && !on) {
            // fire OFF
            obj = new URL(this.offUrl);
        }
        else if (!this.on && on) {
            // fire ON
            obj = new URL(this.onUrl);
        }
        else {
            return;
        }
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        Logger.debug("{}.HTTPActuate turning: {} )", getName(), on);
        Logger.debug("{}.HTTPActuate requesting: {} )", getName(), obj.toString());
        Logger.debug("{}.HTTPActuate responseCode: {} )", getName(), responseCode);
        Logger.debug("{}.HTTPActuate response: {} )", getName(), response);
        this.on = on;

        getMachine().fireMachineHeadActivity(head);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new HttpActuatorConfigurationWizard(this);
    }

    public String getOnUrl() {
        return this.onUrl;
    }

    public void setOnUrl(String url) {
        this.onUrl = url;
        firePropertyChange("onUrl", null, this.onUrl);
    }

    public String getOffUrl() {
        return this.offUrl;
    }

    public void setOffUrl(String url) {
        this.offUrl = url;
        firePropertyChange("offUrl", null, this.offUrl);
    }
}
