package net.pms.dlna;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.pms.network.UPNPControl.Renderer;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalIdRepo {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalIdRepo.class);
	
	/**
	 * Store <filename, id>
	 */
	Map<String, String> idMap = new HashMap<>();
	/**
	 * Store <id, DLNAResource>
	 */
	Ehcache resourcesMap = null;
	/**
	 * Store <cookie, Renderer>
	 */
	Ehcache renderCache = null;
	/**
	 * Store <id, filename>
	 */
	Map<String, String> filenameMap = new HashMap<>();
	
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
		CacheManager cacheManager = CacheManager.newInstance();
		resourcesMap = cacheManager.addCacheIfAbsent("PMS");
		renderCache = cacheManager.addCacheIfAbsent("renderer"); 
	}

	public String getId(String filename) {
		return idMap.get(filename);
	}
	
	public String getFilename(String id) {
		return filenameMap.get(id);
	}
	
	public void addRenderer(String cookie, Renderer r) {
		Element el = new Element(cookie, r);
		el.setTimeToIdle(2 * 60); // 2 minutes
		renderCache.put(el);
	}
	
	public Renderer getRenderer(String cookie) {
		Renderer r = null;
		Element el = renderCache.get(cookie);
		if (el != null)
			r = (Renderer) el.getObjectValue();
		
		return r;
	}
	
	/**
	 * This method ensures a unique id for DLNAResource identified by a file (path + filename).
	 * @param d
	 */
	public synchronized void add(DLNAResource d) {
		String filename = d.getSystemName();
		String id = d.getId();
		if (get(id) != null)
			return;
		
		if (id == null) {
			id = getId(filename);
			if (id == null) {
				d.setIndexId(globalId++);
				id = d.getId();
			} else {
				d.setId(id);
				return;
			}
		}
		
		Element el = new Element(new Key(id, filename), d);
		if ("0".equals(id)) {
			el = new Element(new Key(id, null), d); // hashcode uses filename which is usually not available while fetching from cache
		}
		el.setEternal(true);
//		System.out.println(id + ": " + filename);
		
		resourcesMap.put(el);
//		idMap.put(filename, id);
//		filenameMap.put(id, filename);
//		System.out.println(resourcesMap.isKeyInCache(el) + " :::: " + get(id));
	}

	public synchronized DLNAResource get(String id) {
		Element el = resourcesMap.get(new Key(id, null));
		if (el == null)
			return null;
		return (DLNAResource) el.getObjectValue();
	}

	public void remove(DLNAResource d) {
		remove(d.getId());
	}

	public void remove(String id) {
		resourcesMap.remove(id);
//		filenameMap.remove(idMap.get(id));
//		idMap.remove(id);
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
		return idMap.containsKey(id);
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
class Key implements Serializable {
	String id, filename;
	Key(String id, String file) {
		this.id = id;
		this.filename = file;
	}
	
	@Override
	public boolean equals(Object o) {
		boolean result = false;
		if (o instanceof Key) {
			Key k = (Key) o;
			result = id.equals(k.id) || (filename == null && k.filename == null) || (filename != null && filename.equals(k.filename));
		}
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
	
	@Override
	public String toString() {
		return new StringBuffer(id).append(" : ").append(filename).toString();
	}
}
