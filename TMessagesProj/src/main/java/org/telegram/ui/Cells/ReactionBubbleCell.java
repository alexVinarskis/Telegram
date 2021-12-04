/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.ChatListItemAnimator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class ReactionBubbleCell extends RelativeLayout {

    int finalWidth;
    int finalHeight;
    LinearLayout planeLayout;
    ArrayList<ReactionEmojiCell> emojisList;
    ReactionBubbleCellDelegate delegate;
    MessageObject message;

    public void setDelegate(ReactionBubbleCellDelegate reactionBubbleCellDelegate) {
        delegate = reactionBubbleCellDelegate;
    }
    public interface ReactionBubbleCellDelegate {
        default void didPressReaction(ReactionEmojiCell cell, MessageObject message, String reaction) {
        }
    }

    public ReactionBubbleCell(Context context, MessageObject message, ArrayList<TLRPC.TL_availableReaction> availableReactions, TLRPC.ChatFull info, int totalHeight) {
        super(context);

        this.message = message;

        // generic init
        int totalEmojis = 0;
        int sidePadding = 5;
        // was 44
        int emojiSize = 42;
        // was manually 300
        int maxAllowedWidth = (int) Math.round(emojiSize*6.5+2*sidePadding);
        int viewHeight = totalHeight - 8 - 8;

        emojisList = new ArrayList<>();

        // scroll view
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);

        // background
        GradientDrawable shape =  new GradientDrawable();
        shape.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));

        // parent view
        planeLayout = new LinearLayout(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            planeLayout.setElevation(12);
            planeLayout.setClipToOutline(true);
            shape.setCornerRadius(AndroidUtilities.dp(viewHeight/2f));
        } else {
            shape.setCornerRadius(AndroidUtilities.dp(viewHeight/8f));
        }

//        // foreground
//        GradientDrawable foreground =  new GradientDrawable();
//        foreground.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));
//
//        // Todo: add mini bubbles
//        // attempt to add blow bubbles under main bubble
//        CombinedDrawable combinedDrawable = new CombinedDrawable(shape, foreground, 0, 0);
//        combinedDrawable.setFullsize(true);

        planeLayout.setBackgroundDrawable(shape);

        // emoji holder
        LinearLayout emojiHolder = new LinearLayout(context);
        emojiHolder.setOrientation(LinearLayout.HORIZONTAL);
        emojiHolder.setPadding(AndroidUtilities.dp(sidePadding), 0, AndroidUtilities.dp(sidePadding), 0);
        scrollView.addView(emojiHolder, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL));

        // add individual emojis
        for (TLRPC.TL_availableReaction reaction : availableReactions) {
            // allow all emoji for PMs
            if (info == null || info.available_reactions.contains(reaction.reaction)) {
                TLRPC.Document emojiAnimation = reaction.select_animation;

                ReactionEmojiCell cell = new ReactionEmojiCell(context, emojiSize, false);
                cell.setSticker(emojiAnimation, emojiHolder, 0.25f, 500);
                cell.setScaled(true);
//                cell.setOnClickListener((view -> cell.playOnce()));
                cell.setOnClickListener((view -> delegate.didPressReaction(cell, message, cell.getReactionTitle())));
                cell.setReactionTitle(reaction.reaction);
                cell.setPadding(0, AndroidUtilities.dp((viewHeight-emojiSize)/2f), 0, AndroidUtilities.dp((viewHeight-emojiSize)/2f));
                emojiHolder.addView(cell);

                emojisList.add(cell);
            }
        }

        // combine & exit
        planeLayout.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        addView(planeLayout, LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, viewHeight, 8, 8, 8, 8));

        finalWidth = AndroidUtilities.dp(Math.min(emojisList.size()*emojiSize + 2*sidePadding, maxAllowedWidth));
        finalHeight = AndroidUtilities.dp(viewHeight);

        slideView(planeLayout, 0, finalWidth);
    }

    private void handleClick(ReactionEmojiCell cell) {
        String title = cell.getReactionTitle();
        if (title != null) {
            // Todo: pass back callback to main code, to CLOSE popup, RUN ANIMATION, and SEND EMOJI
        }
    }

    public int getFinalWidth() {
        return finalWidth;
    }

    public void animateIn() {
        RelativeLayout.LayoutParams params = (LayoutParams) planeLayout.getLayoutParams();
        params.width = 0;
        params.height = 0;
        planeLayout.setLayoutParams(params);
        planeLayout.setVisibility(VISIBLE);
        if (planeLayout != null) slideExpandView(planeLayout, 0, finalWidth, 0, finalHeight);
    }
    public void animateOut() {
        if (planeLayout != null) {
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setDuration(250);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) { planeLayout.setVisibility(INVISIBLE); }
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            planeLayout.startAnimation(anim);
        }
        for (ReactionEmojiCell cell : emojisList) {
            cell.restartPlayOnce();
        }
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
    public static void slideExpandView(View view, int currentWidth, int newWidth, int currentHeight, int newHeight) {

        ValueAnimator slideAnimator = ValueAnimator
                .ofInt(currentWidth, newWidth)
                .setDuration(ChatListItemAnimator.DEFAULT_DURATION);
        ValueAnimator expandAnimator = ValueAnimator
                .ofInt(currentHeight, newHeight)
                .setDuration(ChatListItemAnimator.DEFAULT_DURATION);

        slideAnimator.addUpdateListener(animation1 -> {
            view.getLayoutParams().width = (Integer) animation1.getAnimatedValue();
            view.requestLayout();
        });
        expandAnimator.addUpdateListener(animation1 -> {
            view.getLayoutParams().height = (Integer) animation1.getAnimatedValue();
            view.requestLayout();
        });

        AnimatorSet animationSet = new AnimatorSet();
        animationSet.setInterpolator(ChatListItemAnimator.DEFAULT_INTERPOLATOR);
        animationSet.playTogether(slideAnimator, expandAnimator);
        animationSet.start();
    }
}
