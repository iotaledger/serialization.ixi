package org.iota.ict.ixi.serialization;

import org.iota.ict.Ict;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.ixi.IxiModuleInfo;
import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.model.*;
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
        DataFragment fragment0 = builder.build();

        ClassFragment classFragment2 = new ClassFragment.Builder()
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();

        builder = new DataFragment.Builder(classFragment2);
        data = new byte[]{-1,-1,-1,-1,-1,-1,-1,-1,-1};
        builder.setData(data);
        builder.setReference(0, fragment0);
        DataFragment fragment1 = builder.build();

        serializationModule.publishBundleFragment(fragment0);
        serializationModule.publishBundleFragment(fragment1);

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
        DataFragment fragment0 = builder.build();

        ClassFragment classFragment2 = new ClassFragment.Builder()
                .addReferencedClasshash(TestUtils.random(81))
                .addReferencedClasshash(TestUtils.random(81))
                .build();

        builder = new DataFragment.Builder(classFragment2);
        data = TestUtils.randomTrits(25002);
        builder.setData(data);
        builder.setReference(0, fragment0);
        DataFragment fragment1 = builder.build();

        serializationModule.publishBundleFragment(fragment0);
        serializationModule.publishBundleFragment(fragment1);

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
        DataFragment referencingFragment = serializationModule.buildDataFragment(builder);
        serializationModule.publishBundleFragment(referencingFragment);

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
        ClassFragment classFragment = serializationModule.buildClassFragment(classFragmentBuilder);
        serializationModule.publishBundleFragment(classFragment);

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
        ClassFragment classFragment = serializationModule.buildClassFragment(classFragmentBuilder);
        serializationModule.publishBundleFragment(classFragment);

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
        ClassFragment.Prepared preparedClass = serializationModule.prepareMetadata(builder);
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

}
