package org.kidfolk.flyinmenu;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

public class RootView extends ViewGroup {

	private View mMenu;// 菜单
	private View mHost;// 内容
	private int mMenuId;
	private int mHostId;
	private int mHostRemainWidth;

	private OverScroller mScroller;
	private boolean isAnimating;
	private boolean isMoving;
	private boolean isOpened;

	private int mBezelSwipeWidth;
	private int mScreenWidth;
	private static final int ANIMATION_FRAME_DURATION = 1000 / 60;
	private static final int ANIMATION_DURATION = 500;
	private static final int HOST_REMAIN_WIDTH = 44;
	private static final int BEZEL_SWIPE_WIDTH = 30;
	private static final Interpolator sInterpolator = new Interpolator() {

		@Override
		public float getInterpolation(float input) {
			input -= 1.0f;
			return input * input * input * input * input + 1.0f;
		}

	};
	private static final String TAG = "RootView";

	public RootView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.RootView, defStyle, 0);
		int menuId = a.getResourceId(R.styleable.RootView_menu, 0);
		if (menuId == 0) {
			throw new IllegalArgumentException(
					"The menu attribute is required and must refer to a valid child.");
		}
		int hostId = a.getResourceId(R.styleable.RootView_host, 0);
		if (hostId == 0) {
			throw new IllegalArgumentException(
					"The host attribute is required and must refer to a valid child.");
		}
		mMenuId = menuId;
		mHostId = hostId;
		a.recycle();

		mHostRemainWidth = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, HOST_REMAIN_WIDTH, getResources()
						.getDisplayMetrics());
		mBezelSwipeWidth = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, BEZEL_SWIPE_WIDTH, getResources()
						.getDisplayMetrics());
		mViewConfig = ViewConfiguration.get(context);
		// mMaximumVelocity = mViewConfig.getScaledMaximumFlingVelocity();
		mScreenWidth = getResources().getDisplayMetrics().widthPixels;
		mScroller = new OverScroller(context, sInterpolator);
	}

	public RootView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	@Override
	protected void onFinishInflate() {
		Log.d(TAG, "onFinishInflate");
		mMenu = findViewById(mMenuId);
		if (mMenu == null) {
			throw new IllegalArgumentException(
					"The menu attribute is must refer to an existing child.");
		}
		mHost = findViewById(mHostId);
		if (mHost == null) {
			throw new IllegalArgumentException(
					"The host attribute is must refer to an existing child.");
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int maxHeight = 0;
		int maxWidth = 0;

		final View menu = mMenu;
		final View host = mHost;
		measureChildWithMargins(menu, widthMeasureSpec, mHostRemainWidth,
				heightMeasureSpec, 0);
		maxWidth = Math.max(menu.getMeasuredWidth(), maxWidth);
		maxHeight = Math.max(menu.getMeasuredHeight(), maxHeight);

		measureChildWithMargins(host, widthMeasureSpec, 0, heightMeasureSpec, 0);
		maxWidth = Math.max(host.getMeasuredWidth(), maxWidth);
		maxHeight = Math.max(host.getMeasuredHeight(), maxHeight);

		maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
		maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

		setMeasuredDimension(
				MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		Log.d(TAG, "onLayout");

		final View menu = mMenu;
		final int menuWidth = menu.getMeasuredWidth();
		final int menuHeight = menu.getMeasuredHeight();
		menu.layout(0, 0, menuWidth, menuHeight);

		final View host = mHost;
		final int hostWidth = host.getMeasuredWidth();
		final int hostHeight = host.getMeasuredHeight();
		host.layout(0, 0, hostWidth, hostHeight);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "keyCode:" + keyCode);
		return super.onKeyDown(keyCode, event);
	}

	private float mStartX;
	private float mStartY;
	private float mLastX;
	private float mLastY;
	private ViewConfiguration mViewConfig;
	private boolean mIsBeingDragged;
	/**
	 * ID of the active pointer. This is used to retain consistency during
	 * drags/flings if multiple pointers are used.
	 */
	private int mActivePointerId = INVALID_POINTER_ID;
	// private float mMaximumVelocity;
	/**
	 * Sentinel value for no current active pointer. Used by
	 * {@link #mActivePointerId}.
	 */
	private static final int INVALID_POINTER_ID = -1;

	/**
	 * when can a gesture be considered as a swipe
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		/**
		 * This method just determines whether we want to intercept the motion.
		 * If we return true.onTouchEvent will be called and we do the actual
		 * scrolling there
		 */
		final int action = ev.getAction() & MotionEvent.ACTION_MASK;

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "onInterceptTouchEvent: ACTION_DOWN");
			mLastX = mStartX = ev.getX();
			mLastY = mStartY = ev.getY();
			mActivePointerId = ev.getPointerId(0);
			mIsBeingDragged = false;
			break;
		case MotionEvent.ACTION_MOVE:
			final int pointerIndex = ev.findPointerIndex(mActivePointerId);
			final float x = ev.getX(pointerIndex);
			final float y = ev.getY(pointerIndex);

			double distance = Math.hypot(x - mStartX, y - mStartY);
			if (mStartX <= mBezelSwipeWidth
					&& distance > mViewConfig.getScaledTouchSlop()
					&& (x - mStartX) > 0
					&& distance > mViewConfig.getScaledPagingTouchSlop()
					&& !isOpened) {
				// open gesture
				mIsBeingDragged = true;
			}
			mLastX = x;
			mLastY = y;
			break;
		case MotionEvent.ACTION_CANCEL:
			Log.d(TAG, "onInterceptTouchEvent: ACTION_CANCEL");
			mIsBeingDragged = false;

		case MotionEvent.ACTION_POINTER_UP:
			Log.d(TAG, "onInterceptTouchEvent: ACTION_POINTER_UP");
			break;
		}
		return mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_MOVE: {
			// Log.d(TAG, "mActivePointerId:"+mActivePointerId);
			final int pointerIndex = event.findPointerIndex(mActivePointerId);
			// Log.d(TAG, "pointerIndex:"+pointerIndex);
			final float x = event.getX(pointerIndex);
			final float y = event.getY(pointerIndex);
			if (mIsBeingDragged) {
				float distance = x - mLastX;
				int right = mHost.getRight();
				if(right+distance<mHost.getMeasuredWidth()){
					//修正host左边界移出屏幕范围
					distance = mHost.getMeasuredWidth()-right;
				}
				mHost.offsetLeftAndRight((int) distance);
				postInvalidate();
			} else {
				double diff = Math.hypot(x - mStartX, y - mStartY);
				if (mStartX >= mScreenWidth - mBezelSwipeWidth
						&& (mStartX - x) > 0
						&& diff > mViewConfig.getScaledTouchSlop()
						&& diff > mViewConfig.getScaledPagingTouchSlop()
						&& isOpened) {
					mIsBeingDragged = true;
				}
			}
			mLastX = x;
			mLastY = y;
			break;
		}
		case MotionEvent.ACTION_UP: {
			Log.d(TAG, "onTouchEvent: ACTION_UP");
			if (mIsBeingDragged) {
				final int pointerIndex = event
						.findPointerIndex(mActivePointerId);
				final float x = event.getX(pointerIndex);
				float distance = Math.abs(x - mStartX);
				if (distance > mMenu.getMeasuredWidth() / 2) {
					if (!isOpened) {
						// open
						mScroller.startScroll(0, 0,
								-(mMenu.getMeasuredWidth() - mHost.getLeft()),
								0, ANIMATION_DURATION);
						mHandler.post(new Fling(true));
					} else {
						// close
						mScroller.startScroll(0, 0, mHost.getLeft(), 0,
								ANIMATION_DURATION);
						mHandler.post(new Fling(false));
					}
				} else {
					if (!isOpened) {
						// close
						mScroller.startScroll(0, 0, mHost.getLeft(), 0,
								ANIMATION_DURATION);
						mHandler.post(new Fling(false));
					} else {
						// open
						mScroller.startScroll(0, 0,
								-(mMenu.getMeasuredWidth() - mHost.getLeft()),
								0, ANIMATION_DURATION);
						mHandler.post(new Fling(true));
					}
				}
			}
			mActivePointerId = INVALID_POINTER_ID;
			break;
		}
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "onTouchEvent: ACTION_DOWN");
			mLastX = mStartX = event.getX();
			mLastY = mStartY = event.getY();
			mActivePointerId = event.getPointerId(0);
			return true;
		case MotionEvent.ACTION_CANCEL:
			Log.d(TAG, "onTouchEvent: ACTION_CANCEL");
			mIsBeingDragged = false;
			mActivePointerId = INVALID_POINTER_ID;
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			break;
		}
		case MotionEvent.ACTION_POINTER_UP: {
			final int pointerIndex = event.getActionIndex();
			final int pointerId = event.getPointerId(pointerIndex);
			if (pointerId == mActivePointerId) {
				// this is our active pointer going up.choose a new
				// active pointer an adjust accordingly.
				Log.d(TAG, "active pointer going up");
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				mLastX = event.getX(newPointerIndex);
				mLastY = event.getY(newPointerIndex);
				mActivePointerId = event.getPointerId(newPointerIndex);
			}
			break;
		}
		}
		return super.onTouchEvent(event);
	}

	public void animateClose() {
		if (isMoving || isAnimating || !isOpened)
			return;
		mScroller.startScroll(0, 0, mMenu.getMeasuredWidth(), 0,
				ANIMATION_DURATION);
		mHandler.post(new Fling(false));
	}

	public void animateOpen() {
		if (isMoving || isAnimating || isOpened)
			return;
		mScroller.startScroll(0, 0, -mMenu.getMeasuredWidth(), 0,
				ANIMATION_DURATION);
		mHandler.post(new Fling(true));
	}

	public void animateToggle() {
		if (isOpened) {
			animateClose();
		} else {
			animateOpen();
		}
	}

	public void close() {
		mHost.offsetLeftAndRight(-mMenu.getMeasuredWidth());
		isOpened = false;
	}

	public void open() {
		mHost.offsetLeftAndRight(mMenu.getMeasuredWidth());
		isOpened = true;
	}

	public void toggle() {
		if (isOpened) {
			close();
		} else {
			open();
		}
	}

	public boolean isOpened() {
		return isOpened;
	}

	public boolean isMoving() {
		return isMoving;
	}

	public boolean isAnimating() {
		return isAnimating;
	}

	private Handler mHandler = new Handler();

	class Fling implements Runnable {
		private int lastX;
		boolean open;

		Fling(boolean open) {
			this.open = open;
		}

		@Override
		public void run() {
			boolean more = mScroller.computeScrollOffset();
			int x = mScroller.getCurrX();
			int diff = x - lastX;
			if (diff != 0) {
				isAnimating = true;
				isMoving = true;
				mHost.offsetLeftAndRight(-diff);
				lastX = x;
				invalidate();
			}
			if (more) {
				mHandler.postDelayed(this, ANIMATION_FRAME_DURATION);
			} else {
				isAnimating = false;
				isMoving = false;
				mIsBeingDragged = false;
				if (open) {
					isOpened = true;
				} else {
					isOpened = false;
				}
			}

		}

	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new RootView.LayoutParams(getContext(), attrs);
	}

	@Override
	protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
	}

	public static class LayoutParams extends MarginLayoutParams {

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public LayoutParams(android.view.ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}

	}

}
