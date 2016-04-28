/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Philippe Proulx
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableEntry;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiData;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiProcess;

/**
 * Aspect for Process names
 *
 * @author Philippe Proulx
 */
public class LamiProcessNameAspect extends LamiTableEntryAspect {

    private final int fColIndex;

    /**
     * Constructor
     *
     * @param colName
     *            Column name
     * @param colIndex
     *            Column index
     */
    public LamiProcessNameAspect(String colName, int colIndex) {
        super(colName + " (" + Messages.LamiAspect_Name +')', null); //$NON-NLS-1$
        fColIndex = colIndex;
    }

    @Override
    public boolean isNumerical() {
        return false;
    }

    @Override
    public boolean isTimeStamp() {
        return false;
    }

    @Override
    public @Nullable String resolveString(LamiTableEntry entry) {
        LamiData data = entry.getValue(fColIndex);
        if (data instanceof LamiProcess) {
            return ((LamiProcess) data).getName();
        }
        /* Could be null, unknown, etc. */
        return data.toString();
    }

    @Override
    public double resolveDouble(LamiTableEntry entry) {
        return 0;
    }

}
