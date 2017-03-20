package net.pms.dlna;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalIdRepo {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalIdRepo.class);

	// Global ids start at 1, since id 0 is reserved as a pseudonym for 'renderer root'
	private int curGlobalId = 1, deletionsCount = 0;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final ArrayList<ID> ids = new ArrayList<>();

	private static class ID {
		final int id;
		final DLNAResource dlnaResource;

		private ID(DLNAResource dlnaResource, int id) {
			this.id = id;
			this.dlnaResource = dlnaResource;
			dlnaResource.setIndexId(id);
		}
	}

	public GlobalIdRepo() {
	}

	public void add(DLNAResource dlnaResource) {
		lock.writeLock().lock();
		try {
			String id = dlnaResource.getId();
			if (id != null) {
				remove(id);
			}
			ids.add(new ID(dlnaResource, curGlobalId++));
		} finally {
			lock.writeLock().unlock();
		}
	}

	public DLNAResource get(String id) {
		return get(parseIndex(id));
	}

	public DLNAResource get(int id) {
		lock.readLock().lock();
		try {
			int index = indexOf(id);
			return index > -1 ? ids.get(index).dlnaResource : null;
		} finally {
			lock.readLock().unlock();
		}
	}

	public void remove(DLNAResource d) {
		remove(d.getId());
	}

	public void remove(String id) {
		remove(parseIndex(id));
	}

	public void remove(int id) {
		lock.writeLock().lock();
		try {
			int index = indexOf(id);
			if (index > -1) {
				LOGGER.debug("GlobalIdRepo: removing id {} - {}", id, ids.get(index).dlnaResource.getName());
				ids.remove(index);
				deletionsCount++;
			}
		} finally {
			lock.writeLock().unlock();
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

	private int indexOf(int id) {
		lock.readLock().lock();
		try {
			if (id > 0 && id < curGlobalId) {
				// We're in sequence by definition, so binary search is quickest

				// Exclude any areas where the id can't possibly be
				int ceil = ids.size() - 1;
				int top = id - 1; // id 0 is reserved, so index is id-1 at most
				int hi = top < ceil ? top : ceil;
				int floor = hi - deletionsCount;
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
		} finally {
			lock.readLock().unlock();
		}
		LOGGER.debug("GlobalIdRepo: id not found: {}", id);
		return -1;
	}
}
