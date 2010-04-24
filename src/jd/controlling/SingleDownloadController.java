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

package jd.controlling;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.Balloon;
import jd.gui.swing.dialog.AgbDialog;
import jd.http.Browser.BrowserException;
import jd.nutils.Formatter;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * In dieser Klasse wird der Download parallel zum Hauptthread gestartet
 * 
 * @author astaldo/JD-Team
 */
public class SingleDownloadController extends Thread {
    public static final String WAIT_TIME_ON_CONNECTION_LOSS = "WAIT_TIME_ON_CONNECTION_LOSS";

    private static final Object DUPELOCK = new Object();

    private boolean aborted = false;

    /**
     * Das Plugin, das den aktuellen Download steuert
     */
    private PluginForHost currentPlugin;

    private DownloadLink downloadLink;

    private LinkStatus linkStatus;

    /**
     * Der Logger
     */
    private Logger logger = JDLogger.getLogger();

    private long startTime;

    /**
     * Erstellt einen Thread zum Start des Downloadvorganges
     * 
     * @param controller
     *            Controller
     * @param dlink
     *            Link, der heruntergeladen werden soll
     */
    public SingleDownloadController(DownloadLink dlink) {
        super("JD-StartDownloads");
        downloadLink = dlink;
        linkStatus = downloadLink.getLinkStatus();
        setPriority(Thread.MIN_PRIORITY);
        downloadLink.setDownloadLinkController(this);
    }

    /**
     * Bricht den Downloadvorgang ab.
     */
    public SingleDownloadController abortDownload() {
        aborted = true;
        DownloadInterface dli = downloadLink.getDownloadInstance();
        if (dli != null) dli.stopDownload();
        interrupt();
        return this;
    }

    private void fireControlEvent(ControlEvent controlEvent) {
        JDUtilities.getController().fireControlEvent(controlEvent);

    }

    private void fireControlEvent(int controlID, Object param) {
        JDUtilities.getController().fireControlEvent(controlID, param);

    }

    public PluginForHost getCurrentPlugin() {
        return currentPlugin;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    private void handlePlugin() {
        try {
            this.startTime = System.currentTimeMillis();
            linkStatus.setStatusText(JDL.L("gui.download.create_connection", "Connecting..."));
            System.out.println("PreDupeChecked: no mirror found!");
            fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, currentPlugin);
            DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
            currentPlugin.init();
            try {
                try {
                    currentPlugin.handle(downloadLink);
                } catch (BrowserException e) {
                    /* damit browserexceptions korrekt weitergereicht werden */
                    e.closeConnection();
                    if (e.getException() != null) {
                        throw e.getException();
                    } else {
                        throw e;
                    }
                }
            } catch (UnknownHostException e) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setErrorMessage(JDL.L("plugins.errors.nointernetconn", "No Internet connection?"));
                linkStatus.setValue(SubConfiguration.getConfig("CONNECTION_PROBLEMS").getGenericProperty("UnknownHostException", 5 * 60 * 1000l));
            } catch (SocketTimeoutException e) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setErrorMessage(JDL.L("plugins.errors.hosteroffline", "Hoster offline?"));
                linkStatus.setValue(SubConfiguration.getConfig("CONNECTION_PROBLEMS").getGenericProperty("SocketTimeoutException", 10 * 60 * 1000l));
            } catch (SocketException e) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setErrorMessage(JDL.L("plugins.errors.disconnect", "Disconnect?"));

