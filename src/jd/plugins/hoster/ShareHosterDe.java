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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharehoster.de" }, urls = { "http://[\\w\\.]*?sharehoster\\.(de|com|net)/(dl|wait|vid)/[a-z0-9]+" }, flags = { 0 })
public class ShareHosterDe extends PluginForHost {

    public ShareHosterDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.sharehoster.de/index.php?content=agb";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("sharehoster\\.(com|net|de)", "sharehoster.com").replace("/dl/", "/wait/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL().replace("/vid/", "/wait/"));
        // No filename or size is on the page so just check if there is an
        // error, if not, the file should be online!
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("download_failed") || br.getRedirectLocation().contains("downloadfailed") || br.getRedirectLocation().contains("premium&vid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.getPage(br.getRedirectLocation());
        }
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("download_failed") || br.getRedirectLocation().contains("downloadfailed") || br.getRedirectLocation().contains("premium&vid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String fileID = new Regex(downloadLink.getDownloadURL(), "/(wait|vid)/(.+)").getMatch(1);
        br.setFollowRedirects(true);
        String waitCode = br.getRegex("name=\"wait\" value=\"(.*?)\"").getMatch(0);
        if (waitCode == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String postData = "continue=Fortfahren&file=" + fileID + "&wait=" + waitCode;
        if (downloadLink.getDownloadURL().contains("/vid/")) postData += "&open=show_wait";
        String postPage = downloadLink.getDownloadURL().replace("/wait/", "/dl/");
        if (downloadLink.getDownloadURL().contains("/vid/")) postPage = downloadLink.getDownloadURL();
        br.setFollowRedirects(false);
        br.postPage(postPage, postData);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.getRedirectLocation().contains("/vid/"))
            br.getPage(br.getRedirectLocation());
        else
            br.getPage("http://www.sharehoster.com/?open=download_prepare&file=" + fileID);
        // Streaming links don't need any captchas
        String dllink = br.getRegex("addVariable\\(\"file\",\"(http://.*?)\"\\)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://media\\d+\\.sharehoster.com/stream/[a-z0-9]+\\.flv)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("type=\"hidden\" name=\"stream\" value=\"(http://.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(http://media\\d+\\.sharehoster\\.com/video/[a-z0-9/]+\\.avi)\"").getMatch(0);
                }
            }
        }
        if (dllink == null) {
            for (int i = 0; i <= 5; i++) {
                br.setFollowRedirects(false);
                Form dlform = br.getFormbyProperty("name", "downloadprepare");
                if (dlform == null || !br.containsHTML("captcha.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String captchaUrl = "http://www.sharehoster.com/content/captcha.php";
                String code = getCaptchaCode(captchaUrl, downloadLink);
                dlform.put("code", code);
                br.submitForm(dlform);
                dllink = br.getRedirectLocation();
                if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (dllink.contains("&err")) {
                    br.setFollowRedirects(true);
                    br.getPage(br.getRedirectLocation());
                    continue;
                }
                break;
            }
            if (dllink.contains("&error")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -8);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}