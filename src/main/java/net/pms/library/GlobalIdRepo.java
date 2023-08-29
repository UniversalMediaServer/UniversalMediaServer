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
package net.pms.library;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.UmsContentDirectoryService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalIdRepo {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalIdRepo.class);

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final ArrayList<ID> ids = new ArrayList<>();
	private final ReferenceQueue<LibraryResource> idCleanupQueue = new ReferenceQueue<>();

	// Global ids start at 1, since id 0 is reserved as a pseudonym for 'renderer root'
	private int curGlobalId = 1;
	private int deletionsCount = 0;

	public GlobalIdRepo() {
		startIdCleanup();
	}

	public void add(LibraryResource resource) {
		lock.writeLock().lock();
		try {
			if (resource.getId() == null || get(resource.getId()) != resource) {
				ids.add(new ID(resource, curGlobalId++));
				UmsContentDirectoryService.bumpSystemUpdateId();
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public LibraryResource get(String id) {
		return id != null ? get(parseIndex(id)) : null;
	}

	public boolean exists(String id) {
		return get(id) != null;
	}

	public void replace(LibraryResource a, LibraryResource b) {
		ID item = getItem(parseIndex(a.getId()));
		if (item != null) {
			synchronized (lock) {
				lock.writeLock().lock();
				try {
					item.setRef(b);
					UmsContentDirectoryService.bumpSystemUpdateId();
				} finally {
					lock.writeLock().unlock();
				}
			}
		}
	}

	public void delete(LibraryResource a) {
		int id = parseIndex(a.getId());
		if (id > 0) {
			int index = indexOf(id);
			if (index > -1) {
				delete(index);
			}
		}
	}

	// Here scope=false means util.DLNAList is telling us the underlying
	// LibraryResource has been removed and we should ignore its id, i.e. not
	// share any hard references to it via get(), between now and whenever
	// garbage collection actually happens (or whenever the item is re-added,
	// in the case of items that are just being moved).

	public void setScope(LibraryResource resource, boolean scope) {
		lock.writeLock().lock();
		try {
			ID item = getItem(parseIndex(resource.getId()));
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
				UmsContentDirectoryService.bumpSystemUpdateId();
				deletionsCount++;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	private LibraryResource get(int id) {
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
					// Once an underlying LibraryResource is ready for garbage
					// collection, its weak reference will pop out here
					SoftMediaResourceRef ref = (SoftMediaResourceRef) idCleanupQueue.remove();
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

	private class SoftMediaResourceRef extends SoftReference<LibraryResource> {
		int id;

		SoftMediaResourceRef(LibraryResource resource, int id) {
			super(resource, idCleanupQueue);
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
		SoftMediaResourceRef dlnaRef;

		private ID(LibraryResource resource, int id) {
			this.id = id;
			setRef(resource);
			scope = true;
		}

		final void setRef(LibraryResource resource) {
			if (dlnaRef != null) {
				dlnaRef.cancel();
			}
			dlnaRef = new SoftMediaResourceRef(resource, id);
			resource.setIndexId(id);
		}
	}

}
