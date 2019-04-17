package org.iota.ict.ixi.serialization;

import org.iota.ict.Ict;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.ixi.IxiModuleInfo;
import org.iota.ict.ixi.serialization.model.AllTypesSampleData;
import org.iota.ict.ixi.serialization.model.SampleSerializableClass;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.utils.properties.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
                        assertEquals(dataFragment.getAsciiValue(0),"hello");
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
