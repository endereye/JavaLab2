package cn.endereye.framework.ioc;

import cn.endereye.framework.ioc.annotations.InjectSource;
import cn.endereye.framework.ioc.annotations.InjectTarget;
import cn.endereye.framework.utils.Annotations;
import cn.endereye.framework.utils.scanner.Scanner;
import com.google.common.collect.HashBasedTable;

import java.lang.reflect.Field;
import java.util.*;

public class IOCManager {
    private static IOCManager instance = null;

    private final HashMap<Class<?>, Object> sharedObjects = new HashMap<>();

    private final HashBasedTable<Class<?>, String, LinkedList<Class<?>>> sources = HashBasedTable.create();

    private final ArrayList<Class<?>> targets = new ArrayList<>();

    public static IOCManager getInstance() {
        if (instance == null) {
            synchronized (IOCManager.class) {
                if (instance == null)
                    instance = new IOCManager();
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSingleton(Class<T> type) throws IOCFrameworkException {
        try {
            if (!sharedObjects.containsKey(type))
                sharedObjects.put(type, type.newInstance());
            return (T) sharedObjects.get(type);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IOCFrameworkException("Cannot instantiate class " + type.getName());
        }
    }

    public void register(Class<?> type) {
        if (type.getAnnotation(Deprecated.class) == null) {
            final InjectSource annotation = Annotations.findAnnotation(InjectSource.class, type);
            if (annotation != null) {
                registerSource(type, annotation.name(), type);
                for (Class<?> inf : type.getInterfaces())
                    registerSource(inf, annotation.name(), type);
            }
            if (Arrays.stream(type.getDeclaredFields()).anyMatch(f -> f.getAnnotation(InjectTarget.class) != null))
                targets.add(type);
        }
    }

    public void inject() throws IOCFrameworkException {
        for (Class<?> target : targets) {
            final Object instance = getSingleton(target);
            for (Field field : target.getDeclaredFields()) {
                final InjectTarget annotation = field.getAnnotation(InjectTarget.class);
                if (annotation != null) {
                    final LinkedList<Class<?>> dependencies;
                    if (sources.contains(field.getType(), field.getName())) {
                        // 1st priority
                        // Search for sources matching both type and name.
                        dependencies = sources.get(field.getType(), field.getName());
                    } else {
                        // 2nd priority
                        // Search for sources matching the corresponding key specified by policy.
                        dependencies = new LinkedList<>();
                        (annotation.policy() == InjectTarget.Policy.BY_TYPE
                                ? sources.row(field.getType())
                                : sources.column(field.getName())).values().forEach(dependencies::addAll);
                    }
                    if (dependencies.size() < 1)
                        throw new IOCFrameworkException("No matching source of " + field.toGenericString());
                    if (dependencies.size() > 1)
                        throw new IOCFrameworkException("Too many matching source of " + field.toGenericString());
                    try {
                        field.setAccessible(true);
                        field.set(instance, getSingleton(dependencies.getFirst()));
                    } catch (IllegalAccessException e) {
                        throw new IOCFrameworkException("Cannot inject into " + field.toGenericString());
                    }
                }
            }
        }
    }

    public void scan(String pkg) throws IOCFrameworkException {
        try {
            Scanner.scan(pkg, this::register);
        } catch (Exception e) {
            throw new IOCFrameworkException("Failed when scanning classes");
        }
    }

    private IOCManager() {
    }

    private void registerSource(Class<?> type, String name, Class<?> source) {
        if (!sources.contains(type, name))
            sources.put(type, name, new LinkedList<>(Collections.singletonList(source)));
        else
            sources.get(type, name).addLast(source);
    }

}