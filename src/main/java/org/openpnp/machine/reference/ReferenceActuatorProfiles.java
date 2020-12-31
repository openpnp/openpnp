/*
 * Copyright (C) 2020 <mark@makr.zone>
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

package org.openpnp.machine.reference;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceActuatorProfilesWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Named;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Actuator.ActuatorValueType;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.base.AbstractActuator;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

public class ReferenceActuatorProfiles extends AbstractTableModel {

    @Attribute(required = false)
    private String actuator1Id;

    @Attribute(required = false)
    private String actuator2Id;

    @Attribute(required = false)
    private String actuator3Id;

    @Attribute(required = false)
    private String actuator4Id;

    @Attribute(required = false)
    private String actuator5Id;

    @Attribute(required = false)
    private String actuator6Id;

    @ElementList(required = false)
    private ArrayList<Profile> profiles = new ArrayList<Profile>();

    private Actuator actuator1;
    private Actuator actuator2;
    private Actuator actuator3;
    private Actuator actuator4;
    private Actuator actuator5;
    private Actuator actuator6;

    private ReferenceActuator containingActuator; 

    public ReferenceActuatorProfiles() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                // We don't have access to machine/actuator and head here. So we need to scan them all. 
                // I'm sure there is a better solution.
                Machine machine = configuration.getMachine();
                actuator1 = machine.getActuator(actuator1Id);
                actuator2 = machine.getActuator(actuator2Id);
                actuator3 = machine.getActuator(actuator3Id);
                actuator4 = machine.getActuator(actuator4Id);
                actuator5 = machine.getActuator(actuator5Id);
                actuator6 = machine.getActuator(actuator6Id);
                for (Head head : machine.getHeads()) {
                    if (actuator1 == null) {
                        actuator1 = head.getActuator(actuator1Id);
                    }
                    if (actuator2 == null) {
                        actuator2 = head.getActuator(actuator2Id);
                    }
                    if (actuator3 == null) {
                        actuator3 = head.getActuator(actuator3Id);
                    }
                    if (actuator4 == null) {
                        actuator4 = head.getActuator(actuator4Id);
                    }
                    if (actuator5 == null) {
                        actuator5 = head.getActuator(actuator5Id);
                    }
                    if (actuator6 == null) {
                        actuator6 = head.getActuator(actuator6Id);
                    }
                }
            }
        });
    }

    public String getActuator1Id() {
        return actuator1Id;
    }

    public void setActuator1Id(String actuator1Id) {
        this.actuator1Id = actuator1Id;
    }

    public String getActuator2Id() {
        return actuator2Id;
    }

    public void setActuator2Id(String actuator2Id) {
        this.actuator2Id = actuator2Id;
    }

    public String getActuator3Id() {
        return actuator3Id;
    }

    public void setActuator3Id(String actuator3Id) {
        this.actuator3Id = actuator3Id;
    }

    public String getActuator4Id() {
        return actuator4Id;
    }

    public void setActuator4Id(String actuator4Id) {
        this.actuator4Id = actuator4Id;
    }

    public String getActuator5Id() {
        return actuator5Id;
    }

    public void setActuator5Id(String actuator5Id) {
        this.actuator5Id = actuator5Id;
    }

    public String getActuator6Id() {
        return actuator6Id;
    }

    public void setActuator6Id(String actuator6Id) {
        this.actuator6Id = actuator6Id;
    }

    public ArrayList<Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(ArrayList<Profile> profiles) {
        this.profiles = profiles;
    }

    public Actuator getActuator1() {
        return actuator1;
    }

    public void setActuator1(Actuator actuator1) {
        this.actuator1 = actuator1;
        this.actuator1Id = (actuator1 == null) ? null : actuator1.getId();
    }

    public Actuator getActuator2() {
        return actuator2;
    }

    public void setActuator2(Actuator actuator2) {
        this.actuator2 = actuator2;
        this.actuator2Id = (actuator2 == null) ? null : actuator2.getId();
    }

    public Actuator getActuator3() {
        return actuator3;
    }

    public void setActuator3(Actuator actuator3) {
        this.actuator3 = actuator3;
        this.actuator3Id = (actuator3 == null) ? null : actuator3.getId();
    }

    public Actuator getActuator4() {
        return actuator4;
    }

    public void setActuator4(Actuator actuator4) {
        this.actuator4 = actuator4;
        this.actuator4Id = (actuator4 == null) ? null : actuator4.getId();
    }

    public Actuator getActuator5() {
        return actuator5;
    }

    public void setActuator5(Actuator actuator5) {
        this.actuator5 = actuator5;
        this.actuator5Id = (actuator5 == null) ? null : actuator5.getId();
    }

    public Actuator getActuator6() {
        return actuator6;
    }

    public void setActuator6(Actuator actuator6) {
        this.actuator6 = actuator6;
        this.actuator6Id = (actuator6 == null) ? null : actuator6.getId();
    }

    public ReferenceActuator getContainingActuator() {
        return containingActuator;
    }

    public void setContainingActuator(ReferenceActuator containingActuator) {
        this.containingActuator = containingActuator;
    }

    Wizard getConfigurationWizard(ReferenceActuator actuator) {
        setContainingActuator(actuator);
        return new ReferenceActuatorProfilesWizard(actuator, this);
    }

    static public class Profile extends AbstractModelObject implements Named {
        @Attribute
        private String name = "";
        @Attribute(required = false)
        private boolean defaultOn;
        @Attribute(required = false)
        private boolean defaultOff;
        @Element(required = false)
        private Object value1;
        @Element(required = false)
        private Object value2;
        @Element(required = false)
        private Object value3;
        @Element(required = false)
        private Object value4;
        @Element(required = false)
        private Object value5;
        @Element(required = false)
        private Object value6;

        private ReferenceActuatorProfiles container;

        @Override 
        public String getName() {
            return name;
        }
        @Override 
        public void setName(String name) {
            // Always keep it trimmed.
            name = name.trim(); 
            Object oldValue = this.name;
            this.name = name;
            firePropertyChange("name", oldValue, name);
            if (container != null) {
                if (container.containingActuator != null) {
                    container.getContainingActuator().fireProfilesChanged(); 
                }
            }
        }
        public boolean isDefaultOn() {
            return defaultOn;
        }
        public void setDefaultOn(boolean defaultOn) {
            boolean oldValue = this.defaultOn;
            this.defaultOn = defaultOn;
            if (oldValue != defaultOn) {
                firePropertyChange("defaultOn", oldValue, defaultOn);
                if (defaultOn && defaultOff) {
                    // can't be on at the same time
                    setDefaultOff(false);
                }
                if (defaultOn && container != null) {
                    // Mutually exclusive
                    for (Profile profile : container.profiles) {
                        if (profile != this) {
                            profile.setDefaultOn(false);
                        }
                    }
                    container.fireTableRowsUpdated(0, container.profiles.size()-1);
                }
            }
        }
        public boolean isDefaultOff() {
            return defaultOff;
        }
        public void setDefaultOff(boolean defaultOff) {
            boolean oldValue = this.defaultOff;
            this.defaultOff = defaultOff;
            if (oldValue != defaultOff) {
                firePropertyChange("defaultOff", oldValue, defaultOff);
                if (defaultOn && defaultOff) {
                    // can't be on at the same time
                    setDefaultOn(false);
                }
                if (defaultOff && container != null) {
                    // Mutually exclusive
                    for (Profile profile : container.profiles) {
                        if (profile != this) {
                            profile.setDefaultOff(false);
                        }
                    }
                    container.fireTableRowsUpdated(0, container.profiles.size()-1);
                }
            }
        }

        public Object getValue1() {
            return AbstractActuator.typedValue(value1, container.getActuator1());
        }
        public void setValue1(Object value1) {
            Object oldValue = this.value1;
            this.value1 = value1;
            firePropertyChange("value1", oldValue, value1);
        }
        public Object getValue2() {
            return AbstractActuator.typedValue(value2, container.getActuator2());
        }
        public void setValue2(Object value2) {
            Object oldValue = this.value2;
            this.value2 = value2;
            firePropertyChange("value2", oldValue, value2);
        }
        public Object getValue3() {
            return AbstractActuator.typedValue(value3, container.getActuator3());
        }
        public void setValue3(Object value3) {
            Object oldValue = this.value3;
            this.value3 = value3;
            firePropertyChange("value3", oldValue, value3);
        }
        public Object getValue4() {
            return AbstractActuator.typedValue(value4, container.getActuator4());
        }
        public void setValue4(Object value4) {
            Object oldValue = this.value4;
            this.value4 = value4;
            firePropertyChange("value4", oldValue, value4);
        }
        public Object getValue5() {
            return AbstractActuator.typedValue(value5, container.getActuator5());
        }
        public void setValue5(Object value5) {
            Object oldValue = this.value5;
            this.value5 = value5;
            firePropertyChange("value5", oldValue, value5);
        }
        public Object getValue6() {
            return AbstractActuator.typedValue(value6, container.getActuator6());
        }
        public void setValue6(Object value6) {
            Object oldValue = this.value6;
            this.value6 = value6;
            firePropertyChange("value6", oldValue, value6);
        }
        public ReferenceActuatorProfiles getContainer() {
            return container;
        }
        public void setContainer(ReferenceActuatorProfiles container) {
            this.container = container;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch(columnIndex) {
            case 0:
                return "Name";
            case 1:
                return "Default ON";
            case 2:
                return "Default OFF";
            case 3:
                return getActuator1() == null ? null : getActuator1().getName();
            case 4:
                return getActuator2() == null ? null : getActuator2().getName();
            case 5:
                return getActuator3() == null ? null : getActuator3().getName();
            case 6:
                return getActuator4() == null ? null : getActuator4().getName();
            case 7:
                return getActuator5() == null ? null : getActuator5().getName();
            case 8:
                return getActuator6() == null ? null : getActuator6().getName();
        }
        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch(columnIndex) {
            case 0:
                return String.class;
            case 1:
                return Boolean.class;
            case 2:
                return Boolean.class;
            case 3:
                return getActuatorValueClass(getActuator1());
            case 4:
                return getActuatorValueClass(getActuator2());
            case 5:
                return getActuatorValueClass(getActuator3());
            case 6:
                return getActuatorValueClass(getActuator4());
            case 7:
                return getActuatorValueClass(getActuator5());
            case 8:
                return getActuatorValueClass(getActuator6());
        }
        return Object.class;
    }

    static Class<?> getActuatorValueClass(Actuator actuator) {
        if (actuator != null) {
            switch (actuator.getValueType()) {
                case Boolean:
                    return Boolean.class;
                case Double:
                    return Double.class;
                case String:
                    return String.class;
                case Profile:
                    // A nested actuator is handled as if it is a string actuator.
                    return String.class;
            }
        }
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return getColumnClass(columnIndex) != Object.class;
    }

    @Override
    public int getRowCount() {
        return profiles.size();
    }

    @Override
    public int getColumnCount() {
        int count = 9;
        if (getActuator6() != null) {
            return count;
        }
        count--;
        if (getActuator5() != null) {
            return count;
        }
        count--;
        if (getActuator4() != null) {
            return count;
        }
        count--;
        if (getActuator3() != null) {
            return count;
        }
        count--;
        if (getActuator2() != null) {
            return count;
        }
        count--;
        return count;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Profile profile = profiles.get(rowIndex); 
        profile.setContainer(this);
        switch(columnIndex) {
            case 0:
                return profile.getName();
            case 1:
                return profile.isDefaultOn();
            case 2:
                return profile.isDefaultOff();
            case 3:
                return profile.getValue1();
            case 4:
                return profile.getValue2();
            case 5:
                return profile.getValue3();
            case 6:
                return profile.getValue4();
            case 7:
                return profile.getValue5();
            case 8:
                return profile.getValue6();
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Profile profile = profiles.get(rowIndex); 
        switch(columnIndex) {
            case 0:
                profile.setName(aValue.toString());
                break;
            case 1:
                if (aValue instanceof Boolean) {
                    profile.setDefaultOn((Boolean)aValue);
                }
                break;
            case 2:
                if (aValue instanceof Boolean) {
                    profile.setDefaultOff((Boolean)aValue);
                }
                break;
            case 3:
                profile.setValue1(aValue);
                break;
            case 4:
                profile.setValue2(aValue);
                break;
            case 5:
                profile.setValue3(aValue);
                break;
            case 6:
                profile.setValue4(aValue);
                break;
            case 7:
                profile.setValue5(aValue);
                break;
            case 8:
                profile.setValue6(aValue);
                break;
        }
    }

    public String getToolTipAt(int rowIndex, int columnIndex) {
        Profile profile = profiles.get(rowIndex); 
        return null;
    }

    public void add(Profile profile) {
        profiles.add(profile);
        fireTableDataChanged();
    }

    public void addNew() {
        add(new Profile());
    }

    public void delete(Profile profile) {
        profiles.remove(profile);
        fireTableDataChanged();
    }

    public Profile get(int i) {
        return profiles.get(i);
    }

    public String[] getProfileNames() {
        String  [] names = new String [profiles.size()];
        int i = 0;
        for (Profile profile : profiles) {
            if (profile.getName().length() > 0) {
                names[i++] = profile.getName();
            }
        }
        return names;
    }

    protected Profile findProfile(String name) {
        for (Profile profile : profiles) {
            if (profile.getName().equals(name)) {
                return profile;
            }
        }
        return null;
    }

    protected Profile findProfile(boolean on) {
        for (Profile profile : profiles) {
            if (on && profile.isDefaultOn()) {
                return profile;
            }
            else if ((!on) && profile.isDefaultOff()) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Actuate the Profile identified by name.  
     * 
     * @param actuator
     * @param name
     * @return
     * @throws Exception
     */
    public String actuate(Actuator actuator, String name) throws Exception {
        Profile profile = findProfile(name);
        if (profile == null) {
            // None found.
            throw new Exception("Actuator "+actuator+" profile "+name+" not found.");
        }
        actuate(actuator, profile);
        return profile.getName();
    }

    /**
     * Actuate the default ON/OFF profile according to the boolean on.  
     * 
     * @param actuator
     * @param on
     * @return
     * @throws Exception
     */
    public String actuate(Actuator actuator, boolean on) throws Exception { 
        Profile profile = findProfile(on);
        if (profile == null) {
            // None found.
            throw new Exception("Actuator "+actuator+" "+(on ? getColumnName(1) : getColumnName(2))+" profile not found.");
        }
        actuate(actuator, profile);
        return profile.getName();
    }

    protected void actuate(Actuator actuator, Profile profile) throws Exception {
        if (profile == null) {
            return;
        }
        profile.setContainer(this);
        actuateProfileActuator(actuator, profile, getActuator1(), profile.getValue1());
        actuateProfileActuator(actuator, profile, getActuator2(), profile.getValue2());
        actuateProfileActuator(actuator, profile, getActuator3(), profile.getValue3());
        actuateProfileActuator(actuator, profile, getActuator4(), profile.getValue4());
        actuateProfileActuator(actuator, profile, getActuator5(), profile.getValue5());
        actuateProfileActuator(actuator, profile, getActuator6(), profile.getValue6());
        return;
    }

    private void actuateProfileActuator(Actuator actuator, Profile profile, Actuator profileActuator, Object value) throws Exception {
        if (profileActuator != null && value != null) {
            if (profileActuator.getValueType() == ActuatorValueType.Profile) {
                // If nested in a profile, handle this one as a String actuator.
                profileActuator.actuate(value.toString());
            }
            else {
                profileActuator.actuate(value);
            }
        }
    }
}
