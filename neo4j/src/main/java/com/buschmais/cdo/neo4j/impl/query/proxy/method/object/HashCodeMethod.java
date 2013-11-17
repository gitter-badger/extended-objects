package com.buschmais.cdo.neo4j.impl.query.proxy.method.object;

import com.buschmais.cdo.neo4j.impl.query.proxy.method.RowProxyMethod;

import java.util.Map;

public class HashCodeMethod implements RowProxyMethod {

    @Override
    public Object invoke(Map<String, Object> element, Object instance, Object[] args) {
        return element.hashCode();
    }
}