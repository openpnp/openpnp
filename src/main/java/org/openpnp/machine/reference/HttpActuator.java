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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.HttpActuatorConfigurationWizard;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.util.TextUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;

public class HttpActuator extends ReferenceActuator {

    @Element(required = false)
    protected String onUrl = "";

    @Element(required = false)
    protected String offUrl = "";

    @Element(required = false)
    protected String paramUrl = "";

    @Element(required = false)
    protected String regex = "";

    // The actuation state should not be persisted.
    @Deprecated
    @Element(required = false)
    protected boolean on = false;

    // Instead we remember the last formed URL per session. 
    // Storing the formed URL instead of the value covers folding through formating/canonical form as well as
    // configuration changes. 
    protected String lastActuationUrl = null;

    @Element(required = false)
    protected String readUrl = "";

    public HttpActuator() {}

    @Override
    public boolean isDriverless() {
        return true;
    }

    @Override
    protected void driveActuation(boolean on) throws Exception {
        if (on && this.onUrl.isEmpty()) {
            driveActuation("1");
        }
        else if ((!on) && this.offUrl.isEmpty()) {
            driveActuation("0");
        }
        else {
            URL url = (on ? 
                    new URL(this.onUrl) 
                    : new URL(this.offUrl));
            actuateUrl(on, url);
        }
    }

    @Override
    protected void driveActuation(double value) throws Exception {
        String urlString = TextUtils.substituteVar(paramUrl, "val", value);
        URL url = new URL(urlString);
        actuateUrl(value, url);
    }

    @Override
    protected void driveActuation(String value) throws Exception {
        String urlString = TextUtils.substituteVar(paramUrl, "val", value);
        URL url = new URL(urlString);
        actuateUrl(value, url);
    }

    protected void actuateUrl(Object value, URL url) throws IOException, ProtocolException {
        if (this.lastActuationUrl != null 
                && this.lastActuationUrl.equals(url.toString())) {
            // URL hasn't changed: don't bother.
            return;
        }
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
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

        Logger.trace("{}.HTTPActuate value: {} )", getName(), value);
        Logger.trace("{}.HTTPActuate requesting: {} )", getName(), url.toString());
        Logger.trace("{}.HTTPActuate responseCode: {} )", getName(), responseCode);
        Logger.trace("{}.HTTPActuate response: {} )", getName(), response);
        this.lastActuationUrl = url.toString();
    }

    @Override
    public String read() throws Exception {
        if (isCoordinatedBeforeRead()) {
            coordinateWithMachine(false);
        }
        
        
        // getDriver().actuate(this, on);
        URL obj = null;
        obj = new URL(this.readUrl);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        Pattern pattern = Pattern.compile(regex);

        while ((inputLine = in.readLine()) != null) {
            Matcher matcher = pattern.matcher(inputLine);
            if (matcher.matches()) {
                String s = matcher.group("Value");
                response.append(s);
            }
            if (regex.length() == 0) {
                response.append(inputLine);
            }

        }
        in.close();

        if (isCoordinatedAfterActuate()) {
            coordinateWithMachine(true);
        }
        getMachine().fireMachineHeadActivity(head);

        return response.toString();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new HttpActuatorConfigurationWizard(getMachine(), this);
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

    public String getParamUrl() {
        return paramUrl;
    }

    public void setParamUrl(String paramUrl) {
        this.paramUrl = paramUrl;
    }

    public String getReadUrl() {
        return this.readUrl;
    }

    public void setReadUrl(String url) {
        this.readUrl = url;
        firePropertyChange("readUrl", null, this.readUrl);
    }

    public String getRegex() {
        return this.regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
        firePropertyChange("regex", null, this.regex);
    }

    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);
        if (solutions.isTargeting(Milestone.Basics)) {
            if (this.readUrl.length() > 0 && this.regex.length() == 0) {

                solutions.add(new Solutions.Issue(this,
                        "A HTTPActuator with Read URL likely needs a regular Expression to parse the value.",
                        "Set an example expression", Severity.Warning,
                        "https://github.com/openpnp/openpnp/wiki/HttpActuatorRead") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        setRegex("read:(?<Value>-?\\d+)");
                        super.setState(state);
                    }
                });
            }
        }
    }
}
