package org.telegram.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.EmojiTextCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCellAnimated;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class ReactionsEditActivity extends BaseFragment {

    private ArrayList<EmojiTextCell> emojiButtons;
    private TextCheckCellAnimated switchCell;
    private TextInfoPrivacyCell hintCell;
    private TextInfoPrivacyCell spaceCell;
    private LinearLayout infoContainer;
    private LinearLayout linearLayout;

    private TLRPC.Chat currentChat;
    private TLRPC.ChatFull info;
    private long chatId;
    private boolean isChannel;

    private ArrayList<TLRPC.TL_availableReaction> availableReactions;
    private ArrayList<String> ogAvailableReactions;

    public ReactionsEditActivity(long chatId, TLRPC.ChatFull info, TLRPC.TL_messages_availableReactions availableReactions) {
        this.chatId = chatId;
        this.info = info;
        this.ogAvailableReactions = new ArrayList<>(info.available_reactions);
        if (availableReactions != null) {
            this.availableReactions = availableReactions.reactions;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        currentChat = getMessagesController().getChat(chatId);
        if (currentChat == null) {
            currentChat = MessagesStorage.getInstance(currentAccount).getChatSync(chatId);
            if (currentChat != null) {
                getMessagesController().putChat(currentChat, true);
            } else {
                return false;
            }
            if (info == null) {
                info = MessagesStorage.getInstance(currentAccount).loadChatInfo(chatId, ChatObject.isChannel(currentChat), new CountDownLatch(1), false, false);
                if (info == null) {
                    return false;
                }
            }
        }
        isChannel = ChatObject.isChannel(currentChat) && !currentChat.megagroup;
        return super.onFragmentCreate();
    }
    private boolean reactionIdentical() {
        ArrayList<String> list = new ArrayList<String>(info.available_reactions);
        Collections.sort(list);
        Collections.sort(ogAvailableReactions);
        return  list.equals(ogAvailableReactions);
    }

    private void pushAvailableEmoji() {
        if (!reactionIdentical()) {
            Log.e("DB", "pushing emoji to server; id: " + chatId  + " size: " + info.available_reactions.size());
            TLRPC.TL_messages_setChatAvailableReactions req = new TLRPC.TL_messages_setChatAvailableReactions();
            req.peer = getMessagesController().getInputPeer(-chatId);
            req.available_reactions = info.available_reactions;
            getConnectionsManager().sendRequest(req, (response1, error1) ->  {
                if (error1 != null) {
                    Log.e("DB","Error updating allowed emojis " + error1.text);
                } else {
                    // reset server state counter
                    ogAvailableReactions = new ArrayList<>(info.available_reactions);

                    // force re-pull updates for chatInfo
//                    TLRPC.TL_messages_getFullChat req2 = new TLRPC.TL_messages_getFullChat();
//                    req2.chat_id = chatId;
//                    getConnectionsManager().sendRequest(req2, (response, error) ->  {
//                        if (error != null) {
//                            Log.e("DB","Error pulling chatFull: " + error.text);
//                        } else {
//                            TLRPC.TL_messages_chatFull messagesChatFull = (TLRPC.TL_messages_chatFull) response;
//                            if (messagesChatFull.full_chat != null) {
//                                Log.e("DB","pushing chatInfo updates");
//                                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.chatInfoDidLoad, messagesChatFull.full_chat));
//                            }
//                        }
//                    });
                }
            });
        } else {
            Log.e("DB", "NOT pushing emoji to server - nothing changed");
        }
    }
    private void toggleEnableEmoji(TLRPC.TL_availableReaction reaction, EmojiTextCell emojiSwitch  ) {
        emojiSwitch.setChecked(!emojiSwitch.isChecked());
        // update locals
        if (emojiSwitch.isChecked()) {
            if (!info.available_reactions.contains(reaction.reaction)) info.available_reactions.add(reaction.reaction);
        } else {
            if (info.available_reactions.contains(reaction.reaction)) info.available_reactions.remove(reaction.reaction);
        }
        // check if all were off
        if (info.available_reactions.isEmpty() && switchCell.isChecked()) toggleEnableAllEmoji();
        if (!info.available_reactions.isEmpty() && !switchCell.isChecked()) toggleEnableAllEmoji();
        // push to server; REMOVED - on exit instead;
        // pushAvailableEmoji();
    }
    private void toggleEnableAllEmoji() {
        switchCell.setChecked(!switchCell.isChecked());
        if (switchCell.isChecked()) {
            // update views
            for (EmojiTextCell emojiButton : emojiButtons) {
                emojiButton.setChecked(true, false);
            }
            info.available_reactions.clear();
            for (TLRPC.TL_availableReaction reaction : availableReactions) {
                info.available_reactions.add(reaction.reaction);
            }
            // animate list
            infoContainer.setVisibility(View.VISIBLE);
            spaceCell.setVisibility(View.VISIBLE);
            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(250);
            anim.setFillAfter(true);
            infoContainer.startAnimation(anim);
            spaceCell.startAnimation(anim);
        } else {
            // set all local to be false, but do not update views
            info.available_reactions.clear();
            // animate list
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setDuration(250);
            anim.setFillAfter(true);
            infoContainer.startAnimation(anim);
            spaceCell.startAnimation(anim);
        }
        // push updates to server; REMOVED - on exit instead;
        // pushAvailableEmoji();
    }
    private boolean checkAllowedEmoji(TLRPC.TL_availableReaction reaction) {
        return info.available_reactions.contains(reaction.reaction);
    }

    @Override
    public void onPause() {
        super.onPause();
        pushAvailableEmoji();
    }

    @Override
    public View createView(Context context) {
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("ReactionsFragmentTitle", R.string.ReactionsFragmentTitle));

        // parent view
        fragmentView = new LinearLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        LinearLayout parentView = (LinearLayout) fragmentView;
        parentView.setOrientation(LinearLayout.VERTICAL);

        // switch
        switchCell = new TextCheckCellAnimated(context);
        switchCell.setHeight(54);            // same as actionBar height
        switchCell.setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        switchCell.setTextAndCheck(LocaleController.getString("ReactionsFragmentSwitch", R.string.ReactionsFragmentSwitch), !info.available_reactions.isEmpty(), false);
        switchCell.setOnClickListener(v -> toggleEnableAllEmoji());
        parentView.addView(switchCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // scroll view
        ScrollView scrollView = new ScrollView(context) {
            @Override
            public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
                rectangle.bottom += AndroidUtilities.dp(60);
                return super.requestChildRectangleOnScreen(child, rectangle, immediate);
            }
        };
        scrollView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        scrollView.setFillViewport(true);
        linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        parentView.addView(scrollView, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // scroll: switch
        // MOVE SWITCH HERE TO SCROLL FULL PAGE

//        switchCell = new TextCheckCellAnimated(context);
//        switchCell.setHeight(54);            // same as actionBar height
//        switchCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//        switchCell.setTextAndCheck(LocaleController.getString("ReactionsFragmentSwitch", R.string.ReactionsFragmentSwitch), !info.available_reactions.isEmpty(), false);
//        switchCell.setOnClickListener(v -> toggleEnableAllEmoji());
//        linearLayout.addView(switchCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // scroll: hint
        hintCell = new TextInfoPrivacyCell(context);
        linearLayout.addView(hintCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        hintCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        hintCell.setText(isChannel ? LocaleController.getString("ReactionsFragmentHintChannel", R.string.ReactionsFragmentHintChannel) : LocaleController.getString("ReactionsFragmentHintGroup", R.string.ReactionsFragmentHintGroup));
        hintCell.setBackgroundDrawable(Theme.getThemedDrawable(hintCell.getContext(), R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));

        // scroll: button container
        infoContainer = new LinearLayout(context);
        infoContainer.setOrientation(LinearLayout.VERTICAL);
        infoContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        linearLayout.addView(infoContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        if (!switchCell.isChecked()) infoContainer.setVisibility(View.GONE);

        HeaderCell headerCell = new HeaderCell(context, 23);
        headerCell.setHeight(46);
        headerCell.setText(LocaleController.getString("ReactionsFragmentAvailable", R.string.ReactionsFragmentAvailable));
        infoContainer.addView(headerCell);

        emojiButtons = new ArrayList<>();

        if (availableReactions != null) {
            for (int i = 0; i < availableReactions.size(); i++) {
                TLRPC.TL_availableReaction reaction = availableReactions.get(i);

                EmojiTextCell emojiSwitch = new EmojiTextCell(context);
                emojiSwitch.setBackgroundDrawable(Theme.getSelectorDrawable(false));
                emojiSwitch.setTextAndValueAndIcon(reaction.title, reaction.reaction, checkAllowedEmoji(reaction), i != availableReactions.size() - 1);
                emojiSwitch.setOnClickListener(v -> {
                    toggleEnableEmoji(reaction, emojiSwitch);
                });
                infoContainer.addView(emojiSwitch, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                emojiButtons.add(emojiSwitch);
            }
        }

        // scroll: bottom shadow
        spaceCell = new TextInfoPrivacyCell(context);
        if (!switchCell.isChecked()) spaceCell.setVisibility(View.GONE);
        linearLayout.addView(spaceCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        spaceCell.setText("");
        spaceCell.setBackgroundDrawable(Theme.getThemedDrawable(hintCell.getContext(), R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));

        return fragmentView;
    }
}
