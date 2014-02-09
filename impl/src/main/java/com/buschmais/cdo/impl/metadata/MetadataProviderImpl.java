package com.buschmais.cdo.impl.metadata;

import com.buschmais.cdo.api.CdoException;
import com.buschmais.cdo.api.CompositeObject;
import com.buschmais.cdo.api.ResultIterable;
import com.buschmais.cdo.api.annotation.ImplementedBy;
import com.buschmais.cdo.api.annotation.ResultOf;
import com.buschmais.cdo.impl.MetadataProvider;
import com.buschmais.cdo.impl.reflection.BeanMethodProvider;
import com.buschmais.cdo.spi.annotation.EntityDefinition;
import com.buschmais.cdo.spi.annotation.IndexDefinition;
import com.buschmais.cdo.spi.annotation.QueryDefinition;
import com.buschmais.cdo.spi.annotation.RelationDefinition;
import com.buschmais.cdo.spi.datastore.*;
import com.buschmais.cdo.spi.metadata.method.*;
import com.buschmais.cdo.spi.metadata.type.EntityTypeMetadata;
import com.buschmais.cdo.spi.metadata.type.RelationTypeMetadata;
import com.buschmais.cdo.spi.metadata.type.SimpleTypeMetadata;
import com.buschmais.cdo.spi.metadata.type.TypeMetadata;
import com.buschmais.cdo.spi.reflection.AnnotatedMethod;
import com.buschmais.cdo.spi.reflection.AnnotatedType;
import com.buschmais.cdo.spi.reflection.PropertyMethod;
import com.buschmais.cdo.spi.reflection.UserMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.buschmais.cdo.api.Query.Result;

/**
 * Implementation of the {@link MetadataProvider}.
 *
 * @param <EntityMetadata>        The type of datastore specific entity metadata.
 * @param <EntityDiscriminator>   The type of datastore specific entity type discriminators.
 * @param <RelationMetadata>      The type of datastore specific relation metadata.
 * @param <RelationDiscriminator> The type of datastore specific relationtype discriminators.
 */
