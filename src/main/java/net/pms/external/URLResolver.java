package net.pms.external;


public interface URLResolver extends ExternalListener{
	
	class URLResult {
		public String url;
		public String[] args;
	}
	
	public URLResult urlResolve(String url);

}
