package jd.gui.swing.jdgui.views.settings.panels.advanced;

import jd.gui.swing.jdgui.views.settings.panels.components.SettingsTable;

import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class AdvancedTable extends SettingsTable<AdvancedConfigEntry> {

    public AdvancedTable() {
        super(new AdvancedTableModel("AdvancedTable"));
    }

    @Override
    public boolean isSearchEnabled() {
        return true;
    }

}
