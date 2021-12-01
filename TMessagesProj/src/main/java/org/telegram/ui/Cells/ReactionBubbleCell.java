/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.ChatListItemAnimator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class ReactionBubbleCell extends RelativeLayout {

    int finalWidth;

    public ReactionBubbleCell(Context context, ArrayList<TLRPC.TL_availableReaction> availableReactions, TLRPC.ChatFull info) {
        super(context);

        // generic init
        int totalEmojis = 0;
        int sidePadding = 5;
        // was 44
        int emojiSize = 42;
        // was manually 300
        int maxAllowedWidth = (int) Math.round(emojiSize*6.5+2*sidePadding);
        int viewHeight = AndroidUtilities.dp(48);

        // Todo: add mini bubbles
        // attempt to add blow bubbles under main bubble
        // CombinedDrawable combinedDrawable = new CombinedDrawable(background, shape, 0, 0);
        // combinedDrawable.setFullsize(true);

        // scroll view
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);

        // background
        GradientDrawable shape =  new GradientDrawable();
        shape.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));

        // parent view
        LinearLayout planeLayout = new LinearLayout(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            planeLayout.setElevation(12);
            planeLayout.setClipToOutline(true);

            shape.setCornerRadius(viewHeight/2f);
        } else {
            shape.setCornerRadius(viewHeight/8f);
        }
        planeLayout.setBackgroundDrawable(shape);

        // emoji holder
        LinearLayout emojiHolder = new LinearLayout(context);
        emojiHolder.setOrientation(LinearLayout.HORIZONTAL);
        emojiHolder.setPadding(AndroidUtilities.dp(sidePadding), 0, AndroidUtilities.dp(sidePadding), 0);

        scrollView.addView(emojiHolder);

        // add individual emojis
        for (TLRPC.TL_availableReaction reaction : availableReactions) {
            // allow all emoji for PMs
            if (info == null || info.available_reactions.contains(reaction.reaction)) {
                TLRPC.Document emojiAnimation = reaction.select_animation;

                ReactionEmojiCell cell = new ReactionEmojiCell(context, emojiSize, false);
                cell.setSticker(emojiAnimation, emojiHolder, 0.25f, 500);
                cell.setScaled(true);
                cell.setOnClickListener((view -> cell.playOnce()));
                emojiHolder.addView(cell, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

                totalEmojis++;
            }
        }

        // combine & exit
        planeLayout.addView(scrollView);
        addView(planeLayout, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT, 8, 8, 8, 8));

        finalWidth = AndroidUtilities.dp(Math.min(totalEmojis*emojiSize + 2*sidePadding, maxAllowedWidth));

        slideView(scrollView, 0, finalWidth);
    }

    public int getFinalWidth() {
        return finalWidth;
    }

    public static void slideView(View view, int currentWidth, int newWidth) {

        ValueAnimator slideAnimator = ValueAnimator
                .ofInt(currentWidth, newWidth)
                .setDuration(ChatListItemAnimator.DEFAULT_DURATION); // was 220-250

        slideAnimator.addUpdateListener(animation1 -> {
            view.getLayoutParams().width = (Integer) animation1.getAnimatedValue();
            view.requestLayout();
        });

        AnimatorSet animationSet = new AnimatorSet();
        // attempt to add overshoot interpolar; DISREGARDED: looks ugly, too fast in the beginning, to slow in the end
        // animationSet.setInterpolator(new OvershootInterpolator(1f));
        animationSet.setInterpolator(ChatListItemAnimator.DEFAULT_INTERPOLATOR);
        animationSet.play(slideAnimator);
        animationSet.start();
    }
}
