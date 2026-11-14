package androidx.constraintlayout.solver.widgets;

import androidx.constraintlayout.solver.LinearSystem;
import androidx.constraintlayout.solver.widgets.ConstraintWidget;

/* loaded from: classes.dex */
public class Optimizer {
    static boolean[] flags = new boolean[3];

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void checkMatchParent(ConstraintWidgetContainer container, LinearSystem system, ConstraintWidget widget) {
        widget.mHorizontalResolution = -1;
        widget.mVerticalResolution = -1;
        ConstraintWidget.DimensionBehaviour dimensionBehaviour = container.mListDimensionBehaviors[0];
        ConstraintWidget.DimensionBehaviour dimensionBehaviour2 = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
        if (dimensionBehaviour != dimensionBehaviour2 && widget.mListDimensionBehaviors[0] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
            int left = widget.mLeft.mMargin;
            int right = container.getWidth() - widget.mRight.mMargin;
            ConstraintAnchor constraintAnchor = widget.mLeft;
            constraintAnchor.mSolverVariable = system.createObjectVariable(constraintAnchor);
            ConstraintAnchor constraintAnchor2 = widget.mRight;
            constraintAnchor2.mSolverVariable = system.createObjectVariable(constraintAnchor2);
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = 2;
            widget.setHorizontalDimension(left, right);
        }
        if (container.mListDimensionBehaviors[1] != dimensionBehaviour2 && widget.mListDimensionBehaviors[1] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
            int top = widget.mTop.mMargin;
            int bottom = container.getHeight() - widget.mBottom.mMargin;
            ConstraintAnchor constraintAnchor3 = widget.mTop;
            constraintAnchor3.mSolverVariable = system.createObjectVariable(constraintAnchor3);
            ConstraintAnchor constraintAnchor4 = widget.mBottom;
            constraintAnchor4.mSolverVariable = system.createObjectVariable(constraintAnchor4);
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0 || widget.getVisibility() == 8) {
                ConstraintAnchor constraintAnchor5 = widget.mBaseline;
                constraintAnchor5.mSolverVariable = system.createObjectVariable(constraintAnchor5);
                system.addEquality(widget.mBaseline.mSolverVariable, widget.mBaselineDistance + top);
            }
            widget.mVerticalResolution = 2;
            widget.setVerticalDimension(top, bottom);
        }
    }

    public static final boolean enabled(int optimizationLevel, int optimization) {
        return (optimizationLevel & optimization) == optimization;
    }
}
