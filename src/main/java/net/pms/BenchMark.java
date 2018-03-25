package net.pms;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.pms.util.ImdbUtil;


public class BenchMark {

	private BenchMark() {
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {

		// ************** Set benchmark parameters **************

		File folder = new File("D:\\Movies"); // Set folder here
		/*
		 * Method:
		 * 1 = ImdbUtil.computeHashFileChannel(File)
		 * 2 = ImdbUtil.computeHashFileChannel(Path)
		 * 3 = ImdbUtil.computeHashInputStream(file)
		 * 4 = ImdbUtil.computeHashRandomAccessFile(file)
		 */
		final int method = 1; // Set method here
		int numThreads = 1; // Set number of threads here
		int multiply = 1000; // Set the number of times to repeat all files here

		// ******************************************************

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);

		File[] files = folder.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isFile();
			}
		});

		System.out.println("Starting file hash benchmark for " + files.length * multiply + " files using " + numThreads + " threads");
		long start;
		if (method == 2) {
			Path[] paths = new Path[files.length];
			for (int i = 0; i < files.length; i++) {
				paths[i] = files[i].toPath();
			}
			start = System.nanoTime();
			for (int i = 0; i < multiply; i++) {
				for (Path path : paths) {
					final Path finalPath = path;
					Runnable runnable = new Runnable() {

						@Override
						public void run() {
							try {
								String hash = ImdbUtil.computeHashFileChannel(finalPath);
								System.out.println("Calculated hash " + hash + " for " + finalPath);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					};
					executor.submit(runnable);
				}
			}
		} else {
			start = System.nanoTime();
			for (int i = 0; i < multiply; i++) {
				for (File file : files) {
					final File finalFile = file;
					Runnable runnable = new Runnable() {

						@Override
						public void run() {
							try {
								String hash = null;
								switch (method) {
									case 1:
										hash = ImdbUtil.computeHashFileChannel(finalFile);
										break;
									case 3:
										hash = ImdbUtil.computeHashInputStream(finalFile);
										break;
									case 4:
										hash = ImdbUtil.computeHashRandomAccessFile(finalFile);
										break;
								}
								System.out.println("Calculated hash " + hash + " for " + finalFile);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					};
					executor.submit(runnable);
				}
			}
		}
		try {
			executor.shutdown();
			executor.awaitTermination(100, TimeUnit.DAYS);
			long duration = System.nanoTime() - start;
			System.out.println(
				"Benchmarking of hashing " + files.length * multiply + " files using " + numThreads +
				" threads took " + duration / 1000000 + " ms (" + duration / (files.length * multiply) + " ns average per file)");
		} catch (InterruptedException e) {
			System.err.println("Benchmark interrupted");
		}
	}

}
