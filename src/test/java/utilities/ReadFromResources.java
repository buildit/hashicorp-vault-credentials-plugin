package utilities;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;

public class ReadFromResources {

    public static String readFromResources(String path) {
        try {
            String url = URLDecoder.decode(new ReadFromResources().getClass().getClassLoader().getResource(path).toString().replace("file:", ""), "UTF8");
            FileInputStream in = new FileInputStream(new File(url));
            String result = IOUtils.toString(in);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
