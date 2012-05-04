/*******************************************************************************
 * Copyright (c) 2009, 2010 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng.core.tests.trace;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.TestCase;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.linuxtools.internal.lttng.core.event.LttngEvent;
import org.eclipse.linuxtools.internal.lttng.core.trace.LTTngTrace;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.experiment.TmfExperiment;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.osgi.framework.FrameworkUtil;

/**
 * <b><u>TmfExperimentTest</u></b>
 * <p>
 * TODO: Implement me. Please.
 */
@SuppressWarnings("nls")
public class LTTngExperimentTest extends TestCase {

    private static final String DIRECTORY   = "traceset";
    private static final String TEST_STREAM = "trace-15316events_nolost_newformat";
    private static final String EXPERIMENT  = "MyExperiment";
    private static int          NB_EVENTS   = 15316;

    // Note: Start/end times are for the LTTng *trace*, not the actual events
    private static final TmfTimestamp  fStartTime = new TmfTimestamp(13589759412128L, (byte) -9);
    private static final TmfTimestamp  fEndTime   = new TmfTimestamp(13589907059242L, (byte) -9);

    private static ITmfTrace<LttngEvent>[] fTraces;
    private static TmfExperiment<LttngEvent> fExperiment;

    // ------------------------------------------------------------------------
    // Housekeeping
    // ------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private synchronized static ITmfTrace<LttngEvent>[] setupTrace(final String path) {
        if (fTraces == null) {
            fTraces = new ITmfTrace[1];
            try {
                final URL location = FileLocator.find(FrameworkUtil.getBundle(LTTngExperimentTest.class), new Path(path), null);
                final File testfile = new File(FileLocator.toFileURL(location).toURI());
                final LTTngTrace trace = new LTTngTrace(null, testfile.getPath(), false);
                fTraces[0] = trace;
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return fTraces;
    }

    private synchronized static void setupExperiment() {
        if (fExperiment == null)
            fExperiment = new TmfExperiment<LttngEvent>(LttngEvent.class, EXPERIMENT, fTraces, TmfTimestamp.ZERO, 1000, true);
    }

    public LTTngExperimentTest(final String name) throws Exception {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setupTrace(DIRECTORY + File.separator + TEST_STREAM);
        setupExperiment();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public void testBasicTmfExperimentConstructor() {

        assertEquals("GetId", EXPERIMENT, fExperiment.getName());
        assertEquals("GetNbEvents", NB_EVENTS, fExperiment.getNbEvents());

        final long nbTraceEvents = fExperiment.getTraces()[0].getNbEvents();
        assertEquals("GetNbEvents", NB_EVENTS, nbTraceEvents);

        final TmfTimeRange timeRange = fExperiment.getTimeRange();
        assertTrue("getStartTime", fStartTime.equals(timeRange.getStartTime()));
        assertTrue("getEndTime",   fEndTime.equals(timeRange.getEndTime()));
    }

}