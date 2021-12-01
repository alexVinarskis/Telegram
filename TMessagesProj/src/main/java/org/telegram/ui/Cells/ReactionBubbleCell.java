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

    public ReactionBubbleCell(Context context, ArrayList<TLRPC.TL_availableReaction> availableReactions, TLRPC.ChatFull info) {
        super(context);

        // generic init
        int sidePadding = 5;
        int emojiSize = 44;
        int viewHeight = AndroidUtilities.dp(48);
        setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));

        // background
        GradientDrawable shape =  new GradientDrawable();
        shape.setCornerRadius(viewHeight/2f);
        shape.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));

        // Todo: add mini bubbles
        // attempt to add blow bubbles under main bubble
        // CombinedDrawable combinedDrawable = new CombinedDrawable(background, shape, 0, 0);
        // combinedDrawable.setFullsize(true);

        // scroll view
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);

        // parent view
        LinearLayout planeLayout = new LinearLayout(context);
        planeLayout.setBackgroundDrawable(shape);
        planeLayout.setElevation(3);
        planeLayout.setClipToOutline(true);

        // emoji holder
        LinearLayout emojiHolder = new LinearLayout(context);
        emojiHolder.setOrientation(LinearLayout.HORIZONTAL);
        emojiHolder.setPadding(AndroidUtilities.dp(sidePadding), 0, AndroidUtilities.dp(sidePadding), 0);

        scrollView.addView(emojiHolder);

        // add individual emojis
        for (TLRPC.TL_availableReaction reaction : availableReactions) {
            if (info.available_reactions.contains(reaction.reaction)) {
                TLRPC.Document emojiAnimation = reaction.select_animation;

                ReactionEmojiCell cell = new ReactionEmojiCell(context, emojiSize);
                cell.setSticker(emojiAnimation, emojiHolder);
                cell.setScaled(true);
                cell.setOnClickListener((view -> cell.playOnce()));
                emojiHolder.addView(cell, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));
            }
        }

        // combine & exit
        planeLayout.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));
        addView(planeLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

        float finalSize = AndroidUtilities.dp(Math.min(info.available_reactions.size()*emojiSize + 2*sidePadding, 300));

        slideView(scrollView, 0, Math.round(finalSize));
    }

    public static void slideView(View view, int currentHeight, int newHeight) {

        ValueAnimator slideAnimator = ValueAnimator
                .ofInt(currentHeight, newHeight)
                .setDuration(ChatListItemAnimator.DEFAULT_DURATION); // was 220-250

        slideAnimator.addUpdateListener(animation1 -> {
            Integer value = (Integer) animation1.getAnimatedValue();
            view.getLayoutParams().width = value;
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
