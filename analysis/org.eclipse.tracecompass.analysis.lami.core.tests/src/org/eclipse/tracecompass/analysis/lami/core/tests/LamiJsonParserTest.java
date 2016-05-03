/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Michael Jeanson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.lami.core.tests;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect.LamiTableEntryAspect;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiResultTable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableClass;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableEntry;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiBitrate;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiData;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiDuration;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiSize;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiSystemCall;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimeRange;
import org.eclipse.tracecompass.tmf.core.analysis.ondemand.OnDemandAnalysisException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Test cases to verify the JSON parsing of LamiAnalyses.
 */
public class LamiJsonParserTest {

    private static final double DELTA = 0.001;

    private static final String TRACEPATH = "fake/path/to/trace";
    private LamiTmfTraceStub fTrace;

    /**
     * Extend TmfTraceStub to return a fake path.
     */
    private static class LamiTmfTraceStub extends TmfTraceStub {
        @Override
        public String getPath() {
            return TRACEPATH;
        }
    }

    /**
     * Test setup
     */
    @Before
    public void setup() {
        fTrace = new LamiTmfTraceStub();
    }

    /**
     * Test teardown
     */
    @After
    public void teardown() {
        fTrace.dispose();
    }

    /**
     * Test the metadata parsing.
     */
    @Test
    public void testMetadata() {
        LamiAnalysisStub analysis = new LamiAnalysisStub("test-metadata.json", "test-results.json");

        assertTrue(analysis.canExecute(fTrace));
        assertEquals("LAMI test", analysis.getAnalysisTitle());

        Map<String, LamiTableClass> tableModels = analysis.getTableModels();

        /* Table models tests */
        assertNotNull(tableModels);
        assertFalse(tableModels.isEmpty());
        assertEquals(3, tableModels.size());

        /* Table class tests */
        LamiTableClass perSyscallClass = tableModels.get("per-syscall");
        assertNotNull(perSyscallClass);
        LamiTableClass perProcessClass = tableModels.get("per-proc");
        assertNotNull(perProcessClass);
        LamiTableClass perInterruptClass = tableModels.get("per-irq");
        assertNotNull(perInterruptClass);

        assertEquals("Per-syscall stuff", perSyscallClass.getTableTitle());
        assertEquals("Per-process stuff", perProcessClass.getTableTitle());
        assertEquals("Per-interrupt stuff", perInterruptClass.getTableTitle());

        /* Aspects tests */
        List<LamiTableEntryAspect> aspects = perSyscallClass.getAspects();

        assertFalse(aspects.isEmpty());
        assertEquals(8, aspects.size());

        assertEquals("System call", aspects.get(0).getName());
        assertEquals("Duration (ns)", aspects.get(1).getName());
        assertEquals("Size (bytes)", aspects.get(2).getName());
        assertEquals("Bitrate (bps)", aspects.get(3).getName());
        assertEquals("Time range (begin)", aspects.get(4).getName());
        assertEquals("Time range (end)", aspects.get(5).getName());
        assertEquals("Time range (duration) (ns)", aspects.get(6).getName());
        assertEquals("", aspects.get(7).getName()); // Empty aspect to fix SWT display bug
    }

