/*******************************************************************************
 * Copyright (c) 2015 EfficiOS inc, Jonathan Rajotte-Julien
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.BiMap;

/**
 *
 * Format label based on a given Map<String, Integer>
 *
 * @author Jonathan Rajotte-Julien
 *
 */
public class LamiLabelFormat extends Format {

    private final BiMap<String, Integer> fMap;

    /**
     * Constructor
     *
     * @param The map
     */
    public LamiLabelFormat(BiMap<String, Integer> map) {
        super();
        fMap = map;
    }

    /**
     *
     */
    private static final long serialVersionUID = 4939553034329681316L;

    @Override
    public @Nullable StringBuffer format(@Nullable Object obj, @Nullable StringBuffer toAppendTo, @Nullable FieldPosition pos) {
       if (toAppendTo != null && obj != null) {
           /* Return string buffer with a space in it since SWT do not like to draw empty string */
            if ((((Double)obj) % 1 != 0) || !fMap.containsValue(((Double)obj).intValue())) {
                return new StringBuffer(" ");
            }

            for (java.util.Map.Entry<@NonNull String, @NonNull Integer> entry : fMap.entrySet()) {
                if (entry.getValue() == ((Double)obj).intValue()) {
                    if (entry.getKey().isEmpty()) {
                        return new StringBuffer("?");
                    }
                    return toAppendTo.append(entry.getKey());
                }
            }
        }
        return new StringBuffer(" ");
    }

    @Override
    public @Nullable Object parseObject(@Nullable String source, @Nullable ParsePosition pos) {
        return fMap.get(source);
    }

}
