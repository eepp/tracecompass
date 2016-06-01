/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Philippe Proulx
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types;

import org.eclipse.jdt.annotation.Nullable;

class LamiCPU extends LamiData {
    private final int fId;

    public LamiCPU(int id) {
        fId = id;
    }

    public int getId() {
        return fId;
    }

    @Override
    public @Nullable String toString() {
        return Integer.toString(fId);
    }
}
