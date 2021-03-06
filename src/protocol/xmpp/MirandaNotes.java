package protocol.xmpp;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.Scheme;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.models.list.VirtualListModel;
import sawim.Clipboard;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.util.JLocale;

import java.util.List;
import java.util.Vector;


public final class MirandaNotes {
    private static final int COMMAND_ADD = 0;
    private static final int COMMAND_EDIT = 1;
    private static final int COMMAND_DEL = 2;
    private static final int MENU_COPY = 3;
    private static final int MENU_COPY_ALL = 4;

    private Xmpp xmpp;
    private Vector notes = new Vector();

    private VirtualList screen;
    private VirtualListModel model;

    void init(Xmpp protocol) {
        screen = VirtualList.getInstance();
        model = new VirtualListModel();
        screen.setCaption(JLocale.getString("notes"));
        xmpp = protocol;
        screen.setModel(model);
        screen.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.NONE, COMMAND_ADD, Menu.NONE, JLocale.getString("add_to_list"));
            }

            @Override
            public void onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case COMMAND_ADD: {
                        new NoteEditor(addEmptyNote()).showIt();
                        break;
                    }
                }
            }
        });
        screen.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.add(Menu.FIRST, COMMAND_EDIT, 2, JLocale.getString("edit"));
                menu.add(Menu.FIRST, COMMAND_DEL, 2, JLocale.getString("delete"));
                menu.add(Menu.FIRST, MENU_COPY, 2, JLocale.getString("copy_text"));
                menu.add(Menu.FIRST, MENU_COPY_ALL, 2, JLocale.getString("copy_all_text"));
            }

            @Override
            public void onContextItemSelected(int listItem, int itemMenuId) {
                switch (itemMenuId) {
                    case COMMAND_EDIT: {
                        Note note = (Note) notes.elementAt(listItem);
                        new NoteEditor(note).showIt();
                        break;
                    }

                    case COMMAND_DEL:
                        removeNote(listItem);
                        xmpp.getConnection().saveMirandaNotes(getNotesStorage());
                        refresh();
                        break;

                    case MENU_COPY:
                        VirtualListItem item = screen.getModel().elements.get(listItem);
                        Clipboard.setClipBoardText(item.getLabel() + "\n" + item.getDescStr());
                        break;

                    case MENU_COPY_ALL:
                        StringBuffer s = new StringBuffer();
                        List<VirtualListItem> listItems = screen.getModel().elements;
                        for (int i = 0; i < listItems.size(); ++i) {
                            s.append(listItems.get(i).getLabel()).append("\n")
                                    .append(listItems.get(i).getDescStr()).append("\n");
                        }
                        Clipboard.setClipBoardText(s.toString());
                        break;
                }
            }
        });
        screen.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(int position) {
                Note note = (Note) notes.elementAt(position);
                new NoteEditor(note).showIt();
            }

            @Override
            public boolean back() {
                screen.updateModel();
                return true;
            }
        });
    }

    public void clear() {
        model.clear();
    }

    public void showIt() {
        clear();
        notes.removeAllElements();
        VirtualListItem wait = model.createNewParser(true);
        wait.addDescription(JLocale.getString("wait"),
                Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(wait);
        xmpp.getConnection().requestMirandaNotes();
        screen.show();
        screen.updateModel();
    }

    void addNote(String title, String tags, String text) {
        Note note = new Note();
        note.title = title;
        note.tags = tags;
        note.text = text;
        notes.addElement(note);
        addNote(note);
    }

    private void addNote(Note note) {
        VirtualListItem parser = model.createNewParser(true);
        parser.addLabel(note.title + "\n" + "*" + note.tags, Scheme.THEME_PARAM_VALUE, Scheme.FONT_STYLE_BOLD);
        parser.addDescription(note.text, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(parser);
        screen.updateModel();
    }

    String getNotesStorage() {
        StringBuffer storage = new StringBuffer();
        int size = notes.size();
        for (int i = 0; i < size; ++i) {
            Note note = (Note) notes.elementAt(i);
            storage.append("<note tags='").append(Util.xmlEscape(note.tags)).append("'>");
            storage.append("<title>").append(Util.xmlEscape(note.title)).append("</title>");
            storage.append("<text>").append(Util.xmlEscape(note.text.toString())).append("</text>");
            storage.append("</note>");
        }
        return storage.toString();
    }

    private void refresh() {
        int index = screen.getCurrItem();
        clear();
        int size = notes.size();
        for (int i = 0; i < size; ++i) {
            addNote((Note) notes.elementAt(i));
        }
        screen.setCurrentItemIndex(index, false);
    }

    public Note addEmptyNote() {
        Note note = new Note();
        notes.addElement(note);
        return note;
    }

    private void removeNote(int currItem) {
        notes.removeElementAt(currItem);
    }

    private void selectNote(Note note) {
        screen.setCurrentItemIndex(notes.indexOf(note), false);
    }

    public void showNoteEditor(Note n) {
        new NoteEditor(n).showIt();
    }

    public class Note {
        private String title;
        public String tags;
        public CharSequence text;
    }

    private class NoteEditor implements FormListener {
        private static final int FIELD_TITLE = 0;
        private static final int FIELD_TAGS = 1;
        private static final int FIELD_TEXT = 2;

        private Note note;

        private NoteEditor(Note note) {
            this.note = note;
        }

        private void showIt() {
            Forms form = new Forms("notes", this, false);
            form.addTextField(FIELD_TITLE, "title", note.title);
            form.addTextField(FIELD_TAGS, "tags", note.tags);
            form.addTextField(FIELD_TEXT, "text", note.text == null ? "" : note.text.toString());
            form.show();
        }

        public void formAction(Forms form, boolean apply) {
            if (apply) {
                String title = form.getTextFieldValue(FIELD_TITLE);
                String tags = form.getTextFieldValue(FIELD_TAGS);
                String text = form.getTextFieldValue(FIELD_TEXT);

                if (StringConvertor.isEmpty(title) &&
                        StringConvertor.isEmpty(tags) &&
                        StringConvertor.isEmpty(text)) {
                    return;
                }

                note.title = title;
                note.tags = tags;
                note.text = text;

                refresh();
                selectNote(note);
                xmpp.getConnection().saveMirandaNotes(getNotesStorage());
                form.back();
            }
        }
    }
}