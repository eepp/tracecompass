/*******************************************************************************
 * Copyright (c) 2015 EfficiOS inc, Jonathan Rajotte-Julien
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.viewers;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.tracecompass.common.core.format.DecimalUnitFormat;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect.LamiTableEntryAspect;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiChartModel;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiChartModel.ChartType;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiLabelFormat;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiResultTable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableEntry;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTimeStampFormat;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.signals.LamiSelectionUpdateSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.swtchart.IAxisTick;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;
import org.swtchart.Range;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterators;

/**
 * XY Scatter chart viewer for Lami views
 *
 * @author Jonathan Rajotte-Julien
 */
public class LamiScatterViewer extends LamiXYChartViewer {

    /*
     * Since it is possible to graph non numerical sortable values an internal
     * sorted list is necessary for graphing. Translation for index to the
     * actual table is provided.
     */
    List<@NonNull LamiTableEntry> fInternalEntryList;

    /**
     * Constructor
     *
     * @param parent
     *            parent
     * @param resultTable
     *            Result table populating this chart
     * @param graphModel
     *            Model of this chart
     */
    public LamiScatterViewer(Composite parent, LamiResultTable resultTable, LamiChartModel graphModel) {
        super(parent, resultTable, graphModel);
        if (getChartModel().getChartType() != ChartType.XY_SCATTER) {
            throw new IllegalStateException();
        }

        List<LamiTableEntryAspect> aspects = new ArrayList<>(getResultTable().getTableClass().getAspects());

        /* Get the aspect for the X axis */
        LamiTableEntryAspect xAxisAspect = getAspectFromName(aspects, getChartModel().getXAxisColumn());

        /* When x axis is non numerical sort via it's comparator */
        /* FIXME use the aspect comparator or whatever */
        if (!xAxisAspect.isNumerical()) {
            /* For now compare based on string representation */
            fInternalEntryList = new ArrayList<>(getResultTable().getEntries());
            Collections.sort(fInternalEntryList, new Comparator<LamiTableEntry>() {
                @Override
                public int compare(@NonNull LamiTableEntry o1, @NonNull LamiTableEntry o2) {
                    return checkNotNull(xAxisAspect.resolveString(o1)).compareToIgnoreCase(xAxisAspect.resolveString(o2));
                }

            });
        } else {
            fInternalEntryList = getResultTable().getEntries();
        }

        /* Create X series */
        double[] xSerie;
        HashBiMap<String, Integer> xMap = HashBiMap.create();
        boolean xIsLog = graphModel.xAxisIsLog();

        /* Create Y series */
        if (xAxisAspect.isNumerical()) {
            DoubleStream xSerieStream = fInternalEntryList.stream().mapToDouble(entry -> xAxisAspect.resolveDouble(entry));

            if (xIsLog) {
                /* Log axis does not support 0 values. Clamp them to 0.9 */
                xSerieStream = xSerieStream.map(elem -> (elem < 0.9) ? 0.9 : elem);
            }
            xSerie = xSerieStream.toArray();
        } else {
            /*
             * Create the categories map
             */
            for (LamiTableEntry entry : fInternalEntryList) {
                if (!xMap.containsKey(xAxisAspect.resolveString(entry))) {
                    /* Assign a number to the new category */
                    xMap.put(checkNotNull(xAxisAspect.resolveString(entry)), xMap.size());
                }
            }
            xSerie = fInternalEntryList.stream().mapToDouble(entry -> xMap.get(xAxisAspect.resolveString(entry))).toArray();
        }

        /*
         * Create Y series
         *
         * FIXME: handle when series does not have the same type Possible
         * Solution: simply prevent this a the dialog level on selection one or
         * the other type but not both For now simple throw an IllegalState
         * exception. FIXME: logScal should no be applied on non numerical value
         * Dynamic dialog should mitigate most of this problem.
         */
        boolean yIsLog = graphModel.yAxisIsLog();

        List<LamiTableEntryAspect> yAspects = getYAxisAspects();
        Boolean areYAspectsNumerical = false;
        HashBiMap<String, Integer> yMap = HashBiMap.create();

        /* Check all aspect are the same type */
        for (LamiTableEntryAspect aspect : yAspects) {
            if (aspect.isNumerical() == yAspects.get(0).isNumerical()) {
                areYAspectsNumerical = aspect.isNumerical();
            } else {
                throw new IllegalStateException();
            }
        }

        /*
         * When yAspects are non-numerical create a map for all values of all
         * series
         */
        /*
         * FIXME: How to handle multiple series that might order differently ...
         * who knows
         */
        if (!areYAspectsNumerical) {
            TreeSet<String> set = new TreeSet<>();
            for (LamiTableEntryAspect aspect : yAspects) {
                for (LamiTableEntry entry : fInternalEntryList) {
                    set.add(checkNotNull(aspect.resolveString(entry)));
                }
            }
            /* Ordered label mapping to double */
            for (String string : set) {
                yMap.put(string, yMap.size());
            }

        }

        /* Plot the series */
        for (LamiTableEntryAspect aspect : getYAxisAspects()) {
            String name = aspect.getName();
            double[] ySeries;

            if (aspect.isNumerical()) {
                if (yIsLog) {
                    ySeries = fInternalEntryList.stream().mapToDouble(entry -> aspect.resolveDouble(entry))
                            /*
                             * Log axis does not support 0 values. Clamp them to
                             * 0.9
                             */
                            .map(elem -> (elem < 0.9) ? 0.9 : elem).toArray();
                } else {
                    ySeries = fInternalEntryList.stream().mapToDouble(entry -> aspect.resolveDouble(entry)).toArray();
                }
            } else {
                /* Map string to value */
                if (yMap.isEmpty()) {
                    /* Well something got very wrong during map creation */
                    throw new IllegalStateException();
                }
                ySeries = fInternalEntryList.stream().mapToDouble(entry -> yMap.get(aspect.resolveString(entry))).toArray();
            }

            ILineSeries scatterSeries = (ILineSeries) getChart().getSeriesSet().createSeries(SeriesType.LINE, name);
            scatterSeries.setLineStyle(LineStyle.NONE);

            scatterSeries.setXSeries(xSerie);
            scatterSeries.setYSeries(ySeries);
        }

        /* Modify x axis related chart styling */
        IAxisTick xTick = getChart().getAxisSet().getXAxis(0).getTick();
        if (xAxisAspect.isNumerical()) {
            if (xAxisAspect.isTimeStamp()) {
                /* Only apply a custom format on Timestamp */
                xTick.setFormat(new LamiTimeStampFormat());
            }
        } else {
            xTick.setFormat(new LamiLabelFormat(xMap));
            int stepSizePixel = getChart().getPlotArea().getSize().x / ((xSerie.length != 0) ? xSerie.length - 1 : 1);
            /*
             * This step is a limitation on swtchart side regarding minimal grid
             * step hint size. When the step size are smaller it get defined as
             * the "default" value for the axis instead of the smallest one.
             */
            if (IAxisTick.MIN_GRID_STEP_HINT > stepSizePixel) {
                stepSizePixel = (int) IAxisTick.MIN_GRID_STEP_HINT;
            }
            xTick.setTickMarkStepHint(stepSizePixel);

            /* Remove vertical grid line */
            getChart().getAxisSet().getXAxis(0).getGrid().setStyle(LineStyle.NONE);
        }

        /* Modify Y axis related chart styling */
        IAxisTick yTick = getChart().getAxisSet().getYAxis(0).getTick();
        if (areYAspectsNumerical) {
            /* Set the formatter on the Y axis */
            yTick.setFormat(new DecimalUnitFormat());
        } else {
            yTick.setFormat(new LamiLabelFormat(yMap));
            /*
             * Use xSerie length since it is exposed and is equal to the size of
             * any y serie
             */
            int stepSizePixel = getChart().getPlotArea().getSize().y / ((xSerie.length != 0) ? xSerie.length - 1 : 1);
            /*
             * This step is a limitation on swtchart side regarding minimal grid
             * step hint size. When the step size are smaller it get defined as
             * the "default" value for the axis instead of the smallest one.
             */
            if (IAxisTick.MIN_GRID_STEP_HINT > stepSizePixel) {
                stepSizePixel = (int) IAxisTick.MIN_GRID_STEP_HINT;
            }
            yTick.setTickMarkStepHint(stepSizePixel);

            /* Remove horizontal grid line */
            getChart().getAxisSet().getYAxis(0).getGrid().setStyle(LineStyle.NONE);
        }

        setLineSeriesColor();

        getChart().getAxisSet().adjustRange();

        /* Put log scale if necessary */

        Stream.of(getChart().getAxisSet().getXAxes()).forEach(axis -> axis.enableLogScale(xIsLog));

        if (xIsLog && xAxisAspect.isNumerical() && !xAxisAspect.isTimeStamp()) {
            Stream.of(getChart().getAxisSet().getXAxes()).forEach(axis -> axis.enableLogScale(xIsLog));
            /*
             * In case of a log Y axis, bump the X axis to hide the "fake" 0.9
             * values.
             */
            Range yRange = getChart().getAxisSet().getXAxis(0).getRange();
            getChart().getAxisSet().getXAxis(0).setRange(new Range(0.9, yRange.upper));
        }

        if (yIsLog && areYAspectsNumerical) {
            /* Set the axis as logscale */
            Stream.of(getChart().getAxisSet().getYAxes()).forEach(axis -> axis.enableLogScale(yIsLog));

            /*
             * In case of a log Y axis, bump the X axis to hide the "fake" 0.9
             * values.
             */
            Range yRange = getChart().getAxisSet().getYAxis(0).getRange();
            getChart().getAxisSet().getYAxis(0).setRange(new Range(0.9, yRange.upper));
        }

        getChart().getPlotArea().addListener(SWT.MouseDown, new LamiScatterMouseDownListener());
        getChart().getPlotArea().addPaintListener(new LamiScatterPainterListener());

        /* Register to receive LamiSelectionUpdateSignal */
        TmfSignalManager.register(this);
    }

