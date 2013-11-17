package net.pms.configuration;

import net.pms.Messages;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;

import java.net.InetAddress;

public class WebRender extends RendererConfiguration {

    private String name;
    private String ip;
    private int port;
    private String ua;

    public WebRender(String name)  throws ConfigurationException {
        super(null);
        this.name = name;
        ip = "";
        port = 0;
        ua = "";
    }

    public String getRendererName() {
        String ipStr;
        if (StringUtils.isNotEmpty(ip)) {
            ipStr = "\n(" + ip + (port != 0 ? ":" + port : "") + ")";
        }
        else {
            ipStr = "";
        }
        return name + "@\n" + Messages.getString("PMS.140") + ipStr;
    }

    public void associateIP(InetAddress sa) {
        super.associateIP(sa);
        ip = sa.getHostAddress();
    }

    public void associatePort(int port) {
        this.port = port;
    }

    public void setUA(String ua) {
        this.ua = ua.toLowerCase();
    }

    public String getRendererIcon() {
        if (StringUtils.isEmpty(ua)) {
            return super.getRendererIcon();
        }
        if (ua.contains("chrome")) {
            return "chrome.png";
        }
        if (ua.contains("msie")) {
            return "ie.png";
        }
        if(ua.contains("firefox")) {
           return "ff.png";
        }
        return super.getRendererIcon();
    }

    public String toString() {
        return getRendererName();
    }
}
