package io.forestframework.example.todopolyglot;

import com.github.blindpirate.annotationmagic.Extends;
import io.forestframework.core.Forest;
import io.forestframework.core.ForestApplication;
import io.forestframework.core.config.ConfigProvider;
import io.forestframework.core.http.PlainHttpContext;
import io.forestframework.core.http.Router;
import io.forestframework.core.http.WebContext;
import io.forestframework.core.http.param.JsonRequestBody;
import io.forestframework.core.http.param.PathParam;
import io.forestframework.core.http.result.ResultProcessor;
import io.forestframework.core.http.result.RoutingResultProcessor;
import io.forestframework.core.http.routing.Delete;
import io.forestframework.core.http.routing.Get;
import io.forestframework.core.http.routing.Patch;
import io.forestframework.core.http.routing.Post;
import io.forestframework.core.http.routing.Routing;
import io.forestframework.ext.api.EnableExtensions;
import io.forestframework.ext.core.AutoScanComponentsExtension;
import io.forestframework.ext.core.IncludeGraalJs;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@IncludeGraalJs("node")
@EnableExtensions(extensions = AutoScanComponentsExtension.class)
public class TodoPolyglotApp {
    public static void main(String[] args) {
        Forest.run(TodoPolyglotApp.class);
    }

    @Singleton
    public static class BlockingJDBCTodoService  {


        @Inject
        public BlockingJDBCTodoService(ConfigProvider configProvider) {
            this.databaseUrl = configProvider.getInstance("forest.jdbc.url", String.class);
            this.databaseUsername = configProvider.getInstance("forest.jdbc.user", String.class);
            this.databasePassword = configProvider.getInstance("forest.jdbc.password", String.class);

        }

        private Connection getConnection() {
            try {
                return DriverManager.getConnection(databaseUrl, databaseUsername, databaseUrl);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void initData() {
            executeUpdate(SQL_CREATE);
        }

        private void executeUpdate(String sql) {
            try (Statement statement = getConnection().createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Todo insert(Todo todo) {
            try (PreparedStatement ps = getConnection().prepareStatement(SQL_INSERT)) {
                ps.setInt(1, todo.getId());
                ps.setString(2, todo.getTitle());
                ps.setBoolean(3, todo.isCompleted());
                ps.setInt(4, todo.getOrder());
                ps.setString(5, todo.getUrl());
                ps.executeUpdate();
                return todo;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<Todo> all() {
            try (Statement statement = getConnection().createStatement()) {
                List<Todo> todos = new ArrayList<>();
                ResultSet resultSet = statement.executeQuery(SQL_QUERY_ALL);
                while (resultSet.next()) {
                    todos.add(new Todo(
                            resultSet.getInt(1),
                            resultSet.getString(2),
                            resultSet.getBoolean(3),
                            resultSet.getInt(4),
                            resultSet.getString(5))
                    );
                }

                return todos;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Optional<Todo> getCertain(String todoId) {
            try (PreparedStatement ps = getConnection().prepareStatement(SQL_QUERY)) {
                ps.setString(1, todoId);
                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    return Optional.of(new Todo(
                            resultSet.getInt(1),
                            resultSet.getString(2),
                            resultSet.getBoolean(3),
                            resultSet.getInt(4),
                            resultSet.getString(5))
                    );
                } else {
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Optional<Todo> update(String todoId, Todo newTodo) {
            return getCertain(todoId).map(oldTodo -> {
                Todo todo = oldTodo.merge(newTodo);
                try (PreparedStatement ps = getConnection().prepareStatement(SQL_UPDATE)) {
                    ps.setInt(1, todo.getId());
                    ps.setString(2, todo.getTitle());
                    ps.setBoolean(3, todo.isCompleted());
                    ps.setInt(4, todo.getOrder());
                    ps.setString(5, todo.getUrl());
                    ps.setString(6, todoId);
                    ps.executeUpdate();
                    return todo;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public void delete(String todoId) {
            try (PreparedStatement ps = getConnection().prepareStatement(SQL_DELETE)) {
                ps.setString(1, todoId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void deleteAll() {
            executeUpdate(SQL_DELETE_ALL);
        }
    }

}
