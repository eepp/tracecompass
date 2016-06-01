/*******************************************************************************
 * Copyright (c) 2015, 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Lami time range data type
 *
 * @author Alexandre Montplaisir
 */
public class LamiTimeRange extends LamiData {

    private final LamiTimestamp fBegin;
    private final LamiTimestamp fEnd;

    /**
     * Construct a new time range
     *
     * @param begin
     *            Begin time
     * @param end
     *            End time
     */
    public LamiTimeRange(LamiTimestamp begin, LamiTimestamp end) {
        fBegin = begin;
        fEnd = end;
    }

    /**
     * Get the start time of this time range.
     *
     * @return The start time
     */
    public LamiTimestamp getBegin() {
        return fBegin;
    }

    /**
     * Get the end time of this time range.
     *
     * @return The end time
     */
    public LamiTimestamp getEnd() {
        return fEnd;
    }

    /**
     * Get the duration of this time range.
     *
     * @return The duration
     */
    public @Nullable Long getDuration() {
        // TODO: Consider the low and high limits.

        // If both values exist, use the difference as the value
        Number begin = fBegin.getValue();
        Number end = fEnd.getValue();

        if (begin != null && end != null) {
            return end.longValue() - begin.longValue();
        }

        return null;
    }

    @Override
    public @Nullable String toString() {
        Number startValue = fBegin.getValue();
        Number endValue = fEnd.getValue();

        // TODO: The string should probably include the low and
        //       high limits here.
        if (startValue != null && endValue != null) {
            return "[" + String.valueOf(fBegin) + " - " + String.valueOf(fEnd) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        return null;
    }
}
