/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Jonathan Rajotte-Julien
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.views;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Button;

/**
 * Basic representation of a check box option for dialog.
 *
 * @author Jonathan Rajotte-Julien
 */
class LamiAxisCheckBoxOption {
    private final String fName;
    private final boolean fDefaultValue;
    private @Nullable Button fButton;
    private boolean fValue;

    /**
     * @param name
     *          The name of the check box. The actual string shown to user.
     * @param defaultValue
     *          The default value of the check box.
     */
    public LamiAxisCheckBoxOption(String name, boolean defaultValue) {
        fName = name;
        this.fDefaultValue = defaultValue;
        this.fValue = defaultValue;
        fButton = null;
    }

    public String getName() {
        return fName;
    }

    public boolean getDefaultValue() {
        return fDefaultValue;
    }

    public void setButton(Button button) {
        fButton = button;
    }

    /**
     * @return the value
     */
    public boolean getValue() {
        return fValue;
    }

    public void updateValue() {
        if (fButton != null) {
            fValue = fButton.getSelection();
        }
    }
}