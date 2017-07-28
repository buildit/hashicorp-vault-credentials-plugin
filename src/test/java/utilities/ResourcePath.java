package utilities;

import java.net.URLDecoder;

public class ResourcePath {
    public static String resourcePath(String path) {
        String result;
        try{
            result = URLDecoder.decode(new ResourcePath().getClass().getClassLoader().getResource(path).toString().replace("file:", ""), "UTF8");
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        System.out.println("Full path of " + path + " is " + result);
        return result == null || result.endsWith("null") ? null : result;
    }
}
