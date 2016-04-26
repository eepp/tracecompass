/*******************************************************************************
 * Copyright (c) 2016 EfficiOS inc, Philippe Proulx
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect;

import java.util.Comparator;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableEntry;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiData;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiProcess;

/**
 * Aspect for process PID
 *
 * @author Philippe Proulx
 */
public class LamiProcessTIDAspect extends LamiTableEntryAspect {

    private final int fColIndex;

    /**
     * Constructor
     *
     * @param colName
     *            Column name
     * @param colIndex
     *            Column index
     */
    public LamiProcessTIDAspect(String colName, int colIndex) {
        super(colName + " (TID)", null); //$NON-NLS-1$
        fColIndex = colIndex;
    }

    @Override
    public boolean isNumerical() {
        return true;
    }

    @Override
    public boolean isTimeStamp() {
        return false;
    }

    @Override
    public @Nullable String resolveString(LamiTableEntry entry) {
        LamiData data = entry.getValue(fColIndex);
        if (data instanceof LamiProcess) {
            Long tid = ((LamiProcess) data).getTID();

            if (tid == null) {
                return null;
            }

            return tid.toString();
        }
        /* Could be null, unknown, etc. */
        return data.toString();
    }

    @Override
    public double resolveDouble(LamiTableEntry entry) {
        LamiData data = entry.getValue(fColIndex);
        if (data instanceof LamiProcess) {
            Long tid = ((LamiProcess) data).getTID();

            if (tid == null) {
                return 0;
            }

            return tid;
        }

        return 0;
    }

    @Override
    public Comparator<LamiTableEntry> getComparator() {
        return (o1, o2) -> Double.compare(resolveDouble(o1), resolveDouble(o2));
    }

}
