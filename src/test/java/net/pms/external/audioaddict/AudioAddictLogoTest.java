package net.pms.external.audioaddict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AudioAddictLogoTest {

	@Test
	void refreshesLogoFromMetadataAndKeepsLastImageOnFailure() throws IOException {
		AtomicLong clock = new AtomicLong();
		AtomicInteger metadataRequests = new AtomicInteger();
		AtomicInteger version = new AtomicInteger();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/metadata", exchange -> {
			metadataRequests.incrementAndGet();
			String html = "<meta property='og:image' content='/unused.png'>" +
				"<meta property='og:image:secure_url' content='/logo-" + version.get() + ".png'>";
			send(exchange, 404, "text/html", html.getBytes(StandardCharsets.UTF_8));
		});
		server.createContext("/logo-1.png", exchange -> send(exchange, 200, "image/png", image(0x005500)));
		server.createContext("/logo-2.png", exchange -> send(exchange, 200, "image/png", image(0x555555)));
		server.start();
		try {
			URI metadataUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/metadata");
			AudioAddictLogo logo = new AudioAddictLogo(metadataUri, metadataUri.getHost(), clock::get);

			assertNull(logo.get());
			version.set(1);
			clock.addAndGet(Duration.ofMinutes(5).toMillis());
			String firstTag = logo.get().etag();
			assertEquals(firstTag, logo.get().etag());
			assertEquals(2, metadataRequests.get());

			version.set(3);
			clock.addAndGet(Duration.ofHours(23).toMillis());
			assertEquals(firstTag, logo.get().etag());
			assertEquals(2, metadataRequests.get());

			clock.addAndGet(Duration.ofHours(1).toMillis());
			assertEquals(firstTag, logo.get().etag());
			assertEquals(firstTag, logo.get().etag());
			assertEquals(3, metadataRequests.get());

			version.set(2);
			clock.addAndGet(Duration.ofMinutes(5).toMillis());
			assertNotEquals(firstTag, logo.get().etag());
			assertEquals(4, metadataRequests.get());
		} finally {
			server.stop(0);
		}
	}

	private static byte[] image(int color) throws IOException {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, color);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);
		return output.toByteArray();
	}

	private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
		try (exchange) {
			exchange.getResponseHeaders().set("Content-Type", type);
			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
		}
	}
}
