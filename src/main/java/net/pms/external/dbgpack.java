package net.pms.external;

/*
 * Examples:
 * 
 * public Object dbgpack_cb() {
 * 	return mylog;
 * }
 * 
 * public Object dbgpack_cb() {
 * 	return new String[] {mylog, myconf};
 * }
 */

public interface dbgpack {
	// Return a String or String[]
	public Object dbgpack_cb();
}
