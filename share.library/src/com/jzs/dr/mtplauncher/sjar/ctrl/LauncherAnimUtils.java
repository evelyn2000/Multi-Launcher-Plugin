/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jzs.dr.mtplauncher.sjar.ctrl;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;

import com.jzs.dr.mtplauncher.sjar.utils.Util;

import java.util.HashSet;

public class LauncherAnimUtils {
    private final static String TAG = "AnimUtils";
    public static HashSet<Animator> sAnimators = new HashSet<Animator>();
    public static Animator.AnimatorListener sEndAnimListener = new Animator.AnimatorListener() {
        public void onAnimationStart(Animator animation) {
            if (Util.DEBUG_ANIM) {
                Util.Log.e(TAG, "onAnimationStart(1) animation = " + animation);
            }
        }

        public void onAnimationRepeat(Animator animation) {
            if (Util.DEBUG_ANIM) {
                Util.Log.e(TAG, "onAnimationRepeat(1) animation = " + animation);
            }
        }

        public void onAnimationEnd(Animator animation) {
            if (Util.DEBUG_ANIM) {
                Util.Log.e(TAG, "onAnimationEnd(1) animation = " + animation);
            }
            sAnimators.remove(animation);
        }

        public void onAnimationCancel(Animator animation) {
            if (Util.DEBUG_ANIM) {
                Util.Log.e(TAG, "onAnimationCancel(1) animation = " + animation);
            }
            sAnimators.remove(animation);
        }
    };

    public static void cancelOnDestroyActivity(Animator a) {
        sAnimators.add(a);
        a.addListener(sEndAnimListener);
    }

    public static void onDestroyActivity() {
        HashSet<Animator> animators = new HashSet<Animator>(sAnimators);
        for (Animator a : animators) {
            if (a.isRunning()) {
                a.cancel();
            } else {
                sAnimators.remove(a);
            }
        }
    }

    public static AnimatorSet createAnimatorSet() {
        AnimatorSet anim = new AnimatorSet();
        cancelOnDestroyActivity(anim);
        return anim;
    }

    public static ValueAnimator ofFloat(float... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setFloatValues(values);
        cancelOnDestroyActivity(anim);
        return anim;
    }

    public static ObjectAnimator ofFloat(Object target, String propertyName, float... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setPropertyName(propertyName);
        anim.setFloatValues(values);
        cancelOnDestroyActivity(anim);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(Object target,
            PropertyValuesHolder... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        cancelOnDestroyActivity(anim);
        return anim;
    }
}
