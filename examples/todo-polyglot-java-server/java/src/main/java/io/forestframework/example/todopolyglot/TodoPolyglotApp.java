package io.forestframework.example.todopolyglot;

import com.github.blindpirate.annotationmagic.Extends;
import io.forestframework.core.Forest;
import io.forestframework.core.ForestApplication;
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
import io.forestframework.ext.core.IncludeGraalJs;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;

import javax.inject.Singleton;

@IncludeGraalJs("node")
@ForestApplication
public class TodoPolyglotApp {
    public static void main(String[] args) {
        Forest.run(TodoPolyglotApp.class);
    }

    @Extends(ResultProcessor.class)
    @ResultProcessor(by = MessageResultProcess.class)
    public @interface EventBusMessage {
    }

    @Singleton
    public static class MessageResultProcess implements RoutingResultProcessor {
        @Override
        public Object processResponse(WebContext context, Routing routing, Object returnValue) {
            Message<String> message = (Message<String>) returnValue;
            return ((PlainHttpContext) context).response().write(message.body());
        }
    }

    @Router
    public static class TodoRouter {
        private final EventBus eventBus;

        public TodoRouter(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        @Get("/todos/:todoId")
        @EventBusMessage
        public Future<Message<String>> handleGetTodo(@PathParam("todoId") String todoId) {
            return eventBus.request("get.todo", todoId);
        }

        @Get(value = "/todos")
        @EventBusMessage
        public Future<Message<String>> handleGetAll() {
            return eventBus.request("get.todo", "all");
        }

        @Post("/todos")
        @EventBusMessage
        public Future<Message<String>> handleCreateTodo(@JsonRequestBody Todo todo, HttpServerRequest request) {
            return eventBus.request("insert.todo", Todo.wrapTodo(todo, request));
        }

        @Patch("/todos/:todoId")
        @EventBusMessage
        public Future<Message<String>> handleUpdateTodo(@PathParam("todoId") String todoId, @JsonRequestBody String todo) {
            return eventBus.request("update.todo", todo);
        }


        @Delete("/todos/:todoId")
        public Future<Message<String>> handleDeleteOne(@PathParam("todoId") String todoId) {
            return eventBus.request("delete.todo" , todoId);
        }

        @Delete("/todos")
        public Future<Message<Object>> handleDeleteAll() {
            return eventBus.request("delete.todo", "all");
        }
    }
}
