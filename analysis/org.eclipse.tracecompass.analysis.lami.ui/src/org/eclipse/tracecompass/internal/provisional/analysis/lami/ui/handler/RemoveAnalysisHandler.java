/*******************************************************************************
 * Copyright (c) 2015, 2016 EfficiOS Inc., Philippe Proulx
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.handler;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.LamiConfigUtils;
import org.eclipse.tracecompass.tmf.core.analysis.ondemand.OnDemandAnalysisManager;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfOnDemandAnalysisElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfUserDefinedOnDemandAnalysisElement;

/**
 * The command handler for the "Remove External Analysis" menu option.
 *
 * @author Philippe Proulx
 */
public class RemoveAnalysisHandler extends AbstractHandler {

    @Override
    public @Nullable Object execute(@Nullable ExecutionEvent event) throws ExecutionException {
        final Object elem = HandlerUtils.getSelectedModelElement();

        if (elem == null) {
            return null;
        }

        if (!(elem instanceof TmfUserDefinedOnDemandAnalysisElement)) {
            return null;
        }

        final TmfOnDemandAnalysisElement analysisElem = (TmfOnDemandAnalysisElement) elem;

        // Unregister from the manager
        OnDemandAnalysisManager.getInstance().unregisterAnalysis(analysisElem.getAnalysis());

        // Refresh the project explorer
        analysisElem.getParent().refresh();

        // Remove the corresponding configuration file
        try {
            LamiConfigUtils.removeConfigFile(analysisElem.getAnalysis().getName());
        } catch (IOException e) {
            // Ignore this: not the end of the world
        }

        return null;
    }

}
