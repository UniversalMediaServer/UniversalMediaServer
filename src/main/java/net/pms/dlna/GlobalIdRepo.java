package net.pms.dlna;


import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalIdRepo {
	private int globalId;
	// This map will need to be explicitly expunged 
	private HashMap<Integer, WeakReference<DLNAResource>> map;
	// WeakHashMap is self-expunging
	private WeakHashMap<DLNAResource, Integer> revMap;
	private int ct;
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalIdRepo.class);

	public GlobalIdRepo() {
		globalId = 1;
		map = new HashMap<Integer, WeakReference<DLNAResource>>();
		revMap = new WeakHashMap<DLNAResource, Integer>();
	}

	public synchronized void add(DLNAResource res) {
		int id = globalId++;
		res.setIndexId(id);
		map.put(id, new WeakReference<DLNAResource>(res));
		revMap.put(res, id);
		
		// Unnecessary to expunge every single time
		if (++ct % 100 == 0) {
			expunge();
		}
	}

	public DLNAResource get(int id) {
		WeakReference<DLNAResource> ref = map.get(id);
		if (ref != null) {
			DLNAResource d = ref.get();
			if (d == null) {
				// Object no longer exists
				map.remove(id);
				LOGGER.debug("GlobalIdRepo: expunged id {}", id);
			}
			return d;
		}
		return null;
	}

	public DLNAResource get(String id) {
		try {
			return get(Integer.parseInt(id));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public void remove(int id) {
		LOGGER.debug("GlobalIdRepo: removing by id {}", id);
		revMap.remove(get(id));
		map.remove(id);
	}

	public void remove(DLNAResource res) {
		LOGGER.debug("GlobalIdRepo: removing by object {} - {}", res.getId(), res.getName());
		revMap.remove(res);
		map.remove(res.getId());
	}

	public int getId(DLNAResource d) {
		return revMap.get(d);
	}

	private void expunge() {
		int x = 0, s = map.size();
		gc(); // for debugging only
		for(Iterator<Map.Entry<Integer, WeakReference<DLNAResource>>> it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Integer, WeakReference<DLNAResource>> e = it.next();
			if (e.getValue().get() == null) {
			 	// Object no longer exists
			 	it.remove();
			 	x++;
			}
		}
		LOGGER.debug("GlobalIdRepo: expunged {}/{} ids. revMap size is {}", x, s, revMap.size());
	}

	// Force garbage collection - for debugging only
	public static void gc() {
		Object obj = new Object();
		WeakReference ref = new WeakReference<Object>(obj);
		obj = null;
		while(ref.get() != null) {
			System.gc();
		}
	}
}
