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

package jd.plugins.hoster;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "depositfiles.com" }, urls = { "http://[\\w\\.]*?depositfiles\\.com(/\\w{1,3})?/files/[\\w]+" }, flags = { 2 })
public class DepositFiles extends PluginForHost {

    static private final String FILE_NOT_FOUND = "Dieser File existiert nicht";

    private static final String PATTERN_PREMIUM_FINALURL = "<div id=\"download_url\">.*?<a href=\"(.*?)\"";

    private Pattern FILE_INFO_NAME = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);

    private Pattern FILE_INFO_SIZE = Pattern.compile("Dateigr.*?: <b>(.*?)</b>");

    private static final Object PREMLOCK = new Object();

    private static int simultanpremium = 1;

    public DepositFiles(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://depositfiles.com/signup.php?ref=down1");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.forceDebug(true);
        requestFileInformation(downloadLink);
        String link = downloadLink.getDownloadURL();
        br.getPage(link);
        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
            br.getPage(link);
        }
        checkErrors();
        String dllink = br.getRegex("download_url\".*?<form action=\"(.*?)\"").getMatch(0);
        if (dllink != null) {
            // handling for txt file downloadlinks, dunno why they made a
            // completely different page for txt files
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            URLConnectionAdapter con = dl.getConnection();
            if (Plugin.getFileNameFromHeader(con) == null || Plugin.getFileNameFromHeader(con).indexOf("?") >= 0) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (!con.isContentDisposition()) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }
            dl.startDownload();
        } else {
            Form form = br.getFormBySubmitvalue("Kostenlosen+download");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.submitForm(form);
            checkErrors();
            if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) { throw new PluginException(LinkStatus.ERROR_RETRY); }
            dllink = br.getRegex("<div id=\"download_url\" style=\"display:none;\">.*?<form action=\"(.*?)\" method=\"get").getMatch(0);
            String icid = br.getRegex("get_download_img_code\\.php\\?icid=(.*?)\"").getMatch(0);
            /* check for captcha */
            if (dllink == null && icid != null) {
                Form cap = new Form();
                cap.setAction(link);
                cap.setMethod(Form.MethodType.POST);
                cap.put("icid", icid);
                // form.put("submit", "Continue");
                String captcha = getCaptchaCode("http://depositfiles.com/de/get_download_img_code.php?icid=" + icid, downloadLink);
                cap.put("img_code", captcha);
                br.submitForm(cap);
                dllink = br.getRegex("<div id=\"download_url\" style=\"display:none;\">.*?<form action=\"(.*?)\" method=\"get").getMatch(0);
                if (dllink == null) {
                    /* TODO: get correct output here */
                    if (br.containsHTML("get_download_img_code.php")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    dllink = br.getRegex("<div id=\"download_url\" style=\"display:none;\">.*?<form action=\"(.*?)\" method=\"get").getMatch(0);
                }

            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            URLConnectionAdapter con = dl.getConnection();
            if (Plugin.getFileNameFromHeader(con) == null || Plugin.getFileNameFromHeader(con).indexOf("?") >= 0) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (!con.isContentDisposition()) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    public void checkErrors() throws NumberFormatException, PluginException {
        /* download not available at the moment */
        if (br.containsHTML("Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        /* limit reached */
        if (br.containsHTML("You used up your limit") || br.containsHTML("Please try in")) {
            String wait = br.getRegex("html_download_api-limit_interval\">(\\d+)</span>").getMatch(0);
            if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait.trim()) * 1000l);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
        }
        /* county slots full */
        if (br.containsHTML("but all downloading slots for your country")) {
            // String wait =
            // br.getRegex("html_download_api-limit_country\">(\\d+)</span>").getMatch(0);
            // if (wait != null) throw new
            // PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE,
            // Integer.parseInt(wait.trim()) * 1000l);
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.depositfilescom.errors.allslotsbusy", "All download slots for your country are busy"), 10 * 60 * 1000l);
        }
        /* already loading */
        if (br.containsHTML("Von Ihren IP-Addresse werden schon einige")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1001l);
        if (br.containsHTML("You cannot download more than one file in parallel")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1001l);
        /* unknown error, try again */
        String wait = br.getRegex("Bitte versuchen Sie noch mal nach(.*?)<\\/strong>").getMatch(0);
        if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Regex.getMilliSeconds(wait));
        /* You have exceeded the 15 GB 24-hour limit */
        if (br.containsHTML("GOLD users can download no more than")) {
            logger.info("GOLD users can download no more than 15 GB for the last 24 hours");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
    }

    public void login(Account account) throws Exception {
        br.setDebug(true);
        br.setFollowRedirects(true);
        setLangtoGer();
        br.getPage("http://depositfiles.com/de/gold/payment.php");
        Form login = br.getFormBySubmitvalue("Anmelden");
        login.put("login", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
        br.setFollowRedirects(false);
        String cookie = br.getCookie("http://depositfiles.com", "autologin");
        if (cookie == null || br.containsHTML("Benutzername-Passwort-Kombination")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public void setLangtoGer() throws IOException {
        br.setCookie("http://depositfiles.com", "lang_current", "de");
    }

    public boolean isFreeAccount() throws IOException {
        setLangtoGer();
        br.getPage("http://depositfiles.com/de/gold/");
        if (br.containsHTML("Ihre aktuelle Status: Frei - Mitglied</div>")) return true;
        if (br.containsHTML(">Goldmitgliedschaft<")) return false;
        if (br.containsHTML("noch den Gold-Zugriff")) return false;
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
        br.setDebug(true);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
            account.setValid(false);
            return ai;
        }
        if (isFreeAccount()) {
            ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountok", "Account is OK.(Free User)"));
            account.setValid(true);
            return ai;
        }
        String expire = br.getRegex("noch den Gold-Zugriff: <b>(.*?)</b></div>").getMatch(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
        if (expire == null) {
            ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountbad", "Account expitred or not valid."));
            account.setValid(false);
            return ai;
        }
        ai.setStatus(JDL.L("plugins.hoster.depositfilescom.accountok", "Account is OK."));
        Date date;
        try {
            date = dateFormat.parse(expire);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }

        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        boolean free = false;
        synchronized (PREMLOCK) {
            requestFileInformation(downloadLink);
            login(account);
            if (this.isFreeAccount()) {
                simultanpremium = 1;
                free = true;
            } else {
                if (simultanpremium + 1 > 20) {
                    simultanpremium = 20;
                } else {
                    simultanpremium++;
                }
            }
        }
        if (free) {
            handleFree(downloadLink);
            return;
        }
        String link = downloadLink.getDownloadURL();
        br.getPage(link);

        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
            br.getPage(link);
        }

        checkErrors();
        link = br.getRegex(PATTERN_PREMIUM_FINALURL).getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 0);
        URLConnectionAdapter con = dl.getConnection();
        if (Plugin.getFileNameFromHeader(con) == null || Plugin.getFileNameFromHeader(con).indexOf("?") >= 0) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (!con.isContentDisposition()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://depositfiles.com/en/agreem.html";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.com(/.*?)?/files", ".com/de/files"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        String link = downloadLink.getDownloadURL();
        setLangtoGer();
        /* needed so the download gets counted,any referer should work */
        br.getHeaders().put("Referer", "http://www.google.de");
        br.setFollowRedirects(false);
        br.getPage(link);

        // Datei geloescht?
        if (br.containsHTML(FILE_NOT_FOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("<strong>Achtung! Sie haben ein Limit")) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.depositfilescom.errors.limitreached", "Download limit reached"));
            return AvailableStatus.TRUE;
        }
        String fileName = br.getRegex(FILE_INFO_NAME).getMatch(0);
        String fileSizeString = br.getRegex(FILE_INFO_SIZE).getMatch(0);
        if (fileName == null || fileSizeString == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(fileName);
        downloadLink.setDownloadSize(Regex.getSize(fileSizeString));
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        synchronized (PREMLOCK) {
            return simultanpremium;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
