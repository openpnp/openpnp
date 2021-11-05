/*
 * Copyright (C) 2021 <mark@makr.zone>
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

package org.openpnp.util;

public class KernelUtils {
    private static double gaussErrorFunction(double x) {
        //from http://picomath.org/javascript/erf.js.html

        // constants
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        // Save the sign of x
        double sign = Math.signum(x);
        x = Math.abs(x);

        // A&S formula 7.1.26
        double t = 1.0/(1.0 + p*x);
        double y = 1.0 - (((((a5*t + a4)*t) + a3)*t + a2)*t + a1)*t*Math.exp(-x*x);

        return sign*y;
    }

    private static double getIntGaussian(double x, double mu, double sigma) {
        return 0.5 * gaussErrorFunction((x-mu)/(Math.sqrt(2) * sigma));
    }

    public static double [] getGaussianKernel(double sigma, double mu, int kernelSize) {
        double xStart = -(kernelSize/2.0);
        double xEnd = (kernelSize/2.0);
        double step = 1;
        double [] kernel = new double [kernelSize];
        int bin = 0;
        double lastInt = getIntGaussian(xStart, mu, sigma);
        for (double xi = xStart; xi < xEnd; xi += step) {
            double newInt = getIntGaussian(xi + step, mu, sigma);
            kernel[bin++] = newInt - lastInt;
            lastInt = newInt;
        }

        // Sum of weights.
        double sum = 0;
        for (double c : kernel) {
            sum += c;
        }

        // Normalize.
        for (bin = 0; bin < kernel.length; bin++) {
            kernel[bin] /= sum;
        }
        return kernel;
    }

    public static double[] applyKernel(final int channels, final int size,
            double[] crossSection, double[] kernel) {
        double [] crossSectionFiltered = new double[crossSection.length];
        return applyKernel(channels, size, crossSection, kernel, crossSectionFiltered);
    }

    public static double[] applyKernel(final int channels, final int size,
            double[] crossSection, double[] kernel, double[] crossSectionFiltered) {
        int kernelSize = kernel.length;
        int halfSize = kernelSize/2;
        for (int ch = 0; ch < channels; ch++) {
            // Apply the kernel
            for (int slot = halfSize; slot < size - halfSize; slot++) {
                double v = 0;
                for (int k = 0; k < kernelSize; k++) {
                    v += crossSection[(slot + k - halfSize)*channels + ch]*kernel[k];
                }
                crossSectionFiltered[slot*channels + ch] = Math.abs(v);
            }
            // Fill up the margins.
            for (int slot = 0; slot < halfSize; slot++) {
                crossSectionFiltered[slot*channels + ch] = crossSectionFiltered[halfSize*channels + ch]; 
            }
            for (int slot = size - halfSize; slot < size; slot++) {
                crossSectionFiltered[slot*channels + ch] = crossSectionFiltered[(size-halfSize-1)*channels + ch]; 
            }
        }
        return crossSectionFiltered;
    }

}
