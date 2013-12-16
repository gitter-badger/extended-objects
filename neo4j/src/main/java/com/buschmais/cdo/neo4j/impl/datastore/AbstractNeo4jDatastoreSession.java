package com.buschmais.cdo.neo4j.impl.datastore;

import com.buschmais.cdo.api.CdoException;
import com.buschmais.cdo.api.ResultIterator;
import com.buschmais.cdo.neo4j.api.annotation.Cypher;
import com.buschmais.cdo.neo4j.impl.datastore.metadata.NodeMetadata;
import com.buschmais.cdo.neo4j.impl.datastore.metadata.PrimitivePropertyMetadata;
import com.buschmais.cdo.spi.datastore.DatastorePropertyManager;
import com.buschmais.cdo.spi.datastore.DatastoreSession;
import com.buschmais.cdo.spi.datastore.TypeSet;
import com.buschmais.cdo.spi.metadata.IndexedPropertyMethodMetadata;
import com.buschmais.cdo.spi.metadata.MetadataProvider;
import com.buschmais.cdo.spi.metadata.PrimitivePropertyMethodMetadata;
import com.buschmais.cdo.spi.metadata.TypeMetadata;
import org.neo4j.graphdb.*;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractNeo4jDatastoreSession<GDS extends GraphDatabaseService> implements DatastoreSession<Long, Node, Label, Long, Relationship> {

    private GDS graphDatabaseService;
    private MetadataProvider metadataProvider;
    private Neo4jPropertyManager propertyManager;

    public AbstractNeo4jDatastoreSession(GDS graphDatabaseService, MetadataProvider metadataProvider) {
        this.graphDatabaseService = graphDatabaseService;
        this.metadataProvider = metadataProvider;
        this.propertyManager = new Neo4jPropertyManager();
    }

    @Override
    public DatastorePropertyManager getDatastorePropertyManager() {
        return propertyManager;
    }

    public GDS getGraphDatabaseService() {
        return graphDatabaseService;
    }

    @Override
    public Node create(TypeSet types, Set<Label> discriminators) {
        Node node = getGraphDatabaseService().createNode();
        for (Label label : discriminators) {
            node.addLabel(label);
        }
        return node;
    }

    @Override
    public ResultIterator<Node> find(Class<?> type, Object value) {
        TypeMetadata<NodeMetadata> typeMetadata = metadataProvider.getEntityMetadata(type);
        Label label = typeMetadata.getDatastoreMetadata().getDiscriminator();
        if (label == null) {
            throw new CdoException("Type " + type.getName() + " has no label.");
        }
        IndexedPropertyMethodMetadata<?> indexedProperty = typeMetadata.getDatastoreMetadata().getIndexedProperty();
        if (indexedProperty == null) {
            indexedProperty = typeMetadata.getIndexedProperty();
        }
        if (indexedProperty == null) {
            throw new CdoException("Type " + typeMetadata.getType().getName() + " has no indexed property.");
        }
        PrimitivePropertyMethodMetadata<PrimitivePropertyMetadata> propertyMethodMetadata = indexedProperty.getPropertyMethodMetadata();
        ResourceIterable<Node> nodesByLabelAndProperty = getGraphDatabaseService().findNodesByLabelAndProperty(label, propertyMethodMetadata.getDatastoreMetadata().getName(), value);
        ResourceIterator<Node> iterator = nodesByLabelAndProperty.iterator();
        return new ResourceResultIterator(iterator);
    }

    @Override
    public void migrate(Node entity, TypeSet types, TypeSet targetTypes) {
        Set<Label> labels = metadataProvider.getDiscriminators(types);
        Set<Label> targetLabels = metadataProvider.getDiscriminators(targetTypes);
        Set<Label> labelsToRemove = new HashSet<>(labels);
        labelsToRemove.removeAll(targetLabels);
        for (Label label : labelsToRemove) {
            entity.removeLabel(label);
        }
        Set<Label> labelsToAdd = new HashSet<>(targetLabels);
        labelsToAdd.removeAll(labels);
        for (Label label : labelsToAdd) {
            entity.addLabel(label);
        }
    }

    @Override
    public boolean isEntity(Object o) {
        return Node.class.isAssignableFrom(o.getClass());
    }

    @Override
    public Long getId(Node entity) {
        return Long.valueOf(entity.getId());
    }

    @Override
    public void delete(Node node) {
        node.delete();
    }

    @Override
    public void flush(Iterable<Node> nodes) {
    }

    protected <QL> String getCypher(QL expression) {
        if (expression instanceof String) {
            return (String) expression;
        } else if (expression instanceof Class<?>) {
            Class<?> typeExpression = (Class) expression;
            Cypher cypher = typeExpression.getAnnotation(Cypher.class);
            if (cypher == null) {
                throw new CdoException(typeExpression.getName() + " must be annotated with " + Cypher.class.getName());
            }
            return cypher.value();
        }
        throw new CdoException("Unsupported query expression " + expression);
    }

    @Override
    public Set<Label> getDiscriminators(Node node) {
        Set<Label> labels = new HashSet<>();
        for (Label label : node.getLabels()) {
            labels.add(label);
        }
        return labels;
    }
}
