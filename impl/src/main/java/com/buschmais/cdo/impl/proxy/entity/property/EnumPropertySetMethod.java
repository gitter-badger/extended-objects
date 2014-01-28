package com.buschmais.cdo.impl.proxy.entity.property;

import com.buschmais.cdo.impl.AbstractPropertyManager;
import com.buschmais.cdo.impl.SessionContext;
import com.buschmais.cdo.impl.proxy.common.property.AbstractPropertyMethod;
import com.buschmais.cdo.spi.metadata.method.EnumPropertyMethodMetadata;

public class EnumPropertySetMethod<Entity, Relation> extends AbstractPropertyMethod<Entity,Entity,Relation,EnumPropertyMethodMetadata> {

    public EnumPropertySetMethod(SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?> sessionContext, EnumPropertyMethodMetadata metadata) {
        super(sessionContext, metadata);
    }

    @Override
    protected AbstractPropertyManager<Entity, Entity, Relation> getPropertyManager() {
        return getSessionContext().getEntityPropertyManager();
    }

    @Override
    public Object invoke(Entity entity, Object instance, Object[] args) {
        Enum<?> value = (Enum<?>) args[0];
        getSessionContext().getEntityPropertyManager().setEnumProperty(entity, getMetadata(), value);
        return null;
    }
}
