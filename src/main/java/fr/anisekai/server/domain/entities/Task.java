package fr.anisekai.server.domain.entities;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.persistence.domain.IncrementableEntity;
import fr.anisekai.server.domain.enums.TaskStatus;
import fr.anisekai.server.types.JSONType;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
public class Task extends IncrementableEntity {

    public static final byte PRIORITY_DEFAULT        = 0;
    public static final byte PRIORITY_AUTOMATIC_LOW  = 1;
    public static final byte PRIORITY_MANUAL_LOW     = 2;
    public static final byte PRIORITY_AUTOMATIC_HIGH = 3;
    public static final byte PRIORITY_MANUAL_HIGH    = 4;
    public static final byte PRIORITY_URGENT         = 5;

    @Column(nullable = false)
    private String factoryName;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(nullable = false)
    private byte priority = 0;

    @Type(JSONType.class)
    private AnisekaiJson arguments;

    @Column(nullable = false)
    private byte failureCount;

    private ZonedDateTime startedAt;

    @Column
    private ZonedDateTime completedAt;


    public @NotNull String getFactoryName() {

        return this.factoryName;
    }

    public void setFactoryName(@NotNull String factoryName) {

        this.factoryName = factoryName;
    }

    public @NotNull String getName() {

        return this.name;
    }

    public void setName(@NotNull String name) {

        this.name = name;
    }

    public @NotNull TaskStatus getStatus() {

        return this.status;
    }

    public void setStatus(@NotNull TaskStatus status) {

        this.status = status;
    }

    public byte getPriority() {

        return this.priority;
    }

    public void setPriority(byte priority) {

        this.priority = priority;
    }

    public @NotNull AnisekaiJson getArguments() {

        return this.arguments;
    }

    public void setArguments(@NotNull AnisekaiJson arguments) {

        this.arguments = arguments;
    }

    public byte getFailureCount() {

        return this.failureCount;
    }

    public void setFailureCount(byte failureCount) {

        this.failureCount = failureCount;
    }

    public @Nullable ZonedDateTime getStartedAt() {

        return this.startedAt;
    }

    public void setStartedAt(ZonedDateTime startedAt) {

        this.startedAt = startedAt;
    }

    public @Nullable ZonedDateTime getCompletedAt() {

        return this.completedAt;
    }

    public void setCompletedAt(ZonedDateTime completedAt) {

        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof Task task) return EntityUtils.equals(this, task);
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(this.getId());
    }

    public String toDiscordName() {

        return String.format(
                "Tâche **%s** (**%s** : `%s`)",
                this.getId(),
                this.getFactoryName(),
                this.getName()
        );
    }

}
