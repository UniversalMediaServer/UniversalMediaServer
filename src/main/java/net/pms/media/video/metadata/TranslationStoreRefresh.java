package net.pms.media.video.metadata;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.store.MediaStoreIds;

/**
 * Bumps the store id of a resource and of its containers after a background
 * translation changed its title.
 */
final class TranslationStoreRefresh {

	/** Either a file, or a resource already known by its store name. */
	record Target(Long fileId, String systemName) {
	}

	private static final Set<Target> PENDING = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean SCHEDULED = new AtomicBoolean();

	private TranslationStoreRefresh() {
	}

	static void request(Target target, Executor executor) {
		if (target == null || (target.fileId() == null && target.systemName() == null)) {
			return;
		}
		PENDING.add(target);
		if (SCHEDULED.compareAndSet(false, true)) {
			try {
				executor.execute(TranslationStoreRefresh::flush);
			} catch (RejectedExecutionException e) {
				flush();
			}
		}
	}

	private static void flush() {
		SCHEDULED.set(false);
		List<Target> targets = new ArrayList<>(PENDING);
		if (targets.isEmpty()) {
			return;
		}
		PENDING.removeAll(targets);
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection == null) {
				// without a database the client keeps the untranslated title until its next browse.
				return;
			}
			Set<String> names = new LinkedHashSet<>();
			for (Target target : targets) {
				String name = target.systemName() != null ?
					target.systemName() :
					MediaTableFiles.getFilenameById(connection, target.fileId());
				if (name != null) {
					names.add(name);
				}
			}
			for (String name : names) {
				MediaStoreIds.incrementUpdateIdForFilenameWithAncestors(connection, name);
			}
		} finally {
			MediaDatabase.close(connection);
		}
	}
}
