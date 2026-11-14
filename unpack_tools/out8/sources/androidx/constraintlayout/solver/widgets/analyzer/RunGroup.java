package androidx.constraintlayout.solver.widgets.analyzer;

import androidx.constraintlayout.solver.widgets.ConstraintWidgetContainer;
import java.util.ArrayList;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class RunGroup {
    public static int index;
    int direction;
    WidgetRun firstRun;
    int groupIndex;
    WidgetRun lastRun;
    public int position = 0;
    public boolean dual = false;
    ArrayList<WidgetRun> runs = new ArrayList<>();

    public RunGroup(WidgetRun run, int dir) {
        this.firstRun = null;
        this.lastRun = null;
        this.groupIndex = 0;
        int i = index;
        this.groupIndex = i;
        index = i + 1;
        this.firstRun = run;
        this.lastRun = run;
        this.direction = dir;
    }

    public void add(WidgetRun run) {
        this.runs.add(run);
        this.lastRun = run;
    }

    private long traverseStart(DependencyNode node, long startPosition) {
        WidgetRun run = node.run;
        if (run instanceof HelperReferences) {
            return startPosition;
        }
        long position = startPosition;
        int count = node.dependencies.size();
        for (int i = 0; i < count; i++) {
            Dependency dependency = node.dependencies.get(i);
            if (dependency instanceof DependencyNode) {
                DependencyNode nextNode = (DependencyNode) dependency;
                if (nextNode.run != run) {
                    position = Math.max(position, traverseStart(nextNode, nextNode.margin + startPosition));
                }
            }
        }
        if (node == run.start) {
            long dimension = run.getWrapDimension();
            return Math.max(Math.max(position, traverseStart(run.end, startPosition + dimension)), (startPosition + dimension) - run.end.margin);
        }
        return position;
    }

    private long traverseEnd(DependencyNode node, long startPosition) {
        WidgetRun run = node.run;
        if (run instanceof HelperReferences) {
            return startPosition;
        }
        long position = startPosition;
        int count = node.dependencies.size();
        for (int i = 0; i < count; i++) {
            Dependency dependency = node.dependencies.get(i);
            if (dependency instanceof DependencyNode) {
                DependencyNode nextNode = (DependencyNode) dependency;
                if (nextNode.run != run) {
                    position = Math.min(position, traverseEnd(nextNode, nextNode.margin + startPosition));
                }
            }
        }
        if (node == run.end) {
            long dimension = run.getWrapDimension();
            return Math.min(Math.min(position, traverseEnd(run.start, startPosition - dimension)), (startPosition - dimension) - run.start.margin);
        }
        return position;
    }

    public long computeWrapSize(ConstraintWidgetContainer container, int orientation) {
        WidgetRun widgetRun = this.firstRun;
        if (widgetRun instanceof ChainRun) {
            ChainRun chainRun = (ChainRun) widgetRun;
            if (chainRun.orientation != orientation) {
                return 0L;
            }
        } else if (orientation == 0) {
            if (!(widgetRun instanceof HorizontalWidgetRun)) {
                return 0L;
            }
        } else if (!(widgetRun instanceof VerticalWidgetRun)) {
            return 0L;
        }
        DependencyNode containerStart = (orientation == 0 ? container.horizontalRun : container.verticalRun).start;
        DependencyNode containerEnd = (orientation == 0 ? container.horizontalRun : container.verticalRun).end;
        boolean runWithStartTarget = widgetRun.start.targets.contains(containerStart);
        boolean runWithEndTarget = this.firstRun.end.targets.contains(containerEnd);
        long dimension = this.firstRun.getWrapDimension();
        if (runWithStartTarget && runWithEndTarget) {
            long maxPosition = traverseStart(this.firstRun.start, 0L);
            long minPosition = traverseEnd(this.firstRun.end, 0L);
            long endGap = maxPosition - dimension;
            WidgetRun widgetRun2 = this.firstRun;
            int i = widgetRun2.end.margin;
            long maxPosition2 = -i;
            if (endGap >= maxPosition2) {
                endGap += i;
            }
            int i2 = widgetRun2.start.margin;
            long startGap = ((-minPosition) - dimension) - i2;
            if (startGap >= i2) {
                startGap -= i2;
            }
            float bias = widgetRun2.widget.getBiasPercent(orientation);
            long gap = 0;
            if (bias > 0.0f) {
                gap = (((float) startGap) / bias) + (((float) endGap) / (1.0f - bias));
            }
            long runDimension = (((float) gap) * bias) + 0.5f + dimension + (((float) gap) * (1.0f - bias)) + 0.5f;
            long gap2 = this.firstRun.start.margin;
            return (gap2 + runDimension) - r2.end.margin;
        }
        if (runWithStartTarget) {
            long maxPosition3 = traverseStart(this.firstRun.start, r1.margin);
            long runDimension2 = this.firstRun.start.margin + dimension;
            return Math.max(maxPosition3, runDimension2);
        }
        if (runWithEndTarget) {
            long minPosition2 = traverseEnd(this.firstRun.end, r1.margin);
            long runDimension3 = (-this.firstRun.end.margin) + dimension;
            return Math.max(-minPosition2, runDimension3);
        }
        return (r1.start.margin + this.firstRun.getWrapDimension()) - this.firstRun.end.margin;
    }
}
