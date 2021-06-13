package com.example.cardstackview;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;


public class TowerCardManager extends CardManager {

    private float mTouchDownX = 0;
    private float mTouchDownY = 0;
    private boolean mIsTouchUp = false;
    // the dragged border position to differentiate whether the dragged card be drop or reset
    private float mDragThresholdX, mDragThresholdY;
    private int mMinVelocityThreshold = 2000;
    private int mMaxVelocityThreshold = 4500;

    private VelocityTracker mVelocityTracker = null;

    private int mCardOffset = 10;
    private int mCardElevation = 10;

    public TowerCardManager(Context context, CardRecyclerView recyclerView) {
        super(context, recyclerView);
        // forbidden the default item animator
//        ((DefaultItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mRecyclerView.setItemAnimator(null);
        mDragThresholdX = mScreenWidth / 3;
        mDragThresholdY = mScreenHeight / 3;
        ViewCompat.setTranslationZ(mRecyclerView, -1);
    }

    public void setCardOffset(int cardOffset) {
        if (cardOffset < 0)
            throw new IllegalArgumentException("cardOffset must be over zero !!!");
        this.mCardOffset = cardOffset;
    }

    public void setCardElevation(int cardElevation) {
        if (cardElevation < 0)
            throw new IllegalArgumentException("cardElevation must be over zero !!!");
        this.mCardElevation = cardElevation;
    }


    @Override
    public void onLayoutCards(RecyclerView.Recycler recycler, RecyclerView.State state) {
        onLayoutCards(recycler, state, mRecyclerView.getVisibleCardCount(), mCardOffset, mCardElevation);
        setTargetDragCard();
    }

