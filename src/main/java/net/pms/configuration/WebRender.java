package net.pms.configuration;

import net.pms.Messages;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;

import java.net.InetAddress;

public class WebRender extends RendererConfiguration {

    private String name;
    private String ip;
    private int port;

    public WebRender(String name)  throws ConfigurationException {
        super(null);
        this.name = name;
        ip = "";
        port = 0;
    }

    public String getRendererName() {
        String ipStr;
        if (StringUtils.isNotEmpty(ip)) {
            ipStr = "(" + ip + (port != 0 ? ":" + port : "") + ")";
        }
        else {
            ipStr = "";
        }
        return name + "@" + Messages.getString("PMS.140") + ipStr;
    }

    public void associateIP(InetAddress sa) {
        super.associateIP(sa);
        ip = sa.getHostAddress();
    }

    public void associatePort(int port) {
        this.port = port;
    }

    public String toString() {
        return getRendererName();
    }
}
