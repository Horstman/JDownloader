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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "http://[//w//.]*?soundcloud\\.com/.*?/[a-z\\-_0-9]+" }, flags = { 0 })
public class SoundcloudCom extends PluginForHost {

    private String url;

    public SoundcloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://soundcloud.com/terms-of-use";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);

        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("Oops, looks like we can't find that page")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<em>(.*?)</em>").getMatch(0);

        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim().replace("&amp; ", "");
        String type = br.getRegex("title=\"Uploaded format\">(.*?)<").getMatch(0);
        if (type == null) type = "mp3";
        filename += "." + type;
        String[] data = br.getRegex("\"uid\":\"(.*?)\".*?\"token\":\"(.*?)\"").getRow(0);
        url = "http://media.soundcloud.com/stream/" + data[0] + "?stream_token=" + data[1];
        URLConnectionAdapter con = br.openGetConnection(url);
        if (!con.getContentType().contains("html"))
            parameter.setDownloadSize(con.getContentLength());
        else
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        parameter.setFinalFileName(filename);
        con.disconnect();
        return AvailableStatus.TRUE;
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
