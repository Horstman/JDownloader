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

import java.util.Calendar;
import java.util.GregorianCalendar;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "airspeedfiles.com" }, urls = { "http://[\\w\\.]*?airspeedfiles\\.com/([a-z]{2}/)?file/[0-9]+/" }, flags = { 2 })
public class AirSpeedFilesCom extends PluginForHost {

    public AirSpeedFilesCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://airspeedfiles.com/register.php");
    }

    // MhfScriptBasic 1.0
    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    public String finalLink = null;
    private static final String COOKIE_HOST = "http://airspeedfiles.com";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        String fileid = new Regex(parameter.getDownloadURL(), "/file/(\\d+)/").getMatch(0);
        br.getPage("http://www.airspeedfiles.com/api/index.php?number=" + fileid);
        String status = br.getRegex("STATUS:'(.*?)'").getMatch(0);
        if (status == null || !status.equals("1")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("FILENAME:'(.*?)'").getMatch(0);
        String filesize = br.getRegex("SIZE:'(.*?)'").getMatch(0);
        if (filename == null || filename.matches("") || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        findLink(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void findLink(DownloadLink link) throws Exception {
        finalLink = br.getRegex("DOWNLOADURL:'(.*?)'").getMatch(0);
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(COOKIE_HOST + "/login.php");
        Form form = br.getFormbyProperty("name", "lOGIN");
        if (form == null) form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("user", Encoding.urlEncode(account.getUser()));
        form.put("pass", Encoding.urlEncode(account.getPass()));
        // If the referer is still in the form (and if it is a valid
        // downloadlink) the download starts directly after logging in so we
        // MUST remove it!
        form.remove("refer_url");
        form.put("autologin", "0");
        br.submitForm(form);
        String premium = br.getRegex("<b>Your Package</b></td>.*?<b>(.*?)</b></A>").getMatch(0);
        if (br.getCookie(COOKIE_HOST, "yab_passhash") == null || (br.getCookie(COOKIE_HOST, "yab_uid") == null || br.getCookie(COOKIE_HOST, "yab_uid").equals("0")) || premium == null || !premium.equals("Premium")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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

        br.getPage(COOKIE_HOST + "/members.php");
        String premium = br.getRegex("<b>Your Package</b></td>.*?<b>(.*?)</b></A>").getMatch(0);
        if (!premium.equals("Premium")) {
            account.setValid(true);
            return ai;
        }
        String expired = br.getRegex("Expired\\?</b></td>.*?<td align=.*?>(.*?)<").getMatch(0);
        if (expired != null) {
            if (expired.trim().equalsIgnoreCase("No"))
                ai.setExpired(false);
            else if (expired.equalsIgnoreCase("Yes")) ai.setExpired(true);
        }

        String expires = br.getRegex("Package Expire Date</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (expires != null) {
            String[] e = expires.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + e[2]), Integer.parseInt(e[0]) - 1, Integer.parseInt(e[1]));
            ai.setValidUntil(cal.getTimeInMillis());
        }

        String create = br.getRegex("Register Date</b></td>.*?<td align=.*?>(.*?)<").getMatch(0);
        if (create != null) {
            String[] c = create.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + c[2]), Integer.parseInt(c[0]) - 1, Integer.parseInt(c[1]));
            ai.setCreateTime(cal.getTimeInMillis());
        }

        ai.setFilesNum(0);
        String files = br.getRegex("<b>Hosted Files</b></td>.*?<td align=.*?>(.*?)<").getMatch(0);
        if (files != null) {
            ai.setFilesNum(Integer.parseInt(files.trim()));
        }

        ai.setPremiumPoints(0);
        String points = br.getRegex("<b>Total Points</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (points != null) {
            ai.setPremiumPoints(Integer.parseInt(points.trim()));
        }

        ai.setStatus("Premium User");
        account.setValid(true);

        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.setFollowRedirects(false);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        String fileid = new Regex(parameter.getDownloadURL(), "/file/(\\d+)/").getMatch(0);
        br.getPage("http://www.airspeedfiles.com/api/index.php?number=" + fileid);
        // br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null && (br.getRedirectLocation().contains("access_key=") || br.getRedirectLocation().contains("getfile.php"))) {
            finalLink = br.getRedirectLocation();
        } else {
            if (br.containsHTML("You have got max allowed download sessions from the same IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            // br.postPage(parameter.getDownloadURL(),
            // "sent=1&B6=Premium+User");
            findLink(parameter);
        }
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, finalLink, true, 1);
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
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
