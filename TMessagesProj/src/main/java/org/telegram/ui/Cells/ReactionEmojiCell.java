/*
 * This is the source code of Telegram for Android v. 5.x.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;

public class ReactionEmojiCell extends FrameLayout {

    private BackupImageView imageView;
    private TLRPC.Document sticker;
    private Object parentObject;
    private float alpha = 1;
    private boolean changingAlpha;
    private long lastUpdateTime;
    private boolean scaled;
    private float scale;
    private long time;
    private boolean recent;
    private static AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);

    // Todo: prevent autoplay ffs

    public ReactionEmojiCell(Context context, int size) {
        super(context);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView, LayoutHelper.createFrame(size, size, Gravity.CENTER)); // was 42

        setFocusable(true);

        RLottieDrawable drawable = imageView.getImageReceiver().getLottieAnimation();
        if (drawable != null) {
            drawable.setAutoRepeat(0);
            drawable.stop();
            drawable.setCurrentFrame(1);
        }
        invalidate();
        imageView.invalidate();
    }

    public TLRPC.Document getSticker() {
        return sticker;
    }

    public Object getParentObject() {
        return parentObject;
    }

    public boolean isRecent() {
        return recent;
    }

    public void setRecent(boolean value) {
        recent = value;
    }

    public MessageObject.SendAnimationData getSendAnimationData() {
        ImageReceiver imageReceiver = imageView.getImageReceiver();
        if (!imageReceiver.hasNotThumb()) {
            return null;
        }
        MessageObject.SendAnimationData data = new MessageObject.SendAnimationData();
        int[] position = new int[2];
        imageView.getLocationInWindow(position);
        data.x = imageReceiver.getCenterX() + position[0];
        data.y = imageReceiver.getCenterY() + position[1];
        data.width = imageReceiver.getImageWidth();
        data.height = imageReceiver.getImageHeight();
        return data;
    }

    public void setSticker(TLRPC.Document document, Object parent) {
        if (document != null) {
            sticker = document;
            parentObject = parent;
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
            SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document,  Theme.key_windowBackgroundGray, 1.0f);

            if (MessageObject.canAutoplayAnimatedSticker(document)) {
                if (svgThumb != null) {
                    // this is being used
                    imageView.setImage(ImageLocation.getForDocument(document), "66_66", null, svgThumb, parentObject);
                } else if (thumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "66_66", ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
                } else {
                    imageView.setImage(ImageLocation.getForDocument(document), "66_66", null, null, parentObject);
                }
            } else {
                if (svgThumb != null) {
                    if (thumb != null) {
                        // this is being used
                        imageView.setImage(ImageLocation.getForDocument(thumb, document), null, "webp", svgThumb, parentObject);
                    } else {
                        imageView.setImage(ImageLocation.getForDocument(document), null, "webp", svgThumb, parentObject);
                    }
                } else if (thumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(thumb, document), null, "webp", null, parentObject);
                } else {
                    imageView.setImage(ImageLocation.getForDocument(document), null, "webp", null, parentObject);
                }
            }

            RLottieDrawable drawable = imageView.getImageReceiver().getLottieAnimation();
            if (drawable != null) {
                drawable.stop();
                drawable.setAutoRepeat(0);
                drawable.setCurrentFrame(1);
            }
            invalidate();
            imageView.invalidate();
        }
    }

    public void disable() {
        changingAlpha = true;
        alpha = 0.5f;
        time = 0;
        imageView.getImageReceiver().setAlpha(alpha);
        imageView.invalidate();
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }
    public void playOnce() {
        RLottieDrawable drawable = imageView.getImageReceiver().getLottieAnimation();
        if (drawable != null) {
            drawable.start();
            drawable.setCurrentFrame(1);
        }
        invalidate();
    }
    public void stopPlaying() {
        RLottieDrawable drawable = imageView.getImageReceiver().getLottieAnimation();
        if (drawable != null) {
            drawable.stop();
            drawable.setAutoRepeat(0);
            drawable.setCurrentFrame(1);
        }
        invalidate();
    }

    public void setScaled(boolean value) {
        scaled = value;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public boolean isDisabled() {
        return changingAlpha;
    }

    public BackupImageView getImageView() {
        return imageView;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = super.drawChild(canvas, child, drawingTime);
        if (child == imageView && (changingAlpha || scaled && scale != 0.8f || !scaled && scale != 1.0f)) {
            long newTime = System.currentTimeMillis();
            long dt = (newTime - lastUpdateTime);
            lastUpdateTime = newTime;
            if (changingAlpha) {
                time += dt;
                if (time > 1050) {
                    time = 1050;
                }
                alpha = 0.5f + interpolator.getInterpolation(time / 1050.0f) * 0.5f;
                if (alpha >= 1.0f) {
                    changingAlpha = false;
                    alpha = 1.0f;
                }
                imageView.getImageReceiver().setAlpha(alpha);
            } else if (scaled && scale != 0.8f) {
                scale -= dt / 400.0f;
                if (scale < 0.8f) {
                    scale = 0.8f;
                }
            } else {
                scale += dt / 400.0f;
                if (scale > 1.0f) {
                    scale = 1.0f;
                }
            }
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
            imageView.invalidate();
            invalidate();
        }
        return result;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        String descr = LocaleController.getString("AttachSticker", R.string.AttachSticker);
        if (sticker != null) {
            for (int a = 0; a < sticker.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = sticker.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                    if (attribute.alt != null && attribute.alt.length() > 0) {
                        descr = attribute.alt + " " + descr;
                    }
                    break;
                }
            }
        }
        info.setContentDescription(descr);
        info.setEnabled(true);
    }
}
