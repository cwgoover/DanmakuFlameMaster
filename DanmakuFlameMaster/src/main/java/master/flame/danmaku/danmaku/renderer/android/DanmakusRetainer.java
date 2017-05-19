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

package master.flame.danmaku.danmaku.renderer.android;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

public class DanmakusRetainer {

    private IDanmakusRetainer rldrInstance = null;

    private IDanmakusRetainer lrdrInstance = null;

    private IDanmakusRetainer ftdrInstance = null;

    private IDanmakusRetainer fbdrInstance = null;

    public DanmakusRetainer(boolean alignBottom) {
        alignBottom(alignBottom);
    }

    public void alignBottom(boolean alignBottom) {
        rldrInstance = alignBottom ? new AlignBottomRetainer() : new AlignTopRetainer();
        lrdrInstance = alignBottom ? new AlignBottomRetainer() : new AlignTopRetainer();
        if (ftdrInstance == null) {
            ftdrInstance = new FTDanmakusRetainer();
        }
        if (fbdrInstance == null) {
            fbdrInstance = new AlignBottomRetainer();
        }
    }

    public void fix(BaseDanmaku danmaku, IDisplayer disp, Verifier verifier) {

        int type = danmaku.getType();
        switch (type) {
            case BaseDanmaku.TYPE_SCROLL_RL:
                rldrInstance.fix(danmaku, disp, verifier);
                break;
            case BaseDanmaku.TYPE_SCROLL_LR:
                lrdrInstance.fix(danmaku, disp, verifier);
                break;
            case BaseDanmaku.TYPE_FIX_TOP:
                ftdrInstance.fix(danmaku, disp, verifier);
                break;
            case BaseDanmaku.TYPE_FIX_BOTTOM:
                fbdrInstance.fix(danmaku, disp, verifier);
                break;
            case BaseDanmaku.TYPE_SPECIAL:
                danmaku.layout(disp, 0, 0);
                break;
        }

    }

    public void clear() {
        if (rldrInstance != null) {
            rldrInstance.clear();
        }
        if (lrdrInstance != null) {
            lrdrInstance.clear();
        }
        if (ftdrInstance != null) {
            ftdrInstance.clear();
        }
        if (fbdrInstance != null) {
            fbdrInstance.clear();
        }
    }
    
    public void release(){
        clear();
    }

    public interface Verifier {

        public boolean skipLayout(BaseDanmaku danmaku, float fixedTop, int lines, boolean willHit);

    }

    public interface IDanmakusRetainer {

        public void fix(BaseDanmaku drawItem, IDisplayer disp, Verifier verifier);

        public void clear();

    }

    private static class RetainerState {
        public int lines = 0;
        public BaseDanmaku insertItem = null, firstItem = null, lastItem = null, minRightRow = null, removeItem = null;
        public boolean overwriteInsert = false;
        public boolean shown = false;
        public boolean willHit = false;
    }


    private static class AlignTopRetainer implements IDanmakusRetainer {
        protected class RetainerConsumer extends IDanmakus.Consumer<BaseDanmaku, RetainerState> {
            public IDisplayer disp;
            int lines = 0;
            public BaseDanmaku insertItem = null, firstItem = null, lastItem = null, minRightRow = null, drawItem = null;
            boolean overwriteInsert = false;
            boolean shown = false;
            boolean willHit = false;

            @Override
            public void before() {
                lines = 0;
                insertItem = firstItem = lastItem = minRightRow = null;
                overwriteInsert = shown = willHit = false;
            }

