//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "veoh.com" }, urls = { "http://(www\\.)?veoh.com/browse/videos/category/.*?/watch/[A-Za-z0-9]+" }, flags = { 0 })
public class VeohCom extends PluginForHost {

    public VeohCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.veoh.com/corporate/termsofuse";
    }

    // Important: If this key changes we also have to change it ;)
    private static final String APIKEY = "NTY5Nzc4MUUtMUM2MC02NjNCLUZGRDgtOUI0OUQyQjU2RDM2";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        String videoID = new Regex(downloadLink.getDownloadURL(), "/watch/(.+)").getMatch(0);
        br.getPage("http://www.veoh.com/rest/v2/execute.xml?apiKey=" + Encoding.Base64Decode(APIKEY) + "&method=veoh.video.findByPermalink&permalink=" + videoID + "&");
        if (br.containsHTML("(<rsp stat=\"fail\"|\"The video does not exist\"|name=\"YouTube\\.com\" type=\"\")")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String extension = br.getRegex("extension=\"(.*?)\"").getMatch(0);
        String filename = br.getRegex("title=\"(.*?)\"").getMatch(0);
        DLLINK = br.getRegex("(fullPreviewHashPath|previewUrl)=\"(http://.*?)\"").getMatch(1);
        if (DLLINK == null) DLLINK = br.getRegex("\"(http://content\\.veoh\\.com/flash/p/\\d+/[A-Za-z0-9]+/[A-Za-z0-9]+\\.fll\\?ct=[A-Za-z0-9]+)\"").getMatch(0);
        if (filename == null || DLLINK == null || extension == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + extension);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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
