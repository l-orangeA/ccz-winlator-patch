package androidx.transition;

import android.view.ViewGroup;

/* loaded from: classes.dex */
public class Scene {
    private Runnable mExitAction;
    private ViewGroup mSceneRoot;

    public void exit() {
        Runnable runnable;
        if (getCurrentScene(this.mSceneRoot) == this && (runnable = this.mExitAction) != null) {
            runnable.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setCurrentScene(ViewGroup sceneRoot, Scene scene) {
        sceneRoot.setTag(R$id.transition_current_scene, scene);
    }

    public static Scene getCurrentScene(ViewGroup sceneRoot) {
        return (Scene) sceneRoot.getTag(R$id.transition_current_scene);
    }
}
