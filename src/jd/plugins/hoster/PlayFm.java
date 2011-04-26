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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "play.fm" }, urls = { "http://(www\\.)?play\\.fm/(recording/\\w+|(recordings)?#play_\\d+)" }, flags = { 0 })
public class PlayFm extends PluginForHost {

    private String DLLINK = null;

    public PlayFm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.orgasm.com/termsconditions.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 30);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        final String id = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().lastIndexOf("_") + 1);
        if (id == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        br.getPage("http://www.play.fm/flexRead/recording?rec%5Fid=" + id);

        if (br.containsHTML("var vid_title = \"\"")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<title><!\\[CDATA\\[(.*?)\\]\\]></title>").getMatch(0);
        final String highBitrate = br.getRegex("<file_id>(.*?)</file_id>").getMatch(0);
        final String fileId1 = br.getRegex("<file_id>(.*?)</file_id>").getMatch(0, 1);
        final String url = br.getRegex("<url>(.*?)</url>").getMatch(0);
        final String uuid = br.getRegex("<uuid>(.*?)</uuid>").getMatch(0);
        if (filename == null || highBitrate == null || fileId1 == null || url == null || uuid == null) {
            filename = br.getRegex("content=\"(.*?) \\- Madthumbs\\.com\"").getMatch(0);
        }

        DLLINK = "http://" + url + "/public/" + highBitrate + "/offset/0/sh/" + uuid + "/rec/" + id + "/jingle/" + fileId1 + "/loc/";
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".wav");
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
