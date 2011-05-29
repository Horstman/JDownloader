package org.jdownloader.extensions.webinterfaceng;

import javax.swing.SpinnerNumberModel;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.webinterface.translate.T;
import org.jdownloader.gui.settings.Pair;

public class WebinterfaceNgConfigPanel extends ExtensionConfigPanel<WebinterfaceNgExtension> {

    private Pair<Checkbox>       useRefresh;

    private Pair<Spinner>        refreshInterval;

    private Pair<Spinner>        port;

    private Pair<Checkbox>       useLocalhostOnly;

    private Pair<Checkbox>       useLogin;

    private Pair<Checkbox>       useHttps;

    private Pair<TextInput>      user;

    private Pair<TextInput>      password;

    private WebinterfaceNgConfig config;

    public WebinterfaceNgConfigPanel(WebinterfaceNgExtension plg, WebinterfaceNgConfig webinterfaceNgConfig) {
        super(plg);
        this.config = webinterfaceNgConfig;
        initComponents();
    }

    @Override
    public void save() {
        config.setUseRefresh(useRefresh.getComponent().isSelected());
        config.setRefreshInterval((Integer) refreshInterval.getComponent().getValue());
        config.setPort((Integer) port.getComponent().getValue());
        config.setUseLocalhostOnly(useLocalhostOnly.getComponent().isSelected());
        config.setUseLogin(useLogin.getComponent().isSelected());
        config.setUseHttps(useHttps.getComponent().isSelected());
        config.setUser(user.getComponent().getText());
        config.setPassword(password.getComponent().getText());
    }

    @Override
    public void updateContents() {

        useRefresh.getComponent().setSelected(config.isUseRefresh());
        refreshInterval.getComponent().setValue(config.getRefreshInterval());
        port.getComponent().setValue(config.getPort());
        useLocalhostOnly.getComponent().setSelected(config.isUseLocalhostOnly());
        useLogin.getComponent().setSelected(config.isUseLogin());
        useHttps.getComponent().setSelected(config.isUseHttps());
        user.getComponent().setText(config.getUser());
        password.getComponent().setText(config.getPassword());

    }

    private void initComponents() {

        useRefresh = this.addPair(T._.plugins_optional_webinterface_refresh(), new Checkbox());
        Spinner interval = new Spinner(5, 60);
        ((SpinnerNumberModel) interval.getModel()).setStepSize(5);

        refreshInterval = this.addPair(T._.plugins_optional_webinterface_refresh_interval(), interval);
        port = this.addPair(T._.plugins_optional_webinterface_port(), new Spinner(1, 65000));
        useLocalhostOnly = this.addPair(T._.plugins_optional_webinterface_localhostonly(), new Checkbox());
        useLogin = this.addPair(T._.plugins_optional_webinterface_needlogin(), new Checkbox());
        useHttps = this.addPair(T._.plugins_optional_webinterface_https(), new Checkbox());
        user = this.addPair(T._.plugins_optional_webinterface_loginname(), new TextInput());
        password = this.addPair(T._.plugins_optional_webinterface_loginpass(), new TextInput());
    }

}
