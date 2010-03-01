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

//
//    Alle Ausgaben sollten lediglich eine Zeile lang sein, um die kompatibilität zu erhöhen.
//

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.linkgrabberview.LinkGrabberPanel;
import jd.http.Browser;
import jd.nrouter.IPCheck;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.nutils.httpserver.Handler;
import jd.nutils.httpserver.HttpServer;
import jd.nutils.httpserver.Request;
import jd.nutils.httpserver.Response;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

@OptionalPlugin(rev = "$Revision$", id = "remotecontrol", interfaceversion = 5)
public class JDRemoteControl extends PluginOptional implements ControlListener, LinkGrabberControllerListener {

    private static final String PARAM_PORT = "PORT";
    private static final String PARAM_ENABLED = "ENABLED";
    private final SubConfiguration subConfig;

    private boolean grabberIsBusy;

    private static final String LINK_TYPE_OFFLINE = "offline";
    private static final String LINK_TYPE_AVAIL = "available";

    private static final String ERROR_MALFORMED_REQUEST = "JDRemoteControl - Malformed request. Use /help";
    private static final String ERROR_LINK_GRABBER_RUNNING = "ERROR: Link grabber is currently running. Please try again in a few seconds.";

    // private static final String ERROR_TOO_FEW_PARAMETERS =
    // "ERROR: Too few request parametes: check /help for instructions.";

    public JDRemoteControl(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = getPluginConfig();
        initConfig();
        LinkGrabberController.getInstance().addListener(this);
    }

    @Override
    public String getIconKey() {
        return "gui.images.network";
    }

    private class Serverhandler implements Handler {

