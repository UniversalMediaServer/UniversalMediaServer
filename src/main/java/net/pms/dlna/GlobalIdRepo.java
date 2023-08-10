/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.dlna;

import java.lang.ref.SoftReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalIdRepo {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalIdRepo.class);

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final ArrayList<ID> ids = new ArrayList<>();
	private final ReferenceQueue<MediaResource> idCleanupQueue = new ReferenceQueue<>();

	// Global ids start at 1, since id 0 is reserved as a pseudonym for 'renderer root'
	private int curGlobalId = 1;
	private int deletionsCount = 0;

	public GlobalIdRepo() {
		startIdCleanup();
	}

	public void add(MediaResource dlnaResource) {
		lock.writeLock().lock();
		try {
			if (dlnaResource.getId() == null || get(dlnaResource.getId()) != dlnaResource) {
				ids.add(new ID(dlnaResource, curGlobalId++));
				MediaResource.bumpSystemUpdateId();
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public MediaResource get(String id) {
		return id != null ? get(parseIndex(id)) : null;
	}

	public boolean exists(String id) {
		return get(id) != null;
	}

	public void replace(MediaResource a, MediaResource b) {
		ID item = getItem(parseIndex(a.getId()));
		if (item != null) {
			synchronized (lock) {
				lock.writeLock().lock();
				try {
					item.setRef(b);
					MediaResource.bumpSystemUpdateId();
				} finally {
					lock.writeLock().unlock();
				}
			}
		}
	}

	// Here scope=false means util.DLNAList is telling us the underlying
	// MediaResource has been removed and we should ignore its id, i.e. not
	// share any hard references to it via get(), between now and whenever
	// garbage collection actually happens (or whenever the item is re-added,
	// in the case of items that are just being moved).

	public void setScope(MediaResource dlnaResource, boolean scope) {
		lock.writeLock().lock();
		try {
			ID item = getItem(parseIndex(dlnaResource.getId()));
			if (item != null) {
				LOGGER.debug("GlobalIdRepo: marking id {} {} scope", item.id, scope ? "in" : "out of");
				item.scope = scope;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void clear() {
		lock.writeLock().lock();
		try {
			ids.clear();
			curGlobalId = 1;
			deletionsCount = 0;
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void delete(int index) {
		lock.writeLock().lock();
		try {
			if (index > -1 && index < ids.size()) {
				ids.remove(index);
				MediaResource.bumpSystemUpdateId();
				deletionsCount++;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	private MediaResource get(int id) {
		ID item = getItem(id);
		if (item != null && !item.scope) {
			LOGGER.debug("GlobalIdRepo: id {} is not in scope, returning null", id);
			return null;
		}
		return item != null ? item.dlnaRef.get() : null;
	}

	private ID getItem(int id) {
		lock.readLock().lock();
		try {
			if (id > 0) {
				int index = indexOf(id);
				if (index > -1) {
					return ids.get(index);
				}
			}
			return null;
		} finally {
			lock.readLock().unlock();
		}
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

	// id cleanup
	private void startIdCleanup() {
		new Thread(() -> {
			while (true) {
				try {
					// Once an underlying MediaResource is ready for garbage
					// collection, its weak reference will pop out here
					SoftDLNARef ref = (SoftDLNARef) idCleanupQueue.remove();
					if (ref.id > 0) {
						// Delete the associated id from our repo list
						LOGGER.debug("deleting invalid id {}", ref.id);
						delete(indexOf(ref.id));
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}, "GlobalId cleanup").start();
	}

	private static int parseIndex(String id) {
		try {
			// Id strings may have optional tags beginning with $ appended, e.g. '1234$Temp'
			return Integer.parseInt(StringUtils.substringBefore(id, "$"));
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private class SoftDLNARef extends SoftReference<MediaResource> {
		int id;

		SoftDLNARef(MediaResource dlnaResource, int id) {
			super(dlnaResource, idCleanupQueue);
			this.id = id;
		}

		void cancel() {
			// We've been replaced, i.e. another weakDLNARef is now holding our
			// id, and it will trigger id cleanup at garbage collection time
			id = -1;
		}
	}

	private class ID {
		int id;
		boolean scope;
		SoftDLNARef dlnaRef;

		private ID(MediaResource dlnaResource, int id) {
			this.id = id;
			setRef(dlnaResource);
			scope = true;
		}

		final void setRef(MediaResource dlnaResource) {
			if (dlnaRef != null) {
				dlnaRef.cancel();
			}
			dlnaRef = new SoftDLNARef(dlnaResource, id);
			dlnaResource.setIndexId(id);
		}
	}

}
