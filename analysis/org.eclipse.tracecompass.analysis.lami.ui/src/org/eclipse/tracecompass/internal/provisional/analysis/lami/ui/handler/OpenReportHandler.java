/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.handler;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiAnalysisReport;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.views.LamiReportViewFactory;
import org.eclipse.tracecompass.tmf.core.analysis.ondemand.IOnDemandAnalysisReport;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfReportElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * The command handler for the "Open Report" menu option for Report project
 * model elements.
 *
 * Double-clicking should also call this handler.
 *
 * @author Alexandre Montplaisir
 */
public class OpenReportHandler extends AbstractHandler {

    /** Report elements to open */
    private @Nullable List<TmfReportElement> fReportElements;

    @Override
    public boolean isEnabled() {
        // Check if we are closing down
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return false;
        }

        // Get the selection
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        final IWorkbenchPart part = page.getActivePart();
        if (part == null) {
            return false;
        }
        final ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return false;
        }
        final ISelection selection = selectionProvider.getSelection();

        /*
         * Get the corresponding project elements from the selection.
         */
        fReportElements = null;
        if (selection instanceof TreeSelection) {
            final TreeSelection sel = (TreeSelection) selection;
            /*
             * There can be more than one element selected, but they should all
             * be report elements, as per the plugin.xml.
             */
            List<?> elements = sel.toList();
            List<TmfReportElement> reportElements = elements.stream()
                .filter(elem -> elem instanceof TmfReportElement)
                .map(elem -> (TmfReportElement) elem)
                .collect(Collectors.toList());
            fReportElements = reportElements;
        }

        return (fReportElements != null);
    }

    @Override
    public @Nullable Object execute(@Nullable ExecutionEvent event) throws ExecutionException {
        // Check if we are closing down
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        // Check that the elements are valid
        List<TmfReportElement> reportElems = fReportElements;
        if (reportElems == null) {
            return null;
        }

        for (TmfReportElement reportElem : reportElems) {
            IOnDemandAnalysisReport report = reportElem.getReport();
            if (!(report instanceof LamiAnalysisReport)) {
                /* This handler deals with LAMI reports only */
                continue;
            }
            LamiAnalysisReport lamiReport = (LamiAnalysisReport) report;

            Display.getDefault().syncExec(() -> {
                try {
                    LamiReportViewFactory.createNewViews(lamiReport);
                } catch (PartInitException e) {
                }
            });
        }

        return null;
    }
}