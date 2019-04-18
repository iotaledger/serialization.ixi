package org.iota.ict.ixi.serialization;

import org.iota.ict.Ict;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.ixi.IxiModuleInfo;
import org.iota.ict.ixi.serialization.model.AllTypesSampleData;
import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.ixi.serialization.model.SampleSerializableClass;
import org.iota.ict.ixi.serialization.model.StructuredDataFragment;
import org.iota.ict.ixi.serialization.util.TritsConverter;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.bundle.BundleBuilder;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.properties.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationModuleIT {

    private static SerializationModule serializationModule;
    private static Ict ict;

    @BeforeAll
    public static void startIct() throws Exception {
        System.setProperty("log4j.configurationFile","log4j2.xml");
        Properties properties = new Properties();
        ict = new Ict(properties.toFinal());
        ict.getModuleHolder().loadVirtualModule(SerializationModule.class, "Serialization.ixi");
        ict.getModuleHolder().startAllModules();
        serializationModule = (SerializationModule) getModuleByName(ict.getModuleHolder(),"Serialization.ixi");
    }

    @AfterAll
    public static void terminate(){
        serializationModule.terminate();
    }

    @Test
    public void testRegisterMetadata() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean received = new AtomicBoolean(false);
        serializationModule.registerDataListener(
                dataFragment -> dataFragment.getClassHash().equals(SampleData.classWithOneAsciiField.hash()),
                dataFragment -> {
                    received.set(true);
                    try {
                        assertEquals(dataFragment.getValue(0, TritsConverter.ASCII),"hello");
                    } catch (UnknownMetadataException e) {
                        fail("Metadata should be known");
                    }
                    countDownLatch.countDown();
                }
        );

        serializationModule.registerMetadata(SampleData.classWithOneAsciiField);
        ict.submit(SampleData.simpleDataFragment.getHeadTransaction());
        countDownLatch.await(3, TimeUnit.SECONDS);
        assertTrue(received.get());
    }

    @Test
    public void testJavaInstanceListener() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean received = new AtomicBoolean(false);
        serializationModule.registerDataListener(
                SampleSerializableClass.class,
                sample -> {
                    received.set(true);
                    assertEquals(17, sample.myInteger);

                    countDownLatch.countDown();
                }
        );
        serializationModule.publish(SampleData.sample);
        countDownLatch.await(1, TimeUnit.SECONDS);
        assertTrue(received.get());
    }


    @Test
    public void serializeAllTypeClassInstance() throws UnknownMetadataException, InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean received = new AtomicBoolean(false);
        AllTypesSampleData sample = SampleData.allTypesSample;

        serializationModule.registerDataListener(AllTypesSampleData.class, new DataFragmentListener<AllTypesSampleData>() {
            @Override
            public void onData(AllTypesSampleData sampleData) {
                assertEquals(true, sampleData.isTest);
                assertEquals("aLabel", sampleData.myLabel);
                assertEquals(sample.aReferenceHash, sampleData.aReferenceHash);
                List<String> values = sampleData.listOfReferences;
                for(int i=0;i<50;i++){
                    assertEquals(sample.listOfReferences.get(i),values.get(i));
                }
                assertEquals(17, sampleData.myInteger);
                assertEquals(55, sampleData.myIntegerObject.intValue());
                assertEquals(Long.MAX_VALUE, sampleData.myLong);
                assertEquals(56, sampleData.myLongObject.intValue());
                assertEquals(Long.MAX_VALUE, sampleData.myBigInteger.longValue());
                assertEquals(333.12f, sampleData.myFloat);
                assertEquals(1.23456f, sampleData.myFloatObject.floatValue());
                assertEquals(222.12, sampleData.myDouble);
                assertEquals(1.234567, sampleData.myDoubleObject.doubleValue());
                assertEquals(sample.myBigDecimal, sampleData.myBigDecimal);
                assertEquals(sample.isTestList, sampleData.isTestList);
                assertEquals(sample.myLabelList, sampleData.myLabelList);
                assertEquals(sample.myIntegerList, sampleData.myIntegerList);
                assertEquals(sample.myLongObjectList, sampleData.myLongObjectList);
                assertEquals(sample.myBigIntegerList, sampleData.myBigIntegerList);
                assertEquals(sample.myFloatObjectList, sampleData.myFloatObjectList);
                assertEquals(sample.myDoubleObjectList, sampleData.myDoubleObjectList);
                assertEquals(sample.myBigDecimalList, sampleData.myBigDecimalList);
                received.set(true);
                countDownLatch.countDown();
            }
        });
        serializationModule.publish(sample);
        countDownLatch.await(1, TimeUnit.SECONDS);
        assertTrue(received.get());


    }


    @Test
    public void loadFragmentDataThrowExceptionTest() throws UnknownMetadataException {
        serializationModule.forgetAllMetadata();
        String dataHeadHash = createAndSubmitBundle();
        assertThrows(UnknownMetadataException.class, () ->
                serializationModule.loadFragmentData(dataHeadHash));
    }

    @Test
    public void loadFragmentDataTest() throws UnknownMetadataException {
        String dataHeadHash = createAndSubmitBundle();
        MetadataFragment.Builder builder = MetadataFragment.Builder.fromClass(SampleSerializableClass.class);
        MetadataFragment metadataFragment = serializationModule.buildMetadataFragment(builder);
        serializationModule.registerMetadata(metadataFragment);
        StructuredDataFragment dataFragment = serializationModule.loadFragmentData(dataHeadHash);
        assertNotNull(dataFragment);
        assertTrue(serializationModule.getTritsForKeyAtIndex(dataFragment,0)[0]==1);
        byte[] expected = new byte[99];
        byte[] hello_word = Trytes.toTrits(Trytes.fromAscii("hello world"));
        System.arraycopy(hello_word,0,expected,0,hello_word.length);
        assertArrayEquals(expected,serializationModule.getTritsForKeyAtIndex(dataFragment,1));
    }

    @Test
    public void loadFragmentDataWithClassTest() throws UnknownMetadataException {
        String dataHeadHash = createAndSubmitBundle();
        serializationModule.registerMetadata(MetadataFragment.Builder.fromClass(SampleSerializableClass.class).build());
        SampleSerializableClass data = serializationModule.loadFragmentData(dataHeadHash, SampleSerializableClass.class);
        assertNotNull(data);
        assertTrue(data.isTest);
        assertEquals("hello world", data.myLabel);
    }

    @Test
    public void loadFragmentDataFromNoFragmentHeadTest() throws UnknownMetadataException {
        Bundle bundle = createDataBundle();
        List<Transaction> txs = bundle.getTransactions();
        for(int i = txs.size()-1; i>=0 ;i--){
            ict.submit(txs.get(i));
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Transaction nonHead = txs.get(0);
        StructuredDataFragment dataFragment = serializationModule.loadFragmentData(nonHead.hash);
        assertNull(dataFragment);
    }

    @Test
    public void metadataFragmentIsRegistered(){
        serializationModule.forgetAllMetadata();
        MetadataFragment.Prepared metadataFragment = serializationModule.prepareMetadata(SampleSerializableClass.class);
        Bundle bundle = createMetadataBundle(metadataFragment);
        String classHash = MetadataFragment.Builder.fromClass(SampleSerializableClass.class).build().hash();
        assertNull(serializationModule.getMetadataFragment(classHash));
        Transaction bundleHead = submitBundle(bundle);
        assertNotNull(serializationModule.getMetadataFragment(classHash));
    }

    private String createAndSubmitBundle() {
        Bundle bundle = createDataBundle();
        List<Transaction> txs = bundle.getTransactions();
        Transaction fragmentHead = txs.get(1);
        String dataHeadHash = fragmentHead.hash;
        for(int i = txs.size()-1; i>=0 ;i--){
            ict.submit(txs.get(i));
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return dataHeadHash;
    }

    private Transaction submitBundle(Bundle bundle) {
        List<Transaction> txs = bundle.getTransactions();
        for(int i = txs.size()-1; i>=0 ;i--){
            ict.submit(txs.get(i));
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return bundle.getHead();
    }



    private Bundle createMetadataBundle(MetadataFragment.Prepared prepared) {

        BundleBuilder bundleBuilder = new BundleBuilder();

        TransactionBuilder tx0 = new TransactionBuilder();
        tx0.address="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        bundleBuilder.append(tx0); //bundle tail
        List<TransactionBuilder> transactionBuilders = prepared.fromTailToHead();
        bundleBuilder.append(transactionBuilders);
        TransactionBuilder tx1 = new TransactionBuilder();
        tx1.address="ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ";
        bundleBuilder.append(tx1);
        return bundleBuilder.build();
    }


    private Bundle createDataBundle() {
        SampleSerializableClass myData = new SampleSerializableClass();
        myData.isTest = true;
        myData.myLabel = "hello world";
        StructuredDataFragment.Prepared preparedData = serializationModule.prepare(myData);
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
        return bundleBuilder.build();
    }

    private static IxiModule getModuleByName(IxiModuleHolder moduleHolder, String name){
        for(IxiModule ixiModule:moduleHolder.getModules()){
            IxiModuleInfo info = moduleHolder.getInfo(ixiModule);
            if(info.name.equals(name)){
                return ixiModule;
            }
        }
        return null;
    }

}
