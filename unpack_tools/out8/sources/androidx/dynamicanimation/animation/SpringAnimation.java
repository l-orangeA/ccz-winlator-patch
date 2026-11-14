package androidx.dynamicanimation.animation;

import androidx.dynamicanimation.animation.DynamicAnimation;

/* loaded from: classes.dex */
public final class SpringAnimation extends DynamicAnimation<SpringAnimation> {
    private boolean mEndRequested;
    private float mPendingPosition;
    private SpringForce mSpring;

    public <K> SpringAnimation(K object, FloatPropertyCompat<K> property) {
        super(object, property);
        this.mSpring = null;
        this.mPendingPosition = Float.MAX_VALUE;
        this.mEndRequested = false;
    }

    public SpringAnimation setSpring(SpringForce force) {
        this.mSpring = force;
        return this;
    }

    @Override // androidx.dynamicanimation.animation.DynamicAnimation
    public void start() {
        sanityCheck();
        this.mSpring.setValueThreshold(getValueThreshold());
        super.start();
    }

    public void animateToFinalPosition(float finalPosition) {
        if (isRunning()) {
            this.mPendingPosition = finalPosition;
            return;
        }
        if (this.mSpring == null) {
            this.mSpring = new SpringForce(finalPosition);
        }
        this.mSpring.setFinalPosition(finalPosition);
        start();
    }

    private void sanityCheck() {
        SpringForce springForce = this.mSpring;
        if (springForce == null) {
            throw new UnsupportedOperationException("Incomplete SpringAnimation: Either final position or a spring force needs to be set.");
        }
        double finalPosition = springForce.getFinalPosition();
        if (finalPosition > this.mMaxValue) {
            throw new UnsupportedOperationException("Final position of the spring cannot be greater than the max value.");
        }
        if (finalPosition < this.mMinValue) {
            throw new UnsupportedOperationException("Final position of the spring cannot be less than the min value.");
        }
    }

    @Override // androidx.dynamicanimation.animation.DynamicAnimation
    boolean updateValueAndVelocity(long deltaT) {
        if (!this.mEndRequested) {
            if (this.mPendingPosition == Float.MAX_VALUE) {
                DynamicAnimation.MassState massState = this.mSpring.updateValues(this.mValue, this.mVelocity, deltaT);
                this.mValue = massState.mValue;
                this.mVelocity = massState.mVelocity;
            } else {
                this.mSpring.getFinalPosition();
                DynamicAnimation.MassState massState2 = this.mSpring.updateValues(this.mValue, this.mVelocity, deltaT / 2);
                this.mSpring.setFinalPosition(this.mPendingPosition);
                this.mPendingPosition = Float.MAX_VALUE;
                DynamicAnimation.MassState massState3 = this.mSpring.updateValues(massState2.mValue, massState2.mVelocity, deltaT / 2);
                this.mValue = massState3.mValue;
                this.mVelocity = massState3.mVelocity;
            }
            float max = Math.max(this.mValue, this.mMinValue);
            this.mValue = max;
            float min = Math.min(max, this.mMaxValue);
            this.mValue = min;
            if (isAtEquilibrium(min, this.mVelocity)) {
                this.mValue = this.mSpring.getFinalPosition();
                this.mVelocity = 0.0f;
                return true;
            }
            return false;
        }
        float f = this.mPendingPosition;
        if (f != Float.MAX_VALUE) {
            this.mSpring.setFinalPosition(f);
            this.mPendingPosition = Float.MAX_VALUE;
        }
        this.mValue = this.mSpring.getFinalPosition();
        this.mVelocity = 0.0f;
        this.mEndRequested = false;
        return true;
    }

    boolean isAtEquilibrium(float value, float velocity) {
        return this.mSpring.isAtEquilibrium(value, velocity);
    }
}
