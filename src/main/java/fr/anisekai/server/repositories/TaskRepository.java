package fr.anisekai.server.repositories;

import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.scheduler.tasking.enums.TaskStatus;
import fr.anisekai.server.domain.entities.Task;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends AnisekaiRepository<Task, UUID> {

    List<Task> findAllByNameAndStatusIn(String name, List<TaskStatus> scheduled);

    List<Task> findAllByStatusOrderByPriorityDescCreatedAtAscIdAsc(TaskStatus status);

    Optional<Task> findFirstByFactoryNameAndStatusIn(String factoryName, List<TaskStatus> statuses);

    boolean existsByNameAndStatusIn(String name, Collection<TaskStatus> status);

    Optional<Task> findFirstByFactoryNameAndNameAndStatusIn(String factoryName, String name, Collection<TaskStatus> status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE Task t
            SET t.status = :executing, t.startedAt = :startedAt, t.completedAt = NULL
            WHERE t.id = :id AND t.status = :scheduled
            """)
    int claim(UUID id, TaskStatus scheduled, TaskStatus executing, Instant startedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE Task t
            SET t.status = :scheduled, t.startedAt = NULL
            WHERE t.status = :executing
            """)
    int resetExecuting(TaskStatus executing, TaskStatus scheduled);

}
