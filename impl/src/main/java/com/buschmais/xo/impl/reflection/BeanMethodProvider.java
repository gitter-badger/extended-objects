package com.buschmais.xo.impl.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import com.buschmais.xo.api.XOException;
import com.buschmais.xo.spi.reflection.AnnotatedMethod;
import com.buschmais.xo.spi.reflection.GetPropertyMethod;
import com.buschmais.xo.spi.reflection.SetPropertyMethod;
import com.buschmais.xo.spi.reflection.UserMethod;
import com.google.common.base.CaseFormat;

public final class BeanMethodProvider {

    private final Set<Method> methods = new HashSet<>();
    private final Map<String, Method> getters = new HashMap<>();
    private final Map<String, Method> setters = new HashMap<>();
    private final Map<String, Class<?>> types = new HashMap<>();
    private final Map<String, Type> genericTypes = new HashMap<>();

    private BeanMethodProvider() {
    }

    public static BeanMethodProvider newInstance() {
        return new BeanMethodProvider();
    }

    public Collection<AnnotatedMethod> getMethods(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            Type genericReturnType = method.getGenericReturnType();
            Class<?>[] parameterTypes = method.getParameterTypes();
            Type[] genericParameterTypes = method.getGenericParameterTypes();
            if (methodName.startsWith("get") && parameterTypes.length == 0 && !void.class.equals(returnType)) {
                String name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.substring(3));
                getters.put(name, method);
                addType(type, name, returnType, genericReturnType);
            } else if (methodName.startsWith("is") && parameterTypes.length == 0 && !void.class.equals(returnType)) {
                String name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.substring(2));
                getters.put(name, method);
                addType(type, name, returnType, genericReturnType);
            } else if (methodName.startsWith("set") && parameterTypes.length == 1 && void.class.equals(returnType) && methodName.startsWith("set")) {
                String name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.substring(3));
                setters.put(name, method);
                addType(type, name, parameterTypes[0], genericParameterTypes[0]);
            } else {
                methods.add(method);
            }
        }
        List<AnnotatedMethod> typeMethods = new ArrayList<>();
        Map<String, GetPropertyMethod> getPropertyMethods = new HashMap<>();
        for (Map.Entry<String, Method> methodEntry : getters.entrySet()) {
            String name = methodEntry.getKey();
            Method getter = methodEntry.getValue();
            Class<?> propertyType = types.get(name);
            Type genericType = genericTypes.get(name);
            GetPropertyMethod getPropertyMethod = new GetPropertyMethod(getter, name, propertyType, genericType);
            typeMethods.add(getPropertyMethod);
            getPropertyMethods.put(name, getPropertyMethod);
        }
        for (Map.Entry<String, Method> methodEntry : setters.entrySet()) {
            String name = methodEntry.getKey();
            Method setter = methodEntry.getValue();
            GetPropertyMethod getPropertyMethod = getPropertyMethods.get(name);
            Class<?> propertyType = types.get(name);
            Type genericType = genericTypes.get(name);
            SetPropertyMethod setPropertyMethod = new SetPropertyMethod(setter, getPropertyMethod, name, propertyType, genericType);
            typeMethods.add(setPropertyMethod);
        }
        for (Method method : methods) {
            typeMethods.add(new UserMethod(method));
        }
        return typeMethods;
    }

    private void addType(Class<?> declaringType, String name, Class<?> type, Type genericType) {
        Class<?> existingType = types.put(name, type);
        if (existingType != null && !existingType.equals(type)) {
            throw new XOException("Get and set methods for property '" + name + "' of type '" + declaringType.getName() + "' do not declare the same type: "
                    + existingType.getName() + " <> " + type.getName());
        }
        Type existingGenericType = genericTypes.put(name, genericType);
        if (existingGenericType != null && !existingGenericType.equals(genericType)) {
            throw new XOException("Get and set methods for property '" + name + "' of type '" + declaringType.getName()
                    + "' do not declare the same generic type: " + existingGenericType + " <> " + type.getName());
        }
    }
}
