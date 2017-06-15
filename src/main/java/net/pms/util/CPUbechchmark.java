package net.pms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  A micro-benchmark that includes a "warmup" step, to ensure
 *  that Hotspot is done compiling methods.
 */

// based on http://www.kdgregory.com/index.php?page=java.microBenchmark

public class CPUbechchmark {
	private static final Logger LOGGER = LoggerFactory.getLogger(CPUbechchmark.class);
	private static final int REPS = 1000000;
	private static final String PART1 = "for a real test, make this big.";
	private static final String PART2 = "so that we can see GC effects.";
	private static final String REGEX = "XXX";
	private static final String REPL = " -- ";
	private static final String TEMPLATE = PART1 + REGEX + PART2;

	/**
	 * Run CPU benchmark only once.
	 * 
	 * @return time the benchmark lasts in millisecond
	 */
	public static long run() throws Exception { // run benchmark only once
		return run(1);
	}

	/**
	 * Run CPU benchmarks with -n repetitions. Each result is stored and
	 * used to calculate the average value.  
	 *
	 * @param repeat how many times the benchmark will be repeated
	 * 
	 * @return the average time in millisecond of all benchmarks were run
	 */
	public static long run(int repeat) throws Exception {
		warmup();
		LOGGER.info("Running CPU benchmark");
		long averageBenchmarkTime = 0;
		for (int i = 0; i < repeat; i++) {
			averageBenchmarkTime = averageBenchmarkTime + execute();
		}

		averageBenchmarkTime = averageBenchmarkTime / repeat;
		LOGGER.trace("Average benchmark elapsed time: {} ms. Benchmark was run {} times", averageBenchmarkTime, repeat);
		return averageBenchmarkTime;
	}

	private static void warmup() {
		LOGGER.trace("Running CPU bechchmark warmup");
		for (int ii = 0 ; ii < 100000 ; ii++) {
			// alternating in this way may be voodoo
			executeReplace(1, TEMPLATE, REGEX, REPL);
		}
	}

	private static long execute() {
		long elapsed = 0;
		long start = System.currentTimeMillis();
		executeReplace(REPS, TEMPLATE, REGEX, REPL);
		elapsed = System.currentTimeMillis() - start;
		LOGGER.trace("Benchmark elapsed time = " + elapsed + " ms.");
		
		return elapsed;
	}

	private static void executeReplace(int reps, String template, String regex, String repl) {
		for (int ii = 0 ; ii < reps ; ii++) {
			String s = template.replaceAll(regex, repl);
		}
	}
}
