package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetadataFragmentTest {

    @Test
    public void simpleMetadataFragmentTest(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(false,48,"a simple label");
        builder.appendField(FieldDescriptor.withAsciiLabel(false,48,"a simple label"));
        MetadataFragment metadataFragment = builder.build();
        String expected = MetadataFragment.METADATA_LANGUAGE_VERSION+descriptor.toTrytes();
        expected = Trytes.padRight(expected, Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        assertEquals(expected,metadataFragment.getHeadTransaction().signatureFragments());
        String expectedTag = Trytes.padRight("9D",Transaction.Field.TAG.tryteLength);
        assertEquals(expectedTag, metadataFragment.getHeadTransaction().tag());

        assertTrue(metadataFragment.hasHeadFlag(metadataFragment.getHeadTransaction()));
        assertTrue(metadataFragment.hasTailFlag(metadataFragment.getTailTransaction()));
    }

    @Test
    public void simpleMetadataFragmentWith40FieldTest(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(false,48,"a simple label");
        for(int i=0; i<40 ; i++){
            builder.appendField(descriptor);
        }
        MetadataFragment metadataFragment = builder.build();
        String expected = MetadataFragment.METADATA_LANGUAGE_VERSION;
        for(int i=0; i<40 ; i++){
            expected+=descriptor.toTrytes();
        }
        expected = Trytes.padRight(expected,Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        assertEquals(expected,metadataFragment.getHeadTransaction().signatureFragments());
        String expectedTag = Trytes.padRight("9D",Transaction.Field.TAG.tryteLength);
        assertEquals(expectedTag, metadataFragment.getHeadTransaction().tag());

        assertTrue(metadataFragment.hasHeadFlag(metadataFragment.getHeadTransaction()));
        assertTrue(metadataFragment.hasTailFlag(metadataFragment.getTailTransaction()));
        assertEquals(40, metadataFragment.getKeyCount());
    }

    @Test
    public void simpleMetadataFragmentWith42FieldTest(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(false,48,"a simple label");
        for(int i=0; i<42 ; i++){
            builder.appendField(descriptor);
        }
        MetadataFragment metadataFragment = builder.build();
        String expected = MetadataFragment.METADATA_LANGUAGE_VERSION;
        for(int i=0; i<42 ; i++){
            expected+=descriptor.toTrytes();
        }
        expected = expected.substring(0,Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        assertEquals(expected,metadataFragment.getHeadTransaction().signatureFragments());
        String expectedTag = Trytes.padRight("9C",Transaction.Field.TAG.tryteLength);
        assertEquals(expectedTag, metadataFragment.getHeadTransaction().tag());
        expectedTag = Trytes.padRight("9A",Transaction.Field.TAG.tryteLength);
        assertEquals(expectedTag, metadataFragment.getTailTransaction().tag());

        assertTrue(metadataFragment.hasHeadFlag(metadataFragment.getHeadTransaction()));
        assertTrue(metadataFragment.hasTailFlag(metadataFragment.getTailTransaction()));
        assertEquals(42, metadataFragment.getKeyCount());
    }

    @Test
    public void simpleMetadataFragmentWith81FieldTest(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(false,48,"a simple label");
        for(int i=0; i<85 ; i++){
            builder.appendField(descriptor);
        }
        MetadataFragment metadataFragment = builder.build();
        String expected = MetadataFragment.METADATA_LANGUAGE_VERSION;
        for(int i=0; i<42 ; i++){
            expected+=descriptor.toTrytes();
        }
        expected = expected.substring(0,Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        assertEquals(expected,metadataFragment.getHeadTransaction().signatureFragments());
        String expectedTag = Trytes.padRight("9C",Transaction.Field.TAG.tryteLength);
        assertEquals(expectedTag, metadataFragment.getHeadTransaction().tag());
        expectedTag = Trytes.padRight("9A",Transaction.Field.TAG.tryteLength);
        assertEquals(expectedTag, metadataFragment.getTailTransaction().tag());
        expectedTag = Trytes.padRight("9",Transaction.Field.TAG.tryteLength);
        assertEquals(expectedTag, metadataFragment.getHeadTransaction().getTrunk().tag());

        assertTrue(metadataFragment.hasHeadFlag(metadataFragment.getHeadTransaction()));
        assertTrue(metadataFragment.hasTailFlag(metadataFragment.getTailTransaction()));
        assertEquals(85, metadataFragment.getKeyCount());
    }

    @Test
    public void invalidInputCheck(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        Exception exception = assertThrows(IllegalStateException.class, () ->
                builder.build());
        assertEquals("Cannot build metadata fragment with no fields", exception.getMessage());

    }

    @Test
    public void metadataFragmentFromClassTest(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        MetadataFragment metadataFragment = builder.fromClass(SampleSerializableClass.class).build();

        String expectedTag = Trytes.padRight("9D",Transaction.Field.TAG.tryteLength);
        assertEquals(expectedTag, metadataFragment.getHeadTransaction().tag());

        assertTrue(metadataFragment.hasHeadFlag(metadataFragment.getHeadTransaction()));
        assertTrue(metadataFragment.hasTailFlag(metadataFragment.getTailTransaction()));

        assertEquals(5, metadataFragment.getKeyCount());
        FieldDescriptor descriptor0 = metadataFragment.getDescriptor(0);
        FieldDescriptor descriptor1 = metadataFragment.getDescriptor(1);
        FieldDescriptor descriptor2 = metadataFragment.getDescriptor(2);
        FieldDescriptor descriptor3 = metadataFragment.getDescriptor(3);
        FieldDescriptor descriptor4 = metadataFragment.getDescriptor(4);

        assertEquals(false,descriptor0.isList());
        assertEquals(false,descriptor1.isList());
        assertEquals(false,descriptor2.isList());
        assertEquals(true,descriptor3.isList());
        assertEquals(false,descriptor4.isList());

        assertEquals(1,descriptor0.getTritSize().intValue());
        assertEquals(99,descriptor1.getTritSize().intValue());
        assertEquals(243,descriptor2.getTritSize().intValue());
        assertEquals(243,descriptor3.getTritSize().intValue());
        assertEquals(27,descriptor4.getTritSize().intValue());

        assertEquals("isTest",descriptor0.getAsciiLabel());
        assertEquals("myLabel",descriptor1.getAsciiLabel());
        assertEquals("aReference",descriptor2.getAsciiLabel());
        assertEquals("aReferenceList",descriptor3.getAsciiLabel());
        assertEquals("myInteger",descriptor4.getAsciiLabel());
    }
}
