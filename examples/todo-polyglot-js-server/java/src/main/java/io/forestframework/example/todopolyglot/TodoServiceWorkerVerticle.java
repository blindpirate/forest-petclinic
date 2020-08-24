package io.forestframework.example.todopolyglot;

import io.forestframework.core.config.Config;
import io.forestframework.core.event.OnEvent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Singleton
public class TodoServiceWorkerVerticle extends AbstractVerticle {
    private static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS `todo` (\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
            "  `title` varchar(255) DEFAULT NULL,\n" +
            "  `completed` tinyint(1) DEFAULT NULL,\n" +
            "  `order` int(11) DEFAULT NULL,\n" +
            "  `url` varchar(255) DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`) )";
    private static final String SQL_INSERT = "INSERT INTO `todo` " +
            "(`id`, `title`, `completed`, `order`, `url`) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE `todo`\n" +
            "        SET\n" +
            "        `id` = ?,\n" +
            "        `title` = ?,\n" +
            "        `completed` = ?,\n" +
            "        `order` = ?,\n" +
            "        `url` = ?\n" +
            "        WHERE `id` = ?";
    private static final String SQL_QUERY_ALL = "SELECT `id`, `title`, `completed`, `order`, `url` FROM todo";
    private static final String SQL_DELETE = "DELETE FROM `todo` WHERE `id` = ?";
    private static final String SQL_DELETE_ALL = "DELETE FROM `todo`";
    private static final String SQL_QUERY = "SELECT `id`, `title`, `completed`, `order`, `url` FROM todo WHERE id = ?";

    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final EventBus eventBus;

    @Inject
    public TodoServiceWorkerVerticle(
            @Config("forest.jdbc.url") String databaseUrl,
            @Config("forest.jdbc.user") String databaseUsername,
            @Config("forest.jdbc.password") String databasePassword,
            EventBus eventBus) {
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.eventBus = eventBus;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        executeUpdate(SQL_CREATE);
    }

    @OnEvent("get.todo")
    private String onDelete(String message) {
    }

    @OnEvent("insert.todo")
    private String onUpdate(String message) {
    }

    @OnEvent("update.todo")
    private String onInsert(String message) {
    }

    @OnEvent("get.todo")
    private String onGet(String message) {
    }

    @Override
    public void stop() throws Exception {
    }

    private void executeUpdate(String sql) {
        try (Statement statement = getConnection().createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() {
        try {
            return DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
