package org.kidfolk.flyinmenu;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
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

public class RootView extends ViewGroup {

	private View mMenu;// 菜单
	private View mHost;// 内容
	private int mMenuId;
	private int mHostId;
	private int mHostRemainWidth;
	private int mBezelSwipeWidth;
	private int mScreenWidth;
	private int mShadowWidth;

	private OverScroller mScroller;
	private VelocityTracker mVelocityTracker;
	private int mState;
	private static final int MENU_CLOSED = 1;
	private static final int MENU_OPENED = 2;
	private static final int MENU_DRAGGING = 4;
	private static final int MENU_FLINGING = 8;
	private boolean mGestureToRight = false;

	private Drawable mShadowDrawable;
	private Paint mMenuOverlayPaint;

	private static final int ANIMATION_FRAME_DURATION = 1000 / 60;
	private static final int ANIMATION_DURATION = 500;
	private static final int HOST_REMAIN_WIDTH = 44;// dp
	private static final int BEZEL_SWIPE_WIDTH = 30;// dp
	private static final int SHADOW_WIDTH = 3;// dp
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
		mHostRemainWidth = (int) a.getDimension(
				R.styleable.RootView_host_remain_width, HOST_REMAIN_WIDTH);
		boolean open = a.getBoolean(R.styleable.RootView_open, false);
		if (open) {
			mState = MENU_OPENED;
		} else {
			mState = MENU_CLOSED;
		}
		a.recycle();

