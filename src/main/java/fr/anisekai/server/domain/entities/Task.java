package fr.anisekai.server.domain.entities;

import fr.anisekai.core.persistence.domain.UuidEntity;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.scheduler.tasking.interfaces.structure.TaskInterface;
import fr.anisekai.utils.EntityUtils;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "task")
public class Task extends UuidEntity implements TaskInterface {

    public static final byte PRIORITY_DEFAULT        = 0;
    public static final byte PRIORITY_AUTOMATIC_LOW  = 1;
    public static final byte PRIORITY_MANUAL_LOW     = 2;
    public static final byte PRIORITY_AUTOMATIC_HIGH = 3;
    public static final byte PRIORITY_MANUAL_HIGH    = 4;
    public static final byte PRIORITY_URGENT         = 5;

    @Column(name = "factory_name", nullable = false)
    private String factoryName;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(nullable = false)
    private byte priority = 0;

    @Column(nullable = false)
    private String arguments = "{}";

    @Column(name = "failure_count", nullable = false)
    private byte failureCount;

    @Column(name = "active_key", unique = true, length = 511)
    private String activeKey;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker assignedWorker;

    @Column(name = "isolation_id")
    private UUID isolationId;

    public @NotNull String getFactoryName() {

        return this.factoryName;
    }

    public void setFactoryName(@NotNull String factoryName) {

        this.factoryName = factoryName;
        this.refreshActiveKey();
    }

    public @NotNull String getName() {

        return this.name;
    }

    public void setName(@NotNull String name) {

        this.name = name;
        this.refreshActiveKey();
    }

    public @NotNull TaskStatus getStatus() {

        return this.status;
    }

    public void setStatus(@NotNull TaskStatus status) {

        this.status = status;
        if (status == TaskStatus.SCHEDULED || status == TaskStatus.EXECUTING) {
            this.completedAt = null;
        } else if (this.completedAt == null) {
            this.completedAt = Instant.now();
        }
        this.refreshActiveKey();
    }

    public byte getPriority() {

        return this.priority;
    }

    public void setPriority(byte priority) {

        this.priority = priority;
    }

    public @NotNull String getArguments() {

        return this.arguments;
    }

    public void setArguments(@NotNull String arguments) {

        this.arguments = arguments;
    }

    public byte getFailureCount() {

        return this.failureCount;
    }

    public void setFailureCount(byte failureCount) {

        this.failureCount = failureCount;
    }

    public @Nullable String getActiveKey() {

        return this.activeKey;
    }

    public @Nullable Instant getStartedAt() {

        return this.startedAt;
    }

    public void setStartedAt(Instant startedAt) {

        this.startedAt = startedAt;
    }

    public @Nullable Instant getCompletedAt() {

        return this.completedAt;
    }

    public void setCompletedAt(Instant completedAt) {

        this.completedAt = completedAt;
    }

    public @Nullable Worker getAssignedWorker() {

        return this.assignedWorker;
    }

    public void setAssignedWorker(@Nullable Worker assignedWorker) {

        this.assignedWorker = assignedWorker;
    }

    public @Nullable UUID getIsolationId() {

        return this.isolationId;
    }

    public void setIsolationId(@Nullable UUID isolationId) {

        this.isolationId = isolationId;
    }

    private void refreshActiveKey() {

        // Task names already embed the factory name (e.g. "media:convert:<id>:<ref>"),
        // so storing "factory:name" would duplicate the prefix pointlessly.
        boolean active = this.status == TaskStatus.SCHEDULED || this.status == TaskStatus.EXECUTING;
        this.activeKey = active && this.name != null
                ? this.name
                : null;
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
