/* TruncatedNewtonConstrainedSolver : truncated newton bound constrained 
 * minimization using gradient information, in Java.
 * 
 * Copyright (c) 2020, mark@makr.zone
 * ported from C port    
 * Copyright (c) 2002-2005, Jean-Sebastien Roy (js@jeannot.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * C port messages: 
 * 
 * This software is a C implementation of TNBC, a truncated newton minimization
 * package originally developed by Stephen G. Nash in Fortran.
 *
 * The original source code can be found at :
 * http://iris.gmu.edu/~snash/nash/software/software.html
 *
 * Copyright for the original TNBC fortran routines:
 *
 *   TRUNCATED-NEWTON METHOD:  SUBROUTINES
 *     WRITTEN BY:  STEPHEN G. NASH
 *           SCHOOL OF INFORMATION TECHNOLOGY & ENGINEERING
 *           GEORGE MASON UNIVERSITY
 *           FAIRFAX, VA 22030
 *
 * Conversion into C by Elisabeth Nguyen & Jean-Sebastien Roy
 * Modifications by Jean-Sebastien Roy, 2001-2002
 *
 */

package org.openpnp.util;

import java.util.Arrays;

/**
 * The TruncatedNewtonConstrainedSolver solves the optimization problem
 *
 *   minimize   f(x)
 *     x
 *   subject to   low <= x <= up
 *
 * where x is a vector of n double variables. The method used is
 * a truncated-newton algorithm (see "newton-type minimization via
 * the lanczos algorithm" by s.g. nash (technical report 378, math.
 * the lanczos method" by s.g. nash (siam j. numer. anal. 21 (1984),
 * pp. 770-778). This algorithm finds a local minimum of f(x). It does
 * not assume that the function f is convex (and so cannot guarantee a
 * global solution), but does assume that the function is bounded below.
 * It can solve problems having any number of variables, but it is
 * especially useful when the number of variables (n) is large.
 * 
 * 
 *
 */
public abstract class TruncatedNewtonConstrainedSolver {

    /**
     * The function as required by tnc. Must be overriden by the solver implementation.
     * 
     * @param x The vector of variables (should not be modified)
     * @param g the value of the gradient (output)
     * @return the function value. 
     * @throws Exception 
     */
    protected abstract double function(final double [] x, double [] g) throws Exception;

    /**
     * Override this method if you want logging. 
     *
     * @param message
     */
    protected void log(String message) {
    }

    // Message level 
    public final static int TNC_MSG_NONE = 0; /* No messages */
    public final static int TNC_MSG_ITER = 1; /* One line per iteration */
    public final static int TNC_MSG_INFO = 2; /* Informational messages */

    public final static int TNC_MSG_ALL = TNC_MSG_ITER | TNC_MSG_INFO; /* All messages */

    /*
     * getptc return codes
     */
    protected final static int GETPTC_OK     = 0; /* Suitable point found */
    protected final static int GETPTC_EVAL   = 1; /* Function evaluation required */
    protected final static int GETPTC_EINVAL = 2; /* Bad input values */
    protected final static int GETPTC_FAIL   = 3;  /* No suitable point found */

    /*
     * linearSearch return codes
     */
    protected final static int LS_OK        = 0; /* Suitable point found */
    protected final static int LS_MAXFUN    = 1; /* Max. number of function evaluations reach */
    protected final static int LS_FAIL      = 2; /* No suitable point found */

    public enum SolverState
    {
        LocalMinimum,       /* Local minima reach (|pg| ~= 0) */
        Converged,          /* Converged (|f_n-f_(n-1)| ~= 0) */
        ConvergedX,         /* Converged (|x_n-x_(n-1)| ~= 0) */
        MaxFunctionEvals,   /* Max. number of function evaluations reach */
        LinearSearchFailed, /* Linear search failed */
        ConstantProblem,    /* All lower bounds are equal to the upper bounds */
        NoProgress;         /* Unable to progress */
    }

    private double epsmch;
    private double rteps;

    private SolverState solverState;
    private int n;
    private int nfeval;
    private double alpha;

    public SolverState getSolverState() {
        return solverState;
    }

    public int getFunctionEvalCount() {
        return nfeval;
    }

    public double getFunctionValue() {
        return fvalue;
    }

