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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uloz.to" }, urls = { "http://[\\w\\.]*?((uloz\\.to|ulozto\\.sk|ulozto\\.cz|ulozto\\.net)/[0-9]+/|bagruj\\.cz/[a-z0-9]{12}/.*?\\.html)" }, flags = { 2 })
public class UlozTo extends PluginForHost {

    public UlozTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://uloz.to/kredit/");
    }

    @Override
    public String getAGBLink() {
        return "http://ulozto.net/podminky/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(uloz\\.to|ulozto\\.sk|ulozto\\.cz|ulozto\\.net)", "uloz.to"));
    }

    public boolean rewriteHost(DownloadLink link) {
        if (link.getHost().contains("ulozto.sk") || link.getHost().contains("ulozto.cz") || link.getHost().contains("ulozto.net")) {
            correctDownloadLink(link);
            link.setHost("uloz.to");
            return true;
        }
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (downloadLink.getDownloadURL().matches(".*?bagruj\\.cz/[a-z0-9]{12}.*?") && br.getRedirectLocation() != null) {
            downloadLink.setUrlDownload(br.getRedirectLocation());
            br.getPage(downloadLink.getDownloadURL());
        } else if (br.getRedirectLocation() != null) {
            logger.info("Getting redirect-page");
            br.getPage(br.getRedirectLocation());
        }
        String continuePage = br.getRegex("<p><a href=\"(http://.*?)\">Please click here to continue</a>").getMatch(0);
        if (continuePage != null) {
            downloadLink.setUrlDownload(continuePage);
            br.getPage(downloadLink.getDownloadURL());
        }
        // Wrong links show the mainpage so here we check if we got the mainpage
        // or not
        if (br.containsHTML("(multipart/form-data|Chybka 404 - požadovaná stránka nebyla nalezena<br>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(Pattern.compile("\\&t=(.*?)\"")).getMatch(0);
        if (filename == null) filename = br.getRegex(Pattern.compile("cptm=;Pe/\\d+/(.*?)\\?b")).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("style=\"top:-55px;\"><div>\\d+:\\d+ \\| (.*?)</div></div>")).getMatch(0);
        if (filesize == null) filesize = br.getRegex("class=\"info_velikost\" style=\"top:-55px;\"><div>(.*?)</div></div>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = null;
        boolean failed = true;
        for (int i = 0; i <= 5; i++) {
            String captchaUrl = br.getRegex(Pattern.compile("style=\"width:175px; height:70px;\" width=\"175\" height=\"70\" src=\"(http://.*?)\"")).getMatch(0);
            if (captchaUrl == null) captchaUrl = br.getRegex(Pattern.compile("\"(http://img\\.uloz\\.to/captcha/\\d+\\.png)\"")).getMatch(0);
            Form captchaForm = br.getFormbyProperty("name", "dwn");
            if (captchaForm == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaForm.put("captcha_user", code);
            br.submitForm(captchaForm);
            dllink = br.getRedirectLocation();
            if (dllink != null && dllink.contains("no#cpt")) {
                br.getPage(downloadLink.getDownloadURL());
                continue;
            }
            failed = false;
            break;
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage("http://uloz.to/");
        br.postPage("http://uloz.to/login/", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&pamatovat=1&prihlasit=P%C5%99ihl%C3%A1sit");
        if (br.getCookie("http://uloz.to/", "uloz-to-prihlasen") == null || br.getCookie("http://uloz.to/", "login_hash") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String trafficleft = br.getRegex("Váš kredit : <b><span title=\"Pozor, zmena jednotek:([0-9 ]+KB)").getMatch(0);
        if (trafficleft != null) {
            ai.setTrafficLeft(Regex.getSize(trafficleft.replace(" ", "")));
        }
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.getPage(parameter.getDownloadURL());
        String dllink = br.getRegex("<a name=\"VIP\"></a>.*?<table width=\"90%\" cellspacing=\"5\" cellpadding=\"5\" border=\"0\" style=\"border:1px solid black;margin-left:-10px\">.*?<td><a href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
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
