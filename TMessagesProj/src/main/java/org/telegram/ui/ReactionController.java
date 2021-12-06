package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.RLottieDrawable;

import java.util.ArrayList;

public abstract class ReactionController {
    private static ArrayList<TLRPC.TL_availableReaction> availableReactions = null;
    private static int availableReactionsSize;

    public static ArrayList<TLRPC.TL_availableReaction> getAvailableReactions() {
        return availableReactions;
    }
    public static int getAvailableReactionsSize() {
        return availableReactionsSize;
    }
    public static Drawable getStaticReaction(String reaction) {
        for (TLRPC.TL_availableReaction availableReaction : availableReactions) {
            if (availableReaction.reaction.equals(reaction)) {
                TLRPC.Document staticReactionDocument = availableReaction.static_icon;
                ImageReceiver imageReceiver = new ImageReceiver();
                imageReceiver.setImage(ImageLocation.getForDocument(staticReactionDocument), null, null, "webp", null , 1);
                return imageReceiver.getDrawable();
            }
        }
        return null;
    }
    public static ImageReceiver getStaticReactionIR(String reaction) {
        for (TLRPC.TL_availableReaction availableReaction : availableReactions) {
            if (availableReaction.reaction.equals(reaction)) {
                TLRPC.Document staticReactionDocument = availableReaction.static_icon;
                ImageReceiver imageReceiver = new ImageReceiver();
                imageReceiver.setImage(ImageLocation.getForDocument(staticReactionDocument).path, null, null, "webp", 20);
                return imageReceiver;
            }
        }
        return null;
    }

    public static void init(BaseFragment fragment) {
        if (availableReactions == null || availableReactions.isEmpty()) {
            TLRPC.TL_messages_getAvailableReactions req = new TLRPC.TL_messages_getAvailableReactions();
            req.hash = 0;
            fragment.getConnectionsManager().sendRequest(req, (response, error)  -> {
                if (error != null) {
                    android.util.Log.e("DB", "MAIN: Error pulling available emoji: " + error);
                    return;
                }
                if (response instanceof TLRPC.TL_messages_availableReactions) {
                    // save possible reactions
                    android.util.Log.e("DB", "MAIN: updating emoji available");
                    ArrayList<TLRPC.TL_availableReaction> newAvailableReactions = ((TLRPC.TL_messages_availableReactions) response).reactions;
                    if (newAvailableReactions != null && !newAvailableReactions.isEmpty()) {
                        availableReactions = newAvailableReactions;
                        availableReactionsSize = newAvailableReactions.size();
                    }
                }
            });
        }
    }
}
