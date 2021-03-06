package com.buschmais.xo.impl.proxy.common.resultof;

import com.buschmais.xo.api.Query;
import com.buschmais.xo.api.XOException;
import com.buschmais.xo.api.annotation.ResultOf;
import com.buschmais.xo.api.proxy.ProxyMethod;
import com.buschmais.xo.impl.AbstractInstanceManager;
import com.buschmais.xo.impl.SessionContext;
import com.buschmais.xo.impl.query.XOQueryImpl;
import com.buschmais.xo.spi.metadata.method.ResultOfMethodMetadata;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.util.List;

public abstract class AbstractResultOfMethod<DatastoreType, Entity, Relation> implements ProxyMethod<DatastoreType> {

    private final SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?, ?> sessionContext;
    private final ResultOfMethodMetadata<?> resultOfMethodMetadata;

    public AbstractResultOfMethod(SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?, ?> sessionContext, ResultOfMethodMetadata<?> resultOfMethodMetadata) {
        this.sessionContext = sessionContext;
        this.resultOfMethodMetadata = resultOfMethodMetadata;
    }

    @Override
    public Object invoke(DatastoreType datastoreType, Object instance, Object[] args) {
        XOQueryImpl<?, ?, AnnotatedElement, ?, ?> query = new XOQueryImpl<>(sessionContext, resultOfMethodMetadata.getQuery(), resultOfMethodMetadata.getReturnType());
        String usingThisAs = resultOfMethodMetadata.getUsingThisAs();
        query.withParameter(usingThisAs, getInstanceManager(sessionContext).readInstance(datastoreType));
        List<ResultOf.Parameter> parameters = resultOfMethodMetadata.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            query.withParameter(parameters.get(i).value(), args[i]);
        }
        Query.Result<?> result = query.execute();
        if (void.class.equals(resultOfMethodMetadata.getReturnType())) {
            try {
                result.close();
            } catch (IOException e) {
                throw new XOException("Cannot close query result.", e);
            }
        } else if (resultOfMethodMetadata.isSingleResult()) {
            return result.hasResult() ? result.getSingleResult() : null;
        }
        return result;
    }

    protected abstract AbstractInstanceManager<?, DatastoreType> getInstanceManager(SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?, ?> sessionContext);
}
