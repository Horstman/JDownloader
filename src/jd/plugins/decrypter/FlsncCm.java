package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesonic.com" }, urls = { "http://[\\w\\.]*?filesonic\\.[a-z]{2,3}/.*?folder/[0-9a-z]+" }, flags = { 0 })
public class FlsncCm extends PluginForDecrypt {

    private static String geoDomain = null;

    public FlsncCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private synchronized String getDomain() {
        if (geoDomain != null) return geoDomain;
        String defaultDomain = "http://www.filesonic.com";
        try {
            geoDomain = getDomainAPI();
            if (geoDomain == null) {
                Browser br = new Browser();
                br.setCookie(defaultDomain, "lang", "en");
                br.setFollowRedirects(false);
                br.getPage(defaultDomain);
                geoDomain = br.getRedirectLocation();
                if (geoDomain == null) {
                    geoDomain = defaultDomain;
                } else {
                    String domain = new Regex(br.getRedirectLocation(), "http://.*?(filesonic\\..*?)/").getMatch(0);
                    geoDomain = "http://www." + domain;
                }
            }
        } catch (final Throwable e) {
            geoDomain = defaultDomain;
        }
        return geoDomain;
    }

    private synchronized String getDomainAPI() {
        try {
            Browser br = new Browser();
            br.setFollowRedirects(true);
            br.getPage("http://api.filesonic.com/utility?method=getFilesonicDomainForCurrentIp");
            String domain = br.getRegex("response>.*?filesonic(\\..*?)</resp").getMatch(0);
            if (domain != null) { return "http://www.filesonic" + domain; }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        return null;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String id = new Regex(parameter, "/(folder/[0-9a-z]+)").getMatch(0);
        if (id == null) return null;
        parameter = getDomain() + "/en/" + id;
        boolean failed = false;
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.containsHTML(">No links to show<")) return decryptedLinks;
        if (br.containsHTML("(Folder do not exist<|>The requested folder do not exist or was deleted by the owner|>If you want, you can contact the owner of the referring site to tell him about this mistake)")) return decryptedLinks;
        String[] links = br.getRegex("<td width=\"70%\" align=\"left\" valign=\"top\">(.*?)</tr>").getColumn(0);
        if (links == null || links.length == 0) {
            failed = true;
            links = br.getRegex("<td><a href=\"(http://.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(http://[^/\" ]*?filesonic\\.[a-z]{2,3}/[^\" ]*?file/\\d+/.*?)\"").getColumn(0);
        }
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String data : links) {
            if (failed) {
                if (!data.contains("/folder/")) decryptedLinks.add(createDownloadlink(data));
            } else {
                String filename = new Regex(data, "filesonic\\.[a-z]{2,3}/.*?file/.*?/(.*?)\"").getMatch(0);
                String filesize = new Regex(data, "valign=\"top\">.*?\\|.*?\\|.*?\\|.*?\\|(.*?)\\(.*?</td>").getMatch(0);
                String dlink = new Regex(data, "href=\"(http.*?)\"").getMatch(0);
                if (dlink == null) return null;
                DownloadLink aLink = createDownloadlink(dlink);
                if (filename != null) aLink.setName(filename.trim());
                if (filesize != null) aLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
                if (filename != null && filesize != null) aLink.setAvailable(true);
                if (!dlink.contains("/folder/")) decryptedLinks.add(createDownloadlink(dlink));
            }
            progress.increase(1);
        }
        return decryptedLinks;
    }

}