                linkStatus.setValue(SubConfiguration.getConfig("CONNECTION_PROBLEMS").getGenericProperty("SocketException", 5 * 60 * 1000l));

            } catch (IOException e) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setErrorMessage(JDL.L("plugins.errors.hosterproblem", "Hoster problem?"));
                linkStatus.setValue(10 * 60 * 1000l);
            } catch (InterruptedException e) {
                logger.finest("Hoster Plugin Version: " + downloadLink.getPlugin().getVersion());
                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(JDL.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));
            } catch (Exception e) {
                logger.finest("Hoster Plugin Version: " + downloadLink.getPlugin().getVersion());
                JDLogger.exception(e);
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
                linkStatus.setErrorMessage(JDL.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));
            }

            if (isAborted() && !linkStatus.isFinished()) {
                linkStatus.setErrorMessage(null);
                linkStatus.setStatus(LinkStatus.TODO);
                return;
            }
            if (linkStatus.isFailed()) {
                logger.warning("\r\nError occured- " + downloadLink.getLinkStatus());
            }
            switch (linkStatus.getLatestStatus()) {
            case LinkStatus.ERROR_LOCAL_IO:
                onErrorLocalIO(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_IP_BLOCKED:
                onErrorIPWaittime(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                onErrorDownloadTemporarilyUnavailable(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
                onErrorHostTemporarilyUnavailable(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_AGB_NOT_SIGNED:
                onErrorAGBNotSigned(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_FILE_NOT_FOUND:
                Balloon.showIfHidden(JDL.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDL.LF("ballon.download.fnf.message", "<b>%s<b><hr>File not found", downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")"));
                onErrorFileNotFound(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_LINK_IN_PROGRESS:
                onErrorLinkBlock(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_FATAL:
                Balloon.showIfHidden(JDL.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDL.LF("ballon.download.fatalerror.message", "<b>%s<b><hr>Fatal Plugin Error", downloadLink.getHost()));
                onErrorFatal(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_CAPTCHA:
                onErrorCaptcha(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_PREMIUM:
                onErrorPremium(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
                onErrorIncomplete(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_ALREADYEXISTS:
                onErrorFileExists(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_DOWNLOAD_FAILED:
                onErrorChunkloadFailed(downloadLink, currentPlugin);
                Balloon.showIfHidden(JDL.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDL.LF("ballon.download.failed.message", "<b>%s<b><hr>failed", downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")"));
                break;
            case LinkStatus.ERROR_PLUGIN_DEFECT:
                onErrorPluginDefect(downloadLink, currentPlugin);
                Balloon.showIfHidden(JDL.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDL.LF("ballon.download.plugindefect.message", "<b>%s<b><hr>Plugin defect", downloadLink.getHost()));
                break;
            case LinkStatus.ERROR_NO_CONNECTION:
            case LinkStatus.ERROR_TIMEOUT_REACHED:
                Balloon.showIfHidden(JDL.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDL.LF("ballon.download.connectionlost.message", "<b>%s<b><hr>Connection lost", downloadLink.getHost()));
                onErrorNoConnection(downloadLink, currentPlugin);
                break;
            default:
                if (linkStatus.hasStatus(LinkStatus.FINISHED)) {
                    logger.finest("\r\nFinished- " + downloadLink.getLinkStatus());
                    logger.info("\r\nFinished- " + downloadLink.getFileOutput());
                    onDownloadFinishedSuccessFull(downloadLink);
                } else {
                    retry(downloadLink, currentPlugin);
                }
            }
        } catch (Exception e) {
            logger.severe("Error in Plugin Version: " + downloadLink.getPlugin().getVersion());
            JDLogger.exception(e);
        }
    }

    private void onErrorLinkBlock(DownloadLink downloadLink, PluginForHost currentPlugin) {
        LinkStatus status = downloadLink.getLinkStatus();
        if (status.hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
            onErrorFileExists(downloadLink, currentPlugin);
        } else {
            status.resetWaitTime();
            downloadLink.setEnabled(false);
        }

    }

    private void onErrorPluginDefect(DownloadLink downloadLink2, PluginForHost currentPlugin2) {
        logger.warning("The Plugin for " + currentPlugin.getHost() + " seems to be out of date(rev" + downloadLink.getPlugin().getVersion() + "). Please inform the Support-team http://jdownloader.org/support.");
        if (downloadLink2.getLinkStatus().getErrorMessage() != null) logger.warning(downloadLink2.getLinkStatus().getErrorMessage());
        // Dieser Exception deutet meistens auf einen PLuginfehler hin. Deshalb
        // wird in diesem Fall die zuletzt geladene browserseite aufgerufen.
        try {
            logger.finest(currentPlugin2.getBrowser().getRequest().getHttpConnection() + "");
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        try {
            logger.finest(currentPlugin2.getBrowser() + "");
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        downloadLink2.getLinkStatus().setErrorMessage(JDL.L("controller.status.plugindefective", "Plugin out of date"));
        downloadLink.requestGuiUpdate();
    }

    /**
     * download aborted by user?
     * 
     * @return
     */
    public boolean isAborted() {
        return aborted;
    }

    private void onDownloadFinishedSuccessFull(DownloadLink downloadLink) {
        if ((System.currentTimeMillis() - startTime) > 30000) Balloon.showIfHidden(JDL.L("ballon.download.successfull.title", "Download"), JDTheme.II("gui.images.ok", 32, 32), JDL.LF("ballon.download.successfull.message", "<b>%s<b><hr>finished successfully", downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")"));
        downloadLink.setProperty(DownloadLink.STATIC_OUTPUTFILE, downloadLink.getFileOutput());

        // set all links to disabled that point to the same file location
        // - prerequisite: 'skip link' option selected
        // TODO WORKAROUND FOR NOW.. WILL BE HANDLED BY A MIRROR MANAGER IN THE
        // FUTURE
        if (SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_FILE_EXISTS, 1) == 1) {
            ArrayList<DownloadLink> links = DownloadController.getInstance().getAllDownloadLinks();
            for (DownloadLink link : links) {
                if (downloadLink != link && downloadLink.getFileOutput().equals(link.getFileOutput())) {
                    link.getLinkStatus().setErrorMessage(JDL.LF("controller.status.fileexists.othersource", "File loaded from %s.", link.getHost()));
                    link.setEnabled(false);
                    DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
                }
            }
        }

        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
        if (JDController.isContainerFile(new File(downloadLink.getFileOutput()))) {
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) {
                JDController.loadContainerFile(new File(downloadLink.getFileOutput()));
            }
        }

    }

    public static void onErrorAGBNotSigned(DownloadLink downloadLink2, PluginForHost plugin) throws InterruptedException {
        downloadLink2.getLinkStatus().setStatusText(JDL.L("controller.status.agb_tos", "TOS haven't been accepted."));
        if (!plugin.isAGBChecked()) {
            synchronized (JDUtilities.USERIO_LOCK) {
                if (!plugin.isAGBChecked()) {
                    showAGBDialog(downloadLink2);
                } else {
                    downloadLink2.getLinkStatus().reset();
                }
            }
        } else {
            downloadLink2.getLinkStatus().reset();
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink2);
    }

    /**
     * blockiert EDT sicher bis der Dialog bestätigt wurde
     * 
     * @param downloadLink2
     */
    public static void showAGBDialog(final DownloadLink downloadLink2) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                AgbDialog.showDialog(downloadLink2);
                return null;
            }
        }.waitForEDT();
    }

    /**
     * Diese Funktion wird aufgerufen wenn ein Download wegen eines
     * captchafehlersabgebrochen wird
     * 
     * @param downloadLink
     * @param plugin2
     * @param step
     */
    private void onErrorCaptcha(DownloadLink downloadLink, PluginForHost plugin) {
        retry(downloadLink, plugin);
    }

    private void retry(DownloadLink downloadLink, PluginForHost plugin) {
        int r;
        if (downloadLink.getLinkStatus().getValue() > 0) {
            downloadLink.getLinkStatus().setStatusText(null);
        }
        if ((r = downloadLink.getLinkStatus().getRetryCount()) <= plugin.getMaxRetries()) {
            downloadLink.getLinkStatus().reset();
            downloadLink.getLinkStatus().setRetryCount(r + 1);
            downloadLink.getLinkStatus().setErrorMessage(null);
            try {
                plugin.sleep(Math.max((int) downloadLink.getLinkStatus().getValue(), 2000), downloadLink);
            } catch (PluginException e) {
                downloadLink.getLinkStatus().setStatusText(null);
                DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
                return;
            }
        } else {
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
    }

    private void onErrorChunkloadFailed(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        if (linkStatus.getErrorMessage() == null) {
            linkStatus.setErrorMessage(JDL.L("plugins.error.downloadfailed", "Download failed"));
        }
        if (linkStatus.getValue() != LinkStatus.VALUE_FAILED_HASH) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                return;
            }
            retry(downloadLink, plugin);
        }
    }

    private void onErrorFatal(DownloadLink downloadLink, PluginForHost currentPlugin) {
        downloadLink.requestGuiUpdate();
    }

    private void onErrorFileExists(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus status = downloadLink.getLinkStatus();
        String[] fileExists = new String[] { JDL.L("system.download.triggerfileexists.overwrite", "Overwrite"), JDL.L("system.download.triggerfileexists.skip", "Skip Link"), JDL.L("system.download.triggerfileexists.rename", "Auto rename") };
        String title = JDL.L("jd.controlling.SingleDownloadController.askexists.title", "File exists");
        String msg = JDL.LF("jd.controlling.SingleDownloadController.askexists", "The file \r\n%s\r\n already exists. What do you want to do?", downloadLink.getFileOutput());
        int doit = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_FILE_EXISTS, 1);
        if (doit == 4) {

            // ask
            doit = UserIO.getInstance().requestComboDialog(UserIO.NO_COUNTDOWN, title, msg, fileExists, 0, null, null, null, null);

        }
        if (doit == 3) {
            if (downloadLink.getFilePackage().getIntegerProperty("DO_WHEN_EXISTS", -1) > 0) {

                doit = downloadLink.getFilePackage().getIntegerProperty("DO_WHEN_EXISTS", -1);

                int cd = UserIO.getCountdownTime();
                try {
                    UserIO.setCountdownTime(10);
                    doit = UserIO.getInstance().requestComboDialog(0, title, msg, fileExists, doit, null, null, null, null);
                    downloadLink.getFilePackage().setProperty("DO_WHEN_EXISTS", doit);
                } finally {
                    UserIO.setCountdownTime(cd);
                }
            } else {
                // ask
                doit = UserIO.getInstance().requestComboDialog(0, title, msg, fileExists, 0, null, null, null, null);
                downloadLink.getFilePackage().setProperty("DO_WHEN_EXISTS", doit);
            }

        }
        switch (doit) {
        case 1:
            status.setErrorMessage(JDL.L("controller.status.fileexists.skip", "File already exists."));
            downloadLink.setEnabled(false);
            break;
        case 2:
            // auto rename
            status.reset();
            File file = new File(downloadLink.getFileOutput());
            String filename = file.getName();
            String extension = JDIO.getFileExtension(file);
            String name = filename.substring(0, filename.length() - extension.length() - 1);
            int copy = 2;
            try {
                String[] num = new Regex(name, "(.*)_(\\d+)").getRow(0);
                copy = Integer.parseInt(num[1]) + 1;
                downloadLink.setFinalFileName(name + "_" + copy + "." + extension);
                while (new File(downloadLink.getFileOutput()).exists()) {
                    copy++;
                    downloadLink.setFinalFileName(name + "_" + copy + "." + extension);
                }
            } catch (Exception e) {
                copy = 2;
                downloadLink.setFinalFileName(name + "_" + copy + "." + extension);
            }

            break;
        default:

            if (new File(downloadLink.getFileOutput()).delete()) {
                status.reset();
            } else {
                status.addStatus(LinkStatus.ERROR_FATAL);
                status.setErrorMessage(JDL.L("controller.status.fileexists.overwritefailed", "Failed to overwrite") + downloadLink.getFileOutput());
            }
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
    }

    /**
     * Wird aufgerufenw ennd as Plugin einen filenot found Fehler meldet
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorFileNotFound(DownloadLink downloadLink, PluginForHost plugin) {
        logger.severe("File not found :" + downloadLink.getDownloadURL());
        downloadLink.setEnabled(false);
    }

    private void onErrorIncomplete(DownloadLink downloadLink, PluginForHost plugin) {
        retry(downloadLink, plugin);
    }

    private void onErrorNoConnection(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        logger.severe("Error occurred: No Server connection");
        long milliSeconds = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(WAIT_TIME_ON_CONNECTION_LOSS, 5 * 60) * 1000;
        linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        linkStatus.setWaitTime(milliSeconds);
        if (linkStatus.getErrorMessage() == null) {
            linkStatus.setErrorMessage(JDL.L("controller.status.connectionproblems", "Connection lost."));
        }
    }

    /**
     * Fehlerfunktion für einen UNbekannten premiumfehler.
     * Plugin-premium-support wird deaktiviert und link wird erneut versucht
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorPremium(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        linkStatus.reset();
    }

    private void onErrorLocalIO(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus status = downloadLink.getLinkStatus();
        /*
         * Value<=0 bedeutet das der link dauerhauft deaktiviert bleiben soll.
         * value>0 gibt die zeit an die der link deaktiviert bleiben muss in ms.
         * Der DownloadWatchdoggibt den Link wieder frei ewnn es zeit ist.
         */
        status.setWaitTime(30 * 60 * 1000l);
        downloadLink.setEnabled(false);
    }

    /**
     * Wird aufgerufen wenn ein Link kurzzeitig nicht verfügbar ist. ER wird
     * deaktiviert und kann zu einem späteren zeitpunkt wieder aktiviert werden
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorDownloadTemporarilyUnavailable(DownloadLink downloadLink, PluginForHost plugin) {
        logger.warning("Error occurred: Temporarily unavailable: Please wait " + downloadLink.getLinkStatus().getValue() + " ms for a retry");
        LinkStatus status = downloadLink.getLinkStatus();
        if (status.getErrorMessage() == null) status.setErrorMessage(JDL.L("controller.status.tempunavailable", "Temporarily unavailable"));

        /*
         * Value<0 bedeutet das der link dauerhauft deaktiviert bleiben soll.
         * value>0 gibt die zeit an die der link deaktiviert bleiben muss in ms.
         * value==0 macht default 30 mins Der DownloadWatchdoggibt den Link
         * wieder frei ewnn es zeit ist.
         */
        if (status.getValue() > 0) {
            status.setWaitTime(status.getValue());
        } else if (status.getValue() == 0) {
            status.setWaitTime(30 * 60 * 1000l);
        } else {
            status.resetWaitTime();
            downloadLink.setEnabled(false);
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
    }

    private void onErrorHostTemporarilyUnavailable(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus status = downloadLink.getLinkStatus();
        long milliSeconds = downloadLink.getLinkStatus().getValue();
        if (milliSeconds <= 0) {
            logger.severe(JDL.L("plugins.errors.pluginerror", "Plugin error. Please inform Support"));
            milliSeconds = 3600000l;
        }
        logger.warning("Error occurred: Download from this host is currently not possible: Please wait " + milliSeconds + " ms for a retry");
        status.setWaitTime(milliSeconds);
        DownloadWatchDog.getInstance().setTempUnavailWaittime(plugin.getHost(), milliSeconds);
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
    }

    /**
     * Diese Funktion wird aufgerufen wenn Ein Download mit einem Waittimefehler
     * abgebrochen wird
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorIPWaittime(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus status = downloadLink.getLinkStatus();
        long milliSeconds = downloadLink.getLinkStatus().getValue();

        if (milliSeconds <= 0) {
            logger.severe(JDL.L("plugins.errors.pluginerror", "Plugin error. Please inform Support"));
            milliSeconds = 3600000l;
        }
        status.setWaitTime(milliSeconds);
        DownloadWatchDog.getInstance().setIPBlockWaittime(plugin.getHost(), milliSeconds);
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    // @Override
    @Override
    public void run() {
        try {
            /**
             * Das Plugin, das den aktuellen Download steuert
             */
            PluginForHost plugin;
            linkStatus.setStatusText(null);
            linkStatus.setErrorMessage(null);
            linkStatus.resetWaitTime();
            logger.info("Start working on " + downloadLink.getName());
            currentPlugin = plugin = downloadLink.getPlugin();
            fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_ACTIVE, this));
            if (downloadLink.getDownloadURL() == null) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("controller.status.containererror", "Container Error"));
                downloadLink.getLinkStatus().setErrorMessage(JDL.L("controller.status.containererror", "Container Error"));
                downloadLink.setEnabled(false);
                fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_INACTIVE, this));
                return;
            }
            /* check ob Datei existiert oder bereits geladen wird */
            synchronized (DUPELOCK) {
                /*
                 * dieser sync block dient dazu das immer nur ein link gestartet
                 * wird und dann der dupe check durchgeführt werden kann
                 */
                if (DownloadInterface.preDownloadCheckFailed(downloadLink)) {
                    onErrorLinkBlock(downloadLink, currentPlugin);
                    fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_INACTIVE, this));
                    return;
                }
                /*
                 * setinprogress innerhalb des sync damit keine 2 downloads
                 * gleichzeitig in progress übergehen können
                 */
                linkStatus.setInProgress(true);
            }
            handlePlugin();
            fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_INACTIVE, this));
            plugin.clean();
            downloadLink.requestGuiUpdate();
        } finally {
            linkStatus.setInProgress(false);
            linkStatus.setActive(false);
            /* cleanup the DownloadInterface/Controller references */
            downloadLink.setDownloadLinkController(null);
            downloadLink.setDownloadInstance(null);
        }
    }

}