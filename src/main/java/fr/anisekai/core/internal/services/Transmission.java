package fr.anisekai.core.internal.services;

import fr.alexpado.lib.rest.RestAction;
import fr.alexpado.lib.rest.exceptions.RestException;
import fr.alexpado.lib.rest.interfaces.IRestAction;
import fr.anisekai.core.internal.json.AnisekaiArray;
import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.services.packets.TransmissionAuthPacket;
import fr.anisekai.core.internal.services.packets.TransmissionCustomPacket;

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
         * Indicates whether this torrent status represents a finished state.
         *
         * @return {@code true} if the torrent is finished (seeding or completed), {@code false} otherwise.
         */
        public boolean isFinished() {

            return this.finished;
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
         * Creates a {@link Torrent} instance from an {@link AnisekaiJson} object representing a Transmission torrent.
         *
         * @param json
         *         The JSON object containing torrent information, expected to have keys: "hashString", "status",
         *         "downloadDir", "percentDone", and "files.0.name".
         *
         * @return A new {@link Torrent} instance populated with data parsed from the given JSON.
         */
        public static Torrent of(AnisekaiJson json) {

            String        hash        = json.getString("hashString");
            TorrentStatus status = TorrentStatus.from(json.getInt("status"));
            String        downloadDir = json.getString("downloadDir");
            double        percentDone = json.getDouble("percentDone");
            List<String>  files       = json.readArray("files").map(rawFile -> rawFile.getString("name"));

            return new Torrent(hash, status, downloadDir, percentDone, files);
        }

    }

    private final String endpoint;
    private       String sessionId = null;

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

    /**
     * Send the provided {@link RestAction} toward the transmission daemon RPC API.
     *
     * @param action
     *         The {@link RestAction} to execute.
     *
     * @return The query result.
     */
    private AnisekaiJson send(IRestAction<AnisekaiJson> action) throws Exception {

        if (this.sessionId == null) {
            try {
                new TransmissionAuthPacket(this.endpoint).complete();
            } catch (RestException e) {
                if (e.getCode() == 409) {
                    this.sessionId = e.getHeaders().getOrDefault("X-Transmission-Session-Id", null);
                }

                if (this.sessionId == null) {
                    throw new IllegalStateException("Could not authenticate to Transmission RPC API.", e);
                }
            }
        }

        try {
            return action.complete();
        } catch (RestException e) {
            if (e.getCode() == 409) {
                // Retry with auto-auth enabled
                this.sessionId = null;
                return this.send(action);
            }

            throw e; // We are not supposed to handle this case
        }
    }

    /**
     * Send the provided {@link AnisekaiJson} to the transmission daemon server.
     *
     * @param data
     *         {@link AnisekaiJson} to send
     *
     * @return The query response
     *
     * @throws Exception
     *         Thrown if the query to the server fails.
     */
    private AnisekaiJson sendPacket(AnisekaiJson data) throws Exception {

        return this.send(new TransmissionCustomPacket(this.endpoint, () -> this.sessionId, data));
    }

    /**
     * Refresh, if necessary, the session to the remote transmission daemon server.
     *
     * @throws Exception
     *         Thrown if the query to the server fails.
     */
    public void getSession() throws Exception {

        AnisekaiJson packetData = new AnisekaiJson();
        packetData.put("method", "session-get");

        this.sendPacket(packetData);
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

        AnisekaiJson packetData = new AnisekaiJson();
        packetData.put("method", "torrent-get");
        packetData.put("arguments.fields", DEFAULT_TORRENT_FIELDS);

        if (!hashes.isEmpty()) {
            packetData.put("arguments.ids", hashes);
        }

        AnisekaiJson response = this.sendPacket(packetData);
        String       status   = response.getString("result");

        if (!status.equals("success")) {
            throw new IllegalStateException("Transmission failed to query torrents: Response was " + status);
        }

        AnisekaiJson  arguments  = response.readJson("arguments");
        AnisekaiArray torrents   = arguments.readArray("torrents");
        List<Torrent> torrentSet = new ArrayList<>();

        torrents.forEachJson(json -> torrentSet.add(Torrent.of(json)));
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

        AnisekaiJson packetData = new AnisekaiJson();
        packetData.put("method", "torrent-add");
        packetData.put("arguments.paused", paused);
        packetData.put("arguments.filename", entry.torrent());

        AnisekaiJson response = this.sendPacket(packetData);
        String       result   = response.getString("result");

        if (!result.equals("success")) {
            throw new IllegalStateException("Transmission client failed to queue torrent");
        }

        AnisekaiJson arguments = response.readJson("arguments");
        AnisekaiJson json;

        if (arguments.has("torrent-duplicate")) {
            json = arguments.readJson("torrent-duplicate");
        } else if (arguments.has("torrent-added")) {
            json = arguments.readJson("torrent-added");
        } else {
            throw new IllegalStateException("Transmission client failed to read server response.");
        }

        String hash = json.getString("hashString");
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

        AnisekaiJson packetData = new AnisekaiJson();
        packetData.put("method", "torrent-start");
        packetData.put("arguments.ids", Collections.singleton(torrent.hash()));

        AnisekaiJson response = this.sendPacket(packetData);
        String       result   = response.getString("result");

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

        AnisekaiJson packetData = new AnisekaiJson();
        packetData.put("method", "torrent-remove");
        packetData.put("arguments.ids", Collections.singleton(torrent.hash()));
        packetData.put("arguments.delete-local-data", true);

        AnisekaiJson response = this.sendPacket(packetData);
        String       result   = response.getString("result");

        if (!result.equals("success")) {
            throw new IllegalStateException("Transmission client failed to delete torrent");
        }
    }


}
