package org.eclipse.tracecompass.analysis.realtime.core.periods;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

public class Period implements ISegment {

    private static final long serialVersionUID = 7773169046654993492L;

    private static boolean theCurrentShading;

    private final ITmfEvent fStartEvent;
    private final ITmfEvent fEndEvent;

    private final boolean fIsDark;

    public Period(ITmfEvent startEvent, ITmfEvent endEvent) {
        fStartEvent = startEvent;
        fEndEvent = endEvent;

        synchronized(Period.class) {
            fIsDark = theCurrentShading;
            theCurrentShading = !theCurrentShading;
        }
    }

    @Override
    public long getStart() {
        return fStartEvent.getTimestamp().getValue();
    }

    @Override
    public long getEnd() {
        return fEndEvent.getTimestamp().getValue();
    }

    @Override
    public int compareTo(@NonNull ISegment o) {
        return Long.compare(this.getStart(), o.getStart());
    }

    @Override
    public @NonNull String getName() {
        return "Period";
    }

    /**
     * Visual display hint (should be in the base classes...)
     */
    public boolean isDark() {
        return fIsDark;
    }

}
