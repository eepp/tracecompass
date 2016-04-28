/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableEntry;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiData;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;

/**
 * Aspect for a time range duration
 *
 * @author Alexandre Montplaisir
 */
public class LamiTimeRangeEndAspect extends LamiTableEntryAspect {

    private final int fColIndex;

    /**
     * Constructor
     *
     * @param timeRangeName
     *            Name of the time range
     * @param colIndex
     *            Column index
     */
    public LamiTimeRangeEndAspect(String timeRangeName, int colIndex) {
        super(timeRangeName + " (" + Messages.LamiAspect_TimeRangeEnd + ')', null); //$NON-NLS-1$
        fColIndex = colIndex;
    }

    @Override
    public boolean isNumerical() {
        return true;
    }

    @Override
    public @Nullable String resolveString(LamiTableEntry entry) {
        LamiData data = entry.getValue(fColIndex);
        if (data instanceof LamiTimeRange) {
            LamiTimeRange range = (LamiTimeRange) data;
            return TmfTimestampFormat.getDefaulTimeFormat().format(range.getEnd());
        }
        return data.toString();
    }

    @Override
    public boolean isTimeStamp() {
        return true;
    }

    @Override
    public double resolveDouble(@NonNull LamiTableEntry entry) {
        LamiData data = entry.getValue(fColIndex);
        if (data instanceof LamiTimeRange) {
            LamiTimeRange range = (LamiTimeRange) data;
            return range.getEnd();
        }
        return 0;
    }

}
