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
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiIRQ;

/**
 * Aspect for IRQ numbers
 *
 * @author Philippe Proulx
 */
public class LamiIRQNumberAspect extends LamiTableEntryAspect {

    private final int fColIndex;

    /**
     * Constructor
     *
     * @param colName
     *            Column name
     * @param colIndex
     *            Column index
     */
    public LamiIRQNumberAspect(String colName, int colIndex) {
        super(colName + " (#)", null); //$NON-NLS-1$
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
        if (data instanceof LamiIRQ) {
            return String.valueOf(((LamiIRQ) data).getNumber());
        }
        /* Could be null, unknown, etc. */
        return data.toString();
    }

    @Override
    public double resolveDouble(LamiTableEntry entry) {
        LamiData data = entry.getValue(fColIndex);
        if (data instanceof LamiIRQ) {
            return ((LamiIRQ) data).getNumber();
        }

        return 0;
    }

    @Override
    public Comparator<LamiTableEntry> getComparator() {
        return (o1, o2) -> Double.compare(resolveDouble(o1), resolveDouble(o2));
    }

}