    public int getN() {
        return n;
    }
    /**
     * solve() minimize a function with variables subject to bounds, using
     *       gradient information.
     *
     * 
     * @param x      on input, initial estimate ; on output, the solution
     * @param g      on output, the gradient value at the solution
     *               g should be an allocated vector of size n or null,
     *               in which case the gradient value is not returned.
     * @param low    lower bounds
     * @param up     upper bounds
     *               set low[i] to Double.NEGATIVE_INFINITY to remove the lower bound
     *               set up[i] to Double.POSITIVE_INFINITY to remove the upper bound
     *               if low == null, the lower bounds are removed.
     *               if up == null, the upper bounds are removed.
     * @param scale  scaling factors to apply to each variable
     *               if null, the factors are up-low for interval bounded variables
     *               and 1+|x] for the others.
     * @param offset constant to subtract from each variable
     *               if null, the constant are (up+low)/2 for interval bounded
     *               variables and x for the others.
     * @param messages 
     *               message level, see the TNC_MSG_xxx constants
     * @param maxCGit 
     *               max. number of hessian*vector evaluation per main iteration
     *               if maxCGit == 0, the direction chosen is -gradient
     *               if maxCGit < 0, maxCGit is set to max(1,min(50,n/2))
     * @param maxnfeval 
     *               max. number of function evaluation
     * @param eta    severity of the line search. if < 0 or > 1, set to 0.25
     * @param stepmx maximum step for the line search. may be increased during call
     *               if too small, will be set to 10.0
     * @param accuracy 
     *               relative precision for finite difference calculations
     *               if <= machine_precision, set to sqrt(machine_precision)
     * @param fmin   minimum function value estimate
     * @param ftol   precision goal for the value of f in the stoping criterion
     *               if ftol < 0.0, ftol is set to accuracy
     * @param xtol   precision goal for the value of x in the stopping criterion
     *               (after applying x scaling factors)
     *               if xtol < 0.0, xtol is set to sqrt(machine_precision)
     * @param pgtol  precision goal for the value of the projected gradient in the
     *               stopping criterion (after applying x scaling factors)
     *               if pgtol < 0.0, pgtol is set to 1e-2 * sqrt(accuracy)
     *               setting it to 0.0 is not recommended
     * @param rescale 
     *               f scaling factor (in log10) used to trigger f value rescaling
     *               if 0, rescale at each iteration
     *               if a big value, never rescale
     *               if < 0, rescale is set to 1.3
     *
     * @return       Optimized function value.
     * 
     * On output, x, f and g may be very slightly out of sync because of scaling.
     * @throws Exception 
     */
    public double solve(double [] x, double [] g,
            double [] low, double [] up, double [] scale, double [] offset,
            int messages, int maxCGit, int maxnfeval, double eta, double stepmx,
            double accuracy, double fmin, double ftol, double xtol, double pgtol,
            double rescale) throws Exception {

        n = x.length;
        nfeval = 0;

        /* Check for errors in the input parameters */
        if (n <= 0) {
            solverState = SolverState.ConstantProblem;
            return 0.0;
        }

        /* Check bounds arrays */
        if (low == null) {
            low = new double[n];
            Arrays.fill(low, Double.NEGATIVE_INFINITY);
        }
        if (up == null) {
            up = new double[n];
            Arrays.fill(up, Double.POSITIVE_INFINITY);
        }

        /* Coherency check */
        for (int i = 0 ; i < n ; i++) {
            if (low[i] > up [i]) {
                throw new IllegalArgumentException("Lower bound "+i+" larger than upper bound");
            }
        }

        /* Coerce x into bounds */
        coercex(n, x, low, up);

        if (maxnfeval < 1) {
            throw new IllegalArgumentException("maxnfeval < 1");
        }

        /* Allocate g if necessary */
        if(g == null) {
            g = new double[n];
        }

        /* Initial function evaluation */
        fvalue = function(x, g);
        nfeval ++;

        /* Constant problem ? */
        int nc = 0;
        for (int i = 0 ; i < n ; i++) {
            if ((low[i] == up[i]) || (scale != null && scale[i] == 0.0)) {
                x[i] = low[i];
                nc ++;
            }
        }

        if (nc == n) {
            solverState = SolverState.ConstantProblem;
            return 0.0;
        }

        /* Scaling parameters */
        double [] xscale = new double[n];
        double [] xoffset = new double[n];
        fscale = 1.0;

        for (int i = 0 ; i < n ; i++) {
            if (scale != null) {
                xscale[i] = Math.abs(scale[i]);
                if (xscale[i] == 0.0) {
                    xoffset[i] = low[i] = up[i] = x[i];
                }
            }
            else if (low[i] != Double.NEGATIVE_INFINITY && up[i] != Double.POSITIVE_INFINITY) {
                xscale[i] = up[i] - low[i];
                xoffset[i] = (up[i]+low[i])*0.5;
            }
            else {
                xscale[i] = 1.0+Math.abs(x[i]);
                xoffset[i] = x[i];
            }
            if (offset != null) {
                xoffset[i] = offset[i];
            }
        }

        /* Default values for parameters */
        epsmch = mchpr1();
        rteps = Math.sqrt(epsmch);

        if (stepmx < rteps * 10.0) { 
            stepmx = 1.0e1;
        }
        if (eta < 0.0 || eta >= 1.0) {
            eta = 0.25;
        }
        if (rescale < 0) { 
            rescale = 1.3;
        }
        if (maxCGit < 0) /* maxCGit == 0 is valid */ {
            maxCGit = n / 2;
            if (maxCGit < 1) {
                maxCGit = 1;
            }
            else if (maxCGit > 50) {
                maxCGit = 50;
            }
        }
        if (maxCGit > n) { 
            maxCGit = n;
        }
        if (accuracy <= epsmch) {
            accuracy = rteps;
        }
        if (ftol < 0.0) {
            ftol = accuracy;
        }
        if (pgtol < 0.0) {
            pgtol = 1e-2 * Math.sqrt(accuracy);
        }
        if (xtol < 0.0) {
            xtol = rteps;
        }

        /* Optimization */
        tncMinimize(n, x, g,
                xscale, xoffset, low, up, messages,
                maxCGit, maxnfeval, eta, stepmx, accuracy, fmin, ftol, xtol, pgtol,
                rescale);

        return fvalue;
    }

    /**
     * Simpler access to the solver.
     * 
     * @see the full solve() for param descriptions. 
     * 
     * @param x
     * @param low
     * @param up
     * @param messages
     * @param maxnfeval
     * @param accuracy
     * @param fmin
     * @return
     * @throws Exception
     */
    public double solve(double [] x, 
            double [] low, 
            double [] up, 
            int messages, 
            int maxnfeval, 
            double accuracy,
            double fmin,
            double ftol) throws Exception {
        return solve(x, null, low, up, null, null, messages, -1, maxnfeval, -1, -1, accuracy, fmin, ftol, -1, -1, -1);
    }

    /* Coerce x into bounds */
    private void coercex(int n, double [] x, double [] low, double [] up) {
        for (int i = 0 ; i < n ; i++) {
            if (x[i]<low[i]) {
                x[i] = low[i];
            }
            else if (x[i]>up[i]) { 
                x[i] = up[i];
            }
        }
    }

    /* Unscale x */
    private void unscalex(int n, double [] x, double [] xscale, double [] xoffset) {
        for (int i = 0 ; i < n ; i++) {
            x[i] = x[i]*xscale[i]+xoffset[i];
        }
    }

    /* Scale x */
    private void scalex(int n, double [] x, double [] xscale, double [] xoffset) {
        for (int i = 0 ; i < n ; i++) {
            if (xscale[i] > 0.0) {
                x[i] = (x[i]-xoffset[i])/xscale[i];
            }
        }
    }

    /* Scale g */
    private void scaleg(int n, double [] g, double [] xscale, double fscale){
        for (int i = 0 ; i < n ; i++) {
            g[i] *= xscale[i]*fscale;
        }
    }

    /* Calculate the pivot vector */
    private void setConstraints(int n, double [] x, int [] pivot, double [] xscale,
            double [] xoffset, double [] low, double [] up) {
        for (int i = 0; i < n; i++) {
            /* tolerances should be better ajusted */
            if (xscale[i] == 0.0) {
                pivot[i] = 2;
            }
            else {
                if (low[i] != Double.NEGATIVE_INFINITY &&
                        (x[i]*xscale[i]+xoffset[i] - low[i] <= epsmch * 10.0 * (Math.abs(low[i]) + 1.0))) {
                    pivot[i] = -1;
                }
                else {
                    if (up[i] != Double.POSITIVE_INFINITY &&
                            (x[i]*xscale[i]+xoffset[i] - up[i] >= epsmch * 10.0 * (Math.abs(up[i]) + 1.0))) {
                        pivot[i] = 1;
                    }
                    else {
                        pivot[i] = 0;
                    }
                }
            }
        }
    }

    private double fscale; 
    private double fvalue; 


