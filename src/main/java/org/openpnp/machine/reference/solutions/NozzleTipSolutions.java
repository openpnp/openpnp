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

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.BackgroundCalibrationMethod;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.RecalibrationTrigger;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipCalibrationWizard;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.Collect;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
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
                                    "Set the manual nozzle tip change location for "+nozzle.getName()+".", 
                                    "Jog "+nozzle.getName()+" to the manual nozzle tip changing location, then press Accept.", 
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
                                    return "<html>"
                                            + "<p>Jog "+nozzle.getName()+" to a suitable location where you can manually exchange the nozzle tips.</p><br/>"
                                            + "<p>Even if you (plan to) use an automatic nozzle tip changer, it is useful to have this location "
                                            + "definied, in case you want to disable automatic changing temporarily.</p><br/>"
                                            + "<p>Often it is best to move Z all the way up so the tip is well reachable. On some head designs "
                                            + "the uppermost Z position is blocked by a limiter, so it is the best position to apply the necessary "
                                            + "force when inserting the nozzle tip.</p><br/>"
                                            + "<p>Note for experts: the captured location will be automatically adjusted if you change the nozzle "
                                            + "head offsets later.</p><br/>"
                                            + "<p>Press <strong>Accept</strong> to store the location.</p><br/>"
                                            + "</html>";
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
                                    "Nozzle tip "+nozzleTip.getName()+" has no compatible nozzle.", 
                                    "Go to the nozzle(s) and enable the Compatible switches where appropriate.", 
                                    Solutions.Severity.Error,
                                    "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Nozzle-Setup#nozzle-to-nozzle-tip-compatibility"));
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
        
        if (!nozzleTip.getCalibration().isEnabled()) {
            solutions.add(machine.getVisionSolutions().new VisionFeatureIssue(
                    nozzleTip,
                    (ReferenceCamera) camera,
                    oldVisionDiameter,
                    "Enable nozzle tip "+nozzleTip.getName()+" calibration.", 
                    "Enable run-out, background and offset calibration for nozzle tip "+nozzleTip.getName()+".", 
                    Solutions.Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Nozzle-Tip-Calibration-Setup") {

                @Override 
                public void activate() throws Exception {
                    super.activate();
                    MainFrame.get().getMachineControls().setSelectedTool(nozzle);
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>It is recommended to enable nozzle tip calibration for run-out, offsets and background calibration. "
                            + "For more information, press the blue Info button (below) to open the Wiki.</p><br/>"
                            + (nozzleTip != nozzle.getNozzleTip() ?
                                    "<p>Load nozzle tip "+nozzleTip.getName()+" to nozzle "+nozzle.getName() + ".</p><br/>"
                                    : "<p>Nozzle tip "+nozzleTip.getName()+" is already loaded on nozzle "+nozzle.getName() + ".</p><br/>")
                            + "<p>Press the <strong>Center Nozzle</strong> button (below) to center nozzle "+nozzle.getName()
                            +" over the camera "+camera.getName()+".</p><br/>"
                            + "<p>Adjust the <strong>Feature diameter</strong> up and down and see if it is detected right in the "
                            + "camera view. A green circle and cross-hairs should appear and hug the wanted contour. "
                            + "Zoom the camera using the scroll-wheel. Make sure to target a circular edge that can be detected "
                            + "consistently even when seen from the side. This means it has to be a rather sharp-angled edge. "
                            + "Typically, the air bore contour is targeted.</p><br/>"
                            + "<p>Then press Accept to enable and perform the nozzle tip calibration.</p>"
                            + "</html>";
                }

                @Override
                public Solutions.Issue.CustomProperty[] getProperties() {
                    Solutions.Issue.CustomProperty[] props1 = super.getProperties();
                    Solutions.Issue.CustomProperty[] props0 = new Solutions.Issue.CustomProperty[] {
                            nozzleTipLoadActionProperty(this, nozzle, nozzleTip),
                            new Solutions.Issue.ActionProperty( 
                                    "", "Center nozzle "+nozzle.getName()+" over camera "+camera.getName()) {
                                @Override
                                public Action get() {
                                    return new AbstractAction("Center Nozzle", Icons.centerTool) {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            UiUtils.submitUiMachineTask(() -> {
                                                if (nozzleTip != nozzle.getNozzleTip()) {
                                                    throw new Exception("The nozzle tip "+nozzleTip.getName()+" is not loaded on nozzle "+nozzle.getName()+".");
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
                            throw new Exception("The nozzle tip "+nozzleTip.getName()+" is not loaded on nozzle "+nozzle.getName()+".");
                        }
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    // Perform preliminary camera calibration. 
                                    Length visionDiameter = camera.getUnitsPerPixel().getLengthX().multiply(featureDiameter);
                                    nozzleTip.getCalibration().setCalibrationTipDiameter(visionDiameter);
                                    Logger.info("Set nozzle tip "+nozzleTip.getName()+" vision diameter to "+visionDiameter+" (previously "+oldVisionDiameter+")");
                                    nozzleTip.getCalibration().setEnabled(true);
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
                        nozzleTip.getCalibration().setFailHoming(oldFailHoming);
                        nozzleTip.getCalibration().setRecalibrationTrigger(oldRecalibrationTrigger);
                        super.setState(state);
                    }
                }
            });
        }
        else if (oldBackgroundCalibrationMethod == BackgroundCalibrationMethod.None) {
            solutions.add(new Solutions.Issue(
                    nozzleTip, 
                    "Set background calibration method for "+nozzleTip.getName()+".", 
                    "Depending on the type of nozzle tip or shade, select the proper background calibration.", 
                    Solutions.Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Nozzle-Tip-Background-Calibration") {

                @Override 
                public void activate() throws Exception {
                    MainFrame.get().getMachineControls().setSelectedTool(nozzle);
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>Select the proper background calibration.</p><br/>"
                            + "<p><strong color=\"red\">CAUTION</strong>: Nozzle "+nozzle.getName()+" will move over camera "
                            + camera.getName()+" and perform a new nozzle tip calibration calibration, including the enabled "
                            + "background calibration. Any problems will be indicated with purple highlights.</p><br/>"
                            + "<p>When ready, press Accept.</p>"
                            + (getState() == State.Solved ? 
                                    "<br/><h4>Results</h4>"
                                    + "<p style=\"max-width: 40em\">"+nozzleTip.getCalibration().getBackgroundDiagnostics()
                                    .replace("<html>", "").replace("</html>", "").replace("<hr/>", "<br/>")+"</p><br/>" 
                                            + "<p>More information on the Calibration tab of nozzle tip "+nozzleTip.getName()+".</p>"
                                            : "")
                            + "</html>";
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
                                    "<html><h3>Brighness and Key-Color</h3>"
                                            + "<p>The nozzle tip and/or background (shade) is color-keyed "
                                            + "so computer vision can robustly distinguish background pixels from "
                                            + "foreground pixels (\"green-screening\"). Use for green Juki style nozzles.</p>"
                                            + "</html>",
                                            null),
                            new Solutions.Issue.Choice(BackgroundCalibrationMethod.Brightness, 
                                    "<html><h3>Brightness</h3>"
                                            + "<p>The background is just dark, the foreground is distinguished by "
                                            + "brighness only.</p><br/>"
                                            + "</html>",
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
                            throw new Exception("The nozzle tip "+nozzleTip.getName()+" is not loaded on nozzle "+nozzle.getName()+".");
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
    
    protected Solutions.Issue.ActionProperty nozzleTipLoadActionProperty(Solutions.Issue issue, Nozzle nozzle,
            ReferenceNozzleTip nozzleTip) {
        return issue.new ActionProperty( 
                "", "Load nozzle tip "+nozzleTip.getName()+" on "+nozzle.getName()) {
            @Override
            public Action get() {
                return new AbstractAction("Load Nozzle Tip "+nozzleTip.getName(), Icons.nozzleTipLoad) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        UiUtils.submitUiMachineTask(() -> {
                            if (nozzleTip == nozzle.getNozzleTip()) {
                                throw new Exception("The nozzle tip "+nozzleTip.getName()+" is already loaded on nozzle "+nozzle.getName()+".");
                            }
                            nozzle.loadNozzleTip(nozzleTip);
                        });
                    }
                };
            }
        };
    }

}
