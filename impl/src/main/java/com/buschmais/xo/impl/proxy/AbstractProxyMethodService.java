package com.buschmais.xo.impl.proxy;

import com.buschmais.xo.api.XOException;
import com.buschmais.xo.api.proxy.ProxyMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AbstractProxyMethodService<E> implements ProxyMethodService<E> {

    private final Map<Method, ProxyMethod<E>> proxyMethods = new HashMap<>();

    @Override
    public Object invoke(E element, Object instance, Method method, Object[] args) throws Exception {
        ProxyMethod<E> proxyMethod = proxyMethods.get(method);
        if (proxyMethod == null) {
            throw new XOException("Cannot find proxy for method '" + method.toGenericString() + "'");
        }
        return proxyMethod.invoke(element, instance, args);
    }

    protected void addMethod(ProxyMethod<E> proxyMethod, Class<?> type, String name, Class<?>... argumentTypes) {
        Method method;
        try {
            method = type.getDeclaredMethod(name, argumentTypes);
        } catch (NoSuchMethodException e) {
            throw new XOException("Cannot resolve method '" + name + "' (" + Arrays.asList(argumentTypes) + ")", e);
        }
        addProxyMethod(proxyMethod, method);
    }

    protected void addProxyMethod(ProxyMethod<E> proxyMethod, Method method) {
        if (method != null) {
            proxyMethods.put(method, proxyMethod);
        }
    }
}
