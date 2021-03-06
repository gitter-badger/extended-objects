package com.buschmais.xo.neo4j.test.validation.composite;

import com.buschmais.xo.neo4j.api.annotation.Indexed;
import com.buschmais.xo.neo4j.api.annotation.Label;

import javax.validation.constraints.NotNull;

@Label("A")
public interface A {

    @NotNull
    @Indexed
    String getName();

    void setName(String name);

    @NotNull
    B getB();

    void setB(B b);
}
