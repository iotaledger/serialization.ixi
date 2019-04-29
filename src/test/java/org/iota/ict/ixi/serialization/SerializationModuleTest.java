package org.iota.ict.ixi.serialization;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.model.DataFragment;
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
                serializationModule.loadClassFragment(null));
        assertEquals("'null' is not a valid transaction hash", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadClassFragment(""));
        assertEquals("'' is not a valid transaction hash", exception.getMessage());

        String shortHash = TestUtils.random(80);
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadClassFragment(shortHash));
        assertEquals("'"+shortHash+"' is not a valid transaction hash", exception.getMessage());

        String longHash = TestUtils.random(82);
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadClassFragment(longHash));
        assertEquals("'"+longHash+"' is not a valid transaction hash", exception.getMessage());
    }

    @Test
    public void loadMetadataFromUnknownHash(){
        assertNull(serializationModule.loadClassFragment(TestUtils.randomHash())," Expecting a null response when transaction hash is unknown");
    }

    @Test
    public void loadStructuredDataFromInvalidHash(){

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadDataFragment(null));
        assertEquals("'null' is not a valid transaction hash", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadDataFragment(""));
        assertEquals("'' is not a valid transaction hash", exception.getMessage());

        String shortHash = TestUtils.random(80);
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadDataFragment(shortHash));
        assertEquals("'"+shortHash+"' is not a valid transaction hash", exception.getMessage());

        String longHash = TestUtils.random(82);
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.loadDataFragment(longHash));
        assertEquals("'"+longHash+"' is not a valid transaction hash", exception.getMessage());
    }

    @Test
    public void loadStructuredDataFromUnknownHash(){
        assertNull(serializationModule.loadDataFragment(TestUtils.randomHash())," Expecting a null response when transaction hash is unknown");
    }

    @Test
    public void checkBuilderCannotBeNull(){
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.buildClassFragment(null));
        assertEquals("builder cannot be null", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                serializationModule.buildDataFragment(null));
        assertEquals("builder cannot be null", exception.getMessage());
    }

    @Test
    public void prepareDataFragment(){
        DataFragment.Builder builder = new DataFragment.Builder(TestUtils.randomHash());
        builder.setReference(1, TestUtils.randomHash());
        DataFragment.Prepared preparedData = serializationModule.prepare(builder);
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
        assertEquals(4,bundle.getTransactions().size());
    }


    @Test
    public void prepareBigDataFragment(){
        Bundle bundle = createBundle();
        assertEquals("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ",bundle.getHead().address());
        assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",bundle.getTail().address());
    }

    private Bundle createBundle() {
        DataFragment.Builder builder = new DataFragment.Builder(TestUtils.randomHash());
        builder.setReference(0,TestUtils.randomHash());
        builder.setReference(1,TestUtils.randomHash());
        DataFragment.Prepared preparedData = serializationModule.prepare(builder);
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
        return bundleBuilder.build();
    }



}
