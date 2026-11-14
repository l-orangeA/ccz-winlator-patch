package androidx.constraintlayout.solver;

/* loaded from: classes.dex */
public class Cache {
    Pools$Pool<ArrayRow> arrayRowPool;
    SolverVariable[] mIndexedVariables = new SolverVariable[32];
    Pools$Pool<ArrayRow> optimizedArrayRowPool;
    Pools$Pool<SolverVariable> solverVariablePool;

    public Cache() {
        final int i = 256;
        this.optimizedArrayRowPool = new Pools$Pool<T>(i) { // from class: androidx.constraintlayout.solver.Pools$SimplePool
            private final Object[] mPool;
            private int mPoolSize;

            /* JADX INFO: Access modifiers changed from: package-private */
            {
                if (i <= 0) {
                    throw new IllegalArgumentException("The max pool size must be > 0");
                }
                this.mPool = new Object[i];
            }

            @Override // androidx.constraintlayout.solver.Pools$Pool
            public T acquire() {
                int i2 = this.mPoolSize;
                if (i2 <= 0) {
                    return null;
                }
                int i3 = i2 - 1;
                Object[] objArr = this.mPool;
                T t = (T) objArr[i3];
                objArr[i3] = null;
                this.mPoolSize = i2 - 1;
                return t;
            }

            @Override // androidx.constraintlayout.solver.Pools$Pool
            public boolean release(T instance) {
                int i2 = this.mPoolSize;
                Object[] objArr = this.mPool;
                if (i2 < objArr.length) {
                    objArr[i2] = instance;
                    this.mPoolSize = i2 + 1;
                    return true;
                }
                return false;
            }

            @Override // androidx.constraintlayout.solver.Pools$Pool
            public void releaseAll(T[] variables, int count) {
                if (count > variables.length) {
                    count = variables.length;
                }
                for (int i2 = 0; i2 < count; i2++) {
                    T instance = variables[i2];
                    int i3 = this.mPoolSize;
                    Object[] objArr = this.mPool;
                    if (i3 < objArr.length) {
                        objArr[i3] = instance;
                        this.mPoolSize = i3 + 1;
                    }
                }
            }
        };
        this.arrayRowPool = new Pools$Pool<T>(i) { // from class: androidx.constraintlayout.solver.Pools$SimplePool
            private final Object[] mPool;
            private int mPoolSize;

            /* JADX INFO: Access modifiers changed from: package-private */
            {
                if (i <= 0) {
                    throw new IllegalArgumentException("The max pool size must be > 0");
                }
                this.mPool = new Object[i];
            }

            @Override // androidx.constraintlayout.solver.Pools$Pool
            public T acquire() {
                int i2 = this.mPoolSize;
                if (i2 <= 0) {
                    return null;
                }
                int i3 = i2 - 1;
                Object[] objArr = this.mPool;
                T t = (T) objArr[i3];
                objArr[i3] = null;
                this.mPoolSize = i2 - 1;
                return t;
            }

            @Override // androidx.constraintlayout.solver.Pools$Pool
            public boolean release(T instance) {
                int i2 = this.mPoolSize;
                Object[] objArr = this.mPool;
                if (i2 < objArr.length) {
                    objArr[i2] = instance;
                    this.mPoolSize = i2 + 1;
                    return true;
                }
                return false;
            }

            @Override // androidx.constraintlayout.solver.Pools$Pool
            public void releaseAll(T[] variables, int count) {
                if (count > variables.length) {
                    count = variables.length;
                }
                for (int i2 = 0; i2 < count; i2++) {
                    T instance = variables[i2];
                    int i3 = this.mPoolSize;
                    Object[] objArr = this.mPool;
                    if (i3 < objArr.length) {
                        objArr[i3] = instance;
                        this.mPoolSize = i3 + 1;
                    }
                }
            }
        };
        this.solverVariablePool = new Pools$Pool<T>(i) { // from class: androidx.constraintlayout.solver.Pools$SimplePool
            private final Object[] mPool;
            private int mPoolSize;

            /* JADX INFO: Access modifiers changed from: package-private */
            {
                if (i <= 0) {
                    throw new IllegalArgumentException("The max pool size must be > 0");
                }
                this.mPool = new Object[i];
            }

            @Override // androidx.constraintlayout.solver.Pools$Pool
            public T acquire() {
                int i2 = this.mPoolSize;
                if (i2 <= 0) {
                    return null;
                }
                int i3 = i2 - 1;
                Object[] objArr = this.mPool;
                T t = (T) objArr[i3];
                objArr[i3] = null;
                this.mPoolSize = i2 - 1;
                return t;
            }

            @Override // androidx.constraintlayout.solver.Pools$Pool
            public boolean release(T instance) {
                int i2 = this.mPoolSize;
                Object[] objArr = this.mPool;
                if (i2 < objArr.length) {
                    objArr[i2] = instance;
                    this.mPoolSize = i2 + 1;
                    return true;
                }
                return false;
            }

            @Override // androidx.constraintlayout.solver.Pools$Pool
            public void releaseAll(T[] variables, int count) {
                if (count > variables.length) {
                    count = variables.length;
                }
                for (int i2 = 0; i2 < count; i2++) {
                    T instance = variables[i2];
                    int i3 = this.mPoolSize;
                    Object[] objArr = this.mPool;
                    if (i3 < objArr.length) {
                        objArr[i3] = instance;
                        this.mPoolSize = i3 + 1;
                    }
                }
            }
        };
    }
}
