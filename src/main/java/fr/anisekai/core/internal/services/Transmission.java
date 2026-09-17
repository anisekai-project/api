package fr.anisekai.core.internal.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Basic client to interact with a Transmission BitTorrent daemon using its RPC interface.
 *
 * <p><b>Note:</b> This is a minimal implementation tailored for the Anisekai project.
 * For more advanced usage and features, consider using a dedicated Transmission client library.</p>
 */
public class Transmission {

    /**
     * Default set of torrent fields requested when querying Transmission. These fields represent common torrent
     * metadata such as ID, name, status, download directory, progress, and files.
     */
    public static final List<String> DEFAULT_TORRENT_FIELDS = Arrays.asList(
            "hashString",
            "name",
            "status",
            "downloadDir",
            "percentDone",
            "files"
    );
    private static final String SESSION_HEADER = "X-Transmission-Session-Id";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
                                                        .connectTimeout(Duration.ofSeconds(30))
                                                        .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String endpoint;
    private volatile String sessionId = null;

    /**
     * Create a Transmission client targeting the specified RPC endpoint.
     *
     * @param endpoint
     *         The Transmission RPC URL
     */
    public Transmission(String endpoint) {

        this.endpoint = endpoint;
    }

    /**
     * Retrieve the RPC Endpoint for the transmission daemon server.
     *
     * @return A URL
     */
    public String getEndpoint() {

        return this.endpoint;
    }

