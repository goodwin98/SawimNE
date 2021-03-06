package protocol.mrim;

import DrawControls.icons.Icon;
import android.view.ContextMenu;
import android.view.Menu;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import sawim.util.JLocale;

public class MrimPhoneContact extends MrimContact {
    static final String PHONE_UIN = "pho" + "ne";

    public MrimPhoneContact(String phones) {
        super(PHONE_UIN, PHONE_UIN);
        setPhones(phones);
        setName(phones);
        setBooleanValue(Contact.CONTACT_NO_AUTH, false);
    }

    int getFlags() {
        return 0x100000;
    }

    public Icon getLeftIcon(Protocol p) {
        return Mrim.getPhoneContactIcon();
    }

    public void activate(Protocol p) {
        if (hasChat()) {
            p.getChat(this).activate();
        } else {
            new ContactMenu(p, this).doAction(USER_MENU_SEND_SMS);
        }
    }

    protected void initContextMenu(Protocol protocol, ContextMenu menu) {
        menu.add(Menu.FIRST, USER_MENU_SEND_SMS, 2, JLocale.getString("send_sms"));
        menu.add(Menu.FIRST, ContactMenu.USER_MENU_USER_INFO, 2, JLocale.getString("info"));
        if ((protocol.getGroupItems().size() > 1) && !isTemp()) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_MOVE, 2, JLocale.getString("move_to_group"));
        }
        menu.add(Menu.FIRST, ContactMenu.USER_MENU_USER_REMOVE, 2, JLocale.getString("remove"));
        menu.add(Menu.FIRST, ContactMenu.USER_MENU_RENAME, 2, JLocale.getString("rename"));
    }

    public boolean isVisibleInContactList() {
        return true;
    }
}


