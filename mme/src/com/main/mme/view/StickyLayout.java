package com.main.mme.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import java.util.NoSuchElementException;

public class StickyLayout extends LinearLayout {
	private static final String TAG = "StickyLayout";
	private static final boolean DEBUG = true;

	int destHeight = 0;

	public interface OnGiveUpTouchEventListener {
		public boolean giveUpTouchEvent(MotionEvent event);
	}

	private View mHeader;
	private View mContent;
	private OnGiveUpTouchEventListener mGiveUpTouchEventListener;

	private int mOriginalHeaderHeight;//header高度
	private int mHeaderHeight;

	private int mStatus = STATUS_EXPANDED;
	public static final int STATUS_EXPANDED = 1;
	public static final int STATUS_COLLAPSED = 2;

	private int mTouchSlop;

	// 分别记录上次滑动的坐标
	private int mLastY = 0;
	int mLastX = 0;

	// 分别记录上次滑动的坐标(onInterceptTouchEvent)
	private int mLastXIntercept = 0;
	private int mLastYIntercept = 0;

	// 用来控制滑动角度，仅当角度a满足如下条件才进行滑动：tan a = deltaX / deltaY > 2
	private static final int TAN = 2;

	private boolean mIsSticky = true;
	private boolean mInitDataSucceed = false;
	private boolean mDisallowInterceptTouchEventOnHeader = true;

	public StickyLayout(Context context) {
		super(context);
	}

