package org.iota.ict.ixi.serialization;

import org.iota.ict.eee.Environment;
import org.iota.ict.eee.call.EEEFunction;
import org.iota.ict.eee.call.FunctionEnvironment;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.serialization.model.BundleFragment;
import org.iota.ict.ixi.serialization.model.ClassFragment;
import org.iota.ict.ixi.serialization.model.DataFragment;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipPreprocessor;
import org.iota.ict.utils.Trytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class SerializationModule extends IxiModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationModule.class);

    private Persistence persistence = new Persistence();
    private Map<DataFragment.Filter, String> listeners = new HashMap<>();

    public SerializationModule(Ixi ixi) {
        super(ixi);
    }

    @Override
    public void run() {
        EEEFunctions.init(this, ixi);
        GossipPreprocessor gossipPreprocessor = new GossipPreprocessor(ixi, -4000);
        ixi.addListener(gossipPreprocessor);
        GossipEventHandler gossipEventHandler = new GossipEventHandler();
        try{
            while(isRunning()){
                GossipEvent gossipEvent = gossipPreprocessor.takeEffect();
                gossipEventHandler.handleEvent(gossipEvent);
            }
        }catch (InterruptedException e){
            LOGGER.info("Serialization.ixi interrupted...");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LOGGER.info("Serialization.ixi started...");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        runningThread.interrupt();
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
        if (Trytes.NULL_HASH.equals(transactionHash)) {
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid classFragment hash");
        }
        Transaction tx = ixi.findTransactionByHash(transactionHash);
        if (tx == null) {
            return null;
        }
        if (!ClassFragment.isHead(tx)) {
            return null;
        }
        return new ClassFragment(tx);
    }

    public ClassFragment loadClassFragmentForClassHash(String classHash) {
        if (classHash == null || classHash.equals(Trytes.NULL_HASH)) {
            throw new IllegalArgumentException("searched classHash cannot be null");
        }
        return persistence.search(classHash);
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
        if (Trytes.NULL_HASH.equals(transactionHash)) {
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid data fragment transaction hash");
        }
        Transaction tx = ixi.findTransactionByHash(transactionHash);
        if (tx == null) {
            return null;
        }
        ClassFragment classFragment = persistence.search(tx.address());
        if(classFragment!=null) {
            DataFragment dataFragment = new DataFragment(tx, classFragment);
            return dataFragment;
        }
        return null;
    }

    /**
     * @param classHash searched classHash
     * @return all DataFragment for a given classHash
     */
    public Set<DataFragment> findDataFragmentForClassHash(String classHash) {
        Set<Transaction> transactions = ixi.findTransactionsByAddress(classHash);
        Set<DataFragment> ret = new HashSet<>();
        for (Transaction t : transactions) {
            try {
                ClassFragment classFragment = persistence.search(t.address());
                if(classFragment!=null) {
                    DataFragment fragment = new DataFragment(t, classFragment);
                    ret.add(fragment);
                }
            } catch (IllegalArgumentException e) {
                //not a valid bundle fragment.
            }
        }
        return ret;
    }

    /**
     * @param referencedTransactionHash the transaction hash of the dataFragment to be referenced
     * @return all DataFragment referencing *referencedTransactionHash* from reference at index *index*
     */
    public Set<DataFragment> findDataFragmentReferencing(String referencedTransactionHash, DataFragment.Filter filter) {
        if (referencedTransactionHash == null || referencedTransactionHash.equals(Trytes.NULL_HASH)) {
            throw new IllegalArgumentException("referencedTransactionHash cannot be null");
        }
        Set<DataFragment> ret = persistence.referencing.get(referencedTransactionHash);
        if(ret==null) return Collections.EMPTY_SET;
        if(filter==null){
            return ret;
        }
        if(ret.size()==0)return Collections.EMPTY_SET;
        HashSet<DataFragment> filtered = new HashSet<>();
        for(DataFragment f:ret){
            if(filter.match(f)){
                filtered.add(f);
            }
        }
        return filtered;
    }

    public DataFragment getFragmentAtIndex(DataFragment fragment, int index) {
        return loadDataFragment(fragment.getReference(index));
    }

    public String getAttributeTrytes(DataFragment dataFragment, int index) {
        if (dataFragment == null) {
            throw new IllegalArgumentException("dataFragment cannot be null");
        }
        return dataFragment.getAttributeAsTryte(index);
    }

    public String getAttributeTrytes(String dataFragmentTransactionHash, int index) {
        DataFragment dataFragment = loadDataFragment(dataFragmentTransactionHash);
        if (dataFragment == null) {
            return null;
        }
        return dataFragment.getAttributeAsTryte(index);
    }

    /**
     * Request submission of effect when dataFragment with a particular classHash is received.
     *
     * @param environmentId : environment where effect will be sent
     */
    public void registerDataListener(String classHash, String environmentId) {
        DataFragment.Filter matcher = dataFragment ->
                dataFragment.getClassHash().equals(classHash);
        registerDataListener(matcher, environmentId);
    }


    /**
     * Request submission of effect when dataFragment with a particular classHash is received.
     *
     * @param matcher  : a filter for fragment of interest
     * @param environmentId : environment where effect will be sent
     */
    public void registerDataListener(DataFragment.Filter matcher, String environmentId) {
        listeners.put(matcher, environmentId);
    }

    void notifyListeners(DataFragment dataFragment) {
        for (DataFragment.Filter dataFragmentFilter : listeners.keySet()) {
            if (dataFragmentFilter.match(dataFragment)) {
                Environment env = new Environment(listeners.get(dataFragmentFilter));
                ixi.submitEffect(env, dataFragment);
            }
        }
    }


    public <F extends BundleFragment,T extends BundleFragment.Builder<F>> F publishBundleFragment(T fragmentBuilder) {
        fragmentBuilder.setHeadFragment(true);
        return submitFragment(fragmentBuilder);
    }

    public <F extends BundleFragment,T extends BundleFragment.Builder<F>> F prepareBundleFragment(T fragmentBuilder) {
        return submitFragment(fragmentBuilder);
    }

    private <F extends BundleFragment,T extends BundleFragment.Builder<F>> F submitFragment(T fragmentBuilder) {
        if(fragmentBuilder.getReferencedTrunk()==null || Utils.isBundleHead(fragmentBuilder.getReferencedTrunk())){
            fragmentBuilder.setTailFragment(true);
        }
        F fragment = fragmentBuilder.build();
        Stack<Transaction> stack = new Stack<>();
        Transaction t = fragment.getHeadTransaction();
        stack.push(t);
        while (t.getTrunk() != null) {
            t = t.getTrunk();
            stack.push(t);
        }
        while (!stack.isEmpty()) {
            ixi.submit(stack.pop());
        }
        return fragment;
    }

    /**
     * Build a DataFragment.Prepared for data.
     * The DataFragment.Prepared can be used later to insert the dataFragment in Bundle.
     *
     * @return a preparedDataFragment.
     */
    public DataFragment.Prepared prepare(DataFragment.Builder builder) {
        return builder.prepare();
    }

    /**
     * Build a ClassFragment.Prepared from builder.
     * The ClassFragment.Prepared can be used later to insert the classFragment in a Bundle.
     *
     * @return a prepared ClassFragment.
     */
    public ClassFragment.Prepared prepareClassFragment(ClassFragment.Builder builder) {
        return builder.prepare();
    }


    public DataFragment getDataFragment(DataFragment dataFragment, int index) {
        String headTransactionHash = dataFragment.getReference(index);
        if (headTransactionHash != null) {
            Transaction headTransaction = ixi.findTransactionByHash(headTransactionHash);
            if (headTransaction != null) {
                ClassFragment classFragment = persistence.search(headTransaction.address());
                if(classFragment!=null) {
                    return new DataFragment(headTransaction, classFragment);
                }
            }
        }
        return null;
    }





    /**
     * Receive Bundles and inspect them to find class fragments or data fragments.
     */
    private class GossipEventHandler {

        public void handleEvent(GossipEvent effect) {
            Transaction tx = effect.getTransaction();
            if (tx.isBundleHead) {
                Bundle bundle = new Bundle(tx);
                if (bundle.isComplete() && bundle.isStructureValid()) {
                    processBundle(bundle, null);
                } else {
                    throw new RuntimeException("Received an incomplete or invalid bundle. This shouldn't append");
                }
            }
        }

        private void processBundle(Bundle bundle, Transaction startingTransaction) {
            Transaction t = startingTransaction == null ? bundle.getHead() : startingTransaction;

            //search for fragment head
            while (!t.isBundleTail && !isFragmentHead(t)) {
                t = t.getTrunk();
            }

            Transaction fragmentTail = null;
            if (isFragmentHead(t)) {
                if (ClassFragment.isHead(t)) {
                    if (foundClassFragmentTail(bundle, t)) {
                        fragmentTail = processClassFragment(t);
                    }
                } else {
                    if (foundDataFragmentTail(bundle, t)) {
                        fragmentTail = processDataFragment(t);
                    }
                }
            }

            //process the remainder of the bundle
            if (fragmentTail != null && !fragmentTail.isBundleTail && fragmentTail.getTrunk() != null) {
                processBundle(bundle, fragmentTail.getTrunk());
            }
        }

        private boolean foundClassFragmentTail(Bundle bundle, Transaction fragmentHead) {
            Transaction t = fragmentHead;
            while (t != null && !ClassFragment.isTail(t)) {
                t = t.getTrunk();
                if (!bundle.getTransactions().contains(t)) {
                    return false;
                }
            }
            return ClassFragment.isTail(t);
        }

        private boolean foundDataFragmentTail(Bundle bundle, Transaction fragmentHead) {
            Transaction t = fragmentHead;
            while (t != null && !DataFragment.isTail(t)) {
                t = t.getTrunk();
                if (!bundle.getTransactions().contains(t)) {
                    return false;
                }
            }
            return DataFragment.isTail(t);
        }

        private boolean isFragmentHead(Transaction transaction) {
            return ClassFragment.isHead(transaction) || DataFragment.isHead(transaction);
        }

        private Transaction processClassFragment(Transaction fragmentHead) {
            assert ClassFragment.isHead(fragmentHead);
            ClassFragment classFragment = new ClassFragment(fragmentHead);
            persistence.persist(classFragment);
            return classFragment.getTailTransaction();
        }

        private Transaction processDataFragment(Transaction fragmentHead) {
            ClassFragment classFragment = persistence.search(fragmentHead.address());
            if(classFragment!=null) {
                DataFragment dataFragment = new DataFragment(fragmentHead, classFragment);
                persistence.persist(dataFragment);
                notifyListeners(dataFragment);
            }
            return null;
        }
    }


    private class Persistence {
        private Map<String, ClassFragment> classFragments = new HashMap<>();
        private Map<String, Set<DataFragment>> referencing = new HashMap<>();

        public void persist(ClassFragment classFragment){
            classFragments.put(classFragment.getClassHash(), classFragment);
        }

        public void persist(DataFragment dataFragment){
            for(int i=0;i<dataFragment.getClassFragment().getRefCount();i++){
                String referenced = dataFragment.getReference(i);
                if(!referenced.equals(Trytes.NULL_HASH)){
                    Set<DataFragment> set = referencing.get(referenced);
                    if(set==null){
                        set = new HashSet<>();
                        referencing.put(referenced,set);
                    }
                    set.add(dataFragment);
                }
            }
        }
        public ClassFragment search(String classHash){
            ClassFragment fragment = classFragments.get(classHash);
            return fragment;
        }
    }

}
