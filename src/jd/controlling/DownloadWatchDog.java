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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Dieser Controller verwaltet die downloads. Während StartDownloads.java für
 * die Steuerung eines einzelnen Downloads zuständig ist, ist DownloadWatchdog
 * für die Verwaltung und steuerung der ganzen Download Liste zuständig
 * 
 * @author JD-Team
 * 
 */
public class DownloadWatchDog implements ControlListener, DownloadControllerListener {

    private static final String JDL_PREFIX = "jd.controlling.DownloadWatchDog.";

    public static enum STATE {
        RUNNING, NOT_RUNNING, STOPPING
    }

    private boolean aborted = false;

    private boolean aborting;

    private HashMap<DownloadLink, SingleDownloadController> DownloadControllers = new HashMap<DownloadLink, SingleDownloadController>();
    private ArrayList<DownloadLink> stopMarkTracker = new ArrayList<DownloadLink>();

    private HashMap<String, Long> HOST_IPBLOCK = new HashMap<String, Long>();
    private HashMap<String, Long> HOST_TEMP_UNAVAIL = new HashMap<String, Long>();

    private static final Object nostopMark = new Object();
    private static final Object hiddenstopMark = new Object();
    private Object stopMark = nostopMark;

    private HashMap<String, Integer> activeHosts = new HashMap<String, Integer>();

    private Logger logger = JDLogger.getLogger();

    private boolean paused = false;

    private int totalSpeed = 0;

    private Thread watchDogThread = null;

    private DownloadController dlc = null;

    private int activeDownloads = 0;
    private int downloadssincelastStart = 0;

    private final static Object StartStopSync = new Object();

    private final static Object CountLOCK = new Object();

    private final static Object DownloadLOCK = new Object();

    /**
     * Hier kann de Status des Downloads gespeichert werden.
     */
    private STATE downloadStatus = STATE.NOT_RUNNING;

    private static DownloadWatchDog INSTANCE;

    public synchronized static DownloadWatchDog getInstance() {
        if (INSTANCE == null) INSTANCE = new DownloadWatchDog();
        return INSTANCE;
    }

    public int getActiveDownloads() {
        return activeDownloads;
    }

    /**
     * set ipblock waittime for given host. <0 for disable/delete
     * 
     * @param host
     * @param until
     */
    public void setIPBlockWaittime(String host, long waittime) {
        synchronized (HOST_IPBLOCK) {
            if (waittime <= 0) {
                HOST_IPBLOCK.remove(host);
            } else
                HOST_IPBLOCK.put(host, System.currentTimeMillis() + waittime);
        }
    }

    public void setTempUnavailWaittime(String host, long waittime) {
        synchronized (HOST_TEMP_UNAVAIL) {
            if (waittime <= 0) {
                HOST_TEMP_UNAVAIL.remove(host);
            } else
                HOST_TEMP_UNAVAIL.put(host, System.currentTimeMillis() + waittime);
        }
    }

    public void resetIPBlockWaittime(String host) {
        synchronized (HOST_IPBLOCK) {
            if (host != null) {
                HOST_IPBLOCK.remove(host);
            } else
                HOST_IPBLOCK.clear();
        }
    }

    public void resetTempUnavailWaittime(String host) {
        synchronized (HOST_TEMP_UNAVAIL) {
            if (host != null) {
                HOST_TEMP_UNAVAIL.remove(host);
            } else
                HOST_TEMP_UNAVAIL.clear();
        }
    }

    public long getRemainingTempUnavailWaittime(String host) {
        synchronized (HOST_TEMP_UNAVAIL) {
            if (!HOST_TEMP_UNAVAIL.containsKey(host)) return 0;
            return Math.max(0, HOST_TEMP_UNAVAIL.get(host) - System.currentTimeMillis());
        }
    }

    public long getRemainingIPBlockWaittime(String host) {
        synchronized (HOST_IPBLOCK) {
            if (!HOST_IPBLOCK.containsKey(host)) return 0;
            return Math.max(0, HOST_IPBLOCK.get(host) - System.currentTimeMillis());
        }
    }

