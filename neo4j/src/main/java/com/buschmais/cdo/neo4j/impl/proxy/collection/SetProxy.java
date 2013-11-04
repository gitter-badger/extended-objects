package com.buschmais.cdo.neo4j.impl.proxy.collection;

import com.buschmais.cdo.neo4j.impl.proxy.InstanceManager;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.AbstractSet;
import java.util.Iterator;

public class SetProxy<E> extends AbstractSet<E> {

    private Node node;
    private RelationshipType relationshipType;
    private InstanceManager instanceManager;

    public SetProxy(Node node, RelationshipType relationshipType, InstanceManager instanceManager) {
        this.node = node;
        this.relationshipType = relationshipType;
        this.instanceManager = instanceManager;
    }

    @Override
    public Iterator<E> iterator() {
        Iterable<Relationship> relationships = node.getRelationships(relationshipType, Direction.OUTGOING);
        final Iterator<Relationship> iterator = relationships.iterator();
        return new Iterator<E>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                return instanceManager.getInstance(iterator.next().getEndNode());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove not supported");
            }
        };
    }

    @Override
    public int size() {
        int size = 0;
        for (Iterator<Relationship> iterator = node.getRelationships(relationshipType, Direction.OUTGOING).iterator(); iterator.hasNext(); ) {
            iterator.next();
            size++;
        }
        return size;
    }

    @Override
    public boolean add(E e) {
        if (contains(e)) {
            return false;
        }
        Node endNode = instanceManager.getNode(e);
        node.createRelationshipTo(endNode, relationshipType);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (instanceManager.isNode(o)) {
            Node endNode = instanceManager.getNode(o);
            for (Relationship relationship : node.getRelationships(relationshipType, Direction.OUTGOING)) {
                if (endNode.equals(relationship.getEndNode())) {
                    relationship.delete();
                    return true;
                }
            }
        }
        return false;
    }
}
