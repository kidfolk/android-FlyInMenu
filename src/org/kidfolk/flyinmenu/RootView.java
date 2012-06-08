package org.kidfolk.flyinmenu;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.OverScroller;
import android.widget.Scroller;

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

	private VelocityTracker mVelocityTracker;
	private int mBezelSwipeWidth;
	private int mScreenWidth;
	private static final int ANIMATION_FRAME_DURATION = 1000 / 60;
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
				TypedValue.COMPLEX_UNIT_DIP, 44, getResources()
						.getDisplayMetrics());
		mBezelSwipeWidth = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 30, getResources()
						.getDisplayMetrics());
		mViewConfig = ViewConfiguration.get(context);
		mMaximumVelocity = mViewConfig.getScaledMaximumFlingVelocity();
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
		// mMenu.setVisibility(View.GONE);
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
		// return true;
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		return super.dispatchTouchEvent(ev);
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
	private int mActivePointerId = INVALID_POINTER;
	private float mMaximumVelocity;
	/**
	 * Sentinel value for no current active pointer. Used by
	 * {@link #mActivePointerId}.
	 */
	private static final int INVALID_POINTER = -1;

	/**
	 * when can a gesture be considered as a swipe
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		/**
		 * This method just determines whether we want ot intercept the motion.
		 * If we return true.onTouchEvent will be called and we do the actual
		 * scrolling there
		 */
		final int action = ev.getAction() & MotionEvent.ACTION_MASK;
		if (action == MotionEvent.ACTION_CANCEL
				|| action == MotionEvent.ACTION_UP) {
			mIsBeingDragged = false;
			mActivePointerId = INVALID_POINTER;
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			return false;
		}

		if (action != MotionEvent.ACTION_DOWN) {
			if (mIsBeingDragged) {
				return true;
			}
		}

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG,
					"onInterceptTouchEvent DOWN x:" + ev.getX() + ",y:"
							+ ev.getY());
			mStartX = ev.getX();
			mStartY = ev.getY();
			mLastX = ev.getX();
			mLastY = ev.getY();
			mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
			break;
		case MotionEvent.ACTION_MOVE:

			final int activePointerId = mActivePointerId;
			if (activePointerId == INVALID_POINTER) {
				// if we don't have a valid id,the touch down wasn't no content
				break;
			}
			final int pointerIndex = MotionEventCompat.findPointerIndex(ev,
					activePointerId);
			final float x = MotionEventCompat.getX(ev, pointerIndex);
			final float y = MotionEventCompat.getY(ev, pointerIndex);
			Log.d(TAG, "onInterceptTouchEvent MOVE x:" + x + ",y:" + y);

			mLastX = x;
			mLastY = y;

			double distance = Math.hypot(mLastX - mStartX, mLastY - mStartY);
			if (mStartX <= mBezelSwipeWidth
					&& distance > mViewConfig.getScaledTouchSlop()
					&& (mLastX - mStartX) > 0
					&& distance > mViewConfig.getScaledPagingTouchSlop()) {
				// open
				mIsBeingDragged = true;
			} else if (mStartX >= mScreenWidth - mBezelSwipeWidth
					&& distance > mViewConfig.getScaledTouchSlop()
					&& (mStartX - mLastX) > 0
					&& distance > mViewConfig.getScaledPagingTouchSlop()) {
				// close
				mIsBeingDragged = true;
			}
			break;
		}
		if (!mIsBeingDragged) {
			// track the velocity as long as we aren't dragging.
			// once we start a real drag we will track in onTouchEvent
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(ev);
		}
		return mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);
		final int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_MOVE: {
			Log.d(TAG,
					"onTouchEvent MOVE x:" + event.getX() + ",y:"
							+ event.getY());
			if (mIsBeingDragged) {
				final int activePointerIndex = MotionEventCompat
						.findPointerIndex(event, mActivePointerId);
				final float x = MotionEventCompat.getX(event,
						activePointerIndex);
				final float y = MotionEventCompat.getY(event,
						activePointerIndex);
				float distance = x - mLastX;
				Log.d(TAG, "onTouchEvent MOVE distance:" + distance);
				mHost.offsetLeftAndRight((int) distance);
				invalidate();
				mLastX = x;
				mLastY = y;
			}
			break;
		}
		case MotionEvent.ACTION_UP: {
			if (mIsBeingDragged) {
				final int activePointerIndex = MotionEventCompat
						.findPointerIndex(event, mActivePointerId);
				final float x = MotionEventCompat.getX(event,
						activePointerIndex);
				Log.d(TAG,
						"onTouchEvent UP x:" + event.getX() + ",y:"
								+ event.getY());
				float distance = x - mStartX;
				if (distance > mMenu.getMeasuredWidth() / 2) {
					if (!isOpened) {
						// open
						mScroller.startScroll(0, 0,
								-(mMenu.getMeasuredWidth() - mHost.getLeft()),
								0, 1000);
						mHandler.post(new Fling(true));
					} else {
						// close
						mScroller.startScroll(0, 0, mMenu.getMeasuredWidth()
								- mHost.getLeft(), 0, 1000);
						mHandler.post(new Fling(false));
					}
				} else {
					if (!isOpened) {
						// close
						mScroller.startScroll(0, 0, mHost.getLeft(), 0, 1000);
						mHandler.post(new Fling(false));
					} else {
						// open
						mScroller.startScroll(0, 0,
								-(mMenu.getMeasuredWidth() - mHost.getLeft()),
								0, 1000);
						mHandler.post(new Fling(true));
					}
				}
			}
			break;
		}
		case MotionEvent.ACTION_DOWN:
			mLastX = mStartX = event.getX();
			mLastY = mStartY = event.getY();
			mActivePointerId = MotionEventCompat.getPointerId(event, 0);
			break;
		case MotionEvent.ACTION_CANCEL:
			if (mIsBeingDragged) {
				mIsBeingDragged = false;
				mActivePointerId = INVALID_POINTER;
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			break;
		case MotionEvent.ACTION_POINTER_UP:
			break;
		}
		return true;
	}

	public void animateClose() {
		if (isMoving || isAnimating || !isOpened)
			return;
		mScroller.startScroll(0, 0, mMenu.getMeasuredWidth(), 0, 1000);
		mHandler.post(new Fling(false));
	}

	public void animateOpen() {
		if (isMoving || isAnimating || isOpened)
			return;
		mScroller.startScroll(0, 0, -mMenu.getMeasuredWidth(), 0, 1000);
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
			Log.d(TAG, "scroller x:" + x);
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