    /*
     * This routine is a bounds-constrained truncated-newton method.
     * the truncated-newton method is preconditioned by a limited-memory
     * quasi-newton method (this preconditioning strategy is developed
     * in this routine) with a further diagonal scaling
     * (see routine diagonalscaling).
     */
    private void tncMinimize(int n, double [] x, 
            double [] gfull, 
            double [] xscale, double [] xoffset, 
            double [] low, double [] up, int messages,
            int maxCGit, int maxnfeval, double eta, double stepmx,
            double accuracy, double fmin, double ftol, double xtol, double pgtol,
            double rescale) throws Exception {
        /* Allocate temporary vectors */
        double [] oldg = new double [n];
        double [] g = new double [n];
        double [] temp = new double [n];
        double [] diagb = new double [n];
        double [] pk = new double [n];
        double [] sk = new double [n];
        double [] yk = new double [n];
        double [] sr = new double [n];
        double [] yr = new double [n];

        int [] pivot = new int [n];

        /* Initialize variables */

        double difnew = 0.0;
        double epsred = 0.05;
        boolean upd1 = true;
        int niter = 0;
        double icycle = n - 1;
        boolean newcon = true;

        /* Uneeded initializations */
        boolean lreset = false;
        double yrsr = 0.0;
        double yksk = 0.0;

        /* Initial scaling */
        scalex(n, x, xscale, xoffset);
        fvalue *= fscale;

        /* initial pivot calculation */
        setConstraints(n, x, pivot, xscale, xoffset, low, up);

        dcopy1(n, gfull, g);
        scaleg(n, g, xscale, fscale);

        /* Test the lagrange multipliers to see if they are non-negative. */
        for (int i = 0; i < n; i++) {
            if (-pivot[i] * g[i] < 0.0) {
                pivot[i] = 0;
            }
        }

        project(n, g, pivot);

        /* Set initial values to other parameters */
        double gnorm = dnrm21(n, g);

        double fLastConstraint = fvalue; /* Value at last constraint */
        double fLastReset = fvalue; /* Value at last reset */

        if ((messages & TNC_MSG_ITER) != 0) {
            log(String.format("  NIT   NF   F                       GTG\n"));
            printCurrentIteration(n, fvalue / fscale, gfull,
                    niter, nfeval, pivot);
        }

        /* Set the diagonal of the approximate hessian to unity. */
        for (int i = 0; i < n; i++) {
            diagb[i] = 1.0;
        }

        /* Start of main iterative loop */
        while(true) {
            /* Local minimum test */
            if (dnrm21(n, g) <= pgtol * fscale) {
                /* |PG| == 0.0 => local minimum */
                dcopy1(n, gfull, g);
                project(n, g, pivot);
                if ((messages & TNC_MSG_INFO) != 0) {
                    log(String.format("tnc: |pg| = %g -> local minimum\n", dnrm21(n, g) / fscale));
                }
                solverState = SolverState.LocalMinimum;
                break;
            }

            /* Terminate if more than maxnfeval evaluations have been made */
            if (nfeval >= maxnfeval) {
                solverState = SolverState.MaxFunctionEvals;
                break;
            }

            /* Rescale function if necessary */
            double newscale = dnrm21(n, g);
            if ((newscale > epsmch) && (Math.abs(Math.log10(newscale)) > rescale)) {

                newscale = 1.0/newscale;

                fvalue *= newscale;
                fscale *= newscale;
                gnorm *= newscale;
                fLastConstraint *= newscale;
                fLastReset *= newscale;
                difnew *= newscale;

                for (int i = 0; i < n; i++) {
                    g[i] *= newscale;
                    diagb[i] = 1.0;
                }

                upd1 = true;
                icycle = n - 1;
                newcon = true;

                if ((messages & TNC_MSG_INFO) != 0) { 
                    log(String.format("tnc: fscale = %g\n", fscale));
                }
            }

            dcopy1(n, x, temp);
            project(n, temp, pivot);
            double xnorm = dnrm21(n, temp);
            double oldnfeval = nfeval;

            /* Compute the new search direction */
            tncDirection(pk, diagb, x, g, n, maxCGit, maxnfeval, 
                    upd1, yksk, yrsr, sk, yk, sr, yr,
                    lreset, xscale, xoffset, fscale,
                    pivot, accuracy, gnorm, xnorm, low, up);
            if (!newcon) {
                if (!lreset) {
                    /* Compute the accumulated step and its corresponding gradient
                      difference. */
                    dxpy1(n, sk, sr);
                    dxpy1(n, yk, yr);
                    icycle++;
                }
                else {
                    /* Initialize the sum of all the changes */
                    dcopy1(n, sk, sr);
                    dcopy1(n, yk, yr);
                    fLastReset = fvalue;
                    icycle = 1;
                }
            }

            dcopy1(n, g, oldg);
            double oldf = fvalue;
            double oldgtp = ddot1(n, pk, g);

            /* Maximum unconstrained step length */
            double ustpmax = stepmx / (dnrm21(n, pk) + epsmch);

            /* Maximum constrained step length */
            double spe = stepMax(ustpmax, n, x, pk, pivot, low, up, xscale, xoffset);

            if (spe > 0.0) {
                /* Set the initial step length */
                alpha = initialStep(fvalue, fmin / fscale, oldgtp, spe);

                /* Perform the linear search */
                LinearSearch lsrc = new LinearSearch(n, low, up,
                        xscale, xoffset, fscale, pivot,
                        eta, ftol, spe, pk, x, gfull, maxnfeval);

                if (lsrc.returnCode == LS_FAIL) {
                    solverState  = SolverState.LinearSearchFailed;
                    break;
                }

                /* If we went up to the maximum unconstrained step, increase it */
                if (alpha >= 0.9 * ustpmax) {
                    stepmx *= 1e2;
                    if ((messages & TNC_MSG_INFO) != 0) {
                        log(String.format("tnc: stepmx = %g\n", stepmx));
                    }
                }

                /* If we went up to the maximum constrained step,
                     a new constraint was encountered */
                if (alpha - spe >= -epsmch * 10.0) {
                    newcon = true;
                }
                else {
                    /* Break if the linear search has failed to find a lower point */
                    if (lsrc.returnCode != LS_OK) {
                        if (lsrc.returnCode == LS_MAXFUN) {
                            solverState = SolverState.MaxFunctionEvals;
                        }
                        else { 
                            solverState= SolverState.LinearSearchFailed;
                        }
                        break;
                    }
                    newcon = false;
                }
            }
            else {
                /* Maximum constrained step == 0.0 => new constraint */
                newcon = true;
            }

            if (newcon) {
                if (!addConstraint(n, x, pk, pivot, low, up, xscale, xoffset)) {
                    if(nfeval == oldnfeval) {
                        solverState = SolverState.NoProgress;
                        break;
                    }
                }

                fLastConstraint = fvalue;
            }

            niter++;

            /* Set up parameters used in convergence and resetting tests */
            double difold = difnew;
            difnew = oldf - fvalue;

            /* If this is the first iteration of a new cycle, compute the
           percentage reduction factor for the resetting test */
            if (icycle == 1) {
                if (difnew > difold * 2.0) { 
                    epsred += epsred;
                }
                if (difnew < difold * 0.5) {
                    epsred *= 0.5;
                }
            }

            dcopy1(n, gfull, g);
            scaleg(n, g, xscale, fscale);

            dcopy1(n, g, temp);
            project(n, temp, pivot);
            gnorm = dnrm21(n, temp);

            /* Reset pivot */
            boolean remcon = removeConstraint(oldgtp, gnorm, pgtol*fscale, fvalue,
                    fLastConstraint, g, pivot, n);

            /* If a constraint is removed */
            if (remcon) {
                /* Recalculate gnorm and reset fLastConstraint */
                dcopy1(n, g, temp);
                project(n, temp, pivot);
                gnorm = dnrm21(n, temp);
                fLastConstraint = fvalue;
            }

            if (!remcon && !newcon) {
                /* No constraint removed & no new constraint : tests for convergence */
                if (Math.abs(difnew) <= ftol*fscale) {
                    if ((messages & TNC_MSG_INFO) != 0) {
                        log(String.format("tnc: |fn-fn-1] = %g -> convergence\n", Math.abs(difnew) / fscale));
                    }
                    solverState = SolverState.Converged;
                    break;
                }
                if (alpha * dnrm21(n, pk) <= xtol) {
                    if ((messages & TNC_MSG_INFO) != 0) {
                        log(String.format("tnc: |xn-xn-1] = %g -> convergence\n", alpha * dnrm21(n, pk)));
                    }
                    solverState = SolverState.ConvergedX;
                    break;
                }
            }

            project(n, g, pivot);

            if ((messages & TNC_MSG_ITER) != 0) { 
                printCurrentIteration(n, fvalue / fscale, gfull,
                        niter, nfeval, pivot);
            }

            /* Compute the change in the iterates and the corresponding change in the
              gradients */
            if (!newcon) {

                for (int i = 0; i < n; i++) {
                    yk[i] = g[i] - oldg[i];
                    sk[i] = alpha * pk[i];
                }

                /* Set up parameters used in updating the preconditioning strategy */
                yksk = ddot1(n, yk, sk);

                if (icycle == (n - 1) || difnew < epsred * (fLastReset - fvalue)) {
                    lreset = true;
                }
                else {
                    yrsr = ddot1(n, yr, sr);
                    if (yrsr <= 0.0) {
                        lreset = true;
                    }
                    else {
                        lreset = false;
                    }
                }
                upd1 = false;
            }
        }

        if ((messages & TNC_MSG_ITER) != 0) {
            printCurrentIteration(n, fvalue / fscale, gfull,
                    niter, nfeval, pivot);
        }

        /* Unscaling */
        unscalex(n, x, xscale, xoffset);
        coercex(n, x, low, up);
        fvalue /= fscale;
        
        if (solverState == SolverState.LinearSearchFailed) {
            throw new Exception(getClass().getSimpleName()+" linear search failed");
        }
        else if (solverState == SolverState.NoProgress) {
            throw new Exception(getClass().getSimpleName()+" no progress");
        }
    }

