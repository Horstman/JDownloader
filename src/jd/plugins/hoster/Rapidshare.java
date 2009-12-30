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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.Browser.BrowserException;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.RAFDownload;
import jd.utils.SnifferException;
import jd.utils.Sniffy;
import jd.utils.locale.JDL;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidshare.com" }, urls = { "http://[\\w\\.]*?rapidshare\\.com/files/\\d+/.+" }, flags = { 2 })
public class Rapidshare extends PluginForHost {

    private static final Pattern PATTERM_MATCHER_ALREADY_LOADING = Pattern.compile("(Warten Sie bitte, bis der Download abgeschlossen ist)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_FIND_DOWNLOAD_POST_URL = Pattern.compile("<form name=\"dl[f]?\" action=\"(.*?)\" method=\"post\"");

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE = Pattern.compile("<h1>Fehler</h1>.*?<div class=\"klappbox\">.*?herunterladen:.*?<p>(.*?)</p", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_1 = Pattern.compile("<h1>Fehler</h1>.*?<div class=\"klappbox\">.*?<p.*?>(.*?)</p", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_2 = Pattern.compile("<!-- E#[\\d]{1,2} -->(.*?)<", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_3 = Pattern.compile("<!-- E#[\\d]{1,2} --><p>(.*?)<\\/p>", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_MIRROR_URL = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");

    private static final Pattern PATTERN_FIND_MIRROR_URLS = Pattern.compile("<input.*?type=\"radio\" name=\"mirror\" onclick=\"document\\.dlf?\\.action=[\\\\]?'(.*?)[\\\\]?';\" /> (.*?)<br />", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_FIND_PRESELECTED_SERVER = Pattern.compile("<form name=\"dlf?\" action=\"(.*?)\" method=\"post\">");

    private static final Pattern PATTERN_FIND_TICKET_WAITTIME = Pattern.compile("var c=([\\d]*?);");

    private static final String PROPERTY_INCREASE_TICKET = "INCREASE_TICKET";

    private static final String PROPERTY_SELECTED_SERVER = "SELECTED_SERVER";

    private static final String PROPERTY_SELECTED_SERVER2 = "SELECTED_SERVER#2";

    private static final String PROPERTY_SELECTED_SERVER3 = "SELECTED_SERVER#3";

    private static final String PROPERTY_USE_PRESELECTED = "USE_PRESELECTED";

    private static final String WAIT_HOSTERFULL = "WAIT_HOSTERFULL";

    private static final String PROPERTY_USE_TELEKOMSERVER = "USE_TELEKOMSERVER";

    private static String[] serverList1 = new String[] { "cg", "cg2", "dt", "gc", "gc2", "l3", "l32", "l33", "l34", "tg", "tl", "tl2" };

    private static String[] serverList2 = new String[] { "cg", "dt", "gc", "gc2", "l3", "l32", "tg", "tg2", "tl", "tl2", "tl3" };

    private static String[] serverList3 = new String[] { "cg", "dt", "gc", "gc2", "l3", "l32", "l33", "l34", "tg", "tg2", "tl", "tl2" };

    final static private Object LOCK = new Object();

    final static private Boolean HTMLWORKAROUND = new Boolean(false);

    private static long rsapiwait = 0;

    private static HashMap<String, String> serverMap = new HashMap<String, String>();
    static {
        serverMap.put("Cogent", "cg");
        serverMap.put("Cogent #2", "cg2");
        serverMap.put("Deutsche Telekom", "dt");
        serverMap.put("GlobalCrossing", "gc");
        serverMap.put("GlobalCrossing #2", "gc2");
        serverMap.put("Level(3)", "l3");
        serverMap.put("Level(3) #2", "l32");
        serverMap.put("Level(3) #3", "l33");
        serverMap.put("Level(3) #4", "l34");
        serverMap.put("Tata Com.", "tg");
        serverMap.put("Tata Com. #2", "tg2");
        serverMap.put("Teleglobe", "tg");
        serverMap.put("Teleglobe #2", "tg2");
        serverMap.put("TeliaSonera", "tl");
        serverMap.put("TeliaSonera #2", "tl2");
        serverMap.put("TeliaSonera #3", "tl3");
    }

    private static final Account dummyAccount = new Account("TRAFSHARE", "TRAFSHARE");

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(getCorrectedURL(link.getDownloadURL()));
    }

    /**
     * Korrigiert die URL und befreit von subdomains etc.
     * 
     * @param link
     * @return
     * @throws IOException
     */
    private String getCorrectedURL(String link) {
        if (link == null) return null;
        if (link.contains("://ssl.") || !link.startsWith("http://rapidshare.com")) {
            link = "http://rapidshare.com" + link.substring(link.indexOf("rapidshare.com") + 14);
        }
        String fileid = new Regex(link, "http://[\\w\\.]*?rapidshare\\.com/files/([\\d]{3,9})/?.*").getMatch(0);
        String filename = new Regex(link, "http://[\\w\\.]*?rapidshare\\.com/files/[\\d]{3,9}/?(.*)").getMatch(0);
        return "http://rapidshare.com/files/" + fileid + "/" + filename;
    }

    private String selectedServer;

    public Rapidshare(PluginWrapper wrapper) {

        super(wrapper);

        setConfigElements();
        enablePremium("http://rapidshare.com/premium.html");
        this.setMaxConnections(30);

    }

    @Override
    public int getTimegapBetweenConnections() {
        return 100;
    }

    /**
     * Bietet der hoster eine Möglichkeit mehrere links gleichzeitig zu prüfen,
     * kann das über diese Funktion gemacht werden.
     */
    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            if (rsapiwait > System.currentTimeMillis()) {
                for (DownloadLink u : urls) {
                    u.setAvailable(true);
                    u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.apiflood", "unchecked (API Flood)"));
                }
                return true;
            }
            logger.finest("OnlineCheck: " + urls.length + " links");
            StringBuilder idlist = new StringBuilder();
            StringBuilder namelist = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int size = 0;
            for (DownloadLink u : urls) {
                if (size > 3000) {
                    logger.finest("OnlineCheck: SplitCheck " + links.size() + "/" + urls.length + " links");
                    /* do not stop here because we are not finished yet */
                    checkLinksIntern2(links);
                    links.clear();
                    idlist.delete(0, idlist.capacity());
                    namelist.delete(0, namelist.capacity());
                }
                /* workaround reset */
                u.setProperty("htmlworkaround", null);
                idlist.append("," + getID(u));
                namelist.append("," + getName(u));
                links.add(u);
                size = ("http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=checkfiles_v1&files=" + idlist.toString().substring(1) + "&filenames=" + namelist.toString().substring(1) + "&incmd5=1").length();
            }
            if (links.size() != 0) {
                if (links.size() != urls.length) {
                    logger.finest("OnlineCheck: SplitCheck " + links.size() + "/" + urls.length + " links");
                } else {
                    logger.finest("OnlineCheck: Check " + urls.length + " links");
                }
                if (!checkLinksIntern2(links)) return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean checkLinksIntern2(ArrayList<DownloadLink> links) {
        if (!checkLinksIntern(links.toArray(new DownloadLink[] {}))) return false;
        ArrayList<DownloadLink> retry = new ArrayList<DownloadLink>();
        for (DownloadLink link : links) {
            if (link.getProperty("htmlworkaround", null) != null) {
                retry.add(link);
            }
        }
        if (retry.size() > 0) if (!checkLinksIntern(retry.toArray(new DownloadLink[] {}))) return false;
        return true;
    }

    public boolean checkLinksIntern(DownloadLink[] urls) {
        if (urls == null) { return false; }
        ArrayList<DownloadLink> checkurls = new ArrayList<DownloadLink>();
        ArrayList<DownloadLink> finishedurls = new ArrayList<DownloadLink>();
        for (DownloadLink u : urls) {
            checkurls.add(u);
        }
        try {
            for (int retry = 0; retry < 3; retry++) {
                StringBuilder idlist = new StringBuilder();
                StringBuilder namelist = new StringBuilder();
                checkurls.removeAll(finishedurls);
                for (DownloadLink u : checkurls) {
                    idlist.append("," + getID(u));
                    namelist.append("," + getName(u));
                }
                String req = "http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=checkfiles_v1&files=" + idlist.toString().substring(1) + "&filenames=" + namelist.toString().substring(1) + "&incmd5=1";

                queryAPI(null, req);

                if (br.containsHTML("access flood")) {
                    logger.warning("RS API flooded! will not check again the next 5 minutes!");
                    rsapiwait = System.currentTimeMillis() + 5 * 60 * 1000l;
                    return false;
                }

                String[][] matches = br.getRegex("([^\n^\r^,]+),([^,]+),([^,]+),([^,]+),([^,]+),([^,]+),([^\n^\r]+)").getMatches();
                int i = 0;
                boolean doretry = false;
                for (DownloadLink u : checkurls) {
                    finishedurls.add(u);
                    if (i > matches.length - 1) {
                        doretry = true;
                        break;
                    }
                    u.setDownloadSize(Long.parseLong(matches[i][2]));
                    u.setFinalFileName(matches[i][1]);
                    u.setMD5Hash(matches[i][6]);
                    // 0=File not found 1=File OK 2=File OK (direct download)
                    // 3=Server down 4=File abused 5
                    switch (Integer.parseInt(matches[i][4])) {
                    case 0:
                        tryWorkaround(u);
                        u.getLinkStatus().setErrorMessage(JDL.L("plugin.host.rapidshare.status.filenotfound", "File not found"));
                        u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.filenotfound", "File not found"));
                        u.setAvailable(false);
                        break;
                    case 1:
                        // u.getLinkStatus().setStatusText("alles prima");
                        u.setAvailable(true);
                        u.getLinkStatus().setStatusText("");
                        u.getLinkStatus().setErrorMessage(null);
                        break;
                    case 2:
                        u.setAvailable(true);
                        u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.directdownload", "Direct Download"));
                        u.getLinkStatus().setErrorMessage(null);
                        break;
                    case 3:
                        u.getLinkStatus().setErrorMessage(JDL.L("plugin.host.rapidshare.status.servernotavailable", "Server temp. not available. Try later!"));
                        u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.servernotavailable", "Server temp. not available. Try later!"));
                        u.setAvailable(false);
                        break;
                    case 4:
                        u.setAvailable(false);
                        u.getLinkStatus().setErrorMessage(JDL.L("plugin.host.rapidshare.status.abused", "File abused"));
                        u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.abused", "File abused"));
                        break;
                    case 5:
                        u.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                        u.getLinkStatus().setErrorMessage(JDL.L("plugin.host.rapidshare.status.anonymous", "File without Account(annonymous)"));
                        u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.anonymous", "File without Account(annonymous)"));
                        break;
                    }
                    i++;
                }
                if (!doretry) return true;
            }
            return false;
        } catch (Exception e) {
            if (br.containsHTML("access flood")) {
                logger.warning("RS API flooded! will not check again the next 5 minutes!");
                rsapiwait = System.currentTimeMillis() + 5 * 60 * 1000l;
            }
            return false;
        }
    }

    /**
     * requests the API url req. if the http ip is blocked (UK-BT isp returns
     * 500 or 502 error) https is used.
     * 
     * @param br
     * @param req
     * @throws IOException
     */
    private void queryAPI(Browser br, String req) throws IOException {
        if (br == null) br = this.br;
        try {
            br.getPage(req);
        } catch (BrowserException e) {
            if (e.getConnection() != null && !req.startsWith("https")) {
                switch (e.getConnection().getResponseCode()) {
                case 500:
                case 502:
                    req = "https" + req.substring(4);
                    br.getPage(req);
                    break;
                default:
                    throw e;
                }
            } else {
                throw e;
            }
        }

    }

    private String getName(DownloadLink link) {
        String name;
        if (link.getProperty("htmlworkaround", null) == null) {
            /* remove html ending, because rs now checks the complete filename */
            name = new Regex(link.getDownloadURL(), "files/\\d+/(.*?)(\\.html?|$)").getMatch(0);
        } else {
            name = new Regex(link.getDownloadURL(), "files/\\d+/(.*?)$").getMatch(0);
        }
        return name;
    }

    private void tryWorkaround(DownloadLink link) {
        if (link.getProperty("htmlworkaround", null) == null) {
            link.setProperty("htmlworkaround", HTMLWORKAROUND);
        }
    }

    private String getID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "files/(\\d+)/").getMatch(0);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        try {
            if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                if (Sniffy.hasSniffer()) throw new SnifferException();
            }
            LinkStatus linkStatus = downloadLink.getLinkStatus();
            // if (ddl)this.doPremium(downloadLink);

            // if (getRemainingWaittime() > 0) { return
            // handleDownloadLimit(downloadLink); }
            String freeOrPremiumSelectPostURL = null;
            br.setAcceptLanguage(ACCEPT_LANGUAGE);
            br.setFollowRedirects(false);

            String link = downloadLink.getDownloadURL();

            // RS URL wird aufgerufen
            // req = new GetRequest(link);
            // req.load();
            br.getPage(link);
            if (br.getRedirectLocation() != null) {
                logger.info("Direct Download for Free Users");
                this.handlePremium(downloadLink, dummyAccount);
                return;
            }
            // posturl für auswahl free7premium wird gesucht
            freeOrPremiumSelectPostURL = new Regex(br, PATTERN_FIND_MIRROR_URL).getMatch(0);
            // Fehlerbehandlung auf der ersten Seite
            if (freeOrPremiumSelectPostURL == null) {
                handleErrorsFree();
            }

            // Post um freedownload auszuwählen
            Form[] forms = br.getForms();
            if (forms.length == 0) {
                /*
                 * sometimes the server does not answer with a valid / complete
                 * html file, then just wait few mins and try again
                 */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
            }
            br.submitForm(forms[0]);
            // PostRequest pReq = new PostRequest(freeOrPremiumSelectPostURL);
            // pReq.setPostVariable("dl.start", "free");
            // pReq.load();
            handleErrorsFree();
            // Ticketwartezeit wird gesucht
            String ticketTime = new Regex(br, PATTERN_FIND_TICKET_WAITTIME).getMatch(0);
            if (ticketTime != null && ticketTime.equals("0")) {
                ticketTime = null;
            }

            String ticketCode = br + "";

            String tt = new Regex(ticketCode, "var tt =(.*?)document\\.getElementById\\(\"dl\"\\)\\.innerHTML").getMatch(0);

            String fun = "function f(){ return " + tt + "} f()";
            Context cx = Context.enter();
            Scriptable scope = cx.initStandardObjects();

            // Collect the arguments into a single string.

            // Now evaluate the string we've colected.
            Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);

            // Convert the result to a string and print it.
            String code = Context.toString(result);
            if (tt != null) ticketCode = code;
            Context.exit();
            if (ticketCode.contains("Leider sind derzeit keine freien Slots ")) {
                downloadLink.getLinkStatus().setStatusText("All free slots in use: try to download again after 2 minutes");
                logger.warning("All free slots in use: try to download again after 2 minutes");
                if (getPluginConfig().getBooleanProperty(WAIT_HOSTERFULL, true)) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "All free slots in use", 120 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "All free slots in use", 120 * 1000l);
                }
            }
            if (new Regex(ticketCode, ".*download.{0,3}limit.{1,50}free.{0,3}users.*").matches()) {
                String waitfor = new Regex(ticketCode, "Or try again in about(.*?)minutes").getMatch(0);
                long waitTime = 15 * 60 * 1000l;
                try {
                    waitTime = new Long(waitfor.trim()) * 60 * 1000l;
                } catch (Exception e) {
                    logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);

            }
            long pendingTime = 0;
            if (ticketTime != null) {
                pendingTime = Long.parseLong(ticketTime);

                if (getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) > 0) {
                    logger.warning("Waittime increased by JD: " + pendingTime + " --> " + (pendingTime + getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime / 100));
                    pendingTime = pendingTime + getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime / 100;
                }
                pendingTime *= 1000;
            }

