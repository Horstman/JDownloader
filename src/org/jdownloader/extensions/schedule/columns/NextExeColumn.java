//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.schedule.columns;

import org.jdownloader.extensions.schedule.Actions;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextTableColumn;
import jd.nutils.Formatter;
import jd.utils.locale.JDL;

public class NextExeColumn extends JDTextTableColumn {

    private static final long serialVersionUID = -2945101320574207493L;
    private long nexttime = 0;

    public NextExeColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    protected String getStringValue(Object value) {
        if (!((Actions) value).isEnabled()) {
            return JDL.L("jd.plugins.optional.schedule.disabled", "disabled");
        } else {
            nexttime = ((Actions) value).getDate().getTime() - System.currentTimeMillis();
            if (nexttime < 0) {
                if (((Actions) value).getRepeat() == 0) {
                    return JDL.L("jd.plugins.optional.schedule.expired", "expired");
                } else {
                    return JDL.L("jd.plugins.optional.schedule.wait", "wait a moment");
                }
            } else {
                /*
                 * we will not show secs, so show 1 min left
                 */
                if (nexttime < 60000) nexttime = 60000;
                return Formatter.formatSeconds(nexttime / 1000, false);
            }
        }
    }

}
