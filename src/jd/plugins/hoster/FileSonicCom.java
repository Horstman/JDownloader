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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesonic.com" }, urls = { "http://[\\w\\.]*?(sharingmatrix|filesonic)\\.com/.*?file/(.*?/)?[0-9]+(/.+)?" }, flags = { 2 })
public class FileSonicCom extends PluginForHost {

    public FileSonicCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filesonic.com/en/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.filesonic.com/en/contact-us";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        /* convert sharingmatrix to filesonic that set english language */
        String id = new Regex(link.getDownloadURL(), "file/(.*?/)?(\\d+.+)").getMatch(1);
        link.setUrlDownload("http://www.filesonic.com/en/file/" + id);
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://www.filesonic.com/en");
        br.postPage("http://www.filesonic.com/en/user/login", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        String premCookie = br.getCookie("http://www.filesonic.com", "role");
        if (premCookie == null || !premCookie.equalsIgnoreCase("premium")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
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
        br.getPage("http://www.filesonic.com/en/dashboard");
        String expiredate = br.getRegex("Premium membership valid until.*?<span>(.*?),").getMatch(0);
        String daysleft = br.getRegex("Premium membership valid until.*?<span>.*?,.*?(\\d+)").getMatch(0);
        if (expiredate != null) {
            ai.setStatus("Premium User");
            ai.setValidUntil(Regex.getMilliSeconds(expiredate, "yyyy-MM-dd", null));
            account.setValid(true);
            return ai;
        }
        if (daysleft != null) {
            /* days left seems buggy atm */
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(daysleft) * 24 * 60 * 60 * 1000));
            ai.setStatus("Premium User");
            account.setValid(true);
            return ai;
        }
        account.setValid(false);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(downloadLink);
        login(account);
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        String url = br.getRedirectLocation();
        if (url == null) {
            /* no redirect, what the frak */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        /* first download try, without password */
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("This file is password protected")) {
                /* password handling */
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(JDL.L("plugins.hoster.sharingmatrixcom.password", "Password?"), downloadLink);
                } else {
                    /* get saved password */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                Form form = br.getForm(0);
                form.put("password", Encoding.urlEncode(passCode));
                /* second downloadtry with password */
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 0);
            } else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            errorHandling(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://www.filesonic.com/en");
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename: </span> <strong>(.*?)<").getMatch(0);
        String filesize = br.getRegex("<span class=\"size\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (filename.contains("...")) {
            String otherName = new Regex(parameter.getDownloadURL(), "file/\\d+/(.+)").getMatch(0);
            if (otherName != null && otherName.length() > filename.length()) {
                filename = otherName;
            }
        }
        filesize = filesize.replace("&nbsp;", "");
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    private void errorHandling(DownloadLink downloadLink) throws PluginException {
        if (br.containsHTML("Download session in progress")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download session in progress", 10 * 60 * 1000l);
        if (br.containsHTML("This file is password protected")) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.forceDebug(true);
        requestFileInformation(downloadLink);
        String passCode = null;
        String id = new Regex(downloadLink.getDownloadURL(), "file/(\\d+)").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage("http://www.filesonic.com/en/download-free/" + id);
        errorHandling(downloadLink);
        if (br.containsHTML("This file is password protected")) {
            /* password handling */
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(JDL.L("plugins.hoster.sharingmatrixcom.password", "Password?"), downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            Form form = br.getForm(0);
            form.put("password", Encoding.urlEncode(passCode));
            br.submitForm(form);

        }
        String countDownDelay = br.getRegex("countDownDelay = (\\d+)").getMatch(0);
        if (countDownDelay != null) {
            /*
             * we have to wait a little longer than needed cause its not exactly
             * the same time
             */
            if (Long.parseLong(countDownDelay) > 300) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(countDownDelay + 120) * 1000l);
            this.sleep(Long.parseLong(countDownDelay) * 1000, downloadLink);
        }
        String downloadUrl = br.getRegex("downloadUrl = \"(http://.*?)\"").getMatch(0);
        if (downloadUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /*
         * limited to 1 chunk at the moment cause don't know if its a server
         * error that more are possible and resume should also not work ;)
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, false, 1);
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            errorHandling(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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