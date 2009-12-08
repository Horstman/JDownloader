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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashares.com" }, urls = { "http://[\\w\\.]*?(d[0-9]{2}\\.)?megashares\\.com/(.*\\?d[0-9]{2}=[0-9a-f]{7}|.*?dl/[0-9a-f]+/)" }, flags = { 2 })
public class MegasharesCom extends PluginForHost {

    public MegasharesCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.megashares.com/lc_order.php?tid=sasky");
    }

    private void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://d01.megashares.com/");
        br.postPage("http://d01.megashares.com/", "lc_email=" + Encoding.urlEncode(account.getUser()) + "&lc_pin=" + Encoding.urlEncode(account.getPass()) + "&lc_signin=Sign-In");
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
        String expires = br.getRegex("</font> Expires: <font.*?>(.*?)</font>").getMatch(0);
        if (expires == null) {
            account.setValid(false);
            return ai;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.UK);
        try {
            Date date = dateFormat.parse(expires);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
        }
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
        if (br.containsHTML("You have reached the maximum download limit")) {
            long wait = Long.parseLong(dat[1]) * 60000l + Long.parseLong(dat[2]) * 1000l;
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(wait);
            return;
        }

        // Captchacheck
        if (br.containsHTML("Your Passport needs to be reactivated.")) {
            String captchaAddress = br.getRegex("<dt>Enter the passport reactivation code in the graphic, then hit the \"Reactivate Passport\" button.</dt>.*?<dd><img src=\"(.*?)\" alt=\"Security Code\" style=.*?>").getMatch(0);

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
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Dateigröße holen
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
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

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        loadpage(downloadLink.getDownloadURL());
        if (br.containsHTML("class=\"button_free\">")) {
            loadpage(downloadLink.getDownloadURL());
        }
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
        String dsize = br.getRegex("Filesize:</span></strong>(.*?)<br />").getMatch(0);
        String fln = br.getRegex("download page link title.*?<h1 class=.*?>(.*?)<").getMatch(0);
        if (dsize == null || fln == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(fln.trim());
        downloadLink.setDownloadSize(Regex.getSize(dsize));
        return AvailableStatus.TRUE;
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