    /* Print the results of the current iteration */
    private void printCurrentIteration(int n, double f, double [] g, int niter,
            int nfeval, int [] pivot) {
        double gtg = 0.0;
        for (int i = 0; i < n; i++) {
            if (pivot[i] == 0) {
                gtg += g[i] * g[i];
            }
        }

        log(String.format( " %4d %4d %22.15E  %15.8E\n", niter, nfeval, f, gtg));
    }

    /*
     * Set x[i] = 0.0 if direction i is currently constrained
     */
    private void project(int n, double [] x, int [] pivot) {
        for (int i = 0; i < n; i++) {
            if (pivot[i] != 0) {
                x[i] = 0.0;
            }
        }
    }

    /*
     * Set x[i] = 0.0 if direction i is constant
     */
    private void projectConstants(int n, double [] x, double [] xscale) {
        for (int i = 0; i < n; i++) {
            if (xscale[i] == 0.0) {
                x[i] = 0.0;
            }
        }
    }

    /*
     * Compute the maximum allowable step length
     */
    private double stepMax(double step, int n, double [] x, double [] dir,
            int [] pivot, double [] low, double [] up, double [] xscale, double [] xoffset) {
        /* Constrained maximum step */
        for (int i = 0; i < n; i++) {
            if ((pivot[i] == 0) && (dir[i] != 0.0)) {
                if (dir[i] < 0.0) {
                    double t = (low[i]-xoffset[i])/xscale[i] - x[i];
                    if (t > step * dir[i]) {
                        step = t / dir[i];
                    }
                }
                else {
                    double t = (up[i]-xoffset[i])/xscale[i] - x[i];
                    if (t < step * dir[i]) {
                        step = t / dir[i];
                    }
                }
            }
        }

        return step;
    }

    /*
     * Update the constraint vector pivot if a new constraint is encountered
     */
    private boolean addConstraint(int n, double [] x, double [] p, int [] pivot,
            double [] low, double [] up, double [] xscale, double [] xoffset)
    {
        boolean newcon = false;

        for (int i = 0; i < n; i++) {
            if ((pivot[i] == 0) && (p[i] != 0.0)) {
                if (p[i] < 0.0 && low[i] != Double.NEGATIVE_INFINITY) {
                    double tol = epsmch * 10.0 * (Math.abs(low[i]) + 1.0);
                    if (x[i]*xscale[i]+xoffset[i] - low[i] <= tol) {
                        pivot[i] = -1;
                        x[i] = (low[i]-xoffset[i])/xscale[i];
                        newcon = true;
                    }
                }
                else if (up[i] != Double.POSITIVE_INFINITY) {
                    double tol = epsmch * 10.0 * (Math.abs(up[i]) + 1.0);
                    if (up[i] - (x[i]*xscale[i]+xoffset[i]) <= tol) {
                        pivot[i] = 1;
                        x[i] = (up[i]-xoffset[i])/xscale[i];
                        newcon = true;
                    }
                }
            }
        }
        return newcon;
    }

    /*
     * Check if a constraint is no more active
     */
    private boolean removeConstraint(double gtpnew, double gnorm, double pgtolfs,
            double f, double fLastConstraint, double [] g, int [] pivot, int n) {

        if (((fLastConstraint - f) <= (gtpnew * -0.5)) && (gnorm > pgtolfs)) {
            return false;
        }

        int imax = -1;
        double cmax = 0.0;

        for (int i = 0; i < n; i++) {
            if (pivot[i] == 2) {
                continue;
            }
            double t = -pivot[i] * g[i];
            if (t < cmax) {
                cmax = t;
                imax = i;
            }
        }

        if (imax != -1) {
            pivot[imax] = 0;
            return true;
        }
        else {
            return false;
        }

        /*
         * For details, see gill, murray, and wright (1981, p. 308) and
         * fletcher (1981, p. 116). The multiplier tests (here, testing
         * the sign of the components of the gradient) may still need to
         * modified to incorporate tolerances for zero.
         */
    }