            @Override
            public int accept(BaseDanmaku item) {
                if (mCancelFixingFlag) {
                    return ACTION_BREAK;
                }
                lines++;
                if(item == drawItem){
                    insertItem = item;
                    lastItem = null;
                    shown = true;
                    willHit = false;
                    return ACTION_BREAK;
                }

                if (firstItem == null)
                    firstItem = item;

                if (drawItem.paintHeight + item.getTop() > disp.getHeight()) {
                    // 此时insertItem为null
                    overwriteInsert = true;
                    return ACTION_BREAK;
                }

                if (minRightRow == null) {
                    minRightRow = item;
                } else {
                    if (minRightRow.getRight() >= item.getRight()) {
                        minRightRow = item;
                    }
                }

                // 检查碰撞
                willHit = DanmakuUtils.willHitInDuration(disp, item, drawItem,
                        drawItem.getDuration(), drawItem.getTimer().currMillisecond);
                if (!willHit) {
                    insertItem = item;
                    return ACTION_BREAK;
                }

                lastItem = item;
                return ACTION_CONTINUE;
            }

            @Override
            public RetainerState result() {
                RetainerState retainerState = new RetainerState();
                retainerState.lines = this.lines;
                retainerState.firstItem = this.firstItem;
                retainerState.insertItem = this.insertItem;
                retainerState.lastItem = this.lastItem;
                retainerState.minRightRow = this.minRightRow;
                retainerState.overwriteInsert = this.overwriteInsert;
                retainerState.shown = this.shown;
                retainerState.willHit = this.willHit;
                return retainerState;
            }
        }

        //保存需要显示的弹幕容器类（保存的一行只有一条弹幕，下面会说明），内部持有一个以弹幕的y坐标排序的TreeSet集合，这个需要注意
        protected Danmakus mVisibleDanmakus = new Danmakus(Danmakus.ST_BY_YPOS);
        protected boolean mCancelFixingFlag = false;
        protected RetainerConsumer mConsumer = new RetainerConsumer();

