package fr.anisekai.library.services;

import fr.anisekai.core.internal.services.Nyaa;
import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.server.services.SettingService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class SpringTransmissionClient {

    private final SettingService settingService;
    private       Transmission   client;

    public SpringTransmissionClient(SettingService settingService) {

        this.settingService = settingService;
    }

    /**
     * This method allow the current instance to self-check the state of the underlying transmission client.
     */
    public void check() {

        Optional<String> optionalEndpoint = this.settingService.getDownloadServer();

        if (optionalEndpoint.isEmpty()) {
            this.client = null;
            return;
        }

        String server = optionalEndpoint.get();

        if (this.client != null && this.client.getEndpoint().equals(server)) {
            return;
        }

        this.client = new Transmission(server);
    }

    public boolean isAvailable() {

        return this.client != null;
    }

    public List<Transmission.Torrent> query() throws Exception {

        return this.client.query(Collections.emptyList());
    }

    public List<Transmission.Torrent> query(Collection<String> hashes) throws Exception {

        return this.client.query(hashes);
    }

    public Transmission.Torrent download(Nyaa.Entry entry) throws Exception {

        return this.client.download(entry, false);
    }

    public Transmission.Torrent query(Nyaa.Entry entry) throws Exception {

        return this.client.download(entry, true);
    }

    public Transmission.Torrent resume(Transmission.Torrent torrent) throws Exception {

        return this.client.start(torrent);
    }

    public void delete(Transmission.Torrent torrent) throws Exception {

        this.client.delete(torrent);
    }

}
