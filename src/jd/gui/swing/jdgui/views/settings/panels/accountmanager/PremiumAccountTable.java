package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import jd.gui.swing.jdgui.views.settings.panels.components.SettingsTable;
import jd.plugins.Account;

public class PremiumAccountTable extends SettingsTable<Account> {

    public PremiumAccountTable() {

        super(new PremiumAccountTableModel());
        this.addMouseListener(new ContextMenuListener(this));

    }

}
