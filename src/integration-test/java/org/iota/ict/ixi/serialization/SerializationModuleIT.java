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
        Bundle bundle = createMyDataBundle(TestUtils.randomHash(), null);
        List<Transaction> txs = bundle.getTransactions();
        for(int i = txs.size()-1; i>=0 ;i--){
            ict.submit(txs.get(i));
        }
        Thread.sleep(50);
        Transaction nonHead = txs.get(0);
        assertThrows(IllegalArgumentException.class,() -> serializationModule.loadDataFragment(nonHead.hash));
    }


    @Test
    public void loadClassFragmentFromNoFragmentHeadTest() throws InterruptedException {
        Bundle bundle = createClassBundle();
        List<Transaction> txs = bundle.getTransactions();
        for(int i = txs.size()-1; i>=0 ;i--){
            ict.submit(txs.get(i));
        }
        Thread.sleep(50);

        Transaction nonHead = txs.get(0);
        ClassFragment classFragment = serializationModule.loadClassFragment(nonHead.hash);
        assertNull(classFragment);
    }

    @Test
    public void testGetDataFragment(){
        ClassFragment classFragment = new ClassFragment.Builder()
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();

        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        byte[] data = new byte[]{1,1,1,1,1,1,1,1,1};
        builder.setData(data);

        ClassFragment classFragment2 = new ClassFragment.Builder()
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();
        DataFragment fragment0 = serializationModule.publishBundleFragment(builder);

        DataFragment.Builder builder1 = new DataFragment.Builder(classFragment2);
        data = new byte[]{-1,-1,-1,-1,-1,-1,-1,-1,-1};
        builder1.setData(data);
        builder1.setReference(0, fragment0);

        DataFragment fragment1 = serializationModule.publishBundleFragment(builder1);
        DataFragment fragment0Clone = serializationModule.getDataFragment(fragment1, 0);

        assertEquals(fragment0.getReference(0), fragment0Clone.getReference(0));
        assertArrayEquals(fragment0.getData(), fragment0Clone.getData());
    }

    @Test
    public void testGetDataFragmentLongData(){
        ClassFragment classFragment = new ClassFragment.Builder()
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();

        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        byte[] data = TestUtils.randomTrits(25002);
        builder.setData(data);
        DataFragment fragment0 = serializationModule.publishBundleFragment(builder);

        ClassFragment classFragment2 = new ClassFragment.Builder()
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();

        builder = new DataFragment.Builder(classFragment2);
        data = TestUtils.randomTrits(25002);
        builder.setData(data);
        builder.setReference(0, fragment0);
        DataFragment fragment1 = serializationModule.publishBundleFragment(builder);

        DataFragment fragment0Clone = serializationModule.getDataFragment(fragment1, 0);

        assertEquals(fragment0.getReference(0), fragment0Clone.getReference(0));
        assertArrayEquals(fragment0.getData(), fragment0Clone.getData());
        assertArrayEquals(data, fragment1.getData());
    }

    private String createAndSubmitBundle(String dataBundleClassHash, byte[] data) throws InterruptedException {
        Bundle bundle = createMyDataBundle(dataBundleClassHash, data);
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
        Thread.sleep(50);
        return bundle.getHead();
    }

    @Test
    public void testFindDataFragments() throws InterruptedException {
        String classHash = TestUtils.randomHash();
        createAndSubmitBundle(classHash, null);
        createAndSubmitBundle(TestUtils.randomHash(), null);
        Set<DataFragment> fragments = serializationModule.findDataFragmentForClassHash(classHash);
        assertEquals(1,fragments.size());
        DataFragment fragment = fragments.iterator().next();
        assertEquals(classHash, fragment.getClassHash());
    }

    @Test
    public void testFindFragmentReferencing() throws InterruptedException {
        String classHash = TestUtils.randomHash();
        String classHash2 = TestUtils.randomHash();
        String dataTransactionHash = createAndSubmitBundle(classHash, null);
        DataFragment.Builder builder = new DataFragment.Builder(classHash2);
        builder.setReference(0, dataTransactionHash);
        DataFragment referencingFragment = serializationModule.publishBundleFragment(builder);

        Set<DataFragment> fragments = serializationModule.findDataFragmentReferencing(classHash2,dataTransactionHash,0);
        assertEquals(1,fragments.size());
        DataFragment fragment = fragments.iterator().next();
        assertEquals(referencingFragment.getClassHash(), fragment.getClassHash());
        assertEquals(referencingFragment.getHeadTransaction().hash, fragment.getHeadTransaction().hash);
    }

    @Test
    public void loadClassFragmentFromClassHashTest(){
        assertThrows(IllegalArgumentException.class, ()-> serializationModule.loadClassFragmentForClassHash(null));
        assertThrows(IllegalArgumentException.class, ()-> serializationModule.loadClassFragmentForClassHash(Trytes.NULL_HASH));
        assertNull(serializationModule.loadClassFragmentForClassHash(TestUtils.randomHash()));
        ClassFragment.Builder classFragmentBuilder = new ClassFragment.Builder();
        classFragmentBuilder.withDataSize(99);
        classFragmentBuilder.addReferencedClasshash(TestUtils.randomHash());
        ClassFragment classFragment = serializationModule.publishBundleFragment(classFragmentBuilder);

        ClassFragment reloadedClassFragment = serializationModule.loadClassFragmentForClassHash(classFragment.getClassHash());
        assertEquals(classFragment.getClassHash(),reloadedClassFragment.getClassHash());
    }

    @Test
    public void loadClassFragmentFromTransactionHashTest(){
        assertThrows(IllegalArgumentException.class, ()-> serializationModule.loadClassFragment(null));
        assertThrows(IllegalArgumentException.class, ()-> serializationModule.loadClassFragment(Trytes.NULL_HASH));
        assertNull(serializationModule.loadClassFragment(TestUtils.randomHash()));
        ClassFragment.Builder classFragmentBuilder = new ClassFragment.Builder();
        classFragmentBuilder.withDataSize(99);
        classFragmentBuilder.addReferencedClasshash(TestUtils.randomHash());
        ClassFragment classFragment = serializationModule.publishBundleFragment(classFragmentBuilder);

        ClassFragment loaded = serializationModule.loadClassFragment(classFragment.getHeadTransaction().hash);
        assertEquals(classFragment.getClassHash(), loaded.getClassHash());
    }


    @Test
    public void getDataTest() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, ()->serializationModule.getData((DataFragment) null));
        assertThrows(IllegalArgumentException.class, ()->serializationModule.getData((String) null));
        assertThrows(IllegalArgumentException.class, ()->serializationModule.getData(Trytes.NULL_HASH));
        assertNull(serializationModule.getData(TestUtils.randomHash()));
        byte[] data = new byte[]{1,1,1,0,0,0,-1,-1,-1};
        String dataFragmentHash = createAndSubmitBundle(TestUtils.randomHash(),data);
        byte[] loadedData = serializationModule.getData(dataFragmentHash);
        assertArrayEquals(data, loadedData);

        DataFragment dataFragment = serializationModule.loadDataFragment(dataFragmentHash);
        loadedData = serializationModule.getData(dataFragment);
        assertArrayEquals(data, loadedData);
    }


    private Bundle createMyDataBundle(String dataBundleClassHash, byte[] data) {
        DataFragment.Builder builder = new DataFragment.Builder(dataBundleClassHash);
        builder.setData(data);
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
        ClassFragment.Builder builder = new ClassFragment.Builder();
        builder.withDataSize(33);
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

        registerReturnHandler(env,effect -> {
                String[] split = effect.toString().split(";");
                if(split[0].equals("1")) {
                    assertEquals(2, split.length);
                    assertEquals("9ZHHQLPWVSDOJIOROTRHQNGMLMZAWISFMYSFSECRCJBZRHNZJCFWGBASZITSZAGMSQRIMZSJGGCLLWI9Y", split[1]);
                    done.set(true);
                }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"1;25");

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

        ict.submitEffect(env,"2;25;"+TestUtils.randomHash()+";"+TestUtils.randomHash());

        countDownLatch.await(200, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }


    @Test
    public void publishDataFragmentEEETest() throws InterruptedException {
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

        ict.submitEffect(env,"1;DATA;"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"+TestUtils.randomHash());

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void publishDataFragment2EEETest() throws InterruptedException {
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

        ict.submitEffect(env,"2;DATA;"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"
                +TestUtils.randomHash()+";"+TestUtils.randomHash());

        countDownLatch.await(10000, TimeUnit.MILLISECONDS);
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
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"1;25;"+TestUtils.randomHash()+";"+TestUtils.randomHash());

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
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"2;25;"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"+TestUtils.randomHash());

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
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"3;25;"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"
                +TestUtils.randomHash()+";"+TestUtils.randomHash());

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }




    @Test
    public void prepareDataFragmentEEETest() throws InterruptedException {
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

        ict.submitEffect(env,"1;DATA;"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"+TestUtils.randomHash());

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void prepareDataFragment2EEETest() throws InterruptedException {
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

        ict.submitEffect(env,"2;DATA;"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"
                +TestUtils.randomHash()+";"+TestUtils.randomHash());

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
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertFalse(Utils.isBundleHead(split[1]));
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"1;25;"+TestUtils.randomHash()+";"+TestUtils.randomHash());

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
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertFalse(Utils.isBundleHead(split[1]));
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"2;25;"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"+TestUtils.randomHash());

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
                assertEquals(2, split.length);
                assertEquals(Transaction.Field.TRUNK_HASH.tryteLength, split[1].length());
                assertFalse(Utils.isBundleHead(split[1]));
                done.set(true);
            }
        }, countDownLatch, throwableHolder);

        ict.submitEffect(env,"3;25;"+TestUtils.randomHash()+";"+TestUtils.randomHash()+";"
                +TestUtils.randomHash()+";"+TestUtils.randomHash());

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void buildGetDataEEETest() throws InterruptedException {
        final FunctionEnvironment env = new FunctionEnvironment("Serialization.ixi","getData");

        String randomClassHah = TestUtils.randomHash();
        DataFragment.Builder builder = new DataFragment.Builder(randomClassHah);
        builder.setData("DATA");
        BundleFragment fragment = serializationModule.publishBundleFragment(builder);
        String tx_hash = fragment.getHeadTransaction().hash;

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        final AtomicBoolean done2 = new AtomicBoolean(false);

        registerReturnHandler(env,effect -> {
            String[] split = effect.toString().split(";");
            if(split[0].equals("search")) {
                assertEquals(2, split.length);
                assertEquals("DATA", split[1]);
                done2.set(true);
            }
        }, countDownLatch, throwableHolder);
        ict.submitEffect(env,"search;"+tx_hash);
        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(done2.get());
        if(throwableHolder.throwable!=null){
            fail(throwableHolder.throwable);
        }
    }

    @Test
    public void registerListenerTest() throws InterruptedException {
        String randomClassHash = TestUtils.randomHash();
        String envId = "test0";
        final Environment environment = new Environment(envId);
        serializationModule.registerDataListener(randomClassHash, envId);
        DataFragment.Builder fragmentBuilder = new DataFragment.Builder(randomClassHash).setData("DATADATA");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean done = new AtomicBoolean(false);
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        EffectListener<DataFragment> checker = new EffectListener<DataFragment>() {
            @Override
            public void onReceive(DataFragment effect) {
                try {
                    assertEquals("DATADATA", Trytes.fromTrits(effect.getData()));
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
}
