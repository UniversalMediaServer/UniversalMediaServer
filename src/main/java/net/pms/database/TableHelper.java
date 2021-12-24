package net.pms.database;

import net.pms.PMS;

public abstract class TableHelper extends DatabaseHelper {
	protected static final MediasDatabase DATABASE = PMS.get().getDatabase();

	// Generic constant for the maximum string size: 255 chars
	protected static final int SIZE_MAX = 255;

}
