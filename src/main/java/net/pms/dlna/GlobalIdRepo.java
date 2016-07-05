package net.pms.dlna;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalIdRepo {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalIdRepo.class);
	
	/**
	 * Store <id, filename>
	 */
	Map<String, String> idMap = new WeakHashMap<>();
	/**
	 * Store <id, filename>
	 */
	Map<String, DLNAResource> resourcesMap = new WeakHashMap<>();
	/**
	 * Store <filename, id>
	 */
	Map<String, String> filenameMap = new WeakHashMap<>();

	// Global ids start at 1, since id 0 is reserved as a pseudonym for 'renderer root'
	private int globalId = 1, deletions = 0;

	class ID {
		int id;
		DLNAResource dlna;
		ID(DLNAResource d) {
			id = globalId++;
			d.setIndexId(id);
			dlna = d;
		}
		
		
		@Override
		public String toString() {
			return dlna.toString();
		}
	}
	private ArrayList<ID> ids = new ArrayList<>();

	public GlobalIdRepo() {
	}

	public String getId(String filename) {
		return filenameMap.get(filename);
	}
	
	public String getFilename(String id) {
		return idMap.get(id);
	}
	
	public synchronized void add(DLNAResource d) {
		if (d.getId() == null) {
			d.setIndexId(globalId++);
		}
		
		if (d.getMedia() != null) {
		filenameMap.put(d.getId(), d.getMedia().getFileName());
		idMap.put(d.getMedia().getFileName(), d.getId());
		}
		resourcesMap.put(d.getId(), d);
	}

	public DLNAResource get(String id) {
//		return get(parseIndex(id));
		return resourcesMap.get(id);
	}

	public synchronized DLNAResource get(int id) {
		int index = indexOf(id);
		return index > -1 ? ids.get(index).dlna : null;
	}

	public void remove(DLNAResource d) {
		remove(d.getId());
	}

	public void remove(String id) {
		remove(parseIndex(id));
	}

	public synchronized void remove(int id) {
		int index = indexOf(id);
		if (index > -1) {
			LOGGER.debug("GlobalIdRepo: removing id {} - {}", id, ids.get(index).dlna.getName());
			ids.remove(index);
			deletions++;
		}
	}

	public static int parseIndex(String id) {
		try {
			// Id strings may have optional tags beginning with $ appended, e.g. '1234$Temp'
			return Integer.parseInt(StringUtils.substringBefore(id, "$"));
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public boolean exists(String id) {
		return indexOf(parseIndex(id)) != -1;
	}

	private synchronized int indexOf(int id) {
		if (id > 0 && id < globalId) {
			// We're in sequence by definition, so binary search is quickest

			// Exclude any areas where the id can't possibly be
			int ceil = ids.size() - 1;
			int top = id - 1; // id 0 is reserved, so index is id-1 at most
			int hi = top < ceil ? top : ceil;
			int floor = hi - deletions;
			int lo = floor > 0 ? floor : 0;

			while (lo <= hi) {
				int mid = lo + (hi - lo) / 2;
				int idm = ids.get(mid).id;
				if (id < idm) {
					hi = mid - 1;
				} else if (id > idm) {
					lo = mid + 1;
				} else {
					return mid;
				}
			}
		}
		LOGGER.debug("GlobalIdRepo: id not found: {}", id);
		return -1;
	}
}
