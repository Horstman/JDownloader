package jd.utils.locale;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.event.MessageEvent;
import jd.event.MessageListener;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;

public class Converter implements MessageListener {
    static {
        // USe Jacksonmapper in this project
        JSonStorage.setMapper(new JacksonMapper());

    }

    public static void main(String[] args) throws IOException {

        new Converter().start();
    }

    private SrcParser sourceParser;

    private void start() throws IOException {
        sourceParser = new SrcParser(new File("src/"));
        sourceParser.getBroadcaster().addListener(this);
        sourceParser.parse();

        convert();

    }

    private void convert() throws IOException {
        for (TInterface ti : InterfaceCache.list()) {
            if (ti.getMap().size() == 0) continue;
            if (ti.getPath().toString().contains(".java")) continue;
            System.out.println("--->" + ti.getPath());

            StringBuilder sb = new StringBuilder();
            HashMap<String, HashMap<String, String>> lngfiles = new HashMap<String, HashMap<String, String>>();
            for (File f : new File("ressourcen/jd/languages/").listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.endsWith(".loc");
                }
            })) {
                lngfiles.put(f.getName().substring(0, f.getName().length() - 4), new HashMap<String, String>());
            }
            for (ConvertEntry c : ti.getMap().values()) {
                System.out.println(c);
                String defValue = c.getValue();
                if (defValue == null || defValue.equals("null")) {
                    defValue = readValue("en", c.getKey());
                }
                String key = c.getKey().replace(".", "_");
                sb.append("\r\n");
                String value = defValue;
                int count = 1;
                int index = 0;
                while (true) {
                    index = value.indexOf("%s", index);
                    if (index < 0) break;

                    value = value.substring(0, index) + "%s" + count + value.substring(index + 2);
                    index += 3;
                    count++;
                }
                count--;
                sb.append("@Default(lngs = { \"en\" }, values = { \"" + value + "\" })");
                for (String s : lngfiles.keySet()) {
                    HashMap<String, String> lsb = lngfiles.get(s);
                    String r = convert(readValue(s, c.getKey()));
                    if (r != null) lsb.put(key, r);
                }
                sb.append("\r\n");
                sb.append("Object " + key + "(");
                boolean first = true;
                for (int i = 0; i < count; i++) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append("String s" + (i + 1));
                }
                sb.append(");");

                for (File f : c.getFiles()) {
                    String content = IO.readFileToString(f);
                    int i = content.indexOf("import ");
                    String pkg = ti.getPath().toString().substring(4).replace("\\", ".").replace("/", ".");
                    content = content.substring(0, i) + "\r\n import " + pkg + ".*;\r\n" + content.substring(i);
                    String pat;

                    int found;
                    int matches = content.split("T\\._\\.").length;
                    pat = "JDL\\.L\\(\"" + Pattern.quote(c.getKey()) + "\",\\s\"" + Pattern.quote(defValue) + "\"\\)";
                    content = content.replaceAll(pat, "T._." + key + "()");
                    found = content.indexOf("\"" + c.getKey() + "\"");
                    if (found >= 0) {
                        pat = "JDL\\.LF\\(\"" + Pattern.quote(c.getKey()) + "\",\\s\"" + Pattern.quote(defValue) + "\",";
                        content = content.replaceAll(pat, "T._." + key + "(");
                        found = content.indexOf("\"" + c.getKey() + "\"");
                        if (found >= 0) {

                            pat = "JDL\\.L\\(\"" + Pattern.quote(c.getKey()) + "\",.*?\\)";
                            content = content.replaceAll(pat, "T._." + key + "()");
                            found = content.indexOf("\"" + c.getKey() + "\"");
                            if (found >= 0) {
                                pat = "JDL\\.LF\\(\"" + Pattern.quote(c.getKey()) + "\",.*?,";
                                content = content.replaceAll(pat, "T._." + key + "(");
                                found = content.indexOf("\"" + c.getKey() + "\"");
                                if (found >= 0) {

                                    System.out.println(2);

                                }
                            }

                        }
                    }
                    f.delete();
                    IO.writeStringToFile(f, content);
                }

            }
            System.out.println(sb);
            ti.getTranslationFile().delete();

            StringBuilder sb2 = new StringBuilder();
            String pkg = ti.getPath().toString().substring(4).replace("\\", ".").replace("/", ".");
            sb2.append("package " + pkg + ";\r\n");
            sb2.append("import org.appwork.txtresource.*;\r\n");
            sb2.append("@Defaults(lngs = { \"en\"})\r\n");

            sb2.append("public interface " + ti.getClassName() + "Translation extends " + ti.getClassName() + "Interface {\r\n");
            sb2.append(sb + "\r\n");
            sb2.append("}");

            IO.writeStringToFile(ti.getTranslationFile(), sb2.toString());
            for (String s : lngfiles.keySet()) {
                HashMap<String, String> lsb = lngfiles.get(s);

                ti.getShortFile().delete();
                ti.getTranslationFile().getParentFile().mkdirs();
                File lngF = new File(ti.getPath(), ti.getClassName() + "Translation." + s + ".lng");
                lngF.delete();
                IO.writeStringToFile(lngF, JSonStorage.toString(lsb));
                sb2 = new StringBuilder();
                pkg = ti.getPath().toString().substring(4).replace("\\", ".").replace("/", ".");
                sb2.append("package " + pkg + ";\r\n");
                sb2.append("import org.appwork.txtresource.TranslationFactory;\r\n");
                sb2.append("public class T {\r\n");
                sb2.append("public static final " + ti.getClassName() + " T = TranslationFactory.create(" + ti.getClassName() + ".class);\r\n");
                sb2.append("}");
                IO.writeStringToFile(ti.getShortFile(), sb2.toString());

            }
        }
    }

    private String convert(String value) {
        if (value == null) return null;
        int count = 1;
        int index = 0;
        while (true) {
            index = value.indexOf("%s", index);
            if (index < 0) break;

            value = value.substring(0, index) + "%s" + count + value.substring(index + 2);
            index += 3;
            count++;
        }
        return value;
    }

    private String readValue(String lng, String key) throws IOException {
        File file = new File("ressourcen/jd/languages/" + lng + ".loc");
        String[] content = Regex.getLines(IO.readFileToString(file));
        for (String l : content) {
            l = l.trim();
            int i = l.indexOf("=");
            if (key.equals(l.substring(0, i).trim())) {
                String v = l.substring(i + 1).trim();
                return v;
            }

        }
        return null;
    }

    public void onMessage(MessageEvent event) {
        System.out.println(event.getMessage());
    }
}