    /*
     * This routine performs a preconditioned conjugate-gradient
     * iteration in order to solve the newton equations for a search
     * direction for a truncated-newton algorithm.
     * When the value of the quadratic model is sufficiently reduced,
     * the iteration is terminated.
     */
    private void tncDirection(double [] zsol, double [] diagb,
            double [] x, double g[], int n,
            int maxCGit, int maxnfeval, 
            boolean upd1, double yksk, double yrsr,
            double [] sk, double [] yk, double [] sr, double [] yr,
            boolean lreset, 
            double xscale[], double xoffset[], double fscale,
            int [] pivot, double accuracy,
            double gnorm, double xnorm, double low[], double up[]) throws Exception
    {
        /* Temporary vectors */

        /* No CG it. => dir = -grad */
        if (maxCGit == 0) {
            dcopy1(n, g, zsol);
            dneg1(n, zsol);
            project(n, zsol, pivot);
            return;
        }

        /* General initialization */
        double rhsnrm = gnorm;
        double tol = 1e-12;
        double qold = 0.0;
        double rzold = 0.0; /* Uneeded */

        double [] r = new double [n]; /* Residual */
        double [] v = new double [n];
        double [] zk = new double [n];
        double [] emat = new double [n]; /* Diagonal preconditoning matrix */
        double [] gv = new double [n]; /* hessian times v */

        /* Initialization for preconditioned conjugate-gradient algorithm */
        initPreconditioner(diagb, emat, n, lreset, yksk, yrsr, sk, yk, sr, yr,
                upd1);

        for (int i = 0; i < n; i++) {
            r[i] = -g[i];
            v[i] = 0.0;
            zsol[i] = 0.0; /* Computed search direction */
        }

        /* Main iteration */
        for (int k = 0; k < maxCGit; k++) {
            /* CG iteration to solve system of equations */
            project(n, r, pivot);
            msolve(r, zk, n, sk, yk, diagb, sr, yr, upd1, yksk, yrsr, lreset);

            project(n, zk, pivot);
            double rz = ddot1(n, r, zk);

            if ((rz / rhsnrm < tol) || (nfeval >= (maxnfeval-1))) {
                /* Truncate algorithm in case of an emergency
                 or too many function evaluations */
                if (k == 0) {
                    dcopy1(n, g, zsol);
                    dneg1(n, zsol);
                    project(n, zsol, pivot);
                }
                break;
            }

            double beta; 
            if (k == 0) {
                beta = 0.0;
            }
            else {
                beta = rz / rzold;
            }

            for (int i = 0; i < n; i++) {
                v[i] = zk[i] + beta * v[i];
            }

            project(n, v, pivot);
            hessianTimesVector(v, gv, n, x, g, 
                    xscale, xoffset, fscale, accuracy, xnorm, low, up);
            ++nfeval;

            project(n, gv, pivot);

            double vgv = ddot1(n, v, gv);
            if (vgv / rhsnrm < tol) {
                /* Truncate algorithm in case of an emergency */
                if (k == 0) {
                    msolve(g, zsol, n, sk, yk, diagb, sr, yr, upd1, yksk, yrsr,
                            lreset);
                    dneg1(n, zsol);
                    project(n, zsol, pivot);
                }
                break;
            }
            diagonalScaling(n, emat, v, gv, r);

            /* Compute linear step length */
            double alpha = rz / vgv;

            /* Compute current solution and related vectors */
            daxpy1(n, alpha, v, zsol);
            daxpy1(n, -alpha, gv, r);

            /* Test for convergence */
            double gtp = ddot1(n, zsol, g);
            double pr = ddot1(n, r, zsol);
            double qnew = (gtp + pr) * 0.5;
            double qtest = (k + 1) * (1.0 - qold / qnew);
            if (qtest <= 0.5) {
                break;
            }

            /* Perform cautionary test */
            if (gtp > 0.0) {
                /* Truncate algorithm in case of an emergency */
                daxpy1(n, -alpha, v, zsol);
                break;
            }

            qold = qnew;
            rzold = rz;
        }

        /* Terminate algorithm */
        /* Store (or restore) diagonal preconditioning */
        dcopy1(n, emat, diagb);
    }

    /*
     * Update the preconditioning matrix based on a diagonal version
     * of the bfgs quasi-newton update.
     */
    private void diagonalScaling(int n, double [] e, double [] v, double [] gv,
            double [] r) {
        double vr = 1.0/ddot1(n, v, r);
        double vgv = 1.0/ddot1(n, v, gv);
        for (int i = 0; i < n; i++) {
            e[i] += - r[i]*r[i]*vr + gv[i]*gv[i]*vgv;
            if (e[i] <= 1e-6) {
                e[i] = 1.0;
            }
        }
    }

    /*
     * Returns the length of the initial step to be taken along the
     * vector p in the next linear search.
     */
    private double initialStep(double fnew, double fmin, double gtp, double smax) {
        double d = Math.abs(fnew - fmin);
        double alpha = 1.0;
        if (d * 2.0 <= -(gtp) && d >= epsmch) {
            alpha = d * -2.0 / gtp;
        }
        if (alpha >= smax) {
            alpha = smax;
        }

        return alpha;
    }

    /*
     * Hessian vector product through finite differences
     */
    private void hessianTimesVector(double [] v, double [] gv, int n,
            double [] x, double [] g, 
            double [] xscale, double [] xoffset, double fscale,
            double accuracy, double xnorm, double [] low, double [] up) throws Exception {
        double [] xv = new double [n];

        double delta = accuracy * (xnorm + 1.0);
        for (int i = 0; i < n; i++) {
            xv[i] = x[i] + delta * v[i];
        }

        unscalex(n, xv, xscale, xoffset);
        coercex(n, xv, low, up);
        function(xv, gv);
        scaleg(n, gv, xscale, fscale);

        double dinv = 1.0 / delta;
        for (int i = 0; i < n; i++) {
            gv[i] = (gv[i] - g[i]) * dinv;
        }

        projectConstants(n, gv, xscale);
    }