    private ObjectNode sendPacket(ObjectNode data) throws Exception {

        String body = data.toString();
        for (int attempt = 0; attempt < 2; attempt++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(this.endpoint))
                                                     .timeout(Duration.ofSeconds(30))
                                                     .header("Content-Type", "application/json")
                                                     .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            String currentSession = this.sessionId;
            if (currentSession != null) {
                request.header(SESSION_HEADER, currentSession);
            }

            HttpResponse<byte[]> response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 409 && attempt == 0) {
                String renewedSession = response.headers().firstValue(SESSION_HEADER).orElse(null);
                if (renewedSession != null && !renewedSession.isBlank()) {
                    this.sessionId = renewedSession;
                    continue;
                }
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw httpError(response);
            }
            return (ObjectNode) OBJECT_MAPPER.readTree(response.body());
        }
        throw new IllegalStateException("Could not authenticate to Transmission RPC API.");
    }

    private static RuntimeException httpError(HttpResponse<byte[]> response) {

        HttpStatus statusCode = HttpStatus.valueOf(response.statusCode());
        if (statusCode.is4xxClientError()) {
            return new HttpClientErrorException(statusCode, "", httpHeaders(response), response.body(), StandardCharsets.UTF_8);
        }
        return new HttpServerErrorException(statusCode, "", httpHeaders(response), response.body(), StandardCharsets.UTF_8);
    }

    private static org.springframework.http.HttpHeaders httpHeaders(HttpResponse<byte[]> response) {

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        response.headers().map().forEach((key, values) -> headers.addAll(key, values));
        return headers;
    }

    /**
     * Refresh, if necessary, the session to the remote transmission daemon server.
     *
     * @throws Exception
     *         Thrown if the query to the server fails.
     */
    public void getSession() throws Exception {

        ObjectNode packetData = OBJECT_MAPPER.createObjectNode();
        packetData.put("method", "session-get");

        ObjectNode response = this.sendPacket(packetData);
        if (!response.get("result").asText().equals("success")) {
            throw new IllegalStateException("Transmission client failed to retrieve session");
        }
    }

    /**
     * Retrieve a {@link Set} of {@link Torrent} from the remote transmission daemon server.
     *
     * @param hashes
     *         List of {@link Torrent} hashes to query. If empty, all torrents will be retrieved.
     *
     * @return A {@link Collection} of {@link Torrent}.
     *
     * @throws Exception
     *         Thrown if the query to the server fails.
     * @throws IllegalStateException
     *         Thrown if the response indicate a failure or if the response was not parsable.
     */
    public List<Torrent> query(Collection<String> hashes) throws Exception {

        ObjectNode packetData = OBJECT_MAPPER.createObjectNode();
        packetData.put("method", "torrent-get");
        ObjectNode requestArguments = OBJECT_MAPPER.createObjectNode();
        requestArguments.set("fields", OBJECT_MAPPER.valueToTree(DEFAULT_TORRENT_FIELDS));
        packetData.set("arguments", requestArguments);

        if (!hashes.isEmpty()) {
            requestArguments.set("ids", OBJECT_MAPPER.valueToTree(hashes));
        }

        ObjectNode response = this.sendPacket(packetData);
        String       status   = response.get("result").asText();

        if (!status.equals("success")) {
            throw new IllegalStateException("Transmission failed to query torrents: Response was " + status);
        }

        ObjectNode    arguments  = (ObjectNode) response.get("arguments");
        ArrayNode     torrents   = (ArrayNode) arguments.get("torrents");
        List<Torrent> torrentSet = new ArrayList<>();

        for (int i = 0; i < torrents.size(); i++) {
            torrentSet.add(Torrent.of((ObjectNode) torrents.get(i)));
        }
        return torrentSet;
    }

    /**
     * Retrieve a single {@link Torrent} from the remote transmission daemon server.
     *
     * @param hash
     *         {@link Torrent} hash to query.
     *
     * @return A {@link Torrent}.
     *
     * @throws Exception
     *         Thrown if the query to the server fails.
     * @throws IllegalStateException
     *         Thrown if the response indicate a failure or if the response was not parsable.
     * @throws IllegalArgumentException
     *         Thrown if the transmission daemon response did not include the requested {@link Torrent}
     */
    public Torrent query(String hash) throws Exception {

        List<Torrent> query = this.query(Collections.singleton(hash));
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Torrent with hash " + hash + " not found");
        }
        return query.getFirst();
    }

    /**
     * Send the provided {@link Nyaa.Entry} to the transmission daemon server.
     *
     * @param entry
     *         The {@link Nyaa.Entry} to download.
     * @param paused
     *         Define if the download should not start immediately.
     *
     * @return The added {@link Torrent} matching the provided {@link Nyaa.Entry}.
     *
     * @throws Exception
     *         Thrown if the query to the server fails.
     * @throws IllegalStateException
     *         Thrown if the response indicate a failure or if the response was not parsable.
     */
    public Torrent download(Nyaa.Entry entry, boolean paused) throws Exception {

        ObjectNode packetData = OBJECT_MAPPER.createObjectNode();
        packetData.put("method", "torrent-add");
        ObjectNode requestArguments = OBJECT_MAPPER.createObjectNode();
        requestArguments.put("paused", paused);
        requestArguments.put("filename", entry.torrent());
        packetData.set("arguments", requestArguments);

        ObjectNode response = this.sendPacket(packetData);
        String     result   = response.get("result").asText();

        if (!result.equals("success")) {
            throw new IllegalStateException("Transmission client failed to queue torrent");
        }

        ObjectNode arguments = (ObjectNode) response.get("arguments");
        ObjectNode json;

        if (arguments.has("torrent-duplicate")) {
            json = (ObjectNode) arguments.get("torrent-duplicate");
        } else if (arguments.has("torrent-added")) {
            json = (ObjectNode) arguments.get("torrent-added");
        } else {
            throw new IllegalStateException("Transmission client failed to read server response.");
        }

        String hash = json.get("hashString").asText();
        return this.query(hash);
    }

    /**
     * Starts the provided {@link Torrent} when it has been added with the pause flag.
     *
     * @param torrent
     *         The {@link Torrent} to start.
     *
     * @return The refreshed {@link Torrent}.
     *
     * @throws Exception
     *         Thrown if the query to the server fails.
     * @throws IllegalStateException
     *         Thrown if the response indicate a failure or if the response was not parsable.
     */
    public Torrent start(Torrent torrent) throws Exception {

        ObjectNode packetData = OBJECT_MAPPER.createObjectNode();
        packetData.put("method", "torrent-start");
        ObjectNode requestArguments = OBJECT_MAPPER.createObjectNode();
        requestArguments.set("ids", OBJECT_MAPPER.valueToTree(Collections.singleton(torrent.hash)));
        packetData.set("arguments", requestArguments);

        ObjectNode response = this.sendPacket(packetData);
        String     result   = response.get("result").asText();

        if (!result.equals("success")) {
            throw new IllegalStateException("Transmission client failed to start torrent");
        }

        return this.query(torrent.hash);
    }

    /**
     * Delete the provided {@link Torrent}.
     *
     * @param torrent
     *         The {@link Torrent} to delete.
     *
     * @throws Exception
     *         Thrown if the query to the server fails.
     * @throws IllegalStateException
     *         Thrown if the response indicate a failure or if the response was not parsable.
     */
    public void delete(Torrent torrent) throws Exception {

        ObjectNode packetData = OBJECT_MAPPER.createObjectNode();
        packetData.put("method", "torrent-remove");
        ObjectNode requestArguments = OBJECT_MAPPER.createObjectNode();
        requestArguments.set("ids", OBJECT_MAPPER.valueToTree(Collections.singleton(torrent.hash)));
        packetData.set("arguments", requestArguments);
        requestArguments.put("delete-local-data", true);

        ObjectNode response = this.sendPacket(packetData);
        String     result   = response.get("result").asText();

        if (!result.equals("success")) {
            throw new IllegalStateException("Transmission client failed to delete torrent");
        }
    }

    /**
     * Current status of a torrent in Transmission.
     */
    public enum TorrentStatus {

        /**
         * Unknown status — the torrent's state could not be determined.
         */
        UNKNOWN(-1, false),

        /**
         * Torrent is stopped and not actively downloading or seeding.
         */
        STOPPED(0, false),

        /**
         * Torrent verification is queued but not yet started.
         */
        VERIFY_QUEUED(1, false),

        /**
         * Torrent is currently verifying existing data.
         */
        VERIFYING(2, false),

        /**
         * Torrent is queued and waiting to start downloading.
         */
        DOWNLOAD_QUEUED(3, false),

        /**
         * Torrent is actively downloading data.
         */
        DOWNLOADING(4, false),

        /**
         * Torrent is queued and waiting to start seeding.
         */
        SEED_QUEUED(5, true),

        /**
         * Torrent is actively seeding (uploading to peers).
         */
        SEEDING(6, true);

        private final int     id;
        private final boolean finished;

        TorrentStatus(int id, boolean finished) {

            this.id       = id;
            this.finished = finished;
        }

        /**
         * Converts a numeric status code to its corresponding {@link TorrentStatus} enum constant.
         *
         * @param status
         *         The numeric status code from Transmission.
         *
         * @return The matching {@link TorrentStatus}, or {@link #UNKNOWN} if no match is found.
         */
        public static TorrentStatus from(int status) {

            for (TorrentStatus value : values()) {
                if (value.id == status) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        /**
         * Indicates whether this torrent status represents a finished state.
         *
         * @return {@code true} if the torrent is finished (seeding or completed), {@code false} otherwise.
         */
        public boolean isFinished() {

            return this.finished;
        }

    }

    /**
     * Represents a Transmission torrent with basic metadata.
     *
     * @param hash
     *         The {@link Torrent}'s hash.
     * @param status
     *         The {@link Torrent}'s {@link TorrentStatus}.
     * @param downloadDir
     *         The {@link Torrent}'s download directory
     * @param percentDone
     *         The {@link Torrent}'s download progress (0 to 1)
     * @param files
     *         The {@link Torrent}'s file names.
     */
    public record Torrent(
            String hash,
            TorrentStatus status,
            String downloadDir,
            double percentDone,
            List<String> files
    ) {

        /**
         * Creates a {@link Torrent} instance from an {@link ObjectNode} object representing a Transmission torrent.
         *
         * @param json
         *         The JSON object containing torrent information, expected to have keys: "hashString", "status",
         *         "downloadDir", "percentDone", and "files.0.name".
         *
         * @return A new {@link Torrent} instance populated with data parsed from the given JSON.
         */
        public static Torrent of(ObjectNode json) {

            String        hash        = json.get("hashString").asText();
            TorrentStatus status      = TorrentStatus.from(json.get("status").asInt());
            String        downloadDir = json.get("downloadDir").asText();
            double        percentDone = json.get("percentDone").asDouble();
            ArrayNode     rawFiles    = (ArrayNode) json.get("files");
            List<String>  files       = new ArrayList<>();
            for (int i = 0; i < rawFiles.size(); i++) {
                files.add(rawFiles.get(i).get("name").asText());
            }

            return new Torrent(hash, status, downloadDir, percentDone, files);
        }

    }

}
