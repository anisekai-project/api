package fr.anisekai.server.services;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class DatabaseLockService {

    public static final String BROADCAST_SCHEDULE = "broadcast-schedule";
    public static final String TASK_QUEUE = "task-queue";

    private final JdbcClient jdbc;

    public DatabaseLockService(JdbcClient jdbc) {

        this.jdbc = jdbc;
    }

    public void lock(String name) {

        this.jdbc.sql("SELECT `name` FROM `application_lock` WHERE `name` = :name FOR UPDATE")
                 .param("name", name)
                 .query(String.class)
                 .optional()
                 .orElseThrow(() -> new IllegalStateException("Unknown application lock: " + name));
    }

}
