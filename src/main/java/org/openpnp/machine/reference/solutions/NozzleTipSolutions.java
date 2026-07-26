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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.BackgroundCalibrationMethod;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.RecalibrationTrigger;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipCalibrationWizard;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.Collect;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.pmw.tinylog.Logger;

public class NozzleTipSolutions implements Solutions.Subject  {
    private ReferenceMachine machine;

    public NozzleTipSolutions() {
        super();
    }

    public NozzleTipSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Kinematics)) {
            for (Head head : machine.getHeads()) {
                for (Nozzle n : head.getNozzles()) {
                    if (n instanceof ReferenceNozzle) {
                        ReferenceNozzle nozzle = (ReferenceNozzle) n;
                        if (!nozzle.getManualNozzleTipChangeLocation().isInitialized()) {
                            solutions.add(new Solutions.Issue(
                                    nozzle, 
                                    Translations.format("NozzleTipSolutions.Issue.ManualTipChangeLocation", nozzle.getName()), //$NON-NLS-1$
                                    Translations.format("NozzleTipSolutions.Solution.ManualTipChangeLocation", nozzle.getName()), //$NON-NLS-1$
                                    Solutions.Severity.Suggestion,
                                    "https://github.com/openpnp/openpnp/wiki/Kinematic-Solutions#capture-safe-z") {

                                @Override 
                                public void activate() throws Exception {
                                    MainFrame.get().getMachineControls().setSelectedTool(nozzle);
                                }

                                @Override
                                public Icon getExtendedIcon() {
                                    return Icons.nozzleTipLoad;
                                }

                                @Override 
                                public String getExtendedDescription() {
                                    return Translations.format("NozzleTipSolutions.ExtendedDescription.ManualTipChangeLocation", nozzle.getName()); //$NON-NLS-1$
                                }

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    nozzle.setManualNozzleTipChangeLocation(state == State.Solved ? 
                                            nozzle.getLocation() : Location.origin);
                                    super.setState(state);
                                }
                            });
                        }
                    }
                }
            }
        }
        if (solutions.isTargeting(Milestone.Calibration)) {
            for (NozzleTip nt : machine.getNozzleTips()) {
                if (nt instanceof ReferenceNozzleTip && !((ReferenceNozzleTip) nt).isUnloadedNozzleTipStandin()) {
                    ReferenceNozzleTip nozzleTip = (ReferenceNozzleTip) nt;
                    try {
                        Camera camera = VisionUtils.getBottomVisionCamera();
                        Nozzle defaultNozzle = nozzleTip.getNozzleWhereLoaded();
                        if (defaultNozzle == null) {
                            for (Head head : machine.getHeads()) {
                                for (Nozzle nozzle : head.getNozzles()) {
                                    if (nozzle.getCompatibleNozzleTips().contains(nozzleTip)) {
                                        defaultNozzle = nozzle;
                                        break;
                                    }
                                }
                            }
                        }
                        if (defaultNozzle == null) {
                            solutions.add(new Solutions.PlainIssue(
                                    nozzleTip, 
                                    Translations.format("NozzleTipSolutions.Issue.NoCompatibleNozzle", nozzleTip.getName()), //$NON-NLS-1$
                                    Translations.getString("NozzleTipSolutions.Solution.NoCompatibleNozzle"), //$NON-NLS-1$
                                    Solutions.Severity.Error,
                                    "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Nozzle-Setup#nozzle-to-nozzle-tip-compatibility"));
                        }
                        else {
                            perNozzleTip(solutions, nozzleTip, camera, defaultNozzle);
                        }
                    }
                    catch (Exception e) {
                        Logger.trace(e);
                    }
                }
            }

        }
    }

    protected void perNozzleTip(Solutions solutions, ReferenceNozzleTip nozzleTip, Camera camera,
            Nozzle nozzle) {
        final Length oldVisionDiameter = nozzleTip.getCalibration().getCalibrationTipDiameter();
        final RecalibrationTrigger oldRecalibrationTrigger = nozzleTip.getCalibration().getRecalibrationTrigger();
        final boolean oldFailHoming = nozzleTip.getCalibration().isFailHoming();
        final BackgroundCalibrationMethod oldBackgroundCalibrationMethod = nozzleTip.getCalibration().getBackgroundCalibrationMethod();
        final CvPipeline oldPipeline = nozzleTip.getCalibration().getPipeline(); 
        LengthConverter lengthConverter = new LengthConverter(); 

        if (!nozzleTip.getCalibration().isEnabled()) {
            solutions.add(machine.getVisionSolutions().new VisionFeatureIssue(
                    nozzleTip,
                    (ReferenceCamera) camera,
                    oldVisionDiameter,
                    Translations.format("NozzleTipSolutions.Issue.EnableCalibration", nozzleTip.getName()), //$NON-NLS-1$
                    Translations.format("NozzleTipSolutions.Solution.EnableCalibration", nozzleTip.getName()), //$NON-NLS-1$
                    Solutions.Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Nozzle-Tip-Calibration-Setup") {

                @Override 
                public void activate() throws Exception {
                    super.activate();
                    MainFrame.get().getMachineControls().setSelectedTool(nozzle);
                }

                @Override 
                public String getExtendedDescription() {
                    return Translations.format("NozzleTipSolutions.ExtendedDescription.EnableCalibration", //$NON-NLS-1$
                            (nozzleTip != nozzle.getNozzleTip() ?
                                    Translations.format("NozzleTipSolutions.ExtendedDescription.EnableCalibration.LoadTip", nozzleTip.getName(), nozzle.getName()) //$NON-NLS-1$
                                    : Translations.format("NozzleTipSolutions.ExtendedDescription.EnableCalibration.TipLoaded", nozzleTip.getName(), nozzle.getName())), //$NON-NLS-1$
                            nozzle.getName(), camera.getName());
                }

                @Override
                public Solutions.Issue.CustomProperty[] getProperties() {
                    Solutions.Issue.CustomProperty[] props1 = super.getProperties();
                    Solutions.Issue.CustomProperty[] props0 = new Solutions.Issue.CustomProperty[] {
                            nozzleTipLoadActionProperty(this, nozzle, nozzleTip),
                            new Solutions.Issue.ActionProperty( 
                                    "", Translations.format("NozzleTipSolutions.Property.CenterNozzle", nozzle.getName(), camera.getName())) { //$NON-NLS-1$
                                @Override
                                public Action get() {
                                    return new AbstractAction(Translations.getString("NozzleTipSolutions.Action.CenterNozzle"), Icons.centerTool) { //$NON-NLS-1$
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            UiUtils.submitUiMachineTask(() -> {
                                                if (nozzleTip != nozzle.getNozzleTip()) {
                                                    throw new Exception(Translations.format("NozzleTipSolutions.Exception.TipNotLoaded", nozzleTip.getName(), nozzle.getName())); //$NON-NLS-1$
                                                }
                                                MovableUtils.moveToLocationAtSafeZ(nozzle, camera.getLocation(nozzle));
                                                MovableUtils.fireTargetedUserAction(nozzle);
                                            });
                                        }
                                    };
                                }
                            },
                    };
                    return Collect.concat(props0, props1);
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        if (nozzleTip != nozzle.getNozzleTip()) {
                            throw new Exception(Translations.format("NozzleTipSolutions.Exception.TipNotLoaded", nozzleTip.getName(), nozzle.getName())); //$NON-NLS-1$
                        }
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    // Perform preliminary camera calibration. 
                                    Length visionDiameter = camera.getUnitsPerPixel().getLengthX().multiply(featureDiameter);
                                    nozzleTip.getCalibration().setCalibrationTipDiameter(visionDiameter);
                                    Logger.info("Set nozzle tip "+nozzleTip.getName()+" vision diameter to "+visionDiameter+" (previously "+oldVisionDiameter+")");
                                    nozzleTip.getCalibration().setEnabled(true);
                                    nozzleTip.getCalibration().resetPipeline();
                                    nozzleTip.getCalibration().setRecalibrationTrigger(RecalibrationTrigger.MachineHome);
                                    nozzleTip.getCalibration().setFailHoming(false);
                                    nozzleTip.getCalibration().calibrate((ReferenceNozzle) nozzle);
                                    return true;
                                },
                                (result) -> {
                                    UiUtils.messageBoxOnException(() -> super.setState(state));
                                },
                                (t) -> {
                                    UiUtils.showError(t);
                                    // restore old state
                                    UiUtils.messageBoxOnException(() -> setState(oldState));
                                });
                    }
                    else {
                        // Restore the old vision diameter.
                        nozzleTip.getCalibration().setCalibrationTipDiameter(oldVisionDiameter);
                        nozzleTip.getCalibration().setEnabled(false);
                        nozzleTip.getCalibration().setPipeline(oldPipeline);
                        nozzleTip.getCalibration().setFailHoming(oldFailHoming);
                        nozzleTip.getCalibration().setRecalibrationTrigger(oldRecalibrationTrigger);
                        super.setState(state);
                    }
                }
            });
        }
        else { 
            if (nozzleTip.getMaxPickTolerance().compareTo(new Length(1.0, LengthUnit.Millimeters)) > 0) {
                solutions.add(new Solutions.PlainIssue(
                        nozzleTip, 
                        Translations.format("NozzleTipSolutions.Issue.LargePickTolerance", nozzleTip.getName(), lengthConverter.convertForward(nozzleTip.getMaxPickTolerance())), //$NON-NLS-1$
                        Translations.getString("NozzleTipSolutions.Solution.LargePickTolerance"), //$NON-NLS-1$
                        Severity.Error,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Nozzle-Setup#nozzle-tip-configuration"));
            }
            else if (nozzleTip.getMinPartDiameter().compareTo(nozzleTip.getMaxPickTolerance().multiply(2)) <= 0) {
                solutions.add(new Solutions.PlainIssue(
                        nozzleTip, 
                        Translations.format("NozzleTipSolutions.Issue.InvalidMinPartDiameter", nozzleTip.getName(), lengthConverter.convertForward(nozzleTip.getMinPartDiameter())), //$NON-NLS-1$
                        Translations.format("NozzleTipSolutions.Solution.InvalidMinPartDiameter", lengthConverter.convertForward(nozzleTip.getMaxPickTolerance())), //$NON-NLS-1$
                        Severity.Error,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Nozzle-Setup#nozzle-tip-configuration"));
            }
            else if (nozzleTip.getMinPartDiameter().compareTo(nozzleTip.getMaxPartDiameter()) >= 0) {
                solutions.add(new Solutions.PlainIssue(
                        nozzleTip, 
                        Translations.format("NozzleTipSolutions.Issue.MaxMinPartDiameter", nozzleTip.getName()), //$NON-NLS-1$
                        Translations.getString("NozzleTipSolutions.Solution.MaxMinPartDiameter"), //$NON-NLS-1$
                        Severity.Error,
                        "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Nozzle-Setup#nozzle-tip-configuration"));
            }
            if (oldBackgroundCalibrationMethod == BackgroundCalibrationMethod.None) {
                solutions.add(new Solutions.Issue(
                        nozzleTip, 
                        Translations.format("NozzleTipSolutions.Issue.BackgroundCalibration", nozzleTip.getName()), //$NON-NLS-1$
                        Translations.getString("NozzleTipSolutions.Solution.BackgroundCalibration"), //$NON-NLS-1$
                        Solutions.Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Nozzle-Tip-Background-Calibration") {

                    @Override 
                    public void activate() throws Exception {
                        MainFrame.get().getMachineControls().setSelectedTool(nozzle);
                    }

                    @Override 
                    public String getExtendedDescription() {
                        return Translations.format("NozzleTipSolutions.ExtendedDescription.BackgroundCalibration", //$NON-NLS-1$
                                nozzle.getName(), camera.getName(),
                                (getState() == State.Solved ? 
                                        Translations.format("NozzleTipSolutions.ExtendedDescription.BackgroundCalibration.Results", //$NON-NLS-1$
                                                nozzleTip.getCalibration().getBackgroundDiagnostics()
                                                .replace("<html>", "").replace("</html>", "").replace("<hr/>", "<br/>"),
                                                nozzleTip.getName())
                                        : ""));
                    }

                    @Override
                    public Solutions.Issue.CustomProperty[] getProperties() {
                        return new Solutions.Issue.CustomProperty[] {
                                nozzleTipLoadActionProperty(this, nozzle, nozzleTip),
                        };
                    }

                    @Override
                    public Solutions.Issue.Choice[] getChoices() {
                        return new Solutions.Issue.Choice[] {
                                new Solutions.Issue.Choice(BackgroundCalibrationMethod.BrightnessAndKeyColor, 
                                        Translations.getString("NozzleTipSolutions.Choice.BrightnessAndKeyColor"), //$NON-NLS-1$
                                        null),
                                new Solutions.Issue.Choice(BackgroundCalibrationMethod.Brightness, 
                                        Translations.getString("NozzleTipSolutions.Choice.Brightness"), //$NON-NLS-1$
                                                null),
                        };
                    }

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        nozzleTip.getCalibration().setBackgroundCalibrationMethod(
                                state == State.Solved ? 
                                        (BackgroundCalibrationMethod) getChoice() : oldBackgroundCalibrationMethod);
                        if (state == State.Solved) {
                            if (nozzleTip != nozzle.getNozzleTip()) {
                                throw new Exception(Translations.format("NozzleTipSolutions.Exception.TipNotLoaded", nozzleTip.getName(), nozzle.getName())); //$NON-NLS-1$
                            }
                            UiUtils.submitUiMachineTask(() -> {
                                nozzleTip.getCalibration().calibrate((ReferenceNozzle) nozzle);
                                UiUtils.messageBoxOnExceptionLater(() -> {
                                    super.setState(state);
                                    ReferenceNozzleTipCalibrationWizard.showBackgroundProblems(nozzleTip, false);
                                });
                            });
                        }
                        else {
                            super.setState(state);
                        }
                    }
                });
            }
        }
    }

    protected Solutions.Issue.ActionProperty nozzleTipLoadActionProperty(Solutions.Issue issue, Nozzle nozzle,
            ReferenceNozzleTip nozzleTip) {
        return issue.new ActionProperty( 
                "", Translations.format("NozzleTipSolutions.Property.LoadNozzleTip", nozzleTip.getName(), nozzle.getName())) { //$NON-NLS-1$
            @Override
            public Action get() {
                return new AbstractAction(Translations.format("NozzleTipSolutions.Action.LoadNozzleTip", nozzleTip.getName()), Icons.nozzleTipLoad) { //$NON-NLS-1$
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        UiUtils.submitUiMachineTask(() -> {
                            if (nozzleTip == nozzle.getNozzleTip()) {
                                throw new Exception(Translations.format("NozzleTipSolutions.Exception.TipAlreadyLoaded", nozzleTip.getName(), nozzle.getName())); //$NON-NLS-1$
                            }
                            nozzle.loadNozzleTip(nozzleTip);
                        });
                    }
                };
            }
        };
    }

}
