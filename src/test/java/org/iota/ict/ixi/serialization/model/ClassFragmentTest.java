package org.iota.ict.ixi.serialization.model;

import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassFragmentTest  {

    @Test
    public void buildSimpleClassFragmentTest(){
        ClassFragment.Builder builder = new ClassFragment.Builder();
        builder.withDataSize(25);
        ClassFragment classFragment = builder.build();
        assertEquals(25, classFragment.getDataSize());
        assertEquals(0, classFragment.getRefCount());

        builder = new ClassFragment.Builder();
        builder.withDataSize(26);
        ClassFragment classFragment2 = builder.build();
        assertEquals(26, classFragment2.getDataSize());
        assertEquals(0, classFragment2.getRefCount());

        assertNotEquals(classFragment.getClassHash(), classFragment2.getClassHash());
        assertTrue(classFragment.hasHeadFlag(classFragment.getHeadTransaction()));
        assertTrue(classFragment.hasTailFlag(classFragment.getTailTransaction()));
        assertTrue(classFragment.hasTailFlag(classFragment.getHeadTransaction()));
        assertTrue(classFragment.hasHeadFlag(classFragment.getTailTransaction()));
    }

    @Test
    public void buildClassFragmentWithOneRefTest(){
        ClassFragment.Builder builder = new ClassFragment.Builder();
        builder.withDataSize(25);
        ClassFragment classFragment = builder.build();

        builder = new ClassFragment.Builder();
        builder.withDataSize(0);
        builder.addReferencedClass(classFragment);
        ClassFragment classFragment2 = builder.build();
        assertEquals(0, classFragment2.getDataSize());
        assertEquals(1, classFragment2.getRefCount());
        assertEquals(classFragment.getClassHash(), classFragment2.getClassHashForReference(0));
        assertTrue(classFragment.hasHeadFlag(classFragment.getHeadTransaction()));
        assertTrue(classFragment.hasTailFlag(classFragment.getTailTransaction()));
        assertTrue(classFragment.hasTailFlag(classFragment.getHeadTransaction()));
        assertTrue(classFragment.hasHeadFlag(classFragment.getTailTransaction()));
    }
    @Test
    public void buildClassFragmentWithTwoRefTest(){
        ClassFragment.Builder builder = new ClassFragment.Builder();
        builder.withDataSize(25);
        ClassFragment classFragment = builder.build();
//        assertEquals(classFragment.getClassHash(), classFragment.getHeadTransaction().address());

        builder = new ClassFragment.Builder();
 //       builder.withDataSize(5);
        builder.addReferencedClass(classFragment);
        builder.addReferencedClass(classFragment);
        ClassFragment classFragment2 = builder.build();
 //       assertEquals(5, classFragment2.getDataSize());
        assertEquals(2, classFragment2.getRefCount());
//        assertEquals(classFragment2.getClassHash(), classFragment2.getHeadTransaction().address());
        assertEquals(classFragment.getClassHash(), classFragment2.getClassHashForReference(0));
        assertEquals(classFragment.getClassHash(), classFragment2.getClassHashForReference(1));
        assertEquals(Trytes.NULL_HASH, classFragment2.getClassHashForReference(2));
        assertTrue(classFragment2.hasHeadFlag(classFragment2.getHeadTransaction()));
        assertTrue(classFragment2.hasTailFlag(classFragment2.getTailTransaction()));
        assertFalse(classFragment2.hasHeadFlag(classFragment2.getTailTransaction()));
        assertFalse(classFragment2.hasTailFlag(classFragment2.getHeadTransaction()));
    }

    @Test
    public void buildClassFragmentWithTreeRefTest(){
        ClassFragment.Builder builder = new ClassFragment.Builder();
        builder.withDataSize(25);
        ClassFragment classFragment = builder.build();

        builder = new ClassFragment.Builder();
        builder.withDataSize(0);
        builder.addReferencedClass(classFragment);
        builder.addReferencedClass(classFragment);
        builder.addReferencedClass(classFragment);
        ClassFragment classFragment2 = builder.build();
        assertNotNull(classFragment2.getClassHash());
        assertEquals(0, classFragment2.getDataSize());
        assertEquals(3, classFragment2.getRefCount());
        assertEquals(classFragment.getClassHash(), classFragment2.getClassHashForReference(0));
        assertEquals(classFragment.getClassHash(), classFragment2.getClassHashForReference(1));
        assertEquals(classFragment.getClassHash(), classFragment2.getClassHashForReference(2));
        assertEquals(Trytes.NULL_HASH, classFragment2.getClassHashForReference(3));
    }
}