            sleep(pendingTime, downloadLink);

            String postTarget = getDownloadTarget(downloadLink, ticketCode);

            // Falls Serverauswahl fehlerhaft war
            if (linkStatus.isFailed()) return;

            Request request = br.createPostRequest(postTarget, "mirror=on&x=" + Math.random() * 40 + "&y=" + Math.random() * 40);

            /** TODO: Umbauen auf jd.plugins.BrowserAdapter.openDownload(br,...) **/
            // Download
            dl = new RAFDownload(this, downloadLink, request);
            // long startTime = System.currentTimeMillis();
            URLConnectionAdapter con = dl.connect();
            if (con.getHeaderField("Location") != null) {
                con.disconnect();
                request = br.createGetRequest(con.getHeaderField("Location"));
                dl = new RAFDownload(this, downloadLink, request);
                // long startTime = System.currentTimeMillis();
                con = dl.connect();
            }
            if (!con.isContentDisposition() && con.getHeaderField("Cache-Control") != null) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }

            dl.startDownload();
        } finally {
            if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                selectedServer = null;
            }
        }

    }

    @Override
    public String getSessionInfo() {
        if (selectedServer != null) return " @ " + selectedServer;
        return "";
    }

    private void handleErrorsPremium(Account account) throws PluginException {
        String error = null;
        if ((error = findError(br.toString())) != null) {
            logger.warning(error);
            if (Regex.matches(error, Pattern.compile("Der Downloadlink wurde manipuliert und ist damit "))) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (Regex.matches(error, Pattern.compile("(Diese Datei steht im Verdacht illegal)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("(Verletzung unserer Nutzungsbedingungen)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("(weder einem Premiumaccount)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("(Der Uploader hat diese Datei)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("(in 2 Minuten)"))) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many users are currently downloading this file", 120 * 1000l); }
            if (Regex.matches(error, Pattern.compile("(Die Datei konnte nicht gefunden werden)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("(Betrugserkennung)"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugin.rapidshare.error.fraud", "Fraud detected: This Account has been illegally used by several users."), PluginException.VALUE_ID_PREMIUM_DISABLE); }
            if (Regex.matches(error, Pattern.compile("(expired|abgelaufen)"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), PluginException.VALUE_ID_PREMIUM_DISABLE); }
            if (Regex.matches(error, Pattern.compile("(You have exceeded the download limit|Sie haben heute das Limit)"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugin.rapidshare.error.limitexeeded", "You have exceeded the download limit."), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE); }
            if (Regex.matches(error, Pattern.compile("IP"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE); }
            if (Regex.matches(error, Pattern.compile("Der Server .*? ist momentan nicht verf.*"))) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.LF("plugin.rapidshare.error.serverunavailable", "The Server %s is currently unavailable.", error.substring(11, error.indexOf(" ist"))), 3600 * 1000l); }
            if (Regex.matches(error, Pattern.compile("(Ihr Cookie wurde nicht erkannt)"))) {
                if (account != null) account.setProperty("cookies", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (Regex.matches(error, Pattern.compile("Passwort ist falsch"))) {
                if (account != null) account.setProperty("cookies", null);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (Regex.matches(error, Pattern.compile("(Account wurde nicht gefunden|Your Premium Account has not been found)"))) {
                if (account != null) account.setProperty("cookies", null);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugin.rapidshare.error.accountnotfound", "Your Premium Account has not been found."), PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                if (account != null) account.setProperty("cookies", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error));
            }
        }
    }

    private void handleErrorsFree() throws PluginException {
        String error = null;
        if ((error = findError(br.toString())) != null) {
            logger.warning(error);
            if (Regex.matches(error, Pattern.compile("Der Downloadlink wurde manipuliert und ist damit "))) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (Regex.matches(error, Pattern.compile("(Diese Datei steht im Verdacht illegal)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("(Der Uploader hat diese Datei)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("(als 200 Megabyte)"))) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugin.rapidshare.error.filetolarge", "This file is larger than 200 MB, you need a premium-account to download this file."));
            if (Regex.matches(error, Pattern.compile("(weder einem Premiumaccount)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("(keine freien Slots)")) || Regex.matches(error, Pattern.compile("(Sie sind nicht angemeldet)")) || Regex.matches(error, Pattern.compile("(Diese Datei k.*?Sie nur als)")) || Regex.matches(error, Pattern.compile("(Es sind derzeit keine freien Download)"))) {
                if (getPluginConfig().getBooleanProperty(WAIT_HOSTERFULL, true)) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "All free slots in use", 2 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "All free slots in use", 2 * 60 * 1000l);
                }
            }
            if (Regex.matches(error, Pattern.compile("(in 2 Minuten)"))) {
                if (getPluginConfig().getBooleanProperty(WAIT_HOSTERFULL, true)) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many users are currently downloading this file", 2 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many users are currently downloading this file", 2 * 60 * 1000l);
                }
            }
            if (Regex.matches(error, Pattern.compile("(Die Datei konnte nicht gefunden werden)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("Der Server .*? ist momentan nicht verf.*"))) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.LF("plugin.rapidshare.error.serverunavailable", "The Server %s is currently unavailable.", error.substring(11, error.indexOf(" ist"))), 3600 * 1000l); }
            if (Regex.matches(error, PATTERM_MATCHER_ALREADY_LOADING)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Already a download from your ip in progress!", 2 * 60 * 1000l); }
            // für java 1.5
            if (new Regex(error, "(kostenlose Nutzung erreicht)|(.*download.{0,3}limit.{1,50}free.{0,3}users.*)").matches()) {
                if (false) {
                    /* do not remove this! */
                    String waitfor = new Regex(br, "es in ca\\.(.*?)Minuten wieder").getMatch(0);
                    if (waitfor == null) {
                        waitfor = new Regex(br, "Or try again in about(.*?)minutes").getMatch(0);

                    }
                    long waitTime = 15 * 60 * 1000l;
                    try {
                        waitTime = new Long(waitfor.trim()) * 60 * 1000l;
                    } catch (Exception e) {
                        logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                    }
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
                } else {
                    /*
                     * changed to 5 mins, because next download could be
                     * possible earlier
                     */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                }
            }
            reportUnknownError(br, 2);
            throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error));
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        try {
            if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                if (Sniffy.hasSniffer()) throw new SnifferException();
            }

            String freeOrPremiumSelectPostURL = null;
            Request request = null;

            // long startTime = System.currentTimeMillis();
            if (account == dummyAccount) {
                br = new Browser();
            } else {
                br = login(account, true);
            }

            br.setFollowRedirects(false);
            br.setAcceptLanguage(ACCEPT_LANGUAGE);
            br.getPage(downloadLink.getDownloadURL());

            String directurl = br.getRedirectLocation();

            if (directurl == null) {
                logger.finest("InDirect-Download: Server-Selection available!");
                if (account.getStringProperty("cookies", null) == null) {
                    logger.info("LOGIN ERROR");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                handleErrorsPremium(account);

                // posturl für auswahl wird gesucht
                freeOrPremiumSelectPostURL = new Regex(br, PATTERN_FIND_MIRROR_URL).getMatch(0);
                // Fehlerbehandlung auf der ersten Seite
                if (freeOrPremiumSelectPostURL == null) {
                    handleErrorsPremium(account);
                    reportUnknownError(br, 1);
                    logger.warning("could not get newURL");
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                // Post um Premium auszuwählen
                Form[] forms = br.getForms();
                br.submitForm(forms[1]);
                handleErrorsPremium(account);
                String postTarget = getDownloadTarget(downloadLink, br.toString());
                if (postTarget == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                request = br.createGetRequest(postTarget);
            } else {
                logger.finest("Direct-Download: Server-Selection not available!");
                request = br.createGetRequest(directurl);
            }
            /** TODO: Umbauen auf jd.plugins.BrowserAdapter.openDownload(br,...) **/
            // Download
            dl = new RAFDownload(this, downloadLink, request);
            // Premiumdownloads sind resumefähig
            dl.setResume(true);
            // Premiumdownloads erlauben chunkload
            dl.setChunkNum(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
            URLConnectionAdapter urlConnection;
            try {
                urlConnection = dl.connect(br);
            } catch (Exception e) {
                br.setRequest(request);
                request = br.createGetRequest(null);
                logger.info("Load from " + request.getUrl().toString().substring(0, 35));
                // Download
                dl = new RAFDownload(this, downloadLink, request);
                // Premiumdownloads sind resumefähig
                dl.setResume(true);
                // Premiumdownloads erlauben chunkload
                dl.setChunkNum(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

                urlConnection = dl.connect(br);
            }
            // Download starten
            // prüft ob ein content disposition header geschickt wurde. Falls
            // nicht,
            // ist es eintweder eine Bilddatei oder eine Fehlerseite. BIldfiles
            // haben keinen Cache-Control Header
            if (!urlConnection.isContentDisposition() && urlConnection.getHeaderField("Cache-Control") != null) {
                // Lädt die zuletzt aufgebaute vernindung
                br.setRequest(request);
                br.followConnection();

                // Fehlerbehanldung
                /*
                 * Achtung! keine Parsing arbeiten an diesem String!!!
                 */
                handleErrorsPremium(account);
                reportUnknownError(br.toString(), 6);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }

            dl.startDownload();
        } finally {
            if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                selectedServer = null;
            }
            downloadLink.getLinkStatus().setStatusText(JDL.LF("plugins.host.rapidshare.loadedvia", "Loaded via %s", account.getUser()));
        }

    }

    private String findError(String string) {
        String error = null;
        error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE).getMatch(0);

        if (error == null || error.trim().length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_3).getMatch(0);
        }
        if (error == null || error.trim().length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_2).getMatch(0);
        }
        if (error == null || error.trim().length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_1).getMatch(0);
        }

        error = Encoding.htmlDecode(error);
        String[] er = Regex.getLines(error);

        if (er == null || er.length == 0) { return null; }
        er[0] = HTMLEntities.unhtmlentities(er[0]);
        if (er[0] == null || er[0].length() == 0) { return null; }
        return er[0];

    }

    private String dynTranslate(String error) {
        // added known hashes here so that LFE srcparser can find them
        JDL.L("plugins.host.rapidshare.errors.0b37866ecbc00ce1857fa40852df6fef", "Download Session expired. Try again.");
        JDL.L("plugins.host.rapidshare.errors.15fb70be386bb33e91b8e31b3b94c021", "File not found. Please check your download link.");
        JDL.L("plugins.host.rapidshare.errors.33f66fa600e57edd85714127295c7bcc", "File not found. Please check your download link.");
        JDL.L("plugins.host.rapidshare.errors.6b973d801c521a179cc63a4b830314d3", "TOS violation. File removed by rapidshare.");
        JDL.L("plugins.host.rapidshare.errors.811d7f115500de90d1495ef963040930", "The server that stores the file is offline. Try again later.");
        JDL.L("plugins.host.rapidshare.errors.8da4051e59062d67a39a7e10cc026831", "You have reached your daily limit.");
        JDL.L("plugins.host.rapidshare.errors.bcfe246b0634299062224a73ae50f17e", "This file seems to be illegal and is locked. Downloading this file is prohibited by Rapidshare.");
        JDL.L("plugins.host.rapidshare.errors.d11f499020a3607ffdf987ce3968c692", "10 GB limit reached.");

        String error2 = JDL.L("plugins.host.rapidshare.errors." + JDHash.getMD5(error) + "", error);
        if (error.equals(error2)) {
            logger.warning("NO TRANSLATIONKEY FOUND FOR: " + error + "(" + JDHash.getMD5(error) + ")");
        }
        return error2;
    }

    @Override
    public String getAGBLink() {
        return "http://rapidshare.com/faq.html";
    }

    /**
     * Sucht im ticketcode nach der entgültigen DownloadURL Diese Downlaodurl
     * beinhaltet in ihrer Subdomain den zielserver. Durch Anpassung dieses
     * Zielservers kann also die Serverauswahl vorgenommen werden.
     * 
     * @param step
     * @param downloadLink
     * @param ticketCode
     * @return
     * @throws PluginException
     */
    private String getDownloadTarget(DownloadLink downloadLink, String ticketCode) throws PluginException {

        String postTarget = new Regex(ticketCode, PATTERN_FIND_DOWNLOAD_POST_URL).getMatch(0);

        String server1 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER, "Level(3)");
        String server2 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER2, "TeliaSonera");
        String server3 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER3, "TeliaSonera");

        String serverAbb = serverMap.get(server1);
        String server2Abb = serverMap.get(server2);
        String server3Abb = serverMap.get(server3);
        if (serverAbb == null) {
            serverAbb = serverList1[(int) (Math.random() * (serverList1.length - 1))];
            logger.finer("Use Random #1 server " + serverAbb);
        }
        if (server2Abb == null) {
            server2Abb = serverList2[(int) (Math.random() * (serverList2.length - 1))];
            logger.finer("Use Random #2 server " + server2Abb);
        }
        if (server3Abb == null) {
            server3Abb = serverList3[(int) (Math.random() * (serverList3.length - 1))];
            logger.finer("Use Random #3 server " + server3Abb);
        }

        boolean telekom = getPluginConfig().getBooleanProperty(PROPERTY_USE_TELEKOMSERVER, false);
        boolean preselected = getPluginConfig().getBooleanProperty(PROPERTY_USE_PRESELECTED, true);

        if (postTarget == null) {
            logger.severe("postTarget not found:");
            reportUnknownError(ticketCode, 4);
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_RETRY);
            return null;
        }
        String[] serverstrings = new Regex(ticketCode, PATTERN_FIND_MIRROR_URLS).getColumn(0);
        logger.info("wished Mirror #1 Server " + serverAbb);
        logger.info("wished Mirror #2 Server " + server2Abb);
        logger.info("wished Mirror #3 Server " + server3Abb);
        String selected = new Regex(ticketCode, PATTERN_FIND_PRESELECTED_SERVER).getMatch(0);
        logger.info("Preselected Server: " + selected.substring(0, 30));
        String selectedID = new Regex(selected, "\\d*\\d+(\\D+?\\d*?)\\.").getMatch(0);

        if (preselected) {
            logger.info("RS.com Use preselected : " + selected.substring(0, 30));
            postTarget = selected;

            selectedServer = getServerName(selectedID);
        } else if (telekom && ticketCode.indexOf("td.rapidshare.com") >= 0) {
            logger.info("RS.com Use Telekom Server");
            this.selectedServer = "Telekom";
            postTarget = getURL(serverstrings, "Deutsche Telekom", postTarget);
        } else if (ticketCode.indexOf(serverAbb + ".rapidshare.com") >= 0) {
            logger.info("RS.com Use Mirror #1 Server: " + getServerName(serverAbb));
            this.selectedServer = getServerName(serverAbb);
            postTarget = getURL(serverstrings, getServerName(serverAbb), postTarget);
        } else if (ticketCode.indexOf(server2Abb + ".rapidshare.com") >= 0) {
            logger.info("RS.com Use Mirror #2 Server: " + getServerName(server2Abb));
            this.selectedServer = getServerName(server2Abb);
            postTarget = getURL(serverstrings, getServerName(server2Abb), postTarget);
        } else if (ticketCode.indexOf(server3Abb + ".rapidshare.com") >= 0) {
            logger.info("RS.com Use Mirror #3 Server: " + getServerName(server3Abb));
            this.selectedServer = getServerName(server3Abb);
            postTarget = getURL(serverstrings, getServerName(server3Abb), postTarget);
        } else if (serverstrings.length > 0) {
            logger.severe("Kein Server gefunden 1");
        } else {
            logger.severe("Kein Server gefunden 2");

        }

        return postTarget;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        }
        return downloadLink.getAvailableStatus();
    }

    private String getServerName(String id) {
        for (Entry<String, String> next : serverMap.entrySet()) {
            if (next.getValue().equalsIgnoreCase(id)) return next.getKey();
        }
        return null;
    }

    private String getURL(String[] serverstrings, String selected, String postTarget) {
        if (!serverMap.containsKey(selected.trim())) {
            logger.severe("Unknown Servername: " + selected);
            return postTarget;
        }
        String abb = serverMap.get(selected.trim());

        for (String url : serverstrings) {
            if (url.contains(abb + ".rapidshare.com")) {
                logger.info("Load from " + selected + "(" + abb + ")");
                return url;
            }
        }

        logger.warning("No Serverstring found for " + abb + "(" + selected + ")");
        return postTarget;
    }

    private void reportUnknownError(Object req, int id) {
        logger.severe("Unknown error(" + id + "). please add this htmlcode to your bugreport:\r\n" + req);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
        rsapiwait = 0;
    }

    /**
     * Erzeugt den Configcontainer für die Gui
     */
    private void setConfigElements() {
        ArrayList<String> m1 = new ArrayList<String>();
        ArrayList<String> m2 = new ArrayList<String>();
        ArrayList<String> m3 = new ArrayList<String>();
        for (String element : serverList1) {
            m1.add(getServerName(element));
        }
        for (String element : serverList2) {
            m2.add(getServerName(element));
        }
        for (String element : serverList3) {
            m3.add(getServerName(element));
        }
        m1.add(JDL.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));
        m2.add(JDL.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));
        m3.add(JDL.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDL.L("plugins.hoster.rapidshare.com.prefferedServer", "Bevorzugte Server")));
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_USE_PRESELECTED, JDL.L("plugins.hoster.rapidshare.com.preSelection", "Vorauswahl übernehmen")).setDefaultValue(true);
        config.addEntry(cond);

        ConfigEntry ce;
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER, m1.toArray(new String[] {}), "#1").setDefaultValue("Level(3)"));
        ce.setEnabledCondidtion(cond, false);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER2, m2.toArray(new String[] {}), "#2").setDefaultValue("TeliaSonera"));
        ce.setEnabledCondidtion(cond, false);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER3, m3.toArray(new String[] {}), "#3").setDefaultValue("TeliaSonera"));
        ce.setEnabledCondidtion(cond, false);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_USE_TELEKOMSERVER, JDL.L("plugins.hoster.rapidshare.com.telekom", "Telekom Server verwenden falls verfügbar")).setDefaultValue(false));
        ce.setEnabledCondidtion(cond, false);

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), PROPERTY_INCREASE_TICKET, JDL.L("plugins.hoster.rapidshare.com.increaseTicketTime", "Ticketwartezeit verlängern (0%-500%)"), 0, 500).setDefaultValue(0).setStep(1));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), WAIT_HOSTERFULL, JDL.L("plugins.hoster.rapidshare.com.waithosterfull", "Wait if all FreeUser Slots are full")).setDefaultValue(true));
    }

    @SuppressWarnings("unchecked")
    public Browser login(Account account, boolean usesavedcookie) throws IOException, PluginException {
        synchronized (LOCK) {
            Browser br = new Browser();
            br.setDebug(true);
            br.setCookiesExclusive(true);
            br.clearCookies(this.getHost());

            HashMap<String, String> cookies = (HashMap<String, String>) account.getProperty("cookies");

            if (usesavedcookie && cookies != null) {
                if (cookies.get("enc") != null && cookies.get("enc").length() != 0) {
                    logger.finer("Cookie Login");
                    for (Entry<String, String> cookie : cookies.entrySet()) {
                        br.setCookie("http://rapidshare.com", cookie.getKey(), cookie.getValue());
                    }
                    return br;
                } else {
                    account.setProperty("cookies", null);
                }
            }
            String req = "http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=getaccountdetails_v1&withcookie=1&type=prem&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
            queryAPI(br, req);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return br;
            }
            if (br.containsHTML("Login failed")) {
                account.setProperty("cookies", null);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (br.containsHTML("access flood")) {
                logger.warning("RS API flooded! will not check again the next 5 minutes!");
                logger.finer("HTTPS Login");
                br.setAcceptLanguage("en, en-gb;q=0.8");
                br.getPage("https://ssl.rapidshare.com/cgi-bin/premiumzone.cgi?login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            } else {
                logger.finer("API Login");
                String cookie = br.getRegex("cookie=([A-Z0-9]+)").getMatch(0);
                br.setCookie("http://rapidshare.com", "enc", cookie);
            }
            String cookie = br.getCookie("http://rapidshare.com", "enc");
            if (cookie == null) {
                account.setProperty("cookies", null);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("enc", cookie);
            account.setProperty("cookies", map);
            return br;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        ai.setSpecialTraffic(true); /*
                                     * rs has specialtraffic: trafficshare,
                                     * pointconverting
                                     */
        String api = "http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=getaccountdetails_v1&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&type=prem";
        queryAPI(br, api);
        String error = br.getRegex("ERROR:(.*)").getMatch(0);
        if (error != null) {
            account.setProperty("cookies", null);
            ai.setStatus(JDL.LF("plugins.host.rapidshare.apierror", "Rapidshare reports that %s", error.trim()));
            account.setValid(false);
            return ai;
        }
        try {
            String[][] matches = br.getRegex("(\\w+)=([^\r^\n]+)").getMatches();
            HashMap<String, String> data = getMap(matches);
            ai.setTrafficLeft((long) (Long.parseLong(data.get("premkbleft")) / 1000.0) * 1024l * 1024l);
            ai.setTrafficMax(25 * 1000 * 1000 * 1000l);
            ai.setFilesNum(Long.parseLong(data.get("curfiles")));
            ai.setPremiumPoints(Long.parseLong(data.get("fpoints")));
            ai.setAccountBalance((Long.parseLong(data.get("ppoints")) * Long.parseLong(data.get("ppointrate"))) / 1000);
            ai.setUsedSpace(Long.parseLong(data.get("curspace")));
            ai.setTrafficShareLeft((long) (Long.parseLong(data.get("bodkb")) / 1000.0) * 1024l * 1024l);
            ai.setValidUntil(Long.parseLong(data.get("validuntil")) * 1000);
            if (ai.getValidUntil() < System.currentTimeMillis()) {
                ai.setExpired(true);
            }
        } catch (Exception e) {

            logger.severe("RS-API change detected, please inform support!");
        }
        return ai;
    }

    private HashMap<String, String> getMap(String[][] matches) {
        HashMap<String, String> map = new HashMap<String, String>();
        for (String[] m : matches)
            map.put(m[0].trim(), m[1].trim());
        return map;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}