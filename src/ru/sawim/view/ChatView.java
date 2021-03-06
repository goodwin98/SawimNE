package ru.sawim.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.xmpp.Jid;
import protocol.xmpp.MirandaNotes;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.models.ChatsAdapter;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;
import ru.sawim.widget.chat.ChatBarView;
import ru.sawim.widget.chat.ChatInputBarView;
import ru.sawim.widget.chat.ChatListsView;
import ru.sawim.widget.chat.ChatViewRoot;
import sawim.Clipboard;
import sawim.Options;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.MessData;
import sawim.roster.RosterHelper;
import sawim.util.JLocale;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.01.13
 * Time: 20:30
 * To change this template use File | Settings | File Templates.
 */
public class ChatView extends SawimFragment implements RosterHelper.OnUpdateChat, Handler.Callback {

    public static final String TAG = "ChatView";

    private Chat chat;
    private Protocol protocol;
    private Contact contact;
    private String sharingText;
    private boolean sendByEnter;
    private boolean isOpenMenu = false;

    private ChatsAdapter chatsSpinnerAdapter;
    private MessagesAdapter adapter = new MessagesAdapter();
    private EditText messageEditor;
    private MyListView nickList;
    private MyListView chatListView;
    private ChatListsView chatListsView;
    private ChatInputBarView chatInputBarView;
    private ChatViewRoot chatViewLayout;
    private MucUsersView mucUsersView;
    private DrawerLayout drawerLayout;
    private ImageButton usersImage;
    private ImageButton chatsImage;
    private ImageButton menuButton;
    private ImageButton smileButton;
    private ImageButton sendButton;
    private ChatBarView chatBarLayout;

    private Handler handler;
    private static final int UPDATE_CHAT = 0;
    private static final int UPDATE_MUC_LIST = 1;

    public ChatView() {
        Activity activity = General.currentActivity;
        handler = new Handler(this);

        usersImage = new ImageButton(activity);
        chatsImage = new ImageButton(activity);

        menuButton = new ImageButton(activity);
        smileButton = new ImageButton(activity);
        sendButton = new ImageButton(activity);
        messageEditor = new EditText(activity);

        chatBarLayout = new ChatBarView(activity, usersImage, chatsImage);
        chatListView = new MyListView(activity);
        nickList = new MyListView(activity);
        chatListsView = new ChatListsView(activity, General.isManyPane(), chatListView, nickList);
        chatInputBarView = new ChatInputBarView(activity, menuButton, smileButton, messageEditor, sendButton);
        chatViewLayout = new ChatViewRoot(activity, chatListsView, chatInputBarView);
        if (!General.isManyPane())
            drawerLayout = new DrawerLayout(activity);
        General.getInstance().setConfigurationChanged(new General.OnConfigurationChanged() {
            @Override
            public void onConfigurationChanged() {
                MessagesAdapter.isRepaint = true;
            }
        });
    }

    public void removeTitleBar() {
        if (chatBarLayout != null && chatBarLayout.getParent() != null)
            ((ViewGroup) chatBarLayout.getParent()).removeView(chatBarLayout);
    }

    public ChatBarView getTitleBar() {
        return chatBarLayout;
    }

