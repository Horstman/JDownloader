package org.jdownloader.extensions.webinterfaceng;

import jd.plugins.AddonPanel;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class WebinterfaceNgExtension extends AbstractExtension<WebinterfaceNgConfig> {

    private WebinterfaceNgConfigPanel configPanel;

    private Server                    server;

    public WebinterfaceNgExtension() {
        super("Webinterface NG");
    }

    @Override
    protected void stop() throws StopException {
        try {
            server.stop();
        } catch (Exception e) {
            throw new StopException(e);
        }
    }

    @Override
    protected void start() throws StartException {
        try {
            setUpServer();
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    private void setUpServer() throws Exception {
        server = new Server(getSettings().getPort());

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/");
        webAppContext.setResourceBase("WebContent");
        webAppContext.setParentLoaderPriority(true);
        server.setHandler(webAppContext);

        server.start();
    }

    @Override
    protected void initExtension() throws StartException {

        configPanel = new WebinterfaceNgConfigPanel(this, getSettings());
    }

    @Override
    public ExtensionConfigPanel<WebinterfaceNgExtension> getConfigPanel() {
        return configPanel;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getConfigID() {
        return "webinterface";
    }

    @Override
    public String getAuthor() {
        return "Horstman";
    }

    @Override
    public String getDescription() {
        return "Next Generation Webinterface";
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

}
