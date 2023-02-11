/*
 * Copyright (C) 2022 <mark@makr.zone>
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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

package org.openpnp.machine.reference.solutions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.model.Configuration;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.model.Solutions.Subject;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Machine;
import org.openpnp.util.TextUtils;
import org.pmw.tinylog.Logger;

/**
 * This helper class implements the Issues & Solutions for the ReferenceHead. 
 * The idea is not to pollute the head implementation itself.
 *
 */
public class ActuatorSolutions implements Solutions.Subject {
    private final Actuator actuator;
    private Machine machine;

    public ActuatorSolutions(Actuator actuator) {
        this.actuator = actuator;
        this.machine = Configuration.get().getMachine();
    }

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Basics)) {
            // No global issues so far (they are triggered from where the actuator is assigned). 
        }
    }

    public static void findActuateIssues(Solutions solutions, Solutions.Subject holder, Actuator actuator, String qualifier, String uri) {
        if (actuator == null) {
            solutions.add(new Solutions.PlainIssue(
                    holder, 
                    holder.getSubjectText()+" is missing a "+qualifier+" actuator.", 
                    "Create and assign a "+qualifier+" actuator as described in the Wiki.", 
                    Severity.Warning,
                    uri));
        }
        else {
            switch (actuator.getValueType()) {
                case Double: {
                    HashMap<CommandType, String []> suggestions = new HashMap<>();
                    suggestions.put(CommandType.ACTUATE_DOUBLE_COMMAND, new String[] { 
                            "M\u00BF \u00BF{DoubleValue:%.4f}; actuate "+qualifier+" with double value",
                            "M\u00BF \u00BF{IntegerValue}; actuate "+qualifier+" with integer value",
                    });
                    new ActuatorSolutions(actuator).findActuatorIssues(solutions, holder, qualifier, 
                            new CommandType[] { CommandType.ACTUATE_DOUBLE_COMMAND }, suggestions, 
                            uri);
                    break;
                }
                case Boolean: {
                    HashMap<CommandType, String []> suggestions = new HashMap<>();
                    suggestions.put(CommandType.ACTUATE_BOOLEAN_COMMAND, new String[] { 
                            "{True:M\u00BF ; actuate "+qualifier+" ON}\n"
                                    + "{False:M\u00BF ; actuate "+qualifier+" OFF}"
                    });
                    new ActuatorSolutions(actuator).findActuatorIssues(solutions, holder, qualifier, 
                            new CommandType[] { CommandType.ACTUATE_BOOLEAN_COMMAND }, suggestions, 
                            uri);
                    break;
                }
                case String: {
                    HashMap<CommandType, String []> suggestions = new HashMap<>();
                    suggestions.put(CommandType.ACTUATE_STRING_COMMAND, new String[] { 
                            "{StringValue} ; actuate "+qualifier+" with direct string command value",
                            "M\u00BF \u00BF{StringValue} ; actuate "+qualifier+" with string value argument"
                    });
                    new ActuatorSolutions(actuator).findActuatorIssues(solutions, holder, qualifier, 
                            new CommandType[] { CommandType.ACTUATE_STRING_COMMAND }, suggestions, 
                            uri);
                    break;
                }
                case Profile: {
                    // Recurse into single profile actuators, but only when the holder is not itself an actuator (prevent endless recursion).
                    if (!(holder instanceof Actuator)) {
                        for (Actuator profileActuator : ((ReferenceActuator) actuator).getActuatorProfiles().getAll()) {
                            if (profileActuator != actuator) {
                                findActuateIssues(solutions, actuator, profileActuator, qualifier, uri); 
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    public static void findActuatorReadIssues(Solutions solutions, Solutions.Subject holder, Actuator actuator,
            String qualifier, String uri) {
        HashMap<CommandType, String []> suggestions = new HashMap<>();
        suggestions.put(CommandType.ACTUATOR_READ_COMMAND, new String[] { "M105 ; read inputs" });
        suggestions.put(CommandType.ACTUATOR_READ_REGEX, new String[] { 
                ".*\u00BF:(?<Value>-?\\d+).*",
                ".*\u00BF:(?<Value>-?\\d+\\.\\d+).*" 
        });
        new ActuatorSolutions(actuator).findActuatorIssues(solutions, holder, qualifier, 
                new CommandType[] { CommandType.ACTUATOR_READ_COMMAND, CommandType.ACTUATOR_READ_REGEX }, suggestions, 
                uri);
    }

    protected void findActuatorIssues(Solutions solutions, Solutions.Subject holder, String qualifier,
            CommandType[] commandTypes, Map<CommandType, String[]> suggestions, String uri) {
        if (actuator == null) {
            solutions.add(new Solutions.PlainIssue(
                    holder, 
                    holder.getSubjectText()+" is missing a "+qualifier+" actuator.", 
                    "Create and assign a "+qualifier+" actuator as described in the Wiki.", 
                    Severity.Warning,
                    uri));
        }
        else if (actuator.getDriver() == null) {
            if (!actuator.isDriverless()) {solutions.add(new Solutions.PlainIssue(
                    actuator, 
                    "The "+qualifier+" actuator "+actuator.getName()+" has no driver assigned.", 
                    "Assign a driver as described in the Wiki.", 
                    Severity.Warning,
                    "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Actuators#driver-assignment"));
            }
        }
        else if (actuator.getDriver() instanceof GcodeDriver) {
            GcodeDriver driver =  (GcodeDriver) actuator.getDriver();
            for (CommandType commandType : commandTypes) {
                if (driver.getCommand(actuator, commandType) == null) {
                    solutions.add(new ActuatorGcodeIssue(
                            driver,
                            driver,
                            holder,
                            qualifier,
                            commandType, 
                            (suggestions != null ? suggestions.get(commandType) : null), 
                            "The "+qualifier+" actuator "+actuator.getName()+" has no "+commandType+" assigned.", 
                            "Assign the "+(commandType.name().contains("REGEX") ? "regular expression":"command")
                            +" to driver "+driver.getName()+" as described in the Wiki.", 
                            Severity.Warning,
                            uri));
                }
            }
        }
    }

    protected class ActuatorGcodeIssue extends Solutions.Issue {
        private final String oldGcode;
        private final GcodeDriver driver;
        private final Solutions.Subject holder; 
        private final CommandType commandType;
        private final String[] suggestions;
        private final String suggestionToolTip;
        private final String qualifier;

        private String newGcode;

        public ActuatorGcodeIssue(Subject subject, GcodeDriver driver, Solutions.Subject holder, String qualifier, 
                CommandType commandType, String[] suggestions, 
                String issue, String solution, Severity severity, String uri) {
            super(subject, issue, solution, severity, uri);
            this.driver = driver;
            this.holder = holder;
            this.qualifier = qualifier;
            this.commandType = commandType;
            this.suggestions = suggestions;
            oldGcode = driver.getCommand(actuator, commandType);
            newGcode = (oldGcode != null ? oldGcode : "");
            suggestionToolTip = "<html>"
                    + "Suggested templates for the <code>"+commandType+"</code>.<br/><br/>\n"
                    + "<strong>CAUTION:</strong> You may need to adapt these to your specific <br/>\n"
                    + holder.getSubjectText()+", controller and/or "+qualifier+".<br/>\n"
                    + "Replace placeholders \u00BF with the proper numbers/letters/designators."
                    + "</html>";
        }

        @Override
        public void setState(Solutions.State state) throws Exception {
            if (state == State.Solved) {
                if (newGcode.isEmpty()) {
                    throw new Exception(commandType+" must not be empty");
                }
                if (newGcode.contains("\u00BF")) {
                    throw new Exception(commandType+" still contains placeholders \u00BF. "
                            + "Please replace with the proper digits or letters specific to your "
                            + "controller/"+qualifier+" configuration.");
                }
                driver.setCommand(actuator, commandType, newGcode);
            }
            else {
                driver.setCommand(actuator, commandType, oldGcode);
            }
            super.setState(state);
        }

        @Override
        public Solutions.Issue.CustomProperty[] getProperties() {
            if (commandType == CommandType.ACTUATE_BOOLEAN_COMMAND) {
                // Provide true and false properties. 
                return new Solutions.Issue.CustomProperty[] {
                        booleanCommandProperty(true),
                        booleanCommandProperty(false),
                };
            }
            else {
                return new Solutions.Issue.CustomProperty[] {
                        new Solutions.Issue.MultiLineTextProperty(
                                commandType.toString(),
                                "The "+commandType+" for the "+actuator.getName()+".") {

                            @Override
                            public String get() {
                                return newGcode;
                            }
                            @Override
                            public void set(String value) {
                                newGcode = value;
                            }
                            @Override
                            public String[] getSuggestions() {
                                return suggestions;
                            }
                            @Override
                            public String getSuggestionToolTip() {
                                return suggestionToolTip;
                            }
                        }
                };
            }
        }

        protected MultiLineTextProperty booleanCommandProperty(boolean on) {
            return new Solutions.Issue.MultiLineTextProperty(
                    "<html>"+(on ? "ON":"OFF")+"-Switching<br/>"+commandType.toString()+"</html>",
                    "The "+(on ? "ON":"OFF")+"-switching "+commandType+" for the "+actuator.getName()+".") {

                @Override
                public String get() {
                    return extractBooleanCommand(on, newGcode);
                }
                @Override
                public void set(String value) throws Exception {
                    if (on) {
                        newGcode = composeBooleanCommand(value, extractBooleanCommand(!on, newGcode));
                    }
                    else {
                        newGcode = composeBooleanCommand(extractBooleanCommand(!on, newGcode), value);
                    }
                    //Logger.trace(newGcode);
                }
                @Override
                public String[] getSuggestions() {
                    if (suggestions == null) {
                        return null;
                    }
                    ArrayList<String> list = new ArrayList<>();
                    for (String suggestion : suggestions) {
                        list.add(extractBooleanCommand(on, suggestion));
                    }
                    return list.toArray(new String[list.size()]);
                }
                @Override
                public String getSuggestionToolTip() {
                    return suggestionToolTip;
                }
            };
        }

        protected String extractBooleanCommand(boolean on, String command) {
            command = TextUtils.substituteVar(command, "True", on ? on : null);
            command = TextUtils.substituteVar(command, "False", on ? null : on);
            // Get rid of empty lines.
            ArrayList<String> subCommand = new ArrayList<>();
            Arrays.stream(command.split("\\r?\\n")).forEach(line -> {
                if (!line.isEmpty()) {
                    subCommand.add(line);
                }
            });
            return String.join("\n", subCommand);
        }

        protected String composeBooleanCommand(String commandOn, String commandOff) {
            // Reconstruct True/False composition.
            // Split into lines.
            ArrayList<String> commandLinesOn = new ArrayList<>();
            Arrays.stream(commandOn.split("\\r?\\n")).forEach(line -> {
                commandLinesOn.add(line);
            });
            ArrayList<String> commandLinesOff = new ArrayList<>();
            Arrays.stream(commandOff.split("\\r?\\n")).forEach(line -> {
                commandLinesOff.add(line);
            });
            // Pad with empty lines.
            while (commandLinesOn.size() < commandLinesOff.size()) {
                commandLinesOn.add("");
            }
            while (commandLinesOff.size() < commandLinesOn.size()) {
                commandLinesOff.add("");
            }
            // Compose lines.
            ArrayList<String> commandLines = new ArrayList<>();
            for (int i = 0; i < commandLinesOn.size(); i++) {
                String lineOn = commandLinesOn.get(i);
                String lineOff = commandLinesOff.get(i);
                int minLen = Math.min(lineOn.length(), lineOff.length());
                int leftEqual;
                for (leftEqual = 0; leftEqual < minLen; leftEqual++) {
                    if (!lineOn.substring(0, leftEqual+1).equals(lineOff.substring(0, leftEqual+1))) {
                        break;
                    }
                }
                int rightEqual;
                for (rightEqual = 0; rightEqual < minLen - leftEqual; rightEqual++) {
                    if (!lineOn.substring(lineOn.length()-rightEqual-1).equals(lineOff.substring(lineOff.length()-rightEqual-1))) {
                        break;
                    }
                }
                if (leftEqual + rightEqual < minLen/2) {
                    // Line differs too much, switch the full line.
                    leftEqual = 0;
                    rightEqual = 0;
                }
                String line = lineOn.substring(0, leftEqual);
                String onPart = lineOn.substring(leftEqual, lineOn.length()-rightEqual).replace("{", "<((").replace("}", "))>");
                String offPart = lineOff.substring(leftEqual, lineOff.length()-rightEqual).replace("{", "<((").replace("}", "))>");
                if (!onPart.isEmpty()) {
                    line += "{True:"+onPart+"}";
                }
                if (!line.isEmpty() 
                        && leftEqual == 0 && rightEqual == 0) {
                    line += "\n";
                }
                if (!offPart.isEmpty()) {
                    line += "{False:"+offPart+"}";
                }
                line += lineOn.substring(lineOn.length()-rightEqual);
                if (!line.isEmpty()) {
                    commandLines.add(line);
                }
            }
            String command = String.join("\n", commandLines);
            Logger.trace("Composited "+qualifier+" "+commandType+":\n"+command);
            // NOTE: the following check is too hard due to empty lines ambiguities (may be fixed in the future).
            //          if (!(extractBooleanCommand(true, command).equals(commandOn) 
            //                    && extractBooleanCommand(false, command).equals(commandOff))) {
            //              throw new Exception("ON and OFF command composition failed, please set the "+qualifier+" "+commandType+" manually on the GcodeDriver.");
            //          }
            return command;
        }
    }
}