		mBezelSwipeWidth = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, BEZEL_SWIPE_WIDTH, getResources()
						.getDisplayMetrics());
		mShadowWidth = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, SHADOW_WIDTH, getResources()
						.getDisplayMetrics());
		mViewConfig = ViewConfiguration.get(context);
		mMaximumVelocity = mViewConfig.getScaledMaximumFlingVelocity();
		mMinimumVelocity = mViewConfig.getScaledMinimumFlingVelocity();
		Resources res = getResources();
		mScreenWidth = res.getDisplayMetrics().widthPixels;
		mScroller = new OverScroller(context, sInterpolator);
		mShadowDrawable = res.getDrawable(R.drawable.host_shadow);
		mShadowDrawable.setBounds(0, 0, mShadowWidth,
				res.getDisplayMetrics().heightPixels);
		mMenuOverlayPaint = new Paint();
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
		Log.d(TAG, "onMeasure");
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
		if (mState == MENU_OPENED) {
			host.layout(menuWidth, 0, menuWidth + hostWidth, hostHeight);
		} else {
			host.layout(0, 0, hostWidth, hostHeight);
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		if (mState != MENU_CLOSED) {
			// the menu is not closed.that means we can potentially see the host
			// overlapping it.let's add a tiny gradient to indicate the host is
			// sliding over the menu
			canvas.save();
			canvas.translate(mHost.getLeft()
					- mShadowDrawable.getBounds().right, 0);
			mShadowDrawable.draw(canvas);
			canvas.restore();

			final int menuWidth = mMenu.getWidth();
			if (menuWidth != 0) {
				final float opennessRatio = (menuWidth - mHost.getLeft())
						/ (float) menuWidth;

				// we also draw an overlay over the menu indicating the menu is
				// in the process of being visible or invisible
				drawMenuOverlay(canvas, opennessRatio);

				// finally we draw an arrow indicating the feature we are
				// currently int
				drawMenuArrow(canvas, opennessRatio);

				// offset menu to implement the parallax effect
				mMenu.offsetLeftAndRight((int) (-opennessRatio * menuWidth * PARALLAX_SPEED_RATIO)
						- mMenu.getLeft());
			}
		}
	}

	private static final float PARALLAX_SPEED_RATIO = 0.25f;

	public interface OnDrawMenuArrowListener {
		public void onDrawMenuArrow(Canvas canvas, float opennessRatio);
	}

	private OnDrawMenuArrowListener mOnDrawMenuArrowListener;

	public void setOnDrawMenuArrowListener(OnDrawMenuArrowListener listener) {
		this.mOnDrawMenuArrowListener = listener;
	}

	private void drawMenuArrow(Canvas canvas, float opennessRatio) {
		if (mOnDrawMenuArrowListener != null) {
			mOnDrawMenuArrowListener.onDrawMenuArrow(canvas, opennessRatio);
		}
	}

	private static final int MAXIMUM_MENU_ALPHA_OVERLAY = 170;

	private void drawMenuOverlay(Canvas canvas, float opennessRatio) {
		final Paint menuOverlayPaint = mMenuOverlayPaint;
		final int alpha = (int) (MAXIMUM_MENU_ALPHA_OVERLAY * opennessRatio);
		if (alpha > 0) {
			menuOverlayPaint.setColor(Color.argb(alpha, 0, 0, 0));
			canvas.drawRect(0, 0, mHost.getLeft(), getHeight(),
					menuOverlayPaint);
		}
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
	/**
	 * ID of the active pointer. This is used to retain consistency during
	 * drags/flings if multiple pointers are used.
	 */
	private int mActivePointerId = INVALID_POINTER_ID;
	private float mMaximumVelocity;
	private float mMinimumVelocity;
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
			break;
		case MotionEvent.ACTION_MOVE:
			final int pointerIndex = ev.findPointerIndex(mActivePointerId);
			final float x = ev.getX(pointerIndex);
			final float y = ev.getY(pointerIndex);

			double distance = Math.hypot(x - mStartX, y - mStartY);
			if (/*
				 * mStartX <= mBezelSwipeWidth &&
				 */distance > mViewConfig.getScaledTouchSlop()
					&& (x - mStartX) > 0
					&& distance > mViewConfig.getScaledPagingTouchSlop()
					&& mState == MENU_CLOSED) {
				// open gesture
				Log.d(TAG, "open gesture");
				mState |= MENU_DRAGGING;
			} else if (mStartX >= mScreenWidth - mHostRemainWidth
					&& (mStartX - x) > 0
					&& distance > mViewConfig.getScaledTouchSlop()
					&& distance > mViewConfig.getScaledPagingTouchSlop()
					&& mState == MENU_OPENED) {
				Log.d(TAG, "close gesture");
				mState |= MENU_DRAGGING;
			}
			mLastX = x;
			mLastY = y;
			break;
		case MotionEvent.ACTION_CANCEL:
			Log.d(TAG, "onInterceptTouchEvent: ACTION_CANCEL");
			break;

		case MotionEvent.ACTION_POINTER_UP:
			Log.d(TAG, "onInterceptTouchEvent: ACTION_POINTER_UP");
			break;
		}

		if ((mState & MENU_DRAGGING) == 0) {
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(ev);
		}

		return (mState & MENU_DRAGGING) != 0 && (mState & MENU_FLINGING) == 0;
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
			final int pointerIndex = event.findPointerIndex(mActivePointerId);
			final float x = event.getX(pointerIndex);
			final float y = event.getY(pointerIndex);
			if ((mState & MENU_DRAGGING) != 0) {
				float distance = x - mLastX;
				int right = mHost.getRight();
				if (right + distance < mHost.getMeasuredWidth()) {
					// 修正host左边界移出屏幕范围
					distance = mHost.getMeasuredWidth() - right;
				}
				int left = mHost.getLeft();
				if (left + distance > mMenu.getMeasuredWidth()) {
					// 修正menu右边界超过menu的宽度
					distance = mMenu.getMeasuredWidth() - left;
				}

				// 记录最终手势方向
				if (distance > 0) {
					// user want open
					mGestureToRight = true;
				} else {
					// user want close
					mGestureToRight = false;
				}

				mHost.offsetLeftAndRight((int) distance);
				postInvalidate();
			}
			mLastX = x;
			mLastY = y;
			break;
		}
		case MotionEvent.ACTION_UP: {
			Log.d(TAG, "onTouchEvent: ACTION_UP");
			// doActionUpWithVelocityAndThreshold(event);
			doActionUpJustWithGesutureDirection(event);
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
				if (mVelocityTracker != null) {
					mVelocityTracker.clear();
				}
			}
			break;
		}
		}
		return super.onTouchEvent(event);
	}

	private void doActionUpWithVelocityAndThreshold(MotionEvent event) {
		if ((mState & MENU_DRAGGING) != 0) {
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
			float xVelocity = Math.abs(velocityTracker.getXVelocity());
			if (xVelocity > mMinimumVelocity) {
				// velocity is greater than the minimum velocity
				if ((mState & MENU_CLOSED) != 0) {
					// open
					startScroll(0, 0,
							-(mMenu.getMeasuredWidth() - mHost.getLeft()), 0);
				} else if ((mState & MENU_OPENED) != 0) {
					// close
					startScroll(0, 0, mHost.getLeft(), 0);
				}
			} else {
				final int pointerIndex = event
						.findPointerIndex(mActivePointerId);
				final float x = event.getX(pointerIndex);
				float diff = Math.abs(x - mStartX);
				if (diff > mMenu.getMeasuredWidth() / 2) {
					if ((mState & MENU_CLOSED) != 0) {
						// open
						startScroll(0, 0,
								-(mMenu.getMeasuredWidth() - mHost.getLeft()),
								0);
					} else if ((mState & MENU_OPENED) != 0) {
						// close
						startScroll(0, 0, mHost.getLeft(), 0);
					}
				} else {
					if ((mState & MENU_CLOSED) != 0) {
						// close
						startScroll(0, 0, mHost.getLeft(), 0);
					} else if ((mState & MENU_OPENED) != 0) {
						// open
						startScroll(0, 0,
								-(mMenu.getMeasuredWidth() - mHost.getLeft()),
								0);
					}
				}
			}
		}
		mActivePointerId = INVALID_POINTER_ID;
	}

	private void doActionUpJustWithGesutureDirection(MotionEvent event) {
		if (mGestureToRight) {
			startScroll(0, 0, -(mMenu.getMeasuredWidth() - mHost.getLeft()), 0);
		} else {
			startScroll(0, 0, mHost.getLeft(), 0);
		}
	}

	private void startScroll(int startX, int startY, int dx, int dy) {
		mScroller.startScroll(startX, startY, dx, dy, ANIMATION_DURATION);
		mHandler.post(new Scrolling(dx > 0 ? false : true));
	}

	public void animateClose() {
		if ((mState & MENU_CLOSED) != 0 || (mState & MENU_FLINGING) != 0)
			return;
		startScroll(0, 0, mMenu.getMeasuredWidth(), 0);
	}

	public void animateOpen() {
		if ((mState & MENU_OPENED) != 0 || (mState & MENU_FLINGING) != 0)
			return;
		startScroll(0, 0, -mMenu.getMeasuredWidth(), 0);
	}

	public void animateToggle() {
		if ((mState & MENU_OPENED) != 0) {
			animateClose();
		} else if ((mState & MENU_CLOSED) != 0) {
			animateOpen();
		}
	}

	public void close() {
		mHost.offsetLeftAndRight(-mMenu.getMeasuredWidth());
		mState = MENU_CLOSED;
	}

	public void open() {
		mHost.offsetLeftAndRight(mMenu.getMeasuredWidth());
		mState = MENU_OPENED;
	}

	public void toggle() {
		if (mState == MENU_OPENED) {
			close();
		} else if (mState == MENU_CLOSED) {
			open();
		}
	}

	public boolean isOpened() {
		return mState == MENU_OPENED;
	}

	public boolean isDragging() {
		return (mState & MENU_DRAGGING) != 0;
	}

	public boolean isFlinging() {
		return (mState & MENU_FLINGING) != 0;
	}

	private Handler mHandler = new Handler();

	class Scrolling implements Runnable {
		private int lastX;
		boolean open;

		Scrolling(boolean open) {
			this.open = open;
		}

		@Override
		public void run() {
			boolean more = mScroller.computeScrollOffset();
			int x = mScroller.getCurrX();
			int diff = x - lastX;
			if (diff != 0) {
				if ((mState & MENU_DRAGGING) != 0) {
					mState ^= MENU_DRAGGING;
				}
				if ((mState & MENU_FLINGING) == 0) {
					mState |= MENU_FLINGING;
				}
				mHost.offsetLeftAndRight(-diff);
				lastX = x;
				postInvalidate();
			}
			if (more) {
				mHandler.postDelayed(this, ANIMATION_FRAME_DURATION);
			} else {
				if (open) {
					mState = MENU_OPENED;
				} else {
					mState = MENU_CLOSED;
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
