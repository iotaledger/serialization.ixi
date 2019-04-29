package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.TestUtils;
import org.junit.jupiter.api.Test;

import static org.iota.ict.utils.Trytes.NULL_HASH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataFragmentTest {

    @Test
    public void buildDataFragmentTest(){
        ClassFragment classFragment = new ClassFragment.Builder().addReferencedClasshash(TestUtils.random(81)).build();
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        String ref0 = TestUtils.randomHash();
        builder.setReference(0, ref0);
        DataFragment dataFragment = builder.build();
        assertEquals(ref0, dataFragment.getHeadTransaction().extraDataDigest());
        assertEquals(ref0, dataFragment.getReference(0));
        assertEquals(classFragment.getClassHash(), dataFragment.getHeadTransaction().address());
        assertTrue(dataFragment.hasHeadFlag(dataFragment.getHeadTransaction()));
        assertTrue(dataFragment.hasHeadFlag(dataFragment.getTailTransaction()));
        assertTrue(dataFragment.hasTailFlag(dataFragment.getHeadTransaction()));
        assertTrue(dataFragment.hasTailFlag(dataFragment.getTailTransaction()));
    }

    @Test
    public void buildDataFragmentTest2(){
        ClassFragment classFragment = new ClassFragment.Builder()
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        String ref0 = TestUtils.randomHash();
        String ref1 = TestUtils.randomHash();
        builder.setReference(0, ref0);
        builder.setReference(1, ref1);
        DataFragment dataFragment = builder.build();
        assertEquals(ref0, dataFragment.getHeadTransaction().extraDataDigest());
        assertEquals(ref0, dataFragment.getReference(0));
        assertEquals(ref1, dataFragment.getHeadTransaction().getTrunk().address());
        assertEquals(ref1, dataFragment.getReference(1));
        assertEquals(classFragment.getClassHash(), dataFragment.getClassHash());
        assertTrue(dataFragment.hasHeadFlag(dataFragment.getHeadTransaction()));
        assertFalse(dataFragment.hasHeadFlag(dataFragment.getTailTransaction()));
        assertFalse(dataFragment.hasTailFlag(dataFragment.getHeadTransaction()));
        assertTrue(dataFragment.hasTailFlag(dataFragment.getTailTransaction()));
    }


    @Test
    public void buildDataFragmentTest3(){
        ClassFragment classFragment = new ClassFragment.Builder()
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        String ref0 = TestUtils.randomHash();
        String ref1 = TestUtils.randomHash();
        builder.setReference(0, ref0);
        builder.setReference(2, ref1);
        DataFragment dataFragment = builder.build();
        assertEquals(ref0, dataFragment.getHeadTransaction().extraDataDigest());
        assertEquals(ref0, dataFragment.getReference(0));
        assertEquals(ref1, dataFragment.getHeadTransaction().getTrunk().extraDataDigest());
        assertEquals(ref1, dataFragment.getReference(2));
        assertEquals(NULL_HASH, dataFragment.getReference(1));
        assertEquals(classFragment.getClassHash(), dataFragment.getClassHash());
    }
}
