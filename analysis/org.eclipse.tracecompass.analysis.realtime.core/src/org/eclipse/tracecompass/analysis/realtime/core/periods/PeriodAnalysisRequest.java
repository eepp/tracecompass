package org.eclipse.tracecompass.analysis.realtime.core.periods;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule.AbstractSegmentStoreAnalysisRequest;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

class PeriodAnalysisRequest extends AbstractSegmentStoreAnalysisRequest {

    private final PeriodAnalysisModuleConfig fConfig;

    private @Nullable ITmfEvent fLastPeriodBeginEvent = null;
    private @Nullable ITmfEvent fLastSeenEvent = null;

    public PeriodAnalysisRequest(ISegmentStore<ISegment> periods, PeriodAnalysisModuleConfig config) {
        super(periods);
        fConfig = config;
    }

    @Override
    public void handleData(final ITmfEvent event) {
        super.handleData(event);
        fLastSeenEvent = event;

        ITmfEvent lastBeginEvent = fLastPeriodBeginEvent;
        if (lastBeginEvent != null) {
            /*
             * We are currently in a period, check if the current event ends it.
             */
            if (fConfig.eventISPeriodEnd(event)) {
                /* End and save the current period */
                Period finishedPeriod = new Period(lastBeginEvent, event);
                getSegmentStore().add(finishedPeriod);
                fLastPeriodBeginEvent = null;
            }
        }

        /*
         * Note we do not do if/else here. The current event may both end the
         * previous period and start a new one.
         */

        if (fLastPeriodBeginEvent == null) {
            /* We are not in a period, check if the current event starts one */
            if (fConfig.eventIsPeriodStart(event)) {
                fLastPeriodBeginEvent = event;
            }
        }
    }

    @Override
    public void handleCompleted() {
        ITmfEvent lastBeginEvent = fLastPeriodBeginEvent;
        ITmfEvent lastSeenEvent = fLastSeenEvent;
        if (lastBeginEvent != null && lastSeenEvent != null) {
            /*
             * We are currently in a period, we will end it and assign the last
             * event of the trace as its end.
             */
            Period lastPeriod = new Period(lastBeginEvent, lastSeenEvent);
            getSegmentStore().add(lastPeriod);
        }

        fLastPeriodBeginEvent = null;
        fLastSeenEvent = null;
        super.handleCompleted();
    }
}