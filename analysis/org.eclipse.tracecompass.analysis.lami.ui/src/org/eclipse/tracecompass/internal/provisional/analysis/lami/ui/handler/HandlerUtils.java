/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir, Philippe Proulx
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public final class HandlerUtils {

    private HandlerUtils() {
    }

    public static @Nullable Object getSelectedModelElement() {
        // Check if we are closing down
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        // Get the selection
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        final IWorkbenchPart part = page.getActivePart();
        if (part == null) {
            return null;
        }
        final ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return null;
        }
        final ISelection selection = selectionProvider.getSelection();

        /*
         * Make sure there is only one selection and that it is a on-demand
         * analysis element.
         */
        if (selection instanceof TreeSelection) {
            final TreeSelection sel = (TreeSelection) selection;
            // There should be only one item selected as per the plugin.xml
            return sel.getFirstElement();
        }

        return null;
    }

}
