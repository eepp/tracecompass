/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Jonathan Rajotte-Julien
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.views;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

/**
 * Custom list dialog for axis selection.
 *
 * Custom check box option can be added when necessary.
 *
 * @author Jonathan Rajotte-Julien
 */
public class LamiAxisListDialog extends ListDialog {

    private List<LamiAxisCheckBoxOption> fCheckBoxOptions;

    /**
     * Custom list dialog for axis selection.
     *
     * Custom check box option can be added when necessary.
     *
     * @param parent
     *            The shell parent
     */
    public LamiAxisListDialog(Shell parent) {
        super(checkNotNull(parent));
        fCheckBoxOptions = new ArrayList<>();
    }

    @Override
    protected @Nullable Control createDialogArea(@Nullable Composite container) {
        Composite parent = (Composite) super.createDialogArea(container);
        if (parent != null) {
            if (fCheckBoxOptions.size() > 0) {
                Label label = new Label(parent, SWT.NONE);
                label.setText(Messages.LamiSelectionDialog_Options);
                label.setFont(parent.getFont());

                Label separator1 = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
                separator1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                for (LamiAxisCheckBoxOption checkBox : fCheckBoxOptions) {
                    Button button = new Button(parent, SWT.CHECK);
                    button.setSelection(checkBox.getDefaultValue());
                    button.setText(checkBox.getName());
                    checkBox.setButton(button);
                }
            }
        }

        return parent;
    }

    @Override
    protected void okPressed() {
        for (LamiAxisCheckBoxOption checkBox : fCheckBoxOptions) {
            checkBox.updateValue();
        }
        super.okPressed();
    }

    /**
     * @param name
     *            The name of the option. The actual text shown to the user.
     * @param defaultValue
     *            The default state of the check box option.
     * @return The index of the option value in the result table.
     */
    public int addCheckBoxOption(String name, boolean defaultValue) {
        LamiAxisCheckBoxOption checkbox = new LamiAxisCheckBoxOption(name, defaultValue);
        fCheckBoxOptions.add(checkbox);
        return fCheckBoxOptions.size() - 1;
    }

    /**
     * @return The final values of all check box.
     */
    public boolean[] getCheckBoxOptionValues() {
        boolean[] selections = new boolean[fCheckBoxOptions.size()];
        IntStream.range(0, selections.length).forEach(i -> selections[i] = fCheckBoxOptions.get(i).getValue());
        return selections;
    }

}