        public void handle(Request request, Response response) {
            Document xml = JDUtilities.parseXmlString("<jdownloader></jdownloader>", false);

            response.setReturnType("text/html");
            response.setReturnStatus(Response.OK);

            if (request.getRequestUrl().equals("/help")) {
                // Get help page

                Vector<String> commandvec = new Vector<String>();
                Vector<String> infovector = new Vector<String>();

                // Values table
                commandvec.add(" ");
                infovector.add("Get values");

                commandvec.add("/get/rcversion");
                infovector.add("Get RemoteControl version");

                commandvec.add("/get/version");
                infovector.add("Get version");

                commandvec.add("/get/config");
                infovector.add("Get config");

                commandvec.add("/get/ip");
                infovector.add("Get IP");

                commandvec.add("/get/randomip");
                infovector.add("Get random IP as replacement for real IP-check");

                commandvec.add("/get/speed");
                infovector.add("Get current speed");

                commandvec.add("/get/speedlimit");
                infovector.add("Get current speed limit");

                commandvec.add("/get/downloadstatus");
                infovector.add("Get download status => RUNNING, NOT_RUNNING or STOPPING");

                commandvec.add("/get/isreconnect");
                infovector.add("Get whether reconnect is enabled or not");

                commandvec.add("/get/grabber/list");
                infovector.add("Get all links that are currently held by the link grabber (XML)");

                commandvec.add("/get/grabber/count");
                infovector.add("Get amount of all links in linkgrabber");

                commandvec.add("/get/grabber/isbusy");
                infovector.add("Get whether linkgrabber is busy or not");

                commandvec.add("/get/downloads/all/count");
                infovector.add("Get amount of all downloads");

                commandvec.add("/get/downloads/current/count");
                infovector.add("Get amount of current downloads");

                commandvec.add("/get/downloads/finished/count");
                infovector.add("Get amount of finished downloads");

                commandvec.add("/get/downloads/all/list");
                infovector.add("Get list of all downloads (XML)");

                commandvec.add("/get/downloads/current/list");
                infovector.add("Get list of current downloads (XML)");

                commandvec.add("/get/downloads/finished/list");
                infovector.add("Get list of finished downloads (XML)");

                // Actions table
                commandvec.add(" ");
                infovector.add("Actions");

                commandvec.add("/action/start");
                infovector.add("Start downloads");

                commandvec.add("/action/pause");
                infovector.add("Pause downloads");

                commandvec.add("/action/stop");
                infovector.add("Stop downloads");

                commandvec.add("/action/toggle");
                infovector.add("Toggle downloads");

                commandvec.add("/action/reconnect");
                infovector.add("Reconnect");

                commandvec.add("/action/(force)update");
                infovector.add("Do a webupdate - /action/forceupdate will activate auto-restart if update is possible");

                commandvec.add("/action/restart");
                infovector.add("Restart JDownloader");

                commandvec.add("/action/shutdown");
                infovector.add("Shutdown JDownloader");

                commandvec.add("/action/set/download/limit/%X%");
                infovector.add("Set download speedlimit %X%");

                commandvec.add("/action/set/download/max/%X%");
                infovector.add("Set max. sim. Downloads %X%");

                commandvec.add("/action/set/reconnect/(true|false)");
                infovector.add("Set reconnect enabled or not");

                commandvec.add("/action/set/premium/(true|false)");
                infovector.add("Set premium usage enabled or not");

                commandvec.add("/action/add(/confirm)(/start)/links/%X%");
                infovector.add("Add links %X% to grabber<br/>" + "e.g. /action/add/links/http://tinyurl.com/6o73eq" + "<p>Note: Links must be URLEncoded. Use NEWLINE between Links!!</p>");

                commandvec.add("/action/add(/confirm)(/start)/container/%X%");
                infovector.add("Add (remote or local) container %X%<br/>" + "e.g. /action/add/container/C:\\container.dlc" + "<p>Note: Address (remote or local) must be URLEncoded.</p>");

                commandvec.add("/action/save/container(/fromgrabber)/%X%");
                infovector.add("Save DLC-container with all links to %X%<br/>" + "e.g. /action/add/container/%X%" + "<p>fromgrabber: save DLC-container from grabber list instead from download list</p>");

                commandvec.add("/action/grabber/join/%X% %Y%");
                infovector.add("Join all denoted linkgrabber packages %Y% to the package %X%");

                commandvec.add("/action/grabber/rename/%X% %Y%");
                infovector.add("Rename link grabber package from %X% to %Y%");

                commandvec.add("/action/grabber/confirmall");
                infovector.add("Schedule all packages as download that are located in the link grabber");

                commandvec.add("/action/grabber/confirm/%X%");
                infovector.add("Schedule all denoted grabber packages %X% as download");

                commandvec.add("/action/grabber/removetype/%X% %Y%");
                infovector.add("Remove links from grabber that match the denoted type %X% and/or %Y%. Possible values: 'offline' for offline links and 'available' for links that are already scheduled as download");

                commandvec.add("/action/grabber/removeall");
                infovector.add("Remove all links from linkgrabber");

                commandvec.add("/action/grabber/remove/%X%");
                infovector.add("Remove packages %X% from linkgrabber");

                commandvec.add("/action/downloads/removeall");
                infovector.add("Remove all scheduled downloads");

                commandvec.add("/action/downloads/remove/%X%");
                infovector.add("Remove packages %X% from download list");

                // here goes the whole page construct + css
                response.addContent("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"><head><title>JDRemoteControl Help</title><style type=\"text/css\">" + "a {text-decoration:none; color:#5f5f5f}" + "a:hover {text-decoration:underline; color_#5f5f5f;}" + "body {margin:0% 10% 20% 10%; font-size:12px; color:#5f5f5f; background-color:#ffffff; font-family:Verdana, Arial, Helvetica, sans-serif;}" + "table {width:100%; padding:none; border:1px solid #5f5f5f; border-collapse:collapse; background-color:#ffffff;}" + "td {width:50%; padding:5px; border-top:1px solid #5f5f5f; border-bottom:1px solid #5f5f5f; vertical-align:top;}" + "th {color:#ffffff; background-color:#5f5f5f; font-size:13px; font-weight:normal; text-align:left; padding:5px;}"
                        + "tr:hover {background-color:#E3F3B8;}" + "h1 {font-size:25px; font-weight:normal; color:#5f5f5f;}" + "</style></head><body><h1>JDRemoteControl " + getVersion() + "</h1><p>&nbsp;</p>" + "<p>Replace %X% and %Y% with specific values e.g. /action/save/container/C:\\backup.dlc<br/>Replace (true|false) with true or false<br/>Replace (value) with value => optional parameter<p/>");

                // creating tables and fill them with the commands and
                // information
                response.addContent("<table>");

                for (int commandcount = 0; commandcount < commandvec.size(); commandcount++) {
                    if (commandvec.get(commandcount).equals(" ")) {
                        if (commandcount != 0) response.addContent("</table><br/><br/><table>");
                        response.addContent("<tr><th colspan=\"2\">" + infovector.get(commandcount) + "</th></tr>");
                    } else {
                        response.addContent("<tr><td><a href=\"" + commandvec.get(commandcount) + "\" target=\"_blank\">" + commandvec.get(commandcount) + "</a></td><td>" + infovector.get(commandcount) + "</td></tr>");
                    }
                }

                response.addContent("</table></html>");
            } else if (request.getRequestUrl().equals("/get/ip")) {
                // Get IP
                if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                    response.addContent("IPCheck disabled");
                } else {
                    response.addContent(IPCheck.getIPAddress());
                }
            } else if (request.getRequestUrl().equals("/get/randomip")) {
                // Get random-IP
                Random r = new Random();
                response.addContent(r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255));
            } else if (request.getRequestUrl().equals("/get/config")) {
                // Get config
                Property config = JDUtilities.getConfiguration();
                response.addContent("<pre>");

                if (request.getParameters().containsKey("sub")) {
                    config = SubConfiguration.getConfig(request.getParameters().get("sub").toUpperCase());
                }
                for (Entry<String, Object> next : config.getProperties().entrySet()) {
                    response.addContent(next.getKey() + " = " + next.getValue() + "\r\n");
                }

                response.addContent("</pre>");
            } else if (request.getRequestUrl().equals("/get/version")) {
                // Get version
                response.addContent(JDUtilities.getJDTitle());
            } else if (request.getRequestUrl().equals("/get/rcversion")) {
                // Get JDRemoteControl version
                response.addContent(getVersion());
            } else if (request.getRequestUrl().equals("/get/speedlimit")) {
                // Get speed limit
                response.addContent(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
            } else if (request.getRequestUrl().equals("/get/downloads/current/count")) {
                // Get amount of current DLs COUNT
                int counter = 0;

                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().isPluginActive()) {
                            counter++;
                        }
                    }
                }

                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/downloads/current/list")) {
                // Get current DLs
                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    Element fp_xml = addFilePackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().isPluginActive()) {
                            fp_xml.appendChild(addDownloadLink(xml, dl));
                        }
                    }
                }

                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().equals("/get/downloads/all/count")) {
                // Get DLList COUNT
                int counter = 0;

                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    counter += fp.getDownloadLinkList().size();
                }

                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/downloads/all/list")) {
                // Get DLList
                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    Element fp_xml = addFilePackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        fp_xml.appendChild(addDownloadLink(xml, dl));
                    }
                }

                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().equals("/get/downloads/finished/count")) {
                // Get finished DLs COUNT
                int counter = 0;

                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                            counter++;
                        }
                    }
                }

                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/downloads/finished/list")) {
                // Get finished DLs
                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    Element fp_xml = addFilePackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                            fp_xml.appendChild(addDownloadLink(xml, dl));
                        }
                    }
                }

                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().equals("/get/speed")) {
                // Get current speed
                response.addContent(DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage() / 1000);
            } else if (request.getRequestUrl().equals("/get/isreconnect")) {
                // Get isReconnect
                response.addContent(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true));
            } else if (request.getRequestUrl().equals("/get/downloadstatus")) {
                // Get download status
                response.addContent(DownloadWatchDog.getInstance().getDownloadStatus().toString());
            } else if (request.getRequestUrl().matches("/get/grabber/list")) {
                // Get grabber content as xml
                for (LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                    Element fp_xml = addGrabberPackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinks()) {
                        fp_xml.appendChild(addGrabberLink(xml, dl));
                    }
                }

                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().matches("/get/grabber/count")) {
                int counter = 0;

                for (LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                    counter += fp.getDownloadLinks().size();
                }

                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/grabber/isbusy")) {
                // Get if grabber is busy
                boolean isbusy = false;

                if (LinkGrabberPanel.getLinkGrabber().isRunning())
                    isbusy = true;
                else
                    isbusy = false;

                response.addContent(isbusy);
            } else if (request.getRequestUrl().equals("/action/start")) {
                // Do Start downloads
                DownloadWatchDog.getInstance().startDownloads();
                response.addContent("Downloads started");
            } else if (request.getRequestUrl().equals("/action/pause")) {
                // Do Pause downloads
                DownloadWatchDog.getInstance().pauseDownloads(!DownloadWatchDog.getInstance().isPaused());
                response.addContent("Downloads paused");
            } else if (request.getRequestUrl().equals("/action/stop")) {
                // Do Stop downloads
                DownloadWatchDog.getInstance().stopDownloads();
                response.addContent("Downloads stopped");
            } else if (request.getRequestUrl().equals("/action/toggle")) {
                // Do Toggle downloads
                DownloadWatchDog.getInstance().toggleStartStop();
                response.addContent("Downloads toggled");
            } else if (request.getRequestUrl().matches(".*?/action/(force)?update")) {
                // Do Perform webupdate
                if (request.getRequestUrl().matches(".+/action/forceupdate")) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, true);
                    SubConfiguration.getConfig("WEBUPDATE").setProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false);
                }

                WebUpdate.doUpdateCheck(true);
                response.addContent("Do Webupdate...");
            } else if (request.getRequestUrl().equals("/action/reconnect")) {
                // Do Reconnect
                response.addContent("Do Reconnect...");
                Reconnecter.doManualReconnect();
            } else if (request.getRequestUrl().equals("/action/restart")) {
                // Do Restart JD
                response.addContent("Restarting...");

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            JDLogger.exception(e);
                        }
                        JDUtilities.restartJD(false);
                    }
                }).start();
            } else if (request.getRequestUrl().equals("/action/shutdown")) {
                // Do Shutdown JD
                response.addContent("Shutting down...");

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            JDLogger.exception(e);
                        }
                        JDUtilities.getController().exit();
                    }
                }).start();
            } else if (request.getRequestUrl().matches("(?is).*/action/set/download/limit/[0-9]+")) {
                // Set download limit
                Integer newdllimit = Integer.parseInt(new Regex(request.getRequestUrl(), ".*/action/set/download/limit/([0-9]+)").getMatch(0));
                logger.fine("RemoteControl - Set max. Downloadspeed: " + newdllimit.toString());
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, newdllimit.toString());
                SubConfiguration.getConfig("DOWNLOAD").save();
                response.addContent("newlimit=" + newdllimit);
            } else if (request.getRequestUrl().matches("(?is).*/action/set/download/max/[0-9]+")) {
                // Set max. sim. downloads
                Integer newsimdl = Integer.parseInt(new Regex(request.getRequestUrl(), ".*/action/set/download/max/([0-9]+)").getMatch(0));
                logger.fine("RemoteControl - Set max. sim. Downloads: " + newsimdl.toString());
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, newsimdl.toString());
                SubConfiguration.getConfig("DOWNLOAD").save();
                response.addContent("newmax=" + newsimdl);
            } else if (request.getRequestUrl().matches("(?is).*/action/add(/confirm)?(/start)?/links/.+")) {
                // Add link(s)
                ArrayList<String> links = new ArrayList<String>();

                String link = new Regex(request.getRequestUrl(), ".*/action/add(/confirm)?(/start)?/links/(.+)").getMatch(2);

                for (String tlink : HTMLParser.getHttpLinks(Encoding.urlDecode(link, false), null)) {
                    links.add(tlink);
                }

                if (request.getParameters().size() > 0) {
                    Iterator<String> it = request.getParameters().keySet().iterator();

                    while (it.hasNext()) {
                        String help = it.next();

                        if (!request.getParameter(help).equals("")) {
                            links.add(request.getParameter(help));
                        }
                    }
                }

                StringBuilder ret = new StringBuilder();
                char tmp[] = new char[] { '"', '\r', '\n' };

                for (String element : links) {
                    ret.append('\"');
                    ret.append(element.trim());
                    ret.append(tmp);
                }

                link = ret.toString();
                new DistributeData(link, false).start();

                // confirm parameter - passes just added links to the download
                // queue

                if (request.getRequestUrl().matches(".+/confirm/.+")) {
                    grabberIsBusy = true;

                    while (grabberIsBusy) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            logger.warning(e.toString());
                        }
                    }

                    ArrayList<LinkGrabberFilePackage> lgPackages = new ArrayList<LinkGrabberFilePackage>();

                    synchronized (LinkGrabberController.ControllerLock) {
                        lgPackages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (int i = 0; i < lgPackages.size(); i++) {
                            for (String linkurl : links) {
                                for (DownloadLink dll : lgPackages.get(i).getDownloadLinks()) {
                                    if (linkurl.equals(dll.getBrowserUrl())) {
                                        LinkGrabberPanel.getLinkGrabber().confirmPackage(lgPackages.get(i), null, i);
                                    }
                                }
                            }
                        }
                    }
                }

                if (request.getRequestUrl().matches(".+/start/.+")) {
                    DownloadWatchDog.getInstance().startDownloads();
                }

                response.addContent("Link(s) added. (" + link + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/add(/confirm)?(/start)?/container/.+")) {
                // Open a local or remote DLC-container
                String dlcfilestr = new Regex(request.getRequestUrl(), ".*/action/add(/confirm)?(/start)/container/(.+)").getMatch(2);
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);

                if (dlcfilestr.matches("http://.*?\\.(dlc|ccf|rsdf)")) {
                    // remote container file
                    String containerFormat = new Regex(dlcfilestr, ".+\\.((dlc|ccf|rsdf))").getMatch(0);
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);

                    try {
                        Browser.download(container, dlcfilestr);
                        JDUtilities.getController().loadContainerFile(container, false, false);

                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }

                        container.delete();
                    } catch (Exception e) {
                        JDLogger.exception(e);
                    }
                } else {
                    // local container file
                    JDUtilities.getController().loadContainerFile(new File(dlcfilestr), false, false);
                }

                // TODO: compare links in grabber before and after adding dlc ->
                // then confirm only new links

                // if (request.getRequestUrl().matches(".+/confirm/.+")) {
                // grabberIsBusy = true;
                // while (grabberIsBusy) {
                // try {
                // Thread.sleep(1000);
                // } catch (InterruptedException e) {
                // logger.warning(e.toString());
                // }
                // }
                //
                // ArrayList<LinkGrabberFilePackage> lgPackages = new
                // ArrayList<LinkGrabberFilePackage>();
                //
                // synchronized (LinkGrabberController.ControllerLock) {
                // lgPackages.addAll(LinkGrabberController.getInstance().getPackages());
                //
                // for (int i = 0; i < lgPackages.size(); i++) {
                // for (String linkurl : links) {
                // for (DownloadLink dll : lgPackages.get(i).getDownloadLinks())
                // {
                // if (linkurl.equals(dll.getBrowserUrl())) {
                // LinkGrabberPanel.getLinkGrabber().confirmPackage(lgPackages.get(i),
                // null, i);
                // }
                // }
                // }
                // }
                // }
                // }

                if (request.getRequestUrl().matches(".+/start/.+")) {
                    DownloadWatchDog.getInstance().startDownloads();
                }

                response.addContent("Container opened. (" + dlcfilestr + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/save/container(/fromgrabber)?/.+")) {
                // Save linklist as DLC-container
                ArrayList<DownloadLink> dllinks = new ArrayList<DownloadLink>();

                String dlcfilestr = new Regex(request.getRequestUrl(), ".*/action/save/container(/fromgrabber)/?(.+)").getMatch(1);
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);

                boolean savefromGrabber = new Regex(request.getRequestUrl(), ".+/fromgrabber/.+").matches();

                if (savefromGrabber) {
                    if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                        response.addContent(ERROR_LINK_GRABBER_RUNNING);
                    } else {
                        ArrayList<LinkGrabberFilePackage> lgPackages = new ArrayList<LinkGrabberFilePackage>();
                        ArrayList<FilePackage> packages = new ArrayList<FilePackage>();

                        // disable packages, confirm them, save dlc, then delete
                        synchronized (LinkGrabberController.ControllerLock) {
                            lgPackages.addAll(LinkGrabberController.getInstance().getPackages());

                            for (int i = 0; i < lgPackages.size(); i++) {
                                DownloadLink dl = null;

                                for (DownloadLink link : lgPackages.get(i).getDownloadLinks()) {
                                    link.setEnabled(false);
                                    if (dl == null) dl = link;
                                }

                                LinkGrabberPanel.getLinkGrabber().confirmPackage(lgPackages.get(i), null, i);
                                packages.add(dl.getFilePackage());
                            }

                            JDUtilities.getController().saveDLC(new File(dlcfilestr), dllinks);

                            for (FilePackage fp : packages) {
                                JDUtilities.getDownloadController().removePackage(fp);
                            }
                        }
                    }
                } else {
                    dllinks = JDUtilities.getDownloadController().getAllDownloadLinks();
                    JDUtilities.getController().saveDLC(new File(dlcfilestr), dllinks);
                }

                response.addContent("Container saved. (" + dlcfilestr + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/set/reconnect/(true|false)")) {
                // Set Reconnect enabled
                boolean newrc = Boolean.parseBoolean(new Regex(request.getRequestUrl(), ".*/action/set/reconnect/(true|false)").getMatch(0));
                logger.fine("RemoteControl - Set ReConnect: " + newrc);

                if (newrc != JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, newrc);
                    JDUtilities.getConfiguration().save();
                    response.addContent("reconnect=" + newrc + " (CHANGED=true)");
                } else {
                    response.addContent("reconnect=" + newrc + " (CHANGED=false)");
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/set/premium/(true|false)")) {
                // Set Use premium
                boolean newuseprem = Boolean.parseBoolean(new Regex(request.getRequestUrl(), ".*/action/set/premium/(true|false)").getMatch(0));
                logger.fine("RemoteControl - Set Premium: " + newuseprem);

                if (newuseprem != JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, newuseprem);
                    JDUtilities.getConfiguration().save();
                    response.addContent("newprem=" + newuseprem + " (CHANGED=true)");
                } else {
                    response.addContent("newprem=" + newuseprem + " (CHANGED=false)");
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/join/.+")) {
                // Join link grabber packages
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> srcPackages = new ArrayList<LinkGrabberFilePackage>();
                    String[] packagenames = Encoding.htmlDecode(new Regex(request.getRequestUrl(), ".*/action/grabber/join/(.+)").getMatch(0)).split(" ");

                    synchronized (LinkGrabberController.ControllerLock) {
                        LinkGrabberFilePackage destPackage = LinkGrabberController.getInstance().getFPwithName(packagenames[0]);

                        if (destPackage == null) {
                            response.addContent("ERROR: Package '" + packagenames[0] + "' not found!");
                        } else {
                            // iterate packages; add all to tmp package list
                            // that match the given join names
                            for (LinkGrabberFilePackage pack : LinkGrabberController.getInstance().getPackages()) {
                                for (String src : packagenames) {
                                    if ((pack.getName().equals(src)) && (pack != destPackage)) {
                                        srcPackages.add(pack);
                                    }
                                }
                            }

                            // process src
                            for (LinkGrabberFilePackage pack : srcPackages) {
                                destPackage.addAll(pack.getDownloadLinks());
                                LinkGrabberController.getInstance().removePackage(pack);
                            }

                            // prepare response
                            if (srcPackages.size() > 0) {
                                if (srcPackages.size() < packagenames.length - 1) {
                                    response.addContent("WARNING: Not all packages were found. ");
                                }

                                response.addContent("Joined " + srcPackages.size() + " packages into '" + packagenames[0] + "': ");

                                for (int i = 0; i < srcPackages.size(); ++i) {
                                    if (i != 0) response.addContent(", ");
                                    response.addContent("'" + srcPackages.get(i).getName() + "'");
                                }
                            } else {
                                response.addContent("ERROR: No packages joined into '" + packagenames[0] + "'!");
                            }
                        }
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/rename/.+")) {
                // rename link grabber package
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    String[] packagenames = Encoding.htmlDecode(new Regex(request.getRequestUrl(), "(?is).*/action/grabber/rename/(.+)").getMatch(0)).split(" ");

                    synchronized (LinkGrabberController.ControllerLock) {
                        LinkGrabberFilePackage destPackage = LinkGrabberController.getInstance().getFPwithName(packagenames[0]);

                        if (destPackage == null) {
                            response.addContent("ERROR: Package '" + packagenames[0] + "' not found!");
                        } else {
                            destPackage.setName(packagenames[1]);
                            LinkGrabberController.getInstance().throwRefresh();
                            response.addContent("Package '" + packagenames[0] + "' renamed to '" + packagenames[1] + "'.");
                        }
                    }
                }

            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/confirmall")) {
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();

                    synchronized (LinkGrabberController.ControllerLock) {
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (int i = 0; i < packages.size(); i++) {
                            LinkGrabberFilePackage fp = packages.get(i);
                            LinkGrabberPanel.getLinkGrabber().confirmPackage(fp, null, i);
                        }

                        response.addContent("All links are now scheduled for download.");
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/confirm/.+")) {
                // add denoted links in linkgrabber list to download list
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> addedlist = new ArrayList<LinkGrabberFilePackage>();
                    ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();

                    String[] packagenames = Encoding.htmlDecode(new Regex(request.getRequestUrl(), "(?is).*/action/grabber/confirm/(.+)").getMatch(0)).split(" ");

                    synchronized (LinkGrabberController.ControllerLock) {
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (int i = 0; i < packages.size(); i++) {
                            LinkGrabberFilePackage fp = packages.get(i);

                            for (String name : packagenames) {
                                if (name.equalsIgnoreCase(fp.getName())) {
                                    LinkGrabberPanel.getLinkGrabber().confirmPackage(fp, null, i);
                                    addedlist.add(fp);
                                }
                            }
                        }

                        response.addContent("The following packages are now scheduled for download: ");

                        for (int i = 0; i < addedlist.size(); i++) {
                            if (i != 0) response.addContent(", ");
                            response.addContent("'" + addedlist.get(i).getName() + "'");
                        }
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/removetype/.+")) {
                // remove denoted links from grabber that matches the type
                // 'offline', 'available' (in grabber and download list)
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<String> delLinks = new ArrayList<String>();
                    ArrayList<String> delPackages = new ArrayList<String>();

                    String[] types = Encoding.htmlDecode(new Regex(request.getRequestUrl(), "(?is).*/action/grabber/removetype/(.+)").getMatch(0)).split(" ");

                    synchronized (LinkGrabberController.ControllerLock) {
                        ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        response.addContent("Removing links from grabber of type: ");

                        for (int i = 0; i < types.length; ++i) {
                            if (i != 0) response.addContent(", ");
                            if (types[i].equals(LINK_TYPE_OFFLINE) || types[i].equals(LINK_TYPE_AVAIL)) {
                                response.addContent(types[i]);
                            } else {
                                response.addContent("(unknown type: " + types[i] + ")");
                            }
                        }

                        for (LinkGrabberFilePackage fp : packages) {
                            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(fp.getDownloadLinks());

                            for (DownloadLink link : links) {
                                for (String type : types) {
                                    if ((type.equals(LINK_TYPE_OFFLINE) && link.getAvailableStatus().equals(AvailableStatus.FALSE)) || (type.equals(LINK_TYPE_AVAIL) && link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS))) {
                                        fp.remove(link);
                                        delLinks.add(link.getDownloadURL());
                                    }
                                }
                            }

                            if (fp.getDownloadLinks().size() == 0) {
                                delPackages.add(fp.getName());
                            }
                        }

                        response.addContent(" - " + delLinks.size() + " links removed (" + delLinks + ") thus removed " + delPackages.size() + " empty packages (" + delPackages + ").");
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/removeall")) {
                // remove all links from grabber
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();

                    synchronized (LinkGrabberController.ControllerLock) {
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (LinkGrabberFilePackage fp : packages) {
                            LinkGrabberController.getInstance().removePackage(fp);
                        }

                        response.addContent("All links removed from grabber.");
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/remove/.+")) {
                // remove denoted packages from grabber
                String[] packagenames = Encoding.htmlDecode(new Regex(request.getRequestUrl(), ".*/action/grabber/remove/(.+)").getMatch(0)).split(" ");

                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
                    ArrayList<LinkGrabberFilePackage> removelist = new ArrayList<LinkGrabberFilePackage>();

                    synchronized (LinkGrabberController.ControllerLock) {
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (int i = 0; i < packages.size(); i++) {
                            LinkGrabberFilePackage fp = packages.get(i);

                            for (String name : packagenames) {
                                if (name.equalsIgnoreCase(fp.getName())) {
                                    LinkGrabberController.getInstance().removePackage(fp);
                                    removelist.add(fp);
                                }
                            }
                        }

                        response.addContent("The following packages were removed from grabber: ");

                        for (int i = 0; i < removelist.size(); i++) {
                            if (i != 0) response.addContent(", ");
                            response.addContent("'" + removelist.get(i).getName() + "'");
                        }
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/downloads/removeall")) {
                // remove all packages in download list
                ArrayList<FilePackage> packages = new ArrayList<FilePackage>();
                packages.addAll(DownloadController.getInstance().getPackages());

                for (FilePackage fp : packages) {
                    DownloadController.getInstance().removePackage(fp);
                }

                response.addContent("All scheduled packages removed.");
            } else if (request.getRequestUrl().matches("(?is).*/action/downloads/remove/.+")) {
                // remove denoted packages from download list
                ArrayList<FilePackage> packages = new ArrayList<FilePackage>();
                ArrayList<FilePackage> removelist = new ArrayList<FilePackage>();
                String[] packagenames = Encoding.htmlDecode(new Regex(request.getRequestUrl(), ".*/action/downloads/remove/(.+)").getMatch(0)).split(" ");

                packages.addAll(DownloadController.getInstance().getPackages());

                for (int i = 0; i < packages.size(); i++) {
                    FilePackage fp = packages.get(i);

                    for (String name : packagenames) {
                        if (name.equalsIgnoreCase(fp.getName())) {
                            DownloadController.getInstance().removePackage(fp);
                            removelist.add(fp);
                        }
                    }
                }

                response.addContent("The following packages were removed from download list: ");

                for (int i = 0; i < removelist.size(); i++) {
                    if (i != 0) response.addContent(", ");
                    response.addContent("'" + removelist.get(i).getName() + "'");
                }

            } else {
                response.addContent(ERROR_MALFORMED_REQUEST);
            }
        }

        private Element addFilePackage(Document xml, FilePackage fp) {
            Element element = xml.createElement("package");
            xml.getFirstChild().appendChild(element);
            element.setAttribute("package_name", fp.getName());
            element.setAttribute("package_percent", f.format(fp.getPercent()));
            element.setAttribute("package_linksinprogress", fp.getLinksInProgress() + "");
            element.setAttribute("package_linkstotal", fp.size() + "");
            element.setAttribute("package_ETA", Formatter.formatSeconds(fp.getETA()));
            element.setAttribute("package_speed", Formatter.formatReadable(fp.getTotalDownloadSpeed()));
            element.setAttribute("package_loaded", Formatter.formatReadable(fp.getTotalKBLoaded()));
            element.setAttribute("package_size", Formatter.formatReadable(fp.getTotalEstimatedPackageSize()));
            element.setAttribute("package_todo", Formatter.formatReadable(fp.getTotalEstimatedPackageSize() - fp.getTotalKBLoaded()));
            return element;
        }

        private Element addDownloadLink(Document xml, DownloadLink dl) {
            Element element = xml.createElement("file");
            element.setAttribute("file_name", dl.getName());
            element.setAttribute("file_package", dl.getFilePackage().getName());
            element.setAttribute("file_percent", f.format(dl.getDownloadCurrent() * 100.0 / Math.max(1, dl.getDownloadSize())));
            element.setAttribute("file_hoster", dl.getHost());
            element.setAttribute("file_status", dl.getLinkStatus().getStatusString().toString());
            element.setAttribute("file_speed", dl.getDownloadSpeed() + "");
            return element;
        }

        private Element addGrabberLink(Document xml, DownloadLink dl) {
            Element element = xml.createElement("file");
            element.setAttribute("file_name", dl.getName());
            element.setAttribute("file_package", dl.getFilePackage().getName());
            element.setAttribute("file_hoster", dl.getHost());
            element.setAttribute("file_status", dl.getLinkStatus().getStatusString().toString());
            element.setAttribute("file_download_url", dl.getDownloadURL().toString());
            element.setAttribute("file_browser_url", dl.getBrowserUrl().toString());
            return element;
        }

        private Element addGrabberPackage(Document xml, LinkGrabberFilePackage fp) {
            Element element = xml.createElement("package");
            xml.getFirstChild().appendChild(element);
            element.setAttribute("package_name", fp.getName());
            element.setAttribute("package_linkstotal", fp.size() + "");
            return element;
        }
    }

    private DecimalFormat f = new DecimalFormat("#0.00");
    private HttpServer server;
    private MenuAction activate;

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            subConfig.setProperty(PARAM_ENABLED, activate.isSelected());
            subConfig.save();

            if (activate.isSelected()) {
                server = new HttpServer(subConfig.getIntegerProperty(PARAM_PORT, 10025), new Serverhandler());
                server.start();
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.remotecontrol.startedonport2", "%s started on port %s\nhttp://127.0.0.1:%s\n/help for Developer Information.", getHost(), subConfig.getIntegerProperty(PARAM_PORT, 10025), subConfig.getIntegerProperty(PARAM_PORT, 10025)));
            } else {
                if (server != null) server.sstop();
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.remotecontrol.stopped2", "%s stopped.", getHost()));
            }
        } catch (Exception ex) {
            JDLogger.exception(ex);
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        if (activate == null) {
            activate = new MenuAction("remotecontrol", 0);
            activate.setActionListener(this);
            activate.setSelected(subConfig.getBooleanProperty(PARAM_ENABLED, true));
            activate.setTitle(getHost());
        }

        menu.add(activate);
        return menu;
    }

    @Override
    public boolean initAddon() {
        logger.info("RemoteControl OK");
        initRemoteControl();
        JDUtilities.getController().addControlListener(this);
        return true;
    }

    private void initConfig() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PARAM_PORT, JDL.L("plugins.optional.RemoteControl.port", "Port:"), 1000, 65500));
        cfg.setDefaultValue(10025);
    }

    private void initRemoteControl() {
        if (subConfig.getBooleanProperty(PARAM_ENABLED, true)) {
            try {
                server = new HttpServer(subConfig.getIntegerProperty(PARAM_PORT, 10025), new Serverhandler());
                server.start();
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

    @Override
    public void onExit() {
    }


    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        if (event.getID() == LinkGrabberControllerEvent.FINISHED) {
            grabberIsBusy = false;
        }
    }
}