package org.tukaani.xz.lzma;

/* loaded from: classes.dex */
final class State {
    private int state;

    /* JADX INFO: Access modifiers changed from: package-private */
    public int get() {
        return this.state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLiteral() {
        return this.state < 7;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        this.state = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void set(State state) {
        this.state = state.state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLiteral() {
        int i = this.state;
        this.state = i <= 3 ? 0 : i <= 9 ? i - 3 : i - 6;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLongRep() {
        this.state = this.state < 7 ? 8 : 11;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateMatch() {
        this.state = this.state >= 7 ? 10 : 7;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateShortRep() {
        this.state = this.state < 7 ? 9 : 11;
    }
}