    /*
     * This routine acts as a preconditioning step for the
     * linear conjugate-gradient routine. It is also the
     * method of computing the search direction from the
     * gradient for the non-linear conjugate-gradient code.
     * It represents a two-step self-scaled bfgs formula.
     */
    private void msolve(double [] g, double [] y, int n,
            double [] sk, double [] yk, double [] diagb, double [] sr,
            double [] yr, boolean upd1, double yksk, double yrsr,
            boolean lreset) {

        if (upd1) {
            for (int i = 0; i < n; i++) {
                y[i] = g[i] / diagb[i];
            }
            return;
        }

        double gsk = ddot1(n, g, sk);
        double [] hg = new double[n];
        double [] hyr = new double[n];
        double [] hyk = new double[n];

        /* Compute gh and hy where h is the inverse of the diagonals */
        if (lreset) {
            for (int i = 0; i < n; i++) {
                double rdiagb = 1.0 / diagb[i];
                hg[i] = g[i] * rdiagb;
                hyk[i] = yk[i] * rdiagb;
            }
            double ykhyk = ddot1(n, yk, hyk);
            double ghyk = ddot1(n, g, hyk);
            ssbfgs(n, 1.0, sk, hg, hyk, yksk, ykhyk, gsk, ghyk, y);
        }
        else {
            for (int i = 0; i < n; i++) {
                double rdiagb = 1.0 / diagb[i];
                hg[i] = g[i] * rdiagb;
                hyk[i] = yk[i] * rdiagb;
                hyr[i] = yr[i] * rdiagb;
            }
            double gsr = ddot1(n, g, sr);
            double ghyr = ddot1(n, g, hyr);
            double yrhyr = ddot1(n, yr, hyr);
            ssbfgs(n, 1.0, sr, hg, hyr, yrsr, yrhyr, gsr, ghyr, hg);
            double yksr = ddot1(n, yk, sr);
            double ykhyr = ddot1(n, yk, hyr);
            ssbfgs(n, 1.0, sr, hyk, hyr, yrsr, yrhyr, yksr, ykhyr, hyk);
            double ykhyk = ddot1(n, hyk, yk);
            double ghyk = ddot1(n, hyk, g);
            ssbfgs(n, 1.0, sk, hg, hyk, yksk, ykhyk, gsk, ghyk, y);
        }
    }

    /*
     * Self-scaled BFGS
     */
    private void ssbfgs(int n, double gamma, double sj[], double hjv[],
            double hjyj[], double yjsj,
            double yjhyj, double vsj, double vhyj, double hjp1v[]) {
        double beta, delta;

        if (yjsj == 0.0) {
            delta = 0.0;
            beta = 0.0;
        }
        else {
            delta = (gamma * yjhyj / yjsj + 1.0) * vsj / yjsj - gamma * vhyj / yjsj;
            beta = -gamma * vsj / yjsj;
        }

        for (int i = 0; i < n; i++) {
            hjp1v[i] = gamma * hjv[i] + delta * sj[i] + beta * hjyj[i];
        }
    }

    /*
     * Initialize the preconditioner
     */
    private void initPreconditioner(double diagb[], double emat[], int n,
            boolean lreset, double yksk, double yrsr,
            double sk[], double yk[], double sr[], double yr[],
            boolean upd1) {
        if (upd1) {
            dcopy1(n, diagb, emat);
            return;
        }

        double [] bsk = new double[n];

        if (lreset) {
            for (int i = 0; i < n; i++) {
                bsk[i] = diagb[i] * sk[i];
            }
            double sds = ddot1(n, sk, bsk);
            if (yksk == 0.0) {
                yksk = 1.0;
            }
            if (sds == 0.0) {
                sds = 1.0;
            }
            for (int  i = 0; i < n; i++) {
                double td = diagb[i];
                emat[i] = td - td * td * sk[i] * sk[i] / sds + yk[i] * yk[i] / yksk;
            }
        }
        else {
            for (int i = 0; i < n; i++) {
                bsk[i] = diagb[i] * sr[i];
            }
            double sds = ddot1(n, sr, bsk);
            double srds = ddot1(n, sk, bsk);
            double yrsk = ddot1(n, yr, sk);
            if (yrsr == 0.0) {
                yrsr = 1.0;
            }
            if (sds == 0.0) {
                sds = 1.0;
            }
            for (int i = 0; i < n; i++) {
                double td = diagb[i];
                bsk[i] = td * sk[i] - bsk[i] * srds / sds + yr[i] * yrsk / yrsr;
                emat[i] = td - td * td * sr[i] * sr[i] / sds + yr[i] * yr[i] / yrsr;
            }
            sds = ddot1(n, sk, bsk);
            if (yksk == 0.0) {
                yksk = 1.0;
            }
            if (sds == 0.0) {
                sds = 1.0;
            }
            for (int i = 0; i < n; i++) {
                emat[i] = emat[i] - bsk[i] * bsk[i] / sds + yk[i] * yk[i] / yksk;
            }
        }
    }

    /*
     * Line search algorithm of gill and murray
     */
    private class LinearSearch {
        final int maxlsit = 64;
        double [] temp;
        double [] tempgfull;
        double [] newgfull;
        double gu;
        double xnorm;
        double epsmch;
        double rteps;
        double pe;
        double reltol;
        double abstol;

        double tnytol;

        double rtsmll;
        double big;
        int itcnt;

        double fpresn;

        double u;
        double fu;
        double fmin;
        double rmu;

        double tol;
        double a;
        double xw;
        double oldf; 
        double fw; 
        double gw; 
        double gmin; 
        double step; 
        double factor;

        boolean braktd;

        double scxbnd;
        double b;
        double e;
        double b1;
        double gtest1;
        double gtest2;


        int returnCode;
        public LinearSearch(int n, 
                double [] low, double [] up,
                double [] xscale, double [] xoffset, double fscale, int [] pivot,
                double eta, double ftol, double xbnd,
                double [] p, double [] x, 
                double [] gfull, int maxnfeval) throws Exception {

            temp = new double [n];
            tempgfull = new double [n];
            newgfull = new double [n];

            dcopy1(n, gfull, temp);
            scaleg(n, temp, xscale, fscale);
            gu = ddot1(n, temp, p);

            dcopy1(n, x, temp);
            project(n, temp, pivot);
            xnorm = dnrm21(n, temp);

            /* Compute the absolute and relative tolerances for the linear search */
            pe = dnrm21(n, p) + epsmch;
            reltol = rteps * (xnorm + 1.0) / pe;
            abstol = -epsmch * (1.0 + Math.abs(fvalue)) / (gu - epsmch);

            /* Compute the smallest allowable spacing between points in the linear
            search */
            tnytol = epsmch * (xnorm + 1.0) / pe;

            rtsmll = epsmch;
            big = 1.0 / (epsmch * epsmch);
            itcnt = 0;

            /* Set the estimated relative precision in f(x). */
            fpresn = ftol;

            u = alpha;
            fu = fvalue;
            fmin = fvalue;
            rmu = 1e-4;

            /* Setup */
            int itest = getptcInit(tnytol, eta, rmu, xbnd);

            /* If itest == GETPTC_EVAL, the algorithm requires the function value to be
            calculated */
            while(itest == GETPTC_EVAL) {
                /* Test for too many iterations or too many function evals */
                if ((++itcnt > maxlsit) || ((nfeval) >= maxnfeval)) {
                    break;
                }

                double ualpha = alpha + u;
                for (int i = 0; i < n; i++) {
                    temp[i] = x[i] + ualpha * p[i];
                }

                /* Function evaluation */
                unscalex(n, temp, xscale, xoffset);
                coercex(n, temp, low, up);

                fu = function(temp, tempgfull);
                ++(nfeval);

                fu *= fscale;

                dcopy1(n, tempgfull, temp);
                scaleg(n, temp, xscale, fscale);
                gu = ddot1(n, temp, p);

                itest = getptcIter(big, rtsmll, tnytol, fpresn,
                        xbnd);

                /* New best point ? */
                if (alpha == ualpha) {
                    dcopy1(n, tempgfull, newgfull);
                }
            }


            if (itest == GETPTC_OK) { 
                /* A successful search has been made */
                fvalue = fmin;
                daxpy1(n, alpha, p, x);
                dcopy1(n, newgfull, gfull);
                returnCode = LS_OK;
            }
            /* Too many iterations ? */
            else if (itcnt > maxlsit) {
                returnCode = LS_FAIL;
            }
            /* If itest=GETPTC_FAIL or GETPTC_EINVAL a lower point could not be found */
            else if (itest != GETPTC_EVAL) {
                returnCode = LS_FAIL;
            }
            /* Too many function evaluations */
            else {
                returnCode = LS_MAXFUN;
            }
        }

