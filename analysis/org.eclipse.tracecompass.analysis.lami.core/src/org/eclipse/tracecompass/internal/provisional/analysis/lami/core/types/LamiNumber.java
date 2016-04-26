/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

class LamiNumber extends LamiData {

    private final double fValue;

    public LamiNumber(double value) {
        fValue = value;
    }

    public double getValue() {
        return fValue;
    }

    @Override
    public String toString() {
        return checkNotNull(Double.toString(fValue));
    }
}
