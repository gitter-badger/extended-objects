package com.buschmais.xo.spi.datastore;

import com.buschmais.xo.api.ResultIterator;
import com.buschmais.xo.spi.metadata.method.PrimitivePropertyMethodMetadata;
import com.buschmais.xo.spi.metadata.type.EntityTypeMetadata;

import java.util.Map;
import java.util.Set;

/**
 * Defines the interface for all entity related datastore operations.
 */
public interface DatastoreEntityManager<EntityId, Entity, EntityMetadata extends DatastoreEntityMetadata<EntityDiscriminator>, EntityDiscriminator, PropertyMetadata> extends DatastorePropertyManager<Entity, PropertyMetadata> {

    /**
     * Determine if the given object is an entity.
     *
     * @param o The object.
     * @return <code>true</code> if the object is an entity, <code>false</code> otherwise.
     */
    boolean isEntity(Object o);

    /**
     * Return the type discriminators of an entity.
     *
     * @param entity The entity.
     * @return The set of all type discriminators associated with the entity.
     */
    Set<EntityDiscriminator> getEntityDiscriminators(Entity entity);

    /**
     * Return the id of an entity.
     *
     * @param entity The entity.
     * @return The id of the entity.
     */
    EntityId getEntityId(Entity entity);

    /**
     * Create a new entity for the given types using a set of discriminators representing these types.
     *
     * @param types          The types.
     * @param discriminators The set of discriminators.
     * @return The created entity.
     */
    Entity createEntity(TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> types, Set<EntityDiscriminator> discriminators);

    /**
     * Delete an entity.
     *
     * @param entity The entity to deleteEntity.
     */
    void deleteEntity(Entity entity);

    /**
     * Find entities using given primitive property values.
     *
     * @param type          The type of the instances.
     * @param discriminator The discriminator to find the entities.
     * @param values        The primitive value.
     * @return An iterator returning matching entities.
     */
    ResultIterator<Entity> findEntity(EntityTypeMetadata<EntityMetadata> type, EntityDiscriminator discriminator, Map<PrimitivePropertyMethodMetadata<PropertyMetadata>, Object> values);

    /**
     * Migrate an entity of a given type and discriminators to the given target types and target discriminators.
     *
     * @param entity               The entity to migrate.
     * @param types                The entity types before migration.
     * @param discriminators       The discriminators of the entity before migration.
     * @param targetTypes          The entity types after migration.
     * @param targetDiscriminators The discriminators of the entity after migration.
     */
    void migrateEntity(Entity entity, TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> types, Set<EntityDiscriminator> discriminators, TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> targetTypes, Set<EntityDiscriminator> targetDiscriminators);

    /**
     * Flush the given entity to the datastore.
     *
     * @param entity The entity to flushEntity.
     */
    void flushEntity(Entity entity);

}
