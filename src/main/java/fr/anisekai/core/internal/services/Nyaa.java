package fr.anisekai.core.internal.services;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for fetching and parsing RSS feeds from Nyaa.si or any other compatible backend.
 */
public final class Nyaa {

    private Nyaa() {}

    /**
     * Represents a single RSS entry from the Nyaa feed.
     *
     * @param title
     *         The title of the torrent entry.
     * @param link
     *         A unique link (often a GUID or URI).
     * @param torrent
     *         The direct torrent download link.
     * @param hash
     *         The torrent's info hash (can be {@code null} if not present).
     */
    public record Entry(String title, String link, String torrent, String hash) {

    }

    /**
     * Fetches and parses an RSS feed from the given URI.
     *
     * @param uri
     *         The URI of the RSS feed.
     *
     * @return A list of parsed {@link Entry} objects.
     *
     * @throws IOException
     *         If an I/O error occurs during retrieval.
     * @throws FeedException
     *         If the feed is malformed or cannot be parsed.
     * @throws InterruptedException
     *         If the operation is interrupted
     */
    public static List<Entry> fetch(URI uri) throws IOException, InterruptedException, FeedException {

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest               request  = HttpRequest.newBuilder(uri).build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed      feed  = input.build(new XmlReader(response.body()));

            return toNyaaEntries(feed);
        }
    }

    private static @NotNull List<Entry> toNyaaEntries(SyndFeed feed) {

        List<Entry> items = new ArrayList<>();

        for (SyndEntry entry : feed.getEntries()) {

            String title   = entry.getTitle();
            String link    = entry.getUri();
            String torrent = entry.getLink();
            String hash    = null;

            for (Element tag : (Iterable<Element>) entry.getForeignMarkup()) {
                if (tag.getName().equals("infoHash")) {
                    hash = tag.getText();
                }
            }

            items.add(new Entry(title, link, torrent, hash));
        }
        return items;
    }

}
