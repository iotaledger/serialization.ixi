package org.iota.ict.ixi.serialization;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.ixi.serialization.model.PreparedDataFragment;
import org.iota.ict.ixi.serialization.model.SampleSerializableClass;
import org.iota.ict.ixi.serialization.model.StructuredDataFragment;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.bundle.BundleBuilder;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationModuleTest {

    private static SerializationModule serializationModule;

    @BeforeAll
    public static void setup(){
        serializationModule = new SerializationModule(Mockito.mock(Ixi.class));
    }

    @Test
    public void loadMetadataFromInvalidHash(){

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadMetadata(null));
        assertEquals("'null' is not a valid transaction hash", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadMetadata(""));
        assertEquals("'' is not a valid transaction hash", exception.getMessage());

        String shortHash = TestUtils.random(80);
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadMetadata(shortHash));
        assertEquals("'"+shortHash+"' is not a valid transaction hash", exception.getMessage());

        String longHash = TestUtils.random(82);
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadMetadata(longHash));
        assertEquals("'"+longHash+"' is not a valid transaction hash", exception.getMessage());
    }

    @Test
    public void loadMetadataFromUnknownHash(){
        assertNull(serializationModule.loadMetadata(TestUtils.randomHash())," Expecting a null response when transaction hash is unknown");
    }

    @Test
    public void loadStructuredDataFromInvalidHash(){

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadStructuredData(null));
        assertEquals("'null' is not a valid transaction hash", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadStructuredData(""));
        assertEquals("'' is not a valid transaction hash", exception.getMessage());

        String shortHash = TestUtils.random(80);
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadStructuredData(shortHash));
        assertEquals("'"+shortHash+"' is not a valid transaction hash", exception.getMessage());

        String longHash = TestUtils.random(82);
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadStructuredData(longHash));
        assertEquals("'"+longHash+"' is not a valid transaction hash", exception.getMessage());
    }

    @Test
    public void loadStructuredDataFromUnknownHash(){
        try{
            assertNull(serializationModule.loadStructuredData(TestUtils.randomHash())," Expecting a null response when transaction hash is unknown");
        }catch (UnknownMetadataException e){
            fail("Was expecting a null when transaction hash is unknown");
        }
    }

    @Test
    public void checkBuilderConnotBeNull(){
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.buildMetadataFragment(null));
        assertEquals("builder cannot be null", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.buildStructuredDataFragment(null));
        assertEquals("builder cannot be null", exception.getMessage());
    }

    @Test
    public void prepareDataFragment(){
        SampleSerializableClass myData = SampleData.sample;
        PreparedDataFragment preparedData = serializationModule.prepare(myData);
        assertEquals(2,preparedData.fromTailToHead().size());

        BundleBuilder bundleBuilder = new BundleBuilder();

        TransactionBuilder tx0 = new TransactionBuilder();
        tx0.address="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        bundleBuilder.append(tx0); //bundle tail
        List<TransactionBuilder> transactionBuilders = preparedData.fromTailToHead();
        bundleBuilder.append(transactionBuilders);
        TransactionBuilder tx1 = new TransactionBuilder();
        tx1.address="ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ";
        bundleBuilder.append(tx1);
        Bundle bundle = bundleBuilder.build(); //bundle head
        assertEquals("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ",bundle.getHead().address());
        assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",bundle.getTail().address());
        assertEquals( MetadataFragment.Builder.fromClass(SampleSerializableClass.class).build().hash(), bundle.getHead().getTrunk().extraDataDigest());
        assertEquals(4,bundle.getTransactions().size());
    }


    @Test
    public void prepareBigDataFragment(){
        SampleSerializableClass myData = new SampleSerializableClass();
        myData.isTest = true;
        myData.myLabel = "hello world";
        PreparedDataFragment preparedData = serializationModule.prepare(myData);
        assertEquals(1,preparedData.fromTailToHead().size());

        BundleBuilder bundleBuilder = new BundleBuilder();

        TransactionBuilder tx0 = new TransactionBuilder();
        tx0.address="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        bundleBuilder.append(tx0); //bundle tail
        List<TransactionBuilder> transactionBuilders = preparedData.fromTailToHead();
        bundleBuilder.append(transactionBuilders);
        TransactionBuilder tx1 = new TransactionBuilder();
        tx1.address="ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ";
        bundleBuilder.append(tx1);
        Bundle bundle = bundleBuilder.build(); //bundle head
        assertEquals("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ",bundle.getHead().address());
        assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",bundle.getTail().address());
        assertEquals( MetadataFragment.Builder.fromClass(SampleSerializableClass.class).build().hash(), bundle.getHead().getTrunk().extraDataDigest());
    }
}
