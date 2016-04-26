/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.ondemand.lttnganalyses;

/**
 * Wrapper for the "lttng-iolatencystats" analysis.
 *
 * @author Alexandre Montplaisir
 */
public class LttngIOLatencyStatsAnalysis extends AbstractLttngAnalysis {

    /**
     * Constructor
     */
    public LttngIOLatencyStatsAnalysis() {
        super("lttng-iolatencystats-mi"); //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return "LTTng-Analyses - IO Latency Stats"; //$NON-NLS-1$
    }
}
