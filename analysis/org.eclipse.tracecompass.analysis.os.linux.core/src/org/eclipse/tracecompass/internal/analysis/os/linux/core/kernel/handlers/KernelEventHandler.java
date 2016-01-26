/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

import ca.polymtl.dorsal.statesys.ITmfStateSystemBuilder;
import ca.polymtl.dorsal.statesys.exceptions.AttributeNotFoundException;

/**
 * Base class for all kernel event handlers.
 */
public abstract class KernelEventHandler {

    private final IKernelAnalysisEventLayout fLayout;

    /**
     * Constructor
     *
     * @param layout
     *            the analysis layout
     */
    public KernelEventHandler(IKernelAnalysisEventLayout layout) {
        fLayout = layout;
    }

    /**
     * Get the analysis layout
     *
     * @return the analysis layout
     */
    protected IKernelAnalysisEventLayout getLayout() {
        return fLayout;
    }

    /**
     * Handle a specific kernel event.
     *
     * @param ss
     *            the state system to write to
     * @param event
     *            the event
     * @throws AttributeNotFoundException
     *             if the attribute is not yet create
     */
    public abstract void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException;

}
