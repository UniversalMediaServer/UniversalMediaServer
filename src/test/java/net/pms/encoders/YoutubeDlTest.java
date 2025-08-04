package net.pms.encoders;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class YoutubeDlTest {

    @Test
    public void testExtractVideoId_standardUrl() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://www.youtube.com/watch?v=abc123XYZ";
        assertEquals("abc123XYZ", dl.extractVideoId(url));
    }

    @Test
    public void testExtractVideoId_withAdditionalParams() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://www.youtube.com/watch?v=abc123XYZ&list=PLxyz";
        assertEquals("abc123XYZ", dl.extractVideoId(url));
    }

    @Test
    public void testExtractVideoId_shortenedUrl() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://youtu.be/abc123XYZ";
        assertEquals("abc123XYZ", dl.extractVideoId(url));
    }

    @Test
    public void testExtractVideoId_embedUrl() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://www.youtube.com/embed/abc123XYZ";
        assertEquals("abc123XYZ", dl.extractVideoId(url));
    }

    @Test
    public void testExtractVideoId_nullUrl() {
        YoutubeDl dl = new YoutubeDl();
        assertNull(dl.extractVideoId(null));
    }

    @Test
    public void testExtractVideoId_invalidUrl() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://www.example.com";
        assertNull(dl.extractVideoId(url));
    }
}
