package com.buschmais.xo.neo4j.test.relation.qualified.composite;

import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Relation("OneToOne")
@Retention(RUNTIME)
public @interface QualifiedOneToOne {
}