    private DownloadWatchDog() {
        dlc = DownloadController.getInstance();
        dlc.addListener(this);
    }

    public void setStopMark(Object entry) {
        synchronized (stopMark) {
            if (entry == null) entry = nostopMark;
            if (stopMark instanceof DownloadLink) {
                DownloadController.getInstance().fireDownloadLinkUpdate(stopMark);
            } else if (stopMark instanceof FilePackage) {
                DownloadController.getInstance().fireDownloadLinkUpdate(((FilePackage) stopMark).get(0));
            }
            stopMark = entry;
            ToolBarAction stopMark = ActionController.getToolBarAction("toolbar.control.stopmark");
            if (entry instanceof DownloadLink) {
                stopMark.setSelected(true);
                stopMark.setEnabled(true);
                stopMark.setIcon("gui.images.stopmark.enabled");
                stopMark.setToolTipText(JDL.LF(JDL_PREFIX + "stopmark.downloadlink", "Stopmark is set on Downloadlink: %s", ((DownloadLink) entry).getName()));
                DownloadController.getInstance().fireDownloadLinkUpdate(entry);
            } else if (entry instanceof FilePackage) {
                stopMark.setSelected(true);
                stopMark.setEnabled(true);
                stopMark.setIcon("gui.images.stopmark.enabled");
                stopMark.setToolTipText(JDL.LF(JDL_PREFIX + "stopmark.filepackage", "Stopmark is set on Filepackage: %s", ((FilePackage) entry).getName()));
                DownloadController.getInstance().fireDownloadLinkUpdate(((FilePackage) entry).get(0));
            } else if (entry == hiddenstopMark) {
                stopMark.setSelected(true);
                stopMark.setEnabled(true);
                stopMark.setIcon("gui.images.stopmark.enabled");
                stopMark.setToolTipText(JDL.L(JDL_PREFIX + "stopmark.set", "Stopmark is still set!"));
            } else if (entry == nostopMark) {
                stopMark.setSelected(false);
                stopMark.setIcon("gui.images.stopmark.disabled");
                stopMark.setToolTipText(JDL.L("jd.gui.swing.jdgui.actions.actioncontroller.toolbar.control.stopmark.tooltip", "Stop after current Downloads"));
            }

        }
    }

    public boolean isStopMarkSet() {
        return stopMark != nostopMark;
    }

    public void toggleStopMark(Object entry) {
        synchronized (stopMark) {
            if (entry == null || entry == stopMark) {
                setStopMark(nostopMark);
                return;
            }
            setStopMark(entry);
        }
    }

    public Object getStopMark() {
        return stopMark;
    }

    public boolean isStopMark(Object item) {
        return stopMark == item;
    }

    /**
     * Gibt den Status (ID) der downloads zurück
     * 
     * @return
     */
    public STATE getDownloadStatus() {
        return downloadStatus;
    }

