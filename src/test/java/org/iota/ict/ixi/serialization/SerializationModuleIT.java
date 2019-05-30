package org.iota.ict.ixi.serialization;

import org.iota.ict.Ict;
import org.iota.ict.eee.EffectListener;
import org.iota.ict.eee.Environment;
import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.model.*;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.bundle.BundleBuilder;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.properties.EditableProperties;
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
@SuppressWarnings("WeakerAccess")
public class SerializationModuleIT {

    private static SerializationModule serializationModule;
    private static Ict ict;

    @BeforeAll
    public static void startIct() throws Exception {
        //System.setProperty("log4j.configurationFile","log4j2.xml");
        EditableProperties properties = new EditableProperties().host("localhost").port(2387).minForwardDelay(0).maxForwardDelay(10).guiEnabled(false);
        ict = new Ict(properties.toFinal());
        serializationModule = (SerializationModule) ict.getModuleHolder().loadVirtualModule(SerializationModule.class, "Serialization.ixi");
        ict.getModuleHolder().startAllModules();
    }

    @AfterAll
    public static void terminate(){
        serializationModule.terminate();
    }


    @Test
    public void loadDataFragmentFromNoFragmentHeadTest() {
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
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
    public void loadClassFragmentFromNoFragmentHeadTest() {
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
    public void testLoadReferencedDataFragment(){
        ClassFragment classFragment = serializationModule.publishBundleFragment(new ClassFragment.Builder()
                .addAttribute(9)
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81)));
        byte[] data = new byte[]{1,1,1,1,1,1,1,1,1};
        DataFragment.Builder builder = new DataFragment.Builder(classFragment)
                .setAttribute(0,data);

        ClassFragment classFragment2 = new ClassFragment.Builder()
                .addAttribute(9)
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();
        DataFragment fragment0 = serializationModule.publishBundleFragment(builder);

        data = new byte[]{-1,-1,-1,-1,-1,-1,-1,-1,-1};
        DataFragment.Builder builder1 = new DataFragment.Builder(classFragment2)
                .setAttribute(0,data)
                .setReference(0, fragment0);

        DataFragment fragment1 = serializationModule.publishBundleFragment(builder1);
        DataFragment fragment0Clone = serializationModule.loadReferencedDataFragment(fragment1, 0);

        assertEquals(fragment0.getReference(0), fragment0Clone.getReference(0));
        assertEquals(fragment0.getAttributeAsTryte(0), fragment0Clone.getAttributeAsTryte(0));
    }

    @Test
    public void testLoadReferencedDataFragmentLongData(){
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

        DataFragment fragment0Clone = serializationModule.loadReferencedDataFragment(fragment1, 0);

        assertEquals(fragment0.getReference(0), fragment0Clone.getReference(0));
        assertEquals(fragment0.getAttributeAsTryte(0), fragment0Clone.getAttributeAsTryte(0));
        assertEquals(25002, data.length);
        assertEquals(Trytes.fromTrits(data), fragment1.getAttributeAsTryte(0));
    }

    private String createAndSubmitBundle(ClassFragment classFragment, String data) {
        Bundle bundle = createMyDataBundle(classFragment, data);
        Transaction bundleHead = submitBundle(bundle);
        Transaction fragmentHead = bundleHead.getTrunk();
        return fragmentHead.hash;
    }

    private Transaction submitBundle(Bundle bundle) {
        List<Transaction> txs = bundle.getTransactions();
        for(int i = txs.size()-1; i>=0 ;i--){
            ict.submit(txs.get(i));
        }
        safeSleep(50);
        return bundle.getHead();
    }

    @Test
    public void testFindDataFragments() {
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        createAndSubmitBundle(classFragment, null);
        ClassFragment classFragment2 = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        createAndSubmitBundle(classFragment2, null);
        Set<DataFragment> fragments = serializationModule.findDataFragmentForClassHash(classFragment.getClassHash());
        assertEquals(1,fragments.size());
        DataFragment fragment = fragments.iterator().next();
        assertEquals(classFragment.getClassHash(), fragment.getClassHash());
    }

    @Test
    public void testFindFragmentReferencing() {
        ClassFragment class1 = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        ClassFragment class2 = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        String dataTransactionHash = createAndSubmitBundle(class1, null);
        DataFragment.Builder builder = new DataFragment.Builder(class2)
                .setReference(0, dataTransactionHash);
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

        fragments = serializationModule.findDataFragmentReferencing(dataTransactionHash,
                dataFragment -> dataFragment.getAttributeAsTryte(0).startsWith("AAA"));
        assertEquals(1,fragments.size());

    }

