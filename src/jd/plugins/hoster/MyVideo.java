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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.decrypter.TbCm;
import jd.plugins.decrypter.TbCm.DestinationFormat;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myvideo.de" }, urls = { "http://[\\w\\.]*?myvideo.*?/.*?/\\d+\\.flv" }, flags = { 0 })
public class MyVideo extends PluginForHost {
    static private final String AGB = "http://www.myvideo.de/news.php?rubrik=jjghf&p=hm8";

    public MyVideo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return MyVideo.AGB;
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert nachprüfen */
        return 1;
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL());
        if (this.dl.startDownload()) {
            if (downloadLink.getProperty("convertto") != null) {
                final DestinationFormat convertto = DestinationFormat.valueOf(downloadLink.getProperty("convertto").toString());
                final DestinationFormat InType = DestinationFormat.VIDEOFLV;
                /* to load the TbCm plugin */
                JDUtilities.getPluginForDecrypt("youtube.com");
                if (!TbCm.ConvertFile(downloadLink, InType, convertto)) {
                    logger.severe("Video-Convert failed!");
                }
            }
        }
    }

    // @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /*
         * warum sollte ein video das der decrypter sagte es sei online, offline
         * sein ;)
         */
        return AvailableStatus.TRUE;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    // @Override
    public void resetPluginGlobals() {
    }

}
