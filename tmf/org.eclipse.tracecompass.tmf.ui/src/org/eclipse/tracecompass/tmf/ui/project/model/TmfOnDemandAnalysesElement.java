/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.project.model;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tracecompass.tmf.core.analysis.ondemand.IOnDemandAnalysis;
import org.eclipse.tracecompass.tmf.core.analysis.ondemand.OnDemandAnalysisManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Project model element for the "On-demand" / "External Analyses" element,
 * which goes under individual trace and experiment elements.
 *
 * It will list the available implementations of IOndemandAnalysis that can be
 * executed on this particular trace.
 *
 * @author Alexandre Montplaisir
 * @since 2.0
 */
public class TmfOnDemandAnalysesElement extends TmfProjectModelElement {

    /**
     * Element of the resource path
     */
    public static final String PATH_ELEMENT = ".ondemand-analyses"; //$NON-NLS-1$

    private static final String ELEMENT_NAME = Messages.TmfOnDemandAnalysesElement_Name;

    private boolean fInitialized = false;

    /**
     * Constructor
     *
     * @param resource
     *            The resource to be associated with this element
     * @param parent
     *            The parent element
     */
    protected TmfOnDemandAnalysesElement(IResource resource, TmfCommonProjectElement parent) {
        super(ELEMENT_NAME, resource, parent);
    }

    @Override
    public TmfCommonProjectElement getParent() {
        /* Type enforced at constructor */
        return (TmfCommonProjectElement) super.getParent();
    }

    @Override
    public Image getIcon() {
        return TmfProjectModelIcons.ONDEMAND_ANALYSES_ICON;
    }

    @Override
    protected void refreshChildren() {
        ITmfTrace trace = getParent().getTrace();
        if (trace == null) {
            /* Trace is not yet initialized */
            return;
        }

        /*
         * The criteria for which analyses can apply to a trace should never
         * change, so initialization only needs to be done once.
         */
        if (!fInitialized) {
            Set<IOnDemandAnalysis> analyses =
                    OnDemandAnalysisManager.getInstance().getOndemandAnalyses(trace);

            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath nodePath = this.getResource().getFullPath();

            analyses.forEach(analysis -> {
                IFolder analysisRes = checkNotNull(root.getFolder(nodePath.append(analysis.getName())));
                TmfOnDemandAnalysisElement elem = new TmfOnDemandAnalysisElement(
                        analysis.getName(), analysisRes, this, analysis);
                addChild(elem);
            });
        }

        /* Refresh all children */
        getChildren().forEach(child -> ((TmfProjectModelElement) child).refreshChildren());
    }

}
