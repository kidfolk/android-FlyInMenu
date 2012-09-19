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
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

/**
 * 左侧菜单视图
 * 
 * @author 徐武进
 * 
 */
public class RootView extends ViewGroup {

	private View mMenu;// 菜单
	private View mHost;// 内容
	private int mMenuId;// 菜单id
	private int mHostId;// 内容id
	private int mHostRemainWidth;// 菜单打开后host还保留的宽度
	private int mBezelSwipeWidth;
	private int mScreenWidth;// 屏幕宽度
	private int mShadowWidth;// 屏幕高度

	private OverScroller mScroller;
	private VelocityTracker mVelocityTracker;
	private int mState = MENU_CLOSED;
	private static final int MENU_CLOSED = 1;
	private static final int MENU_OPENED = 2;
	private static final int MENU_DRAGGING = 4;
	private static final int MENU_FLINGING = 8;
	private boolean mGestureToRight = false;// 记录是否是向右的打开菜单手势

	private Drawable mShadowDrawable;
	private Paint mMenuOverlayPaint;

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
	private int mMenuHeight;
	private int mMenuWidth;
	private int mHostWidth;
	private int mHostHeight;
	/**
	 * Sentinel value for no current active pointer. Used by
	 * {@link #mActivePointerId}.
	 */
	private static final int INVALID_POINTER_ID = -1;

	private static final float PARALLAX_SPEED_RATIO = 0.25f;
	private static final int MAXIMUM_MENU_ALPHA_OVERLAY = 170;

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

