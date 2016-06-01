/*******************************************************************************
 * Copyright (c) 2015, 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A LAMI number is a quantity of something with optional limits of
 * uncertainty or confidence.
 * <p>
 * The difference between a number and any other data object also
 * having an integer/real number property is that, since it represents
 * a quantity, a number always has an associated <em>unit</em>.
 *
 * @author Philippe Proulx
 *
 */
class LamiNumber extends LamiData {

    private final @Nullable Number fLowValue;
    private final @Nullable Number fValue;
    private final @Nullable Number fHighValue;

    public LamiNumber(Number value) {
        fValue = value;
        fLowValue = null;
        fHighValue = null;
    }

    public LamiNumber(@Nullable Number lowValue, @Nullable Number value, @Nullable Number highValue) {
        fLowValue = lowValue;
        fValue = value;
        fHighValue = highValue;
    }

    public @Nullable Number getLowValue() {
        return fLowValue;
    }

    public @Nullable Number getValue() {
        return fValue;
    }

    public @Nullable Number getHighValue() {
        return fHighValue;
    }

    @Override
    public @Nullable String toString() {
        // TODO: The string should probably include the low and
        //       high limits here.
        if (fValue != null) {
            return fValue.toString();
        }

        return null;
    }

}
