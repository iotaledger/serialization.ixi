package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldType;
import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetadataFragmentTest {

    @Test
    public void simpleMetadataFragmentTest(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("AI"),48,"a simple label");
        builder.appendField(FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("AI"),48,"a simple label"));
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
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("AI"),48,"a simple label");
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
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("AI"),48,"a simple label");
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
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("AI"),48,"a simple label");
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
}
