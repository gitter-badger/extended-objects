package com.buschmais.cdo.neo4j.test.relation.typed.composite;

import com.buschmais.cdo.neo4j.api.annotation.Relation;

import static com.buschmais.cdo.neo4j.api.annotation.Relation.Incoming;
import static com.buschmais.cdo.neo4j.api.annotation.Relation.Outgoing;

@Relation("ManyToMany")
public interface TypedManyToManyRelation extends TypedRelation {

    @Outgoing
    A getA();

    @Incoming
    B getB();

}