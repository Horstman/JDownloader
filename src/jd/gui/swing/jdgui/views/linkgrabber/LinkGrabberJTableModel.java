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

package jd.gui.swing.jdgui.views.linkgrabber;


 import org.jdownloader.gui.translate.*;
import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.linkgrabber.columns.FileColumn;
import jd.gui.swing.jdgui.views.linkgrabber.columns.HosterColumn;
import jd.gui.swing.jdgui.views.linkgrabber.columns.SizeColumn;
import jd.gui.swing.jdgui.views.linkgrabber.columns.StatusColumn;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

public class LinkGrabberJTableModel extends JDTableModel {

    private static final long serialVersionUID = 896882146491584908L;

    public LinkGrabberJTableModel(String configname) {
        super(configname);
    }

    protected void initColumns() {
        this.addColumn(new FileColumn(T._.gui_linkgrabber_header_packagesfiles(), this));
        this.addColumn(new SizeColumn(T._.gui_treetable_header_size(), this));
        this.addColumn(new HosterColumn(T._.gui_treetable_hoster(), this));
        this.addColumn(new StatusColumn(T._.gui_treetable_status(), this));
    }

    public void refreshModel() {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (LinkGrabberController.getInstance().getPackages()) {
                synchronized (list) {
                    list.clear();
                    for (LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                        list.add(fp);
                        if (fp.getBooleanProperty(LinkGrabberController.PROPERTY_EXPANDED, false)) {
                            for (DownloadLink dl : fp.getDownloadLinks()) {
                                list.add(dl);
                            }
                        }
                    }
                }
            }
        }
    }

}