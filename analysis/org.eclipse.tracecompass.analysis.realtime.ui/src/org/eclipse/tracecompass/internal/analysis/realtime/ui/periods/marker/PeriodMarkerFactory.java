package org.eclipse.tracecompass.internal.analysis.realtime.ui.periods.marker;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.realtime.core.periods.PeriodAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.AbstractTmfTraceAdapterFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;

public class PeriodMarkerFactory extends AbstractTmfTraceAdapterFactory {

    private static final Class<?>[] ADAPTER_LIST = { IMarkerEventSource.class };

    @Override
    public Class<?>[] getAdapterList() {
        return ADAPTER_LIST;
    }

    @Override
    protected <T> @Nullable T getTraceAdapter(@NonNull ITmfTrace trace, @Nullable Class<T> adapterType) {
        if (adapterType != null && IMarkerEventSource.class.equals(adapterType)) {
            PeriodAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace,
                    PeriodAnalysisModule.class, PeriodAnalysisModule.ANALYSIS_ID);
            if (module != null && module.hasRun()) {
                return adapterType.cast(new PeriodMarkerSource(module));
            }
        }
        return null;
    }

}
