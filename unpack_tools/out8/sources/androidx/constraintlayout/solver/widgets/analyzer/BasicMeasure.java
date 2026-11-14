package androidx.constraintlayout.solver.widgets.analyzer;

import androidx.constraintlayout.solver.LinearSystem;
import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
import androidx.constraintlayout.solver.widgets.ConstraintWidget;
import androidx.constraintlayout.solver.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.solver.widgets.Guideline;
import androidx.constraintlayout.solver.widgets.Helper;
import androidx.constraintlayout.solver.widgets.Optimizer;
import androidx.constraintlayout.solver.widgets.VirtualLayout;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public class BasicMeasure {
    private ConstraintWidgetContainer constraintWidgetContainer;
    private final ArrayList<ConstraintWidget> mVariableDimensionsWidgets = new ArrayList<>();
    private Measure mMeasure = new Measure();

    /* loaded from: classes.dex */
    public static class Measure {
        public ConstraintWidget.DimensionBehaviour horizontalBehavior;
        public int horizontalDimension;
        public int measuredBaseline;
        public boolean measuredHasBaseline;
        public int measuredHeight;
        public boolean measuredNeedsSolverPass;
        public int measuredWidth;
        public boolean useCurrentDimensions;
        public ConstraintWidget.DimensionBehaviour verticalBehavior;
        public int verticalDimension;
    }

    /* loaded from: classes.dex */
    public interface Measurer {
        void didMeasures();

        void measure(ConstraintWidget constraintWidget, Measure measure);
    }

    public void updateHierarchy(ConstraintWidgetContainer layout) {
        int i;
        this.mVariableDimensionsWidgets.clear();
        int childCount = layout.mChildren.size();
        while (i < childCount) {
            ConstraintWidget widget = layout.mChildren.get(i);
            ConstraintWidget.DimensionBehaviour horizontalDimensionBehaviour = widget.getHorizontalDimensionBehaviour();
            ConstraintWidget.DimensionBehaviour dimensionBehaviour = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
            if (horizontalDimensionBehaviour != dimensionBehaviour) {
                ConstraintWidget.DimensionBehaviour horizontalDimensionBehaviour2 = widget.getHorizontalDimensionBehaviour();
                ConstraintWidget.DimensionBehaviour dimensionBehaviour2 = ConstraintWidget.DimensionBehaviour.MATCH_PARENT;
                i = (horizontalDimensionBehaviour2 == dimensionBehaviour2 || widget.getVerticalDimensionBehaviour() == dimensionBehaviour || widget.getVerticalDimensionBehaviour() == dimensionBehaviour2) ? 0 : i + 1;
            }
            this.mVariableDimensionsWidgets.add(widget);
        }
        layout.invalidateGraph();
    }

    public BasicMeasure(ConstraintWidgetContainer constraintWidgetContainer) {
        this.constraintWidgetContainer = constraintWidgetContainer;
    }

    private void measureChildren(ConstraintWidgetContainer layout) {
        int childCount = layout.mChildren.size();
        Measurer measurer = layout.getMeasurer();
        for (int i = 0; i < childCount; i++) {
            ConstraintWidget child = layout.mChildren.get(i);
            if (!(child instanceof Guideline) && (!child.horizontalRun.dimension.resolved || !child.verticalRun.dimension.resolved)) {
                ConstraintWidget.DimensionBehaviour widthBehavior = child.getDimensionBehaviour(0);
                boolean skip = true;
                ConstraintWidget.DimensionBehaviour heightBehavior = child.getDimensionBehaviour(1);
                ConstraintWidget.DimensionBehaviour dimensionBehaviour = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                if (widthBehavior != dimensionBehaviour || child.mMatchConstraintDefaultWidth == 1 || heightBehavior != dimensionBehaviour || child.mMatchConstraintDefaultHeight == 1) {
                    skip = false;
                }
                if (!skip) {
                    measure(measurer, child, false);
                    Objects.requireNonNull(layout);
                }
            }
        }
        measurer.didMeasures();
    }

    private void solveLinearSystem(ConstraintWidgetContainer layout, String reason, int w, int h) {
        int minWidth = layout.getMinWidth();
        int minHeight = layout.getMinHeight();
        layout.setMinWidth(0);
        layout.setMinHeight(0);
        layout.setWidth(w);
        layout.setHeight(h);
        layout.setMinWidth(minWidth);
        layout.setMinHeight(minHeight);
        this.constraintWidgetContainer.layout();
    }

    public long solverMeasure(ConstraintWidgetContainer layout, int optimizationLevel, int paddingX, int paddingY, int widthMode, int widthSize, int heightMode, int heightSize, int lastMeasureWidth, int lastMeasureHeight) {
        boolean optimize;
        boolean ratio;
        int widthSize2;
        boolean allSolved;
        int heightSize2;
        long layoutTime;
        int optimizations;
        int sizeDependentWidgetsCount;
        int maxIterations;
        int j;
        Measurer measurer;
        boolean containerWrapWidth;
        int optimizations2;
        boolean needSolverPass;
        int heightSize3;
        boolean allSolved2;
        boolean allSolved3;
        Measurer measurer2 = layout.getMeasurer();
        long layoutTime2 = 0;
        int childCount = layout.mChildren.size();
        int startingWidth = layout.getWidth();
        int startingHeight = layout.getHeight();
        boolean optimizeWrap = Optimizer.enabled(optimizationLevel, 128);
        boolean optimize2 = optimizeWrap || Optimizer.enabled(optimizationLevel, 64);
        if (!optimize2) {
            optimize = optimize2;
        } else {
            int i = 0;
            while (i < childCount) {
                ConstraintWidget child = layout.mChildren.get(i);
                ConstraintWidget.DimensionBehaviour horizontalDimensionBehaviour = child.getHorizontalDimensionBehaviour();
                ConstraintWidget.DimensionBehaviour dimensionBehaviour = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                boolean matchWidth = horizontalDimensionBehaviour == dimensionBehaviour;
                boolean optimize3 = optimize2;
                boolean matchHeight = child.getVerticalDimensionBehaviour() == dimensionBehaviour;
                boolean ratio2 = matchWidth && matchHeight && child.getDimensionRatio() > 0.0f;
                if (child.isInHorizontalChain() && ratio2) {
                    ratio = false;
                    break;
                }
                if (child.isInVerticalChain() && ratio2) {
                    ratio = false;
                    break;
                }
                boolean matchHeight2 = child instanceof VirtualLayout;
                if (matchHeight2) {
                    ratio = false;
                    break;
                }
                if (!child.isInHorizontalChain() && !child.isInVerticalChain()) {
                    i++;
                    optimize2 = optimize3;
                } else {
                    ratio = false;
                    break;
                }
            }
            optimize = optimize2;
        }
        ratio = optimize;
        if (ratio) {
            boolean z = LinearSystem.OPTIMIZED_ENGINE;
        }
        boolean optimize4 = ((widthMode == 1073741824 && heightMode == 1073741824) || optimizeWrap) & ratio;
        int computations = 0;
        if (!optimize4) {
            widthSize2 = widthSize;
            allSolved = false;
            heightSize2 = heightSize;
        } else {
            widthSize2 = Math.min(layout.getMaxWidth(), widthSize);
            int heightSize4 = Math.min(layout.getMaxHeight(), heightSize);
            if (widthMode == 1073741824 && layout.getWidth() != widthSize2) {
                layout.setWidth(widthSize2);
                layout.invalidateGraph();
            }
            if (heightMode == 1073741824 && layout.getHeight() != heightSize4) {
                layout.setHeight(heightSize4);
                layout.invalidateGraph();
            }
            if (widthMode == 1073741824 && heightMode == 1073741824) {
                boolean allSolved4 = layout.directMeasure(optimizeWrap);
                computations = 2;
                heightSize3 = heightSize4;
                allSolved2 = allSolved4;
            } else {
                boolean allSolved5 = layout.directMeasureSetup(optimizeWrap);
                heightSize3 = heightSize4;
                if (widthMode == 1073741824) {
                    allSolved5 &= layout.directMeasureWithOrientation(optimizeWrap, 0);
                    computations = 0 + 1;
                }
                if (heightMode != 1073741824) {
                    allSolved2 = allSolved5;
                } else {
                    allSolved2 = allSolved5 & layout.directMeasureWithOrientation(optimizeWrap, 1);
                    computations++;
                }
            }
            if (!allSolved2) {
                allSolved3 = allSolved2;
            } else {
                allSolved3 = allSolved2;
                boolean allSolved6 = widthMode == 1073741824;
                layout.updateFromRuns(allSolved6, heightMode == 1073741824);
            }
            heightSize2 = heightSize3;
            allSolved = allSolved3;
        }
        if (allSolved && computations == 2) {
            return 0L;
        }
        if (childCount > 0) {
            measureChildren(layout);
        }
        int optimizations3 = layout.getOptimizationLevel();
        int sizeDependentWidgetsCount2 = this.mVariableDimensionsWidgets.size();
        if (childCount > 0) {
            solveLinearSystem(layout, "First pass", startingWidth, startingHeight);
        }
        if (sizeDependentWidgetsCount2 > 0) {
            ConstraintWidget.DimensionBehaviour horizontalDimensionBehaviour2 = layout.getHorizontalDimensionBehaviour();
            ConstraintWidget.DimensionBehaviour dimensionBehaviour2 = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
            boolean containerWrapWidth2 = horizontalDimensionBehaviour2 == dimensionBehaviour2;
            boolean containerWrapHeight = layout.getVerticalDimensionBehaviour() == dimensionBehaviour2;
            int minWidth = Math.max(layout.getWidth(), this.constraintWidgetContainer.getMinWidth());
            int minHeight = Math.max(layout.getHeight(), this.constraintWidgetContainer.getMinHeight());
            boolean needSolverPass2 = false;
            int widthSize3 = minHeight;
            int i2 = 0;
            int minWidth2 = minWidth;
            while (i2 < sizeDependentWidgetsCount2) {
                int computations2 = computations;
                ConstraintWidget widget = this.mVariableDimensionsWidgets.get(i2);
                long layoutTime3 = layoutTime2;
                if (!(widget instanceof VirtualLayout)) {
                    optimizations2 = optimizations3;
                } else {
                    int preWidth = widget.getWidth();
                    int preHeight = widget.getHeight();
                    optimizations2 = optimizations3;
                    boolean needSolverPass3 = needSolverPass2 | measure(measurer2, widget, true);
                    Objects.requireNonNull(layout);
                    int measuredWidth = widget.getWidth();
                    int measuredHeight = widget.getHeight();
                    if (measuredWidth == preWidth) {
                        needSolverPass = needSolverPass3;
                    } else {
                        widget.setWidth(measuredWidth);
                        if (containerWrapWidth2) {
                            int preWidth2 = widget.getRight();
                            if (preWidth2 > minWidth2) {
                                int w = widget.getRight() + widget.getAnchor(ConstraintAnchor.Type.RIGHT).getMargin();
                                minWidth2 = Math.max(minWidth2, w);
                            }
                        }
                        needSolverPass = true;
                    }
                    if (measuredHeight != preHeight) {
                        widget.setHeight(measuredHeight);
                        if (containerWrapHeight && widget.getBottom() > widthSize3) {
                            int h = widget.getBottom() + widget.getAnchor(ConstraintAnchor.Type.BOTTOM).getMargin();
                            widthSize3 = Math.max(widthSize3, h);
                        }
                        needSolverPass = true;
                    }
                    VirtualLayout virtualLayout = (VirtualLayout) widget;
                    needSolverPass2 = needSolverPass | virtualLayout.needSolverPass();
                }
                i2++;
                computations = computations2;
                layoutTime2 = layoutTime3;
                optimizations3 = optimizations2;
            }
            layoutTime = layoutTime2;
            optimizations = optimizations3;
            int maxIterations2 = 2;
            int j2 = 0;
            while (j2 < maxIterations2) {
                int i3 = 0;
                while (i3 < sizeDependentWidgetsCount2) {
                    ConstraintWidget widget2 = this.mVariableDimensionsWidgets.get(i3);
                    if ((widget2 instanceof Helper) && !(widget2 instanceof VirtualLayout)) {
                        sizeDependentWidgetsCount = sizeDependentWidgetsCount2;
                    } else if (widget2 instanceof Guideline) {
                        sizeDependentWidgetsCount = sizeDependentWidgetsCount2;
                    } else {
                        sizeDependentWidgetsCount = sizeDependentWidgetsCount2;
                        if (widget2.getVisibility() != 8 && ((!widget2.horizontalRun.dimension.resolved || !widget2.verticalRun.dimension.resolved) && !(widget2 instanceof VirtualLayout))) {
                            int preWidth3 = widget2.getWidth();
                            int preHeight2 = widget2.getHeight();
                            maxIterations = maxIterations2;
                            int preBaselineDistance = widget2.getBaselineDistance();
                            j = j2;
                            needSolverPass2 |= measure(measurer2, widget2, true);
                            Objects.requireNonNull(layout);
                            int measuredWidth2 = widget2.getWidth();
                            measurer = measurer2;
                            int measuredHeight2 = widget2.getHeight();
                            if (measuredWidth2 != preWidth3) {
                                widget2.setWidth(measuredWidth2);
                                if (containerWrapWidth2) {
                                    int preWidth4 = widget2.getRight();
                                    if (preWidth4 <= minWidth2) {
                                        containerWrapWidth = containerWrapWidth2;
                                    } else {
                                        containerWrapWidth = containerWrapWidth2;
                                        int w2 = widget2.getRight() + widget2.getAnchor(ConstraintAnchor.Type.RIGHT).getMargin();
                                        minWidth2 = Math.max(minWidth2, w2);
                                    }
                                } else {
                                    containerWrapWidth = containerWrapWidth2;
                                }
                                needSolverPass2 = true;
                            } else {
                                containerWrapWidth = containerWrapWidth2;
                            }
                            if (measuredHeight2 != preHeight2) {
                                widget2.setHeight(measuredHeight2);
                                if (containerWrapHeight && widget2.getBottom() > widthSize3) {
                                    int h2 = widget2.getBottom() + widget2.getAnchor(ConstraintAnchor.Type.BOTTOM).getMargin();
                                    widthSize3 = Math.max(widthSize3, h2);
                                }
                                needSolverPass2 = true;
                            }
                            if (widget2.hasBaseline() && preBaselineDistance != widget2.getBaselineDistance()) {
                                needSolverPass2 = true;
                            }
                            i3++;
                            sizeDependentWidgetsCount2 = sizeDependentWidgetsCount;
                            maxIterations2 = maxIterations;
                            j2 = j;
                            measurer2 = measurer;
                            containerWrapWidth2 = containerWrapWidth;
                        }
                    }
                    containerWrapWidth = containerWrapWidth2;
                    measurer = measurer2;
                    maxIterations = maxIterations2;
                    j = j2;
                    i3++;
                    sizeDependentWidgetsCount2 = sizeDependentWidgetsCount;
                    maxIterations2 = maxIterations;
                    j2 = j;
                    measurer2 = measurer;
                    containerWrapWidth2 = containerWrapWidth;
                }
                int sizeDependentWidgetsCount3 = sizeDependentWidgetsCount2;
                boolean containerWrapWidth3 = containerWrapWidth2;
                Measurer measurer3 = measurer2;
                int maxIterations3 = maxIterations2;
                int j3 = j2;
                if (needSolverPass2) {
                    solveLinearSystem(layout, "intermediate pass", startingWidth, startingHeight);
                    needSolverPass2 = false;
                }
                j2 = j3 + 1;
                sizeDependentWidgetsCount2 = sizeDependentWidgetsCount3;
                maxIterations2 = maxIterations3;
                measurer2 = measurer3;
                containerWrapWidth2 = containerWrapWidth3;
            }
            if (needSolverPass2) {
                solveLinearSystem(layout, "2nd pass", startingWidth, startingHeight);
                boolean needSolverPass4 = false;
                if (layout.getWidth() < minWidth2) {
                    layout.setWidth(minWidth2);
                    needSolverPass4 = true;
                }
                if (layout.getHeight() < widthSize3) {
                    layout.setHeight(widthSize3);
                    needSolverPass4 = true;
                }
                if (needSolverPass4) {
                    solveLinearSystem(layout, "3rd pass", startingWidth, startingHeight);
                }
            }
        } else {
            layoutTime = 0;
            optimizations = optimizations3;
        }
        layout.setOptimizationLevel(optimizations);
        return layoutTime;
    }

    private boolean measure(Measurer measurer, ConstraintWidget widget, boolean useCurrentDimensions) {
        this.mMeasure.horizontalBehavior = widget.getHorizontalDimensionBehaviour();
        this.mMeasure.verticalBehavior = widget.getVerticalDimensionBehaviour();
        this.mMeasure.horizontalDimension = widget.getWidth();
        this.mMeasure.verticalDimension = widget.getHeight();
        Measure measure = this.mMeasure;
        measure.measuredNeedsSolverPass = false;
        measure.useCurrentDimensions = useCurrentDimensions;
        ConstraintWidget.DimensionBehaviour dimensionBehaviour = measure.horizontalBehavior;
        ConstraintWidget.DimensionBehaviour dimensionBehaviour2 = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
        boolean horizontalMatchConstraints = dimensionBehaviour == dimensionBehaviour2;
        boolean verticalMatchConstraints = measure.verticalBehavior == dimensionBehaviour2;
        boolean horizontalUseRatio = horizontalMatchConstraints && widget.mDimensionRatio > 0.0f;
        boolean verticalUseRatio = verticalMatchConstraints && widget.mDimensionRatio > 0.0f;
        if (horizontalUseRatio && widget.mResolvedMatchConstraintDefault[0] == 4) {
            measure.horizontalBehavior = ConstraintWidget.DimensionBehaviour.FIXED;
        }
        if (verticalUseRatio && widget.mResolvedMatchConstraintDefault[1] == 4) {
            measure.verticalBehavior = ConstraintWidget.DimensionBehaviour.FIXED;
        }
        measurer.measure(widget, measure);
        widget.setWidth(this.mMeasure.measuredWidth);
        widget.setHeight(this.mMeasure.measuredHeight);
        widget.setHasBaseline(this.mMeasure.measuredHasBaseline);
        widget.setBaselineDistance(this.mMeasure.measuredBaseline);
        Measure measure2 = this.mMeasure;
        measure2.useCurrentDimensions = false;
        return measure2.measuredNeedsSolverPass;
    }
}
