package com.buschmais.xo.impl.proxy.example;

import com.buschmais.xo.api.proxy.ProxyMethod;
import com.buschmais.xo.impl.SessionContext;
import com.buschmais.xo.impl.proxy.AbstractProxyMethodService;
import com.buschmais.xo.spi.datastore.DatastoreEntityMetadata;
import com.buschmais.xo.spi.metadata.method.MethodMetadata;
import com.buschmais.xo.spi.metadata.method.PrimitivePropertyMethodMetadata;
import com.buschmais.xo.spi.metadata.type.EntityTypeMetadata;
import com.buschmais.xo.spi.reflection.AnnotatedMethod;
import com.buschmais.xo.spi.reflection.SetPropertyMethod;

import java.util.Map;

public class ExampleProxyMethodService<Entity> extends AbstractProxyMethodService<Map<PrimitivePropertyMethodMetadata<?>, Object>> {

    public ExampleProxyMethodService(Class<?> type, SessionContext<?, Entity, ?, ?, ?, ?, ?, ?, ?> sessionContext) {
        EntityTypeMetadata<? extends DatastoreEntityMetadata<?>> entityMetadata = sessionContext.getMetadataProvider().getEntityMetadata(type);
        for (final MethodMetadata<?, ?> methodMetadata : entityMetadata.getProperties()) {
            if (methodMetadata instanceof PrimitivePropertyMethodMetadata) {
                AnnotatedMethod method = methodMetadata.getAnnotatedMethod();
                if (method instanceof SetPropertyMethod) {
                    addProxyMethod(new ProxyMethod<Map<PrimitivePropertyMethodMetadata<?>, Object>>() {
                        @Override
                        public Object invoke(Map<PrimitivePropertyMethodMetadata<?>, Object> properties, Object instance, Object[] args) throws Exception {
                            properties.put((PrimitivePropertyMethodMetadata<?>) methodMetadata, args[0]);
                            return null;
                        }
                    }, method.getAnnotatedElement());
                }
            }
        }
    }
}
