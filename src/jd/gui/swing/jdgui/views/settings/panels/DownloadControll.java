//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.settings.panels;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.Spinner;

import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;
import org.jdownloader.translate.JDT;

public class DownloadControll extends AbstractConfigPanel {

    private static final long serialVersionUID = 1L;
    private Spinner           maxSimPerHost;
    private ComboBox          remove;
    private ComboBox          ifFileExists;

    public String getTitle() {
        return JDT._.gui_settings_downloadcontroll_title();
    }

    public DownloadControll() {
        super();

        this.addHeader(JDT._.gui_settings_downloadcontroll_title(), Theme.getIcon("downloadmanagment", 32));
        this.addDescription(JDT._.gui_settings_downloadcontroll_description());

        maxSimPerHost = new Spinner(0, 20);
        String[] removeDownloads = new String[] { T._.gui_config_general_toDoWithDownloads_immediate(), T._.gui_config_general_toDoWithDownloads_atstart(), T._.gui_config_general_toDoWithDownloads_packageready(), T._.gui_config_general_toDoWithDownloads_never() };

        remove = new ComboBox(removeDownloads);
        String[] fileExists = new String[] { T._.system_download_triggerfileexists_overwrite(), T._.system_download_triggerfileexists_skip(), T._.system_download_triggerfileexists_rename(), T._.system_download_triggerfileexists_askpackage(), T._.system_download_triggerfileexists_ask() };
        ifFileExists = new ComboBox(fileExists);

        this.addPair(T._.gui_config_download_simultan_downloads_per_host(), maxSimPerHost);
        this.addPair(T._.gui_config_general_todowithdownloads(), remove);
        this.addPair(T._.system_download_triggerfileexists(), ifFileExists);
    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("downloadmanagment", 32);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }
}