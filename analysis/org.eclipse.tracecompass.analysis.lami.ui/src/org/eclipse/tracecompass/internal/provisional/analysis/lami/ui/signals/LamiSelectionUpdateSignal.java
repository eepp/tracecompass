/*******************************************************************************
 * Copyright (c) 2015 EfficiOS inc, Jonathan Rajotte-Julien
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.signals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;

/**
 * Enable signal sending on selection inside a LamiViewer implementation.
 *
 * @author Jonathan Rajotte-Julien
 */
public class LamiSelectionUpdateSignal extends TmfSignal {

    private final List<Integer> fEntryIndexList;
    private final int fSignalHash;

    /**
     * Constructor for a new signal.
     *
     * @param source
     *            The object sending this signal
     * @param entryIndex
     *            The entry index
     * @param signalHash
     *            The hash for exclusivity signaling
     */
    public LamiSelectionUpdateSignal(Object source, List<Integer> entryIndexList, int signalHash) {
        super(source);
        fEntryIndexList = new ArrayList<>(entryIndexList);
        fSignalHash = signalHash;
    }


    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + " (" + fEntryIndexList + ")]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Getter for the entryIndex
     *
     * @return
     *          The new selected entry
     */
    public List<Integer> getEntryIndex() {
        return fEntryIndexList;
    }


    /**
     * Getter for the exclusivity hash
     *
     * @return
     *          The exclusivity hash
     */
    public int getSignalHash() {
        return fSignalHash;
    }
}
