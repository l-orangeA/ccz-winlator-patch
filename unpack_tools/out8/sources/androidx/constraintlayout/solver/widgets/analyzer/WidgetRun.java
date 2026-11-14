package androidx.constraintlayout.solver.widgets.analyzer;

import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
import androidx.constraintlayout.solver.widgets.ConstraintWidget;

/* loaded from: classes.dex */
public abstract class WidgetRun implements Dependency {
    protected ConstraintWidget.DimensionBehaviour dimensionBehavior;
    public int matchConstraintsType;
    RunGroup runGroup;
    ConstraintWidget widget;
    DimensionDependency dimension = new DimensionDependency(this);
    public int orientation = 0;
    boolean resolved = false;
    public DependencyNode start = new DependencyNode(this);
    public DependencyNode end = new DependencyNode(this);
    protected RunType mRunType = RunType.NONE;

    /* loaded from: classes.dex */
    enum RunType {
        NONE,
        START,
        END,
        CENTER
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void apply();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void applyToWidget();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void clear();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean supportsWrapComputation();

    public WidgetRun(ConstraintWidget widget) {
        this.widget = widget;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final DependencyNode getTarget(ConstraintAnchor anchor) {
        ConstraintAnchor constraintAnchor = anchor.mTarget;
        if (constraintAnchor == null) {
            return null;
        }
        ConstraintWidget targetWidget = constraintAnchor.mOwner;
        ConstraintAnchor.Type targetType = constraintAnchor.mType;
        switch (AnonymousClass1.$SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[targetType.ordinal()]) {
            case 1:
                HorizontalWidgetRun run = targetWidget.horizontalRun;
                DependencyNode target = run.start;
                return target;
            case 2:
                HorizontalWidgetRun run2 = targetWidget.horizontalRun;
                DependencyNode target2 = run2.end;
                return target2;
            case 3:
                VerticalWidgetRun run3 = targetWidget.verticalRun;
                DependencyNode target3 = run3.start;
                return target3;
            case 4:
                VerticalWidgetRun run4 = targetWidget.verticalRun;
                DependencyNode target4 = run4.baseline;
                return target4;
            case 5:
                VerticalWidgetRun run5 = targetWidget.verticalRun;
                DependencyNode target5 = run5.end;
                return target5;
            default:
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: androidx.constraintlayout.solver.widgets.analyzer.WidgetRun$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type;

        static {
            int[] iArr = new int[ConstraintAnchor.Type.values().length];
            $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type = iArr;
            try {
                iArr[ConstraintAnchor.Type.LEFT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.RIGHT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.TOP.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.BASELINE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.BOTTOM.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateRunCenter(Dependency dependency, ConstraintAnchor startAnchor, ConstraintAnchor endAnchor, int orientation) {
        DependencyNode startTarget = getTarget(startAnchor);
        DependencyNode endTarget = getTarget(endAnchor);
        if (!startTarget.resolved || !endTarget.resolved) {
            return;
        }
        int startPos = startTarget.value + startAnchor.getMargin();
        int endPos = endTarget.value - endAnchor.getMargin();
        int distance = endPos - startPos;
        if (!this.dimension.resolved && this.dimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            resolveDimension(orientation, distance);
        }
        DimensionDependency dimensionDependency = this.dimension;
        if (!dimensionDependency.resolved) {
            return;
        }
        if (dimensionDependency.value == distance) {
            this.start.resolve(startPos);
            this.end.resolve(endPos);
            return;
        }
        ConstraintWidget constraintWidget = this.widget;
        float bias = orientation == 0 ? constraintWidget.getHorizontalBiasPercent() : constraintWidget.getVerticalBiasPercent();
        if (startTarget == endTarget) {
            startPos = startTarget.value;
            endPos = endTarget.value;
            bias = 0.5f;
        }
        int availableDistance = (endPos - startPos) - this.dimension.value;
        this.start.resolve((int) (startPos + 0.5f + (availableDistance * bias)));
        this.end.resolve(this.start.value + this.dimension.value);
    }

    private void resolveDimension(int orientation, int distance) {
        int value;
        switch (this.matchConstraintsType) {
            case 0:
                this.dimension.resolve(getLimitedDimension(distance, orientation));
                return;
            case 1:
                int wrapValue = getLimitedDimension(this.dimension.wrapValue, orientation);
                this.dimension.resolve(Math.min(wrapValue, distance));
                return;
            case 2:
                ConstraintWidget parent = this.widget.getParent();
                if (parent != null) {
                    DimensionDependency dimensionDependency = (orientation == 0 ? parent.horizontalRun : parent.verticalRun).dimension;
                    if (dimensionDependency.resolved) {
                        ConstraintWidget constraintWidget = this.widget;
                        float percent = orientation == 0 ? constraintWidget.mMatchConstraintPercentWidth : constraintWidget.mMatchConstraintPercentHeight;
                        int targetDimensionValue = dimensionDependency.value;
                        int size = (int) ((targetDimensionValue * percent) + 0.5f);
                        this.dimension.resolve(getLimitedDimension(size, orientation));
                        return;
                    }
                    return;
                }
                return;
            case 3:
                ConstraintWidget constraintWidget2 = this.widget;
                WidgetRun run = constraintWidget2.horizontalRun;
                ConstraintWidget.DimensionBehaviour dimensionBehaviour = run.dimensionBehavior;
                ConstraintWidget.DimensionBehaviour dimensionBehaviour2 = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                if (dimensionBehaviour == dimensionBehaviour2 && run.matchConstraintsType == 3) {
                    VerticalWidgetRun verticalWidgetRun = constraintWidget2.verticalRun;
                    if (verticalWidgetRun.dimensionBehavior == dimensionBehaviour2 && verticalWidgetRun.matchConstraintsType == 3) {
                        return;
                    }
                }
                if (orientation == 0) {
                    run = constraintWidget2.verticalRun;
                }
                if (run.dimension.resolved) {
                    float ratio = constraintWidget2.getDimensionRatio();
                    if (orientation == 1) {
                        value = (int) ((run.dimension.value / ratio) + 0.5f);
                    } else {
                        value = (int) ((run.dimension.value * ratio) + 0.5f);
                    }
                    this.dimension.resolve(value);
                    return;
                }
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateRunStart(Dependency dependency) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateRunEnd(Dependency dependency) {
    }

    @Override // androidx.constraintlayout.solver.widgets.analyzer.Dependency
    public void update(Dependency dependency) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final int getLimitedDimension(int dimension, int orientation) {
        if (orientation == 0) {
            ConstraintWidget constraintWidget = this.widget;
            int max = constraintWidget.mMatchConstraintMaxWidth;
            int min = constraintWidget.mMatchConstraintMinWidth;
            int value = Math.max(min, dimension);
            if (max > 0) {
                value = Math.min(max, dimension);
            }
            if (value != dimension) {
                return value;
            }
            return dimension;
        }
        ConstraintWidget constraintWidget2 = this.widget;
        int max2 = constraintWidget2.mMatchConstraintMaxHeight;
        int min2 = constraintWidget2.mMatchConstraintMinHeight;
        int value2 = Math.max(min2, dimension);
        if (max2 > 0) {
            value2 = Math.min(max2, dimension);
        }
        if (value2 != dimension) {
            return value2;
        }
        return dimension;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final DependencyNode getTarget(ConstraintAnchor anchor, int orientation) {
        ConstraintAnchor constraintAnchor = anchor.mTarget;
        if (constraintAnchor == null) {
            return null;
        }
        ConstraintWidget targetWidget = constraintAnchor.mOwner;
        WidgetRun run = orientation == 0 ? targetWidget.horizontalRun : targetWidget.verticalRun;
        ConstraintAnchor.Type targetType = constraintAnchor.mType;
        switch (AnonymousClass1.$SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[targetType.ordinal()]) {
            case 1:
            case 3:
                DependencyNode target = run.start;
                return target;
            case 2:
            case 5:
                DependencyNode target2 = run.end;
                return target2;
            case 4:
            default:
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void addTarget(DependencyNode node, DependencyNode target, int margin) {
        node.targets.add(target);
        node.margin = margin;
        target.dependencies.add(node);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void addTarget(DependencyNode node, DependencyNode target, int marginFactor, DimensionDependency dimensionDependency) {
        node.targets.add(target);
        node.targets.add(this.dimension);
        node.marginFactor = marginFactor;
        node.marginDependency = dimensionDependency;
        target.dependencies.add(node);
        dimensionDependency.dependencies.add(node);
    }

    public long getWrapDimension() {
        if (this.dimension.resolved) {
            return r0.value;
        }
        return 0L;
    }

    public boolean isResolved() {
        return this.resolved;
    }
}
