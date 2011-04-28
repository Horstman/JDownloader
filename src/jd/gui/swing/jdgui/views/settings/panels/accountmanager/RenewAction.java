package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;

public class RenewAction extends TableBarAction {
    public RenewAction() {

        this.putValue(NAME, T._.settings_accountmanager_renew());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("renew", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public boolean isEnabled() {
        // if (account == null) return false;
        // AccountInfo ai = account.getAccountInfo();
        // if (ai == null) return true;
        // if (ai.isExpired()) return true;
        // return false;
        return true;
    }

}
