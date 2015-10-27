/*******************************************************************************
 * Copyright (c) 2015 EfficiOS inc, Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.realtime.ui.periods.view;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.realtime.core.periods.PeriodAnalysisModule;
import org.eclipse.tracecompass.analysis.realtime.core.periods.PeriodAnalysisModuleConfig;
import org.eclipse.tracecompass.internal.analysis.realtime.ui.Activator;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

public class PeriodTableView extends TmfView {

    /** The view's ID */
    public static final String ID = "org.eclipse.tracecompass.analysis.realtime.view.periods"; //$NON-NLS-1$

    private @Nullable PeriodTableViewer fPeriodViewer;

    public PeriodTableView() {
        super(ID);
    }

    @Override
    public void createPartControl(@Nullable Composite uncheckedParent) {
        Composite parent = checkNotNull(uncheckedParent);
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace == null) {
            return;
        }
        final PeriodAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, PeriodAnalysisModule.class, PeriodAnalysisModule.ANALYSIS_ID);
        if (module == null) {
            return;
        }
        fPeriodViewer = new PeriodTableViewer(parent);

        /* Add toolbar buttons */
        IToolBarManager toolbarMgr = getViewSite().getActionBars().getToolBarManager();

        IAction runAnalysisAction = new Action() {
            @Override
            public void run() {
                InputDialog dialog = new InputDialog(parent.getShell(), "Period definitions",
                        "Enter the event name that determines the start/end of a period.",
                        "", null);
                if (dialog.open() != Window.OK) {
                    /* User clicked Cancel, do nothing */
                    return;
                }
                String eventName = dialog.getValue();

                PeriodAnalysisModuleConfig config = new PeriodAnalysisModuleConfig(
                        event -> event.getName().equals(eventName),
                        event -> event.getName().equals(eventName));
                module.setConfig(config);

                checkNotNull(fPeriodViewer).setData(module);
            }
        };
        runAnalysisAction.setText("Setup Period Definition");
        runAnalysisAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath("icons/shift_r_edit.gif"));

        toolbarMgr.add(runAnalysisAction);
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fPeriodViewer != null) {
            fPeriodViewer.dispose();
        }
    }
}
