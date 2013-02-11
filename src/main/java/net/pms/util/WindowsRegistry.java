/*
 * This code is based on http://transoceanic.blogspot.cz/2011/12/java-read-key-from-windows-registry.html
 */
package net.pms.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class WindowsRegistry {
	 
    /**
     *
     * @param location path in the registry
     * @param key registry key
     * @return registry value or null if not found
     */
    public static final String readRegistry(String location, String key){
        try {
            // Run reg query, then read output with StreamReader (internal class)
        	String query = "reg query " + '"'+ location + "\" /v \"" + key + '"';
            Process process = Runtime.getRuntime().exec(query);
 
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
 
            // Parse out the value
            String result = reader.getResult();
            String parsed = result.substring(result.indexOf("REG_SZ") + 6).trim();

            if (parsed.length() > 1) {
               return parsed;
            }
            
        } catch (Exception e) {}
 
        return null;
    }
 
    static class StreamReader extends Thread {
        private InputStream is;
        private StringWriter sw= new StringWriter();
 
        public StreamReader(InputStream is) {
            this.is = is;
        }
 
        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1)
                    sw.write(c);
            } catch (IOException e) {
            }
        }
 
        public String getResult() {
            return sw.toString();
        }
    }
 }