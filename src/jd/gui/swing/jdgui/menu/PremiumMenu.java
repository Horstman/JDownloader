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

package jd.gui.swing.jdgui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.Timer;

import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.JDController;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.components.premiumbar.PremiumStatus;
import jd.gui.swing.menu.HosterMenu;
import jd.nutils.JDFlags;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class PremiumMenu extends JMenu implements ActionListener, AccountControllerListener {

    private static final long serialVersionUID = 5075413754334671773L;

    private static PremiumMenu INSTANCE;

    private JMenuItem config;

    private Timer updateAsync;

    private ToolBarAction tba;

    private PremiumMenu() {
        super(JDL.L("gui.menu.premium", "Premium"));

        updateAsync = new Timer(250, this);
        updateAsync.setInitialDelay(250);
        updateAsync.setRepeats(false);
        initAction();
        updateMenu();
        AccountController.getInstance().addListener(this);
    }

    private void initAction() {
        tba = new ToolBarAction("premiumMenu.toggle", "gui.images.premium_enabled") {

            private static final long serialVersionUID = 4276436625882302179L;

            @Override
            public void onAction(ActionEvent e) {
                if (!this.isSelected()) {
                    int answer = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN, JDL.L("dialogs.premiumstatus.global.title", "Disable Premium?"), JDL.L("dialogs.premiumstatus.global.message", "Do you really want to disable all premium accounts?"), JDTheme.II("gui.images.warning", 32, 32), JDL.L("gui.btn_yes", "Yes"), JDL.L("gui.btn_no", "No"));
                    if (JDFlags.hasAllFlags(answer, UserIO.RETURN_CANCEL) && !JDFlags.hasAllFlags(answer, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                        this.setSelected(true);
                        return;
                    }
                }

                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, this.isSelected());
                JDUtilities.getConfiguration().save();
            }

            @Override
            public void setIcon(String key) {
                putValue(AbstractAction.SMALL_ICON, JDTheme.II(key, 16, 16));
                putValue(IMAGE_KEY, key);
            }

            @Override
            public void initDefaults() {
                this.setEnabled(true);

                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            setIcon((Boolean) evt.getNewValue() ? "gui.images.premium_enabled" : "gui.images.premium_disabled");
                        }
                    }
                });
                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_USE_GLOBAL_PREMIUM) {
                    @Override
                    public void onPropertyChanged(Property source, String key) {
                        boolean b = source.getBooleanProperty(key, true);
                        setSelected(b);
                        PremiumStatus.getInstance().updateGUI(b);
                    }
                });

                setType(ToolBarAction.Types.TOGGLE);
                setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
            }

            @Override
            public void init() {
                if (inited) return;
                this.inited = true;

            }
        };
    }

    public static PremiumMenu getInstance() {
        if (INSTANCE == null) INSTANCE = new PremiumMenu();
        return INSTANCE;
    }

    private void updateMenu() {
        config = new JMenuItem(JDL.L("gui.menu.action.config.desc", "Premium Settings"));
        config.addActionListener(this);
        config.setIcon(JDTheme.II("gui.images.premium", 16, 16));

        this.add(new JCheckBoxMenuItem(tba));
        this.add(config);
        this.addSeparator();
        HosterMenu.update(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == updateAsync) {
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    update();
                    return null;
                }
            }.start();
            return;
        }
        if (e.getSource() == config) {
            SwingGui.getInstance().requestPanel(UserIF.Panels.PREMIUMCONFIG, null);
        }
    }

    public void update() {
        this.removeAll();
        updateMenu();
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
        case AccountControllerEvent.ACCOUNT_REMOVED:
            updateAsync.restart();
            break;
        }
    }

}