    /**
     * Set the chart series colors.
     */
    private void setLineSeriesColor() {
        Iterator<Color> colorsIt;

        colorsIt = Iterators.cycle(COLORS);

        for (ISeries series : getChart().getSeriesSet().getSeries()) {
            ((ILineSeries) series).setSymbolColor((colorsIt.next()));
            /*
             * Generate initial array of Color to enable per point color change
             * on selection in the future
             */
            ArrayList<Color> colors = new ArrayList<>();
            for (int i = 0; i < series.getXSeries().length; i++) {
                Color color = ((ILineSeries) series).getSymbolColor();
                colors.add(checkNotNull(color));
            }
            ((ILineSeries) series).setSymbolColors(colors.toArray(new Color[colors.size()]));
        }

    }

    private final class LamiScatterMouseDownListener implements Listener {
        @Override
        public void handleEvent(@Nullable Event event) {
            if (event == null) {
                return;
            }
            int xMouseLocation = event.x;
            int yMouseLocation = event.y;

            boolean unselectMode = false;

            ISeries[] series = getChart().getSeriesSet().getSeries();
            Set<Integer> selections = getSelection();

            /* Check for ctrl on click */
            if ((event.stateMask & SWT.CTRL) != 0 ) {
                if ((event.stateMask & SWT.SHIFT) != 0) {
                    unselectMode = true;
                } else {
                    /* Reset selection */
                    selections = getSelection();
                }
            } else {
                unsetSelection();
                selections = new HashSet<>();
            }

            for (ISeries oneSeries : series) {
                ILineSeries lineSerie = (ILineSeries) oneSeries;

                int closest = -1;
                double closestDistance = -1;
                for (int i = 0; i < lineSerie.getXSeries().length; i++) {
                    org.eclipse.swt.graphics.Point dataPoint = lineSerie.getPixelCoordinates(i);

                    /*
                     * Find the distance between the data point and the mouse
                     * location and compare it to the symbol size so when a user
                     * click on a symbol it select it.
                     */

                    double distance = Math.hypot(dataPoint.x - xMouseLocation, dataPoint.y - yMouseLocation);
                    if (distance < lineSerie.getSymbolSize()) {
                        if (closestDistance == -1 || distance < closestDistance) {
                            closest = i;
                            closestDistance = distance;
                        }
                    }
                }
                if (closest != -1) {
                    /* Translate to global index */
                    LamiTableEntry entry = fInternalEntryList.get(closest);
                    int index = getResultTable().getEntries().indexOf(entry);
                    if (unselectMode) {
                        selections.remove(index);
                    } else {
                        selections.add(index);
                    }
                    /* Do no iterate since we already found a match */
                    break;
                }
            }
            setSelection(selections);
            /* Signal all Lami viewers & views of the selection */
            LamiSelectionUpdateSignal signal = new LamiSelectionUpdateSignal(this,
                    selections, checkNotNull(getResultTable().hashCode()));
            TmfSignalManager.dispatchSignal(signal);
            refresh();
        }
    }


