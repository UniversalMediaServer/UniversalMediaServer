package net.pms.media.video.metadata;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LazyVideoJsonTest {
	@BeforeAll
	public static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void defersParsingAndPreservesValues() throws Exception {
		for (Class<?> type : new Class<?>[]{MediaVideoMetadata.class, TvSeriesMetadata.class}) {
			for (String field : new String[]{"Credits", "Images"}) {
				Object metadata = type.getConstructor().newInstance();
				String json = field.equals("Credits") ? "[{\"cast\":[],\"crew\":[]}]" : "[{\"posters\":[],\"backdrops\":[]}]";
				type.getMethod("set" + field, String.class).invoke(metadata, json);
				var stored = type.getDeclaredField(field.toLowerCase());
				stored.setAccessible(true);
				assertNull(stored.get(metadata), "Setting JSON must not parse it");
				Object value = type.getMethod("get" + field).invoke(metadata);
				Class<?> valueType = field.equals("Credits") ? ApiCredits.class : ApiImages.class;
				Gson gson = new Gson();
				assertEquals(gson.toJsonTree(gson.fromJson(json, valueType)), gson.toJsonTree(value));
				assertSame(value, type.getMethod("get" + field).invoke(metadata));
			}
		}
	}

	@Test
	public void replacementNullAndMalformedJsonAreHandled() throws Exception {
		for (Class<?> type : new Class<?>[]{MediaVideoMetadata.class, TvSeriesMetadata.class}) {
			for (String field : new String[]{"Credits", "Images"}) {
				Object metadata = type.getConstructor().newInstance();
				var set = type.getMethod("set" + field, String.class);
				var get = type.getMethod("get" + field);
				set.invoke(metadata, "[]");
				Object first = get.invoke(metadata);
				set.invoke(metadata, "[]");
				assertNotSame(first, get.invoke(metadata));
				set.invoke(metadata, "{broken");
				Object replacement = field.equals("Credits") ? new ApiCredits() : new ApiImages();
				type.getMethod("set" + field, replacement.getClass()).invoke(metadata, replacement);
				assertSame(replacement, get.invoke(metadata));
				set.invoke(metadata, "{broken");
				assertNull(get.invoke(metadata));
				assertNull(get.invoke(metadata));
				set.invoke(metadata, new Object[]{null});
				assertNull(get.invoke(metadata));
				set.invoke(metadata, "null");
				assertNull(get.invoke(metadata));
			}
		}
	}

	@Test
	public void detailSerializationIncludesDeferredImages() {
		MediaVideoMetadata movie = new MediaVideoMetadata();
		movie.setImages("[{\"posters\":[]}]");
		var movieDetail = movie.asJsonObject("en-us");
		assertEquals(new Gson().toJsonTree(movie.getImages()), movieDetail.get("images"));
		TvSeriesMetadata series = new TvSeriesMetadata();
		series.setImages("[{\"backdrops\":[]}]");
		var detail = series.asJsonObject("en-us");
		assertEquals(new Gson().toJsonTree(series.getImages()), detail.get("images"));
	}

	@Test
	public void concurrentReadersShareParsedObject() throws Exception {
		var pool = Executors.newFixedThreadPool(4);
		try {
			for (Class<?> type : new Class<?>[]{MediaVideoMetadata.class, TvSeriesMetadata.class}) {
				for (String field : new String[]{"Credits", "Images"}) {
					Object metadata = type.getConstructor().newInstance();
					type.getMethod("set" + field, String.class).invoke(metadata, "[]");
					var get = type.getMethod("get" + field);
					var results = new ArrayList<java.util.concurrent.Future<Object>>();
					for (int i = 0; i < 20; i++) {
						results.add(pool.submit(() -> get.invoke(metadata)));
					}
					Object first = results.get(0).get(5, TimeUnit.SECONDS);
					for (var result : results) {
						assertSame(first, result.get(5, TimeUnit.SECONDS));
					}
				}
			}
		} finally {
			pool.shutdownNow();
		}
	}
}
