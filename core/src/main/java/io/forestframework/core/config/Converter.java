package io.forestframework.core.config;

import com.google.common.collect.ImmutableMap;
import io.forestframework.utils.Pair;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

public interface Converter<IN, OUT> {
    default boolean accept(IN in, Class<? extends IN> inType, Class<? extends OUT> outType) {
        return true;
    }

    OUT convert(IN in, Class<? extends IN> inType, Class<? extends OUT> outType);

    @SuppressWarnings("rawtypes")
    static Converter getDefaultConverter() {
        return DefaultConverter.INSTANCE;
    }
}

@SuppressWarnings("rawtypes")
enum DefaultConverter implements Converter<Object, Object> {
    INSTANCE;

    private final Map<Pair<Class, Class>, Converter> converters = ImmutableMap.<Pair<Class, Class>, Converter>builder()
        .put(Pair.of(Object.class, String.class), new ObjectToString())
        .put(Pair.of(Object.class, JsonObject.class), new ObjectToJsonObject())
        .put(Pair.of(String.class, Enum.class), new StringToEnum())
        .put(Pair.of(Object.class, long.class), new ObjectToLong())
        .put(Pair.of(String.class, Long.class), new ObjectToLong())
        .put(Pair.of(Object.class, Integer.class), new ObjectToInteger())
        .put(Pair.of(Object.class, int.class), new ObjectToInteger())
        .put(Pair.of(Object.class, Boolean.class), new ObjectToBoolean())
        .put(Pair.of(Object.class, boolean.class), new ObjectToBoolean())
        .put(Pair.of(Map.class, Object.class), new JsonObjectConstructorConverter())
        .put(Pair.of(Map.class, Object.class), new ReflectionMapToObjectConverter())
        .build();

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Object convert(Object obj, Class<?> inType, Class<?> outType) {
        if (outType.isAssignableFrom(inType)) {
            return obj;
        }
        for (Map.Entry<Pair<Class, Class>, Converter> entry : converters.entrySet()) {
            if (entry.getKey().getLeft().isAssignableFrom(inType) && entry.getKey().getRight().isAssignableFrom(outType)
                && entry.getValue().accept(obj, inType, outType)) {
                return entry.getValue().convert(obj, inType, outType);
            }
        }
        throw new RuntimeException("Can't find matching converter: in: " + inType + ", out: " + outType);
    }
}

@SuppressWarnings("rawtypes")
class JsonObjectConstructorConverter implements Converter<Map, Object> {
    @Override
    public boolean accept(Map map, Class<? extends Map> inType, Class<?> outType) {
        try {
            outType.getConstructor(JsonObject.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public Object convert(Map map, Class<? extends Map> inType, Class<?> outType) {
        try {
            Constructor<?> constructor = outType.getConstructor(JsonObject.class);
            return constructor.newInstance(new JsonObject(map));
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Can't convert " + map + " to type " + outType, e);
        }
    }
}

@SuppressWarnings("ALL")
class ReflectionMapToObjectConverter implements Converter<Map, Object> {
    @Override
    public boolean accept(Map map, Class<? extends Map> inType, Class<?> outType) {
        try {
            outType.getConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public Object convert(Map map, Class<? extends Map> inType, Class<?> outType) {
        try {
            Object ret = outType.getConstructor().newInstance();
            map.forEach((key, value) -> {
                PropertyUtils.setProperty(ret, key.toString(), value);
            });
            return ret;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Can't convert " + map + " to type " + outType, e);
        }
    }
}

class ObjectToJsonObject implements Converter<Object, JsonObject> {
    @Override
    public JsonObject convert(Object obj, Class<?> inType, Class<? extends JsonObject> outType) {
        return new JsonObject(Json.CODEC.toString(obj));
    }
}

class ObjectToBoolean implements Converter<Object, Boolean> {
    @Override
    public Boolean convert(Object obj, Class<?> inType, Class<? extends Boolean> outType) {
        String s = obj.toString();
        return Boolean.parseBoolean(s);
    }
}

class ObjectToString implements Converter<Object, String> {
    @Override
    public String convert(Object o, Class<?> inType, Class<? extends String> outType) {
        return Objects.toString(o);
    }
}

@SuppressWarnings("rawtypes")
class StringToEnum implements Converter<String, Enum> {
    @Override
    public Enum convert(String s, Class<? extends String> inType, Class<? extends Enum> outType) {
        try {
            return (Enum) outType.getMethod("valueOf", String.class).invoke(null, s);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}

class ObjectToInteger implements Converter<Object, Integer> {
    @Override
    public Integer convert(Object obj, Class<?> inType, Class<? extends Integer> outType) {
        String s = obj.toString();
        if (s.startsWith("0x")) {
            return Integer.valueOf(s.substring(2), 16);
        } else {
            return Integer.valueOf(s);
        }
    }
}

class ObjectToLong implements Converter<Object, Long> {
    @Override
    public Long convert(Object obj, Class<?> inType, Class<? extends Long> outType) {
        String s = obj.toString();
        if (s.startsWith("0x")) {
            return Long.valueOf(s.substring(2), 16);
        } else {
            return Long.valueOf(s);
        }
    }
}

