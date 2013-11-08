package com.buschmais.cdo.neo4j.test.composite.basic;

import com.buschmais.cdo.api.CdoManager;
import com.buschmais.cdo.api.CdoManagerFactory;
import com.buschmais.cdo.neo4j.impl.EmbeddedNeo4jCdoManagerFactoryImpl;
import com.buschmais.cdo.neo4j.test.composite.AbstractCdoManagerTest;
import com.buschmais.cdo.neo4j.test.composite.basic.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class BasicMappingTest extends AbstractCdoManagerTest {

    private CdoManager cdoManager;

    @Override
    protected Class<?>[] getTypes() {
        return new Class<?>[]{A.class, B.class, C.class, D.class};
    }

    @Before
    public void before() {
        cdoManager = getCdoManagerFactory().createCdoManager();
        cdoManager.begin();
        cdoManager.executeQuery("MATCH (n)-[r]-(d) DELETE r");
        cdoManager.executeQuery("MATCH (n) DELETE n");
        cdoManager.commit();
    }

    @Test
    public void primitiveProperty() {
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        a.setString("value");
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getString(), equalTo("value"));
        a.setString("updatedValue");
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getString(), equalTo("updatedValue"));
        a.setString(null);
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getString(), equalTo(null));
        cdoManager.commit();
    }

    @Test
    public void enumerationProperty() {
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        a.setEnumeratedValue(Enumeration.FIRST);
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getEnumeratedValue(), equalTo(Enumeration.FIRST));
        a.setEnumeratedValue(Enumeration.SECOND);
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getEnumeratedValue(), equalTo(Enumeration.SECOND));
        a.setEnumeratedValue(null);
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getEnumeratedValue(), equalTo(null));
        cdoManager.commit();
    }

    @Test
    public void referenceProperty() {
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        B b1 = cdoManager.create(B.class);
        B b2 = cdoManager.create(B.class);
        a.setB(b1);
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getB(), equalTo(b1));
        a.setB(b2);
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getB(), equalTo(b2));
        a.setB(null);
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getB(), equalTo(null));
        cdoManager.commit();
    }

    @Test
    public void collectionProperty() {
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        B b = cdoManager.create(B.class);
        Set<B> setOfB = a.getSetOfB();
        assertThat(setOfB.add(b), equalTo(true));
        assertThat(setOfB.add(b), equalTo(false));
        assertThat(setOfB.size(), equalTo(1));
        cdoManager.commit();
        cdoManager.begin();
        assertThat(setOfB.remove(b), equalTo(true));
        assertThat(setOfB.remove(b), equalTo(false));
        cdoManager.commit();
    }

    @Test
    public void indexedProperty() {
        cdoManager.begin();
        A a1 = cdoManager.create(A.class);
        a1.setIndex("1");
        A a2 = cdoManager.create(A.class);
        a2.setIndex("2");
        cdoManager.commit();
        cdoManager.begin();
        assertThat(cdoManager.find(A.class, "1").iterator().next(), equalTo(a1));
        assertThat(cdoManager.find(A.class, "2").iterator().next(), equalTo(a2));
        assertThat(cdoManager.find(A.class, "3").iterator().hasNext(), equalTo(false));
        cdoManager.commit();
    }

    @Test
    public void useIndexOf() {
        cdoManager.begin();
        A a1 = cdoManager.create(D.class);
        a1.setIndex("1");
        cdoManager.commit();
        cdoManager.begin();
        assertThat(cdoManager.find(D.class, "1").iterator().next(), equalTo(a1));
        cdoManager.commit();
    }

    @Test
    public void anonymousSuperclass() {
        cdoManager.begin();
        C c = cdoManager.create(C.class);
        c.setIndex("1");
        c.setVersion(1);
        cdoManager.commit();
        cdoManager.begin();
        A a = cdoManager.find(A.class, "1").iterator().next();
        assertThat(c, equalTo(a));
        assertThat(a.getVersion(), equalTo(1L));
        cdoManager.commit();
    }

}
