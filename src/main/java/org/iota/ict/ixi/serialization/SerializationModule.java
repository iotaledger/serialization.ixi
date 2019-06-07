package org.iota.ict.ixi.serialization;

import org.iota.ict.eee.EffectListenerQueue;
import org.iota.ict.eee.Environment;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.serialization.model.BundleFragment;
import org.iota.ict.ixi.serialization.model.ClassFragment;
import org.iota.ict.ixi.serialization.model.DataFragment;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class SerializationModule extends IxiModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationModule.class);

    //visible for testing
    final Persistence persistence = new Persistence();
    private final Map<DataFragment.Filter, String> listeners = new HashMap<>();

    public SerializationModule(Ixi ixi) {
        super(ixi);
    }

    @Override
    public void run() {
        EEEFunctions.init(this, ixi);

        //register an effect listener observing Bundles
        EffectListenerQueue<GossipEvent> bundleListener = new EffectListenerQueue(Constants.Environments.GOSSIP);
        ixi.addListener(bundleListener);
        GossipEventHandler gossipEventHandler = new GossipEventHandler();
        try{
            while(isRunning()){
                GossipEvent gossipEvent = bundleListener.takeEffect();
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
        persistence.terminate();
        runningThread.interrupt();
        LOGGER.info("Serialization.ixi terminated.");
    }

    /**
     * Build a ClassFragment based on builder.
     * @param builder a ClassFragment.Builder
     * @return the ClassFragment
     * @see SerializationModule#publishBundleFragment(BundleFragment.Builder)
     * @see SerializationModule#prepareBundleFragment(BundleFragment.Builder)
     */
    public ClassFragment buildClassFragment(ClassFragment.Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        return builder.build();
    }


    /**
     * Build a DataFragment based on builder.
     * @param builder a DataFragment.Builder
     * @return the DataFragment
     * @see SerializationModule#publishBundleFragment(BundleFragment.Builder)
     * @see SerializationModule#prepareBundleFragment(BundleFragment.Builder)
     */
    public DataFragment buildDataFragment(DataFragment.Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        return builder.build();
    }

    /**
     * @return the ClassFragment with head transaction identified by transactionHash,
     * or null if the transaction is not the head of a valid ClassFragment or cannot be found.
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


    /**
     * @param classHash the classHash to load
     * @return The ClassFragment for the given classHash or null when the fragment cannot be found
     */
    public ClassFragment loadClassFragmentForClassHash(String classHash) {
        if (classHash == null || classHash.equals(Trytes.NULL_HASH)) {
            throw new IllegalArgumentException("searched classHash cannot be null");
        }
        return persistence.search(classHash);
    }

    /**
     * @return the DataFragment with head transaction identified by transactionHash,
     * or null if the transaction is not the head of a DataFragment or cannot be found.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes) or the NULL_HASH
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
            return new DataFragment(tx, classFragment);
        }
        return null;
    }


    /**
     * @return the DataFragment referenced from dataFragment at index index,
     * or null if referenced DataFragment cannot be found.
     */
    public DataFragment loadReferencedDataFragment(DataFragment dataFragment, int index) {
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
     * @param referencedTransactionHash the transaction hash of the dataFragment to be referenced (by any reference)
     * @param filter a filter to refine the result set (can be null)
     * @return all DataFragment referencing *referencedTransactionHash* from any reference
     * @see DataFragment.Filter
     */
    @SuppressWarnings("unchecked")
    public Set<DataFragment> findDataFragmentReferencing(String referencedTransactionHash, DataFragment.Filter filter) {
        if (referencedTransactionHash == null || referencedTransactionHash.equals(Trytes.NULL_HASH)) {
            throw new IllegalArgumentException("referencedTransactionHash cannot be null");
        }
        Set<DataFragment> ret = persistence.referencing.get(referencedTransactionHash);
        if(ret==null || ret.size()==0) return Collections.EMPTY_SET;
        if(filter==null){
            return ret;
        }
        HashSet<DataFragment> filtered = new HashSet<>();
        for(DataFragment f:ret){
            if(filter.match(f)){
                filtered.add(f);
            }
        }
        return filtered;
    }

    /**
     * @param referencedTransactionHash the transaction hash of the dataFragment to be referenced (by reference at index 'referenceIndex')
     * @param referenceIndex of the reference to inspect
     * @param filter a filter to refine the result set (can be null)
     * @return all DataFragment referencing *referencedTransactionHash* from any reference
     * @see DataFragment.Filter
     */
    @SuppressWarnings("unchecked")
    public Set<DataFragment> findDataFragmentReferencingAtIndex(int referenceIndex, String referencedTransactionHash, DataFragment.Filter filter) {
        if (referencedTransactionHash == null || referencedTransactionHash.equals(Trytes.NULL_HASH)) {
            throw new IllegalArgumentException("referencedTransactionHash cannot be null");
        }
        Set<DataFragment> ret = persistence.referencing.get(referencedTransactionHash);
        if(ret==null || ret.size()==0) return Collections.EMPTY_SET;
        if(filter==null){
            filter = dataFragment -> dataFragment.getReference(referenceIndex).equals(referencedTransactionHash);
        }else{
            filter = DataFragment.Filter.and(filter, dataFragment -> dataFragment.getReference(referenceIndex).equals(referencedTransactionHash));
        }
        HashSet<DataFragment> filtered = new HashSet<>();
        for(DataFragment f:ret){
            if(filter.match(f)){
                filtered.add(f);
            }
        }
        return filtered;
    }


    /**
     * @param referencedClassHash the classHash to be referenced (by any reference)
     * @param filter a filter to refine the result set (can be null)
     * @return all ClassFragment referencing *referencedClassHash* from any reference
     * @see ClassFragment.Filter
     */
    @SuppressWarnings("unchecked")
    public Set<ClassFragment> findClassFragmentReferencing(String referencedClassHash, ClassFragment.Filter filter) {
        return findClassFragmentReferencingAtIndex(-1,referencedClassHash, filter);
    }

    /**
     * @param referencedClassHash the classHash to be referenced (by reference at index 'referenceIndex')
     * @param referenceIndex of the reference to inspect
     * @param filter a filter to refine the result set (can be null)
     * @return all ClassFragment referencing *referencedClasshash* from any reference
     * @see ClassFragment.Filter
     */
    @SuppressWarnings("unchecked")
    public Set<ClassFragment> findClassFragmentReferencingAtIndex(int referenceIndex, String referencedClassHash, ClassFragment.Filter filter) {
        if (referencedClassHash == null || referencedClassHash.equals(Trytes.NULL_HASH)) {
            throw new IllegalArgumentException("referencedClassHash cannot be null");
        }
        HashSet<ClassFragment> filtered = new HashSet<>();
        for(ClassFragment classFragment: persistence.classFragments.values()){
            if(classFragment.isReferencing(referenceIndex,referencedClassHash)){
                if(filter!=null){
                    if(filter.match(classFragment)){
                        filtered.add(classFragment);
                    }
                }else{
                    filtered.add(classFragment);
                }
            }
        }
        return filtered;
    }

    /**
     * @param dataFragment : fragment to inspect
     * @param index : index of an attribute
     * @return the tryte string representing the value of this attribute or the empty string when the value is not available.
     * @throws ArrayIndexOutOfBoundsException when attributeIndex is not in range
     */
    public String getAttributeTrytes(DataFragment dataFragment, int index) {
        if (dataFragment == null) {
            throw new IllegalArgumentException("dataFragment cannot be null");
        }
        return dataFragment.getAttributeAsTryte(index);
    }

    /**
     * @param dataFragmentTransactionHash : the hash of the fragment to inspect
     * @param index : index of an attribute
     * @return the tryte string representing the value of this attribute or the empty string when the value is not available.
     * @throws ArrayIndexOutOfBoundsException when attributeIndex is not in range
     */
    public String getAttributeTrytes(String dataFragmentTransactionHash, int index) {
        DataFragment dataFragment = loadDataFragment(dataFragmentTransactionHash);
        if (dataFragment == null) {
            return "";
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


    /**
     * Publish the fragment build with fragmentBuilder. The fragment head transaction will also be the BundleHead transaction.
     * Depending on the fragmentBuilder.referencedTrunk transaction, the fragment-tail will be flagged as bundle tail or not.
     * @param fragmentBuilder can be a DataFragment.Builder or a ClassFragment.Builder
     * @return the published bundle fragment
     */
    public <F extends BundleFragment,T extends BundleFragment.Builder<F>> F publishBundleFragment(T fragmentBuilder) {
        fragmentBuilder.setHeadFragment(true);
        return submitFragment(fragmentBuilder);
    }


    /**
     * Prepare the fragment build with fragmentBuilder. The fragment head transaction will not be BundleHead transaction.
     * Depending on the fragmentBuilder.referencedTrunk transaction, the fragment-tail will be flagged as bundle tail or not.
     * @param fragmentBuilder can be a DataFragment.Builder or a ClassFragment.Builder
     * @return the published bundle fragment
     */
    public <F extends BundleFragment,T extends BundleFragment.Builder<F>> F prepareBundleFragment(T fragmentBuilder) {
        return submitFragment(fragmentBuilder);
    }

    private <F extends BundleFragment,T extends BundleFragment.Builder<F>> F submitFragment(T fragmentBuilder) {
        if(fragmentBuilder.getReferencedTrunk()==null || fragmentBuilder.getReferencedTrunk().equals(Trytes.NULL_HASH) || Utils.isBundleHead(fragmentBuilder.getReferencedTrunk())){
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
                return dataFragment.getTailTransaction();
            }
            return null;
        }
    }


    class Persistence {
        private final Map<String, ClassFragment> classFragments = new HashMap<>();
        private final Map<String, Set<DataFragment>> referencing = new HashMap<>();

        int delay = Constants.RUN_MODUS == Constants.RunModus.MAIN ? 60 : 3;

        public Persistence() {
            execService.schedule(task, delay, TimeUnit.SECONDS);
        }

        public void persist(ClassFragment classFragment){
            classFragments.put(classFragment.getClassHash(), classFragment);
        }

        public void terminate(){
            referencing.clear();
            classFragments.clear();
            execService.shutdownNow();
        }

        public void persist(DataFragment dataFragment){
            for(int i=0;i<dataFragment.getClassFragment().getRefCount();i++){
                String referenced = dataFragment.getReference(i);
                if(!referenced.equals(Trytes.NULL_HASH)){
                    Set<DataFragment> set = referencing.computeIfAbsent(referenced, k -> new HashSet<>());
                    set.add(dataFragment);
                }
            }
        }
        public ClassFragment search(String classHash){
            return classFragments.get(classHash);
        }

        final ScheduledExecutorService execService =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread newThread = Executors.defaultThreadFactory().newThread(r);
                    newThread.setName("PersistenceCleaner");
                    return newThread;
                });

        final Callable<Void> task = new Callable<Void>() {
            public Void call() {
                try {
                    List<String> droppedTransactions = new ArrayList<>();
                    for(String txHash:referencing.keySet()){
                        if(ixi.findTransactionByHash(txHash)==null){
                            droppedTransactions.add(txHash);
                        }
                    }
                    for(String txHash:droppedTransactions){
                        referencing.remove(txHash);
                    }
                    if(droppedTransactions.size()>0){
                        LOGGER.info("Dropped "+droppedTransactions.size()+" referenced transactions.");
                        delay = Math.max(delay/2, 60);
                    }else{
                        delay = Math.min(delay*2, 600);
                        LOGGER.debug("Dropped 0 referenced transactions. Next run in "+delay+" seconds");
                    }
                } catch (Throwable t){
                    LOGGER.warn("Exception when running Persistence cleaner");
                } finally {
                    if(!Thread.currentThread().isInterrupted())
                        execService.schedule(this, delay, TimeUnit.SECONDS);
                }
                return null;
            }
        };

    }

}