    /**
     * Startet den Downloadvorgang. Dies eFUnkton sendet das startdownload event
     * und aktiviert die ersten downloads.
     */
    public boolean startDownloads() {
        if (downloadStatus == STATE.STOPPING || downloadStatus == STATE.RUNNING) return false;
        synchronized (StartStopSync) {
            if (downloadStatus == STATE.NOT_RUNNING) {
                /* set state to running */
                downloadStatus = STATE.RUNNING;
                /* clear stopMarkTracker */
                stopMarkTracker.clear();
                /* remove stopsign if it is reached */
                if (reachedStopMark()) setStopMark(nostopMark);
                /* restore speed limit */
                if (SubConfiguration.getConfig("DOWNLOAD").getProperty("MAXSPEEDBEFOREPAUSE", null) != null) {
                    logger.info("Restoring old speedlimit");
                    SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("MAXSPEEDBEFOREPAUSE", 0));
                    SubConfiguration.getConfig("DOWNLOAD").setProperty("MAXSPEEDBEFOREPAUSE", null);
                    SubConfiguration.getConfig("DOWNLOAD").save();
                }
                /* full start reached */
                logger.info("DownloadWatchDog: start");
                JDController.getInstance().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_START, this));
                /* reset downloadcounter */
                downloadssincelastStart = 0;
                startWatchDogThread();
                return true;
            }
            return false;
        }
    }

    /**
     * Startet den download wenn er angehalten ist und hält ihn an wenn er läuft
     */
    public void toggleStartStop() {
        if (!startDownloads()) {
            stopDownloads();
        }
    }

    /**
     * Bricht den Download ab und blockiert bis er abgebrochen wurde.
     */
    public boolean stopDownloads() {
        if (downloadStatus == STATE.STOPPING || downloadStatus == STATE.NOT_RUNNING) return false;
        synchronized (StartStopSync) {
            if (downloadStatus == STATE.RUNNING) {
                /* set state to stopping */
                downloadStatus = STATE.STOPPING;
                /* check if there are still running downloads, if so abort them */
                aborted = true;
                if (this.getActiveDownloads() > 0) abort();
                /* clear Status */
                clearDownloadListStatus();
                pauseDownloads(false);
                /* remove stopsign if it is reached */
                if (reachedStopMark()) setStopMark(nostopMark);
                /* full stop reached */
                logger.info("DownloadWatchDog: stop");
                downloadStatus = STATE.NOT_RUNNING;
                JDController.getInstance().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_STOP, this));
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Bricht den Watchdog ab. Alle laufenden downloads werden beendet und die
     * downloadliste zurückgesetzt. Diese Funktion blockiert bis alle Downloads
     * erfolgreich abgeborhcen wurden.
     */
    void abort() {
        aborting = true;
        logger.finer("Abort all active Downloads");
        ProgressController progress = new ProgressController(JDL.LF(JDL_PREFIX + "stopping", "Stopping all downloads %s", activeDownloads), activeDownloads, null);
        ArrayList<DownloadLink> al = new ArrayList<DownloadLink>();
        ArrayList<SingleDownloadController> cons = null;
        synchronized (DownloadControllers) {
            cons = new ArrayList<SingleDownloadController>(DownloadControllers.values());
        }
        for (SingleDownloadController singleDownloadController : cons) {
            al.add(singleDownloadController.abortDownload().getDownloadLink());
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(al);
        boolean check = true;
        while (true) {
            progress.setStatusText("Stopping all downloads " + activeDownloads);
            check = true;
            ArrayList<DownloadLink> links = getRunningDownloads();
            for (DownloadLink link : links) {
                if (link.getLinkStatus().isPluginActive()) {
                    check = false;
                    break;
                }
            }
            if (check) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            synchronized (DownloadControllers) {
                cons = new ArrayList<SingleDownloadController>(DownloadControllers.values());
            }
            for (SingleDownloadController singleDownloadController : cons) {
                al.add(singleDownloadController.abortDownload().getDownloadLink());
            }
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(al);
        progress.doFinalize();
        aborting = false;
    }

    /**
     * Setzt den Status der Downloadliste zurück. zB. bei einem Abbruch
     */
    private void clearDownloadListStatus() {
        ArrayList<DownloadLink> links = getRunningDownloads();
        for (DownloadLink link : links) {
            this.deactivateDownload(link);
        }
        resetIPBlockWaittime(null);
        resetTempUnavailWaittime(null);
        ArrayList<FilePackage> fps;
        fps = dlc.getPackages();
        synchronized (fps) {
            for (FilePackage filePackage : fps) {
                links = filePackage.getDownloadLinkList();
                for (int i = 0; i < links.size(); i++) {
                    DownloadLink link = links.get(i);
                    link.setAborted(false);
                    /*
                     * do not reset if link is offline, finished , already exist
                     * or pluginerror (because only plugin updates can fix this)
                     */
                    link.getLinkStatus().resetStatus(LinkStatus.ERROR_FATAL | LinkStatus.ERROR_PLUGIN_DEFECT | LinkStatus.ERROR_ALREADYEXISTS, LinkStatus.ERROR_FILE_NOT_FOUND, LinkStatus.FINISHED);
                }
            }
        }
        DownloadController.getInstance().fireGlobalUpdate();
    }

    public ArrayList<DownloadLink> getRunningDownloads() {
        synchronized (DownloadControllers) {
            return new ArrayList<DownloadLink>(DownloadControllers.keySet());
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_PLUGIN_INACTIVE && event.getSource() instanceof PluginForHost) {
            this.deactivateDownload(((SingleDownloadController) event.getParameter()).getDownloadLink());
        }
    }

    public int activeDownloadControllers() {
        synchronized (DownloadControllers) {
            return DownloadControllers.keySet().size();
        }
    }

    public int getDownloadssincelastStart() {
        return downloadssincelastStart;
    }

    /**
     * Zählt die Downloads die bereits über das Hostplugin laufen
     * 
     * @param plugin
     * @return Anzahl der downloads über das plugin
     */
    private int activeDownloadsbyHosts(PluginForHost plugin) {
        return activeDownloadsbyHosts(plugin.getHost());
    }

    private int activeDownloadsbyHosts(String host) {
        synchronized (this.activeHosts) {
            if (activeHosts.containsKey(host)) { return activeHosts.get(host); }
        }
        return 0;
    }

    private void activateDownload(DownloadLink link, SingleDownloadController con) {
        synchronized (DownloadControllers) {
            if (DownloadControllers.containsKey(link)) return;
            DownloadControllers.put(link, con);
            synchronized (CountLOCK) {
                this.activeDownloads++;
                downloadssincelastStart++;
            }
        }
        String cl = link.getHost();
        synchronized (this.activeHosts) {
            if (activeHosts.containsKey(cl)) {
                int count = activeHosts.get(cl);
                activeHosts.put(cl, count + 1);
            } else {
                activeHosts.put(cl, 1);
            }
        }
    }

    private void deactivateDownload(DownloadLink link) {
        synchronized (DownloadControllers) {
            if (!DownloadControllers.containsKey(link)) {
                logger.severe("Link not in ControllerList!");
                return;
            }
            DownloadControllers.remove(link);
            synchronized (CountLOCK) {
                this.activeDownloads--;
            }
        }
        String cl = link.getHost();
        synchronized (this.activeHosts) {
            if (activeHosts.containsKey(cl)) {
                int count = activeHosts.get(cl);
                if (count - 1 < 0) {
                    logger.severe("WatchDog Counter MissMatch!!");
                    activeHosts.remove(cl);
                } else
                    activeHosts.put(cl, count - 1);
            } else
                logger.severe("WatchDog Counter MissMatch!!");
        }
    }

    /**
     * Liefert den nächsten DownloadLink
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadLink getNextDownloadLink() {
        synchronized (DownloadLOCK) {
            if (this.reachedStopMark()) return null;
            DownloadLink nextDownloadLink = null;
            DownloadLink returnDownloadLink = null;
            try {
                for (FilePackage filePackage : dlc.getPackages()) {
                    for (Iterator<DownloadLink> it2 = filePackage.getDownloadLinkList().iterator(); it2.hasNext();) {
                        nextDownloadLink = it2.next();
                        if (nextDownloadLink.isEnabled() && !nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                            if (nextDownloadLink.getPlugin().isPremiumDownload() || (getRemainingIPBlockWaittime(nextDownloadLink.getHost()) <= 0 && getRemainingTempUnavailWaittime(nextDownloadLink.getHost()) <= 0)) {
                                if (!isDownloadLinkActive(nextDownloadLink)) {
                                    if (!nextDownloadLink.getLinkStatus().isPluginActive()) {
                                        if (nextDownloadLink.getLinkStatus().isStatus(LinkStatus.TODO)) {
                                            int maxPerHost = getSimultanDownloadNumPerHost();
                                            if (activeDownloadsbyHosts(nextDownloadLink.getPlugin()) < (nextDownloadLink.getPlugin()).getMaxSimultanDownloadNum() && activeDownloadsbyHosts(nextDownloadLink.getPlugin()) < maxPerHost && nextDownloadLink.getPlugin().getWrapper().isEnabled()) {
                                                if (returnDownloadLink == null) {
                                                    returnDownloadLink = nextDownloadLink;
                                                } else {
                                                    if (nextDownloadLink.getPriority() > returnDownloadLink.getPriority()) returnDownloadLink = nextDownloadLink;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
            return returnDownloadLink;
        }
    }

    /**
     * try to force a downloadstart, will ignore maxperhost and maxdownloads
     * limits
     */
    public void forceDownload(ArrayList<DownloadLink> links) {
        synchronized (StartStopSync) {
            if (downloadStatus != STATE.RUNNING) return;
        }
        synchronized (DownloadLOCK) {
            for (DownloadLink link : links) {
                if (!link.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                    if (link.getPlugin().isPremiumDownload() || (getRemainingIPBlockWaittime(link.getHost()) <= 0 && getRemainingTempUnavailWaittime(link.getHost()) <= 0)) {
                        if (!isDownloadLinkActive(link)) {
                            if (!link.getLinkStatus().isPluginActive()) {
                                if (link.getLinkStatus().isStatus(LinkStatus.TODO)) {
                                    int activePerHost = activeDownloadsbyHosts(link.getPlugin());
                                    if (activePerHost < (link.getPlugin()).getMaxSimultanDownloadNum() && link.getPlugin().getWrapper().isEnabled()) {
                                        if (!link.isEnabled()) link.setEnabled(true);
                                        startDownloadThread(link);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Gibt die Configeinstellung zurück, wieviele simultane Downloads der user
     * erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNum() {
        return SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2);
    }

    /**
     * Gibt die Configeinstellung zurück, wieviele simultane Downloads der user
     * pro Hoster erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNumPerHost() {
        if (SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST, 0) == 0) return Integer.MAX_VALUE;
        return SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST, 0);
    }

    /**
     * @return the totalSpeed
     */
    public int getTotalSpeed() {
        if (downloadStatus == STATE.NOT_RUNNING) return 0;
        return totalSpeed;
    }

    boolean isDownloadLinkActive(DownloadLink nextDownloadLink) {
        synchronized (DownloadControllers) {
            return DownloadControllers.containsKey(nextDownloadLink);
        }
    }

    public void pauseDownloads(boolean value) {
        if (paused == value) return;
        paused = value;
        if (value) {
            ActionController.getToolBarAction("toolbar.control.pause").setSelected(true);
            SubConfiguration.getConfig("DOWNLOAD").setProperty("MAXSPEEDBEFOREPAUSE", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10));
            logger.info("Pause enabled: Reducing downloadspeed to " + SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10) + " kb/s");
        } else {
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("MAXSPEEDBEFOREPAUSE", 0));
            SubConfiguration.getConfig("DOWNLOAD").setProperty("MAXSPEEDBEFOREPAUSE", null);
            logger.info("Pause disabled: Switch back to old downloadspeed");
            ActionController.getToolBarAction("toolbar.control.pause").setSelected(false);
        }
        SubConfiguration.getConfig("DOWNLOAD").save();
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean newDLStartAllowed() {
        if (paused || Reconnecter.isReconnecting() || aborting || aborted || Reconnecter.isReconnectPrefered()) return false;
        return true;
    }

    private synchronized void startWatchDogThread() {
        if (this.watchDogThread == null || !this.watchDogThread.isAlive()) {
            /**
             * Workaround, due to activeDownloads bug.
             */
            synchronized (CountLOCK) {
                this.activeDownloads = 0;
            }
            watchDogThread = new Thread() {
                @Override
                public void run() {
                    JDUtilities.getController().addControlListener(INSTANCE);
                    this.setName("DownloadWatchDog");
                    ArrayList<DownloadLink> links;
                    ArrayList<DownloadLink> updates = new ArrayList<DownloadLink>();
                    ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
                    DownloadLink link;
                    LinkStatus linkStatus;
                    boolean hasInProgressLinks;
                    boolean hasTempDisabledLinks;
                    aborted = false;
                    aborting = false;
                    int stopCounter = 5;
                    int currentTotalSpeed = 0;
                    int inProgress = 0;
                    while (aborted != true) {
                        Reconnecter.setReconnectRequested(false);
                        hasInProgressLinks = false;
                        hasTempDisabledLinks = false;

                        /* so we can work on a list without threading errors */
                        fps.clear();
                        synchronized (DownloadController.ControllerLock) {
                            synchronized (dlc.getPackages()) {
                                fps.addAll(dlc.getPackages());
                            }
                        }
                        currentTotalSpeed = 0;
                        inProgress = 0;
                        updates.clear();
                        try {
                            for (FilePackage filePackage : fps) {
                                links = filePackage.getDownloadLinkList();
                                for (int i = 0; i < links.size(); i++) {
                                    link = links.get(i);
                                    if (link.getPlugin() == null) continue;
                                    linkStatus = link.getLinkStatus();
                                    if (!linkStatus.hasStatus(LinkStatus.PLUGIN_IN_PROGRESS) && link.isEnabled()) {
                                        /* enabled and not in progress */
                                        if ((linkStatus.hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) || linkStatus.hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE))) {
                                            /* download or hoster temp. unavail */
                                            if (linkStatus.getRemainingWaittime() == 0) {
                                                /* reset if waittime is over */
                                                linkStatus.reset();
                                            } else if (linkStatus.getRemainingWaittime() > 0) {
                                                /*
                                                 * we have temp. unavail links
                                                 * in list
                                                 */
                                                hasTempDisabledLinks = true;
                                                updates.add(link);
                                            }
                                        } else if (linkStatus.hasStatus(LinkStatus.ERROR_IP_BLOCKED)) {
                                            /* ip blocked link */
                                            if (linkStatus.getRemainingWaittime() == 0) {
                                                /* reset if waittime is over */
                                                linkStatus.reset();
                                            } else if (linkStatus.getRemainingWaittime() > 0) {
                                                /*
                                                 * we request a reconnect if
                                                 * possible
                                                 */
                                                if (activeDownloadsbyHosts(link.getHost()) == 0) {
                                                    /*
                                                     * do not reconnect if the
                                                     * request comes from host
                                                     * with active downloads,
                                                     * this will prevent
                                                     * reconnect loops for
                                                     * plugins that allow resume
                                                     * and parallel downloads
                                                     */
                                                    Reconnecter.setReconnectRequested(true);
                                                }
                                                updates.add(link);
                                            }
                                        } else if (getRemainingTempUnavailWaittime(link.getHost()) > 0 && !link.getLinkStatus().isFinished()) {
                                            /*
                                             * we have links that are temp.
                                             * unavail in list
                                             */

                                            hasTempDisabledLinks = true;
                                            updates.add(link);
                                        } else if (getRemainingIPBlockWaittime(link.getHost()) > 0 && !link.getLinkStatus().isFinished()) {
                                            /*
                                             * we have links that are ipblocked
                                             * in list
                                             */
                                            if (activeDownloadsbyHosts(link.getHost()) == 0) {
                                                /*
                                                 * do not reconnect if the
                                                 * request comes from host with
                                                 * active downloads, this will
                                                 * prevent reconnect loops for
                                                 * plugins that allow resume and
                                                 * parallel downloads
                                                 */
                                                Reconnecter.setReconnectRequested(true);
                                            }
                                            updates.add(link);
                                        }
                                    } else if (link.isEnabled()) {
                                        /* enabled links */
                                        if (linkStatus.isPluginActive()) {
                                            /* we have active links in list */
                                            hasInProgressLinks = true;
                                        }
                                        if (linkStatus.hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                                            /* link is downloading atm */
                                            inProgress++;
                                            currentTotalSpeed += link.getDownloadSpeed();
                                        }
                                    }
                                }
                            }
                            /* request a reconnect if allowed and needed */
                            Reconnecter.doReconnectIfRequested(false);
                            if (inProgress > 0) {
                                /* calc speed */
                                for (FilePackage filePackage : fps) {
                                    Iterator<DownloadLink> iter = filePackage.getDownloadLinkList().iterator();
                                    int maxspeed = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) * 1024;
                                    if (maxspeed == 0) {
                                        maxspeed = Integer.MAX_VALUE;
                                    }
                                    int overhead = maxspeed - currentTotalSpeed;
                                    totalSpeed = currentTotalSpeed;
                                    DownloadLink element;
                                    while (iter.hasNext()) {
                                        element = iter.next();
                                        if (element.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                                            element.setSpeedLimit(element.getDownloadSpeed() + overhead / inProgress);
                                        }
                                    }
                                }
                            } else {
                                /* no downloads in progress, speed=0 */
                                totalSpeed = 0;
                            }
                            if (updates.size() > 0) {
                                /* fire gui updates */
                                DownloadController.getInstance().fireDownloadLinkUpdate(updates);
                            }
                            int ret = 0;
                            if (activeDownloads < getSimultanDownloadNum()) {
                                if (!reachedStopMark()) ret = setDownloadActive();
                            }
                            if (ret == 0) {
                                /*
                                 * no new download got started, check what
                                 * happened and what to do next
                                 */
                                if (!hasTempDisabledLinks && !hasInProgressLinks && !Reconnecter.isReconnectRequested() && getNextDownloadLink() == null && activeDownloads == 0) {
                                    /*
                                     * no tempdisabled, no in progress, no
                                     * reconnect and no next download waiting
                                     * and no active downloads
                                     */
                                    if (newDLStartAllowed()) {
                                        /*
                                         * only start countdown to stop
                                         * downloads if we were allowed to start
                                         * new ones
                                         */
                                        stopCounter--;
                                        logger.info(stopCounter + "rounds left to start new downloads");
                                    }
                                    if (stopCounter == 0) {
                                        /*
                                         * countdown reached, prepare to stop
                                         * downloadwatchdog
                                         */
                                        totalSpeed = 0;
                                        break;
                                    }
                                }
                            } else {
                                /*
                                 * reset countdown, because we new downloads got
                                 * started
                                 */
                                stopCounter = 5;
                            }
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                    aborted = true;
                    while (aborting) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
                    JDUtilities.getController().removeControlListener(INSTANCE);
                    DownloadWatchDog.getInstance().stopDownloads();
                }
            };
            watchDogThread.start();
        }
    }

    /**
     * Aktiviert solange neue Downloads, bis die Maxmalanzahl erreicht ist oder
     * die Liste zueende ist
     * 
     * @return
     */
    private int setDownloadActive() {
        DownloadLink dlink;
        int ret = 0;
        if (!newDLStartAllowed()) return ret;
        while (activeDownloads < getSimultanDownloadNum()) {
            dlink = getNextDownloadLink();
            if (dlink == null) {
                break;
            }
            if (dlink != getNextDownloadLink()) {
                break;
            }
            if (reachedStopMark()) return ret;
            startDownloadThread(dlink);
            ret++;
        }
        return ret;
    }

    /**
     * @param totalSpeed
     *            the totalSpeed to set
     */
    public void setTotalSpeed(int totalSpeed) {
        this.totalSpeed = totalSpeed;
    }

    private boolean reachedStopMark() {
        synchronized (stopMark) {
            if (stopMark == hiddenstopMark) return true;
            if (stopMark instanceof DownloadLink) {
                if (stopMarkTracker.contains(stopMark)) return true;
                DownloadLink dl = ((DownloadLink) stopMark);
                if (!dl.isEnabled()) return true;
                if (dl.getLinkStatus().isFinished()) return true;
                return false;
            }
            if (stopMark instanceof FilePackage) {
                for (DownloadLink dl : ((FilePackage) stopMark).getDownloadLinkList()) {
                    if (stopMarkTracker.contains(dl)) continue;
                    if (dl.isEnabled() && dl.getLinkStatus().isFinished()) continue;
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    private void startDownloadThread(DownloadLink dlink) {
        dlink.getLinkStatus().setActive(true);
        SingleDownloadController download = new SingleDownloadController(dlink);
        logger.info("Start new Download: " + dlink.getHost());
        this.activateDownload(dlink, download);
        /* add download to stopMarkTracker */
        if (!stopMarkTracker.contains(dlink)) stopMarkTracker.add(dlink);
        download.start();

        // TODO: rework this
        // Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_DOWNLOAD,
        // dlink);
        //        
        // String outputFilePath = dlink.getFileOutput();
        // int index = outputFilePath.lastIndexOf(PATH_SEPARATOR);
        // String outputFolder = outputFilePath.substring(0, index);
        //        
        // File destinationFolder = new File(outputFolder);
        // if (!destinationFolder.exists()) {
        // boolean createFolder = destinationFolder.mkdirs();
        // if (!createFolder) {
        // logger.warning("The folder '" + destinationFolder +
        // "' couldn't be created, cannot get free space");
        // LinkStatus status = dlink.getLinkStatus();
        // status.setErrorMessage("Cannot create folder " + outputFolder);
        // status.addStatus(LinkStatus.ERROR_LOCAL_IO);
        //	        	
        // dlink.setEnabled(false);
        // dlink.requestGuiUpdate();
        // } else {
        // logger.info("The folder '" + destinationFolder +
        // "' was successfully created, continuing download...");
        // }
        // }
        //        
        // long freeDiskSpace = destinationFolder.getUsableSpace();
        // long fileSize = dlink.getDownloadSize();
        // if (freeDiskSpace > fileSize) {
        // SingleDownloadController download = new
        // SingleDownloadController(dlink);
        // logger.info("Start new Download: " + dlink.getHost());
        // dlink.getLinkStatus().setActive(true);
        // this.activateDownload(dlink, download);
        // /* add download to stopMarkTracker */
        // if (!stopMarkTracker.contains(dlink)) stopMarkTracker.add(dlink);
        // download.start();
        // } else {
        // String fileName = dlink.getName();
        // logger.warning("The file '" + fileName +
        // "' is bigger than the available free space on '"
        // + outputFolder + "' [" + fileSize + " > " + freeDiskSpace + "]");
        //        	
        // String title =
        // JDL.L("system.download.errors.notenoughfreespace.title",
        // "There is not enough free space on '" + outputFolder + "'");
        // String message =
        // JDL.L("system.download.errors.notenoughfreespace.message",
        // "There is not enough free space to download the file '" + fileName +
        // "', please relocate the file");
        // UserIO.getInstance().requestConfirmDialog(UserIO.NO_CANCEL_OPTION,
        // title, message);
        //        	
        // LinkStatus status = dlink.getLinkStatus();
        // status.setErrorMessage(message);
        // status.addStatus(LinkStatus.ERROR_LOCAL_IO);
        //        	
        // dlink.setEnabled(false);
        // dlink.requestGuiUpdate();
        // }
    }

    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getID()) {
        case DownloadControllerEvent.REMOVE_FILPACKAGE:
        case DownloadControllerEvent.REMOVE_DOWNLOADLINK:
            synchronized (stopMark) {
                if (stopMark == event.getParameter()) setStopMark(hiddenstopMark);
            }
            break;
        }

    }
}
