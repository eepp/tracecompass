package org.eclipse.tracecompass.internal.analysis.realtime.ui.periods.marker;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.analysis.realtime.core.periods.Period;
import org.eclipse.tracecompass.analysis.realtime.core.periods.PeriodAnalysisModule;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;

public class PeriodMarkerSource implements IMarkerEventSource {

    private static final RGBA LIGHT_SHADING_COLOR = new RGBA(0xD6, 0xD6, 0xD6, 128);
    private static final RGBA DARK_SHADING_COLOR = new RGBA(0xBB, 0xBB, 0xBB, 128);

    private final PeriodAnalysisModule fModule;

    public PeriodMarkerSource(PeriodAnalysisModule module) {
        fModule = module;
    }

    @Override
    public List<@NonNull String> getMarkerCategories() {
        return Collections.singletonList("Periods");
    }

    @Override
    public List<@NonNull IMarkerEvent> getMarkerList(@NonNull String category, long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        ISegmentStore<ISegment> results = fModule.getSegmentStore();
        if (results == null) {
            return Collections.EMPTY_LIST;
        }
        return results.stream()
            .filter(segment -> (segment.getEnd() > startTime) && (segment.getStart() < endTime))
            .map(segment -> (Period) segment)
            .map(period -> createMarker(category, period))
            .collect(Collectors.toList());
    }

    private static MarkerEvent createMarker(String category, Period period) {
        RGBA color = (period.isDark() ? DARK_SHADING_COLOR : LIGHT_SHADING_COLOR);

        return new MarkerEvent(null, period.getStart(), period.getLength(),
                category, color, "", true); //$NON-NLS-1$
    }

}
