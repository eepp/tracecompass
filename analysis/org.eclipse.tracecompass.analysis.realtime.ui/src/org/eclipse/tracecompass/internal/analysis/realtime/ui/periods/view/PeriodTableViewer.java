/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   France Lapointe Nguyen - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.realtime.ui.periods.view;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.analysis.realtime.core.periods.PeriodAnalysisModule;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfNanoTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.viewers.table.TmfSimpleTableViewer;

import com.google.common.collect.Iterables;

/**
 * Displays the latency analysis data in a column table
 *
 * @author France Lapointe Nguyen
 */
public class PeriodTableViewer extends TmfSimpleTableViewer {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * Current latency analysis module
     */
    private @Nullable PeriodAnalysisModule fAnalysisModule;

    // ------------------------------------------------------------------------
    // Inner class definitions
    // ------------------------------------------------------------------------

    /**
     * Abstract class for the column label provider for the latency analysis
     * table viewer
     */
    private abstract class PeriodTableColumnLabelProvider extends ColumnLabelProvider {

        @Override
        public String getText(@Nullable Object input) {
            if (!(input instanceof ISegment)) {
                /* Doubles as a null check */
                return ""; //$NON-NLS-1$
            }
            return getTextForPeriod((ISegment) input);
        }

        public abstract String getTextForPeriod(ISegment input);
    }

