package org.iota.ict.ixi.serialization;

import org.iota.ict.Ict;
import org.iota.ict.ixi.serialization.model.SampleSerializableClass;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.utils.properties.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        serializationModule = (SerializationModule) ict.getModuleHolder().getModule("virtual/Serialization.ixi");
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
        ict.submit(SampleData.simpleDataFragment.getHeadTransaction());
        countDownLatch.await(3, TimeUnit.SECONDS);
        assertTrue(received.get());
    }


}
