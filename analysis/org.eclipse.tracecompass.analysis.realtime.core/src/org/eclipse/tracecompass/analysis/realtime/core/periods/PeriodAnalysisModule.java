/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.realtime.core.periods;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;

import com.google.common.collect.ImmutableList;

/**
 * @author Alexandre Montplaisir
 * @since 2.0
 */
public class PeriodAnalysisModule extends AbstractSegmentStoreAnalysisModule {

    // ------------------------------------------------------------------------
    // Class attributes
    // ------------------------------------------------------------------------

    /**
     * The ID of this analysis
     */
    public static final String ANALYSIS_ID = "org.eclipse.tracecompass.analysis.realtime.periods"; //$NON-NLS-1$

    private static final String DATA_FILENAME = "periods-analysis.dat"; //$NON-NLS-1$

    private static final Collection<ISegmentAspect> BASE_ASPECTS = ImmutableList.of();

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    private @Nullable PeriodAnalysisModuleConfig fConfig = null;

    private boolean fHasRun = false;

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    public void setConfig(PeriodAnalysisModuleConfig config) {
        fConfig = config;
    }

    // ------------------------------------------------------------------------
    // AbstractSegmentStoreAnalysisModule
    // ------------------------------------------------------------------------

    @Override
    public String getId() {
        return ANALYSIS_ID;
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return BASE_ASPECTS;
    }

    @Override
    public @NonNull String getDataFileName() {
        return DATA_FILENAME;
    }

    @Override
    public AbstractSegmentStoreAnalysisRequest createAnalysisRequest(ISegmentStore<ISegment> periods) {
        return new PeriodAnalysisRequest(periods, checkNotNull(fConfig));
    }

    @Override
    protected Object[] readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        return checkNotNull((Object[]) ois.readObject());
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        /* Only run if the config has been set */
        PeriodAnalysisModuleConfig config = fConfig;
        if (config == null) {
            return false;
        }

        boolean ret = super.executeAnalysis(monitor);
        if (ret) {
            fHasRun = true;
        }
        return ret;
    }

    public boolean hasRun() {
        return fHasRun;
    }

}
