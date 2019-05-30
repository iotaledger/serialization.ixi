package org.iota.ict.ixi.serialization;

import org.iota.ict.Ict;
import org.iota.ict.eee.EffectListener;
import org.iota.ict.eee.Environment;
import org.iota.ict.eee.call.FunctionEnvironment;
import org.iota.ict.eee.call.FunctionReturnEnvironment;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.ixi.IxiModuleInfo;
import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.model.BundleFragment;
import org.iota.ict.ixi.serialization.model.ClassFragment;
import org.iota.ict.ixi.serialization.model.DataFragment;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.utils.properties.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Start a real Ict server (gui disabled, port 2387), load serialization.ixi (virtual mode) and run all @Test methods.
 * After all tests, the ixi.terminate() is invoked.
 */
@SuppressWarnings("WeakerAccess")
public class SerializationEEETest {

    private static SerializationModule serializationModule;
    private static Ict ict;

    @BeforeAll
    public static void startIct() throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
        java.util.Properties javaProperties = new java.util.Properties();
        javaProperties.setProperty(Properties.Property.port.name(), "2388");
        javaProperties.setProperty(Properties.Property.gui_enabled.name(), "false");
        Properties properties = Properties.fromJavaProperties(javaProperties);
        ict = new Ict(properties.toFinal());
        ict.getModuleHolder().loadVirtualModule(SerializationModule.class, "Serialization.ixi");
        ict.getModuleHolder().startAllModules();
        serializationModule = (SerializationModule) getModuleByName(ict.getModuleHolder(), "Serialization.ixi");
    }

    @AfterAll
    public static void terminate() {
        serializationModule.terminate();
    }

    private DataFragment createDataBundle(String... data) {
        ClassFragment.Builder builder = new ClassFragment.Builder("MYTESTCLASS");
        for (String s : data) {
            builder.addAttribute(0, TestUtils.random(10));
        }
        ClassFragment classFragment = serializationModule.publishBundleFragment(builder);
        DataFragment.Builder dataBuilder = new DataFragment.Builder(classFragment);
        for (int i = 0; i < data.length; i++) {
            dataBuilder.setAttribute(i, data[i]);
        }
        return serializationModule.publishBundleFragment(dataBuilder);
    }

    private static IxiModule getModuleByName(IxiModuleHolder moduleHolder, String name) {
        for (IxiModule ixiModule : moduleHolder.getModules()) {
            IxiModuleInfo info = moduleHolder.getInfo(ixiModule);
            if (info.name.equals(name)) {
                return ixiModule;
            }
        }
        return null;
    }

    @Test
    public void computeClassHashEEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "computeClassHash");

        String h1 = TestUtils.randomHash();
        String h2 = TestUtils.randomHash();
        String className = TestUtils.random(9);
        ClassFragment.Builder builder = new ClassFragment.Builder(className);
        builder.addAttribute(3, TestUtils.random(10));
        builder.addAttribute(6, TestUtils.random(10));
        builder.addAttribute(9, TestUtils.random(10));
        builder.addReferencedClasshash(h1);
        builder.addReferencedClasshash(h2);

        final String expected = builder.build().getClassHash();

        submitEffectAndAssert(env,className+";"+
                "3 A;6 B;9 C;" + h1 + ";" + h2,
                response -> {
                    assertEquals(2, response.length);
                    assertEquals(expected, response[1]);
                });
    }

    @Test
    public void computeClassHash2EEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "computeClassHash");
        submitEffectAndAssert(env,
                "MY9CLASS;24 AAA;" + TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash(),
                response -> {
                    assertEquals(2, response.length);
                    assertEquals(81, response[1].length());
                });
    }


    @Test
    public void publishDataFragmentEEETest() throws InterruptedException {
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "publishDataFragment");
        String ref0 = TestUtils.randomHash();
        String ref1 = TestUtils.randomHash();
        submitEffectAndAssert(env,
                classFragment.getClassHash() + ";" + TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";A 0 DATA;R 0 " + ref0 + ";R 1 " + ref1,
                response -> {
                    assertEquals(2, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                    DataFragment fragment = serializationModule.loadDataFragment(response[1]);
                    assertEquals(ref0, fragment.getReference(0));
                    assertEquals(ref1, fragment.getReference(1));
                });
    }

    @Test
    public void publishDataFragment2EEETest() throws InterruptedException {
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "publishDataFragment");

        submitEffectAndAssert(env,
                classFragment.getClassHash() + ";" + TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";R 0 " + TestUtils.randomHash() + ";A 0 DATA;R 0 "
                        + TestUtils.randomHash() + ";R 1 " + TestUtils.randomHash(),
                response -> {
                    assertEquals(2, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                });
    }

    @Test
    public void publishClassFragmentEEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "publishClassFragment");

        submitEffectAndAssert(env, "ABCD;"+
                TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";24 ATTRIB",
                response -> {
                    assertEquals(3, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[2].length());
                });
    }


    @Test
    public void publishClassFragment2EEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "publishClassFragment");

        submitEffectAndAssert(env, "QWERTY;"+
                TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";24 MY9ATTRIB;" + TestUtils.randomHash(),
                response -> {
                    assertEquals(3, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[2].length());
                });
    }

    @Test
    public void publishClassFragment3EEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "publishClassFragment");

        submitEffectAndAssert(env,"THE9CLASS9NAME;"+
                TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";25;"
                        + TestUtils.randomHash() + ";" + TestUtils.randomHash(),
                response -> {
                    assertEquals(3, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[2].length());
                });
    }

    @Test
    public void prepareDataFragmentEEETest() throws InterruptedException {
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "prepareDataFragment");

        submitEffectAndAssert(env,
                classFragment.getClassHash() + ";" + TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";R 0 " + TestUtils.randomHash() + ";A 0 DATA",
                response -> {
                    assertEquals(2, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                    assertFalse(Utils.isBundleHead(response[1]));
                });
    }

    @Test
    public void prepareDataFragment2EEETest() throws InterruptedException {
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "prepareDataFragment");
        submitEffectAndAssert(env,
                classFragment.getClassHash() + ";" + TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";A 0 DATA;R 0 "
                        + TestUtils.randomHash() + ";R 1 " + TestUtils.randomHash(),
                response -> {
                    assertEquals(2, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                    assertFalse(Utils.isBundleHead(response[1]));
                });
    }


    @Test
    public void prepareClassFragmentEEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "prepareClassFragment");

        submitEffectAndAssert(env,"A9CLASS;"+
                TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";25",
                response -> {
                    assertEquals(3, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[2].length());
                    assertFalse(Utils.isBundleHead(response[1]));
                });
    }


    @Test
    public void prepareClassFragment2EEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "prepareClassFragment");
        submitEffectAndAssert(env,"ABC;"+
                TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";" + TestUtils.randomHash() + "25;",
                response -> {
                    assertEquals(3, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[2].length());
                    assertFalse(Utils.isBundleHead(response[1]));
                });
    }

    @Test
    public void prepareClassFragment3EEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "prepareClassFragment");

        submitEffectAndAssert(env, "CLS;"+
                TestUtils.randomBundleHeadHash() + ";" + TestUtils.randomHash() + ";25;"
                        + TestUtils.randomHash() + ";" + TestUtils.randomHash(),
                response -> {
                    assertEquals(3, response.length);
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[1].length());
                    assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, response[2].length());
                    assertFalse(Utils.isBundleHead(response[1]));
                });
    }

    @Test
    public void buildGetAttributeEEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "getAttribute");
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        builder.setAttribute(0, "DATA");
        BundleFragment fragment = serializationModule.publishBundleFragment(builder);
        String tx_hash = fragment.getHeadTransaction().hash;

        submitEffectAndAssert(env,
                tx_hash + ";0",
                response -> {
                    assertEquals(2, response.length);
                    assertEquals("DATA99999999999999999999999", response[1]);
                });
    }


    @Test
    public void buildGetReferenceEEETest() throws InterruptedException {
        String randomHash = TestUtils.randomHash();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "getReference");
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        builder.setAttribute(0, "DATA");
        builder.setReference(0, randomHash);
        BundleFragment fragment = serializationModule.publishBundleFragment(builder);
        String tx_hash = fragment.getHeadTransaction().hash;


        submitEffectAndAssert(env,
                tx_hash + ";0",
                response -> {
                    assertEquals(2, response.length);
                    assertEquals(randomHash, response[1]);
                });
    }


    @Test
    public void buildGetReferencedAttributeEEETest() throws InterruptedException {
        ClassFragment attributesClass = serializationModule.publishBundleFragment(
                new ClassFragment.Builder(TestUtils.random(9)).addAttribute(10, TestUtils.random(10)).addAttribute(20, TestUtils.random(10)));
        DataFragment referenced = serializationModule.publishBundleFragment(
                new DataFragment.Builder(attributesClass).setAttribute(0, "ATT9ONE")
                        .setAttribute(1, "ATT9TWO"));

        ClassFragment referencingClass = serializationModule.publishBundleFragment(
                new ClassFragment.Builder(TestUtils.random(9)).addReferencedClass(attributesClass).addAttribute(20, TestUtils.random(10)));
        DataFragment referencingData = serializationModule.publishBundleFragment(
                new DataFragment.Builder(referencingClass).setReference(0, referenced.getHeadTransaction().hash)
        );
        safeSleep(100);

        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "getReferencedAttribute");

        submitEffectAndAssert(env,
                referencingData.getHeadTransaction().hash + ";0;0",
                response -> {
                    assertEquals(2, response.length);
                    assertEquals("ATT9ONE999", response[1]);
                });

        submitEffectAndAssert(env,
                referencingData.getHeadTransaction().hash + ";0;1",
                response -> {
                    assertEquals(2, response.length);
                    assertEquals("ATT9TWO9999999999999", response[1]);
                });
    }


    @Test
    public void findFragmentForClassHashEEETest() throws InterruptedException {
        DataFragment f0 = createDataBundle("ABCDEF");
        DataFragment f3 = createDataBundle("HHHH", "");
        String cf1 = f0.getHeadTransaction().hash;
        String cf2 = createDataBundle("GGGGGG").getHeadTransaction().hash;
        String cf3 = f3.getHeadTransaction().hash;
        ClassFragment classFragment = f0.getClassFragment();
        ClassFragment classFragment2 = f3.getClassFragment();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "findFragmentsForClass");
        safeSleep(200);

        submitEffectAndAssert(env,
                classFragment.getClassHash(),
                response -> {
                    assertEquals(3, response.length);
                    assertTrue((response[1].equals(cf1) && response[2].equals(cf2)) || (response[2].equals(cf1) && response[1].equals(cf2)));
                });

        submitEffectAndAssert(env,
                classFragment2.getClassHash(),
                response -> {
                    assertEquals(2, response.length);
                    assertEquals(response[1], cf3);
                });
    }


    @Test
    public void findReferencingEEETest() throws InterruptedException {
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        ClassFragment classFragment2 = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        safeSleep(100);

        DataFragment.Builder referencedBuilder = new DataFragment.Builder(classFragment);
        referencedBuilder.setAttribute(0, "I9AM9REFERENCED");
        DataFragment referenced = serializationModule.publishBundleFragment(referencedBuilder);
        safeSleep(100);

        DataFragment.Builder ref1Builder = new DataFragment.Builder(classFragment);
        ref1Builder.setAttribute(0, "TO9REFERENCED9A");
        ref1Builder.setReference(0, referenced.getHeadTransaction().hash);
        DataFragment ref1 = serializationModule.publishBundleFragment(ref1Builder);

        DataFragment.Builder ref2Builder = new DataFragment.Builder(classFragment);
        ref2Builder.setAttribute(0, "TO9REFERENCED9B");
        ref2Builder.setReference(0, referenced.getHeadTransaction().hash);
        DataFragment ref2 = serializationModule.publishBundleFragment(ref2Builder);

        DataFragment.Builder ref3Builder = new DataFragment.Builder(classFragment);
        ref3Builder.setAttribute(0, "NOT9REFERENCING");
        DataFragment ref3 = serializationModule.publishBundleFragment(ref3Builder);
        safeSleep(100);

        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi", "findReferencing");

        submitEffectAndAssert(env,
                referenced.getHeadTransaction().hash,
                response -> {
                    assertEquals(3, response.length);
                    assertTrue((response[1].equals(ref1.getHeadTransaction().hash) && response[2].equals(ref2.getHeadTransaction().hash)) || (response[2].equals(ref1.getHeadTransaction().hash) && response[1].equals(ref2.getHeadTransaction().hash)));
                });

        submitEffectAndAssert(env,
                referenced.getHeadTransaction().hash + ";0;TO9REFERENCED9A",
                response -> {
                    assertEquals(2, response.length);
                    assertEquals(response[1], ref1.getHeadTransaction().hash);
                });
    }

    //UTILS

    private void safeSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    private void submitEffectAndAssert(FunctionEnvironment env, String requestParams, AssertResponse assertions) throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final AtomicBoolean done = new AtomicBoolean(false);

        String reqID = TestUtils.random(20);
        registerReturnHandler(env, effect -> {
            String[] response = effect.toString().split(";");
            if (response[0].equals(reqID)) {
                assertions.assertResponse(response);
                done.set(true);
            }
        }, countDownLatch, throwableHolder);
        ict.submitEffect(env, reqID + ";" + requestParams);

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if (throwableHolder.throwable != null) {
            fail(throwableHolder.throwable);
        }
    }

    interface AssertResponse {
        void assertResponse(String[] response);
    }

    private void registerReturnHandler(FunctionEnvironment env, ReturnHandler returnHandler, CountDownLatch countDownLatch, ThrowableHolder throwableHolder) {
        final Environment returnEnv = new FunctionReturnEnvironment(env);
        ict.addListener(new EffectListener() {
            @Override
            public void onReceive(Object effect) {
                try {
                    returnHandler.onReceive(effect);
                } catch (Throwable error) {
                    error.printStackTrace();
                    throwableHolder.throwable = error;
                }
                countDownLatch.countDown();
            }

            @Override
            public Environment getEnvironment() {
                return returnEnv;
            }
        });
    }

    interface ReturnHandler {
        void onReceive(Object effect);
    }

    private static class ThrowableHolder {
        Throwable throwable;
    }

}
