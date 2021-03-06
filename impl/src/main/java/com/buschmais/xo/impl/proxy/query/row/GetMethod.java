package com.buschmais.xo.impl.proxy.query.row;

import com.buschmais.xo.impl.proxy.query.RowProxyMethod;

import java.util.Map;

public class GetMethod implements RowProxyMethod {

    @Override
    public Object invoke(Map<String, Object> entity, Object instance, Object[] args) {
        String column = (String) args[0];
        Class<?> type = (Class<?>) args[1];
        Object value = entity.get(column);
        return value != null ? type.cast(value) : null;
    }

}
