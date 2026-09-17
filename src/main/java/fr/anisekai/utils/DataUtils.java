package fr.anisekai.utils;

import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.scheduler.commons.ActionPlan;
import fr.anisekai.scheduler.commons.actions.CreateAction;
import fr.anisekai.scheduler.commons.actions.DeleteAction;
import fr.anisekai.scheduler.commons.actions.UpdateAction;
import org.springframework.data.repository.ListCrudRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DataUtils {

    private DataUtils() {}

    /**
     * Apply a specific {@link ActionPlan} using the given {@link ListCrudRepository}.
     *
     * @param repository
     *         The repository which will be used to read from and write to the persistence layer.
     * @param plan
     *         The action plan which should be applied to the persistence layer.
     * @param creator
     *         A function allowing to convert an object to the target type {@code E}. The returned result <b>should
     *         not</b> be a persisted entity.
     * @param <ID>
     *         Type of the entity's identifier
     * @param <E>
     *         Type of the entity
     * @param <C>
     *         Type of the data container that should be converter to an entity.
     *
     * @return A list of created and updated entities.
     */
    public static <ID extends Serializable, E extends Entity<ID>, C> List<E> applyPlan(ListCrudRepository<E, ID> repository, ActionPlan<ID, C, E> plan, Function<C, E> creator) {

        List<E> results = new ArrayList<>();

        if (!plan.updates().isEmpty()) {
            Set<ID> ids   = plan.updates().stream().map(UpdateAction::targetId).collect(Collectors.toSet());
            List<E> tasks = repository.findAllById(ids);

            if (tasks.size() != ids.size()) {
                throw new IllegalStateException(String.format(
                        "Expected %s tasks but found %s instead. Cannot reliably update following the action plan.",
                        ids.size(),
                        tasks.size()
                ));
            }

            // Mapping for easy update.
            Map<ID, E> taskMap = tasks.stream().collect(Collectors.toMap(Entity::getId, Function.identity()));

            for (UpdateAction<ID, E> update : plan.updates()) {
                update.hook().accept(taskMap.get(update.targetId()));
            }

            results.addAll(repository.saveAll(tasks));
        }

        if (!plan.creates().isEmpty()) {
            List<E> tasks = plan.creates().stream().map(CreateAction::what).map(creator).toList();
            results.addAll(repository.saveAll(tasks));
        }

        if (!plan.deletes().isEmpty()) {
            // This could seem unoptimized (which it is), but necessary for sending "deleted" events with the entities data.

            Set<ID> ids   = plan.deletes().stream().map(DeleteAction::targetId).collect(Collectors.toSet());
            List<E> tasks = repository.findAllById(ids);

            if (tasks.size() != ids.size()) {
                throw new IllegalStateException(String.format(
                        "Expected %s tasks but found %s instead. Cannot reliably delete following the action plan.",
                        ids.size(),
                        tasks.size()
                ));
            }

            repository.deleteAll(tasks);
        }

        return results;
    }

}
