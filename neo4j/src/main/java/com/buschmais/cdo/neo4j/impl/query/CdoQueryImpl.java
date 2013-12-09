package com.buschmais.cdo.neo4j.impl.query;

import com.buschmais.cdo.api.CdoException;
import com.buschmais.cdo.api.Query;
import com.buschmais.cdo.api.ResultIterator;
import com.buschmais.cdo.neo4j.impl.common.InstanceManager;
import com.buschmais.cdo.neo4j.spi.DatastoreSession;
import com.buschmais.cdo.neo4j.spi.TypeSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

public class CdoQueryImpl<QL> implements Query {

    private QL expression;
    private DatastoreSession datastoreSession;
    private InstanceManager instanceManager;
    private Collection<Class<?>> types;
    private Map<String, Object> parameters = null;

    public CdoQueryImpl(QL expression, DatastoreSession datastoreSession, InstanceManager instanceManager, Collection<Class<?>> types) {
        this.expression = expression;
        this.datastoreSession = datastoreSession;
        this.instanceManager = instanceManager;
        this.types = types;
    }

    @Override
    public Query withParameter(String name, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        Object oldValue = parameters.put(name, value);
        if (oldValue != null) {
            throw new CdoException("Parameter '" + name + "' has alread been assigned to value '" + value + "'.");
        }
        return this;
    }

    @Override
    public Query withParameters(Map<String, Object> parameters) {
        if (this.parameters != null) {
            throw new CdoException(("Parameters have already beed assigned: " + parameters));
        }
        this.parameters = parameters;
        return this;
    }

    @Override
    public <T> Result<T> execute() {
        Map<String, Object> effectiveParameters = new HashMap<>();
        if (parameters != null) {
            for (Map.Entry<String, Object> parameterEntry : parameters.entrySet()) {
                String name = parameterEntry.getKey();
                Object value = parameterEntry.getValue();
                if (instanceManager.isEntity(value)) {
                    value = instanceManager.getEntity(value);
                }
                effectiveParameters.put(name, value);
            }
        }
        ResultIterator<Map<String, Object>> iterator = datastoreSession.execute(expression, effectiveParameters);
        SortedSet<Class<?>> resultTypes = getResultTypes();
        return new QueryResultIterableImpl(instanceManager, iterator, resultTypes);
    }

    private TypeSet getResultTypes() {
        TypeSet resultTypes = new TypeSet();
        resultTypes.addAll(types);
        if (expression instanceof Class<?>) {
            resultTypes.add((Class<?>) expression);
        }
        return resultTypes;
    }
}