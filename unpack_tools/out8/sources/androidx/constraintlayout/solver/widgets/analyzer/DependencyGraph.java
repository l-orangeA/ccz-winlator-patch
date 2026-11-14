package androidx.constraintlayout.solver.widgets.analyzer;

import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
import androidx.constraintlayout.solver.widgets.ConstraintWidget;
import androidx.constraintlayout.solver.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.solver.widgets.Guideline;
import androidx.constraintlayout.solver.widgets.HelperWidget;
import androidx.constraintlayout.solver.widgets.analyzer.BasicMeasure;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/* loaded from: classes.dex */
public class DependencyGraph {
    private ConstraintWidgetContainer container;
    private ConstraintWidgetContainer mContainer;
    private boolean mNeedBuildGraph = true;
    private boolean mNeedRedoMeasures = true;
    private ArrayList<WidgetRun> mRuns = new ArrayList<>();
    private ArrayList<RunGroup> runGroups = new ArrayList<>();
    private BasicMeasure.Measurer mMeasurer = null;
    private BasicMeasure.Measure mMeasure = new BasicMeasure.Measure();
    ArrayList<RunGroup> mGroups = new ArrayList<>();

    public DependencyGraph(ConstraintWidgetContainer container) {
        this.container = container;
        this.mContainer = container;
    }

    public void setMeasurer(BasicMeasure.Measurer measurer) {
        this.mMeasurer = measurer;
    }

    private int computeWrap(ConstraintWidgetContainer container, int orientation) {
        int count = this.mGroups.size();
        long wrapSize = 0;
        for (int i = 0; i < count; i++) {
            RunGroup run = this.mGroups.get(i);
            long size = run.computeWrapSize(container, orientation);
            wrapSize = Math.max(wrapSize, size);
        }
        int i2 = (int) wrapSize;
        return i2;
    }

