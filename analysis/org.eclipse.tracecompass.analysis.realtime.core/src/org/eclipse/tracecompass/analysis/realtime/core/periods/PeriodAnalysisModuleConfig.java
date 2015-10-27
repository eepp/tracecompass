/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.realtime.core.periods;

import java.util.function.Function;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

public class PeriodAnalysisModuleConfig {

    private final Function<ITmfEvent, Boolean> fPeriodStartFunction;
    private final Function<ITmfEvent, Boolean> fPeriodEndFunction;

    public PeriodAnalysisModuleConfig(Function<ITmfEvent, Boolean> periodStartFunction,
            Function<ITmfEvent, Boolean> periodEndFunction) {
        fPeriodStartFunction = periodStartFunction;
        fPeriodEndFunction = periodEndFunction;
    }

    public boolean eventIsPeriodStart(ITmfEvent event) {
        return fPeriodStartFunction.apply(event).booleanValue();
    }

    public boolean eventISPeriodEnd(ITmfEvent event) {
        return fPeriodEndFunction.apply(event).booleanValue();
    }
}