        /*
         * getptc, an algorithm for finding a steplength, called repeatedly by
         * routines which require a step length to be computed using cubic
         * interpolation. The parameters contain information about the interval
         * in which a lower point is to be found and from this getptc computes a
         * point at which the function can be evaluated by the calling program.
         */
        private int getptcInit(double tnytol,
                double eta, double rmu, double xbnd) {
            /* Check input parameters */
            if (u <= 0.0 || xbnd <= tnytol || gu > 0.0) {
                return GETPTC_EINVAL;
            }
            if (xbnd < abstol) {
                abstol = xbnd;
            }
            tol = abstol;

            /* a and b define the interval of uncertainty, x and xw are points */
            /* with lowest and second lowest function values so far obtained. */
            /* initialize a,smin,xw at origin and corresponding values of */
            /* function and projection of the gradient along direction of search */
            /* at values for latest estimate at minimum. */

            a = 0.0;
            xw = 0.0;
            alpha = 0.0;
            oldf = fu;
            fmin = fu;
            fw = fu;
            gw = gu;
            gmin = gu;
            step = u;
            factor = 5.0;

            /* The minimum has not yet been bracketed. */
            braktd = false;

            /* Set up xbnd as a bound on the step to be taken. (xbnd is not computed */
            /* explicitly but scxbnd is its scaled value.) Set the upper bound */
            /* on the interval of uncertainty initially to xbnd + tol(xbnd). */
            scxbnd = xbnd;
            b = scxbnd + reltol * Math.abs(scxbnd) + abstol;
            e = b + b;
            b1 = b;

            /* Compute the constants required for the two convergence criteria. */
            gtest1 = -rmu * gu;
            gtest2 = -eta * gu;

            /* If the step is too large, replace by the scaled bound (so as to */
            /* compute the new point on the boundary). */
            if (step >= scxbnd)
            {
                step = scxbnd;
                /* Move sxbd to the left so that sbnd + tol(xbnd) = xbnd. */
                scxbnd -= (reltol * Math.abs(xbnd) + abstol) / (1.0 + reltol);
            }
            u = step;
            if (Math.abs(step) < tol && step < 0.0) {
                u = -(tol);
            }
            if (Math.abs(step) < tol && step >= 0.0) {
                u = tol;
            }
            return GETPTC_EVAL;
        }

