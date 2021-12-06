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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
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
import org.telegram.ui.ReactionController;

import java.util.ArrayList;
import java.util.Random;

public class ReactionCounterCell extends FrameLayout {

    public ReactionCounterCell(Context context, int size, TLRPC.TL_reactionCount reaction, ArrayList<TLRPC.TL_messageUserReaction> userReactions ) {
        super(context);

        // TO BE USED IN POPUP UI
        // NOT USED RIGHT NOW

        // CANNOT BE USED FOR CANVAS

        int viewHeight = AndroidUtilities.dp(size);

        // background
        GradientDrawable shape =  new GradientDrawable();
        shape.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));
        shape.setCornerRadius(AndroidUtilities.dp(viewHeight/2f));

        // parent view
        LinearLayout planeLayout = new LinearLayout(context);
        planeLayout.setBackgroundDrawable(shape);
        planeLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Image view
        Drawable drawable = ReactionController.getStaticReaction(reaction.reaction);
        ImageView imageView = new ImageView(context);
        imageView.setImageDrawable(drawable);

        // Text view
        String text = String.format("%s", LocaleController.formatShortNumber(Math.max(1, reaction.count), null));
        TextView textView = new TextView(context);
        textView.setText(text);

        if (reaction.count <= 3 && userReactions != null) {
            // get avatars instead
            // ToDo:
            for (TLRPC.TL_messageUserReaction userReaction : userReactions) {
                Log.e("DB", "got the user and reaction: " + userReaction.user_id + " " + userReaction.reaction);
            }
        }

        planeLayout.addView(imageView, LayoutHelper.createLinear(size, size));
        planeLayout.addView(textView);
        addView(planeLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, viewHeight, Gravity.CENTER));
    }
}
