package androidx.constraintlayout.solver.widgets.analyzer;

import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
import androidx.constraintlayout.solver.widgets.ConstraintWidget;
import androidx.constraintlayout.solver.widgets.Helper;
import androidx.constraintlayout.solver.widgets.analyzer.DependencyNode;
import androidx.constraintlayout.solver.widgets.analyzer.WidgetRun;

/* loaded from: classes.dex */
public class VerticalWidgetRun extends WidgetRun {
    public DependencyNode baseline;
    DimensionDependency baselineDimension;

    public VerticalWidgetRun(ConstraintWidget widget) {
        super(widget);
        DependencyNode dependencyNode = new DependencyNode(this);
        this.baseline = dependencyNode;
        this.baselineDimension = null;
        this.start.type = DependencyNode.Type.TOP;
        this.end.type = DependencyNode.Type.BOTTOM;
        dependencyNode.type = DependencyNode.Type.BASELINE;
        this.orientation = 1;
    }

    public String toString() {
        return "VerticalRun " + this.widget.getDebugName();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // androidx.constraintlayout.solver.widgets.analyzer.WidgetRun
    public void clear() {
        this.runGroup = null;
        this.start.clear();
        this.end.clear();
        this.baseline.clear();
        this.dimension.clear();
        this.resolved = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        this.resolved = false;
        this.start.clear();
        this.start.resolved = false;
        this.end.clear();
        this.end.resolved = false;
        this.baseline.clear();
        this.baseline.resolved = false;
        this.dimension.resolved = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // androidx.constraintlayout.solver.widgets.analyzer.WidgetRun
    public boolean supportsWrapComputation() {
        return this.dimensionBehavior != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT || this.widget.mMatchConstraintDefaultHeight == 0;
    }

    /* renamed from: androidx.constraintlayout.solver.widgets.analyzer.VerticalWidgetRun$1, reason: invalid class name */
    /* loaded from: classes.dex */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$androidx$constraintlayout$solver$widgets$analyzer$WidgetRun$RunType;

        static {
            int[] iArr = new int[WidgetRun.RunType.values().length];
            $SwitchMap$androidx$constraintlayout$solver$widgets$analyzer$WidgetRun$RunType = iArr;
            try {
                iArr[WidgetRun.RunType.START.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$analyzer$WidgetRun$RunType[WidgetRun.RunType.END.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$analyzer$WidgetRun$RunType[WidgetRun.RunType.CENTER.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    @Override // androidx.constraintlayout.solver.widgets.analyzer.WidgetRun, androidx.constraintlayout.solver.widgets.analyzer.Dependency
    public void update(Dependency dependency) {
        switch (AnonymousClass1.$SwitchMap$androidx$constraintlayout$solver$widgets$analyzer$WidgetRun$RunType[this.mRunType.ordinal()]) {
            case 1:
                updateRunStart(dependency);
                break;
            case 2:
                updateRunEnd(dependency);
                break;
            case 3:
                ConstraintWidget constraintWidget = this.widget;
                updateRunCenter(dependency, constraintWidget.mTop, constraintWidget.mBottom, 1);
                return;
        }
        DimensionDependency dimensionDependency = this.dimension;
        if (dimensionDependency.readyToSolve && !dimensionDependency.resolved && this.dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            ConstraintWidget constraintWidget2 = this.widget;
            switch (constraintWidget2.mMatchConstraintDefaultHeight) {
                case 2:
                    ConstraintWidget parent = constraintWidget2.getParent();
                    if (parent != null) {
                        DimensionDependency dimensionDependency2 = parent.verticalRun.dimension;
                        if (dimensionDependency2.resolved) {
                            float percent = this.widget.mMatchConstraintPercentHeight;
                            int targetDimensionValue = dimensionDependency2.value;
                            int size = (int) ((targetDimensionValue * percent) + 0.5f);
                            this.dimension.resolve(size);
                            break;
                        }
                    }
                    break;
                case 3:
                    if (constraintWidget2.horizontalRun.dimension.resolved) {
                        int size2 = 0;
                        int ratioSide = constraintWidget2.getDimensionRatioSide();
                        switch (ratioSide) {
                            case -1:
                                size2 = (int) ((r4.horizontalRun.dimension.value / this.widget.getDimensionRatio()) + 0.5f);
                                break;
                            case 0:
                                size2 = (int) ((r4.horizontalRun.dimension.value * this.widget.getDimensionRatio()) + 0.5f);
                                break;
                            case 1:
                                size2 = (int) ((r4.horizontalRun.dimension.value / this.widget.getDimensionRatio()) + 0.5f);
                                break;
                        }
                        this.dimension.resolve(size2);
                        break;
                    }
                    break;
            }
        }
        DependencyNode dependencyNode = this.start;
        if (dependencyNode.readyToSolve) {
            DependencyNode dependencyNode2 = this.end;
            if (!dependencyNode2.readyToSolve) {
                return;
            }
            if (dependencyNode.resolved && dependencyNode2.resolved && this.dimension.resolved) {
                return;
            }
            if (!this.dimension.resolved && this.dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                ConstraintWidget constraintWidget3 = this.widget;
                if (constraintWidget3.mMatchConstraintDefaultWidth == 0 && !constraintWidget3.isInVerticalChain()) {
                    DependencyNode startTarget = this.start.targets.get(0);
                    DependencyNode endTarget = this.end.targets.get(0);
                    int i = startTarget.value;
                    DependencyNode dependencyNode3 = this.start;
                    int startPos = i + dependencyNode3.margin;
                    int endPos = endTarget.value + this.end.margin;
                    int distance = endPos - startPos;
                    dependencyNode3.resolve(startPos);
                    this.end.resolve(endPos);
                    this.dimension.resolve(distance);
                    return;
                }
            }
            DependencyNode startTarget2 = this.dimension;
            if (!startTarget2.resolved && this.dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT && this.matchConstraintsType == 1 && this.start.targets.size() > 0 && this.end.targets.size() > 0) {
                DependencyNode startTarget3 = this.start.targets.get(0);
                DependencyNode endTarget2 = this.end.targets.get(0);
                int startPos2 = startTarget3.value + this.start.margin;
                int endPos2 = endTarget2.value + this.end.margin;
                int availableSpace = endPos2 - startPos2;
                DimensionDependency dimensionDependency3 = this.dimension;
                int i2 = dimensionDependency3.wrapValue;
                if (availableSpace < i2) {
                    dimensionDependency3.resolve(availableSpace);
                } else {
                    dimensionDependency3.resolve(i2);
                }
            }
            DependencyNode startTarget4 = this.dimension;
            if (startTarget4.resolved && this.start.targets.size() > 0 && this.end.targets.size() > 0) {
                DependencyNode startTarget5 = this.start.targets.get(0);
                DependencyNode endTarget3 = this.end.targets.get(0);
                int startPos3 = startTarget5.value + this.start.margin;
                int endPos3 = endTarget3.value + this.end.margin;
                float bias = this.widget.getVerticalBiasPercent();
                if (startTarget5 == endTarget3) {
                    startPos3 = startTarget5.value;
                    endPos3 = endTarget3.value;
                    bias = 0.5f;
                }
                int distance2 = (endPos3 - startPos3) - this.dimension.value;
                this.start.resolve((int) (startPos3 + 0.5f + (distance2 * bias)));
                this.end.resolve(this.start.value + this.dimension.value);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // androidx.constraintlayout.solver.widgets.analyzer.WidgetRun
    public void apply() {
        ConstraintWidget parent;
        ConstraintWidget parent2;
        ConstraintWidget constraintWidget = this.widget;
        if (constraintWidget.measured) {
            this.dimension.resolve(constraintWidget.getHeight());
        }
        if (!this.dimension.resolved) {
            this.dimensionBehavior = this.widget.getVerticalDimensionBehaviour();
            if (this.widget.hasBaseline()) {
                this.baselineDimension = new BaselineDimensionDependency(this);
            }
            ConstraintWidget.DimensionBehaviour dimensionBehaviour = this.dimensionBehavior;
            if (dimensionBehaviour != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                if (dimensionBehaviour == ConstraintWidget.DimensionBehaviour.MATCH_PARENT && (parent2 = this.widget.getParent()) != null && parent2.getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.FIXED) {
                    int resolvedDimension = (parent2.getHeight() - this.widget.mTop.getMargin()) - this.widget.mBottom.getMargin();
                    addTarget(this.start, parent2.verticalRun.start, this.widget.mTop.getMargin());
                    addTarget(this.end, parent2.verticalRun.end, -this.widget.mBottom.getMargin());
                    this.dimension.resolve(resolvedDimension);
                    return;
                }
                if (this.dimensionBehavior == ConstraintWidget.DimensionBehaviour.FIXED) {
                    this.dimension.resolve(this.widget.getHeight());
                }
            }
        } else if (this.dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_PARENT && (parent = this.widget.getParent()) != null && parent.getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.FIXED) {
            addTarget(this.start, parent.verticalRun.start, this.widget.mTop.getMargin());
            addTarget(this.end, parent.verticalRun.end, -this.widget.mBottom.getMargin());
            return;
        }
        DimensionDependency dimensionDependency = this.dimension;
        boolean z = dimensionDependency.resolved;
        if (z) {
            ConstraintWidget constraintWidget2 = this.widget;
            if (constraintWidget2.measured) {
                ConstraintAnchor[] constraintAnchorArr = constraintWidget2.mListAnchors;
                if (constraintAnchorArr[2].mTarget != null && constraintAnchorArr[3].mTarget != null) {
                    if (constraintWidget2.isInVerticalChain()) {
                        this.start.margin = this.widget.mListAnchors[2].getMargin();
                        this.end.margin = -this.widget.mListAnchors[3].getMargin();
                    } else {
                        DependencyNode startTarget = getTarget(this.widget.mListAnchors[2]);
                        if (startTarget != null) {
                            addTarget(this.start, startTarget, this.widget.mListAnchors[2].getMargin());
                        }
                        DependencyNode endTarget = getTarget(this.widget.mListAnchors[3]);
                        if (endTarget != null) {
                            addTarget(this.end, endTarget, -this.widget.mListAnchors[3].getMargin());
                        }
                        this.start.delegateToWidgetRun = true;
                        this.end.delegateToWidgetRun = true;
                    }
                    if (this.widget.hasBaseline()) {
                        addTarget(this.baseline, this.start, this.widget.getBaselineDistance());
                        return;
                    }
                    return;
                }
                if (constraintAnchorArr[2].mTarget != null) {
                    DependencyNode target = getTarget(constraintAnchorArr[2]);
                    if (target != null) {
                        addTarget(this.start, target, this.widget.mListAnchors[2].getMargin());
                        addTarget(this.end, this.start, this.dimension.value);
                        if (this.widget.hasBaseline()) {
                            addTarget(this.baseline, this.start, this.widget.getBaselineDistance());
                            return;
                        }
                        return;
                    }
                    return;
                }
                if (constraintAnchorArr[3].mTarget != null) {
                    DependencyNode target2 = getTarget(constraintAnchorArr[3]);
                    if (target2 != null) {
                        addTarget(this.end, target2, -this.widget.mListAnchors[3].getMargin());
                        addTarget(this.start, this.end, -this.dimension.value);
                    }
                    if (this.widget.hasBaseline()) {
                        addTarget(this.baseline, this.start, this.widget.getBaselineDistance());
                        return;
                    }
                    return;
                }
                if (constraintAnchorArr[4].mTarget != null) {
                    DependencyNode target3 = getTarget(constraintAnchorArr[4]);
                    if (target3 != null) {
                        addTarget(this.baseline, target3, 0);
                        addTarget(this.start, this.baseline, -this.widget.getBaselineDistance());
                        addTarget(this.end, this.start, this.dimension.value);
                        return;
                    }
                    return;
                }
                if (!(constraintWidget2 instanceof Helper) && constraintWidget2.getParent() != null && this.widget.getAnchor(ConstraintAnchor.Type.CENTER).mTarget == null) {
                    DependencyNode top = this.widget.getParent().verticalRun.start;
                    addTarget(this.start, top, this.widget.getY());
                    addTarget(this.end, this.start, this.dimension.value);
                    if (this.widget.hasBaseline()) {
                        addTarget(this.baseline, this.start, this.widget.getBaselineDistance());
                        return;
                    }
                    return;
                }
                return;
            }
        }
        if (!z && this.dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            ConstraintWidget constraintWidget3 = this.widget;
            switch (constraintWidget3.mMatchConstraintDefaultHeight) {
                case 2:
                    ConstraintWidget parent3 = constraintWidget3.getParent();
                    if (parent3 != null) {
                        DependencyNode targetDimension = parent3.verticalRun.dimension;
                        this.dimension.targets.add(targetDimension);
                        targetDimension.dependencies.add(this.dimension);
                        DimensionDependency dimensionDependency2 = this.dimension;
                        dimensionDependency2.delegateToWidgetRun = true;
                        dimensionDependency2.dependencies.add(this.start);
                        this.dimension.dependencies.add(this.end);
                        break;
                    }
                    break;
                case 3:
                    if (!constraintWidget3.isInVerticalChain()) {
                        ConstraintWidget constraintWidget4 = this.widget;
                        if (constraintWidget4.mMatchConstraintDefaultWidth != 3) {
                            DependencyNode targetDimension2 = constraintWidget4.horizontalRun.dimension;
                            this.dimension.targets.add(targetDimension2);
                            targetDimension2.dependencies.add(this.dimension);
                            DimensionDependency dimensionDependency3 = this.dimension;
                            dimensionDependency3.delegateToWidgetRun = true;
                            dimensionDependency3.dependencies.add(this.start);
                            this.dimension.dependencies.add(this.end);
                            break;
                        }
                    }
                    break;
            }
        } else {
            dimensionDependency.addDependency(this);
        }
        ConstraintWidget constraintWidget5 = this.widget;
        ConstraintAnchor[] constraintAnchorArr2 = constraintWidget5.mListAnchors;
        if (constraintAnchorArr2[2].mTarget != null && constraintAnchorArr2[3].mTarget != null) {
            if (constraintWidget5.isInVerticalChain()) {
                this.start.margin = this.widget.mListAnchors[2].getMargin();
                this.end.margin = -this.widget.mListAnchors[3].getMargin();
            } else {
                DependencyNode startTarget2 = getTarget(this.widget.mListAnchors[2]);
                DependencyNode endTarget2 = getTarget(this.widget.mListAnchors[3]);
                startTarget2.addDependency(this);
                endTarget2.addDependency(this);
                this.mRunType = WidgetRun.RunType.CENTER;
            }
            if (this.widget.hasBaseline()) {
                addTarget(this.baseline, this.start, 1, this.baselineDimension);
            }
        } else if (constraintAnchorArr2[2].mTarget != null) {
            DependencyNode target4 = getTarget(constraintAnchorArr2[2]);
            if (target4 != null) {
                addTarget(this.start, target4, this.widget.mListAnchors[2].getMargin());
                addTarget(this.end, this.start, 1, this.dimension);
                if (this.widget.hasBaseline()) {
                    addTarget(this.baseline, this.start, 1, this.baselineDimension);
                }
                ConstraintWidget.DimensionBehaviour dimensionBehaviour2 = this.dimensionBehavior;
                ConstraintWidget.DimensionBehaviour dimensionBehaviour3 = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                if (dimensionBehaviour2 == dimensionBehaviour3 && this.widget.getDimensionRatio() > 0.0f) {
                    HorizontalWidgetRun horizontalWidgetRun = this.widget.horizontalRun;
                    if (horizontalWidgetRun.dimensionBehavior == dimensionBehaviour3) {
                        horizontalWidgetRun.dimension.dependencies.add(this.dimension);
                        this.dimension.targets.add(this.widget.horizontalRun.dimension);
                        this.dimension.updateDelegate = this;
                    }
                }
            }
        } else if (constraintAnchorArr2[3].mTarget != null) {
            DependencyNode target5 = getTarget(constraintAnchorArr2[3]);
            if (target5 != null) {
                addTarget(this.end, target5, -this.widget.mListAnchors[3].getMargin());
                addTarget(this.start, this.end, -1, this.dimension);
                if (this.widget.hasBaseline()) {
                    addTarget(this.baseline, this.start, 1, this.baselineDimension);
                }
            }
        } else if (constraintAnchorArr2[4].mTarget != null) {
            DependencyNode target6 = getTarget(constraintAnchorArr2[4]);
            if (target6 != null) {
                addTarget(this.baseline, target6, 0);
                addTarget(this.start, this.baseline, -1, this.baselineDimension);
                addTarget(this.end, this.start, 1, this.dimension);
            }
        } else if (!(constraintWidget5 instanceof Helper) && constraintWidget5.getParent() != null) {
            DependencyNode top2 = this.widget.getParent().verticalRun.start;
            addTarget(this.start, top2, this.widget.getY());
            addTarget(this.end, this.start, 1, this.dimension);
            if (this.widget.hasBaseline()) {
                addTarget(this.baseline, this.start, 1, this.baselineDimension);
            }
            ConstraintWidget.DimensionBehaviour dimensionBehaviour4 = this.dimensionBehavior;
            ConstraintWidget.DimensionBehaviour dimensionBehaviour5 = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
            if (dimensionBehaviour4 == dimensionBehaviour5 && this.widget.getDimensionRatio() > 0.0f) {
                HorizontalWidgetRun horizontalWidgetRun2 = this.widget.horizontalRun;
                if (horizontalWidgetRun2.dimensionBehavior == dimensionBehaviour5) {
                    horizontalWidgetRun2.dimension.dependencies.add(this.dimension);
                    this.dimension.targets.add(this.widget.horizontalRun.dimension);
                    this.dimension.updateDelegate = this;
                }
            }
        }
        DependencyNode top3 = this.dimension;
        if (top3.targets.size() == 0) {
            this.dimension.readyToSolve = true;
        }
    }

    @Override // androidx.constraintlayout.solver.widgets.analyzer.WidgetRun
    public void applyToWidget() {
        DependencyNode dependencyNode = this.start;
        if (dependencyNode.resolved) {
            this.widget.setY(dependencyNode.value);
        }
    }
}
