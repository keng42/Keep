package science.keng42.keep.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import science.keng42.keep.HomeActivity;

public class SwipeableRVTL implements RecyclerView.OnItemTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private RecyclerView mRecyclerView;
    private SwipeListener mSwipeListener;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private List<PendingDismissData> mPendingDismisses = new ArrayList<>();
    private int mDismissAnimationRefCount = 0;
    private float mAlpha;
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private int mAnimatingPosition = ListView.INVALID_POSITION;
    private View mDownView;
    private boolean mPaused;
    private float mFinalDelta;

    public SwipeableRVTL(RecyclerView recyclerView, SwipeListener listener) {
        ViewConfiguration vc = ViewConfiguration.get(recyclerView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = recyclerView.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
//        mAnimationTime = 50;
        mRecyclerView = recyclerView;
        mSwipeListener = listener;

        // 确保滚动时不能滑动
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                setEnabled(newState != RecyclerView.SCROLL_STATE_DRAGGING);
            }
        });
    }

    /**
     * 设置是否能滑动
     */
    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent motionEvent) {
        return handleTouchEvent(motionEvent);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent motionEvent) {
        handleTouchEvent(motionEvent);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    /**
     * 处理触摸事件
     */
    private boolean handleTouchEvent(MotionEvent motionEvent) {
        if (mViewWidth < 2) {
            mViewWidth = mRecyclerView.getWidth();
        }
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                actionDown(motionEvent);
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                actionCancel();
                break;
            }
            case MotionEvent.ACTION_UP: {
                actionUp(motionEvent);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                return actionMove(motionEvent);
            }
        }
        return false;
    }

    /**
     * 滑动中，设置 view 的位置和透明度
     */
    private boolean actionMove(MotionEvent motionEvent) {
        if (mVelocityTracker == null || mPaused) {
            return false;
        }

        // 更新速度
        mVelocityTracker.addMovement(motionEvent);
        // x y 轴的增量
        float deltaX = motionEvent.getRawX() - mDownX;
        float deltaY = motionEvent.getRawY() - mDownY;
        // 不是滑动中并且有横向移动且横向移动大于纵向移动
        if (!mSwiping && Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
            // 设置为滑动中
            mSwiping = true;
            mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);
        }

        if (mSwiping) {
            mDownView.setTranslationX(deltaX - mSwipingSlop);
            mDownView.setAlpha(Math.max(0f, Math.min(
                    mAlpha, mAlpha * (1f - Math.abs(deltaX) / mViewWidth))));
            return true;
        }
        return false;
    }

    /**
     * 滑动结束时，判断是否要消失同时执行剩余的动画
     */
    private void actionUp(MotionEvent motionEvent) {
        if (mVelocityTracker == null) {
            return;
        }

        // 最终的横向移动的变量
        mFinalDelta = motionEvent.getRawX() - mDownX;
        // 计算最终的速度
        mVelocityTracker.addMovement(motionEvent);
        mVelocityTracker.computeCurrentVelocity(1000);
        float velocityX = mVelocityTracker.getXVelocity();
        float absVelocityX = Math.abs(velocityX);
        float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());

        boolean dismiss = false;
        boolean dismissRight = false;

        if (Math.abs(mFinalDelta) > mViewWidth / 2 && mSwiping) {
            // 移动量足够大可以消失
            dismiss = true;
            dismissRight = mFinalDelta > 0;
        } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                && absVelocityY < absVelocityX && mSwiping) {
            // 只有在丢出去的方向和滑动方向相同时才消失
            dismiss = (velocityX < 0) == (mFinalDelta < 0);
            dismissRight = mVelocityTracker.getXVelocity() > 0;
        }

        // 当前 view 允许消失且该 view 是不在执行动画中的有效 view
        if (dismiss && mDownPosition != mAnimatingPosition
                && mDownPosition != ListView.INVALID_POSITION) {
            // 执行剩下的消失动画
            final View downView = mDownView; // mDownView 在动画结束之前就变为 null
            final int downPosition = mDownPosition;
            ++mDismissAnimationRefCount;
            mAnimatingPosition = mDownPosition;
            mDownView.animate()
                    .translationX(dismissRight ? mViewWidth : -mViewWidth)
                    .alpha(0)
                    .setDuration(mAnimationTime / 4)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            performDismiss(downView, downPosition);
                        }
                    });
        } else {
            // 取消，执行恢复的动画
            mDownView.animate()
                    .translationX(0)
                    .alpha(mAlpha)
                    .setDuration(mAnimationTime)
                    .setListener(null);
        }

        // 初始化各个全局变量
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mDownX = 0;
        mDownY = 0;
        mDownView = null;
        mDownPosition = ListView.INVALID_POSITION;
        mSwiping = false;
    }

    /**
     * 滑动取消时调用
     */
    private void actionCancel() {
        if (mVelocityTracker == null) {
            return;
        }

        if (mDownView != null && mSwiping) {
            // 取消，执行恢复的动画
            mDownView.animate()
                    .translationX(0)
                    .alpha(mAlpha)
                    .setDuration(mAnimationTime)
                    .setListener(null);
        }

        // 初始化各个全局变量
        mVelocityTracker.recycle();
        mVelocityTracker = null;
        mDownX = 0;
        mDownY = 0;
        mDownView = null;
        mDownPosition = ListView.INVALID_POSITION;
        mSwiping = false;
    }

    /**
     * 滑动开始准备
     */
    private void actionDown(MotionEvent motionEvent) {
        if (mPaused) {
            return;
        }

        // 找到被点击的 view 和位置（包括 top left right bottom）
        Rect rect = new Rect();
        int childCount = mRecyclerView.getChildCount();
        // location 指的是 RecyclerView 左上角以整个屏幕的左上角为参照物的坐标
        int[] location = new int[2];
        mRecyclerView.getLocationOnScreen(location);
        // 此处的 x y 指的是触摸点以 location 为参照物时的坐标
        int x = (int) motionEvent.getRawX() - location[0];
        int y = (int) motionEvent.getRawY() - location[1];
        View child;
        for (int i = 0; i < childCount; i++) {
            child = mRecyclerView.getChildAt(i);
            child.getHitRect(rect);
            if (rect.contains(x, y)) {
                mDownView = child;
                break;
            }
        }

        // 找到相应的 view 并且该 view 不处于执行动画状态
        if (mDownView != null && mAnimatingPosition != mRecyclerView.getChildAdapterPosition(mDownView)) {
            mAlpha = mDownView.getAlpha();
            mDownX = motionEvent.getRawX();
            mDownY = motionEvent.getRawY();
            mDownPosition = mRecyclerView.getChildAdapterPosition(mDownView);
            if (mSwipeListener.canSwipe(mDownPosition)) {
                // ？添加速度追踪器
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(motionEvent);
            } else {
                mDownView = null;
            }
        }
    }

    /**
     * 动画执行结束时调用
     * 0. 使所有要消失的 view 高度设置为 0
     * 1. 所有动画执行完之后触发回调函数
     * This triggers layout on each animation frame;
     *
     * @param dismissView     执行动画的 view
     * @param dismissPosition 该 view 在 RecyclerView 中的位置
     */
    private void performDismiss(final View dismissView, final int dismissPosition) {
        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalLayoutParamsHeight = lp.height;
        final int originalHeight = dismissView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1)
                .setDuration(mAnimationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                --mDismissAnimationRefCount;
                if (mDismissAnimationRefCount == 0) {
                    // No active animations, process all pending dismisses.
                    Collections.sort(mPendingDismisses);
                    int[] dismissPositions = new int[mPendingDismisses.size()];
                    for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
                        dismissPositions[i] = mPendingDismisses.get(i).position;
                    }

                    // 调用回调函数
                    if (mFinalDelta > 0) {
                        mSwipeListener.onDismissedBySwipeRight(mRecyclerView, dismissPositions);
                    } else {
                        mSwipeListener.onDismissedBySwipeLeft(mRecyclerView, dismissPositions);
                    }

                    // 避免 MotionEvent.ACTION_UP 根据旧的位置触发新的动画
                    mDownPosition = ListView.INVALID_POSITION;

                    ViewGroup.LayoutParams lp;
                    for (PendingDismissData pendingDismiss : mPendingDismisses) {
                        // Reset view presentation
                        pendingDismiss.view.setAlpha(mAlpha);
                        pendingDismiss.view.setTranslationX(0);

                        lp = pendingDismiss.view.getLayoutParams();
                        lp.height = originalLayoutParamsHeight;

                        pendingDismiss.view.setLayoutParams(lp);
                    }

                    // Send a cancel event
                    long time = SystemClock.uptimeMillis();
                    MotionEvent cancelEvent = MotionEvent.obtain(time, time,
                            MotionEvent.ACTION_CANCEL, 0, 0, 0);
                    mRecyclerView.dispatchTouchEvent(cancelEvent);

                    mPendingDismisses.clear();
                    mAnimatingPosition = ListView.INVALID_POSITION;
                }
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                lp.height = (Integer) valueAnimator.getAnimatedValue();
//                dismissView.setLayoutParams(lp);
            }
        });

        mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
        animator.start();
    }

    /**
     * 由调用者实现，实现滑动后的具体操作
     */
    public interface SwipeListener {
        boolean canSwipe(int position);

        void onDismissedBySwipeLeft(RecyclerView rv, int[] rsp);

        void onDismissedBySwipeRight(RecyclerView rv, int[] rsp);
    }

    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(@NonNull PendingDismissData other) {
            return other.position - position;
        }
    }
}