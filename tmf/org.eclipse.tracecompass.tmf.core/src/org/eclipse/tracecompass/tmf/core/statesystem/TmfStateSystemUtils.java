/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.statesystem;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;

import ca.polymtl.dorsal.statesys.ITmfStateSystem;
import ca.polymtl.dorsal.statesys.exceptions.AttributeNotFoundException;
import ca.polymtl.dorsal.statesys.exceptions.StateSystemDisposedException;
import ca.polymtl.dorsal.statesys.exceptions.TimeRangeException;
import ca.polymtl.dorsal.statesys.interval.ITmfStateInterval;

/**
 * Utility methods around the state system
 *
 * @author Alexandre Montplaisir
 * @since 2.0
 */
public final class TmfStateSystemUtils {

    private TmfStateSystemUtils() {}

    /**
     * Return the state history of a given attribute, but with at most one
     * update per "resolution". This can be useful for populating views (where
     * it's useless to have more than one query per pixel, for example). A
     * progress monitor can be used to cancel the query before completion.
     *
     * @param ss
     *            The state system to query
     * @param attributeQuark
     *            Which attribute this query is interested in
     * @param t1
     *            Start time of the range query
     * @param t2
     *            Target end time of the query. If t2 is greater than the end of
     *            the trace, we will return what we have up to the end of the
     *            history.
     * @param resolution
     *            The "step" of this query
     * @param monitor
     *            A progress monitor. If the monitor is canceled during a query,
     *            we will return what has been found up to that point. You can
     *            use "null" if you do not want to use one.
     * @return The List of states that happened between t1 and t2
     * @throws TimeRangeException
     *             If t1 is invalid, if t2 <= t1, or if the resolution isn't
     *             greater than zero.
     * @throws AttributeNotFoundException
     *             If the attribute doesn't exist
     * @throws StateSystemDisposedException
     *             If the query is sent after the state system has been disposed
     */
    public static List<ITmfStateInterval> queryHistoryRange(ITmfStateSystem ss,
            int attributeQuark, long t1, long t2, long resolution,
            @Nullable IProgressMonitor monitor)
            throws AttributeNotFoundException, StateSystemDisposedException {
        List<ITmfStateInterval> intervals = new LinkedList<>();
        ITmfStateInterval currentInterval = null;
        long ts, tEnd;

        /* Make sure the time range makes sense */
        if (t2 < t1 || resolution <= 0) {
            throw new TimeRangeException(ss.getSSID() + " Start:" + t1 + ", End:" + t2 + ", Resolution:" + resolution); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        /* Set the actual, valid end time of the range query */
        if (t2 > ss.getCurrentEndTime()) {
            tEnd = ss.getCurrentEndTime();
        } else {
            tEnd = t2;
        }

        IProgressMonitor mon = monitor;
        if (mon == null) {
            mon = new NullProgressMonitor();
        }

        /*
         * Iterate over the "resolution points". We skip unneeded queries in the
         * case the current interval is longer than the resolution.
         */
        for (ts = t1; ts <= tEnd; ts += ((currentInterval.getEndTime() - ts) / resolution + 1) * resolution) {
            if (mon.isCanceled()) {
                return intervals;
            }
            currentInterval = ss.querySingleState(ts, attributeQuark);
            intervals.add(currentInterval);
        }

        /* Add the interval at t2, if it wasn't included already. */
        if (currentInterval != null && currentInterval.getEndTime() < tEnd) {
            currentInterval = ss.querySingleState(tEnd, attributeQuark);
            intervals.add(currentInterval);
        }
        return intervals;
    }
}
