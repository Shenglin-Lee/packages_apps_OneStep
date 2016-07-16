package com.smartisanos.sidebar.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.smartisanos.sidebar.R;
import com.smartisanos.sidebar.SidebarController;
import com.smartisanos.sidebar.SidebarMode;
import com.smartisanos.sidebar.util.LOG;
import com.smartisanos.sidebar.util.ResolveInfoGroup;
import com.smartisanos.sidebar.util.ResolveInfoManager;
import com.smartisanos.sidebar.util.anim.Anim;
import com.smartisanos.sidebar.util.anim.AnimInterpolator;
import com.smartisanos.sidebar.util.anim.AnimListener;
import com.smartisanos.sidebar.util.anim.AnimTimeLine;
import com.smartisanos.sidebar.util.anim.Vector3f;

public class AddItemViewGroup extends LinearLayout implements ContentView.ISubView {
    private static final LOG log = LOG.getInstance(AddItemViewGroup.class);

    private FullGridView mContacts, mResolveInfos;
    private BaseAdapter mAddContactAdapter, mResolveInfoAdapter;
    private TextView mAddContactTitle, mAddResolveTitle;

    public AddItemViewGroup(Context context) {
        this(context, null);
    }

    public AddItemViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AddItemViewGroup(Context context, AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AddItemViewGroup(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContacts = (FullGridView)findViewById(R.id.contact_gridview);
        mAddContactAdapter = new AddContactAdapter(mContext);
        mContacts.setAdapter(mAddContactAdapter);
        mResolveInfos = (FullGridView) findViewById(R.id.resolveinfo_gridview);
        mResolveInfoAdapter = new AddResolveInfoGroupAdapter(mContext, this, findViewById(R.id.add_resolve));
        mResolveInfos.setAdapter(mResolveInfoAdapter);
        mAddContactTitle = (TextView) findViewById(R.id.add_contact_title_view);
        mAddResolveTitle = (TextView) findViewById(R.id.add_resolve_title_view);
    }

    public void removeItemAtIndex(final int index, boolean withAnim) {
        if (mResolveInfos == null) {
            log.error("removeItemAtIndex return by mResolveInfos is null");
            return;
        }
        int childCount = mResolveInfos.getChildCount();
        if (index >= childCount) {
            log.error("removeItemAtIndex over count limited. count ["+childCount+"], index ["+index+"]");
            return;
        }

        final ResolveInfoGroup item = (ResolveInfoGroup) mResolveInfoAdapter.getItem(index);
        log.error("remove item ["+item.getDisplayName()+"], index ["+index+"]");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ResolveInfoManager.getInstance(mContext).addResolveInfoGroup(item);
            }
        };

        if (withAnim) {
            removeItemAnim(index, runnable);
        } else {
            runnable.run();
        }
    }

    private boolean removeAnimRunning = false;

    private void removeItemAnim(int index, final Runnable runnable) {
        if (removeAnimRunning) {
            log.error("removeItemAnim return by anim running");
            return;
        }
        removeAnimRunning = true;
        int childCount = mResolveInfos.getChildCount();
        if (childCount == 0) {
            return;
        }
        final View child = mResolveInfos.getChildAt(index);
        child.setScaleX(1.1f);
        child.setScaleY(1.1f);

        AnimTimeLine timeLine = new AnimTimeLine();
        int time = 200;
        Anim scaleAnim = new Anim(child, Anim.SCALE, time, Anim.CUBIC_OUT, new Vector3f(1, 1), new Vector3f(0.3f, 0.3f));
        Anim alphaAnim = new Anim(child, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, new Vector3f(0, 0, 1), new Vector3f());
        timeLine.addAnim(scaleAnim);
        timeLine.addAnim(alphaAnim);

        if (index + 1 < childCount) {
            for (int i = index; i < childCount; i++) {
                if (i + 1 < childCount) {
                    View view = mResolveInfos.getChildAt(i);
                    View nextView = mResolveInfos.getChildAt(i + 1);
                    Vector3f from = new Vector3f(nextView.getX(), nextView.getY());
                    Vector3f to = new Vector3f(view.getX(), view.getY());
                    Anim moveAnim = new Anim(nextView, Anim.TRANSLATE, time, Anim.CUBIC_OUT, from, to);
                    timeLine.addAnim(moveAnim);
                }
            }
        }
        timeLine.setAnimListener(new AnimListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onComplete(int type) {
                if (runnable != null) {
                    runnable.run();
                }
                removeAnimRunning = false;
            }
        });
        timeLine.start();
    }

    public void show(boolean anim){
        setVisibility(View.VISIBLE);
        if (anim) {
            boolean isLeft = SidebarController.getInstance(mContext).getSidebarMode() == SidebarMode.MODE_LEFT;
            ValueAnimator animator = new ValueAnimator();
            animator.setFloatValues(0f, 1f);
            animator.setDuration(300);
            setPivotX(isLeft ? 0 : getWidth());
            setPivotY(0);
            animator.setInterpolator(new AnimInterpolator.Interpolator(Anim.CUBIC_OUT));
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float percent = (Float) valueAnimator.getAnimatedValue();
                    float scaleX = 0.6f + (0.4f * percent);
                    float scaleY = 0.6f + (0.4f * percent);
                    setScaleX(scaleX);
                    setScaleY(scaleY);
                    setAlpha(percent);
                    setX(0);
                }
            });
            animator.start();
        }
    }

    public void dismiss(boolean anim){
        if (anim) {
            boolean isLeft = SidebarController.getInstance(mContext).getSidebarMode() == SidebarMode.MODE_LEFT;
            SidebarController.getInstance(mContext).getSideView().clickAddButtonAnim(isLeft, false, null);
            ValueAnimator animator = new ValueAnimator();
            animator.setFloatValues(0f, 1f);
            animator.setDuration(300);
            setPivotX(isLeft ? 0 : getWidth());
            setPivotY(0);
            animator.setInterpolator(new AnimInterpolator.Interpolator(Anim.CUBIC_OUT));
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float percent = (Float) valueAnimator.getAnimatedValue();
                    float scaleX = 1 - (0.4f * percent);
                    float scaleY = 1 - (0.4f * percent);
                    setScaleX(scaleX);
                    setScaleY(scaleY);
                    setAlpha(1 - percent);
                    setX(0);
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    setVisibility(View.INVISIBLE);
                    setScaleX(1);
                    setScaleY(1);
                    setAlpha(1);
                }
            });
            animator.start();
        } else {
            setVisibility(View.INVISIBLE);
        }
    }

    private void updateUI(){
        mAddContactTitle.setText(R.string.add_contact_shortcut);
        mAddContactAdapter.notifyDataSetChanged();
        mAddResolveTitle.setText(R.string.add_application_shortcut);
        mResolveInfoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updateUI();
    }
}
