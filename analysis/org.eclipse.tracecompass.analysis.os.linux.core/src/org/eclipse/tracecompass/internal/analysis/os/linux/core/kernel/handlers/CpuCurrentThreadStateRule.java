package org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers;

import java.util.Collections;

import org.eclipse.tracecompass.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.aggregate.AbstractStateAggregationRule;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;


/**
 * Given that (a subset of) the KSP attribute tree looks like this:
 *
 * <pre>
 * +-- CPUs
 * |    +-- 0
 * |    |   +-- Current_thread
 * |    |   +-- Process_state
 * |    +-- 1
 * |    |   ...
 * |   ...
 * |
 * +-- Threads
 *      +-- 1000
 *           +-- Status
 * </pre>
 *
 * "Current_thread" is populated normally by the state provider.
 *
 * This rule is meant to be mounted on "Process_state". It will look at the
 * value of Current_thread, get the corresponding node for this thread under the
 * Threads sub-tree, and use the value of it's "Status" sub-attribute.
 *
 * @author alexandre
 *
 */
public class CpuCurrentThreadStateRule extends AbstractStateAggregationRule {

    protected CpuCurrentThreadStateRule(ITmfStateSystemBuilder ssb, int targetQuark) {
        super(ssb, targetQuark, Collections.EMPTY_LIST);
    }

    @Override
    public ITmfStateValue getOngoingAggregatedState() {
        ITmfStateSystem ss = getStateSystem();

        try {
            /*
             * First query the state of our sibling "Current_thread" attribute
             */
            int thisQuark = getTargetQuark();
            int cpuQuark = ss.getParentAttributeQuark(thisQuark);

            int curThreadQuark = ss.getQuarkRelative(cpuQuark, Attributes.CURRENT_THREAD);
            ITmfStateValue curThreadValue = ss.queryOngoingState(curThreadQuark);

            if (curThreadValue.isNull()) {
                return TmfStateValue.nullValue();
            }

            int thread = curThreadValue.unboxInt();

            /*
             * Now go look for the state of that thread's Status and report that.
             */
            int statusQuark = ss.getQuarkAbsolute(Attributes.THREADS, Integer.toString(thread), Attributes.STATUS);
            ITmfStateValue statusValue = ss.queryOngoingState(statusQuark);
            return statusValue;

        } catch (AttributeNotFoundException e) {
            /*
             * Required attributes have not been created yet, we'll return null
             * for now.
             */
            return TmfStateValue.nullValue();
        }
    }

    @Override
    public ITmfStateInterval getAggregatedState(long timestamp) {
        // TODO Similar steps as above for this method, except we'd use
        // querySingleState(timestamp)
        return null;
    }

}
