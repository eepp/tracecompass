/*******************************************************************************
 * Copyright (c) 2013, 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.tests.stubs.analysis;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import ca.polymtl.dorsal.statesys.ITmfStateSystemBuilder;
import ca.polymtl.dorsal.statesys.exceptions.AttributeNotFoundException;
import ca.polymtl.dorsal.statesys.exceptions.StateValueTypeException;
import ca.polymtl.dorsal.statesys.exceptions.TimeRangeException;
import ca.polymtl.dorsal.statesys.statevalue.TmfStateValue;

/**
 * Stub test provider for test state system analysis module
 *
 * @author Geneviève Bastien
 */
public class TestStateSystemProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 1;
    private final String fString = "[]";
    private int fCount = 0;

    /**
     * Constructor
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     */
    public TestStateSystemProvider(@NonNull ITmfTrace trace) {
        super(trace, "Stub State System");
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new TestStateSystemProvider(this.getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());

        /* Just need something to fill the state system */
        if (fString.equals(event.getContent().getValue())) {
            try {
                int quarkId = ss.getQuarkAbsoluteAndAdd("String");
                int quark = ss.getQuarkRelativeAndAdd(quarkId, fString);
                ss.modifyAttribute(event.getTimestamp().getValue(), TmfStateValue.newValueInt(fCount++), quark);
            } catch (TimeRangeException e) {

            } catch (AttributeNotFoundException e) {

            } catch (StateValueTypeException e) {

            }
        }
    }

}
