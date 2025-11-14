package fr.anisekai.discord.tasks.broadcast;

import fr.anisekai.discord.JDAStore;
import fr.anisekai.library.Library;
import fr.anisekai.server.domain.entities.Broadcast;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.server.services.TaskService;
import fr.anisekai.server.tasking.TaskExecutor;
import fr.anisekai.server.tasking.TaskFactory;

public abstract class BroadcastFactory<T extends TaskExecutor> implements TaskFactory<T> {

    public static final String PREFIX = "broadcast";

    private final Library          library;
    private final TaskService      service;
    private final JDAStore         store;
    private final BroadcastService broadcastService;

    public BroadcastFactory(Library library, TaskService service, JDAStore store, BroadcastService broadcastService) {

        this.library          = library;
        this.service          = service;
        this.store            = store;
        this.broadcastService = broadcastService;
    }

    public String getPrefix() {

        return PREFIX;
    }

    public Library getLibrary() {

        return this.library;
    }

    public TaskService getService() {

        return this.service;
    }

    public BroadcastService getBroadcastService() {

        return this.broadcastService;
    }

    public JDAStore getStore() {

        return this.store;
    }

    @Override
    public boolean hasNamedTask() {

        return true;
    }

    @Override
    public boolean allowDuplicated() {

        return false;
    }

    public String asTaskName(Broadcast broadcast) {

        return String.format("%s:%s", this.getName(), broadcast.getId());
    }

}
