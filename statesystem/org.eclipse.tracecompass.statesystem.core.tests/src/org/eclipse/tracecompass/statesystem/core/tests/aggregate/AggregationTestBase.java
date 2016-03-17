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
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.aggregate.IStateAggregationRule;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Base class for aggregation tests.
 *
 * @author Alexandre Montplaisir
 */
public abstract class AggregationTestBase {

    private ITmfStateSystemBuilder fStateSystem;

    /**
     * Test setup
     */
    @Before
    public void setup() {
        IStateHistoryBackend backend = StateHistoryBackendFactory.createInMemoryBackend("test-ss", 0);
        fStateSystem = StateSystemFactory.newStateSystem(backend);
    }

    /**
     * Clean-up
     */
    @After
    public void teardown() {
        if (fStateSystem != null) {
            fStateSystem.dispose();
        }
    }

    /**
     * @return The state system test fixture
     */
    protected final ITmfStateSystemBuilder getStateSystem() {
        return fStateSystem;
    }

    /**
     * Verify the contents of an interval. The interval will be obtained and
     * tested both with a single and a full query.
     *
     * @param timestamp
     *            The timestamp of the query
     * @param quark
     *            The target quark of the query
     * @param expectedStartTime
     *            The expected start time of the interval
     * @param expectedEndTime
     *            The expected end time of the interval
     * @param expectedValue
     *            The expected state value
     */
    protected final void verifyInterval(long timestamp, int quark,
            long expectedStartTime,
            long expectedEndTime,
            ITmfStateValue expectedValue) {

        ITmfStateSystem ss = getStateSystem();
        try {
            ITmfStateInterval interval1 = ss.querySingleState(timestamp, quark);
            assertEquals(expectedStartTime, interval1.getStartTime());
            assertEquals(expectedEndTime, interval1.getEndTime());
            assertEquals(quark, interval1.getAttribute());
            assertEquals(expectedValue, interval1.getStateValue());

            ITmfStateInterval interval2 = ss.queryFullState(timestamp).get(quark);
            assertEquals(expectedStartTime, interval2.getStartTime());
            assertEquals(expectedEndTime, interval2.getEndTime());
            assertEquals(quark, interval2.getAttribute());
            assertEquals(expectedValue, interval2.getStateValue());

        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Create the rule type handle by the test sub-class.
     *
     * @param ssb
     *            Same as the constructor parameter
     * @param targetQuark
     *            Same as the constructor parameter
     * @param patterns
     *            Same as the constructor parameter
     * @return The test rule
     */
    protected abstract @NonNull IStateAggregationRule createRuleWithParameters(@NonNull ITmfStateSystemBuilder ssb,
            int targetQuark,
            @NonNull List<String @NonNull[]> patterns);

    /**
     * Test a rule pointing to only one quark, which does not exist.
     *
     * <pre>
     * quarks
     *         + --target_quark
     *         + --(invalid_quark)
     * </pre>
     *
     * "target_quark" will point to "invalid_quark", which will never be
     * created.
     *
     * It should always return null values, and the interval range should be
     * equal to the full history range. If an aggregate rule does not work this
     * way for some reason, override this test accordingly.
     */
    @Test
    public void testNonExistingQuark() {
        ITmfStateSystemBuilder ss = getStateSystem();
        assertNotNull(ss);

        int targetQuark = ss.getQuarkAbsoluteAndAdd("quarks", "target_quark");

        ITmfStateValue NULL_VALUE = TmfStateValue.nullValue();

        IStateAggregationRule rule = createRuleWithParameters(ss, targetQuark,
                Collections.singletonList(new String [] { "quarks", "invalid_quark" }));

        ss.addAggregationRule(rule);

        ss.closeHistory(10);

        verifyInterval(5, targetQuark, 0, 10, NULL_VALUE);
    }

    /**
     * Test a rule setup with quark paths, one that exists, another that does
     * not.
     *
     * <pre>
     * quarks
     *         + --target_quark
     *         + --valid_quark
     *         + --(invalid_quark)
     * </pre>
     *
     * "target_quark" will point to both "valid_quark" and "invalid_quark", but
     * the latter will never be created. The aggregation should report only the
     * values of "valid_quark", ignoring the other.
     *
     * If a rule implementation does not aggregate a single valid quark this
     * way, then please override this test accordingly.
     */
    @Test
    public void testExistingAndNonExistingQuarks() {
        ITmfStateSystemBuilder ss = getStateSystem();
        assertNotNull(ss);

        int validQuark = ss.getQuarkAbsoluteAndAdd("quarks", "valid_quark");
        int targetQuark = ss.getQuarkAbsoluteAndAdd("quarks", "target_quark");

        ITmfStateValue STATE_VALUE = TmfStateValue.newValueInt(1);
        ITmfStateValue NULL_VALUE = TmfStateValue.nullValue();

        IStateAggregationRule rule = createRuleWithParameters(ss, targetQuark,
                Arrays.asList(
                        new String[] { "quarks", "valid_quark" },
                        new String[] { "quarks", "invalid_quark" }
                        ));

        ss.addAggregationRule(rule);

        try {
            assertEquals(NULL_VALUE, ss.queryOngoingState(targetQuark));

            ss.modifyAttribute(10, STATE_VALUE, validQuark);

            assertEquals(STATE_VALUE, ss.queryOngoingState(targetQuark));
            verifyInterval(5, targetQuark, 0, 9, NULL_VALUE);

            ss.closeHistory(20);

            verifyInterval(15, targetQuark, 10, 20, STATE_VALUE);

        } catch (AttributeNotFoundException e) {
            fail(e.getMessage());
        }
    }

}

