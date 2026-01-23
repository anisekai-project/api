package fr.anisekai.server.domain.entities;

import fr.anisekai.core.internal.services.Transmission;
import fr.anisekai.core.persistence.annotations.TriggerEvent;
import fr.anisekai.core.persistence.domain.UuidEntity;
import fr.anisekai.server.domain.events.torrent.TorrentStatusUpdatedEvent;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "torrent")
public class Torrent extends UuidEntity {

    @Column(unique = true, nullable = false, length = 40)
    private String hash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @TriggerEvent(TorrentStatusUpdatedEvent.class)
    private Transmission.TorrentStatus status = Transmission.TorrentStatus.DOWNLOAD_QUEUED;

    @Column(nullable = false)
    private double progress = 0;

    @Column(nullable = false)
    private String link;

    @Column(name = "download_directory", nullable = false)
    private String downloadDirectory;

    @Column(nullable = false)
    private byte priority;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "torrent")
    private Set<TorrentFile> files = new LinkedHashSet<>();

    public String getHash() {

        return this.hash;
    }

    public void setHash(String hash) {

        this.hash = hash;
    }

    public @NotNull String getName() {

        return this.name;
    }

    public void setName(@NotNull String name) {

        this.name = name;
    }

    public @NotNull Transmission.TorrentStatus getStatus() {

        return this.status;
    }

    public void setStatus(Transmission.@NotNull TorrentStatus status) {

        this.status = status;
    }

    public double getProgress() {

        return this.progress;
    }

    public void setProgress(double progress) {

        this.progress = progress;
    }

    public @NotNull String getLink() {

        return this.link;
    }

    public void setLink(@NotNull String link) {

        this.link = link;
    }

    public @NotNull String getDownloadDirectory() {

        return this.downloadDirectory;
    }

    public void setDownloadDirectory(@NotNull String downloadDirectory) {

        this.downloadDirectory = downloadDirectory;
    }

    public byte getPriority() {

        return this.priority;
    }

    public void setPriority(byte priority) {

        this.priority = priority;
    }

    public Set<TorrentFile> getFiles() {

        return this.files;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Torrent torrent) return EntityUtils.equals(this, torrent);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

    public Transmission.Torrent asTransmissionIdentifier() {

        return new Transmission.Torrent(
                this.hash,
                this.status,
                this.downloadDirectory,
                this.progress,
                this.getFiles().stream().map(TorrentFile::getName).toList()
        );
    }

}
