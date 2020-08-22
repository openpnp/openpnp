package org.openpnp.spi;

import java.io.Closeable;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Named;

/**
 * A driver is the connection between OpenPnP and a machine controller. This is the purely conceptual interface 
 * while the ReferenceDriver interface holds the interface used for the ReferenceMachine.  
 */
public interface Driver extends Identifiable, Named, Closeable, WizardConfigurable, PropertySheetHolder {
    LengthUnit getUnits();

    boolean isSupportingPreMove();

    boolean isUsingLetterVariables();

    /**
     * The MotionControlType determines how the OpenPnP MotionPlanner will do its planning and how it will talk 
     * to the controller. 
     *
     */
    public enum MotionControlType {
        /**
         * Apply the nominal driver feed-rate limit multiplied by the speed factor to the tool-path. 
         * The driver feed-rate must be specified. No acceleration control is applied. 
         */
        ToolpathFeedRate,
        /**
         * Apply axis feed-rate, acceleration and jerk limits multiplied by the proper speed factors. 
         * The Euclidean Metric is calculated to allow the machine to run faster in a diagonal.
         * All profile motion control is left to the controller.   
         */
        EuclideanAxisLimits,
        /**
         * Apply motion planning assuming a controller with constant acceleration motion control. 
         */
        ConstantAcceleration,
        /**
         * Apply motion planning assuming a controller with simplified S-Curve motion control. 
         * Simplified S-Curves have no constant acceleration phase, only jerk phases (e.g. TinyG, Marlin). 
         */
        SimpleSCurve,

        //TODO: Simulated3rdOrderControl,

        /**
         * Apply motion planning assuming a controller with full 3rd order motion control. 
         */
        Full3rdOrderControl;

        public boolean isConstantAcceleration() {
            return this == ToolpathFeedRate || this == ConstantAcceleration;
        }
    }

    MotionControlType getMotionControlType();

    Length getFeedRatePerSecond();

}
