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
import org.iota.ict.ixi.serialization.model.*;
import org.iota.ict.ixi.serialization.util.Utils;
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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Start a real Ict server (gui disabled, port 2387), load serialization.ixi (virtual mode) and run all @Test methods.
 * After all tests, the ixi.terminate() is invoked.
 */
public class SerializationModuleIT {

    private static SerializationModule serializationModule;
    private static Ict ict;

    @BeforeAll
    public static void startIct() throws Exception {
        System.setProperty("log4j.configurationFile","log4j2.xml");
        java.util.Properties javaProperties = new java.util.Properties();
        javaProperties.setProperty(Properties.Property.port.name(), "2387");
        javaProperties.setProperty(Properties.Property.gui_enabled.name(), "false");
        Properties properties = Properties.fromJavaProperties(javaProperties);
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
    public void loadDataFragmentFromNoFragmentHeadTest() throws InterruptedException {
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        Bundle bundle = createMyDataBundle(classFragment, null);
        List<Transaction> txs = bundle.getTransactions();
        for(int i = txs.size()-1; i>=0 ;i--){
            ict.submit(txs.get(i));
        }
        safeSleep(50);
        Transaction nonHead = txs.get(0);
        assertNull(serializationModule.loadDataFragment(nonHead.hash));
    }


    @Test
    public void loadClassFragmentFromNoFragmentHeadTest() throws InterruptedException {
        Bundle bundle = createClassBundle();
        List<Transaction> txs = bundle.getTransactions();
        for(int i = txs.size()-1; i>=0 ;i--){
            ict.submit(txs.get(i));
        }
        safeSleep(50);

        Transaction nonHead = txs.get(0);
        ClassFragment classFragment = serializationModule.loadClassFragment(nonHead.hash);
        assertNull(classFragment);
    }

    @Test
    public void testGetDataFragment(){
        ClassFragment classFragment = serializationModule.publishBundleFragment(new ClassFragment.Builder()
                .addAttribute(9)
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81)));
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        byte[] data = new byte[]{1,1,1,1,1,1,1,1,1};
        builder.setAttribute(0,data);

        ClassFragment classFragment2 = new ClassFragment.Builder()
                .addAttribute(9)
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();
        DataFragment fragment0 = serializationModule.publishBundleFragment(builder);

        DataFragment.Builder builder1 = new DataFragment.Builder(classFragment2);
        data = new byte[]{-1,-1,-1,-1,-1,-1,-1,-1,-1};
        builder.setAttribute(0,data);
        builder1.setReference(0, fragment0);

        DataFragment fragment1 = serializationModule.publishBundleFragment(builder1);
        DataFragment fragment0Clone = serializationModule.getDataFragment(fragment1, 0);