        private int getptcIter(double big, double
                rtsmll, double tnytol,
                double fpresn, double xbnd) {
            boolean skipToConvergenceCheck = false;

            /* Update a,b,xw, and xmin */
            if (fu <= fmin) {
                /* If function value not increased, new point becomes next */
                /* origin and other points are scaled accordingly. */
                double chordu = oldf - (alpha + u) * gtest1;
                if (fu > chordu) {
                    /* The new function value does not satisfy the sufficient decrease */
                    /* criterion. prepare to move the upper bound to this point and */
                    /* force the interpolation scheme to either bisect the interval of */
                    /* uncertainty or take the linear interpolation step which estimates */
                    /* the root of f(alpha)=chord(alpha). */

                    double chordm = oldf - alpha * gtest1;
                    gu = -(gmin);
                    double denom = chordm - fmin;
                    if (Math.abs(denom) < 1e-15) {
                        denom = 1e-15;
                        if (chordm - fmin < 0.0) {
                            denom = -denom;
                        }
                    }
                    if (alpha != 0.0) {
                        gu = gmin * (chordu - fu) / denom;
                    }
                    fu = 0.5 * u * (gmin + gu) + fmin;
                    if (fu < fmin) {
                        fu = fmin;
                    }
                }
                else
                {
                    fw = fmin;
                    fmin = fu;
                    gw = gmin;
                    gmin = gu;
                    alpha += u;
                    a -= u;
                    b -= u;
                    xw = -u;
                    scxbnd -= u;
                    if (gu <= 0.0) {
                        a = 0.0;
                    }
                    else {
                        b = 0.0;
                        braktd = true;
                    }
                    tol = Math.abs(alpha) * reltol + abstol;
                    skipToConvergenceCheck = true;

                }
            }

            if (!skipToConvergenceCheck) {
                /* If function value increased, origin remains unchanged */
                /* but new point may now qualify as w. */
                if (u < 0.0) {
                    a = u;
                }
                else
                {
                    b = u;
                    braktd = true;
                }
                xw = u;
                fw = fu;
                gw = gu;
            }

            double twotol = tol + tol;
            double xmidpt = 0.5 * (a + b);

            /* Check termination criteria */
            boolean convrg = (Math.abs(xmidpt) <= twotol - 0.5 * (b - a)) ||
                    (Math.abs(gmin) <= gtest2 && fmin < oldf && ((Math.abs(alpha - xbnd) > tol) ||
                            (! (braktd))));
            if (convrg) {
                if (alpha != 0.0) {
                    return GETPTC_OK;
                }

                /*
                 * If the function has not been reduced, check to see that the relative
                 * change in f(x) is consistent with the estimate of the delta-
                 * unimodality constant, tol. If the change in f(x) is larger than
                 * expected, reduce the value of tol.
                 */
                if (Math.abs(oldf - fw) <= fpresn) {
                    return GETPTC_FAIL;
                }
                tol = 0.1 * tol;
                if (tol < tnytol) {
                    return GETPTC_FAIL;
                }
                reltol = 0.1 * reltol;
                abstol = 0.1 * abstol;
                twotol = 0.1 * twotol;
            }

            /* Continue with the computation of a trial step length */
            double r = 0.0;
            double q = 0.0;
            double s = 0.0;
            boolean skipToMinimumFound = false;
            if (Math.abs(e) > tol) {
                /* Fit cubic through xmin and xw */
                r = 3.0 * (fmin - fw) / xw + gmin + gw;
                double absr = Math.abs(r);
                q = absr;
                if (gw != 0.0 && gmin != 0.0)
                {
                    /* Compute the square root of (r*r - gmin*gw) in a way
                 which avoids underflow and overflow. */
                    double abgw = Math.abs(gw);
                    double abgmin = Math.abs(gmin);
                    s = Math.sqrt(abgmin) * Math.sqrt(abgw);
                    if (gw / abgw * gmin > 0.0) {
                        if (r >= s || r <= -s) {
                            /* Compute the square root of r*r - s*s */
                            q = Math.sqrt(Math.abs(r + s)) * Math.sqrt(Math.abs(r - s));
                        }
                        else {
                            r = 0.0;
                            q = 0.0;
                            skipToMinimumFound = true;
                        }
                    }
                    else {
                        /* Compute the square root of r*r + s*s. */
                        double sumsq = 1.0;
                        double p = 0.0;
                        double scale;
                        if (absr >= s)
                        {
                            /* There is a possibility of underflow. */
                            if (absr > rtsmll) {
                                p = absr * rtsmll;
                            }
                            if (s >= p)
                            {
                                double value = s / absr;
                                sumsq = 1.0 + value * value;
                            }
                            scale = absr;
                        }
                        else
                        {
                            /* There is a possibility of overflow. */
                            if (s > rtsmll) {
                                p = s * rtsmll;
                            }
                            if (absr >= p)
                            {
                                double value = absr / s;
                                sumsq = 1.0 + value * value;
                            }
                            scale = s;
                        }
                        sumsq = Math.sqrt(sumsq);
                        q = big;
                        if (scale < big / sumsq) {
                            q = scale * sumsq;
                        }
                    }
                }

                if (!skipToMinimumFound) {
                    /* Compute the minimum of fitted cubic */
                    if (xw < 0.0) { 
                        q = -q;
                    }
                    s = xw * (gmin - r - q);
                    q = gw - gmin + q + q;
                    if (q > 0.0) {
                        s = -s;
                    }
                    if (q <= 0.0) {
                        q = -q;
                    }
                    r = e;
                    if (b1 != step || braktd) {
                        e = step;
                    }
                }
            }


            /* Construct an artificial bound on the estimated steplength */
            double a1 = a;
            b1 = b;
            step = xmidpt;
            if ( (! braktd) || ((a == 0.0 && xw < 0.0) || (b == 0.0 && xw > 0.0)) ) {
                if (braktd) {
                    /* If the minimum is not bracketed by 0 and xw the step must lie
                     within (a1,b1). */
                    double d1 = xw;
                    double d2 = a;
                    if (a == 0.0) {
                        d2 = b;
                    }
                    /* This line might be : */
                    /* if (*a == 0.0) d2 = *e */
                    u = -d1 / d2;
                    step = 5.0 * d2 * (0.1 + 1.0 / u) / 11.0;
                    if (u < 1.0) { 
                        step = 0.5 * d2 * Math.sqrt(u);
                    }
                }
                else {
                    step = -(factor) * xw;
                    if (step > scxbnd) {
                        step = scxbnd;
                    }
                    if (step != scxbnd) {
                        factor = 5.0 * factor;
                    }
                }
                /* If the minimum is bracketed by 0 and xw the step must lie within (a,b) */
                if (step <= 0.0) { 
                    a1 = step;
                }
                if (step > 0.0) {
                    b1 = step;
                }
            }

            /*
             *   Reject the step obtained by interpolation if it lies outside the
             *   required interval or it is greater than half the step obtained
             *   during the last-but-one iteration.
             */
            if (Math.abs(s) <= Math.abs(0.5 * q * r) || s <= q * a1 || s >= q * b1) {
                e = b - a;
            }
            else {
                /* A cubic interpolation step */
                step = s / q;

                /* The function must not be evaluated too close to a or b. */
                if (step - a < twotol || b - step < twotol) {
                    if (xmidpt <= 0.0) {
                        step = -tol;
                    }
                    else {
                        step = tol;
                    }
                }
            }

            /* If the step is too large, replace by the scaled bound (so as to */
            /* compute the new point on the boundary). */
            if (step >= scxbnd) {
                step = scxbnd;
                /* Move sxbd to the left so that sbnd + tol(xbnd) = xbnd. */
                scxbnd -= (reltol * Math.abs(xbnd) + abstol) / (1.0 + reltol);
            }
            u = step;
            if (Math.abs(step) < tol && step < 0.0) { 
                u = -tol; 
            }
            if (Math.abs(step) < tol && step >= 0.0) { 
                u = tol; 
            }
            return GETPTC_EVAL;
        }
    }

    /*
     * Return epsmch, where epsmch is the smallest possible
     * power of 2 such that 1.0 + epsmch > 1.0
     */
    private double mchpr1() {
        double eps = 1.0;
        while((1.0 + (eps*0.5)) > 1.0) {
            eps *= 0.5;
        }

        return eps;
    }

    /* Blas like routines */

    /* dy+=dx */
    static void dxpy1(int n, double [] dx, double [] dy) {
        for (int i = 0; i < n; i++) {
            dy[i] += dx[i];
        }
    }

    /* dy+=da*dx */
    static void daxpy1(int n, double da, double [] dx, double [] dy) {
        for (int i = 0; i < n; i++) {
            dy[i] += da*dx[i];
        }
    }

    /* Copy dx -> dy */
    static void dcopy1(int n, double [] dx, double [] dy) {
        // TODO: does Java have something faster for that? 
        for (int i = 0; i < n; i++) {
            dy[i] = dx[i];
        }
    }

    /* Negate */
    static void dneg1(int n, double [] v) {
        for (int i = 0; i < n; i++) {
            v[i] = -v[i];
        }
    }

    /* Dot product */
    static double ddot1(int n, double [] dx, double [] dy) {
        double dtemp = 0.0;
        for (int i = 0; i < n; i++) {
            dtemp += dy[i]*dx[i];
        }
        return dtemp;
    }

    /* Euclidian norm */
    static double dnrm21(int n, double dx[]) {
        double dssq = 1.0, dscale = 0.0;

        for (int i = 0; i < n; i++) {
            if (dx[i] != 0.0) {
                double dabsxi = Math.abs(dx[i]);
                if (dscale<dabsxi) {
                    /* Normalization to prevent overflow */
                    double ratio = dscale/dabsxi;
                    dssq = 1.0 + dssq*ratio*ratio;
                    dscale = dabsxi;
                }
                else {
                    double ratio = dabsxi/dscale;
                    dssq += ratio*ratio;
                }
            }
        }

        return dscale*Math.sqrt(dssq);
    }
}