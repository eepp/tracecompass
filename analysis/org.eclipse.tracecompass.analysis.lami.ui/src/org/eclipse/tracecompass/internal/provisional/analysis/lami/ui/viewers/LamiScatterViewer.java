/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Jonathan Rajotte-Julien
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.viewers;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect.LamiTableEntryAspect;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiChartModel;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiChartModel.ChartType;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiLabelFormat;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiResultTable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableEntry;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.ui.signals.LamiSelectionUpdateSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.swtchart.IAxisTick;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterators;

/**
 * XY Scatter chart viewer for Lami views
 *
 * @author Jonathan Rajotte-Julien
 */
public class LamiScatterViewer extends LamiXYChartViewer {

    private Map<ISeries,List<Integer>> fIndexMapping;

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

        /* Inspect X series */
        fIndexMapping = new HashMap<>();

        List<LamiTableEntryAspect> xAxisAspects = getXAxisAspects();
        if (xAxisAspects.stream().distinct().count() == 1) {
            LamiTableEntryAspect singleXAspect = xAxisAspects.get(0);
            xAxisAspects.clear();
            xAxisAspects.add(singleXAspect);
        }

        BiMap<@Nullable String, Integer> xMap = checkNotNull(HashBiMap.create());
        boolean xIsLog = graphModel.xAxisIsLog();

        boolean areXAspectsContinuous = areAspectsContinuous(xAxisAspects);
        boolean areXAspectsTimeStamp = areAspectsTimeStamp(xAxisAspects);

        /* Check all aspect are the same type */
        for (LamiTableEntryAspect aspect : xAxisAspects) {
            if (aspect.isContinuous() != areXAspectsContinuous) {
                areXAspectsContinuous = aspect.isContinuous();
                throw new IllegalStateException();
            }
            if (aspect.isTimeStamp() != areXAspectsTimeStamp) {
                areXAspectsTimeStamp = aspect.isTimeStamp();
                throw new IllegalStateException();
            }
        }

        /*
         * When xAxisAspects are discrete create a map for all values of all
         * series
         */
        if (!areXAspectsContinuous) {
           generateLabelMap(xAxisAspects, xMap);
        }


        /*
         * Create Y series
         */
        List<LamiTableEntryAspect> yAspects = getYAxisAspects();
        BiMap<@Nullable String, Integer> yMap = checkNotNull(HashBiMap.create());
        boolean yIsLog = graphModel.yAxisIsLog();

        boolean areYAspectsContinuous = areAspectsContinuous(yAxisAspects);
        boolean areYAspectsTimeStamp = areAspectsTimeStamp(yAxisAspects);

        /* Check all aspect are the same type */
        for (LamiTableEntryAspect aspect : yAxisAspects) {
            if (aspect.isContinuous() != areYAspectsContinuous) {
                throw new IllegalStateException();
            }
            if (aspect.isTimeStamp() != areYAspectsTimeStamp) {
                throw new IllegalStateException();
            }
        }

        /*
         * When yAspects are discrete create a map for all values of all
         * series
         */
        if (!areYAspectsContinuous) {
            generateLabelMap(yAxisAspects, yMap);
        }

