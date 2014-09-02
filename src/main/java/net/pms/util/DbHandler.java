package net.pms.util;

public interface DbHandler {
	public Object create(String[] args);
	public String[] format(Object obj);
	public String name();
}