    /**
     * Test the results parsing.
     *
     * @throws OnDemandAnalysisException when execute() fails.
     */
    @Test
    public void testResults() throws OnDemandAnalysisException {
        LamiAnalysisStub analysis = new LamiAnalysisStub("test-metadata.json", "test-results.json");

        List<LamiResultTable> resultTables = analysis.execute(fTrace, null, "", new NullProgressMonitor());

        assertFalse(resultTables.isEmpty());
        assertEquals(4, resultTables.size());

        LamiResultTable perProcessTable = resultTables.get(0);
        LamiResultTable perSyscallTable = resultTables.get(1);
        LamiResultTable perInterruptTable = resultTables.get(2);
        LamiResultTable perInterruptOverrideTable = resultTables.get(3);

        assertEquals("Per-process stuff", perProcessTable.getTableClass().getTableTitle());
        assertEquals("per-proc", perProcessTable.getTableClass().getTableClassName());

        assertEquals("Per-syscall stuff", perSyscallTable.getTableClass().getTableTitle());
        assertEquals("per-syscall", perSyscallTable.getTableClass().getTableClassName());

        assertEquals("Per-interrupt stuff", perInterruptTable.getTableClass().getTableTitle());
        assertEquals("per-irq", perInterruptTable.getTableClass().getTableClassName());

        assertEquals("Per-interrupt stuff [with overridden title]", perInterruptOverrideTable.getTableClass().getTableTitle());
        assertEquals("Extended per-irq", perInterruptOverrideTable.getTableClass().getTableClassName());

        assertEquals(1000, perProcessTable.getTimeRange().getStart());
        assertEquals(2000, perProcessTable.getTimeRange().getEnd());
        assertEquals(1000, perProcessTable.getTimeRange().getDuration());

        List<LamiTableEntry> syscallEntries = perSyscallTable.getEntries();

        assertFalse(syscallEntries.isEmpty());
        assertEquals(5, syscallEntries.size());

        LamiTableEntry readEntry = syscallEntries.get(0);
        LamiTimeRange readEntryTimeRange = readEntry.getCorrespondingTimeRange();

        assertNotNull(readEntryTimeRange);
        assertEquals(98233, readEntryTimeRange.getStart());
        assertEquals(1293828, readEntryTimeRange.getEnd());
        assertEquals(1195595, readEntryTimeRange.getDuration());

        /* Test raw values */
        LamiData value0 = readEntry.getValue(0);
        assertTrue(value0 instanceof LamiSystemCall);
        assertEquals("read", ((LamiSystemCall) value0).getValue());

        LamiData value1 = readEntry.getValue(1);
        assertTrue(value1 instanceof LamiDuration);
        assertEquals(2398123, ((LamiDuration) value1).getValue());

        LamiData value2 = readEntry.getValue(2);
        assertTrue(value2 instanceof LamiSize);
        assertEquals(8123982, ((LamiSize) value2).getValue());

        LamiData value3 = readEntry.getValue(3);
        assertTrue(value3 instanceof LamiBitrate);
        assertEquals(223232, ((LamiBitrate) value3).getValue());

        LamiData value4 = readEntry.getValue(4);
        assertTrue(value4 instanceof LamiTimeRange);
        assertEquals(98233, ((LamiTimeRange) value4).getStart());
        assertEquals(1293828, ((LamiTimeRange) value4).getEnd());


        /* Test with aspects */
        Map<String, LamiTableClass> tableModels = analysis.getTableModels();
        assertNotNull(tableModels);
        LamiTableClass perSyscallClass = tableModels.get("per-syscall");
        assertNotNull(perSyscallClass);
        List<LamiTableEntryAspect> aspects = perSyscallClass.getAspects();

        assertEquals("read()", aspects.get(0).resolveString(readEntry));
        assertEquals(2398123.0, checkNotNull(aspects.get(1).resolveDouble(readEntry)).doubleValue(), DELTA);
        assertEquals(8123982.0, checkNotNull(aspects.get(2).resolveDouble(readEntry)).doubleValue(), DELTA);
        assertEquals(223232.0, checkNotNull(aspects.get(3).resolveDouble(readEntry)).doubleValue(), DELTA);
        assertEquals(98233.0, checkNotNull(aspects.get(4).resolveDouble(readEntry)).doubleValue(), DELTA);
        assertEquals(1293828.0, checkNotNull(aspects.get(5).resolveDouble(readEntry)).doubleValue(), DELTA);
        assertEquals(1195595.0, checkNotNull(aspects.get(6).resolveDouble(readEntry)).doubleValue(), DELTA);
        assertNull(aspects.get(7).resolveString(readEntry));
    }

    /**
     * Test the error parsing of the results.
     *
     * @throws OnDemandAnalysisException when execute() fails.
     */
    @Test (expected = OnDemandAnalysisException.class)
    public void testResultsError() throws OnDemandAnalysisException {
        LamiAnalysisStub analysis = new LamiAnalysisStub("test-metadata.json", "test-error.json");

        analysis.execute(fTrace, null, "", new NullProgressMonitor());
    }

    /**
     * Test the command generation.
     */
    @Test
    public void testBaseCommand() {
        LamiAnalysisStub analysis = new LamiAnalysisStub("test-metadata.json", "test-error.json");

        ITmfTimestamp begin = TmfTimestamp.fromNanos(98233);
        ITmfTimestamp end = TmfTimestamp.fromNanos(1293828);

        TmfTimeRange timerange = new TmfTimeRange(begin, end);

        assertEquals("StubExecutable " + '\"' + TRACEPATH + '\"', analysis.getBaseCommandAsString(fTrace, null));
        assertEquals("StubExecutable --begin 98233 --end 1293828 " + '\"' + TRACEPATH + '\"', analysis.getBaseCommandAsString(fTrace, timerange));
    }
}
