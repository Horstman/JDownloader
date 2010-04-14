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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileserve.com" }, urls = { "http://[\\w\\.]*?fileserve\\.com/file/[a-zA-Z0-9]+" }, flags = { 0 })
public class FileServeCom extends PluginForHost {

    public FileServeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileserve.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(The file could not be found|Please check the download link|<p>File not available</p>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        String filesize = br.getRegex("<span><strong>(.*?)</strong>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.postPage(downloadLink.getDownloadURL(), "downloadLink=wait");
        // Ticket Time
        String reconTime = br.getRegex("(\\d+)").getMatch(0);
        int tt = 60;
        if (reconTime != null) {
            logger.info("Waittime detected, waiting " + reconTime + " seconds from now on...");
            tt = Integer.parseInt(reconTime);
        }
        sleep(tt * 1001, downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "download=normal");
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            String wait = br.getRegex("<p>You have to wait (\\d+) seconds to start another download").getMatch(0);
            if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            String wait = br.getRegex("<p>You have to wait (\\d+) seconds to start another download").getMatch(0);
            if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}