		init(context);
	}

	/**
	 * 初始化参数信息
	 * 
	 * @param context
	 */
	private void init(Context context) {
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

	public RootView(Context context) {
		super(context);
		init(context);
	}

	@Override
	protected void onFinishInflate() {
		setMenu(mMenuId);
		setHost(mHostId);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int maxHeight = 0;
		int maxWidth = 0;

		final View menu = mMenu;
		final View host = mHost;
		measureChildWithMargins(menu, widthMeasureSpec, mHostRemainWidth,
				heightMeasureSpec, 0);
		if (mMenuWidth * mMenuHeight == 0) {
			mMenuWidth = menu.getMeasuredWidth();
			mMenuHeight = menu.getMeasuredHeight();
		}
		maxWidth = Math.max(mMenuWidth, maxWidth);
		maxHeight = Math.max(mMenuHeight, maxHeight);

		measureChildWithMargins(host, widthMeasureSpec, 0, heightMeasureSpec, 0);
		if (mHostWidth * mHostHeight == 0) {
			mHostWidth = host.getMeasuredWidth();
			mHostHeight = host.getMeasuredHeight();
		}
		maxWidth = Math.max(mHostWidth, maxWidth);
		maxHeight = Math.max(mHostHeight, maxHeight);

		maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
		maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

		setMeasuredDimension(
				MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final View menu = mMenu;
		final int menuWidth = mMenuWidth;
		final int menuHeight = mMenuHeight;
		menu.layout(0, 0, menuWidth, menuHeight);

		final View host = mHost;
		final int hostWidth = mHostWidth;
		final int hostHeight = mHostHeight;
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

			final int menuWidth = mMenuWidth;
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

	private void drawMenuOverlay(Canvas canvas, float opennessRatio) {
		final Paint menuOverlayPaint = mMenuOverlayPaint;
		final int alpha = (int) (MAXIMUM_MENU_ALPHA_OVERLAY * opennessRatio);
		if (alpha > 0) {
			menuOverlayPaint.setColor(Color.argb(alpha, 0, 0, 0));
			canvas.drawRect(0, 0, mHost.getLeft(), getHeight(),
					menuOverlayPaint);
		}
	}

	public interface OnMenuOpenListener {
		public boolean ignoreOpen(MotionEvent event);
	}

	public interface OnMenuCloseListener {

	}

	public OnMenuOpenListener mMenuOpenListener;

	public void setOnMenuOpenListener(OnMenuOpenListener listener) {
		this.mMenuOpenListener = listener;
	}

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
		if (!isEnabled()) {
			return false;
		}
		if (isFlinging()) {
			return false;
		}

		final int action = ev.getAction() & MotionEvent.ACTION_MASK;

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			// Log.d(TAG, "onInterceptTouchEvent: ACTION_DOWN");
			mLastX = ev.getX();
			mLastY = ev.getY();
			mActivePointerId = ev.getPointerId(0);
			break;
		case MotionEvent.ACTION_MOVE:
			// Log.d(TAG, "onInterceptTouchEvent: ACTION_MOVE");
			if (mActivePointerId != INVALID_POINTER_ID) {
				final int pointerIndex = ev.findPointerIndex(mActivePointerId);
				final float x = ev.getX(pointerIndex);
				final float y = ev.getY(pointerIndex);

				float xDiff = Math.abs(x - mLastX);
				float yDiff = Math.abs(y - mLastY);
				double distance = Math.hypot(xDiff, yDiff);

				if (distance > mViewConfig.getScaledTouchSlop()
						&& (x - mLastX) > 0
						&& distance > mViewConfig.getScaledPagingTouchSlop()
						&& mState == MENU_CLOSED && xDiff > yDiff) {
					// open gesture
					if (this.mMenuOpenListener != null) {
						if (this.mMenuOpenListener.ignoreOpen(ev)) {

						} else {
							mState |= MENU_DRAGGING;
							mLastX = x;
							mLastY = y;
						}
					} else {
						mState |= MENU_DRAGGING;
						mLastX = x;
						mLastY = y;
					}
				} else if (mLastX >= mScreenWidth - mHostRemainWidth
						&& (mLastX - x) > 0
						&& distance > mViewConfig.getScaledTouchSlop()
						&& distance > mViewConfig.getScaledPagingTouchSlop()
						&& mState == MENU_OPENED && xDiff > yDiff) {
					// close gesture
					mState |= MENU_DRAGGING;
					mLastX = x;
					mLastY = y;
				}
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			// Log.d(TAG, "onInterceptTouchEvent: ACTION_CANCEL");
			break;

		case MotionEvent.ACTION_POINTER_UP:
			// Log.d(TAG, "onInterceptTouchEvent: ACTION_POINTER_UP");
			onSecondaryPointerUp(ev);
			break;
		}

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		return isDragging();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		if (isFlinging()) {
			return true;
		}
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);
		final int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_MOVE: {
			if (mActivePointerId != INVALID_POINTER_ID) {
				final int pointerIndex = event
						.findPointerIndex(mActivePointerId);
				final float x = event.getX(pointerIndex);
				final float y = event.getY(pointerIndex);
				if (isDragging()) {
					float distance = x - mLastX;
					int right = mHost.getRight();
					if (right + distance < mHostWidth) {
						// 修正host左边界移出屏幕范围
						distance = mHostWidth - right;
					}
					int left = mHost.getLeft();
					if (left + distance > mMenuWidth) {
						// 修正menu右边界超过menu的宽度
						distance = mMenuWidth - left;
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
			}
			break;
		}
		case MotionEvent.ACTION_UP: {
			// Log.d(TAG, "onTouchEvent: ACTION_UP");
			doActionUpJustWithGesutureDirection(event);
			mActivePointerId = INVALID_POINTER_ID;
			break;
		}
		case MotionEvent.ACTION_DOWN:
			// Log.d(TAG, "onTouchEvent: ACTION_DOWN");
			mLastX = event.getX();
			mLastY = event.getY();
			mActivePointerId = event.getPointerId(0);
			return true;
		case MotionEvent.ACTION_CANCEL:
			// Log.d(TAG, "onTouchEvent: ACTION_CANCEL");
			mActivePointerId = INVALID_POINTER_ID;
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			// Log.d(TAG, "onTouchEvent: ACTION_POINTER_DOWN");
			break;
		}
		case MotionEvent.ACTION_POINTER_UP: {
			// Log.d(TAG, "onTouchEvent: ACTION_POINTER_UP");
			onSecondaryPointerUp(event);
			break;
		}
		}
		return super.onTouchEvent(event);
	}

	private void onSecondaryPointerUp(MotionEvent event) {
		final int pointerIndex = event.getActionIndex();
		final int pointerId = event.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			// this is our active pointer going up.choose a new
			// active pointer an adjust accordingly.
			// Log.d(TAG, "active pointer going up");
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastX = event.getX(newPointerIndex);
			mLastY = event.getY(newPointerIndex);
			mActivePointerId = event.getPointerId(newPointerIndex);
			if (mVelocityTracker != null) {
				mVelocityTracker.clear();
			}
		}
	}

	@SuppressWarnings("unused")
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
				float diff = Math.abs(x - mLastX);
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
			startScroll(0, 0, -(mMenuWidth - mHost.getLeft()), 0);
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
		this.post(new Runnable() {

			@Override
			public void run() {
				startScroll(0, 0, mMenuWidth, 0);
			}
		});

	}

	public void animateOpen() {
		if ((mState & MENU_OPENED) != 0 || (mState & MENU_FLINGING) != 0)
			return;
		this.post(new Runnable() {

			@Override
			public void run() {
				startScroll(0, 0, -mMenuWidth, 0);
			}
		});

	}

	public void animateToggle() {
		if ((mState & MENU_OPENED) != 0) {
			animateClose();
		} else if ((mState & MENU_CLOSED) != 0) {
			animateOpen();
		}
	}

	public void close() {
		if (mState == MENU_CLOSED)
			return;
		mHost.offsetLeftAndRight(-mMenuWidth);
		mState = MENU_CLOSED;
	}

	public void open() {
		if (mState == MENU_OPENED)
			return;
		mHost.offsetLeftAndRight(mMenuWidth);
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
			if (more) {
				int x = mScroller.getCurrX();
				int diff = x - lastX;
				Log.d(TAG, "diff: " + diff + ",lastX: " + lastX);
				if (diff != 0) {
					if (isDragging()) {
						// 如果当前视图正在拖动，则当用户松手之后重置拖动状态
						mState ^= MENU_DRAGGING;
					}
					if (!isFlinging()) {
						mState |= MENU_FLINGING;
					}
					if (isFlinging()) {
						mHost.offsetLeftAndRight(-diff);
						lastX = x;
						postInvalidate();
					}
				}
				mHandler.postDelayed(this, ANIMATION_FRAME_DURATION);
			} else {
				mHandler.removeCallbacks(this);
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

	/**
	 * 设置菜单视图
	 * 
	 * @param menuId
	 *            菜单视图的id
	 */
	public void setMenu(int menuId) {
		if (menuId == 0) {
			throw new IllegalArgumentException(
					"The menu attribute is required and must refer to a valid child.");
		}
		mMenuId = menuId;
		mMenu = findViewById(menuId);
		if (mMenu == null) {
			throw new IllegalArgumentException(
					"The menu attribute is must refer to an existing child.");
		}
	}

	/**
	 * 设置内容视图
	 * 
	 * @param hostId
	 *            内容视图的id
	 */
	public void setHost(int hostId) {
		if (hostId == 0) {
			throw new IllegalArgumentException(
					"The host attribute is required and must refer to a valid child.");
		}
		mHostId = hostId;
		mHost = findViewById(hostId);
		if (mHost == null) {
			throw new IllegalArgumentException(
					"The host attribute is must refer to an existing child.");
		}
	}

	public void setMenu(View menu) {
		mMenuId = menu.getId();
		mMenu = menu;
	}

	public void setHost(View host) {
		mHostId = host.getId();
		mHost = host;
	}

}
