/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core.tests.aggregate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.aggregate.AttributePriorityAggregationRule;
import org.eclipse.tracecompass.statesystem.core.aggregate.BitwiseOrAggregationRule;
import org.eclipse.tracecompass.statesystem.core.aggregate.IStateAggregationRule;
import org.eclipse.tracecompass.statesystem.core.aggregate.SymbolicLinkRule;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.junit.Test;

/**
 * Tests for aggregates states that are using other aggregate states.
 *
 * @author Alexandre Montplaisir
 */
public class ChainedAggregatesTest extends AggregationTestBase {

    @Override
    protected IStateAggregationRule createRuleWithParameters(ITmfStateSystemBuilder ssb,
            int targetQuark, List<String @NonNull []> patterns) {
        /* Does not apply to this test */
        throw new UnsupportedOperationException();
    }
    
    @Override
    @Test
    public void testNonExistingQuark() {
        /* Does not apply here */
    }

    @Override
    @Test
    public void testExistingAndNonExistingQuarks() {
        /* Does not apply here */
    }

    /**
     * Build a history like this:
     *
     * <pre>
     *
     * Time --------------------- 1   2   3   4  5  6  7   8  9
     * +-- cpu                    |   |   |   |  |     |   |
     * |    +-- process           [                        ]
     * |    +-- softirq
     * |          +-- raised          [       ]
     * |          +-- active              [         ]
     * |    +-- irq                              [     ]
     * +-- othercpu
     * </pre>
     *
     * <ul>
     * <li>"softirq" is a bitwise-OR aggregate of "raised" and "active"</li>
     * <li>"cpu" is a priority-based aggregate of irq, softirq and process, in
     * this order</li>
     * <li>"othercpu" is simply a symlink to "cpu"</li>
     * </ul>
     */
    @Test
    public void fullTest() {
        ITmfStateSystemBuilder ss = getStateSystem();
        assertNotNull(ss);

        /* State values and attributes that will be used */
        int PROCESS_ACTIVE = (1 << 0);
        int SOFTIRQ_RAISED = (1 << 1);
        int SOFTIRQ_ACTIVE = (1 << 2);
        int IRQ_ACTIVE = (1 << 3);

        ITmfStateValue PROCESS_ACTIVE_VALUE = TmfStateValue.newValueInt(PROCESS_ACTIVE);
        ITmfStateValue SOFTIRQ_RAISED_VALUE = TmfStateValue.newValueInt(SOFTIRQ_RAISED);
        ITmfStateValue SOFTIRQ_ACTIVE_VALUE = TmfStateValue.newValueInt(SOFTIRQ_ACTIVE);
        ITmfStateValue IRQ_ACTIVE_VALUE = TmfStateValue.newValueInt(IRQ_ACTIVE);
        ITmfStateValue NULL_VALUE = TmfStateValue.nullValue();

        ITmfStateValue SOFTIRQ_RAISED_ACTIVE_VALUE =
                TmfStateValue.newValueInt(SOFTIRQ_RAISED | SOFTIRQ_ACTIVE);

        int quarkCpu = ss.getQuarkAbsoluteAndAdd("cpu");
        int quarkProcess = ss.getQuarkAbsoluteAndAdd("cpu", "process");
        int quarkSoftirq = ss.getQuarkAbsoluteAndAdd("cpu", "softirq");
        int quarkSoftirqRaised = ss.getQuarkAbsoluteAndAdd("cpu", "softirq", "raised");
        int quarkSoftirqActive = ss.getQuarkAbsoluteAndAdd("cpu", "softirq", "active");
        int quarkIrq = ss.getQuarkAbsoluteAndAdd("cpu", "irq");
        int quarkOtherCpu = ss.getQuarkAbsoluteAndAdd("othercpu");

        /* Create and register the aggregation rules */
        IStateAggregationRule softIrqRule = new BitwiseOrAggregationRule(ss,
                quarkSoftirq,
                Arrays.asList(
                        new String[] { "cpu", "softirq", "raised" },
                        new String[] { "cpu", "softirq", "active" }));

        IStateAggregationRule cpuRule = new AttributePriorityAggregationRule(ss,
                quarkCpu,
                Arrays.asList(
                        new String[] { "cpu", "irq" },
                        new String[] { "cpu", "softirq" },
                        new String[] { "cpu", "process" }));
        ss.addAggregationRule(cpuRule);

        IStateAggregationRule otherCpuRule = new SymbolicLinkRule(ss,
                quarkOtherCpu,
                new String[] { "cpu" });

        ss.addAggregationRule(softIrqRule);
        ss.addAggregationRule(cpuRule);
        ss.addAggregationRule(otherCpuRule);

        /* Populate the state system */
        try {
            assertEquals(NULL_VALUE, ss.queryOngoingState(quarkOtherCpu));

            ss.modifyAttribute(10, PROCESS_ACTIVE_VALUE, quarkProcess);
            assertEquals(PROCESS_ACTIVE_VALUE, ss.queryOngoingState(quarkCpu));
            assertEquals(PROCESS_ACTIVE_VALUE, ss.queryOngoingState(quarkOtherCpu));

            ss.modifyAttribute(20, SOFTIRQ_RAISED_VALUE, quarkSoftirqRaised);
            assertEquals(SOFTIRQ_RAISED_VALUE, ss.queryOngoingState(quarkCpu));
            assertEquals(SOFTIRQ_RAISED_VALUE, ss.queryOngoingState(quarkOtherCpu));

            ss.modifyAttribute(30, SOFTIRQ_ACTIVE_VALUE, quarkSoftirqActive);
            assertEquals(SOFTIRQ_RAISED_ACTIVE_VALUE, ss.queryOngoingState(quarkCpu));
            assertEquals(SOFTIRQ_RAISED_ACTIVE_VALUE, ss.queryOngoingState(quarkOtherCpu));

            ss.modifyAttribute(40, NULL_VALUE, quarkSoftirqRaised);
            assertEquals(SOFTIRQ_ACTIVE_VALUE, ss.queryOngoingState(quarkCpu));
            assertEquals(SOFTIRQ_ACTIVE_VALUE, ss.queryOngoingState(quarkOtherCpu));

            ss.modifyAttribute(50, IRQ_ACTIVE_VALUE, quarkIrq);
            assertEquals(IRQ_ACTIVE_VALUE, ss.queryOngoingState(quarkCpu));
            assertEquals(IRQ_ACTIVE_VALUE, ss.queryOngoingState(quarkOtherCpu));

            ss.modifyAttribute(60, NULL_VALUE, quarkSoftirqActive);
            assertEquals(IRQ_ACTIVE_VALUE, ss.queryOngoingState(quarkCpu));
            assertEquals(IRQ_ACTIVE_VALUE, ss.queryOngoingState(quarkOtherCpu));

            ss.modifyAttribute(70, NULL_VALUE, quarkIrq);
            assertEquals(PROCESS_ACTIVE_VALUE, ss.queryOngoingState(quarkCpu));
            assertEquals(PROCESS_ACTIVE_VALUE, ss.queryOngoingState(quarkOtherCpu));

            ss.modifyAttribute(80, NULL_VALUE, quarkProcess);
            assertEquals(NULL_VALUE, ss.queryOngoingState(quarkCpu));
            assertEquals(NULL_VALUE, ss.queryOngoingState(quarkOtherCpu));

        } catch (StateValueTypeException | AttributeNotFoundException e) {
            fail(e.getMessage());
        }

        ss.closeHistory(90);

        /* Check the results of queries */
        IntStream.of(quarkCpu, quarkOtherCpu).forEach( quark -> {
            verifyInterval(  5, quark,   0,   9, NULL_VALUE);
            verifyInterval( 15, quark,  10,  19, PROCESS_ACTIVE_VALUE);
            verifyInterval( 25, quark,  20,  29, SOFTIRQ_RAISED_VALUE);
            verifyInterval( 35, quark,  30,  39, SOFTIRQ_RAISED_ACTIVE_VALUE);
            verifyInterval( 45, quark,  40,  49, SOFTIRQ_ACTIVE_VALUE);
            verifyInterval( 55, quark,  50,  69, IRQ_ACTIVE_VALUE);
            verifyInterval( 65, quark,  50,  69, IRQ_ACTIVE_VALUE);
            verifyInterval( 75, quark,  70,  79, PROCESS_ACTIVE_VALUE);
            verifyInterval( 85, quark,  80,  90, NULL_VALUE);
        });
    }
}
