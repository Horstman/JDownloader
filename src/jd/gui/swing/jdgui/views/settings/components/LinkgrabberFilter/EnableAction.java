package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class EnableAction extends AbstractAction {
    private static final long serialVersionUID = 8434201391290149067L;
    private FilterTable       table;

    public EnableAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, table.getExtTableModel().getSelectedObjects().size() == 0 ? _GUI._.settings_linkgrabber_filter_action_enable_all() : _GUI._.settings_linkgrabber_filter_action_enable());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("enable", 20));
    }

    @Override
    public boolean isEnabled() {
        if (table.getExtTableModel().getSelectedObjects().size() == 0) return true;
        for (LinkFilter f : table.getExtTableModel().getSelectedObjects()) {
            if (!f.isEnabled()) return true;
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
    }

}
