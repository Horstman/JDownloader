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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "degracaemaisgostoso.info", "altervista.org", "agaleradodownload.com", "adf.ly", "musicloud.fm", "wowebook.com", "link.songs.pk + songspk.info", "imageto.net", "clubteam.eu", "jforum.uni.cc", "linksole.com", "deurl.me", "yourfileplace.com", "cliphunter.com", "muzgruz.ru", "zero10.net", "aiotool.net", "chip.de/c1_videos", "multiprotect.info", "nbanews.us", "wwenews.us", "top2tech.com", "umquetenha.org", "oneclickmoviez.com/dwnl/", "1tool.biz", "trailerzone.info", "imagetwist.com", "file4ever.us and catchfile.net", "zero10.net and gamz.us", "official.fm", "hypem.com", "academicearth.org", "skreemr.org", "tm-exchange.com", "adiarimore.com", "mafia.to/download", "bogatube.com", "newgrounds.com", "accuratefiles.com", "slutdrive.com", "view.stern.de", "fileblip.com" }, urls = {
        "http://[\\w\\.]*?degracaemaisgostoso\\.(biz|info)/download/\\?url=.*?:ptth", "http://[\\w\\.]*?altervista\\.org/\\?i=[0-9a-zA-Z]+", "http://[\\w\\.]*?agaleradodownload\\.com/download.*?\\?.*?//:ptth", "http://[\\w\\.]*?adf\\.ly/[A-Za-z0-9]+(/[A-Za-z0-9-]+)?", "http://[\\w\\.]*?musicloud\\.fm/dl/[A-Za-z0-9]+", "http://[\\w\\.]*?wowebook\\.com/(e-|non-e-)book/.*?/.*?\\.html", "http://[\\w\\.]*?(link\\.songs\\.pk/(popsong|song1|bhangra)\\.php\\?songid=|songspk\\.info/ghazals/download/ghazals\\.php\\?id=)[0-9]+", "http://[\\w\\.]*?imageto\\.net/(\\?v=|images/)[0-9a-z]+\\..{2,4}", "http://[\\w\\.]*?clubteam\\.eu/dl\\.php\\?id=\\d\\&c=[a-zA-z0-9=]+", "http://[\\w\\.]*?jforum\\.uni\\.cc/protect/\\?r=[a-z0-9]+", "http://[\\w\\.]*?linksole\\.com/[0-9a-z]+", "http://[\\w\\.]*?deurl\\.me/[0-9A-Z]+", "http://[\\w\\.]*?yourfileplace\\.com/files/\\d+/.+\\.html",
        "http://[\\w\\.]*?cliphunter\\.com/w/\\d+/", "http://[\\w\\.]*?muzgruz\\.ru/music/download/\\d+", "http://[\\w\\.]*?zero10\\.net/\\d+", "http://[\\w\\.]*?aiotool\\.net/\\d+", "http://[\\w\\.]*?chip\\.de/c1_videos/.*?-Video_\\d+\\.html", "http://[\\w\\.]*?multiprotect\\.info/\\d+", "http://[\\w\\.]*?nbanews\\.us/\\d+", "http://[\\w\\.]*?wwenews\\.us/\\d+", "http://[\\w\\.]*?top2tech\\.com/\\d+", "http://[\\w\\.]*?umquetenha\\.org/protecao/resolve\\.php\\?link=.+", "http://[\\w\\.]*?oneclickmoviez\\.com/dwnl/.*?/\\d+/\\d+", "http://[\\w\\.]*?1tool\\.biz/\\d+", "http://[\\w\\.]*?trailerzone\\.info/(protect|wait(2)?)\\.php\\?(key|u)=[a-zA-Z0-9=/]+", "http://[\\w\\.]*?imagetwist\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?(file4ever\\.us|catchfile\\.net)/\\d+", "http://[\\w\\.]*?(zero10\\.net/|gamz\\.us/\\?id=)\\d+", "http://(www\\.)?official\\.fm/track/\\d+",
        "http://[\\w\\.]*?hypem\\.com/track/\\d+/", "http://[\\w\\.]*?academicearth\\.org/lectures/.+", "http://[\\w\\.]*?skreemr\\.org/link\\.jsp\\?id=[A-Z0-9]+", "http://[\\w\\.]*?tm-exchange\\.com/(get\\.aspx\\?action=trackgbx|\\?action=trackshow)\\&id=\\d+", "http://[\\w\\.]*?adiarimore\\.com/miralink/[a-z0-9]+", "http://[\\w\\.]*?mafia\\.to/download-[a-z0-9]+\\.cfm", "http://[\\w\\.]*?bogatube\\.com/tube/\\d+/\\d+/.*?\\.php", "http://[\\w\\.]*?newgrounds\\.com/(portal/view/|audio/listen/)\\d+", "http://(www\\.)?accuratefiles\\.com/fileinfo/[a-z0-9]+", "http://(www\\.)?slutdrive\\.com/video-\\d+\\.html", "http://(www\\.)?view\\.stern\\.de/de/(picture|original)/.*?-\\d+\\.html", "http://(www\\.)?fileblip\\.com/[a-z0-9]+" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class DecrypterForRedirectServicesWithoutDirectRedirects extends PluginForDecrypt {

    public DecrypterForRedirectServicesWithoutDirectRedirects(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String NEWSREGEX2 = "<div id='prep2' dir='ltr' ><a  href='(.*?)'";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.setReadTimeout(60 * 1000);
        String finallink = null;
        if (!parameter.contains("imageto.net/") && !parameter.contains("musicloud.fm/dl") && !parameter.contains("yourfileplace.com/") && !parameter.contains("oneclickmoviez.com/dwnl/") && !parameter.contains("1tool.biz") && !parameter.contains("catchfile.net") && !parameter.contains("file4ever.us") && !parameter.contains("trailerzone.info/") && !parameter.contains("fairtilizer.com/") && !parameter.contains("tm-exchange.com/") && !parameter.contains("fileblip.com/")) br.getPage(parameter);
        if (parameter.contains("adf.ly/")) {
            finallink = br.getRedirectLocation();
            if (finallink == null) finallink = br.getRegex("\\.attr\\(\"href\", \\'(.*?)\\'\\)").getMatch(0);
        } else if (parameter.contains("link.songs.pk/") || parameter.contains("songspk.info/ghazals/download/ghazals.php?id=")) {
            finallink = br.getRedirectLocation();
            if (finallink != null) finallink = "directhttp://" + finallink;
        } else if (parameter.contains("musicloud.fm/")) {
            String theId = new Regex(parameter, "musicloud\\.fm/dl/(.+)").getMatch(0);
            if (theId == null) return null;
            br.getPage("http://musicloud.fm/dl.php?id=" + theId);
            finallink = br.getRedirectLocation();
            if (finallink != null) finallink = "directhttp://" + finallink;
        } else if (parameter.contains("imageto")) {
            if (parameter.contains("imageto.net/images/")) {
                finallink = "directhttp://" + parameter;
            } else {
                String fileid = new Regex(parameter, "imageto\\.net/\\?v=(.+)").getMatch(0);
                finallink = "directhttp://http://imageto.net/images/" + fileid;
            }
        } else if (parameter.contains("clubteam.eu/dl")) {
            finallink = br.getRegex("content='0; url=(.*?)'>").getMatch(0);
        } else if (parameter.contains("wowebook.com")) {
            String redirectLink = br.getRegex("\"(http://www\\.wowebook\\.com/download.*?)\"").getMatch(0);
            if (redirectLink == null) return null;
            br.getPage(redirectLink);
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("jforum.uni.cc/")) {
            finallink = br.getRegex("<frame name=\"page\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("linksole.com/")) {
            finallink = br.getRegex("linkRefererUrl = '(.*?)';").getMatch(0);
            if (finallink == null) finallink = br.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("deurl.me/")) {
            finallink = br.getRegex("<i><small>(http://.*?)</small></i>").getMatch(0);
            if (finallink == null) finallink = br.getRegex("- <a href=\"(http://.*?)\">Click here to visit this").getMatch(0);
        } else if (parameter.contains("yourfileplace.com/")) {
            finallink = parameter.replace("yourfileplace", "rapidshare");
        } else if (parameter.contains("cliphunter.com/")) {
            finallink = br.getRegex(">shown on:<a href=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("muzgruz.ru/music/")) {
            finallink = "directhttp://" + parameter;
        } else if (parameter.contains("aiotool.net/")) {
            String id = new Regex(parameter, "aiotool\\.net/(\\d+)").getMatch(0);
            String accessThis = "http://aiotool.net/3-" + id + ".html";
            br.getPage(accessThis);
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("chip.de/c1_videos")) {
            finallink = br.getRegex("id=\"player\" href=\"(http://.*?\\.flv)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://video\\.chip\\.de/\\d+/.*?.flv)\"").getMatch(0);
            if (finallink != null) finallink = "directhttp://" + finallink;
        } else if (parameter.contains("multiprotect.info/")) {
            br.setFollowRedirects(true);
            String id = new Regex(parameter, "multiprotect\\.info/(\\d+)").getMatch(0);
            br.getPage("http://multiprotect.info/index.php?id=" + id + "&d=1");
            finallink = br.getRegex("<div id='prep2' ><a  href='(.*?)'").getMatch(0);
        } else if (parameter.contains("nbanews.us/")) {
            br.setFollowRedirects(true);
            String id = new Regex(parameter, "nbanews\\.us/(\\d+)").getMatch(0);
            br.getPage("http://www.nbanews.us/m1.php?id=" + id);
            finallink = br.getRegex("NewWindow\\('(.*?)'").getMatch(0);
        } else if (parameter.contains("wwenews.us/")) {
            br.setFollowRedirects(true);
            String id = new Regex(parameter, "wwenews\\.us/(\\d+)").getMatch(0);
            br.getPage("http://www.wwenews.us/m1.php?id=" + id);
            finallink = br.getRegex("NewWindow\\('(.*?)'").getMatch(0);
        } else if (parameter.contains("top2tech.com/")) {
            String id = new Regex(parameter, "top2tech\\.com/(\\d+)").getMatch(0);
            br.getPage("http://top2tech.com/2-" + id);
            finallink = br.getRegex("onclick=\"NewWindow\\('(.*?)','").getMatch(0);
        } else if (parameter.contains("umquetenha.org/protecao/resolve.php?link=")) {
            finallink = br.getRegex("http-equiv=\"refresh\" content=\"\\d+; url=(http://.*?)\"").getMatch(0);
        } else if (parameter.contains("oneclickmoviez.com/dwnl/")) {
            Regex allMatches = new Regex(parameter, "oneclickmoviez\\.com/dwnl/(.*?)/(\\d+)/(\\d+)");
            String host = allMatches.getMatch(0);
            String id1 = allMatches.getMatch(1);
            String id2 = allMatches.getMatch(2);
            String getLink = "http://oneclickmoviez.com/wp-content/plugins/link-cloaking-plugin/wplc_redirector.php?post_id=" + id1 + "&link_num=" + id2 + "&cloaked_url=dwnl/" + host + "/" + id1 + "/" + id2;
            br.getPage(getLink);
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("1tool.biz/")) {
            String id = new Regex(parameter, "1tool\\.biz/(\\d+)").getMatch(0);
            br.getPage("http://1tool.biz/2.php?id=" + id);
            finallink = br.getRegex("onclick=\"NewWindow\\('(.*?)','").getMatch(0);
        } else if (parameter.contains("agaleradodownload.com")) {
            String url = new Regex(parameter, "download.*?\\?(.+)").getMatch(0);
            if (url != null) {
                char[] temp = url.toCharArray();
                StringBuilder sb = new StringBuilder("");
                for (int i = url.length() - 1; i >= 0; i--) {
                    sb.append(temp[i]);
                }
                finallink = sb.toString();
            }
        } else if (parameter.contains("trailerzone.info/")) {
            br.getPage(parameter);
            finallink = br.getRegex("var xlink = '(.*?)';").getMatch(0);
            if (finallink == null) {
                String timeStamp = br.getRegex("name=\"timestamp\" value=\"(.*?)\"").getMatch(0);
                String link = br.getRegex("name=\"link\" value=\"(.*?)\"").getMatch(0);
                if (timeStamp == null || link == null) return null;
                br.postPage("http://trailerzone.info/go.php", "timestamp=" + timeStamp + "&link=" + link + "&redir=Weiter+zum+Link");
                finallink = br.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("imagetwist.com/")) {
            finallink = br.getRegex("\"(http://img\\d+\\.imagetwist\\.com/i/\\d+/.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("<p><img src=\"(http://.*?)\"").getMatch(0);
            if (finallink != null) finallink = "directhttp://" + finallink;
        } else if (parameter.contains("catchfile.net") || parameter.contains("file4ever.us")) {
            String damnID = new Regex(parameter, "/(\\d+)$").getMatch(0);
            br.getPage(parameter.replace(damnID, "") + "file.php?id=" + damnID);
            finallink = br.getRegex("<td width=\"70%\">[\t\n\r ]+<a href=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("gamz.us/") || parameter.contains("zero10.net/")) {
            String damnID = new Regex(parameter, "(\\d+)$").getMatch(0);
            br.getPage("http://gamz.us/?id=" + damnID + "&d=1");
            finallink = br.getRegex(NEWSREGEX2).getMatch(0);
        } else if (parameter.contains("official.fm/")) {
            br.setFollowRedirects(true);
            br.getPage(parameter + ".xspf?ll_header=yes");
            finallink = br.getRegex("\"(http://cdn\\.official\\.fm/mp3s/\\d+/\\d+\\.mp3)\"").getMatch(0);
            if (finallink != null) finallink = "directhttp://" + finallink;
        } else if (parameter.contains("hypem.com/")) {
            String id0 = br.getRegex("key: \\'(.*?)\\',").getMatch(0);
            if (id0 == null) return null;
            br.getPage("http://hypem.com/serve/play/" + new Regex(parameter, "hypem\\.com/track/(\\d+)/").getMatch(0) + "/" + id0);
            finallink = br.getRedirectLocation();
            if (finallink != null) finallink = "directhttp://" + finallink;
        } else if (parameter.contains("academicearth.org/")) {
            if (!(br.getRedirectLocation() != null && br.getRedirectLocation().contains("users/login"))) {
                finallink = br.getRegex("flashVars\\.flvURL = \"(.*?)\"").getMatch(0);
                if (finallink == null) finallink = br.getRegex("<div><embed src=\"(.*?)\"").getMatch(0);
                if (finallink != null) {
                    if (!finallink.contains("blip.tv") && !finallink.contains("youtube")) throw new DecrypterException("Found unsupported link in link: " + parameter);
                    if (finallink.contains("blip.tv/")) {
                        br.getPage(finallink);
                        finallink = br.getRedirectLocation();
                        if (finallink != null) finallink = "directhttp://" + finallink;
                    }
                }
            } else {
                throw new DecrypterException("Login required to download link: " + parameter);
            }
        } else if (parameter.contains("skreemr.org/")) {
            finallink = br.getRegex(";soundFile=(http.*?\\.mp3)\\'").getMatch(0);
            if (finallink != null) finallink = "directhttp://" + Encoding.htmlDecode(finallink);
        } else if (parameter.contains("tm-exchange.com/")) {
            finallink = "directhttp://" + parameter.replace("?action=trackshow", "get.aspx?action=trackgbx");
        } else if (parameter.contains("adiarimore.com/")) {
            finallink = br.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("altervista.org")) {
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("mafia.to/download")) {
            br.getPage(parameter.replace("download-", "dl-"));
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("bogatube.com/")) {
            // Those site only contains videos of xvideos.com, just find the id
            // and make the link
            String videoID = br.getRegex("id_video=(\\d+)\"").getMatch(0);
            if (videoID != null) finallink = "http://xvideos.com/video" + videoID;
        } else if (parameter.contains("newgrounds.com/")) {
            if (parameter.contains("/audio/listen/")) {
                finallink = br.getRegex("\\(\"filename=(.*?)\\&length").getMatch(0);
            } else {
                finallink = br.getRegex("var fw = new FlashWriter\\(\"(http.*?\\.swf)\"").getMatch(0);
            }
        } else if (parameter.contains("accuratefiles.com/")) {
            String fileID = br.getRegex("\"/go/(\\d+)\"").getMatch(0);
            if (fileID == null) fileID = br.getRegex("name=\"file_id\" value=\"(\\d+)\"").getMatch(0);
            if (fileID == null) return null;
            br.postPage("http://www.accuratefiles.com/go/" + fileID, "file_id=" + fileID);
            finallink = br.getRegex("id=\"fdown\" name=\"fdown\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("slutdrive.com/video-")) {
            finallink = br.getRegex("allowscriptaccess=\"always\" flashvars=\"file=(http://.*?\\.flv)\\&image=").getMatch(0);
            if (finallink == null) finallink = br.getRegex("(http://fs\\d+\\.slutdrive\\.com/videos/\\d+\\.flv)").getMatch(0);
            if (finallink != null) finallink = "directhttp://" + finallink;
        } else if (parameter.contains("view.stern.de/de/")) {
            br.setFollowRedirects(true);
            br.getPage(parameter.replace("/picture/", "/original/"));
            if (br.containsHTML("/erotikfilter/")) {
                br.postPage(br.getURL(), "savefilter=1&referer=" + Encoding.urlEncode(parameter.replace("/original/", "/picture/")) + "%3Fr%3D1%26g%3Dall");
                br.getPage(parameter.replace("/picture/", "/original/"));
            }
            finallink = br.getRegex("<div class=\"ImgBig\" style=\"width:\\d+px\">[\t\n\r ]+<img src=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://view\\.stern\\.de/de/original/([a-z0-9]+/)?\\d+/.*?\\..{3,4})\"").getMatch(0);
            if (finallink != null) finallink = "directhttp://" + finallink;

        } else if (parameter.contains("degracaemaisgostoso")) {
            String tmp = new Regex(parameter, "url=(.+)").getMatch(0);
            if (tmp != null) {
                StringBuilder sb = new StringBuilder("");
                for (int index = tmp.length() - 1; index >= 0; index--) {
                    sb.append(tmp.charAt(index));
                }
                finallink = sb.toString();
            }
        } else if (parameter.contains("fileblip.com/")) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("X-Prototype-Version", "1.6.0.3");
            br.postPage("http://fileblip.com/getdata.php", "id=" + new Regex(parameter, "fileblip\\.com/(.+)").getMatch(0));
            finallink = br.getRegex("onload=\"framebreakerbreaker\\(\\)\" src=\"(.*?)\"").getMatch(0);
        }
        if (finallink == null) {
            logger.info("DecrypterForRedirectServicesWithoutDirectRedirects says \"Out of date\" for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }
}