        assertEquals(fragment0.getReference(0), fragment0Clone.getReference(0));
        assertEquals(fragment0.getAttributeAsTryte(0), fragment0Clone.getAttributeAsTryte(0));
    }

    @Test
    public void testGetDataFragmentLongData(){
        ClassFragment classFragment = serializationModule.publishBundleFragment(new ClassFragment.Builder()
                .addAttribute(25002/3)
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81)));

        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        byte[] data = TestUtils.randomTrits(25002);
        builder.setAttribute(0,data);
        DataFragment fragment0 = serializationModule.publishBundleFragment(builder);

        ClassFragment classFragment2 = serializationModule.publishBundleFragment(new ClassFragment.Builder()
                .addAttribute(25002/3)
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81)));

        builder = new DataFragment.Builder(classFragment2);
        data = TestUtils.randomTrits(25002);
        builder.setAttribute(0,data);
        builder.setReference(0, fragment0);
        DataFragment fragment1 = serializationModule.publishBundleFragment(builder);

        DataFragment fragment0Clone = serializationModule.getDataFragment(fragment1, 0);

        assertEquals(fragment0.getReference(0), fragment0Clone.getReference(0));
        assertEquals(fragment0.getAttributeAsTryte(0), fragment0Clone.getAttributeAsTryte(0));
        assertEquals(25002, data.length);
        assertEquals(Trytes.fromTrits(data), fragment1.getAttributeAsTryte(0));
    }

    private String createAndSubmitBundle(ClassFragment classFragment, String data) throws InterruptedException {
        Bundle bundle = createMyDataBundle(classFragment, data);
        Transaction bundleHead = submitBundle(bundle);
        Transaction fragmentHead = bundleHead.getTrunk();
        String dataHeadHash = fragmentHead.hash;
        return dataHeadHash;
    }

    private Transaction submitBundle(Bundle bundle) throws InterruptedException {
        List<Transaction> txs = bundle.getTransactions();
        for(int i = txs.size()-1; i>=0 ;i--){
            ict.submit(txs.get(i));
        }
        safeSleep(50);
        return bundle.getHead();
    }

    @Test
    public void testFindDataFragments() throws InterruptedException {
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        createAndSubmitBundle(classFragment, null);
        ClassFragment classFragment2 = getRandomPublishedClassFragment(27);
        createAndSubmitBundle(classFragment2, null);
        Set<DataFragment> fragments = serializationModule.findDataFragmentForClassHash(classFragment.getClassHash());
        assertEquals(1,fragments.size());
        DataFragment fragment = fragments.iterator().next();
        assertEquals(classFragment.getClassHash(), fragment.getClassHash());
    }

    @Test
    public void testFindFragmentReferencing() throws InterruptedException {
        ClassFragment class1 = getRandomPublishedClassFragment(27);
        ClassFragment class2 = getRandomPublishedClassFragment(27);
        String dataTransactionHash = createAndSubmitBundle(class1, null);
        DataFragment.Builder builder = new DataFragment.Builder(class2);
        builder.setReference(0, dataTransactionHash);
        DataFragment referencingFragment = serializationModule.publishBundleFragment(builder);
        safeSleep(200);
        Set<DataFragment> fragments = serializationModule.findDataFragmentReferencing(dataTransactionHash, null);
        assertEquals(1,fragments.size());
        DataFragment fragment = fragments.iterator().next();
        assertEquals(referencingFragment.getClassHash(), fragment.getClassHash());
        assertEquals(referencingFragment.getHeadTransaction().hash, fragment.getHeadTransaction().hash);

        builder = new DataFragment.Builder(class2);
        builder.setReference(0, dataTransactionHash);
        builder.setAttribute(0,"AAA");
        DataFragment referencingFragment2 = serializationModule.publishBundleFragment(builder);

        safeSleep(200);
        fragments = serializationModule.findDataFragmentReferencing(dataTransactionHash, null);
        assertEquals(2,fragments.size());
        fragment = fragments.iterator().next();
        assertEquals(referencingFragment.getClassHash(), fragment.getClassHash());

        fragments = serializationModule.findDataFragmentReferencing(dataTransactionHash, new DataFragment.Filter() {
            @Override
            public boolean match(DataFragment dataFragment) {
                return dataFragment.getAttributeAsTryte(0).startsWith("AAA");
            }
        });
        assertEquals(1,fragments.size());

    }

    @Test
    public void loadClassFragmentFromClassHashTest() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, ()-> serializationModule.loadClassFragmentForClassHash(null));
        assertThrows(IllegalArgumentException.class, ()-> serializationModule.loadClassFragmentForClassHash(Trytes.NULL_HASH));
        assertNull(serializationModule.loadClassFragmentForClassHash(TestUtils.randomHash()));
        ClassFragment.Builder classFragmentBuilder = new ClassFragment.Builder()
                .addAttribute(99)
                .addReferencedClasshash(TestUtils.randomHash());
        ClassFragment classFragment = serializationModule.publishBundleFragment(classFragmentBuilder);
        safeSleep(100);
        ClassFragment reloadedClassFragment = serializationModule.loadClassFragmentForClassHash(classFragment.getClassHash());
        assertEquals(classFragment.getClassHash(),reloadedClassFragment.getClassHash());
    }

    @Test
    public void loadClassFragmentFromTransactionHashTest(){
        assertThrows(IllegalArgumentException.class, ()-> serializationModule.loadClassFragment(null));
        assertThrows(IllegalArgumentException.class, ()-> serializationModule.loadClassFragment(Trytes.NULL_HASH));
        assertNull(serializationModule.loadClassFragment(TestUtils.randomHash()));
        ClassFragment.Builder classFragmentBuilder = new ClassFragment.Builder()
                .addAttribute(99)
                .addReferencedClasshash(TestUtils.randomHash());
        ClassFragment classFragment = serializationModule.publishBundleFragment(classFragmentBuilder);

        ClassFragment loaded = serializationModule.loadClassFragment(classFragment.getHeadTransaction().hash);
        assertEquals(classFragment.getClassHash(), loaded.getClassHash());
    }


    @Test
    public void getDataTest() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, ()->serializationModule.getAttributeTrytes((DataFragment) null, 1));
        assertThrows(IllegalArgumentException.class, ()->serializationModule.getAttributeTrytes((String) null, 1));
        assertThrows(IllegalArgumentException.class, ()->serializationModule.getAttributeTrytes(Trytes.NULL_HASH, 0));
        assertNull(serializationModule.getAttributeTrytes(TestUtils.randomHash(), 0));
        String data = "ABC";
        ClassFragment classFragment = getRandomPublishedClassFragment(3);
        String dataFragmentHash = createAndSubmitBundle(classFragment,data);
        String loadedData = serializationModule.getAttributeTrytes(dataFragmentHash,0);
        assertEquals(data, loadedData);

        DataFragment dataFragment = serializationModule.loadDataFragment(dataFragmentHash);
        loadedData = serializationModule.getAttributeTrytes(dataFragment,0);
        assertEquals(data, loadedData);
    }


    private Bundle createMyDataBundle(ClassFragment classFragment, String data) {
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        builder.setAttribute(0,data);
        DataFragment.Prepared preparedData = serializationModule.prepare(builder);
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

    private Bundle createClassBundle() {
        ClassFragment.Builder builder = new ClassFragment.Builder().addAttribute(33);
        ClassFragment.Prepared preparedClass = serializationModule.prepareClassFragment(builder);
        assertEquals(1,preparedClass.fromTailToHead().size());

        BundleBuilder bundleBuilder = new BundleBuilder();

        TransactionBuilder tx0 = new TransactionBuilder();
        tx0.address="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        bundleBuilder.append(tx0); //bundle tail
        List<TransactionBuilder> transactionBuilders = preparedClass.fromTailToHead();
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

    @Test
    public void computeClassHashEEETest() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","computeClassHash");
        final AtomicBoolean done = new AtomicBoolean(false);

        String h1 = TestUtils.randomHash();
        String h2 = TestUtils.randomHash();
        ClassFragment.Builder builder = new ClassFragment.Builder();
        builder.addAttribute(3);
        builder.addAttribute(6);
        builder.addAttribute(9);
        builder.addReferencedClasshash(h1);
        builder.addReferencedClasshash(h2);

        final String expected = builder.build().getClassHash();

        registerReturnHandler(env,effect -> {
                String[] split = effect.toString().split(";");
                if(split[0].equals("1")) {
                    assertEquals(2, split.length);
                    assertEquals(expected, split[1]);
                    done.set(true);
                }
        }, countDownLatch, throwableHolder);



        ict.submitEffect(env,"1;3;6;9;"+h1+";"+h2);

        countDownLatch.await(200, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void computeClassHash2EEETest() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","computeClassHash");
        final AtomicBoolean done = new AtomicBoolean(false);
        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("2")) {
                assertEquals(2, split.length);
                assertEquals(81, split[1].length());
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"2;24;"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash());

        countDownLatch.await(200, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }


    @Test
    public void publishDataFragmentEEETest() throws InterruptedException {
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","publishDataFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("1")) {
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"1;"+classFragment.getClassHash()+";"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";A 0 DATA;R 0 "+TestUtils.randomHash()+";R 1 "+TestUtils.randomHash());

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void publishDataFragment2EEETest() throws InterruptedException {
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","publishDataFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("2")) {
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"2;"+classFragment.getClassHash()+";"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";R 0 "+TestUtils.randomHash()+";A 0 DATA;R 0 "
                +TestUtils.randomHash()+";R 1 "+TestUtils.randomHash());

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void publishClassFragmentEEETest() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","publishClassFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("1")) {
                assertEquals(3, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[2].length());
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"1;"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";24");

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }


    @Test
    public void publishClassFragment2EEETest() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","publishClassFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("2")) {
                assertEquals(3, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[2].length());
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"2;"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";24;"+TestUtils.randomHash());

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void publishClassFragment3EEETest() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","publishClassFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("3")) {
                assertEquals(3, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[2].length());
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"3;"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";25;"
                +TestUtils.randomHash()+";"+TestUtils.randomHash());

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }




    @Test
    public void prepareDataFragmentEEETest() throws InterruptedException {
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","prepareDataFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("1")) {
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertFalse(Utils.isBundleHead(split[1]));
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"1;"+classFragment.getClassHash()+";"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";R 0 "+TestUtils.randomHash()+";A 0 DATA");

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void prepareDataFragment2EEETest() throws InterruptedException {
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","prepareDataFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("2")) {
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertFalse(Utils.isBundleHead(split[1]));
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"2;"+classFragment.getClassHash()+";"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";A 0 DATA;R 0 "
                +TestUtils.randomHash()+";R 1 "+TestUtils.randomHash());

        countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }


    @Test
    public void prepareClassFragmentEEETest() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","prepareClassFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("1")) {
                assertEquals(3, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[2].length());
                assertFalse(Utils.isBundleHead(split[1]));
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"1;"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";25");

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }


    @Test
    public void prepareClassFragment2EEETest() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","prepareClassFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("2")) {
                assertEquals(3, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[2].length());
                assertFalse(Utils.isBundleHead(split[1]));
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"2;"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";"+TestUtils.randomHash()+"25;");

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void prepareClassFragment3EEETest() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","prepareClassFragment");
        final AtomicBoolean done = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("3")) {
                assertEquals(3, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[2].length());
                assertFalse(Utils.isBundleHead(split[1]));
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"3;"+TestUtils.randomBundleHeadHash()+";"+TestUtils.randomHash()+";25;"
                +TestUtils.randomHash()+";"+TestUtils.randomHash());

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void buildGetAttributeEEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","getAttribute");
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        builder.setAttribute(0,"DATA");
        BundleFragment fragment = serializationModule.publishBundleFragment(builder);
        String tx_hash = fragment.getHeadTransaction().hash;

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final AtomicBoolean done2 = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("search")) {
                assertEquals(2, split.length);
                assertEquals("DATA99999999999999999999999", split[1]);
                done2.set(true);
            }
        }, countDownLatch, throwableHolder);
        ict.submitEffect(env,"search;"+tx_hash+";0");
        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done2.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }


    @Test
    public void buildGetReferenceEEETest() throws InterruptedException {
        String randomHash = TestUtils.randomHash();
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","getReference");
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        builder.setAttribute(0,"DATA");
        builder.setReference(0,randomHash);
        BundleFragment fragment = serializationModule.publishBundleFragment(builder);
        String tx_hash = fragment.getHeadTransaction().hash;

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final AtomicBoolean done2 = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("searchRef")) {
                assertEquals(2, split.length);
                assertEquals(randomHash, split[1]);
                done2.set(true);
            }
        }, countDownLatch, throwableHolder);
        ict.submitEffect(env,"searchRef;"+tx_hash+";0");
        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done2.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void findFragmentForClassHashEEETest() throws InterruptedException {
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        ClassFragment classFragment2 = getRandomPublishedClassFragment(27);
        safeSleep(200);

        String cf1 = submitBundle(createMyDataBundle(classFragment,"ABCDEF")).getTrunk().hash;
        String cf2 = submitBundle(createMyDataBundle(classFragment,"GGGGGG")).getTrunk().hash;
        String cf3 = submitBundle(createMyDataBundle(classFragment2,"HHHH")).getTrunk().hash;

        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","findFragmentsForClass");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final AtomicBoolean done2 = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("findFragForClass")) {
                assertEquals(3, split.length);
                assertTrue((split[1].equals(cf1) && split[2].equals(cf2)) || (split[2].equals(cf1) && split[1].equals(cf2)));
                done2.set(true);
            }
        }, countDownLatch, throwableHolder);
        ict.submitEffect(env,"findFragForClass;"+classFragment.getClassHash());
        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done2.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }

        done2.set(false);
        final CountDownLatch countDownLatch2 = new CountDownLatch(1);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("findFragForClass2")) {
                assertEquals(2, split.length);
                assertTrue(split[1].equals(cf3));
                done2.set(true);
            }
        }, countDownLatch2, throwableHolder);
        ict.submitEffect(env,"findFragForClass2;"+classFragment2.getClassHash());
        countDownLatch2.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done2.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }


    @Test
    public void findReferencingEEETest() throws InterruptedException {
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        ClassFragment classFragment2 = getRandomPublishedClassFragment(27);
        safeSleep(100);

        DataFragment.Builder referencedBuilder = new DataFragment.Builder(classFragment);
        referencedBuilder.setAttribute(0,"I9AM9REFERENCED");
        DataFragment referenced = serializationModule.publishBundleFragment(referencedBuilder);
        safeSleep(100);

        DataFragment.Builder ref1Builder = new DataFragment.Builder(classFragment);
        ref1Builder.setAttribute(0,"TO9REFERENCED9A");
        ref1Builder.setReference(0,referenced.getHeadTransaction().hash);
        DataFragment ref1 = serializationModule.publishBundleFragment(ref1Builder);

        DataFragment.Builder ref2Builder = new DataFragment.Builder(classFragment);
        ref2Builder.setAttribute(0,"TO9REFERENCED9B");
        ref2Builder.setReference(0,referenced.getHeadTransaction().hash);
        DataFragment ref2 = serializationModule.publishBundleFragment(ref2Builder);

        DataFragment.Builder ref3Builder = new DataFragment.Builder(classFragment);
        ref3Builder.setAttribute(0,"NOT9REFERENCING");
        DataFragment ref3 = serializationModule.publishBundleFragment(ref3Builder);
        safeSleep(100);

        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","findReferencing");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final AtomicBoolean done2 = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("findReferencing1")) {
                assertEquals(3, split.length);
                assertTrue((split[1].equals(ref1.getHeadTransaction().hash) && split[2].equals(ref2.getHeadTransaction().hash)) || (split[2].equals(ref1.getHeadTransaction().hash) && split[1].equals(ref2.getHeadTransaction().hash)));
                done2.set(true);
            }
        }, countDownLatch, throwableHolder);
        ict.submitEffect(env,"findReferencing1;"+referenced.getHeadTransaction().hash);
        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done2.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }

        done2.set(false);
        final CountDownLatch countDownLatch2 = new CountDownLatch(1);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("findReferencing2")) {
                assertEquals(2, split.length);
                assertTrue(split[1].equals(ref1.getHeadTransaction().hash));
                done2.set(true);
            }
        }, countDownLatch2, throwableHolder);
        ict.submitEffect(env,"findReferencing2;"+referenced.getHeadTransaction().hash+";0;TO9REFERENCED9A");
        countDownLatch2.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done2.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }





    @Test
    public void registerListenerTest() throws InterruptedException {
        ClassFragment classFragment = getRandomPublishedClassFragment(27);
        String randomClassHash = classFragment.getClassHash();
        String envId = "test0";
        final Environment environment = new Environment(envId);
        serializationModule.registerDataListener(randomClassHash, envId);
        DataFragment.Builder fragmentBuilder = new DataFragment.Builder(classFragment).setAttribute(0, "DATADATA");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean done = new AtomicBoolean(false);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        EffectListener<DataFragment> checker = new EffectListener<DataFragment>() {
            @Override
            public void onReceive(DataFragment effect) {
                try {
                    assertEquals(Trytes.padRight("DATADATA",27), effect.getAttributeAsTryte(0));
                    done.set(true);
                }catch (Throwable e){
                    throwableHolder.throwable = e;
                }
                countDownLatch.countDown();
            }

            @Override
            public Environment getEnvironment() {
                return environment;
            }
        };
        ict.addListener(checker);
        serializationModule.publishBundleFragment(fragmentBuilder);
        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
        assertTrue(done.get());
    }
    private void registerReturnHandler(FunctionEnvironment env, ReturnHandler returnHandler, CountDownLatch countDownLatch, ThrowableHolder throwableHolder){
        final Environment returnEnv = new FunctionReturnEnvironment(env);
        ict.addListener(new EffectListener() {
            @Override
            public void onReceive(Object effect) {
                try {
                    returnHandler.onReceive(effect);
                }catch (Throwable error){
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

    private void safeSleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    private ClassFragment getRandomPublishedClassFragment(int... attributes){
        ClassFragment.Builder builder = new ClassFragment.Builder();
        builder.addReferencedClasshash(TestUtils.randomHash());
        for(int i:attributes)builder.addAttribute(i);
        ClassFragment classFragment = serializationModule.publishBundleFragment(builder);
        safeSleep(100);
        return classFragment;
    }
}
