/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Property;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.Switch;

import java.util.ArrayList;

public class EmojiTextCell extends FrameLayout {

    public final SimpleTextView textView;
    public final BackupImageView imageView;
    private int leftPadding;
    private boolean needDivider;
    private int offsetFromImage = 71;
    public int imageLeft = 21;
    private boolean inDialogs;
    private Switch checkBox;
    private boolean drawCheckRipple = true;

    private ObjectAnimator animator;
    private int height = 50;
    private int animatedColorBackground;
    private float animationProgress;
    private Paint animationPaint;
    private float lastTouchX;

    public static final Property<EmojiTextCell, Float> ANIMATION_PROGRESS = new AnimationProperties.FloatProperty<EmojiTextCell>("animationProgress") {
        @Override
        public Float get(EmojiTextCell emojiTextCell) {
            return emojiTextCell.animationProgress;
        }
        @Override
        public void setValue(EmojiTextCell object, float value) {
            object.setAnimationProgress(value);
            object.invalidate();
        }
    };

    public EmojiTextCell(Context context) {
        this(context, 23, false);
    }

    public EmojiTextCell(Context context, int left, boolean dialog) {
        super(context);

        leftPadding = left;

        textView = new SimpleTextView(context);
        textView.setTextColor(Theme.getColor(dialog ? Theme.key_dialogTextBlack : Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(16);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(textView);

        imageView = new BackupImageView(context);
        addView(imageView);

        checkBox = new Switch(context);
        checkBox.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        addView(checkBox);

        setFocusable(true);
    }

    public void setIsInDialogs() {
        inDialogs = true;
    }

    public SimpleTextView getTextView() {
        return textView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = AndroidUtilities.dp(48);

        textView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(71 + leftPadding) , MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));

        if (imageView.getVisibility() == VISIBLE) {
            imageView.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY));
        }
        if (checkBox.getVisibility() == VISIBLE) {
            checkBox.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(37+23+23), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
        }

        setMeasuredDimension(width, AndroidUtilities.dp(50) + (needDivider ? 1 : 0));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int height = bottom - top;
        int width = right - left;

        int viewTop;
        int viewLeft;

        viewTop = (height - textView.getTextHeight()) / 2;
        if (LocaleController.isRTL) {
            viewLeft = getMeasuredWidth() - textView.getMeasuredWidth() - AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? offsetFromImage : leftPadding);
        } else {
            viewLeft = AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? offsetFromImage : leftPadding);
        }
        textView.layout(viewLeft, viewTop, viewLeft + textView.getMeasuredWidth() - ((checkBox.getVisibility() == VISIBLE) ? AndroidUtilities.dp(37+22) : 0), viewTop + textView.getMeasuredHeight());

        if (imageView.getVisibility() == VISIBLE) {
            viewTop = AndroidUtilities.dp(8);
            viewLeft = !LocaleController.isRTL ? AndroidUtilities.dp(imageLeft) : width - imageView.getMeasuredWidth() - AndroidUtilities.dp(imageLeft);
            imageView.layout(viewLeft, viewTop, viewLeft + imageView.getMeasuredWidth(), viewTop + imageView.getMeasuredHeight());
        }
        if (checkBox.getVisibility() == VISIBLE) {
            viewTop = (height - checkBox.getMeasuredHeight()) / 2;
            viewLeft = LocaleController.isRTL ? 0 : width - checkBox.getMeasuredWidth();
            checkBox.layout(viewLeft, viewTop, viewLeft + checkBox.getMeasuredWidth(), viewTop + checkBox.getMeasuredHeight());
        }
    }
    @Override
    public void setPressed(boolean pressed) {
        if (drawCheckRipple) {
            checkBox.setDrawRipple(pressed);
        }
        super.setPressed(pressed);
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setColors(String icon, String text) {
        textView.setTextColor(Theme.getColor(text));
        textView.setTag(text);
    }
    public void setColors(String key, String switchKey, String switchKeyChecked, String switchThumb, String switchThumbChecked) {
        textView.setTextColor(Theme.getColor(key));
        checkBox.setColors(switchKey, switchKeyChecked, switchThumb, switchThumbChecked);
        textView.setTag(key);
    }

    public void setEnabled(boolean value, ArrayList<Animator> animators) {
        super.setEnabled(value);
        if (animators != null) {
            animators.add(ObjectAnimator.ofFloat(textView, "alpha", value ? 1.0f : 0.5f));
            animators.add(ObjectAnimator.ofFloat(checkBox, "alpha", value ? 1.0f : 0.5f));
            animators.add(ObjectAnimator.ofFloat(imageView, "alpha", value ? 1.0f : 0.5f));
        } else {
            textView.setAlpha(value ? 1.0f : 0.5f);
            checkBox.setAlpha(value ? 1.0f : 0.5f);
            imageView.setAlpha(value ? 1.0f : 0.5f);
        }
    }
    public void setChecked(boolean checked) {
        checkBox.setChecked(checked, true);
    }
    public void setChecked(boolean checked, boolean animated) {
        checkBox.setChecked(checked, animated);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }
    public void setBackgroundColorAnimated(boolean checked, int color) {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (animatedColorBackground != 0) {
            setBackgroundColor(animatedColorBackground);
        }
        if (animationPaint == null) {
            animationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        checkBox.setOverrideColor(checked ? 1 : 2);
        animatedColorBackground = color;
        animationPaint.setColor(animatedColorBackground);
        animationProgress = 0.0f;
        animator = ObjectAnimator.ofFloat(this, ANIMATION_PROGRESS, 0.0f, 1.0f);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setBackgroundColor(animatedColorBackground);
                animatedColorBackground = 0;
                invalidate();
            }
        });
        animator.setInterpolator(CubicBezierInterpolator.EASE_OUT);
        animator.setDuration(240).start();
    }
    private void setAnimationProgress(float value) {
        animationProgress = value;
        float rad = Math.max(lastTouchX, getMeasuredWidth() - lastTouchX) + AndroidUtilities.dp(40);
        float cx = lastTouchX;
        int cy = getMeasuredHeight() / 2;
        float animatedRad = rad * animationProgress;
        checkBox.setOverrideColorProgress(cx, cy, animatedRad);
    }

    public void setOffsetFromImage(int value) {
        offsetFromImage = value;
    }

    public void setTextAndValueAndIcon(String text, TLRPC.Document emoji, boolean checked, boolean divider) {
        checkBox.setVisibility(VISIBLE);
        checkBox.setChecked(checked, false);
        textView.setText(text);
        imageView.setVisibility(VISIBLE);
        imageView.setPadding(0, 0, 0, 0);

        TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(emoji.thumbs, 90);
        imageView.setImage(ImageLocation.getForDocument(thumb, emoji), null, "webp", null, this);

        needDivider = divider;
        setWillNotDraw(!needDivider);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? (inDialogs ? 72 : 68) : 20), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? (inDialogs ? 72 : 68) : 20) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        final CharSequence text = textView.getText();
        if (!TextUtils.isEmpty(text)) {
            info.setText(text);
        }
        info.setClassName("android.widget.Switch");
        info.setCheckable(true);
        info.setChecked(checkBox.isChecked());
        info.setContentDescription(checkBox.isChecked() ? LocaleController.getString("NotificationsOn", R.string.NotificationsOn) : LocaleController.getString("NotificationsOff", R.string.NotificationsOff));
    }

    public void setNeedDivider(boolean needDivider) {
        if (this.needDivider != needDivider) {
            this.needDivider = needDivider;
            setWillNotDraw(!needDivider);
            invalidate();
        }
    }
}
