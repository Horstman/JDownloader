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

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter.LinkgrabberFilter;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

public class Linkgrabber extends AbstractConfigPanel {

    private static final long serialVersionUID = 1L;
    private Checkbox          checkLinks;
    private Checkbox          cnl;
    private Checkbox          rename;
    private LinkgrabberFilter filter;

    public String getTitle() {
        return _JDT._.gui_settings_linkgrabber_title();
    }

    public Linkgrabber() {
        super();

        this.addHeader(getTitle(), NewTheme.I().getIcon("linkgrabber", 32));
        this.addDescription(_JDT._.gui_settings_linkgrabber_description());

        checkLinks = new Checkbox();
        rename = new Checkbox();
        cnl = new Checkbox();

        addPair(_GUI._.gui_config_linkgrabber_onlincheck(), checkLinks);
        addPair(_GUI._.gui_config_linkgrabber_replacechars(), rename);
        addPair(_GUI._.gui_config_linkgrabber_cnl2(), cnl);

        this.addHeader(_GUI._.gui_config_linkgrabber_ignorelist(), NewTheme.I().getIcon("filter", 32));
        this.addDescription(_JDT._.gui_settings_linkgrabber_filter_description());
        filter = new LinkgrabberFilter();

        add(filter);

    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("linkgrabber", 32);
    }

    @Override
    public void save() {
        GeneralSettings st = JsonConfig.create(GeneralSettings.class);
        st.setClickNLoadEnabled(cnl.isSelected());
        st.setLinkcheckEnabled(checkLinks.isSelected());
        st.setCleanUpFilenames(rename.isSelected());
    }

    @Override
    public void updateContents() {
        GeneralSettings st = JsonConfig.create(GeneralSettings.class);
        cnl.setSelected(st.isClickNLoadEnabled());
        checkLinks.setSelected(st.isLinkcheckEnabled());
        rename.setSelected(st.isCleanUpFilenames());

    }
}