        @Override
        public void fix(BaseDanmaku drawItem, IDisplayer disp, Verifier verifier) {
            if (drawItem.isOutside())
                return;
            float topPos = disp.getAllMarginTop();
            int lines = 0;
            boolean shown = drawItem.isShown();
            boolean willHit = !shown && !mVisibleDanmakus.isEmpty();
            boolean isOutOfVertialEdge = false;
            BaseDanmaku removeItem = null;
            int margin = disp.getMargin();
            if (!shown) {
                mCancelFixingFlag = false;
                // 确定弹幕位置
                BaseDanmaku insertItem = null, firstItem = null, lastItem = null, minRightRow = null;
                boolean overwriteInsert = false;
                mConsumer.disp = disp;
                mConsumer.drawItem = drawItem;
                mVisibleDanmakus.forEachSync(mConsumer);
                RetainerState retainerState = mConsumer.result();
                if (retainerState != null) {
                    lines = retainerState.lines;
                    insertItem = retainerState.insertItem;
                    firstItem = retainerState.firstItem;
                    lastItem = retainerState.lastItem;
                    minRightRow = retainerState.minRightRow;
                    overwriteInsert = retainerState.overwriteInsert;
                    shown = retainerState.shown;
                    willHit = retainerState.willHit;
                }
                boolean checkEdge = true;
                if (insertItem != null) { //已经布局过了||目标弹幕不会碰壁可以插入
                    if (lastItem != null) {
                        // 目标弹幕插入，y值即为上一次遍历的弹幕的底部
                        // 因为一个循环相当于一行（如果一行中有位置willHit肯定为false就跳出循环了，能进入下个循环，说明上一层都满了）
                        // 所以这个循环找到的item应该在上次循环所在行的下面
                        topPos = lastItem.getBottom() + margin;
                    } else {
                        topPos = insertItem.getTop();
                    }
                    if (insertItem != drawItem){ // 如果不相等，说明是检查碰撞出来的，insertItem是drawItem的横排前一个弹幕
                        //这里需要注意，因为一行可以放n多条弹幕，只要前后不碰撞就行；
                        //所以下次我们在同一行插入弹幕判断碰壁时，当然要和这行最后一条弹幕去判断；
                        //因此我们移除前一条弹幕，放入插入的目标弹幕，下次添加弹幕判断时就和目标弹幕判断，然后这么循环下去
                        removeItem = insertItem;
                        shown = false; //置为false，以便mVisibleDanmakus 添加还未布局的新弹幕
                    }
                } else if (overwriteInsert && minRightRow != null) { //如果可显示区域内没有找到插入位置(insertItem == null)，但是显示区域不止一行(一个循环后minRightRow肯定有值)
                    // TODO：这里可能会造成重叠现象，相当于弹幕实在没地塞了就捡一个行中空隙最大的塞（要看后面layout时候怎么布x轴）
                    topPos = minRightRow.getTop(); // 放在所有行中最后一个弹幕最靠左的那一行，相当于所有行中最短的那一行
                    checkEdge = false;
                    shown = false; // TODO：这里还有个问题，光添加到行里但是没有删除同行的前一个，mVisibleDanmakus并不是每行只有最后一个弹幕，check！！！！
                } else if (lastItem != null) { //如果mVisibleDanmakus里代表的行都满了但是还没有触底
                    topPos = lastItem.getBottom() + margin;
                    willHit = false;
                } else if (firstItem != null) { //走到这里只能是第一次进循环后就overwriteInsert = true了，否则怎么样都会有insertItem或者lastItem
                    // 相当于弹幕可显示的区域只有一行的距离，注意这种情况并没有检查碰撞过
                    topPos = firstItem.getTop();
                    removeItem = firstItem;
                    shown = false;
                } else { //mVisibleDanmakus 没有数据，截取弹幕集的第一条弹幕
                    topPos = disp.getAllMarginTop();
                }
                if (checkEdge) {
                    isOutOfVertialEdge = isOutVerticalEdge(overwriteInsert, drawItem, disp, topPos, firstItem,
                            lastItem);
                }
                if (isOutOfVertialEdge) {
                    topPos = disp.getAllMarginTop();
                    willHit = true; //如果超出布局范围，等待河蟹
                    lines = 1;
                } else if (removeItem != null) {
                    lines--;
                }
                if (topPos == disp.getAllMarginTop()) {
                    shown = false; //方便加入容器
                }
            }

            //TODO：这是河蟹规则，都是在设置DanmakuContext时指定的，比如最大行数限制，重复限制等等。
            if (verifier != null && verifier.skipLayout(drawItem, topPos, lines, willHit)) {
                return;
            }

            if (isOutOfVertialEdge) { //mVisibleDanmakus中所有弹幕绘制出来都超出范围了
                clear();
            }

            drawItem.layout(disp, drawItem.getLeft(), topPos);

            if (!shown) {
                mVisibleDanmakus.removeItem(removeItem);
                mVisibleDanmakus.addItem(drawItem);
            }

        }

        protected boolean isOutVerticalEdge(boolean overwriteInsert, BaseDanmaku drawItem,
                                            IDisplayer disp, float topPos, BaseDanmaku firstItem, BaseDanmaku lastItem) {
            if (topPos < disp.getAllMarginTop() || (firstItem != null && firstItem.getTop() > 0) || topPos + drawItem.paintHeight > disp.getHeight()) {
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            mCancelFixingFlag = true;
            mVisibleDanmakus.clear();
        }

    }

    private static class FTDanmakusRetainer extends AlignTopRetainer {

        @Override
        protected boolean isOutVerticalEdge(boolean overwriteInsert, BaseDanmaku drawItem,
                                            IDisplayer disp, float topPos, BaseDanmaku firstItem, BaseDanmaku lastItem) {
            if (topPos + drawItem.paintHeight > disp.getHeight()) {
                return true;
            }
            return false;
        }

    }

    private static class AlignBottomRetainer extends FTDanmakusRetainer {
        protected class RetainerConsumer extends IDanmakus.Consumer<BaseDanmaku, RetainerState> {
            public IDisplayer disp;
            int lines = 0;
            public BaseDanmaku removeItem = null, firstItem = null, drawItem = null;
            boolean willHit = false;
            float topPos;

            @Override
            public void before() {
                lines = 0;
                removeItem = firstItem = null;
                willHit = false;
            }