    private void onLayoutCards(RecyclerView.Recycler recycler, RecyclerView.State state,
                               int visibleCardCount, int cardOffset, int cardElevation) {
        if (getItemCount() > 0) {
            // calculate the validate areas that all the visible cards cover.
            int left = 0;
            int top = 0;
            int maxAttachChildrenCount = Math.min(mRecyclerView.getVisibleCardCount(), getItemCount() - 1);
            for (int i = maxAttachChildrenCount; i >= 0; i--) {
                View child_i = recycler.getViewForPosition(i);
                addView(child_i);
                measureChildWithMargins(child_i, 0, 0);
                int childWidth = getDecoratedMeasuredWidth(child_i);
                int childHeight = getDecoratedMeasuredHeight(child_i);
                if (i == maxAttachChildrenCount) {
                    int childHeightWithTotalOffset = childHeight + cardOffset * (visibleCardCount - 1);
                    int parentWExcludePadding = getWidth() - getPaddingLeft() - getPaddingRight();
                    int parentHExcludePadding = getHeight() - getPaddingTop() - getPaddingBottom();
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) child_i.getLayoutParams();
                    int childMarginHorizOffset = params.leftMargin - params.rightMargin;
                    int childMarginVertOffset = params.topMargin - params.bottomMargin;
                    left = (parentWExcludePadding - childWidth) / 2 + getPaddingLeft() + childMarginHorizOffset;
                    top = (parentHExcludePadding - childHeightWithTotalOffset) / 2 + getPaddingTop() + childMarginVertOffset;
                }
                // remove decorator area
                Rect childRect = new Rect();
                calculateItemDecorationsForChild(child_i, childRect);
                int top_i;
                float scaleX = (childWidth - i * cardOffset * 2) * 1f / childWidth;
                float scaleY = (childHeight - i * cardOffset * 2) * 1f / childHeight;
                float transZ = cardElevation * (visibleCardCount - i);
                if (i <= visibleCardCount - 1) {
                    top_i = top + cardOffset * i * 2;
                } else {
                    top_i = top + cardOffset * (visibleCardCount - 1) * 2;
                }
                child_i.setScaleX(scaleX);
                child_i.setScaleY(scaleY);
                // set elevations for all the visible children
                ViewCompat.setTranslationZ(child_i, transZ);

                // reset card
                child_i.setTranslationY(0);
                child_i.setTranslationX(0);
                child_i.setAlpha(1f);
                child_i.setRotation(0);

                layoutDecorated(child_i, left, top_i, left + childWidth, top_i + childHeight);
            }
        }
    }

    private void setTargetDragCard() {
        // count of all the attached cards
        int maxAttachChildrenCount = Math.min(mRecyclerView.getVisibleCardCount(), getItemCount() - 1);
        if (maxAttachChildrenCount >= 0) {
            for (int i = 0; i <= maxAttachChildrenCount; i++) {
                getChildAt(i).setOnTouchListener(this);
                if (i == maxAttachChildrenCount) {
                    dispatchOnDragEvent(getChildAt(i), false, false, 0, 0);
                }
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int maxAttachChildrenCount = Math.min(mRecyclerView.getVisibleCardCount(), getItemCount() - 1);
        if (v != getChildAt(maxAttachChildrenCount))
            return true;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        int velocityX = 0;
        int velocityY = 0;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            ViewCompat.setTranslationZ(mRecyclerView, 1);
            mTouchDownX = event.getRawX();
            mTouchDownY = event.getRawY();
            mIsTouchUp = false;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mIsTouchUp) {
                mTouchDownX = event.getRawX();
                mTouchDownY = event.getRawY();
                mIsTouchUp = false;
            }
            ViewCompat.setTranslationZ(mRecyclerView, 1);
            if (event.getEventTime() - event.getDownTime() >= 100) {
                dragCard(v, event.getRawX() - mTouchDownX, event.getRawY() - mTouchDownY,
                        event.getRawX() - mTouchDownX);
                dispatchOnDragEvent(v, true, false, event.getRawX() - mTouchDownX, event.getRawY() - mTouchDownY);
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            mIsTouchUp = true;
            if (mVelocityTracker != null) {
                mVelocityTracker.computeCurrentVelocity(1000);
                velocityX = (int) mVelocityTracker.getXVelocity();
                velocityY = (int) mVelocityTracker.getYVelocity();
            }
            int velocity = (int) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
            if (velocity > mMaxVelocityThreshold && event.getEventTime() - event.getDownTime() < 100) {
                ViewCompat.setTranslationZ(mRecyclerView, -1);
                return true;
            } else if (velocity >= mMinVelocityThreshold
                    || event.getEventTime() - event.getDownTime() >= 100) {
                releaseCard(v, event.getRawX() - mTouchDownX, event.getRawY() - mTouchDownY, velocity);
                return true;
            } else
                ViewCompat.setTranslationZ(mRecyclerView, -1);
        }
        return false;
    }

    // hold the card just dragging as free as you can
    private void dragCard(View card, float offset_x, float offset_y, float touchX) {
        // trans
        card.setTranslationX(offset_x);
        card.setTranslationY(offset_y);
        // factor
        float distance = (float) Math.sqrt(offset_x * offset_x + offset_y * offset_y);
        float maxDistance = (float) Math.sqrt(mScreenWidth * mScreenWidth + mScreenHeight * mScreenHeight);
        float factor = Math.min(1, distance / maxDistance);
        // tansZ
        float ori_elevation = mRecyclerView.getVisibleCardCount() * mCardElevation;
        ViewCompat.setTranslationZ(card, (float) (ori_elevation * (1 + Math.sqrt(factor))));
        //scale
        card.setScaleX(1 - factor);
        card.setScaleY(1 - factor);
        // alpha
        card.setAlpha(1 - factor * factor);
        // rotate
        float rotateDegree = offset_x == 0 ? 0 : (float) Math.asin(touchX / mScreenWidth);
        rotateDegree = (float) (rotateDegree * 180 / Math.PI);
        // deg factor to make the rotate more smooth
//        rotateDegree = rotateDegree * factor;
        card.setRotation(rotateDegree);
        refreshOtherVisibleCardsPosition(offset_x, offset_y);
    }

    private void releaseCard(View card, float offset_x, float offset_y, int velocity) {
        // check card status to decide next action
        if (Math.abs(offset_x) >= mDragThresholdX
                || Math.abs(offset_y) >= mDragThresholdY
                || (velocity >= mMinVelocityThreshold && velocity <= mMaxVelocityThreshold)) {
            dropCard(card);
        } else {
            resetDragCard(card);
        }
    }

    // drop card without ontouch event drop from left if orientation is minus otherwise right
    @Override
    public void dropCardNoTouch(int orientation) {
        if (getChildCount() <= 0)
            return;
        ViewCompat.setTranslationZ(mRecyclerView, 1);
        final View card = getChildAt(getChildCount() - 1);
        orientation = orientation < 0 ? -1 : 1;
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1.1f);
        final int finalOrientation = orientation;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float offset = (float) animation.getAnimatedValue();
                dragCard(card, finalOrientation * mDragThresholdX * offset, mDragThresholdY * offset / 5,
                        finalOrientation * mScreenWidth / 2 * offset);
                dispatchOnDragEvent(card, true, false, card.getTranslationX(), card.getTranslationY());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dropCard(card);
            }
        });
        animator.setDuration(150);
        animator.start();
    }

    // del the current card or recycle it
    private void dropCard(final View card) {
        final float oriTransX = card.getTranslationX();
        final float oriTransY = card.getTranslationY();
        final float oriRotateDeg = card.getRotation();
        final float oriScaleX = card.getScaleX();
        final float oriScaleY = card.getScaleY();
        final float oriAlpha = card.getAlpha();

        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float offset = (float) animation.getAnimatedValue();
                card.setTranslationX(3 * oriTransX - 2 * oriTransX * offset);
                card.setTranslationY(3 * oriTransY - 2 * oriTransY * offset);
                card.setRotation(3 * oriRotateDeg - 2 * oriRotateDeg * offset);
                card.setScaleX(oriScaleX * offset);
                card.setScaleY(oriScaleY * offset);
                card.setAlpha(oriAlpha * offset);
                refreshOtherVisibleCardsPosition(mDragThresholdX + (Math.abs(oriTransX) - mDragThresholdX) * offset,
                        mDragThresholdY + (Math.abs(oriTransY) - mDragThresholdY) * offset);
                dispatchOnDragEvent(card, false, false, oriTransX, oriTransY);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (null != mRecyclerView && null != mRecyclerView.getAdapter()) {
                    mRecyclerView.dropCard();
                    dispatchOnDragEvent(card, false, true, oriTransX, oriTransY);
                    ViewCompat.setTranslationZ(mRecyclerView, -1);
                }
            }
        });
        animator.setDuration(300);
        animator.start();
    }

    // let the card go back to its ori position
    private void resetDragCard(final View card) {
        final float oriTransX = card.getTranslationX();
        final float oriTransY = card.getTranslationY();
        final float oriRotateDeg = card.getRotation();
        final float oriScaleX = card.getScaleX();
        final float oriScaleY = card.getScaleY();
        final float oriAlpha = card.getAlpha();

        final float oriTransZ = ViewCompat.getTranslationZ(card);
        final float targetTransZ = mRecyclerView.getVisibleCardCount() * mCardElevation;

        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.setInterpolator(new OvershootInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float offset = (float) animation.getAnimatedValue();
                card.setTranslationX(oriTransX * offset);
                card.setTranslationY(oriTransY * offset);
                ViewCompat.setTranslationZ(card, targetTransZ + (oriTransZ - targetTransZ) * offset);
                card.setRotation(oriRotateDeg * offset);
                card.setScaleX((oriScaleX - 1) * offset + 1);
                card.setScaleY((oriScaleY - 1) * offset + 1);
                card.setAlpha((oriAlpha - 1) * offset + 1);
                refreshOtherVisibleCardsPosition(oriTransX * offset, oriTransY * offset);
                dispatchOnDragEvent(card, false, false, oriTransX * offset, oriTransY * offset);

            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ViewCompat.setTranslationZ(mRecyclerView, -1);
            }
        });
        animator.setDuration(500);
        animator.start();
    }

    // refresh other visible cards' positions when dragging
    private void refreshOtherVisibleCardsPosition(float offset_x, float offset_y) {
        float factor = (float) (Math.sqrt(offset_x * offset_x + offset_y * offset_y)
                / Math.sqrt(mDragThresholdX * mDragThresholdX + mDragThresholdY * mDragThresholdY));
        factor = Math.min(factor, 1);
        if (getItemCount() > 1) {
            int cardOffset = mCardOffset;
            // count of all the attached cards
            int maxAttachChildrenCount = Math.min(mRecyclerView.getVisibleCardCount(), getItemCount() - 1);
            // count of all the cards required refreshing
            int totalRefreshingCount = Math.min(getItemCount() - 1, mRecyclerView.getVisibleCardCount());
            for (int i = 1; i <= totalRefreshingCount; i++) {
                int childPosition = maxAttachChildrenCount - i;
                View child_i = getChildAt(childPosition);
                // transY
                if (i < mRecyclerView.getVisibleCardCount()) {
                    child_i.setTranslationY(-2 * cardOffset * (float) Math.sqrt(factor));
                }
                // scale
                float scaleX_start = (child_i.getWidth() - i * cardOffset * 2) * 1f / child_i.getWidth();
                float scaleX_end = (child_i.getWidth() - (i - 1) * cardOffset * 2) * 1f / child_i.getWidth();
                float scaleY_start = (child_i.getHeight() - i * cardOffset * 2) * 1f / child_i.getHeight();
                float scaleY_end = (child_i.getHeight() - (i - 1) * cardOffset * 2) * 1f / child_i.getHeight();
                float currentScaleX = scaleX_start + (scaleX_end - scaleX_start) * (float) Math.sqrt(factor);
                float currentScaleY = scaleY_start + (scaleY_end - scaleY_start) * (float) Math.sqrt(factor);
                child_i.setScaleX(currentScaleX);
                child_i.setScaleY(currentScaleY);

                // calculate the current card ori elevation
                int current = mRecyclerView.getVisibleCardCount() - i;
                int oriElevation = mCardElevation * current;
                int currentElevation = (int) (oriElevation + mCardElevation * (float) Math.sqrt(factor));
                // update transZ
                ViewCompat.setTranslationZ(child_i, currentElevation);
            }
        }
    }

    private void dispatchOnDragEvent(View view, boolean isDragging, boolean isDropped,
                                     float offsetX, float offsetY) {
        if (null != mListener) {
            mListener.onDraggingStateChanged(view, isDragging, isDropped, offsetX, offsetY);
            if (isDragging)
                mListener.onCardDragging(view, offsetX, offsetY);
        }
    }
}