public class MetadataProviderImpl<EntityMetadata extends DatastoreEntityMetadata<EntityDiscriminator>, EntityDiscriminator, RelationMetadata extends DatastoreRelationMetadata<RelationDiscriminator>, RelationDiscriminator> implements MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityTypeMetadata.class);

    private final DatastoreMetadataFactory<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> metadataFactory;
    private final EntityTypeMetadataResolver<EntityMetadata, EntityDiscriminator> entityTypeMetadataResolver;
    private final RelationTypeMetadataResolver<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> relationTypeMetadataResolver;
    private final Map<Class<?>, Collection<AnnotatedMethod>> annotatedMethods;
    private final Map<Class<?>, TypeMetadata> metadataByType = new HashMap<>();

    /**
     * Constructor.
     *
     * @param types     All classes as provided by the CDO unit.
     * @param datastore The datastore.
     */
    public MetadataProviderImpl(Collection<Class<?>> types, Datastore<?, EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> datastore) {
        this.metadataFactory = datastore.getMetadataFactory();
        DependencyResolver.DependencyProvider<Class<?>> classDependencyProvider = new DependencyResolver.DependencyProvider<Class<?>>() {
            @Override
            public Set<Class<?>> getDependencies(Class<?> dependent) {
                return new HashSet<>(Arrays.asList(dependent.getInterfaces()));
            }
        };
        List<Class<?>> allClasses = DependencyResolver.newInstance(types, classDependencyProvider).resolve();
        LOGGER.debug("Processing types {}", allClasses);
        this.annotatedMethods = new HashMap<>();
        for (Class<?> currentClass : allClasses) {
            if (!currentClass.isInterface()) {
                throw new CdoException("Type " + currentClass.getName() + " is not an interface.");
            }
            annotatedMethods.put(currentClass, BeanMethodProvider.newInstance().getMethods(currentClass));
        }
        for (Class<?> currentClass : allClasses) {
            getOrCreateTypeMetadata(currentClass);
        }
        entityTypeMetadataResolver = new EntityTypeMetadataResolver<>(metadataByType);
        relationTypeMetadataResolver = new RelationTypeMetadataResolver<>(metadataByType);
        metadataByType.put(CompositeObject.class, new SimpleTypeMetadata(new AnnotatedType(CompositeObject.class), Collections.<TypeMetadata>emptyList(), Collections.<MethodMetadata<?, ?>>emptyList()));
    }

    @Override
    public TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> getTypes(Set<EntityDiscriminator> entityDiscriminators) {
        return entityTypeMetadataResolver.getTypes(entityDiscriminators);
    }

    @Override
    public TypeMetadataSet<RelationTypeMetadata<RelationMetadata>> getRelationTypes(Set<EntityDiscriminator> sourceDiscriminators, RelationDiscriminator discriminator, Set<EntityDiscriminator> targetDiscriminators) {
        return relationTypeMetadataResolver.getRelationTypes(sourceDiscriminators, discriminator, targetDiscriminators);
    }

    @Override
    public Set<EntityDiscriminator> getEntityDiscriminators(TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> types) {
        Set<EntityDiscriminator> entityDiscriminators = new HashSet<>();
        for (EntityTypeMetadata<EntityMetadata> entityTypeMetadata : types) {
            Set<EntityDiscriminator> discriminatorsOfType = entityTypeMetadataResolver.getDiscriminators(entityTypeMetadata);
            entityDiscriminators.addAll(discriminatorsOfType);
        }
        return entityDiscriminators;
    }

    @Override
    public Collection<TypeMetadata> getRegisteredMetadata() {
        return metadataByType.values();
    }

    @Override
    public EntityTypeMetadata<EntityMetadata> getEntityMetadata(Class<?> entityType) {
        return getMetadata(entityType, EntityTypeMetadata.class);
    }

    @Override
    public RelationTypeMetadata<RelationMetadata> getRelationMetadata(Class<?> relationType) {
        return getMetadata(relationType, RelationTypeMetadata.class);
    }

    @Override
    public RelationTypeMetadata.Direction getRelationDirection(Set<Class<?>> sourceTypes, RelationTypeMetadata<RelationMetadata> relationMetadata, Set<Class<?>> targetTypes) {
        if (sourceTypes.contains(relationMetadata.getFromType()) && targetTypes.contains(relationMetadata.getToType())) {
            return RelationTypeMetadata.Direction.FROM;
        } else if (targetTypes.contains(relationMetadata.getFromType()) && sourceTypes.contains(relationMetadata.getToType())) {
            return RelationTypeMetadata.Direction.TO;
        }
        throw new CdoException("The relation '" + relationMetadata + "' is not defined for the instances.");
    }

    @Override
    public <R> AbstractRelationPropertyMethodMetadata<?> getPropertyMetadata(Class<?> entityType, Class<R> relationType, RelationTypeMetadata.Direction direction) {
        return relationTypeMetadataResolver.getRelationPropertyMethodMetadata(entityType, getRelationMetadata(relationType), direction);
    }

    /**
     * Return the {@link TypeMetadata} for a given type.
     * <p>The metadata will be created if it does not exist yet.</p>
     *
     * @param type The type.
     * @return The {@link TypeMetadata}.
     */
    private TypeMetadata getOrCreateTypeMetadata(Class<?> type) {
        AnnotatedType annotatedType = new AnnotatedType(type);
        TypeMetadata typeMetadata = metadataByType.get(annotatedType.getAnnotatedElement());
        if (typeMetadata == null) {
            typeMetadata = createTypeMetadata(annotatedType);
            LOGGER.debug("Registering class {}", annotatedType.getName());
            metadataByType.put(annotatedType.getAnnotatedElement(), typeMetadata);
        }
        return typeMetadata;
    }

    /**
     * Create the {@link TypeMetadata} for the given {@link AnnotatedType}.
     *
     * @param annotatedType The {@link AnnotatedType}.
     * @return The corresponding metadata.
     */
    private TypeMetadata createTypeMetadata(AnnotatedType annotatedType) {
        Class<?> currentClass = annotatedType.getAnnotatedElement();
        List<TypeMetadata> superTypes = getSuperTypeMetadata(annotatedType);
        Collection<AnnotatedMethod> annotatedMethods = this.annotatedMethods.get(currentClass);
        Collection<MethodMetadata<?, ?>> methodMetadataOfType = getMethodMetadataOfType(annotatedType, annotatedMethods);
        TypeMetadata metadata;
        if (isEntityType(annotatedType)) {
            metadata = createEntityTypeMetadata(annotatedType, superTypes, methodMetadataOfType);
        } else if (isRelationType(annotatedType)) {
            metadata = createRelationTypeMetadata(annotatedType, superTypes, methodMetadataOfType);
        } else {
            metadata = new SimpleTypeMetadata(annotatedType, superTypes, methodMetadataOfType);
        }
        return metadata;
    }

    /**
     * Determines if an {@link AnnotatedType} represents an entity type.
     *
     * @param annotatedType The {@link AnnotatedType}.
     * @return <code>true</code> if the annotated type represents an entity type.
     */
    private boolean isEntityType(AnnotatedType annotatedType) {
        return isOfDefinitionType(annotatedType, EntityDefinition.class);
    }

    /**
     * Determines if an {@link AnnotatedType} represents a relation type.
     *
     * @param annotatedType The {@link AnnotatedType}.
     * @return <code>true</code> if the annotated type represents relation type.
     */
    private boolean isRelationType(AnnotatedType annotatedType) {
        return isOfDefinitionType(annotatedType, RelationDefinition.class);
    }

    /**
     * Determines if an {@link AnnotatedType} represents a specific type identified by a meta annotation.
     *
     * @param annotatedType  The {@link AnnotatedType}.
     * @param definitionType The meta annotation.
     * @return <code>true</code> if the annotated type represents relation type.
     */
    private boolean isOfDefinitionType(AnnotatedType annotatedType, Class<? extends Annotation> definitionType) {
        Annotation definition = annotatedType.getByMetaAnnotation(definitionType);
        if (definition != null) {
            return true;
        }
        for (Class<?> superType : annotatedType.getAnnotatedElement().getInterfaces()) {
            if (isOfDefinitionType(new AnnotatedType(superType), definitionType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a {@link EntityTypeMetadata} instance for the given {@link AnnotatedType}.
     *
     * @param annotatedType        The {@link AnnotatedType}.
     * @param superTypes           The metadata collection of the super types.
     * @param methodMetadataOfType The method metadata of the type.
     * @return The {@link EntityTypeMetadata} instance representing the annotated type.
     */
    private EntityTypeMetadata<EntityMetadata> createEntityTypeMetadata(AnnotatedType annotatedType, List<TypeMetadata> superTypes, Collection<MethodMetadata<?, ?>> methodMetadataOfType) {
        IndexedPropertyMethodMetadata indexedProperty = getIndexedPropertyMethodMetadata(methodMetadataOfType);
        EntityMetadata datastoreEntityMetadata = metadataFactory.createEntityMetadata(annotatedType, metadataByType);
        return new EntityTypeMetadata<>(annotatedType, superTypes, methodMetadataOfType, indexedProperty, datastoreEntityMetadata);
    }

    /**
     * Get or create a {@link RelationTypeMetadata} instance for the given {@link AnnotatedType}.
     *
     * @param annotatedType        The {@link AnnotatedType}.
     * @param superTypes           The metadata collection of the super types.
     * @param methodMetadataOfType The method metadata of the type.
     * @return The {@link RelationTypeMetadata} instance representing the annotated type.
     */
    private RelationTypeMetadata<RelationMetadata> createRelationTypeMetadata(AnnotatedType annotatedType, List<TypeMetadata> superTypes,
                                                                              Collection<MethodMetadata<?, ?>> methodMetadataOfType) {
        Class<?> fromType = null;
        Class<?> toType = null;
        Collection<MethodMetadata<?, ?>> current = methodMetadataOfType;
        Queue<TypeMetadata> queue = new LinkedList<>(superTypes);
        // Starting from the type to be created search all its properties and those of its super types for reference properties defining the from and to entity types
        do {
            for (MethodMetadata<?, ?> methodMetadata : current) {
                if (methodMetadata instanceof EntityReferencePropertyMethodMetadata) {
                    EntityReferencePropertyMethodMetadata<?> propertyMethodMetadata = (EntityReferencePropertyMethodMetadata<?>) methodMetadata;
                    Class<?> type = propertyMethodMetadata.getAnnotatedMethod().getType();
                    switch (propertyMethodMetadata.getDirection()) {
                        case FROM:
                            fromType = type;
                            break;
                        case TO:
                            toType = type;
                            break;
                        default:
                            throw propertyMethodMetadata.getDirection().createNotSupportedException();
                    }
                }
            }
            TypeMetadata superType = queue.poll();
            if (superType != null) {
                queue.addAll(superType.getSuperTypes());
                current = superType.getProperties();
            } else {
                current = null;
            }
        } while (current != null && fromType == null && toType == null);
        RelationMetadata relationMetadata = metadataFactory.createRelationMetadata(annotatedType, metadataByType);
        RelationTypeMetadata<RelationMetadata> relationTypeMetadata = new RelationTypeMetadata<>(annotatedType, superTypes, methodMetadataOfType, fromType, toType, relationMetadata);
        metadataByType.put(annotatedType.getAnnotatedElement(), relationTypeMetadata);
        return relationTypeMetadata;
    }

    /**
     * Returns a list of {@link TypeMetadata} representing the super types of the given annotated type.
     *
     * @param annotatedType The {@link AnnotatedType}.
     * @return The list of {@link TypeMetadata} representing the super types.
     */
    private List<TypeMetadata> getSuperTypeMetadata(AnnotatedType annotatedType) {
        List<TypeMetadata> superTypes = new ArrayList<>();
        for (Class<?> i : annotatedType.getAnnotatedElement().getInterfaces()) {
            superTypes.add(getOrCreateTypeMetadata(i));
        }
        return superTypes;
    }

    /**
     * Determine the indexed property from a list of method metadata.
     *
     * @param methodMetadataOfType The list of method metadata.
     * @return The {@link IndexedPropertyMethodMetadata}.
     */
    private IndexedPropertyMethodMetadata<?> getIndexedPropertyMethodMetadata(Collection<MethodMetadata<?, ?>> methodMetadataOfType) {
        IndexedPropertyMethodMetadata indexedProperty = null;
        for (MethodMetadata methodMetadata : methodMetadataOfType) {
            AnnotatedMethod annotatedMethod = methodMetadata.getAnnotatedMethod();
            Annotation indexedAnnotation = annotatedMethod.getByMetaAnnotation(IndexDefinition.class);
            if (indexedAnnotation != null) {
                if (!(methodMetadata instanceof PrimitivePropertyMethodMetadata)) {
                    throw new CdoException("Only primitive properties are allowed to be used for indexing.");
                }
                indexedProperty = new IndexedPropertyMethodMetadata<>((PropertyMethod) annotatedMethod, (PrimitivePropertyMethodMetadata) methodMetadata, metadataFactory.createIndexedPropertyMetadata((PropertyMethod) annotatedMethod));
            }
        }
        return indexedProperty;
    }

    /**
     * Return the collection of method metadata from the given collection of annotateed methods.
     *
     * @param annotatedMethods The collection of annotated methods.
     * @return The collection of method metadata.
     */
    private Collection<MethodMetadata<?, ?>> getMethodMetadataOfType(AnnotatedType annotatedType, Collection<AnnotatedMethod> annotatedMethods) {
        Collection<MethodMetadata<?, ?>> methodMetadataOfType = new ArrayList<>();
        // Collect the getter methods as they provide annotations holding meta information also to be applied to setters
        for (AnnotatedMethod annotatedMethod : annotatedMethods) {
            MethodMetadata<?, ?> methodMetadata;
            ResultOf resultOf = annotatedMethod.getAnnotation(ResultOf.class);
            ImplementedBy implementedBy = annotatedMethod.getAnnotation(ImplementedBy.class);
            if (implementedBy != null) {
                methodMetadata = new ImplementedByMethodMetadata<>(annotatedMethod, implementedBy.value(), metadataFactory.createImplementedByMetadata(annotatedMethod));
            } else if (resultOf != null) {
                methodMetadata = createResultOfMetadata(annotatedMethod, resultOf);
            } else if (annotatedMethod instanceof PropertyMethod) {
                methodMetadata = createPropertyMethodMetadata(annotatedType, (PropertyMethod) annotatedMethod);
            } else {
                methodMetadata = new UnsupportedOperationMethodMetadata((UserMethod) annotatedMethod);
            }
            methodMetadataOfType.add(methodMetadata);
        }
        return methodMetadataOfType;
    }

    private MethodMetadata<?, ?> createPropertyMethodMetadata(AnnotatedType annotatedType, PropertyMethod propertyMethod) {
        MethodMetadata<?, ?> methodMetadata;
        Class<?> propertyType = propertyMethod.getType();
        if (Collection.class.isAssignableFrom(propertyType)) {
            Type genericType = propertyMethod.getGenericType();
            ParameterizedType type = (ParameterizedType) genericType;
            Class<?> typeArgument = (Class<?>) type.getActualTypeArguments()[0];
            AnnotatedType annotatedTypeArgument = new AnnotatedType(typeArgument);
            if (isEntityType(annotatedTypeArgument)) {
                RelationTypeMetadata.Direction relationDirection = metadataFactory.getRelationDirection(propertyMethod);
                RelationTypeMetadata relationshipType = new RelationTypeMetadata<>(metadataFactory.createRelationMetadata(propertyMethod, metadataByType));
                methodMetadata = new EntityCollectionPropertyMethodMetadata<>(propertyMethod, relationshipType, relationDirection,
                        metadataFactory.createCollectionPropertyMetadata(propertyMethod));
            } else if (isRelationType(annotatedTypeArgument)) {
                TypeMetadata propertyTypeMetadata = getOrCreateTypeMetadata(typeArgument);
                RelationTypeMetadata<RelationMetadata> relationMetadata = (RelationTypeMetadata) propertyTypeMetadata;
                RelationTypeMetadata.Direction relationDirection = getRelationDirection(annotatedType, relationMetadata, propertyTypeMetadata);
                methodMetadata = new RelationCollectionPropertyMethodMetadata<>(propertyMethod, relationMetadata, relationDirection,
                        metadataFactory.createCollectionPropertyMetadata(propertyMethod));
            } else {
                throw new CdoException("Unsupported type argument '" + typeArgument.getName() + "' for collection property: " + propertyType.getName());
            }
        } else if (annotatedMethods.containsKey(propertyType)) {
            AnnotatedType referencedType = new AnnotatedType(propertyType);
            RelationTypeMetadata.Direction relationDirection;
            RelationTypeMetadata<RelationMetadata> relationMetadata;
            if (isEntityType(referencedType)) {
                relationDirection = metadataFactory.getRelationDirection(propertyMethod);
                relationMetadata = new RelationTypeMetadata<>(metadataFactory.createRelationMetadata(propertyMethod, metadataByType));
                methodMetadata = new EntityReferencePropertyMethodMetadata<>(propertyMethod, relationMetadata, relationDirection, metadataFactory.createReferencePropertyMetadata(propertyMethod));
            } else if (isRelationType(referencedType)) {
                TypeMetadata propertyTypeMetadata = getOrCreateTypeMetadata(propertyType);
                relationMetadata = (RelationTypeMetadata) propertyTypeMetadata;
                relationDirection = getRelationDirection(annotatedType, relationMetadata, propertyTypeMetadata);
                methodMetadata = new RelationReferencePropertyMethodMetadata<>(propertyMethod, relationMetadata, relationDirection, metadataFactory.createReferencePropertyMetadata(propertyMethod));
            } else {
                throw new CdoException("Unsupported type for reference property: " + propertyType.getName());
            }
        } else {
            methodMetadata = new PrimitivePropertyMethodMetadata<>(propertyMethod, metadataFactory.createPrimitivePropertyMetadata(propertyMethod));
        }
        return methodMetadata;
    }

    private RelationTypeMetadata.Direction getRelationDirection(AnnotatedType annotatedEntityType, RelationTypeMetadata<RelationMetadata> relationMetadata, TypeMetadata propertyTypeMetadata) {
        RelationTypeMetadata.Direction relationDirection;
        if (annotatedEntityType.getAnnotatedElement().equals(relationMetadata.getFromType())) {
            relationDirection = RelationTypeMetadata.Direction.FROM;
        } else if (annotatedEntityType.getAnnotatedElement().equals(relationMetadata.getToType())) {
            relationDirection = RelationTypeMetadata.Direction.TO;
        } else {
            throw new CdoException("Cannot determine relation direction for type '" + propertyTypeMetadata.getAnnotatedType().getName() + "'");
        }
        return relationDirection;
    }

    private MethodMetadata<?, ?> createResultOfMetadata(AnnotatedMethod annotatedMethod, ResultOf resultOf) {
        Method method = annotatedMethod.getAnnotatedElement();

        // Determine query type
        Class<?> methodReturnType = method.getReturnType();
        Class<?> returnType;
        if (Result.class.isAssignableFrom(methodReturnType)) {
            Type genericReturnType = method.getGenericReturnType();
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            returnType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        } else {
            returnType = methodReturnType;
        }

        // Determine query type
        AnnotatedElement query = resultOf.query();
        if (Object.class.equals(query)) {
            if (annotatedMethod.getByMetaAnnotation(QueryDefinition.class) != null) {
                query = annotatedMethod.getAnnotatedElement();
            } else {
                query = returnType;
            }
        }
        // Determine parameter bindings
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<ResultOf.Parameter> parameters = new ArrayList<>();
        for (Annotation[] parameterAnnotation : parameterAnnotations) {
            ResultOf.Parameter parameter = null;
            for (Annotation annotation : parameterAnnotation) {
                if (ResultOf.Parameter.class.equals(annotation.annotationType())) {
                    parameter = (ResultOf.Parameter) annotation;
                }
            }
            if (parameter == null) {
                throw new CdoException("Cannot determine parameter names for '" + method.getName() + "', all parameters must be annotated with '" + ResultOf.Parameter.class.getName() + "'.");
            }
            parameters.add(parameter);
        }
        boolean singleResult = !(Result.class.equals(methodReturnType) || ResultIterable.class.equals(methodReturnType) || (Iterable.class.equals(methodReturnType)));
        return new ResultOfMethodMetadata<>(annotatedMethod, query, returnType, resultOf.usingThisAs(), parameters, singleResult);
    }

    private <T extends TypeMetadata> T getMetadata(Class<?> type, Class<T> metadataType) {
        TypeMetadata typeMetadata = metadataByType.get(type);
        if (typeMetadata == null) {
            throw new CdoException("Cannot resolve metadata for type " + type.getName() + ".");
        }
        if (!metadataType.isAssignableFrom(typeMetadata.getClass())) {
            throw new CdoException("Expected metadata of type '" + metadataType.getName() + "' but got '" + typeMetadata.getClass() + "' for type '" + type + "'");
        }
        return metadataType.cast(typeMetadata);
    }
}
