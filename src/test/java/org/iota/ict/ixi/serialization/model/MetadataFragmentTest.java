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
        FieldDescriptor descriptor = FieldDescriptor.build(false,48);
        builder.appendField(FieldDescriptor.build(false,48));
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
        FieldDescriptor descriptor = FieldDescriptor.build(false,48);
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
        //be sure that we add enough descriptors to have 2 transactions in the fragment
        final int COUNT = 1+Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength / FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH;
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.build(false,48);
        for(int i=0; i<COUNT ; i++){
            builder.appendField(descriptor);
        }
        MetadataFragment metadataFragment = builder.build();
        String expected = MetadataFragment.METADATA_LANGUAGE_VERSION;
        for(int i=0; i<COUNT ; i++){
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
        assertEquals(COUNT, metadataFragment.getKeyCount());
    }

    @Test
    public void simpleMetadataFragmentWith81FieldTest(){
        //be sure that we add enough descriptors to have 3 transactions in the fragment
        final int COUNT = 2*(1+Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength / FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH);
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.build(false,48);
        for(int i=0; i<COUNT ; i++){
            builder.appendField(descriptor);
        }
        MetadataFragment metadataFragment = builder.build();
        String expected = MetadataFragment.METADATA_LANGUAGE_VERSION;
        for(int i=0; i<COUNT ; i++){
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
        assertEquals(COUNT, metadataFragment.getKeyCount());
    }

    @Test
    public void bigMetadataFragmentTest() {
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        for (int i = 0; i < 85; i++) {
            FieldDescriptor descriptor = FieldDescriptor.build(i % 7 == 0, 48 + i);
            builder.appendField(descriptor);
        }
        MetadataFragment metadataFragment = builder.build();
        Transaction head = metadataFragment.getHeadTransaction();
        MetadataFragment metadataFragmentClone = new MetadataFragment.Builder().fromTransaction(head);
        assertEquals(metadataFragment.getKeyCount(), metadataFragmentClone.getKeyCount());
        for (int i = 0; i < 85; i++) {
            assertEquals(metadataFragment.getDescriptor(i).getTritSize(), metadataFragmentClone.getDescriptor(i).getTritSize());
            assertEquals(metadataFragment.getDescriptor(i).isList(), metadataFragmentClone.getDescriptor(i).isList());
        }
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

    }
}
