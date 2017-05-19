/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
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

package master.flame.danmaku.danmaku.model;



public class R2LDanmaku extends BaseDanmaku {
    
    protected static final long MAX_RENDERING_TIME = 100;
    
    protected static final long CORDON_RENDERING_TIME = 40;

    protected float x = 0;

    protected float y = -1;

    protected int mDistance;

    protected float[] RECT = null;

    protected float mStepX;

    protected long mLastTime;

    public R2LDanmaku(Duration duration) {
        this.duration = duration;
    }

    @Override
    public void layout(IDisplayer displayer, float x, float y) {
        if (mTimer != null) {
            long currMS = mTimer.currMillisecond;
            long deltaDuration = currMS - getActualTime(); // 当前时间减去显示的时间点(弹幕出现的时间)
            // 注意duration.value是显示的时间段，之所以包含弹幕从头显示到最后尾巴消失的整个过程，是measure时候计算的“mStepX”起了关键作用
            if (deltaDuration > 0 && deltaDuration < duration.value) {
                // 弹幕显示在屏幕上
                this.x = getAccurateLeft(displayer, currMS);
                if (!this.isShown()) { // 如果之前还没有显示，此刻显示在屏幕上后赋值y值，更新visibility状态
                    this.y = y;
                    this.setVisibility(true);
                }
                mLastTime = currMS;
                return;
            }
            mLastTime = currMS;
        }
        this.setVisibility(false);
    }

    protected float getAccurateLeft(IDisplayer displayer, long currTime) {
        long elapsedTime = currTime - getActualTime();
        if (elapsedTime >= duration.value) {
            // 如果弹幕已经显示出最左端，直接x返回弹幕尾巴在屏幕最左端的值
            return -paintWidth;
        }

        return displayer.getWidth() - elapsedTime * mStepX;
    }

    @Override
    public float[] getRectAtTime(IDisplayer displayer, long time) {
        if (!isMeasured())
            return null;
        float left = getAccurateLeft(displayer, time);
        if (RECT == null) {
            RECT = new float[4];
        }
        RECT[0] = left;
        RECT[1] = y;
        RECT[2] = left + paintWidth;
        RECT[3] = y + paintHeight;
        return RECT;
    }

    @Override
    public float getLeft() {
        return x;
    }

    @Override
    public float getTop() {
        return y;
    }

    @Override
    public float getRight() {
        return x + paintWidth;
    }

    @Override
    public float getBottom() {
        return y + paintHeight;
    }

    @Override
    public int getType() {
        return TYPE_SCROLL_RL;
    }
    
    @Override
    public void measure(IDisplayer displayer, boolean fromWorkerThread) {
        super.measure(displayer, fromWorkerThread);
        // 在这里的距离是加上弹幕长度的，而显示的总时间是duration.value！
        // 所以在layout中的duration.value时间段内根据mStepX步长可以全部跑完整个弹幕(mStepX * duration.value = mDistance)
        // 相当于在duration.value的时间内(显示在屏幕上的时间)跑mDistance距离的速度(x值！)
        mDistance = (int) (displayer.getWidth() + paintWidth);
        mStepX = mDistance / (float) duration.value;
    }

}