            @Override
            public int accept(BaseDanmaku item) {
                if (mCancelFixingFlag) {
                    return ACTION_BREAK;
                }
                lines++;
                if (item == drawItem) {
                    removeItem = null;
                    willHit = false;
                    return ACTION_BREAK;
                }

                if (firstItem == null) {
                    firstItem = item;
                    if (firstItem.getBottom() != disp.getHeight()) {
                        return ACTION_BREAK;
                    }
                }

                if (topPos < disp.getAllMarginTop()) {
                    removeItem = null;
                    return ACTION_BREAK;
                }

                // 检查碰撞
                willHit = DanmakuUtils.willHitInDuration(disp, item, drawItem,
                        drawItem.getDuration(), drawItem.getTimer().currMillisecond);
                if (!willHit) {
                    removeItem = item;
                    // topPos = item.getBottom() - drawItem.paintHeight;
                    return ACTION_BREAK;
                }

                topPos = item.getTop() - disp.getMargin() - drawItem.paintHeight;
                return ACTION_CONTINUE;
            }

            @Override
            public RetainerState result() {
                RetainerState retainerState = new RetainerState();
                retainerState.lines = this.lines;
                retainerState.firstItem = this.firstItem;
                retainerState.removeItem = this.removeItem;
                retainerState.willHit = this.willHit;
                return retainerState;
            }
        }

        protected RetainerConsumer mConsumer = new RetainerConsumer();
        protected Danmakus mVisibleDanmakus = new Danmakus(Danmakus.ST_BY_YPOS_DESC);

        @Override
        public void fix(BaseDanmaku drawItem, IDisplayer disp, Verifier verifier) {
            if (drawItem.isOutside())
                return;
            boolean shown = drawItem.isShown();
            float topPos = shown ? drawItem.getTop() : -1;
            int lines = 0;
            boolean willHit = !shown && !mVisibleDanmakus.isEmpty();
            boolean isOutOfVerticalEdge = false;
            if (topPos < disp.getAllMarginTop()) {
                topPos = disp.getHeight() - drawItem.paintHeight;
            }
            BaseDanmaku removeItem = null, firstItem = null;
            if (!shown) {
                mCancelFixingFlag = false;
                mConsumer.topPos = topPos;
                mConsumer.disp = disp;
                mConsumer.drawItem = drawItem;
                mVisibleDanmakus.forEachSync(mConsumer);
                RetainerState retainerState = mConsumer.result();
                topPos = mConsumer.topPos;
                if (retainerState != null) {
                    lines = retainerState.lines;
                    firstItem = retainerState.firstItem;
                    removeItem = retainerState.removeItem;
                    shown = retainerState.shown;
                    willHit = retainerState.willHit;
                }

                isOutOfVerticalEdge = isOutVerticalEdge(false, drawItem, disp, topPos, firstItem, null);
                if (isOutOfVerticalEdge) {
                    topPos = disp.getHeight() - drawItem.paintHeight;
                    willHit = true;
                    lines = 1;
                } else {
                    if (topPos >= disp.getAllMarginTop()) {
                        willHit = false;
                    }
                    if (removeItem != null) {
                        lines--;
                    }
                }

            }

            if (verifier != null && verifier.skipLayout(drawItem, topPos, lines, willHit)) {
                return;
            }

            if (isOutOfVerticalEdge) {
                clear();
            }

            drawItem.layout(disp, drawItem.getLeft(), topPos);

            if (!shown) {
                mVisibleDanmakus.removeItem(removeItem);
                mVisibleDanmakus.addItem(drawItem);
            }

        }

        protected boolean isOutVerticalEdge(boolean overwriteInsert, BaseDanmaku drawItem,
                                            IDisplayer disp, float topPos, BaseDanmaku firstItem, BaseDanmaku lastItem) {
            if (topPos < disp.getAllMarginTop() || (firstItem != null && firstItem.getBottom() != disp.getHeight())) {
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            mCancelFixingFlag = true;
            mVisibleDanmakus.clear();
        }

    }

}
