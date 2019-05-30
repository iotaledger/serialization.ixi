package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.TestUtils;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess")
public class ClassFragmentTest  {

    @Test
    public void buildSimpleClassFragmentTest(){
        ClassFragment.Builder builder = new ClassFragment.Builder(TestUtils.random(9));
        builder.addAttribute(24,"");
        ClassFragment classFragment = builder.build();
        assertEquals(24, classFragment.getDataSize());
        assertEquals(0, classFragment.getRefCount());

        builder = new ClassFragment.Builder(TestUtils.random(27));
        builder.addAttribute(27, TestUtils.random(10));
        ClassFragment classFragment2 = builder.build();
        assertEquals(27, classFragment2.getDataSize());
        assertEquals(0, classFragment2.getRefCount());

        assertNotEquals(classFragment.getClassHash(), classFragment2.getClassHash());
        assertTrue(classFragment.hasHeadFlag(classFragment.getHeadTransaction()));
        assertTrue(classFragment.hasTailFlag(classFragment.getTailTransaction()));
        assertTrue(classFragment.hasTailFlag(classFragment.getHeadTransaction()));
        assertTrue(classFragment.hasHeadFlag(classFragment.getTailTransaction()));
    }

    @Test
    public void buildClassFragmentWithOneRefTest(){
        ClassFragment.Builder builder = new ClassFragment.Builder(TestUtils.random(9));
        ClassFragment classFragment = builder.build();

        builder = new ClassFragment.Builder(TestUtils.random(9))
                .addReferencedClass(classFragment);
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
        ClassFragment.Builder builder = new ClassFragment.Builder(TestUtils.random(9));
        builder.addAttribute(25, TestUtils.random(10));
        ClassFragment classFragment = builder.build();

        builder = new ClassFragment.Builder(TestUtils.random(9));
        builder.addReferencedClass(classFragment);
        builder.addReferencedClass(classFragment);
        ClassFragment classFragment2 = builder.build();
        assertEquals(2, classFragment2.getRefCount());
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
        ClassFragment.Builder builder = new ClassFragment.Builder(TestUtils.random(9));
        builder.addAttribute(25, TestUtils.random(10));
        ClassFragment classFragment = builder.build();

        builder = new ClassFragment.Builder(TestUtils.random(9));
        builder.addAttribute(0, TestUtils.random(10));
        builder.addReferencedClass(classFragment);
        builder.addReferencedClass(classFragment);
        builder.addReferencedClass(classFragment);
        ClassFragment classFragment2 = builder.build();
        assertNotNull(classFragment2.getClassHash());
        assertEquals(6, classFragment2.getDataSize());
        assertEquals(3, classFragment2.getRefCount());
        assertEquals(classFragment.getClassHash(), classFragment2.getClassHashForReference(0));
        assertEquals(classFragment.getClassHash(), classFragment2.getClassHashForReference(1));
        assertEquals(classFragment.getClassHash(), classFragment2.getClassHashForReference(2));
        assertEquals(Trytes.NULL_HASH, classFragment2.getClassHashForReference(3));
    }

    @Test
    public void buildClassFragmentWithVariableSizeAttributes(){
        ClassFragment.Builder builder = new ClassFragment.Builder(TestUtils.random(9));
        builder.addAttribute(25, TestUtils.random(10));
        builder.addAttribute(25, TestUtils.random(10));
        builder.addAttribute(0, TestUtils.random(10));
        ClassFragment classFragment = builder.build();
        assertEquals(56, classFragment.getDataSize());
    }
}