    public boolean directMeasure(boolean optimizeWrap) {
        boolean optimizeWrap2 = optimizeWrap & true;
        if (this.mNeedBuildGraph || this.mNeedRedoMeasures) {
            Iterator<ConstraintWidget> it = this.container.mChildren.iterator();
            while (it.hasNext()) {
                ConstraintWidget widget = it.next();
                widget.measured = false;
                widget.horizontalRun.reset();
                widget.verticalRun.reset();
            }
            ConstraintWidgetContainer constraintWidgetContainer = this.container;
            constraintWidgetContainer.measured = false;
            constraintWidgetContainer.horizontalRun.reset();
            this.container.verticalRun.reset();
            this.mNeedRedoMeasures = false;
        }
        boolean avoid = basicMeasureWidgets(this.mContainer);
        if (avoid) {
            return false;
        }
        this.container.setX(0);
        this.container.setY(0);
        ConstraintWidget.DimensionBehaviour originalHorizontalDimension = this.container.getDimensionBehaviour(0);
        ConstraintWidget.DimensionBehaviour originalVerticalDimension = this.container.getDimensionBehaviour(1);
        if (this.mNeedBuildGraph) {
            buildGraph();
        }
        int x1 = this.container.getX();
        int y1 = this.container.getY();
        this.container.horizontalRun.start.resolve(x1);
        this.container.verticalRun.start.resolve(y1);
        measureWidgets();
        ConstraintWidget.DimensionBehaviour dimensionBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
        if (originalHorizontalDimension == dimensionBehaviour || originalVerticalDimension == dimensionBehaviour) {
            if (optimizeWrap2) {
                Iterator<WidgetRun> it2 = this.mRuns.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        break;
                    }
                    if (!it2.next().supportsWrapComputation()) {
                        optimizeWrap2 = false;
                        break;
                    }
                }
            }
            if (optimizeWrap2 && originalHorizontalDimension == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                this.container.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                ConstraintWidgetContainer constraintWidgetContainer2 = this.container;
                constraintWidgetContainer2.setWidth(computeWrap(constraintWidgetContainer2, 0));
                ConstraintWidgetContainer constraintWidgetContainer3 = this.container;
                constraintWidgetContainer3.horizontalRun.dimension.resolve(constraintWidgetContainer3.getWidth());
            }
            if (optimizeWrap2 && originalVerticalDimension == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                this.container.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                ConstraintWidgetContainer constraintWidgetContainer4 = this.container;
                constraintWidgetContainer4.setHeight(computeWrap(constraintWidgetContainer4, 1));
                ConstraintWidgetContainer constraintWidgetContainer5 = this.container;
                constraintWidgetContainer5.verticalRun.dimension.resolve(constraintWidgetContainer5.getHeight());
            }
        }
        boolean checkRoot = false;
        ConstraintWidgetContainer constraintWidgetContainer6 = this.container;
        ConstraintWidget.DimensionBehaviour[] dimensionBehaviourArr = constraintWidgetContainer6.mListDimensionBehaviors;
        ConstraintWidget.DimensionBehaviour dimensionBehaviour2 = dimensionBehaviourArr[0];
        ConstraintWidget.DimensionBehaviour dimensionBehaviour3 = ConstraintWidget.DimensionBehaviour.FIXED;
        if (dimensionBehaviour2 == dimensionBehaviour3 || dimensionBehaviourArr[0] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
            int x2 = constraintWidgetContainer6.getWidth() + x1;
            this.container.horizontalRun.end.resolve(x2);
            this.container.horizontalRun.dimension.resolve(x2 - x1);
            measureWidgets();
            ConstraintWidgetContainer constraintWidgetContainer7 = this.container;
            ConstraintWidget.DimensionBehaviour[] dimensionBehaviourArr2 = constraintWidgetContainer7.mListDimensionBehaviors;
            if (dimensionBehaviourArr2[1] == dimensionBehaviour3 || dimensionBehaviourArr2[1] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
                int y2 = constraintWidgetContainer7.getHeight() + y1;
                this.container.verticalRun.end.resolve(y2);
                this.container.verticalRun.dimension.resolve(y2 - y1);
            }
            measureWidgets();
            checkRoot = true;
        }
        Iterator<WidgetRun> it3 = this.mRuns.iterator();
        while (it3.hasNext()) {
            WidgetRun run = it3.next();
            if (run.widget != this.container || run.resolved) {
                run.applyToWidget();
            }
        }
        boolean allResolved = true;
        Iterator<WidgetRun> it4 = this.mRuns.iterator();
        while (true) {
            if (!it4.hasNext()) {
                break;
            }
            WidgetRun run2 = it4.next();
            if (checkRoot || run2.widget != this.container) {
                if (!run2.start.resolved) {
                    allResolved = false;
                    break;
                }
                if (!run2.end.resolved && !(run2 instanceof GuidelineReference)) {
                    allResolved = false;
                    break;
                }
                if (!run2.dimension.resolved && !(run2 instanceof ChainRun) && !(run2 instanceof GuidelineReference)) {
                    allResolved = false;
                    break;
                }
            }
        }
        this.container.setHorizontalDimensionBehaviour(originalHorizontalDimension);
        this.container.setVerticalDimensionBehaviour(originalVerticalDimension);
        return allResolved;
    }

    public boolean directMeasureSetup(boolean optimizeWrap) {
        if (this.mNeedBuildGraph) {
            Iterator<ConstraintWidget> it = this.container.mChildren.iterator();
            while (it.hasNext()) {
                ConstraintWidget widget = it.next();
                widget.measured = false;
                HorizontalWidgetRun horizontalWidgetRun = widget.horizontalRun;
                horizontalWidgetRun.dimension.resolved = false;
                horizontalWidgetRun.resolved = false;
                horizontalWidgetRun.reset();
                VerticalWidgetRun verticalWidgetRun = widget.verticalRun;
                verticalWidgetRun.dimension.resolved = false;
                verticalWidgetRun.resolved = false;
                verticalWidgetRun.reset();
            }
            ConstraintWidgetContainer constraintWidgetContainer = this.container;
            constraintWidgetContainer.measured = false;
            HorizontalWidgetRun horizontalWidgetRun2 = constraintWidgetContainer.horizontalRun;
            horizontalWidgetRun2.dimension.resolved = false;
            horizontalWidgetRun2.resolved = false;
            horizontalWidgetRun2.reset();
            VerticalWidgetRun verticalWidgetRun2 = this.container.verticalRun;
            verticalWidgetRun2.dimension.resolved = false;
            verticalWidgetRun2.resolved = false;
            verticalWidgetRun2.reset();
            buildGraph();
        }
        boolean avoid = basicMeasureWidgets(this.mContainer);
        if (avoid) {
            return false;
        }
        this.container.setX(0);
        this.container.setY(0);
        this.container.horizontalRun.start.resolve(0);
        this.container.verticalRun.start.resolve(0);
        return true;
    }

    public boolean directMeasureWithOrientation(boolean optimizeWrap, int orientation) {
        ConstraintWidget.DimensionBehaviour dimensionBehaviour;
        boolean optimizeWrap2 = optimizeWrap & true;
        ConstraintWidget.DimensionBehaviour originalHorizontalDimension = this.container.getDimensionBehaviour(0);
        ConstraintWidget.DimensionBehaviour originalVerticalDimension = this.container.getDimensionBehaviour(1);
        int x1 = this.container.getX();
        int y1 = this.container.getY();
        if (optimizeWrap2 && (originalHorizontalDimension == (dimensionBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) || originalVerticalDimension == dimensionBehaviour)) {
            Iterator<WidgetRun> it = this.mRuns.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                WidgetRun run = it.next();
                if (run.orientation == orientation && !run.supportsWrapComputation()) {
                    optimizeWrap2 = false;
                    break;
                }
            }
            if (orientation == 0) {
                if (optimizeWrap2 && originalHorizontalDimension == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                    this.container.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                    ConstraintWidgetContainer constraintWidgetContainer = this.container;
                    constraintWidgetContainer.setWidth(computeWrap(constraintWidgetContainer, 0));
                    ConstraintWidgetContainer constraintWidgetContainer2 = this.container;
                    constraintWidgetContainer2.horizontalRun.dimension.resolve(constraintWidgetContainer2.getWidth());
                }
            } else if (optimizeWrap2 && originalVerticalDimension == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                this.container.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                ConstraintWidgetContainer constraintWidgetContainer3 = this.container;
                constraintWidgetContainer3.setHeight(computeWrap(constraintWidgetContainer3, 1));
                ConstraintWidgetContainer constraintWidgetContainer4 = this.container;
                constraintWidgetContainer4.verticalRun.dimension.resolve(constraintWidgetContainer4.getHeight());
            }
        }
        boolean checkRoot = false;
        if (orientation == 0) {
            ConstraintWidgetContainer constraintWidgetContainer5 = this.container;
            ConstraintWidget.DimensionBehaviour[] dimensionBehaviourArr = constraintWidgetContainer5.mListDimensionBehaviors;
            if (dimensionBehaviourArr[0] == ConstraintWidget.DimensionBehaviour.FIXED || dimensionBehaviourArr[0] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
                int x2 = constraintWidgetContainer5.getWidth() + x1;
                this.container.horizontalRun.end.resolve(x2);
                this.container.horizontalRun.dimension.resolve(x2 - x1);
                checkRoot = true;
            }
        } else {
            ConstraintWidgetContainer constraintWidgetContainer6 = this.container;
            ConstraintWidget.DimensionBehaviour[] dimensionBehaviourArr2 = constraintWidgetContainer6.mListDimensionBehaviors;
            if (dimensionBehaviourArr2[1] == ConstraintWidget.DimensionBehaviour.FIXED || dimensionBehaviourArr2[1] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
                int y2 = constraintWidgetContainer6.getHeight() + y1;
                this.container.verticalRun.end.resolve(y2);
                this.container.verticalRun.dimension.resolve(y2 - y1);
                checkRoot = true;
            }
        }
        measureWidgets();
        Iterator<WidgetRun> it2 = this.mRuns.iterator();
        while (it2.hasNext()) {
            WidgetRun run2 = it2.next();
            if (run2.orientation == orientation && (run2.widget != this.container || run2.resolved)) {
                run2.applyToWidget();
            }
        }
        boolean allResolved = true;
        Iterator<WidgetRun> it3 = this.mRuns.iterator();
        while (true) {
            if (!it3.hasNext()) {
                break;
            }
            WidgetRun run3 = it3.next();
            if (run3.orientation == orientation && (checkRoot || run3.widget != this.container)) {
                if (!run3.start.resolved) {
                    allResolved = false;
                    break;
                }
                if (!run3.end.resolved) {
                    allResolved = false;
                    break;
                }
                if (!(run3 instanceof ChainRun) && !run3.dimension.resolved) {
                    allResolved = false;
                    break;
                }
            }
        }
        this.container.setHorizontalDimensionBehaviour(originalHorizontalDimension);
        this.container.setVerticalDimensionBehaviour(originalVerticalDimension);
        return allResolved;
    }

    private void measure(ConstraintWidget widget, ConstraintWidget.DimensionBehaviour horizontalBehavior, int horizontalDimension, ConstraintWidget.DimensionBehaviour verticalBehavior, int verticalDimension) {
        BasicMeasure.Measure measure = this.mMeasure;
        measure.horizontalBehavior = horizontalBehavior;
        measure.verticalBehavior = verticalBehavior;
        measure.horizontalDimension = horizontalDimension;
        measure.verticalDimension = verticalDimension;
        this.mMeasurer.measure(widget, measure);
        widget.setWidth(this.mMeasure.measuredWidth);
        widget.setHeight(this.mMeasure.measuredHeight);
        widget.setHasBaseline(this.mMeasure.measuredHasBaseline);
        widget.setBaselineDistance(this.mMeasure.measuredBaseline);
    }

    private boolean basicMeasureWidgets(ConstraintWidgetContainer constraintWidgetContainer) {
        ConstraintWidget.DimensionBehaviour horiz;
        ConstraintWidget.DimensionBehaviour vert;
        ConstraintWidget.DimensionBehaviour dimensionBehaviour;
        ConstraintWidget.DimensionBehaviour dimensionBehaviour2;
        Iterator<ConstraintWidget> it = constraintWidgetContainer.mChildren.iterator();
        while (it.hasNext()) {
            ConstraintWidget widget = it.next();
            ConstraintWidget.DimensionBehaviour[] dimensionBehaviourArr = widget.mListDimensionBehaviors;
            ConstraintWidget.DimensionBehaviour horiz2 = dimensionBehaviourArr[0];
            ConstraintWidget.DimensionBehaviour vert2 = dimensionBehaviourArr[1];
            if (widget.getVisibility() == 8) {
                widget.measured = true;
            } else {
                if (widget.mMatchConstraintPercentWidth < 1.0f && horiz2 == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    widget.mMatchConstraintDefaultWidth = 2;
                }
                if (widget.mMatchConstraintPercentHeight < 1.0f && vert2 == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    widget.mMatchConstraintDefaultHeight = 2;
                }
                if (widget.getDimensionRatio() > 0.0f) {
                    ConstraintWidget.DimensionBehaviour dimensionBehaviour3 = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                    if (horiz2 == dimensionBehaviour3 && (vert2 == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT || vert2 == ConstraintWidget.DimensionBehaviour.FIXED)) {
                        widget.mMatchConstraintDefaultWidth = 3;
                    } else if (vert2 == dimensionBehaviour3 && (horiz2 == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT || horiz2 == ConstraintWidget.DimensionBehaviour.FIXED)) {
                        widget.mMatchConstraintDefaultHeight = 3;
                    } else if (horiz2 == dimensionBehaviour3 && vert2 == dimensionBehaviour3) {
                        if (widget.mMatchConstraintDefaultWidth == 0) {
                            widget.mMatchConstraintDefaultWidth = 3;
                        }
                        if (widget.mMatchConstraintDefaultHeight == 0) {
                            widget.mMatchConstraintDefaultHeight = 3;
                        }
                    }
                }
                ConstraintWidget.DimensionBehaviour dimensionBehaviour4 = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                if (horiz2 == dimensionBehaviour4 && widget.mMatchConstraintDefaultWidth == 1 && (widget.mLeft.mTarget == null || widget.mRight.mTarget == null)) {
                    horiz = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                } else {
                    horiz = horiz2;
                }
                if (vert2 == dimensionBehaviour4 && widget.mMatchConstraintDefaultHeight == 1 && (widget.mTop.mTarget == null || widget.mBottom.mTarget == null)) {
                    vert = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                } else {
                    vert = vert2;
                }
                HorizontalWidgetRun horizontalWidgetRun = widget.horizontalRun;
                horizontalWidgetRun.dimensionBehavior = horiz;
                int i = widget.mMatchConstraintDefaultWidth;
                horizontalWidgetRun.matchConstraintsType = i;
                VerticalWidgetRun verticalWidgetRun = widget.verticalRun;
                verticalWidgetRun.dimensionBehavior = vert;
                int i2 = widget.mMatchConstraintDefaultHeight;
                verticalWidgetRun.matchConstraintsType = i2;
                ConstraintWidget.DimensionBehaviour dimensionBehaviour5 = ConstraintWidget.DimensionBehaviour.MATCH_PARENT;
                if ((horiz == dimensionBehaviour5 || horiz == ConstraintWidget.DimensionBehaviour.FIXED || horiz == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) && (vert == dimensionBehaviour5 || vert == ConstraintWidget.DimensionBehaviour.FIXED || vert == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT)) {
                    int width = widget.getWidth();
                    if (horiz == dimensionBehaviour5) {
                        width = (constraintWidgetContainer.getWidth() - widget.mLeft.mMargin) - widget.mRight.mMargin;
                        horiz = ConstraintWidget.DimensionBehaviour.FIXED;
                    }
                    int height = widget.getHeight();
                    if (vert == dimensionBehaviour5) {
                        height = (constraintWidgetContainer.getHeight() - widget.mTop.mMargin) - widget.mBottom.mMargin;
                        vert = ConstraintWidget.DimensionBehaviour.FIXED;
                    }
                    measure(widget, horiz, width, vert, height);
                    widget.horizontalRun.dimension.resolve(widget.getWidth());
                    widget.verticalRun.dimension.resolve(widget.getHeight());
                    widget.measured = true;
                } else {
                    if (horiz == dimensionBehaviour4 && (vert == (dimensionBehaviour2 = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) || vert == ConstraintWidget.DimensionBehaviour.FIXED)) {
                        if (i == 3) {
                            if (vert == dimensionBehaviour2) {
                                measure(widget, dimensionBehaviour2, 0, dimensionBehaviour2, 0);
                            }
                            int height2 = widget.getHeight();
                            int width2 = (int) ((height2 * widget.mDimensionRatio) + 0.5f);
                            ConstraintWidget.DimensionBehaviour dimensionBehaviour6 = ConstraintWidget.DimensionBehaviour.FIXED;
                            measure(widget, dimensionBehaviour6, width2, dimensionBehaviour6, height2);
                            widget.horizontalRun.dimension.resolve(widget.getWidth());
                            widget.verticalRun.dimension.resolve(widget.getHeight());
                            widget.measured = true;
                        } else if (i == 1) {
                            measure(widget, dimensionBehaviour2, 0, vert, 0);
                            widget.horizontalRun.dimension.wrapValue = widget.getWidth();
                        } else if (i == 2) {
                            ConstraintWidget.DimensionBehaviour[] dimensionBehaviourArr2 = constraintWidgetContainer.mListDimensionBehaviors;
                            ConstraintWidget.DimensionBehaviour dimensionBehaviour7 = dimensionBehaviourArr2[0];
                            ConstraintWidget.DimensionBehaviour dimensionBehaviour8 = ConstraintWidget.DimensionBehaviour.FIXED;
                            if (dimensionBehaviour7 == dimensionBehaviour8 || dimensionBehaviourArr2[0] == dimensionBehaviour5) {
                                float percent = widget.mMatchConstraintPercentWidth;
                                int width3 = (int) ((constraintWidgetContainer.getWidth() * percent) + 0.5f);
                                int height3 = widget.getHeight();
                                measure(widget, dimensionBehaviour8, width3, vert, height3);
                                widget.horizontalRun.dimension.resolve(widget.getWidth());
                                widget.verticalRun.dimension.resolve(widget.getHeight());
                                widget.measured = true;
                            }
                        } else {
                            ConstraintAnchor[] constraintAnchorArr = widget.mListAnchors;
                            if (constraintAnchorArr[0].mTarget == null || constraintAnchorArr[1].mTarget == null) {
                                measure(widget, dimensionBehaviour2, 0, vert, 0);
                                widget.horizontalRun.dimension.resolve(widget.getWidth());
                                widget.verticalRun.dimension.resolve(widget.getHeight());
                                widget.measured = true;
                            }
                        }
                    }
                    if (vert == dimensionBehaviour4 && (horiz == (dimensionBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) || horiz == ConstraintWidget.DimensionBehaviour.FIXED)) {
                        if (i2 == 3) {
                            if (horiz == dimensionBehaviour) {
                                measure(widget, dimensionBehaviour, 0, dimensionBehaviour, 0);
                            }
                            int width4 = widget.getWidth();
                            float ratio = widget.mDimensionRatio;
                            if (widget.getDimensionRatioSide() == -1) {
                                ratio = 1.0f / ratio;
                            }
                            int height4 = (int) ((width4 * ratio) + 0.5f);
                            ConstraintWidget.DimensionBehaviour dimensionBehaviour9 = ConstraintWidget.DimensionBehaviour.FIXED;
                            measure(widget, dimensionBehaviour9, width4, dimensionBehaviour9, height4);
                            widget.horizontalRun.dimension.resolve(widget.getWidth());
                            widget.verticalRun.dimension.resolve(widget.getHeight());
                            widget.measured = true;
                        } else if (i2 == 1) {
                            measure(widget, horiz, 0, dimensionBehaviour, 0);
                            widget.verticalRun.dimension.wrapValue = widget.getHeight();
                        } else if (i2 == 2) {
                            ConstraintWidget.DimensionBehaviour[] dimensionBehaviourArr3 = constraintWidgetContainer.mListDimensionBehaviors;
                            ConstraintWidget.DimensionBehaviour dimensionBehaviour10 = dimensionBehaviourArr3[1];
                            ConstraintWidget.DimensionBehaviour dimensionBehaviour11 = ConstraintWidget.DimensionBehaviour.FIXED;
                            if (dimensionBehaviour10 == dimensionBehaviour11 || dimensionBehaviourArr3[1] == dimensionBehaviour5) {
                                float percent2 = widget.mMatchConstraintPercentHeight;
                                int width5 = widget.getWidth();
                                int height5 = (int) ((constraintWidgetContainer.getHeight() * percent2) + 0.5f);
                                measure(widget, horiz, width5, dimensionBehaviour11, height5);
                                widget.horizontalRun.dimension.resolve(widget.getWidth());
                                widget.verticalRun.dimension.resolve(widget.getHeight());
                                widget.measured = true;
                            }
                        } else {
                            ConstraintAnchor[] constraintAnchorArr2 = widget.mListAnchors;
                            if (constraintAnchorArr2[2].mTarget == null || constraintAnchorArr2[3].mTarget == null) {
                                measure(widget, dimensionBehaviour, 0, vert, 0);
                                widget.horizontalRun.dimension.resolve(widget.getWidth());
                                widget.verticalRun.dimension.resolve(widget.getHeight());
                                widget.measured = true;
                            }
                        }
                    }
                    if (horiz == dimensionBehaviour4 && vert == dimensionBehaviour4) {
                        if (i == 1 || i2 == 1) {
                            ConstraintWidget.DimensionBehaviour dimensionBehaviour12 = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                            measure(widget, dimensionBehaviour12, 0, dimensionBehaviour12, 0);
                            widget.horizontalRun.dimension.wrapValue = widget.getWidth();
                            widget.verticalRun.dimension.wrapValue = widget.getHeight();
                        } else if (i2 == 2 && i == 2) {
                            ConstraintWidget.DimensionBehaviour[] dimensionBehaviourArr4 = constraintWidgetContainer.mListDimensionBehaviors;
                            ConstraintWidget.DimensionBehaviour dimensionBehaviour13 = dimensionBehaviourArr4[0];
                            ConstraintWidget.DimensionBehaviour dimensionBehaviour14 = ConstraintWidget.DimensionBehaviour.FIXED;
                            if (dimensionBehaviour13 == dimensionBehaviour14 || dimensionBehaviourArr4[0] == dimensionBehaviour14) {
                                if (dimensionBehaviourArr4[1] == dimensionBehaviour14 || dimensionBehaviourArr4[1] == dimensionBehaviour14) {
                                    float horizPercent = widget.mMatchConstraintPercentWidth;
                                    float vertPercent = widget.mMatchConstraintPercentHeight;
                                    int width6 = (int) ((constraintWidgetContainer.getWidth() * horizPercent) + 0.5f);
                                    int height6 = (int) ((constraintWidgetContainer.getHeight() * vertPercent) + 0.5f);
                                    measure(widget, dimensionBehaviour14, width6, dimensionBehaviour14, height6);
                                    widget.horizontalRun.dimension.resolve(widget.getWidth());
                                    widget.verticalRun.dimension.resolve(widget.getHeight());
                                    widget.measured = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void measureWidgets() {
        DimensionDependency dimensionDependency;
        Iterator<ConstraintWidget> it = this.container.mChildren.iterator();
        while (it.hasNext()) {
            ConstraintWidget widget = it.next();
            if (!widget.measured) {
                ConstraintWidget.DimensionBehaviour[] dimensionBehaviourArr = widget.mListDimensionBehaviors;
                boolean z = false;
                ConstraintWidget.DimensionBehaviour horiz = dimensionBehaviourArr[0];
                ConstraintWidget.DimensionBehaviour vert = dimensionBehaviourArr[1];
                int horizMatchConstraintsType = widget.mMatchConstraintDefaultWidth;
                int vertMatchConstraintsType = widget.mMatchConstraintDefaultHeight;
                ConstraintWidget.DimensionBehaviour dimensionBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                boolean horizWrap = horiz == dimensionBehaviour || (horiz == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT && horizMatchConstraintsType == 1);
                if (vert == dimensionBehaviour || (vert == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT && vertMatchConstraintsType == 1)) {
                    z = true;
                }
                boolean vertWrap = z;
                DimensionDependency dimensionDependency2 = widget.horizontalRun.dimension;
                boolean horizResolved = dimensionDependency2.resolved;
                DimensionDependency dimensionDependency3 = widget.verticalRun.dimension;
                boolean vertResolved = dimensionDependency3.resolved;
                if (horizResolved && vertResolved) {
                    ConstraintWidget.DimensionBehaviour dimensionBehaviour2 = ConstraintWidget.DimensionBehaviour.FIXED;
                    measure(widget, dimensionBehaviour2, dimensionDependency2.value, dimensionBehaviour2, dimensionDependency3.value);
                    widget.measured = true;
                } else if (horizResolved && vertWrap) {
                    measure(widget, ConstraintWidget.DimensionBehaviour.FIXED, dimensionDependency2.value, dimensionBehaviour, dimensionDependency3.value);
                    if (vert == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                        widget.verticalRun.dimension.wrapValue = widget.getHeight();
                    } else {
                        widget.verticalRun.dimension.resolve(widget.getHeight());
                        widget.measured = true;
                    }
                } else if (vertResolved && horizWrap) {
                    measure(widget, dimensionBehaviour, dimensionDependency2.value, ConstraintWidget.DimensionBehaviour.FIXED, dimensionDependency3.value);
                    if (horiz == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                        widget.horizontalRun.dimension.wrapValue = widget.getWidth();
                    } else {
                        widget.horizontalRun.dimension.resolve(widget.getWidth());
                        widget.measured = true;
                    }
                }
                if (widget.measured && (dimensionDependency = widget.verticalRun.baselineDimension) != null) {
                    dimensionDependency.resolve(widget.getBaselineDistance());
                }
            }
        }
    }

    public void invalidateGraph() {
        this.mNeedBuildGraph = true;
    }

    public void invalidateMeasures() {
        this.mNeedRedoMeasures = true;
    }

    public void buildGraph() {
        buildGraph(this.mRuns);
        this.mGroups.clear();
        RunGroup.index = 0;
        findGroup(this.container.horizontalRun, 0, this.mGroups);
        findGroup(this.container.verticalRun, 1, this.mGroups);
        this.mNeedBuildGraph = false;
    }

    public void buildGraph(ArrayList<WidgetRun> runs) {
        runs.clear();
        this.mContainer.horizontalRun.clear();
        this.mContainer.verticalRun.clear();
        runs.add(this.mContainer.horizontalRun);
        runs.add(this.mContainer.verticalRun);
        HashSet<ChainRun> chainRuns = null;
        Iterator<ConstraintWidget> it = this.mContainer.mChildren.iterator();
        while (it.hasNext()) {
            ConstraintWidget widget = it.next();
            if (widget instanceof Guideline) {
                runs.add(new GuidelineReference(widget));
            } else {
                if (widget.isInHorizontalChain()) {
                    if (widget.horizontalChainRun == null) {
                        ChainRun chainRun = new ChainRun(widget, 0);
                        widget.horizontalChainRun = chainRun;
                    }
                    if (chainRuns == null) {
                        chainRuns = new HashSet<>();
                    }
                    chainRuns.add(widget.horizontalChainRun);
                } else {
                    runs.add(widget.horizontalRun);
                }
                if (widget.isInVerticalChain()) {
                    if (widget.verticalChainRun == null) {
                        ChainRun chainRun2 = new ChainRun(widget, 1);
                        widget.verticalChainRun = chainRun2;
                    }
                    if (chainRuns == null) {
                        chainRuns = new HashSet<>();
                    }
                    chainRuns.add(widget.verticalChainRun);
                } else {
                    runs.add(widget.verticalRun);
                }
                if (widget instanceof HelperWidget) {
                    runs.add(new HelperReferences(widget));
                }
            }
        }
        if (chainRuns != null) {
            runs.addAll(chainRuns);
        }
        Iterator<WidgetRun> it2 = runs.iterator();
        while (it2.hasNext()) {
            it2.next().clear();
        }
        Iterator<WidgetRun> it3 = runs.iterator();
        while (it3.hasNext()) {
            WidgetRun run = it3.next();
            if (run.widget != this.mContainer) {
                run.apply();
            }
        }
    }

    private void applyGroup(DependencyNode node, int orientation, int direction, DependencyNode end, ArrayList<RunGroup> groups, RunGroup group) {
        RunGroup group2;
        WidgetRun run = node.run;
        if (run.runGroup == null) {
            ConstraintWidgetContainer constraintWidgetContainer = this.container;
            if (run != constraintWidgetContainer.horizontalRun) {
                if (run == constraintWidgetContainer.verticalRun) {
                    return;
                }
                if (group == null) {
                    RunGroup group3 = new RunGroup(run, direction);
                    groups.add(group3);
                    group2 = group3;
                } else {
                    group2 = group;
                }
                run.runGroup = group2;
                group2.add(run);
                for (Dependency dependent : run.start.dependencies) {
                    if (dependent instanceof DependencyNode) {
                        applyGroup((DependencyNode) dependent, orientation, 0, end, groups, group2);
                    }
                }
                for (Dependency dependent2 : run.end.dependencies) {
                    if (dependent2 instanceof DependencyNode) {
                        applyGroup((DependencyNode) dependent2, orientation, 1, end, groups, group2);
                    }
                }
                if (orientation == 1 && (run instanceof VerticalWidgetRun)) {
                    for (Dependency dependent3 : ((VerticalWidgetRun) run).baseline.dependencies) {
                        if (dependent3 instanceof DependencyNode) {
                            applyGroup((DependencyNode) dependent3, orientation, 2, end, groups, group2);
                        }
                    }
                }
                for (DependencyNode target : run.start.targets) {
                    if (target == end) {
                        group2.dual = true;
                    }
                    applyGroup(target, orientation, 0, end, groups, group2);
                }
                for (DependencyNode target2 : run.end.targets) {
                    if (target2 == end) {
                        group2.dual = true;
                    }
                    applyGroup(target2, orientation, 1, end, groups, group2);
                }
                if (orientation == 1 && (run instanceof VerticalWidgetRun)) {
                    Iterator<DependencyNode> it = ((VerticalWidgetRun) run).baseline.targets.iterator();
                    while (it.hasNext()) {
                        applyGroup(it.next(), orientation, 2, end, groups, group2);
                    }
                }
            }
        }
    }

    private void findGroup(WidgetRun run, int orientation, ArrayList<RunGroup> groups) {
        for (Dependency dependent : run.start.dependencies) {
            if (dependent instanceof DependencyNode) {
                DependencyNode node = (DependencyNode) dependent;
                applyGroup(node, orientation, 0, run.end, groups, null);
            } else if (dependent instanceof WidgetRun) {
                WidgetRun dependentRun = (WidgetRun) dependent;
                applyGroup(dependentRun.start, orientation, 0, run.end, groups, null);
            }
        }
        for (Dependency dependent2 : run.end.dependencies) {
            if (dependent2 instanceof DependencyNode) {
                DependencyNode node2 = (DependencyNode) dependent2;
                applyGroup(node2, orientation, 1, run.start, groups, null);
            } else if (dependent2 instanceof WidgetRun) {
                WidgetRun dependentRun2 = (WidgetRun) dependent2;
                applyGroup(dependentRun2.end, orientation, 1, run.start, groups, null);
            }
        }
        if (orientation == 1) {
            for (Dependency dependent3 : ((VerticalWidgetRun) run).baseline.dependencies) {
                if (dependent3 instanceof DependencyNode) {
                    DependencyNode node3 = (DependencyNode) dependent3;
                    applyGroup(node3, orientation, 2, null, groups, null);
                }
            }
        }
    }
}
