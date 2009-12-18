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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wat.tv" }, urls = { "http://[\\w\\.]*?wat\\.tv/video/.*?\\.html" }, flags = { 0 })
public class WatTv extends PluginForHost {

    public WatTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.wat.tv/cgu";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("ERREUR 404 : Cette page n'existe pas")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>.*?\"(.*?)\"").getMatch(0);
        if (filename == null || filename.equals("")) filename = br.getRegex("<h2 class=\"titre\">(.*?)</h2>").getMatch(0);
        if (filename == null || filename.equals("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim() + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // TODO:Some videosources are RMTP Streams, at the moment this ones
        // can't be downloaded, example:
        // http://www.wat.tv/video/je-suis-belle-je-assume-1zzzj_10zxx_.html
        String videoid = br.getRegex("id=\"media\" value=\"(.*?)\"").getMatch(0);
        if (videoid == null) {
            videoid = br.getRegex("videoId : \"(.*?)\"").getMatch(0);
            if (videoid == null) {
                videoid = br.getRegex("mediaId=(.*?)'").getMatch(0);
                if (videoid == null) {
                    videoid = br.getRegex("abuse/video/(.*?)'").getMatch(0);
                }
            }
        }
        if (videoid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser br2 = br.cloneBrowser();
        br2.getPage("http://www.wat.tv/interface/contentv2/" + videoid);
        String sources = br2.toString().replace("\\", "");
        String dllink = new Regex(sources, "\"(http://www.wat.tv/get/.*?\\.flv)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
