package info.deathsign.tinyioc.core;

import info.deathsign.tinyioc.annotation.AutoInject;
import info.deathsign.tinyioc.annotation.AutoInjectCreater;
import info.deathsign.tinyioc.annotation.AutoInvoke;
import info.deathsign.tinyioc.annotation.AutoManage;
import info.deathsign.tinyioc.misc.Tuple;
import info.deathsign.tinyioc.util.ClassUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class IOCManager {

    HashMap<String, Object> instanceMap = new HashMap<>();
    HashMap<String, Object> creators = new HashMap<>();

    public void load(String... packageNames) {
        Set<Class> classes = scanAllClasses(packageNames);
        List<Class> filteredClasses = filterClasses(classes);
        instanceCreators(classes);
        instanceClasses(filteredClasses);
        autoInject(instanceMap);
        autoInvoke(instanceMap.values());
    }

    public void inject(Object instance) {
        if (!instanceMap.containsKey(instance.getClass().getName())) {
            instanceMap.put(instance.getClass().getName(), instance);
            fieldInstance(instance);
        }
    }

    public <T extends Object> T getInstance(Class<T> clz) {
        return getInstance(clz.getName());
    }

    public <T extends Object> T getInstance(String clz) {
        Object o = instanceMap.get(clz);
        if (o == null) {
            return null;
        }
        return (T) o;
    }

//    public List<Object> getInstanceByAnnotation(Class annotation) {
//        List<Object> collect = this.instanceMap.values().stream()
//                .filter(x -> {
//                    Annotation declaredAnnotation = x.getClass().getAnnotation(annotation);
//                    return declaredAnnotation != null;
//                })
//                .collect(Collectors.toList());
//
//        return collect;
//    }

    public <T extends Object> List<Tuple<Object, Method, T>> getInstanceByMethodAnnotation(Class annotation) {
        List<Tuple<Object, Method, T>> collect =
                this.instanceMap.values().stream()
                        .distinct()
                        .flatMap(x -> {
                            LinkedList<Tuple<Object, Method, T>> ret = new LinkedList<>();
                            Method[] methods = x.getClass().getMethods();
                            for (Method method : methods) {
                                Annotation annotation1 = method.getAnnotation(annotation);

                                if (annotation1 == null)
                                    continue;

                                if (!ret.stream().anyMatch((y) -> {
                                    return y.get2() == method;
                                })) {

                                    ret.add(new Tuple<Object, Method, T>(x, method, (T) annotation1));
                                }
                            }

                            return ret.stream();

                        })
                        .distinct()
                        .collect(Collectors.toList());

        return collect;
    }

    private Set<Class> scanAllClasses(String... packages) {
        Set<Class> classes = new HashSet<>();
        for (String pkg : packages) {
            classes.addAll(ClassUtils.getClasses(pkg));
        }

        return classes;
    }

    private void registerInterface(Object obj) {
        Class<?> aClass = obj.getClass();
        Class<?>[] interfaces = aClass.getInterfaces();

        for (Class iterf : interfaces) {
            Annotation declaredAnnotationsByType = iterf.getDeclaredAnnotation(AutoManage.class);
            if (declaredAnnotationsByType == null)
                continue;

            this.instanceMap.put(iterf.getName(), obj);
            log.debug("IOC register " + iterf.getName() + " for " + obj.toString());
        }
    }

    private void instanceCreators(Set<Class> classes) {

        Set<Object> instances;
        List<Method> methods = classes.stream()
                .flatMap(x -> {
                    LinkedList<Method> ret = new LinkedList<>();

                    Method[] declaredMethods = x.getDeclaredMethods();

                    for (Method m : declaredMethods) {
                        AutoInjectCreater annotation = m.getDeclaredAnnotation(AutoInjectCreater.class);

                        if (annotation != null) {
                            ret.add(m);
                        }
                    }

                    return ret.stream();
                })
                .collect(Collectors.toList());

        methods.stream()
                .forEach(s -> {
                    Class<?> ownerClass = s.getDeclaringClass();
                    if (ownerClass == null) return;
                    Object instance;
                    try {
                        instance = ownerClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        return;
                    }

                    try {
                        Object ins = s.invoke(instance);
                        this.instanceMap.put(ins.getClass().getName(), ins);
                        registerInterface(ins);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        return;
                    }
                });
    }

    private void autoInvoke(Collection<Object> instances) {
        instances.forEach(x -> {
            Method[] methods = x.getClass().getDeclaredMethods();

            for (Method method : methods) {
                AutoInvoke declaredAnnotation = method.getDeclaredAnnotation(AutoInvoke.class);
                if (declaredAnnotation == null) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    method.invoke(x);
                } catch (IllegalAccessException e) {
                    log.error(e.getMessage());
                } catch (InvocationTargetException e) {
                    log.error(e.getMessage());
                }
            }
        });
    }

    private List<Class> filterClasses(Set<Class> inputClasses) {
        return inputClasses.stream()
                .filter(x -> {
                    Annotation declaredAnnotation = x.getDeclaredAnnotation(AutoManage.class);
                    return declaredAnnotation != null;
                })
                .sorted((x, y) -> {
                    AutoManage xa = (AutoManage) x.getDeclaredAnnotation(AutoManage.class);
                    AutoManage ya = (AutoManage) y.getDeclaredAnnotation(AutoManage.class);
                    return xa.order() > ya.order() ? 1 : -1;
                })
                .collect(Collectors.toList());
    }

    private void instanceClasses(List<Class> classes) {
        int count = 0;
        for (Class cls : classes) {
            AutoManage annotation = (AutoManage) cls.getDeclaredAnnotation(AutoManage.class);
            if (annotation == null)
                continue;

            String name = cls.getName();
            if (cls.isInterface()) {
                continue;
            }

            try {
                boolean found = false;
                Object clzInstance = this.instanceMap.get(name);
                if (clzInstance == null) {
                    clzInstance = cls.newInstance();
                    instanceMap.put(name, clzInstance);
                    registerInterface(clzInstance);
                    log.debug("IOC register " + name + " for " + clzInstance.toString());
                }


            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // add self into the map
        instanceMap.put(this.getClass().getName(), this);

        log.info("IOCManager instanced {} classes", instanceMap.size());
    }

    private void autoInject(HashMap<String, Object> map) {
        map.forEach((className, instance) -> {
            fieldInstance(instance);
        });
    }

    private void fieldInstance(Object instance) {
        Field[] fields = instance.getClass().getDeclaredFields();

        for (Field field : fields) {
            AutoInject annotation = field.getAnnotation(AutoInject.class);
            if (annotation == null)
                continue;

            if (instanceMap.containsKey(field.getType().getName())) {
                try {
                    Object obj = instanceMap.get(field.getType().getName());
                    field.setAccessible(true);
                    field.set(instance, instanceMap.get(field.getType().getName()));
                    log.debug("IOC injected " + field.getDeclaringClass().getName() + "." + field.getName() + " for " + obj.getClass().getName());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