    @Test
    public void findDataFragmentReferencingAtIndexTest(){
        ClassFragment referencedClass = TestUtils.getRandomPublishedClassFragment(serializationModule,27, 12, 0);
        DataFragment.Builder referencedFragmentBuilder = new DataFragment.Builder(referencedClass)
                .setAttribute(2,"QWERTY")
                .setAttribute(0,"A")
                .setAttribute(0,"BBBBBBBBBBBBB");
        DataFragment referencedFragment = serializationModule.publishBundleFragment(referencedFragmentBuilder);

        ClassFragment.Builder referencingClass1Builder = new ClassFragment.Builder()
                .addAttribute(1)
                .addReferencedClasshash(referencedClass.getClassHash());
        ClassFragment referencingClass1 = serializationModule.publishBundleFragment(referencingClass1Builder);

        ClassFragment.Builder referencingClass2Builder = new ClassFragment.Builder()
                .addAttribute(1)
                .addReferencedClasshash(Trytes.NULL_HASH)
                .addReferencedClasshash(referencedClass.getClassHash());
        ClassFragment referencingClass2 = serializationModule.publishBundleFragment(referencingClass2Builder);

        safeSleep(100);

        DataFragment f1 = serializationModule.publishBundleFragment(
                new DataFragment.Builder(referencingClass1)
                .setAttribute(0,"A")
                .setReference(0,referencedFragment.getHeadTransaction().hash));

        DataFragment f2 = serializationModule.publishBundleFragment(
                new DataFragment.Builder(referencingClass2)
                .setAttribute(0,"B")
                .setReference(0,referencedFragment.getHeadTransaction().hash)
                .setReference(1,referencedFragment.getHeadTransaction().hash));


        DataFragment f3 = serializationModule.publishBundleFragment(
                new DataFragment.Builder(referencingClass2)
                .setAttribute(0,"C")
                .setReference(1,referencedFragment.getHeadTransaction().hash));

        DataFragment f4 = serializationModule.publishBundleFragment(
                new DataFragment.Builder(referencingClass2)
                .setAttribute(0,"D"));

        safeSleep(100);

        Set<DataFragment> rs1 = serializationModule.findDataFragmentReferencingAtIndex(0, referencedFragment.getHeadTransaction().hash, null);
        Set<DataFragment> rs2 = serializationModule.findDataFragmentReferencingAtIndex(1, referencedFragment.getHeadTransaction().hash, null);
        Set<DataFragment> rs3 = serializationModule.findDataFragmentReferencing(referencedFragment.getHeadTransaction().hash, dataFragment ->
                dataFragment.getReference(0).equals(referencedFragment.getHeadTransaction().hash) &&
                dataFragment.getReference(1).equals(referencedFragment.getHeadTransaction().hash));
        Set<DataFragment> rs4 = serializationModule.findDataFragmentReferencingAtIndex(1, referencedFragment.getHeadTransaction().hash,
                dataFragment -> dataFragment.getAttributeAsTryte(0).equals("C"));

        assertEquals(2, rs1.size());
        assertEquals(2, rs2.size());
        assertEquals(1, rs3.size());
        assertEquals(1, rs4.size());
    }

    @Test
    public void loadClassFragmentFromClassHashTest() {
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
    public void testFindClassFragmentReferencing() {
        ClassFragment class1 = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        ClassFragment class2 = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
        ClassFragment.Builder referencingBuilder = new ClassFragment.Builder().addReferencedClass(class1).addReferencedClass(class2);
        ClassFragment referencing =serializationModule.publishBundleFragment(referencingBuilder);
        safeSleep(100);
        Set<ClassFragment> rs1 = serializationModule.findClassFragmentReferencing(class1.getClassHash(),null);
        assertEquals(1,rs1.size());

        Set<ClassFragment> rs2 = serializationModule.findClassFragmentReferencingAtIndex(0,class1.getClassHash(),null);
        assertEquals(1,rs2.size());

        Set<ClassFragment> rs3 = serializationModule.findClassFragmentReferencingAtIndex(1,class1.getClassHash(),null);
        assertEquals(0,rs3.size());

        Set<ClassFragment> rs4 = serializationModule.findClassFragmentReferencing(class1.getClassHash(), classFragment -> classFragment.getAttributeCount()==1);
        assertEquals(0,rs4.size());

        Set<ClassFragment> rs5 = serializationModule.findClassFragmentReferencingAtIndex(0,class1.getClassHash(),classFragment -> classFragment.getAttributeCount()==0);
        assertEquals(1,rs5.size());

        Set<ClassFragment> rs6 = serializationModule.findClassFragmentReferencing(referencing.getClassHash(),null);
        assertEquals(0,rs6.size());
    }

    @Test
    public void getAttributeTrytesTest() {
        assertThrows(IllegalArgumentException.class, ()->serializationModule.getAttributeTrytes((DataFragment) null, 1));
        assertThrows(IllegalArgumentException.class, ()->serializationModule.getAttributeTrytes((String) null, 1));
        assertThrows(IllegalArgumentException.class, ()->serializationModule.getAttributeTrytes(Trytes.NULL_HASH, 0));
        assertEquals("",serializationModule.getAttributeTrytes(TestUtils.randomHash(), 0));
        String data = "ABC";
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,3);
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

    @Test
    public void registerListenerTest() throws InterruptedException {
        ClassFragment classFragment = TestUtils.getRandomPublishedClassFragment(serializationModule,27);
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

    @Test
    public void persistenceCleanerTest(){
        String randomHash = TestUtils.randomHash();
        ClassFragment classFragment = new ClassFragment.Builder().addAttribute(10).build();
        ClassFragment classFragment2 = new ClassFragment.Builder().addReferencedClass(classFragment).build();
        DataFragment dataFragment = new DataFragment.Builder(classFragment2).setReference(0, randomHash).build();

        serializationModule.persistence.persist(dataFragment);
        Set<DataFragment> rs = serializationModule.findDataFragmentReferencing(randomHash, null);
        assertEquals(1,rs.size());
        safeSleep(5+serializationModule.persistence.delay*1000);
        rs = serializationModule.findDataFragmentReferencing(randomHash, null);
        assertEquals(0,rs.size());

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

}
