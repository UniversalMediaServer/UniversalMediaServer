package net.pms.dlna;


import java.util.HashMap;

public class GlobalIdRepo {
	private int globalId;
	private HashMap<Integer, DLNAResource> map;
	private HashMap<DLNAResource, Integer> revMap;

	public GlobalIdRepo() {
		globalId = 1;
		map = new HashMap<>();
		revMap = new HashMap<>();
	}

	public synchronized void add(DLNAResource res) {
		int id = globalId++;
		res.setIndexId(id);
		map.put(id, res);
		revMap.put(res, id);
	}

	public DLNAResource get(int id) {
		return map.get(id);
	}

	public DLNAResource get(String id) {
		try {
			return get(Integer.parseInt(id));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public void remove(int id) {
		revMap.remove(get(id));
		map.remove(id);
	}

	public void remove(DLNAResource res) {
		revMap.remove(res);
		map.remove(res.getId());
	}
}
