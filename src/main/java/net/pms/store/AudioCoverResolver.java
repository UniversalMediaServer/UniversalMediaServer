package net.pms.store;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.pms.PMS;
import net.pms.dlna.DLNAThumbnail;
import net.pms.media.MediaInfo;
import net.pms.parsers.JaudiotaggerParser;
import net.pms.util.CoverSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches the covers of audio files that carry none themselves from Cover Art Archive, after the
 * file has been stored.
 */
public class AudioCoverResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioCoverResolver.class);
	private static final String WORKER_THREAD_NAME = "cover-lookup-worker";
	private static final int QUEUE_CAPACITY = 20000;
	private static final BlockingQueue<File> QUEUE = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
	private static final AtomicBoolean WORKER_RUNNING = new AtomicBoolean(false);

	/**
	 * This class is not meant to be instantiated.
	 */
	private AudioCoverResolver() {
	}

	/**
	 * Queues an audio file whose cover has to be looked up remotely.
	 */
	public static void enqueue(File file) {
		if (file == null || !CoverSupplier.COVER_ART_ARCHIVE.equals(PMS.getConfiguration().getAudioThumbnailMethod())) {
			return;
		}
		if (!QUEUE.offer(file)) {
			LOGGER.trace("Cover lookup queue is full, skipping \"{}\"", file.getName());
			return;
		}
		startWorkerIfNeeded();
	}

	private static void startWorkerIfNeeded() {
		if (WORKER_RUNNING.compareAndSet(false, true)) {
			Thread worker = new Thread(AudioCoverResolver::runWorker, WORKER_THREAD_NAME);
			worker.setDaemon(true);
			worker.setPriority(Thread.MIN_PRIORITY);
			worker.start();
		}
	}

	private static void runWorker() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				File file = QUEUE.take();
				try {
					resolve(file);
				} catch (Exception e) {
					LOGGER.debug("Could not resolve the cover of \"{}\": {}", file.getName(), e.getMessage());
					LOGGER.trace("", e);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			WORKER_RUNNING.set(false);
		}
	}

	private static void resolve(File file) {
		MediaInfo media = new MediaInfo();
		DLNAThumbnail thumbnail = JaudiotaggerParser.getThumbnail(media, file);
		if (thumbnail == null || media.getThumbnailSource() == null) {
			LOGGER.trace("No cover found for \"{}\"", file.getName());
			return;
		}
		if (ThumbnailStore.updateFileThumbnail(file.getAbsolutePath(), thumbnail, media.getThumbnailSource()) != null) {
			LOGGER.debug("Stored the cover of \"{}\" found at {}", file.getName(), media.getThumbnailSource());
		}
	}

}
