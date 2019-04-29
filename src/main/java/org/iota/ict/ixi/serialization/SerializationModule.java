package org.iota.ict.ixi.serialization;

import org.iota.ict.eee.Environment;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.serialization.model.*;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipPreprocessor;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class SerializationModule extends IxiModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationModule.class);

    public static SerializationModule INSTANCE;

    public SerializationModule(Ixi ixi) {
        super(ixi);
        INSTANCE = this;
    }

    private Map<DataFragment.Filter, DataFragment.Listener> listeners = new HashMap<>();

    @Override
    public void run() {
        ;
    }

    @Override
    public void onStart() {
        super.onStart();
        ixi.addListener(new BundleListener());
        LOGGER.info("Serialization.ixi started...");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        LOGGER.info("Serialization.ixi terminated.");
    }


    public ClassFragment buildClassFragment(ClassFragment.Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        return builder.build();
    }

    public DataFragment buildDataFragment(DataFragment.Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        return builder.build();
    }

    /**
     * @return the ClassFragment with head transaction identified by transactionHash,
     * or null if the transaction is not the head of a valid ClassFragment.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     */
    public ClassFragment loadClassFragment(String transactionHash) {
        if (!Utils.isValidHash(transactionHash)) {
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid transaction hash");
        }
        if(Trytes.NULL_HASH.equals(transactionHash)){
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid classFragment hash");
        }
        Transaction tx = ixi.findTransactionByHash(transactionHash);
        if(tx==null){
            return null;
        }
        if(!ClassFragment.isHead(tx)){
            return null;
        }
        return new ClassFragment(tx);
    }

    public ClassFragment loadClassFragmentForClassHash(String classHash){
        if(classHash==null || classHash.equals(Trytes.NULL_HASH)){
            throw new IllegalArgumentException("searched classHash cannot be null");
        }
        //TODO optimize me
        Set<Transaction> transactions = ixi.findTransactionsByAddress(ClassFragment.METACLASS_HASH);
        for(Transaction tx:transactions){
            try {
                ClassFragment classFragment = new ClassFragment(tx);
                if(classFragment.getClassHash().equals(classHash))return classFragment;
            }catch (IllegalArgumentException e){
                //not a valid ClassFragment
            }
        }
        return null;
    }
    /**
     * @return the DataFragment with head transaction identified by transactionHash,
     * or null if the transaction is not the head of a DataFragment.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     */
    public DataFragment loadDataFragment(String transactionHash) {
        if (!Utils.isValidHash(transactionHash)) {
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid transaction hash");
        }
        if(Trytes.NULL_HASH.equals(transactionHash)){
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid data fragment transaction hash");
        }
        Transaction tx = ixi.findTransactionByHash(transactionHash);
        if(tx==null){
            return null;
        }
        DataFragment dataFragment = new DataFragment(tx);
        return dataFragment;
    }

    /**
     * @param classHash
     * @return all DataFragment for a given classHash
     */
    public Set<DataFragment> findDataFragmentForClassHash(String classHash){
        Set<Transaction> transactions = ixi.findTransactionsByAddress(classHash);
        Set<DataFragment> ret = new HashSet<>();
        for(Transaction t:transactions){
            try{
                DataFragment fragment = new DataFragment(t);
                ret.add(fragment);
            }catch (IllegalArgumentException e){
                //not a valid bundle fragment.
            }
        }
        return ret;
    }

    /**
     * @param classHash the classHash of the searched fragments
     * @param referencedTransactionHash the transaction hash of the dataFragment to be referenced
     * @param index index of the reference
     * @return all DataFragment referencing *referencedTransactionHash* from reference at index *index*
     */
    public Set<DataFragment> findDataFragmentReferencing(String classHash, String referencedTransactionHash, int index){
        if(referencedTransactionHash==null || referencedTransactionHash.equals(Trytes.NULL_HASH)){
            throw new IllegalArgumentException("referencedTransactionHash cannot be null");
        }
        if(classHash==null || classHash.equals(Trytes.NULL_HASH)){
            throw new IllegalArgumentException("classHash hash cannot be null");
        }
        if(index<0){
            throw new IllegalArgumentException("index cannot be < 0");
        }
        //TODO : optimize me
        Set<Transaction> transactions = ixi.findTransactionsByAddress(classHash);
        Set<DataFragment> ret = new HashSet<>();
        for(Transaction t:transactions){
            try{
                DataFragment fragment = new DataFragment(t);
                if(fragment.getReference(index).equals(referencedTransactionHash))
                    ret.add(fragment);
            }catch (IllegalArgumentException e){
                //not a valid bundle fragment.
            }
        }
        return ret;
    }
    public DataFragment getFragmentAtIndex(DataFragment fragment, int index){
        return loadDataFragment(fragment.getReference(index));
    }

    public byte[] getData(DataFragment dataFragment){
        if(dataFragment==null){
            throw new IllegalArgumentException("dataFragment cannot be null");
        }
        return dataFragment.getData();
    }

    public byte[] getData(String dataFragmentTransactionHash){
        DataFragment dataFragment = loadDataFragment(dataFragmentTransactionHash);
        if(dataFragment==null){
            return null;
        }
        return dataFragment.getData();
    }

    /**
     * @return the value in trytes of key at index
     * @throws IndexOutOfBoundsException when index is invalid
     */
    public byte[] getDataAtIndex(DataFragment dataFragment, int index) {
        if(dataFragment==null){
            throw new IllegalArgumentException("dataFragment cannot be null");
        }
        if(index<0){
            throw new IllegalArgumentException("index must be >= 0");
        }
        DataFragment referenced = getFragmentAtIndex(dataFragment, index);
        if(referenced!=null){
            return referenced.getData();
        }
        return null;
    }

    //TODO : review listeners mechanics
    /**
     * Register a dataListener to be notified when a StructuredDataFragment of a particular class clazz is received.
     * @param listener : the listener to callback
     */
    public  void registerDataListener(String classHash, final DataFragment.Listener listener) {
        DataFragment.Filter matcher = dataFragment ->
                dataFragment.getClassHash().equals(classHash);
        registerDataListener(matcher, listener);
    }


    /**
     * Register a dataListener to be notified when a StructuredDataFragment is received.
     * @param matcher : a filter for fragment of interest
     * @param listener : the listener to callback
     */
    public void registerDataListener(DataFragment.Filter matcher, DataFragment.Listener listener) {
        listeners.put(matcher, listener);
    }

    void notifyListeners(DataFragment dataFragment) {
        for (DataFragment.Filter dataFragmentFilter : listeners.keySet()) {
            if (dataFragmentFilter.match(dataFragment)) {
                listeners.get(dataFragmentFilter).onData(dataFragment);
            }
        }
    }


    public <T extends BundleFragment> T publishBundleFragment(T fragment){
        Stack<Transaction> stack = new Stack<>();
        Transaction t = fragment.getHeadTransaction();
        stack.push(t);
        while(t.getTrunk()!=null) {
            t = t.getTrunk();
            stack.push(t);
        }
        while(!stack.isEmpty()) {
            ixi.submit(stack.pop());
        }
        return fragment;
    }
    /**
     * Build a StructuredDataFragment.Prepared for data.
     * The StructuredDataFragment.Prepared can be used later to insert the dataFragment in Bundle.
     * @return a preparedDataFragment.
     */
    public DataFragment.Prepared prepare(DataFragment.Builder builder) {
        return  builder.prepare();
    }

    /**
     * Build a MetadataFragment.Prepared from builder.
     * The MetadataFragment.Prepared can be used later to insert the metadataFragment in a Bundle.
     * @return a prepared MetadataFragment.
     */
    public ClassFragment.Prepared prepareMetadata(ClassFragment.Builder builder) {
        return  builder.prepare();
    }


    public DataFragment getDataFragment(DataFragment dataFragment, int index){
        String headTransactionHash = dataFragment.getReference(index);
        if(headTransactionHash!=null){
            Transaction headTransaction = ixi.findTransactionByHash(headTransactionHash);
            if(headTransaction!=null){
                return new DataFragment(headTransaction);
            }
        }
        return null;
    }

    /**
     * Receive Bundles and inspect them to find metadata fragments or structured data fragments.
     * When a metadataFragment is found: it is registered @SerializationModule.
     * When a structureData fragment is found and it's metadata fragment is available : the structured fragment is published.
     */
    private class BundleListener extends GossipPreprocessor {

        public BundleListener() {
            super(ixi, -4000);
        }

        @Override
        public void onReceive(GossipEvent effect) {
            Transaction tx = effect.getTransaction();
            if(tx.isBundleHead){
                Bundle bundle = new Bundle(tx);
                if(bundle.isComplete() && bundle.isStructureValid()){
                    processBundle(bundle, null);
                }else{
                    throw new RuntimeException("Received an incomplete or invalid bundle. This shouldn't append");
                }
            }
        }

        @Override
        public Environment getEnvironment() {
            return Constants.Environments.GOSSIP_PREPROCESSOR_CHAIN;
        }


        private void processBundle(Bundle bundle, Transaction startingTransaction){
            Transaction t = startingTransaction==null?bundle.getHead():startingTransaction;

            //search for fragment head
            while(!t.isBundleTail && !isFragmentHead(t)){
                t = t.getTrunk();
            }

            Transaction fragmentTail = null;
            if(isFragmentHead(t)){
                if(ClassFragment.isHead(t)){
                    if(foundClassFragmentTail(bundle, t)) {
                        fragmentTail = processClassFragment(t);
                    }
                }else{
                    if(foundDataFragmentTail(bundle, t)) {
                        fragmentTail = processDataFragment(t);
                    }
                }
            }

            //process the remainder of the bundle
            if(fragmentTail!=null && !fragmentTail.isBundleTail && fragmentTail.getTrunk()!=null){
                processBundle(bundle, fragmentTail.getTrunk());
            }
        }

        private boolean foundClassFragmentTail(Bundle bundle, Transaction fragmentHead){
            Transaction t = fragmentHead;
            while(t!=null && !ClassFragment.isTail(t)){
                t = t.getTrunk();
                if(!bundle.getTransactions().contains(t)){
                    return false;
                }
            }
            return ClassFragment.isTail(t);
        }

        private boolean foundDataFragmentTail(Bundle bundle, Transaction fragmentHead){
            Transaction t = fragmentHead;
            while(t!=null && !DataFragment.isTail(t)){
                t = t.getTrunk();
                if(!bundle.getTransactions().contains(t)){
                    return false;
                }
            }
            return t!=null && DataFragment.isTail(t);
        }

        private boolean isFragmentHead(Transaction transaction){
            return ClassFragment.isHead(transaction) || DataFragment.isHead(transaction);
        }

        private Transaction processClassFragment(Transaction fragmentHead){
            assert ClassFragment.isHead(fragmentHead);
            Transaction t = fragmentHead;
            ClassFragment classFragment = new ClassFragment(t);
            if(classFragment != null){
                return classFragment.getTailTransaction();
            }
            return t;
        }

        private Transaction processDataFragment(Transaction fragmentHead){
            String classHash = fragmentHead.address();
            ClassFragment classFragment = loadClassFragment(classHash);
            if(classFragment!=null){
                DataFragment dataFragment = new DataFragment(fragmentHead);
                notifyListeners(dataFragment);
            }
            return null;
        }
    }

}
