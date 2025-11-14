package fr.anisekai.server.domain.events.torrent;

import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.core.persistence.events.EntityPropertyChangedEvent;
import fr.anisekai.server.domain.entities.Torrent;

public class TorrentStatusUpdatedEvent extends EntityPropertyChangedEvent<Torrent, Transmission.TorrentStatus> {

    public TorrentStatusUpdatedEvent(Object source, Torrent entity, Transmission.TorrentStatus previous, Transmission.TorrentStatus current) {

        super(source, entity, previous, current);
    }

}
