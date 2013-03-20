package net.pms.external;

import java.util.List;
import net.pms.dlna.DLNAResource;

public interface URLResolver extends ExternalListener {
	public static final String ID = "URLResolver";
	class URLResult {
		public String url;
		public List<String> args;
		public List<String> precoder;
		public int flags;
	}

	public URLResult urlResolve(String url, DLNAResource dlna);
}
