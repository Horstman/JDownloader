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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
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

/*TODO: Support für andere Linkcards(bestimmte Anzahl Downloads,unlimited usw) einbauen*/

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashares.com" }, urls = { "http://[\\w\\.]*?(d[0-9]{2}\\.)?megashares\\.com/(.*\\?d[0-9]{2}=[0-9a-zA-Z]{7}|dl/[0-9a-zA-Z]{7}/)" }, flags = { 2 })
public class MegasharesCom extends PluginForHost {

    public MegasharesCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.megashares.com/lc_order.php?tid=sasky");
    }

    private void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://d01.megashares.com/");
        br.postPage("http://d01.megashares.com/myms_login.php", "mymslogin_name=" + Encoding.urlEncode(account.getUser()) + "&mymspassword=" + Encoding.urlEncode(account.getPass()) + "&myms_login=Login");
        if (br.getCookie("http://megashares.com", "linkcard") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        /* TODO: there can be many different kind of linkcards */
        return ai;
    }

    public void loadpage(String url) throws IOException {
        boolean tmp = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        br.getPage(url);
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        br.setFollowRedirects(tmp);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws IOException {
        Browser brt = new Browser();
        brt.setFollowRedirects(false);
        brt.getPage(link.getDownloadURL());
        if (brt.getRedirectLocation() != null) {
            link.setUrlDownload(brt.getRedirectLocation());
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        // Password protection
        loadpage(downloadLink.getDownloadURL());
        if (!checkPassword(downloadLink)) { return; }
        if (br.containsHTML("All download slots for this link are currently filled")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        String url = br.getRegex("<div>\\s*?<a href=\"(http://.*?megashares.*?)\">").getMatch(0);
        if (url == null) url = br.getRegex("<div id=\"show_download_button(_\\d+)?\".*?>\\n*?\\s*?<a href=\"(http://.*?megashares.*?)\">").getMatch(1);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, -6);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        // Password protection
        if (!checkPassword(downloadLink)) { return; }

        // Sie laden gerade eine datei herunter
        if (br.containsHTML("You already have the maximum")) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(60 * 1000l);
            return;
        }
        if (br.containsHTML("All download slots for this link are currently filled")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        // Reconnet/wartezeit check
        String[] dat = br.getRegex("Your download passport will renew.*?in.*?(\\d+).*?:.*?(\\d+).*?:.*?(\\d+)</strong>").getRow(0);
        if (br.containsHTML("You have reached.*?maximum download limit")) {
            long wait = Long.parseLong(dat[1]) * 60000l + Long.parseLong(dat[2]) * 1000l;
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(wait);
            return;
        }

        // Captchacheck
        if (br.containsHTML("Your Passport needs to be reactivated.")) {
            String captchaAddress = br.getRegex("then hit the \"Reactivate Passport\" button\\.</dt>.*?<dd><img src=\"(.*?)\"").getMatch(0);
            if (captchaAddress == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            HashMap<String, String> input = HTMLParser.getInputHiddenFields(br + "");

            String code = getCaptchaCode(captchaAddress, downloadLink);
            String geturl = downloadLink.getDownloadURL() + "&rs=check_passport_renewal&rsargs[]=" + code + "&rsargs[]=" + input.get("random_num") + "&rsargs[]=" + input.get("passport_num") + "&rsargs[]=replace_sec_pprenewal&rsrnd=" + System.currentTimeMillis();
            br.getPage(geturl);

            if (br.containsHTML("You already have the maximum")) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setValue(30 * 1000l);
                return;
            }
            requestFileInformation(downloadLink);
            if (!checkPassword(downloadLink)) { return; }
        }
        // Downloadlink
        String url = br.getRegex("<div>\\s*?<a href=\"(http://.*?megashares.*?)\">").getMatch(0);
        if (url == null) url = br.getRegex("<div id=\"show_download_button(_\\d+)?\".*?>\\n*?\\s*?<a href=\"(http://.*?megashares.*?)\">").getMatch(1);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Dateigröße holen
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.getHttpConnection().toString().contains("Your Passport needs to")) throw new PluginException(LinkStatus.ERROR_RETRY);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dl.startDownload()) {
            downloadLink.getLinkStatus().setRetryCount(0);
        }
    }

    private boolean checkPassword(DownloadLink link) throws Exception {

        if (br.containsHTML("This link requires a password")) {
            Form form = br.getFormBySubmitvalue("Validate+Password");
            String pass = link.getStringProperty("pass");
            if (pass != null) {
                form.put("passText", pass);
                br.submitForm(form);
                if (!br.containsHTML("This link requires a password")) { return true; }
            }
            int i = 0;
            while ((i++) < 5) {
                pass = Plugin.getUserInput(JDL.LF("plugins.hoster.passquestion", "Link '%s' is passwordprotected. Enter password:", link.getName()), link);
                if (pass != null) {
                    form.put("passText", pass);
                    br.submitForm(form);
                    if (!br.containsHTML("This link requires a password")) {
                        link.setProperty("pass", pass);
                        return true;
                    }
                }
            }
            link.setProperty("pass", null);
            link.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);
            link.getLinkStatus().setErrorMessage("Link password wrong");
            return false;
        }
        return true;
    }

    @Override
    public String getAGBLink() {
        return "http://d01.megashares.com/tos.php";
    }

    public void renew(Browser br, int buttonID) throws IOException {
        Browser brc = br.cloneBrowser();
        String renew[] = br.getRegex("\"renew\\('(.*?)','(.*?)'").getRow(0);
        String post = br.getRegex("renew\\.php'.*?\\{(.*?):").getMatch(0);
        if (post != null) post = post.trim();
        if (renew == null || renew.length != 2) return;
        if (buttonID <= 0) {
            brc.postPage("/renew.php", post + "=" + renew[0]);
        } else {
            brc.postPage("/renew.php", post + "=" + renew[0] + "&button_id=" + buttonID);
        }
        br.getPage(renew[1]);
        brc = null;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setDebug(true);
        loadpage(downloadLink.getDownloadURL());
        /* new filename, size regex */
        String fln = br.getRegex("FILE Download.*?>.*?>(.*?)<").getMatch(0);
        String dsize = br.getRegex("FILE Download.*?>.*?>.*?>(\\d+.*?)<").getMatch(0);
        try {
            renew(br, 0);
            if (br.containsHTML("class=\"order_push_box_left(_2)?\">")) {
                renew(br, 1);
            }
            if (br.containsHTML("Invalid link")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("You already have the maximum")) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.megasharescom.errors.alreadyloading", "Cannot check, because aready loading file"));
                return AvailableStatus.UNCHECKABLE;
            }
            if (br.containsHTML("All download slots for this link are currently filled")) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.megasharescom.errors.allslotsfilled", "Cannot check, because all slots filled"));
                return AvailableStatus.UNCHECKABLE;
            }
            if (br.containsHTML("This link requires a password")) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.megasharescom.errors.passwordprotected", "Password protected download"));
                return AvailableStatus.UNCHECKABLE;
            }
            if (br.containsHTML("This link is currently offline")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This link is currently offline for scheduled maintenance, please try again later", 60 * 60 * 1000l);
            /* fallback regex */
            if (dsize == null) dsize = br.getRegex("Filesize:</span></strong>(.*?)<br />").getMatch(0);
            if (fln == null) fln = br.getRegex("download page link title.*?<h1 class=.*?>(.*?)<").getMatch(0);
            if (dsize == null || fln == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            if (dsize != null) downloadLink.setDownloadSize(Regex.getSize(dsize));
            if (fln != null) downloadLink.setName(fln.trim());
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