	public StickyLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public StickyLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		if (hasWindowFocus && (mHeader == null || mContent == null)) {
			initData();
		}
	}

	private void initData() {
		int headerId= getResources().getIdentifier("sticky_header", "id", getContext().getPackageName());
		int contentId = getResources().getIdentifier("sticky_content", "id", getContext().getPackageName());
		if (headerId != 0 && contentId != 0) {
			mHeader = findViewById(headerId);
			mContent = findViewById(contentId);
			mOriginalHeaderHeight = mHeader.getMeasuredHeight();
			mHeaderHeight = mOriginalHeaderHeight;
			mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
			if (mHeaderHeight > 0) {
				mInitDataSucceed = true;
			}
			if (DEBUG) {
				Log.d(TAG, "mTouchSlop = " + mTouchSlop + "mHeaderHeight = " + mHeaderHeight);
			}
		} else {
			throw new NoSuchElementException("Did your view with id \"sticky_header\" or \"sticky_content\" exists?");
		}
	}

	public void setOnGiveUpTouchEventListener(OnGiveUpTouchEventListener l) {
		mGiveUpTouchEventListener = l;
	}
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		Log.i("TAG", "StickyLayout....1");
		if (!mInitDataSucceed) {
			initData();
		}
		return super.dispatchTouchEvent(ev);
	}
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		int intercepted = 0;
		int x = (int) event.getX();
		int y = (int) event.getY();
		Log.i("TAG", "StickyLayout....2");
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				mLastXIntercept = x;
				mLastYIntercept = y;
				mLastX = x;
				mLastY = y;
				intercepted = 0;
				Log.i("TAG", "StickyLayout....3");
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				int deltaX = x - mLastXIntercept;
				int deltaY = y - mLastYIntercept;
				if (mDisallowInterceptTouchEventOnHeader && y <= getHeaderHeight()) {
					if (mHeaderHeight <= mOriginalHeaderHeight * 0.5) {
						if(deltaY > 0){
							intercepted = 1;
						}else{
							intercepted = 0;
						}
						Log.i("TAG", "StickyLayout....4");
					}else{
						intercepted = 0;
						Log.i("TAG", "StickyLayout....5");
					}
				}else if(mDisallowInterceptTouchEventOnHeader && y > getHeaderHeight()){
					if (mHeaderHeight > mOriginalHeaderHeight * 0.5 && mHeaderHeight<= mOriginalHeaderHeight) {
//					intercepted = 1;
						if(deltaY > 0){
							intercepted = 0;
						}else{
							intercepted = 1;
						}
						Log.i("TAG", "StickyLayout....4");
					}else{
						intercepted = 0;
						Log.i("TAG", "StickyLayout....5");
					}
				}else if (Math.abs(deltaY) <= Math.abs(deltaX)) {
					intercepted = 0;
					Log.i("TAG", "StickyLayout....6");
				} else if (mStatus == STATUS_EXPANDED && deltaY <= -mTouchSlop) {
					intercepted = 1;
					Log.i("TAG", "StickyLayout....7");
				} else if (mGiveUpTouchEventListener != null) {
					if (mGiveUpTouchEventListener.giveUpTouchEvent(event) && deltaY >= mTouchSlop) {
						if (mHeaderHeight > mOriginalHeaderHeight * 0.5 && mHeaderHeight<= mOriginalHeaderHeight) {
							intercepted = 1;
							Log.i("TAG", "StickyLayout....8");
						}else{
							intercepted = 0;
							Log.i("TAG", "StickyLayout....9");
						}
					}
				}
				break;
			}
			case MotionEvent.ACTION_UP: {
				intercepted = 0;


				mLastXIntercept = mLastYIntercept = 0;
				Log.i("TAG", "StickyLayout....10");
				break;
			}
			default:
				break;
		}

		if (DEBUG) {
			Log.d(TAG, "intercepted=" + intercepted);
		}
		return intercepted != 0 && mIsSticky;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!mIsSticky) {
			return true;
		}
		int x = (int) event.getX();
		int y = (int) event.getY();
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {

				break;
			}
			case MotionEvent.ACTION_MOVE: {
				int deltaX = x - mLastX;
				int deltaY = y - mLastY;
				if (DEBUG) {
					Log.d(TAG, "mHeaderHeight=" + mHeaderHeight + "  deltaY=" + deltaY + "  mlastY=" + mLastY);
				}

				mHeaderHeight += deltaY;

				if (mHeaderHeight <= mOriginalHeaderHeight * 0.5){
					setHeaderHeight(mOriginalHeaderHeight/2);
					Log.i("TAG", "StickyLayout....11");
					return false;
				}else{
					setHeaderHeight(mHeaderHeight);
					Log.i("TAG", "StickyLayout....11111");
				}
				break;
			}
			case MotionEvent.ACTION_UP: {
				// 这里做了下判断，当松开手的时候，会自动向两边滑动，具体向哪边滑，要看当前所处的位置           int destHeight = 0;
				if (mHeaderHeight <= mOriginalHeaderHeight * 0.5) {
					destHeight = mOriginalHeaderHeight/2;
					mStatus = STATUS_COLLAPSED;
					Log.i("TAG", "StickyLayout....12");
				} else {
					destHeight = mOriginalHeaderHeight;
					mStatus = STATUS_EXPANDED;
					Log.i("TAG", "StickyLayout....13");
				}
				// 慢慢滑向终点
				//			this.smoothSetHeaderHeight(mHeaderHeight, destHeight, 0);
				break;
			}
			default:
				break;
		}
		mLastX = x;
		mLastY = y;
		return false;
	}

	public void smoothSetHeaderHeight(final int from, final int to, long duration) {
		smoothSetHeaderHeight(from, to, duration, false);
	}

	public void smoothSetHeaderHeight(final int from, final int to, long duration, final boolean modifyOriginalHeaderHeight) {
		final int frameCount = (int) (duration / 1000f * 30) + 1;
		final float partation = (to - from) / (float) frameCount;
		new Thread("Thread#smoothSetHeaderHeight") {

			@Override
			public void run() {
				for (int i = 0; i < frameCount; i++) {
					final int height;
					if (i == frameCount - 1) {
						height = to;
					} else {
						height = (int) (from + partation * i);
					}
					post(new Runnable() {
						public void run() {
							setHeaderHeight(height);
						}
					});
					//try {
					//sleep(10);
					//}catch (InterruptedException e) {
					//e.printStackTrace();
					//}
				}

				if (modifyOriginalHeaderHeight) {
					setOriginalHeaderHeight(to);
				}
			};

		}.start();
	}

	public void setOriginalHeaderHeight(int originalHeaderHeight) {
		mOriginalHeaderHeight = originalHeaderHeight;
	}

	public void setHeaderHeight(int height, boolean modifyOriginalHeaderHeight) {
		if (modifyOriginalHeaderHeight) {
			setOriginalHeaderHeight(height);
		}
		setHeaderHeight(height);
	}

	public void setHeaderHeight(int height) {
		if (!mInitDataSucceed) {
			initData();
		}

		if (DEBUG) {
			Log.d(TAG, "setHeaderHeight height=" + height);
		}
		if (height <= 0) {
			height = 0;
		} else if (height > mOriginalHeaderHeight) {
			height = mOriginalHeaderHeight;
		}

		if (height == 0) {
			mStatus = STATUS_COLLAPSED;
		} else {
			mStatus = STATUS_EXPANDED;
		}

		if (mHeader != null && mHeader.getLayoutParams() != null) {
			mHeader.getLayoutParams().height = height;
			mHeader.requestLayout();
			mHeaderHeight = height;
		} else {
			if (DEBUG) {
				Log.e(TAG, "null LayoutParams when setHeaderHeight");
			}
		}
	}

	public int getHeaderHeight() {
		return mHeaderHeight;
	}

	public void setSticky(boolean isSticky) {
		mIsSticky = isSticky;
	}

	public void requestDisallowInterceptTouchEventOnHeader(boolean disallowIntercept) {
		mDisallowInterceptTouchEventOnHeader = disallowIntercept;
	}
}