package com.bobek.metronome.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/** 带有移动回调的布局，用于悬浮窗移动 */
public class TouchLinearLayout extends LinearLayout {
	/** 手指开始按下的坐标 */
	private float startX = 0;
	private float startY = 0;
	/** 上一个拖动点的位置坐标 */
	private float lastX = 0;
	private float lastY = 0;
	/** 是否处于拖动状态 */
	private boolean isDraged = false;

	public TouchLinearLayout(Context context) {
		super(context);
	}

	public TouchLinearLayout(Context context,@Nullable AttributeSet attrs) {
		super(context,attrs);
	}

	public TouchLinearLayout(Context context,@Nullable AttributeSet attrs,int defStyleAttr) {
		super(context,attrs,defStyleAttr);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		int action = ev.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				startX = ev.getX();
				startY = ev.getY();
				lastX = ev.getRawX();
				lastY = ev.getRawY();
				isDraged = false;
				return false;
			case MotionEvent.ACTION_MOVE:
				if (isDraged) {
					return true;
				} else {
					// 移动很小的一段距离也视为点击
					if (!(Math.abs(ev.getX() - startX) < 6) && !(Math.abs(ev.getY() - startY) < 6)) {
						isDraged = true;
						return true;
					}
				}
		}
		return false;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_MOVE: {
				if (onTouchCallbackListenerRef != null && onTouchCallbackListenerRef.get() != null) {
					onTouchCallbackListenerRef.get().onTouchCallback(ev.getRawX() - lastX, ev.getRawY() - lastY);
				}
				lastX = ev.getRawX();
				lastY = ev.getRawY();
				break;
			}
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_POINTER_UP: {
				break;
			}
		}
		return true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec,heightMeasureSpec);
	}

	/** 事件监听 */
	// private OnTouchCallbackListener onTouchCallbackListener;
	// 使用弱引用
	private WeakReference<OnTouchCallbackListener> onTouchCallbackListenerRef;


	// public void setOnTouchCallbackListener(OnTouchCallbackListener onTouchCallbackListener) {
	// 	this.onTouchCallbackListener = onTouchCallbackListener;
	// }

	// 设置监听器
	public void setOnTouchCallbackListener(OnTouchCallbackListener onTouchCallbackListener) {
		if (onTouchCallbackListener != null) {
			this.onTouchCallbackListenerRef = new WeakReference<>(onTouchCallbackListener);
		}
	}


	public interface OnTouchCallbackListener {
		void onTouchCallback(float x,float y);
	}

}
