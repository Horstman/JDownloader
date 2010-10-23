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
import java.util.Calendar;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "2shared.com" }, urls = { "http://[\\w\\.]*?2shared\\.com/(audio|file|video)/.*?/[a-zA-Z0-9._]+" }, flags = { 0 })
public class TwoSharedCom extends PluginForHost {

    public TwoSharedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.2shared.com/terms.jsp";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/audio/", "/file/"));
        link.setUrlDownload(link.getDownloadURL().replace("/video/", "/file/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        br.setCookiesExclusive(true);
        br.getPage(downloadLink.getDownloadURL());
        Form pwform = br.getForm(0);
        if (pwform != null) {
            String filename = br.getRegex("<td class=\"header\" align=\"center\">Download (.*?)</td>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(filename.trim());
            return AvailableStatus.TRUE;
        }
        String filesize = br.getRegex(Pattern.compile("<span class=.*?>File size:</span>(.*?)&nbsp; &nbsp;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String filename = br.getRegex("<title>2shared - download(.*?)</title>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim().replaceAll(",|\\.", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form pwform = br.getForm(0);
        if (pwform != null) {
            String passCode;
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                passCode = downloadLink.getStringProperty("pass", null);
            }
            pwform.put("userPass2", passCode);
            br.submitForm(pwform);
            if (br.containsHTML("passError\\(\\);")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA, JDL.L("plugins.hoster.2sharedcom.errors.wrongcaptcha", "Wrong captcha"));
            } else {
                downloadLink.setProperty("pass", passCode);
            }
        }
        String link = br.getRegex(Pattern.compile("\\$\\.get\\('(.*?)'", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        int viw = Calendar.DAY_OF_WEEK;
        String l2surl = new Regex(link, "id=(.*?$)").getMatch(0);
        link = new Regex(link,"^(.*?id=)").getMatch(0);
        if (l2surl.charAt(0) % 2 == 1) {
            link = link + l2surl.substring(0, viw) + l2surl.substring(16 + viw);
        } else {
            link = link + l2surl.substring(0, 16 - viw) + l2surl.substring(l2surl.length() - viw);
        }
        link = br.getPage("http://www.2shared.com" + link).trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getURL().getQuery().contains("MAX_IP")) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.2sharedcom.errors.sessionlimit", "Session limit reached"), 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
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