    private final class LamiScatterPainterListener implements PaintListener {
        @Override
        public void paintControl(@Nullable PaintEvent e) {
            if (e == null || !isSelected()) {
                return;
            }
            GC gc = e.gc;
            gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA));

            gc.setLineWidth(1);
            gc.setLineStyle(SWT.LINE_SOLID);
            for (ISeries series : getChart().getSeriesSet().getSeries()) {
                for (int index : getInternalSelections()) {
                    /* Generate a cross for each selected dot */
                    org.eclipse.swt.graphics.Point point = series.getPixelCoordinates(index);
                    /* Vertical line */
                    gc.drawLine(point.x, 0 , point.x, getChart().getPlotArea().getSize().y);
                    /* Horizontal line */
                    gc.drawLine(0, point.y, getChart().getPlotArea().getSize().x, point.y);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.viewers.LamiXYChartViewer#refreshDisplayLabels()
     */
    @Override
    protected void refreshDisplayLabels() {
    }

    @Override
    public void dispose() {
        TmfSignalManager.deregister(this);
        super.dispose();
    }


    protected Set<Integer> getInternalSelections() {
        /* Translate to internal table location */
        Set<Integer> indexes = super.getSelection();
        Set<Integer> internalIndexes = indexes.stream()
                .mapToInt(index -> fInternalEntryList.indexOf((getResultTable().getEntries().get(index))))
                .boxed()
                .collect(Collectors.toSet());
        return internalIndexes;
    }

}