        /* Plot the series */
        int index = 0;
        for (LamiTableEntryAspect yAspect : getYAxisAspects()) {
            String name = ""; //$NON-NLS-1$
            LamiTableEntryAspect xAspect;
            if (xAxisAspects.size() == 1 ) {
                /* Always map to the same x series */
                xAspect = xAxisAspects.get(0);
                name = yAspect.getLabel();
            } else {
                xAspect = xAxisAspects.get(index);
                name = (yAspect.getName() + ' ' + Messages.LamiScatterViewer_by + ' ' + xAspect.getName());
            }

            List<@Nullable Double> xDoubleSeries = new ArrayList<>();
            List<@Nullable Double> yDoubleSeries = new ArrayList<>();

            if (xAspect.isContinuous()) {
                xDoubleSeries = getResultTable().getEntries().stream().map((entry -> xAspect.resolveDouble(entry))).collect(Collectors.toList());
            } else {
                xDoubleSeries = getResultTable().getEntries().stream().map(entry -> {
                    @Nullable String string = xAspect.resolveString(entry);
                    Integer value = xMap.get(string) ;
                    if (value != null) {
                        return Double.valueOf(value.doubleValue());
                    }
                    return null;

                }).collect(Collectors.toList());
            }

            if (yAspect.isContinuous()) {
                yDoubleSeries = getResultTable().getEntries().stream().map((entry -> yAspect.resolveDouble(entry))).collect(Collectors.toList());
            } else {
                yDoubleSeries = getResultTable().getEntries().stream().map(entry -> {
                    @Nullable String string = yAspect.resolveString(entry);
                    Integer value = yMap.get(string) ;
                    if (value != null) {
                        return Double.valueOf(value.doubleValue());
                    }
                    return null;

                }).collect(Collectors.toList());
            }

            List<@Nullable Double> validXDoubleSeries = new ArrayList<>();
            List<@Nullable Double> validYDoubleSeries = new ArrayList<>();
            List<Integer> indexSeriesCorrespondance = new ArrayList<>();

            if (xDoubleSeries.size() != yDoubleSeries.size()) {
                throw new IllegalStateException("Series sizes don't match!"); //$NON-NLS-1$
            }


            /* Check for invalid tuple value. Any null elements are invalid */
            for (int i = 0; i < xDoubleSeries.size(); i++) {
                @Nullable Double xValue = xDoubleSeries.get(i);
                @Nullable Double yValue = yDoubleSeries.get(i);
                if (xValue == null || yValue == null) {
                    /* Reject this tuple */
                    continue;
                }
                if ((xIsLog && xValue <= ZERO) || (yIsLog && yValue <= ZERO)) {
                    /* Equal or less than 0 values can't be plotted on logscale */
                    continue;
                }
                    validXDoubleSeries.add(xValue);
                    validYDoubleSeries.add(yValue);
                    indexSeriesCorrespondance.add(i);
            }

            ILineSeries scatterSeries = (ILineSeries) getChart().getSeriesSet().createSeries(SeriesType.LINE, name);
            scatterSeries.setLineStyle(LineStyle.NONE);

            double[] xserie = validXDoubleSeries.stream().mapToDouble(elem -> checkNotNull(elem).doubleValue()).toArray();
            double[] yserie = validYDoubleSeries.stream().mapToDouble(elem -> checkNotNull(elem).doubleValue()).toArray();
            scatterSeries.setXSeries(xserie);
            scatterSeries.setYSeries(yserie);
            fIndexMapping.put(scatterSeries, indexSeriesCorrespondance);
            index++;
        }

        /* Modify x axis related chart styling */
        IAxisTick xTick = getChart().getAxisSet().getXAxis(0).getTick();
        if (areXAspectsContinuous) {
            xTick.setFormat(getContinuousAxisFormatter(xAxisAspects, getResultTable().getEntries()));
        } else {
            xTick.setFormat(new LamiLabelFormat(xMap));
            updateTickMark(xMap, xTick, getChart().getPlotArea().getSize().x);

            /* Remove vertical grid line */
            getChart().getAxisSet().getXAxis(0).getGrid().setStyle(LineStyle.NONE);
        }


        /* Modify Y axis related chart styling */
        IAxisTick yTick = getChart().getAxisSet().getYAxis(0).getTick();
        if (areYAspectsContinuous) {
            yTick.setFormat(getContinuousAxisFormatter(yAxisAspects, getResultTable().getEntries()));
        } else {
            yTick.setFormat(new LamiLabelFormat(yMap));
            updateTickMark(yMap, yTick, getChart().getPlotArea().getSize().y);

            /*
             * SWTCHART workaround: Swtchart fiddle with tick mark visibility
             * based on the fact that it can parse the label to double or not.
             * If the label happen to be a double it check for the presence of
             * the value in it's own tick labels to value map for it presence.
             * If it happen that the parsed value is not present in the map the
             * tick get a visibility of false. The X axis does not have this
             * problem since SWTCHART check on label angle and if !=0 simply do
             * no logic regarding visibility. So simply set a label angle of 1
             * to the axis.
             */
            yTick.setTickLabelAngle(1);

            /* Remove horizontal grid line */
            getChart().getAxisSet().getYAxis(0).getGrid().setStyle(LineStyle.NONE);
        }

        setLineSeriesColor();


        /* Put log scale if necessary */
        if (xIsLog && areXAspectsContinuous && !areXAspectsTimeStamp) {
            Stream.of(getChart().getAxisSet().getXAxes()).forEach(axis -> axis.enableLogScale(xIsLog));
        }

        if (yIsLog && areYAspectsContinuous && !areYAspectsTimeStamp) {
            /* Set the axis as logscale */
            Stream.of(getChart().getAxisSet().getYAxes()).forEach(axis -> axis.enableLogScale(yIsLog));
        }

        getChart().getAxisSet().adjustRange();