    private void resetBar() {
        General.actionBar.setDisplayShowTitleEnabled(false);
        General.actionBar.setDisplayHomeAsUpEnabled(false);
        General.actionBar.setDisplayShowHomeEnabled(false);
        General.actionBar.setDisplayUseLogoEnabled(false);
        if (!General.isManyPane()) {
            removeTitleBar();
            General.actionBar.setDisplayShowCustomEnabled(true);
            General.actionBar.setCustomView(chatBarLayout);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceStateLog) {
        updateChatIcon();
        if (drawerLayout != null && drawerLayout.getParent() != null)
            ((ViewGroup) drawerLayout.getParent()).removeView(drawerLayout);
        if (chatViewLayout.getParent() != null)
            ((ViewGroup) chatViewLayout.getParent()).removeView(chatViewLayout);

        if (Scheme.isSystemBackground()) {
            chatViewLayout.setBackgroundResource(Util.getSystemBackground(getActivity()));
        } else {
            chatViewLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        }
        chatBarLayout.updateDivider(Scheme.isBlack());
        chatViewLayout.updateDivider(Scheme.isBlack());
        chatListsView.updateDivider(Scheme.isBlack());
        chatInputBarView.setImageButtons(menuButton, smileButton, sendButton);
        if (!General.isManyPane()) {
            DrawerLayout.LayoutParams nickListLP = new DrawerLayout.LayoutParams(Util.dipToPixels(getActivity(), 240), DrawerLayout.LayoutParams.MATCH_PARENT);
            DrawerLayout.LayoutParams drawerLayoutLP = new DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT);
            drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
            drawerLayout.setScrimColor(Scheme.isBlack() ? 0x55FFFFFF : 0x99000000);
            nickListLP.gravity = Gravity.START;
            drawerLayout.setLayoutParams(drawerLayoutLP);
            nickList.setBackgroundResource(Util.getSystemBackground(getActivity()));
            nickList.setLayoutParams(nickListLP);
            if (nickList.getParent() != null)
                ((ViewGroup) nickList.getParent()).removeView(nickList);
            drawerLayout.addView(chatViewLayout);
            drawerLayout.addView(nickList);

            chatBarLayout.setVisibilityUsersImage(ImageView.VISIBLE);
            usersImage.setBackgroundColor(0);
            usersImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (nickList == null) return;
                    if (drawerLayout.isDrawerOpen(nickList)) {
                        drawerLayout.closeDrawer(nickList);
                    } else {
                        drawerLayout.openDrawer(nickList);
                    }
                }
            });
        } else
            chatBarLayout.setVisibilityUsersImage(ImageView.GONE);
        chatsImage.setBackgroundColor(0);
        chatsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forceGoToChat(ChatHistory.instance.getPreferredItem());
                RosterView rosterView = (RosterView) ChatView.this.getFragmentManager().findFragmentById(R.id.roster_fragment);
                if (rosterView != null)
                    rosterView.update();
            }
        });
        if (General.isManyPane()) {
            menuButton.setVisibility(ImageButton.VISIBLE);
            menuButton.setBackgroundColor(0);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (contact == null) return;
                    isOpenMenu = true;
                    getActivity().openOptionsMenu();
                }
            });
        } else
            menuButton.setVisibility(ImageButton.GONE);
        smileButton.setBackgroundColor(0);
        smileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(view);
                new SmilesView().show(General.currentActivity.getSupportFragmentManager(), "show-smiles");
            }
        });
        messageEditor.clearFocus();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            messageEditor.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        messageEditor.setSingleLine(false);
        messageEditor.setMaxLines(4);
        messageEditor.setHorizontallyScrolling(false);
        messageEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageEditor.setHint(R.string.hint_message);
        messageEditor.addTextChangedListener(textWatcher);
        messageEditor.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        if (sendByEnter) {
            messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
            messageEditor.setOnEditorActionListener(enterListener);
        }
        sendByEnter = Options.getBoolean(Options.OPTION_SIMPLE_INPUT);
        if (sendByEnter) {
            sendButton.setVisibility(ImageButton.GONE);
        } else {
            sendButton.setVisibility(ImageButton.VISIBLE);
            sendButton.setBackgroundColor(0);
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    send();
                    closePane();
                }
            });
            sendButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    insert("/me ");
                    showKeyboard();
                    return true;
                }
            });
        }
        return General.isManyPane() ? chatViewLayout : drawerLayout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MessagesAdapter.isRepaint = false;
        if (General.isManyPane()) return;
        chatsImage.setOnClickListener(null);
        menuButton.setOnClickListener(null);
        usersImage.setOnClickListener(null);
        smileButton.setOnClickListener(null);
        chatListView.setOnItemClickListener(null);
        messageEditor.removeTextChangedListener(textWatcher);
        chatListView.setOnCreateContextMenuListener(null);
        chat = null;
        contact = null;
        protocol = null;
        sharingText = null;
        mucUsersView = null;
        chatDialogFragment = null;
        chatsSpinnerAdapter = null;
    }

    public boolean hasBack() {
        if (nickList != null && !General.isManyPane())
            if (drawerLayout.isDrawerOpen(nickList)) {
                drawerLayout.closeDrawer(nickList);
                return false;
            }
        return true;
    }

    private void closePane() {
        if (nickList != null && !General.isManyPane())
            if (drawerLayout.isDrawerOpen(nickList)) {
                drawerLayout.closeDrawer(nickList);
            }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (General.currentActivity == null)
            General.currentActivity = (ActionBarActivity) getActivity();
        if (contact == null)
            initChat(RosterHelper.getInstance().getCurrentProtocol(), RosterHelper.getInstance().getCurrentContact());
        if (contact != null)
            openChat(protocol, contact);
        if (General.isManyPane()) {
            if (contact == null)
                chatViewLayout.showHint();
        } else {
            getActivity().supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pause(chat);
    }

    @Override
    public void onResume() {
        super.onResume();
        resume(chat);
    }

    public void pause(Chat chat) {
        if (chat == null) return;
        initChat(protocol, contact);

        View item = chatListView.getChildAt(0);
        chat.scrollPosition = chatListView.getFirstVisiblePosition();
        chat.offset = (item == null) ? 0 : Math.abs(item.getBottom());
        chat.dividerPosition = chat.getMessCount();
        chat.message = getText();

        chat.setVisibleChat(false);
        RosterHelper.getInstance().setOnUpdateChat(null);
        chat.resetUnreadMessages();
        if (chat.empty()) ChatHistory.instance.unregisterChat(chat);
    }

    public void resume(Chat chat) {
        resetBar();
        if (chat == null) return;
        chat.setVisibleChat(true);
        ChatHistory.instance.registerChat(chat);
        RosterHelper.getInstance().setOnUpdateChat(this);
        chat.resetUnreadMessages();
        removeMessages(Options.getInt(Options.OPTION_MAX_MSG_COUNT));
        if (sharingText != null) chat.message += " " + sharingText;
        setText(chat.message);

        setPosition();
        updateChatIcon();
        updateList(contact);

        if (General.isManyPane()) MessagesAdapter.isRepaint = true;
        else drawerLayout.setDrawerLockMode(contact.isConference() ?
                DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        chatBarLayout.setVisibilityLabelImage(contact.isConference() ? ImageView.GONE : ImageView.VISIBLE);
    }

    private void setPosition() {
        boolean hasHistory = chat.getHistory() != null && chat.getHistory().getHistorySize() > 0;
        adapter.setPosition(chat.dividerPosition);
        if (chat.dividerPosition == 0) {
            if (contact.isConference() || (!contact.isConference() && !hasHistory)) {
                chatListView.setSelection(0);
            } else if (hasHistory) {
                chatListView.setSelection(chat.getMessCount());
            }
        } else {
            if (isLastPosition()) {
                chatListView.setSelectionFromTop(chat.scrollPosition + 1, chat.offset);
            } else {
                chatListView.setSelectionFromTop(chat.scrollPosition + 2, chat.offset);
            }
        }
    }

    public boolean isLastPosition() {
        if (chat == null) return false;
        return chat.dividerPosition == chat.getMessCount();
    }

    public void setSharingText(String sharingText) {
        this.sharingText = sharingText;
    }

    private void removeMessages(final int limit) {
        if (chat.getMessCount() < limit) return;
        if ((0 < limit) && (0 < chat.getMessCount())) {
            while (limit < chat.getMessCount()) {
                chat.scrollPosition--;
                chat.dividerPosition--;
                chat.getMessData().remove(0);
            }
        } else ChatHistory.instance.unregisterChat(chat);
    }

    private void forceGoToChat(int position) {
        Chat current = ChatHistory.instance.chatAt(position);
        if (current == null) return;
        pause(chat);
        openChat(current.getProtocol(), current.getContact());
        resume(current);
    }

    public void initChat(Protocol p, Contact c) {
        protocol = p;
        contact = c;
    }

    public void openChat(Protocol p, Contact c) {
        chatViewLayout.hideHint();
        initChat(p, c);
        chat = protocol.getChat(contact);

        initLabel();
        initList();
        initMucUsers();
    }

    public Chat getCurrentChat() {
        return chat;
    }

    DialogFragment chatDialogFragment;

    private void initLabel() {
        chatsSpinnerAdapter = new ChatsAdapter(getActivity());
        chatBarLayout.updateLabelIcon(chatsSpinnerAdapter.getImageChat(chat, false));
        chatBarLayout.updateTextView(contact.getName());
        chatBarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatDialogFragment = new DialogFragment() {
                    @Override
                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                        final Context context = getActivity();
                        View dialogView = LayoutInflater.from(context).inflate(R.layout.chats_dialog, null);
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                        dialogBuilder.setView(dialogView);
                        dialogBuilder.setInverseBackgroundForced(Util.isNeedToInverseDialogBackground());
                        MyListView lv = (MyListView) dialogView.findViewById(R.id.listView);
                        lv.setAdapter(chatsSpinnerAdapter);
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Object o = parent.getAdapter().getItem(position);
                                if (o instanceof Chat) {
                                    Chat current = (Chat) o;
                                    pause(chat);
                                    openChat(current.getProtocol(), current.getContact());
                                    resume(current);
                                    dismiss();
                                    RosterView rosterView = (RosterView) ChatView.this.getFragmentManager().findFragmentById(R.id.roster_fragment);
                                    if (rosterView != null)
                                        rosterView.update();
                                }
                            }
                        });
                        Dialog dialog = dialogBuilder.create();
                        dialog.setCanceledOnTouchOutside(true);
                        return dialog;
                    }
                };
                chatDialogFragment.show(getFragmentManager().beginTransaction(), "force-go-to-chat");
                chatsSpinnerAdapter.refreshList();
            }
        });
    }

    private void initList() {
        adapter.init(chat);
        chatListView.setAdapter(adapter);
        chatListView.setStackFromBottom(true);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        chatListView.setOnCreateContextMenuListener(this);
        chatListView.setOnItemClickListener(chatClick);
        chatListView.setFocusable(true);
    }

    private void initMucUsers() {
        if (General.isManyPane())
            nickList.setVisibility(View.VISIBLE);
        else if (drawerLayout.isDrawerOpen(nickList))
            drawerLayout.closeDrawer(nickList);

        if (contact instanceof XmppServiceContact && contact.isConference()) {
            mucUsersView = new MucUsersView();
            mucUsersView.init(protocol, (XmppServiceContact) contact);
            mucUsersView.show(this, nickList);
            chatBarLayout.setVisibilityUsersImage(General.isManyPane() ? View.GONE : View.VISIBLE);
        } else {
            chatBarLayout.setVisibilityUsersImage(View.GONE);
            if (General.isManyPane()) {
                nickList.setVisibility(View.GONE);
            } else {
                if (drawerLayout.isDrawerOpen(nickList)) {
                    drawerLayout.closeDrawer(nickList);
                }
            }
        }
    }

    private void updateList(Contact contact) {
        if (contact == this.contact) {
            if (adapter != null)
                adapter.refreshList(chat.getMessData());
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_CHAT:
                updateChatIcon();
                updateList((Contact) msg.obj);
                if (chatsSpinnerAdapter != null && chatDialogFragment != null && chatDialogFragment.isVisible())
                    chatsSpinnerAdapter.refreshList();
                break;
            case UPDATE_MUC_LIST:
                if (contact != null && contact.isPresence() == (byte) 1)
                    if (adapter != null)
                        adapter.refreshList(chat.getMessData());
                if (mucUsersView != null)
                    mucUsersView.update();
                break;
        }
        return false;
    }

    @Override
    public void updateChat(final Contact contact) {
        handler.sendMessage(Message.obtain(handler, UPDATE_CHAT, contact));
    }

    @Override
    public void updateMucList() {
        handler.sendEmptyMessage(UPDATE_MUC_LIST);
    }

    private void updateChatIcon() {
        if (chatBarLayout == null) return;
        Drawable icMess = ChatHistory.instance.getUnreadMessageIcon();
        if (contact != null)
            usersImage.setImageDrawable(StatusInfo.STATUS_ONLINE == contact.getStatusIndex() ? SawimResources.usersIconOn : SawimResources.usersIcon);
        if (icMess == null) {
            chatBarLayout.setVisibilityChatsImage(View.GONE);
        } else {
            chatBarLayout.setVisibilityChatsImage(View.VISIBLE);
            chatsImage.setImageDrawable(icMess);
        }
        if (chatsSpinnerAdapter != null)
            chatBarLayout.updateLabelIcon(chatsSpinnerAdapter.getImageChat(chat, false));
    }

    StringBuffer multiQuoteBuffer = new StringBuffer();
    private ListView.OnItemClickListener chatClick = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            MessData mData = (MessData) adapterView.getAdapter().getItem(position);
            if (adapter.isMultiQuote()) {
                mData.setMarked(!mData.isMarked());
                for (int i = 0; i < chat.getMessData().size(); ++i) {
                    MessData messData = chat.getMessageDataByIndex(i);
                    if (messData.isMarked()) {
                        CharSequence msg = messData.getText();
                        if (messData.isMe())
                            msg = "*" + messData.getNick() + " " + msg;
                        multiQuoteBuffer.append(Clipboard.serialize(false, messData.isIncoming(), messData.getNick() + " " + messData.strTime, msg));
                        multiQuoteBuffer.append("\n---\n");
                    }
                }
                Clipboard.setClipBoardText(0 == multiQuoteBuffer.length() ? null : multiQuoteBuffer.toString());
                adapter.notifyDataSetChanged();
            } else {
                if (contact instanceof XmppServiceContact) {
                    XmppServiceContact xmppServiceContact = ((XmppServiceContact) contact);
                    if (xmppServiceContact.getContact(mData.getNick()) == null && !xmppServiceContact.getName().equals(mData.getNick())) {
                        Toast.makeText(General.currentActivity, getString(R.string.contact_walked), Toast.LENGTH_LONG).show();
                    }
                }
                setText(chat.onMessageSelected(mData));
                showKeyboard();
                if (General.isManyPane()) {
                    if (nickList.getVisibility() == View.VISIBLE && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                        nickList.setVisibility(View.GONE);
                } else {
                    if (drawerLayout.isDrawerOpen(nickList) && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                        drawerLayout.closeDrawer(nickList);
                }
            }
        }
    };

    public boolean isOpenMenu() {
        return isOpenMenu;
    }

    public void setOpenMenu(boolean openMenu) {
        isOpenMenu = openMenu;
    }

    public void onPrepareOptionsMenu_(Menu menu) {
        if (chat == null) return;
        menu.clear();
        boolean accessible = chat.getWritable() && (contact.isSingleUserContact() || contact.isOnline());
        menu.add(Menu.FIRST, ContactMenu.MENU_MULTI_CITATION, 2, getString(adapter.isMultiQuote() ?
                R.string.disable_multi_citation : R.string.include_multi_citation));
        if (0 < chat.getAuthRequestCounter()) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_GRANT_AUTH, 2, JLocale.getString("grant"));
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_DENY_AUTH, 2, JLocale.getString("deny"));
        }
        if (!contact.isAuth()) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_REQU_AUTH, 2, JLocale.getString("requauth"));
        }
        if (accessible) {
            if (sawim.modules.fs.FileSystem.isSupported()) {
                menu.add(Menu.FIRST, ContactMenu.USER_MENU_FILE_TRANS, 2, JLocale.getString("ft_name"));
            }
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_CAM_TRANS, 2, JLocale.getString("ft_cam"));
        }
        menu.add(Menu.FIRST, ContactMenu.USER_MENU_STATUSES, 2, General.currentActivity.getResources().getString(R.string.user_statuses));
        if (!contact.isSingleUserContact() && contact.isOnline()) {
            menu.add(Menu.FIRST, ContactMenu.CONFERENCE_DISCONNECT, 2, JLocale.getString("leave_chat"));
        }
        menu.add(Menu.FIRST, ContactMenu.ACTION_CURRENT_DEL_CHAT, 2, JLocale.getString("delete_chat"));
        menu.add(Menu.FIRST, ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR, 2, JLocale.getString("all_contact_except_this"));
        menu.add(Menu.FIRST, ContactMenu.ACTION_DEL_ALL_CHATS, 2, JLocale.getString("all_contacts"));
        super.onPrepareOptionsMenu(menu);
    }

    public void onOptionsItemSelected_(MenuItem item) {
        switch (item.getItemId()) {
            case ContactMenu.MENU_MULTI_CITATION:
                if (adapter.isMultiQuote()) {
                    adapter.setMultiQuote(false);
                    multiQuoteBuffer.delete(0, multiQuoteBuffer.length());
                } else {
                    adapter.setMultiQuote(true);
                    Toast.makeText(General.currentActivity, R.string.hint_multi_citation, Toast.LENGTH_LONG).show();
                }
                adapter.notifyDataSetChanged();
                getActivity().supportInvalidateOptionsMenu();
                break;

            case ContactMenu.ACTION_CURRENT_DEL_CHAT:
                ChatHistory.instance.unregisterChat(chat);
                if (General.isManyPane())
                    chatViewLayout.setVisibility(LinearLayout.GONE);
                else
                    getFragmentManager().popBackStack();
                break;

            case ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR:
                ChatHistory.instance.removeAll(chat);
                break;

            case ContactMenu.ACTION_DEL_ALL_CHATS:
                ChatHistory.instance.removeAll(null);
                if (General.isManyPane())
                    chatViewLayout.setVisibility(LinearLayout.GONE);
                else
                    getFragmentManager().popBackStack();
                break;

            default:
                new ContactMenu(protocol, contact).doAction(item.getItemId());
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.FIRST, ContactMenu.MENU_COPY_TEXT, 0, android.R.string.copy);
        menu.add(Menu.FIRST, ContactMenu.ACTION_QUOTE, 0, JLocale.getString("quote"));
        if (contact instanceof XmppServiceContact && contact.isConference()) {
            menu.add(Menu.FIRST, ContactMenu.COMMAND_PRIVATE, 0, R.string.open_private);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_INFO, 0, R.string.info);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_STATUS, 0, R.string.user_statuses);
        }
        if (protocol instanceof Xmpp) {
            menu.add(Menu.FIRST, ContactMenu.ACTION_TO_NOTES, 0, R.string.add_to_notes);
        }
        if (!Options.getBoolean(Options.OPTION_HISTORY) && chat.hasHistory()) {
            menu.add(Menu.FIRST, ContactMenu.ACTION_ADD_TO_HISTORY, 0, JLocale.getString("add_to_history"));
        }
        contact.addChatMenuItems(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        MessData md = adapter.getItem(info.position);
        String nick = md.getNick();
        CharSequence msg = md.getText();
        switch (item.getItemId()) {
            case ContactMenu.MENU_COPY_TEXT:
                if (null == md) {
                    return false;
                }
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                Clipboard.setClipBoardText(msg + "\n");
                Toast.makeText(General.currentActivity, R.string.hint_citation, Toast.LENGTH_LONG).show();
                break;

            case ContactMenu.ACTION_QUOTE:
                StringBuffer sb = new StringBuffer();
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                sb.append(Clipboard.serialize(true, md.isIncoming(), md.getNick() + " " + md.strTime, msg));
                sb.append("\n-----\n");
                Clipboard.setClipBoardText(0 == sb.length() ? null : sb.toString());
                Toast.makeText(General.currentActivity, R.string.hint_citation, Toast.LENGTH_LONG).show();
                break;

            case ContactMenu.COMMAND_PRIVATE:
                String jid = Jid.realJidToSawimJid(contact.getUserId() + "/" + nick);
                XmppServiceContact c = (XmppServiceContact) protocol.getItemByUIN(jid);
                if (null == c) {
                    c = (XmppServiceContact) protocol.createTempContact(jid);
                    protocol.addTempContact(c);
                }
                pause(getCurrentChat());
                openChat(protocol, c);
                resume(getCurrentChat());
                break;
            case ContactMenu.COMMAND_INFO:
                protocol.showUserInfo(((XmppServiceContact) contact).getPrivateContact(nick));
                break;
            case ContactMenu.COMMAND_STATUS:
                protocol.showStatus(((XmppServiceContact) contact).getPrivateContact(nick));
                break;

            case ContactMenu.ACTION_ADD_TO_HISTORY:
                chat.addTextToHistory(md);
                break;

            case ContactMenu.ACTION_TO_NOTES:
                MirandaNotes notes = ((Xmpp) protocol).getMirandaNotes();
                notes.showIt();
                MirandaNotes.Note note = notes.addEmptyNote();
                note.tags = md.getNick() + " " + md.strTime;
                note.text = md.getText();
                notes.showNoteEditor(note);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void showKeyboard(View view) {
        Configuration conf = Resources.getSystem().getConfiguration();
        if (conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO) {
            InputMethodManager keyboard = (InputMethodManager) General.currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    private void hideKeyboard(View view) {
        if (Options.getBoolean(Options.OPTION_HIDE_KEYBOARD)) {
            InputMethodManager imm = (InputMethodManager) General.currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void showKeyboard() {
        messageEditor.requestFocus();
        showKeyboard(messageEditor);
    }

    @Override
    public void pastText(final String text) {
        insert(" " + text + " ");
        showKeyboard();
    }

    private void send() {
        if (chat == null) return;
        hideKeyboard(messageEditor);
        chat.sendMessage(getText());
        resetText();
        adapter.setPosition(-1);
        updateChat(contact);
        for (int i = 0; i < chat.getMessData().size(); ++i) {
            MessData messData = chat.getMessageDataByIndex(i);
            if (messData.isMarked()) {
                messData.setMarked(false);
            }
        }
    }

    private boolean canAdd(String what) {
        String text = getText();
        if (0 == text.length()) return false;
        // more then one comma
        if (text.indexOf(',') != text.lastIndexOf(',')) return true;
        // replace one post number to another
        if (what.startsWith("#") && !text.contains(" ")) return false;
        return true/*!text.endsWith(", ")*/;
    }

    private void resetText() {
        messageEditor.setText("");
    }

    private String getText() {
        return messageEditor.getText().toString();
    }

    private void setText(final String text) {
        String t = null == text ? "" : text;
        if ((0 == t.length()) || !canAdd(t)) {
            messageEditor.setText(t);
            messageEditor.setSelection(t.length());
        } else {
            insert(t);
        }
    }

    private boolean hasText() {
        return 0 < messageEditor.getText().length();
    }

    public void insert(String text) {
        if (messageEditor == null) return;
        int start = messageEditor.getSelectionStart();
        int end = messageEditor.getSelectionEnd();
        messageEditor.getText().replace(Math.min(start, end), Math.max(start, end),
                text, 0, text.length());
    }

    private boolean isDone(int actionId) {
        return (EditorInfo.IME_NULL == actionId)
                || (EditorInfo.IME_ACTION_DONE == actionId)
                || (EditorInfo.IME_ACTION_SEND == actionId);
    }

    private boolean compose = false;
    private TextWatcher textWatcher = new TextWatcher() {
        private String previousText;
        private int lineCount = 0;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (sendByEnter) {
                previousText = s.toString();
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (sendByEnter && (start + count <= s.length()) && (1 == count)) {
                boolean enter = ('\n' == s.charAt(start));
                if (enter) {
                    messageEditor.setText(previousText);
                    messageEditor.setSelection(start);
                    send();
                    return;
                }
            }
            if (protocol == null) return;
            int length = s.length();
            if (length > 0) {
                if (!compose) {
                    compose = true;
                    protocol.sendTypingNotify(contact, true);
                }
            } else {
                if (compose) {
                    compose = false;
                    protocol.sendTypingNotify(contact, false);
                }
            }
            if (lineCount != messageEditor.getLineCount()) {
                lineCount = messageEditor.getLineCount();
                messageEditor.requestLayout();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            String text = s.subSequence(0, s.length()).toString();
            if (adapter.isMultiQuote()) {
                if (Clipboard.getClipBoardText() != null && text.equals(Clipboard.getClipBoardText())) {
                    adapter.setMultiQuote(false);
                    multiQuoteBuffer.delete(0, multiQuoteBuffer.length());
                    adapter.notifyDataSetChanged();
                    getActivity().supportInvalidateOptionsMenu();
                }
            }
        }
    };

    private final TextView.OnEditorActionListener enterListener = new android.widget.TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(android.widget.TextView textView, int actionId, KeyEvent event) {
            if (isDone(actionId)) {
                if ((null == event) || (event.getAction() == KeyEvent.ACTION_DOWN)) {
                    send();
                    return true;
                }
            }
            return false;
        }
    };
}