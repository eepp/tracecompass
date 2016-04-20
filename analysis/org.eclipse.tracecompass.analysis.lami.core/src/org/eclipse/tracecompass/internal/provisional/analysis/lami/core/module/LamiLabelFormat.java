/**
 *
 */
package org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.HashBiMap;


/**
 * @author Jonathan Rajotte-Julien
 *
 */
public class LamiLabelFormat extends Format {

    private final HashBiMap<String, Integer> fMap;

    /**
     * @param map
     */
    public LamiLabelFormat(HashBiMap<String, Integer> map) {
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
            if ((((Double)obj) % 1 != 0) || !fMap.containsValue(((Double)obj).intValue())) {
                return new StringBuffer(" ");
            }

            for (java.util.Map.Entry<@NonNull String, @NonNull Integer> entry : fMap.entrySet()) {
                if (entry.getValue() == ((Double)obj).intValue()) {
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
