package net.pms.encoders;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class YoutubeDlTest {

    @Test
    public void testExtractYoutubeVideoId_standardUrl() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://www.youtube.com/watch?v=abc123XYZ";
        assertEquals("abc123XYZ", dl.extractYoutubeVideoId(url));
    }

    @Test
    public void testExtractYoutubeVideoId_withAdditionalParams() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://www.youtube.com/watch?v=abc123XYZ&list=PLxyz";
        assertEquals("abc123XYZ", dl.extractYoutubeVideoId(url));
    }

    @Test
    public void testExtractYoutubeVideoId_shortenedUrl() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://youtu.be/abc123XYZ";
        assertEquals("abc123XYZ", dl.extractYoutubeVideoId(url));
    }

    @Test
    public void testExtractYoutubeVideoId_embedUrl() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://www.youtube.com/embed/abc123XYZ";
        assertEquals("abc123XYZ", dl.extractYoutubeVideoId(url));
    }

    @Test
    public void testExtractYoutubeVideoId_nullUrl() {
        YoutubeDl dl = new YoutubeDl();
        assertNull(dl.extractYoutubeVideoId(null));
    }

    @Test
    public void testExtractYoutubeVideoId_invalidUrl() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://www.example.com";
        assertNull(dl.extractYoutubeVideoId(url));
    }

    @Test
    public void testExtractYoutubeVideoId_nonYouTubeUrl() {
        YoutubeDl dl = new YoutubeDl();
        String url = "https://vimeo.com/12345678";
        assertNull(dl.extractYoutubeVideoId(url));
    }
}