    /**
     * Listener to select a range in other viewers when a cell of the latency
     * table view is selected
     */
    private class PeriodTableSelectionListener extends SelectionAdapter {
        @Override
        public void widgetSelected(@Nullable SelectionEvent e) {
            ISegment selectedSegment = ((ISegment) NonNullUtils.checkNotNull(e).item.getData());
            ITmfTimestamp start = new TmfNanoTimestamp(selectedSegment.getStart());
            ITmfTimestamp end = new TmfNanoTimestamp(selectedSegment.getEnd());
            TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(PeriodTableViewer.this, start, end));
        }
    }

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param tableViewer
     *            Table viewer of the view
     * @param followTimeSelection
     *            Should this viewer handle
     *            {@link TmfSelectionRangeUpdatedSignal} signals sent by other
     *            views
     */
    public PeriodTableViewer(Composite parent) {
        super(new TableViewer(parent, SWT.FULL_SELECTION | SWT.VIRTUAL));

        // Sort order of the content provider is by start time by default
        getTableViewer().setContentProvider(new PeriodTableContentProvider());

        createColumns();
        getTableViewer().getTable().addSelectionListener(new PeriodTableSelectionListener());
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Create columns for start time, end time and duration
     */
    private void createColumns() {
        createColumn("Period Start", new PeriodTableColumnLabelProvider() {
            @Override
            public String getTextForPeriod(ISegment input) {
                return NonNullUtils.nullToEmptyString(TmfTimestampFormat.getDefaulTimeFormat().format(input.getStart()));
            }
        }, SegmentComparators.INTERVAL_START_COMPARATOR);

        createColumn("Period End", new PeriodTableColumnLabelProvider() {
            @Override
            public String getTextForPeriod(ISegment input) {
                return NonNullUtils.nullToEmptyString(TmfTimestampFormat.getDefaulTimeFormat().format(input.getEnd()));
            }
        }, SegmentComparators.INTERVAL_END_COMPARATOR);

        createColumn("Period Duration", new PeriodTableColumnLabelProvider() {
            @Override
            public String getTextForPeriod(ISegment input) {
                return NonNullUtils.nullToEmptyString(Long.toString(input.getLength()));
            }
        }, SegmentComparators.INTERVAL_LENGTH_COMPARATOR);
    }

     /**
      * Set the data into the viewer. Will update model is analysis is completed
      * or run analysis if not completed
      *
      * @param analysis
      *            Latency analysis module
      */
     public void setData(final PeriodAnalysisModule analysis) {
         analysis.schedule();
         analysis.waitForCompletion();
         fAnalysisModule = analysis;
         ISegmentStore<ISegment> results = analysis.getSegmentStore();

        final TableViewer tableViewer = getTableViewer();
        Display.getDefault().asyncExec(() -> {
            if (tableViewer.getTable().isDisposed()) {
                return;
            }
            // Go to the top of the table
            tableViewer.getTable().setTopIndex(0);
            // Reset selected row
            tableViewer.setSelection(StructuredSelection.EMPTY);
            if (results == null) {
                tableViewer.setInput(null);
                tableViewer.setItemCount(0);
                return;
            }
            tableViewer.setInput(results);
            PeriodTableContentProvider latencyContentProvider = (PeriodTableContentProvider) getTableViewer().getContentProvider();
            tableViewer.setItemCount(latencyContentProvider.getSegmentCount());
        });
    }

     @Override
     protected void appendToTablePopupMenu(IMenuManager manager, IStructuredSelection sel) {
         final ISegment segment = (ISegment) sel.getFirstElement();

         IAction gotoStartTime = new Action("Go to Start Event") {
             @Override
             public void run() {
                 broadcast(new TmfSelectionRangeUpdatedSignal(PeriodTableViewer.this, new TmfNanoTimestamp(segment.getStart())));
             }
         };

         IAction gotoEndTime = new Action("Go to End Event") {
             @Override
             public void run() {
                 broadcast(new TmfSelectionRangeUpdatedSignal(PeriodTableViewer.this, new TmfNanoTimestamp(segment.getEnd())));
             }
         };

        manager.add(gotoStartTime);
        manager.add(gotoEndTime);
     }

     private void clearTable() {
         if (!getTableViewer().getTable().isDisposed()) {
             getTableViewer().setInput(null);
             refresh();
         }
     }

    // ------------------------------------------------------------------------
    // Signal handlers
    // ------------------------------------------------------------------------

    /**
     * Trace selected handler
     *
     * @param signal
     *            Different opened trace (on which latency analysis as already
     *            been performed) has been selected
     */
    @TmfSignalHandler
    public void traceSelected(TmfTraceSelectedSignal signal) {
        clearTable();
    }

    /**
     * Trace opened handler
     *
     * @param signal
     *            New trace (on which latency analysis has not been performed)
     *            is opened
     */
    @TmfSignalHandler
     public void traceOpened(TmfTraceOpenedSignal signal) {
        clearTable();
     }

     /**
      * Trace closed handler
      *
      * @param signal
     *            Last opened trace was closed
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        // Check if there is no more opened trace
        if (TmfTraceManager.getInstance().getActiveTrace() == null) {
            clearTable();
        }
    }

    /**
     * Signal handler for handling of the selected range signal.
     *
     * @param signal
     *            The TmfSelectionRangeUpdatedSignal
     */
    @TmfSignalHandler
    public void selectionRangeUpdated(TmfSelectionRangeUpdatedSignal signal) {
        PeriodAnalysisModule module = fAnalysisModule;
        if (signal.getSource() == this ||
                TmfTraceManager.getInstance().getActiveTrace() == null ||
                module == null) {
            return;
        }
        ISegmentStore<ISegment> dataset = module.getSegmentStore();
        if (dataset == null) {
            return;
        }

        /*
         * Update selection to the closest period starting before the timestamp
         */
        long time = signal.getBeginTime().getValue();
        Iterable<ISegment> segments = dataset.getIntersectingElements(time);
        /* Give this particular store, there should be only 1 segment */
        ISegment targetSegment = Iterables.<@Nullable ISegment> getFirst(segments, null);
        if (targetSegment == null) {
            return;
        }
        /*
         * Get the position of that element in the database
         *
         * FIXME The segment store should provide this!
         */
        int pos = 0;
        for (ISegment segment : dataset) {
            if (segment == targetSegment) {
                break;
            }
            pos++;
        }

        getTableViewer().getTable().setSelection(pos);
    }
}
