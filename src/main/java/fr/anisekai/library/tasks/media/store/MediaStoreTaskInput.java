package fr.anisekai.library.tasks.media.store;

import java.util.Objects;
import java.util.UUID;

public record MediaStoreTaskInput(UUID episode, boolean refresh) {

    public MediaStoreTaskInput {

        Objects.requireNonNull(episode, "episode");
    }

}