        getChart().getPlotArea().addListener(SWT.MouseDown, new LamiScatterMouseDownListener());
        getChart().getPlotArea().addPaintListener(new LamiScatterPainterListener());

        /* On resize check for axis tick updating */
        getChart().addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(@Nullable Event event) {
                if (yTick.getFormat() instanceof LamiLabelFormat) {
                    updateTickMark(yMap, yTick, getChart().getPlotArea().getSize().y);
                }
                if (xTick.getFormat() instanceof LamiLabelFormat) {
                    updateTickMark(xMap, xTick, getChart().getPlotArea().getSize().x);
                }
            }
        });
    }

    private void generateLabelMap(List<LamiTableEntryAspect> aspects, BiMap<@Nullable String, Integer> map ) {
        TreeSet<@Nullable String> set = new TreeSet<>();
        for (LamiTableEntryAspect aspect : aspects) {
            for (LamiTableEntry entry : getResultTable().getEntries()) {
                String string = aspect.resolveString(entry);
                if (string != null) {
                    set.add(string);
                }
            }
        }
        /* Ordered label mapping to double */
        for (String string : set) {
            map.put(string, map.size());
        }
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

            boolean ctrlMode = false;

            ISeries[] series = getChart().getSeriesSet().getSeries();
            Set<Integer> selections = getSelection();

            /* Check for ctrl on click */
            if ((event.stateMask & SWT.CTRL) != 0) {
                selections = getSelection();
                ctrlMode = true;
            } else {
                /* Reset selection */
                unsetSelection();
                selections = new HashSet<>();
            }

            for (ISeries oneSeries : series) {
                ILineSeries lineSerie = (ILineSeries) oneSeries;

                int closest = -1;
                double closestDistance = -1;
                for (int i = 0; i < lineSerie.getXSeries().length; i++) {
                    Point dataPoint = lineSerie.getPixelCoordinates(i);

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
                    int tableEntryIndex = getTableEntryIndexFromGraphIndex(checkNotNull(oneSeries), closest);
                    if (tableEntryIndex < 0) {
                        continue;
                    }
                    LamiTableEntry entry = getResultTable().getEntries().get(tableEntryIndex);
                    int index = getResultTable().getEntries().indexOf(entry);

                    if (!ctrlMode || !selections.remove(index)) {
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

    private int getTableEntryIndexFromGraphIndex(ISeries series,int index) {
        List<Integer> indexes = fIndexMapping.get(series);
        if (indexes == null || index > indexes.size() || index < 0) {
            return -1;
        }
        return indexes.get(index);
    }

    private int getGraphIndexFromTableEntryIndex(ISeries series, int index) {
        List<Integer> indexes = fIndexMapping.get(series);
        if (indexes == null || !indexes.contains(index)) {
            return -1;
        }
        return indexes.indexOf(index);
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
                    int graphIndex = getGraphIndexFromTableEntryIndex(checkNotNull(series), index);
                    if (graphIndex < 0) {
                        continue;
                    }
                    Point point = series.getPixelCoordinates(graphIndex);
                    /* Vertical line */
                    gc.drawLine(point.x, 0 , point.x, getChart().getPlotArea().getSize().y);
                    /* Horizontal line */
                    gc.drawLine(0, point.y, getChart().getPlotArea().getSize().x, point.y);
                }
            }
        }
    }

    @Override
    protected void refreshDisplayLabels() {
    }

    /**
     * Return the current selection in internal mapping
     *
     * @return the internal selections
     */
    protected Set<Integer> getInternalSelections() {
        /* Translate to internal table location */
        Set<Integer> indexes = super.getSelection();
        Set<Integer> internalIndexes = indexes.stream()
                .mapToInt(index -> getResultTable().getEntries().indexOf((getResultTable().getEntries().get(index))))
                .boxed()
                .collect(Collectors.toSet());
        return internalIndexes;
    }

    private static void updateTickMark(BiMap<@Nullable String, Integer> map, IAxisTick tick, int availableLenghtPixel) {
        int stepSizePixel = availableLenghtPixel / ((map.size() != 0) ? map.size() - 1 : 1);
        /*
         * This step is a limitation on swtchart side regarding
         * minimal grid step hint size. When the step size are
         * smaller it get defined as the "default" value for the
         * axis instead of the smallest one.
         */
        if (IAxisTick.MIN_GRID_STEP_HINT > stepSizePixel) {
            stepSizePixel = (int) IAxisTick.MIN_GRID_STEP_HINT;
        }
        tick.setTickMarkStepHint(stepSizePixel);
    }

}
