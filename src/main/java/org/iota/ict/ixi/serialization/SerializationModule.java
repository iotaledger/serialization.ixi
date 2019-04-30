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
import org.iota.ict.model.transaction.TransactionBuilder;
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

    /**
     * Input  : <data size decimal>;[<classHash1>;<classHash2>;...]
     * Output : <ClassHash trytes>
     */
    private final EEEFunction computeClassHash = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "computeClassHash"));

    /**
     * Input  : <classHash>;<data trytes>;<trunk_hash>;<branch_hash>;[<ref0_transaction_hash>;<ref1_transaction_hash>;...]
     * Output : <fragment_tail_transaction_trytes>[;<body_transaction_trytes>*;<head_transaction_trytes>]
     */
    private final EEEFunction buildDataFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "buildDataFragment"));

    /**
     * Input  : <data size decimal>;<trunk_hash>;<branch_hash>;[<classHash1>;<classHash2>;...]
     * Output : <fragment_tail_transaction_trytes>[;<body_transaction_trytes>*;<head_transaction_trytes>]
     */
    private final EEEFunction buildClassFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "buildClassFragment"));

    /**
     * Input  : <data_fragment_head_transaction_hash>
     * Output : <data as trytes>
     */
    private final EEEFunction getData = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "getData"));

    /**
     * Input  : <data_fragment_head_transaction_hash>;<index_of_the_reference>
     * Output : <data of the referenced fragment (as trytes)>
     */
    private final EEEFunction getReferencedData = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "getReferencedData"));

    /**
     * Input  : <data_fragment_head_transaction_hash>;<index_of_the_reference>
     * Output : <transactionHash referenced at index>
     */
    private final EEEFunction getReference = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "getReference"));

    /**
     * Input  : <classHash>
     * Output : <transaction hash of dataFragment>*
     */
    private final EEEFunction findFragmentsForClass = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "findFragmentsForClass"));

    /**
     * Input  : <classHash>;<referenced transaction hash>;<index of searched reference>
     * Output : <transaction hash of dataFragment>*
     */
    private final EEEFunction findReferencing = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "findReferencing"));


    public SerializationModule(Ixi ixi) {
        super(ixi);

        ixi.addListener(new BundleListener());

        //EEE
        ixi.addListener(computeClassHash);
        ixi.addListener(buildDataFragment);
        ixi.addListener(buildClassFragment);
        ixi.addListener(getData);
        ixi.addListener(getReferencedData);
        ixi.addListener(findFragmentsForClass);
        ixi.addListener(findReferencing);
    }

    private Map<DataFragment.Filter, DataFragment.Listener> listeners = new HashMap<>();

    @Override
    public void run() {
        new EEERequestHandler(computeClassHash, request -> processComputeClassHashRequest(request)).start();
        new EEERequestHandler(buildDataFragment, request -> processBuildDataRequest(request)).start();
        new EEERequestHandler(buildClassFragment, request -> processBuildClassRequest(request)).start();
        new EEERequestHandler(getData, request -> processGetDataRequest(request)).start();
        new EEERequestHandler(getReferencedData, request -> processGetReferencedDataRequest(request)).start();
        new EEERequestHandler(getReference, request -> processGetReferenceRequest(request)).start();
        new EEERequestHandler(findFragmentsForClass, request -> processFindFragmentsForClassRequest(request)).start();
        new EEERequestHandler(findReferencing, request -> processFindReferencingRequest(request)).start();
    }

    @Override
    public void onStart() {
        super.onStart();
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
        //TODO optimize me
        Set<Transaction> transactions = ixi.findTransactionsByAddress(ClassFragment.METACLASS_HASH);
        for (Transaction tx : transactions) {
            try {
                ClassFragment classFragment = new ClassFragment(tx);
                if (classFragment.getClassHash().equals(classHash)) return classFragment;
            } catch (IllegalArgumentException e) {
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
        if (Trytes.NULL_HASH.equals(transactionHash)) {
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid data fragment transaction hash");
        }
        Transaction tx = ixi.findTransactionByHash(transactionHash);
        if (tx == null) {
            return null;
        }
        DataFragment dataFragment = new DataFragment(tx);
        return dataFragment;
    }

    /**
     * @param classHash
     * @return all DataFragment for a given classHash
     */
    public Set<DataFragment> findDataFragmentForClassHash(String classHash) {
        Set<Transaction> transactions = ixi.findTransactionsByAddress(classHash);
        Set<DataFragment> ret = new HashSet<>();
        for (Transaction t : transactions) {
            try {
                DataFragment fragment = new DataFragment(t);
                ret.add(fragment);
            } catch (IllegalArgumentException e) {
                //not a valid bundle fragment.
            }
        }
        return ret;
    }

    /**
     * @param classHash                 the classHash of the searched fragments
     * @param referencedTransactionHash the transaction hash of the dataFragment to be referenced
     * @param index                     index of the reference
     * @return all DataFragment referencing *referencedTransactionHash* from reference at index *index*
     */
    public Set<DataFragment> findDataFragmentReferencing(String classHash, String referencedTransactionHash, int index) {
        if (referencedTransactionHash == null || referencedTransactionHash.equals(Trytes.NULL_HASH)) {
            throw new IllegalArgumentException("referencedTransactionHash cannot be null");
        }
        if (classHash == null || classHash.equals(Trytes.NULL_HASH)) {
            throw new IllegalArgumentException("classHash hash cannot be null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be < 0");
        }
        //TODO : optimize me
        Set<Transaction> transactions = ixi.findTransactionsByAddress(classHash);
        Set<DataFragment> ret = new HashSet<>();
        for (Transaction t : transactions) {
            try {
                DataFragment fragment = new DataFragment(t);
                if (fragment.getReference(index).equals(referencedTransactionHash))
                    ret.add(fragment);
            } catch (IllegalArgumentException e) {
                //not a valid bundle fragment.
            }
        }
        return ret;
    }

    public DataFragment getFragmentAtIndex(DataFragment fragment, int index) {
        return loadDataFragment(fragment.getReference(index));
    }

    public byte[] getData(DataFragment dataFragment) {
        if (dataFragment == null) {
            throw new IllegalArgumentException("dataFragment cannot be null");
        }
        return dataFragment.getData();
    }

    public byte[] getData(String dataFragmentTransactionHash) {
        DataFragment dataFragment = loadDataFragment(dataFragmentTransactionHash);
        if (dataFragment == null) {
            return null;
        }
        return dataFragment.getData();
    }

    /**
     * @return the value in trytes of key at index
     * @throws IndexOutOfBoundsException when index is invalid
     */
    public byte[] getDataAtIndex(DataFragment dataFragment, int index) {
        if (dataFragment == null) {
            throw new IllegalArgumentException("dataFragment cannot be null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        DataFragment referenced = getFragmentAtIndex(dataFragment, index);
        if (referenced != null) {
            return referenced.getData();
        }
        return null;
    }

    //TODO : review listeners mechanics

    /**
     * Register a dataListener to be notified when a StructuredDataFragment of a particular class clazz is received.
     *
     * @param listener : the listener to callback
     */
    public void registerDataListener(String classHash, final DataFragment.Listener listener) {
        DataFragment.Filter matcher = dataFragment ->
                dataFragment.getClassHash().equals(classHash);
        registerDataListener(matcher, listener);
    }


    /**
     * Register a dataListener to be notified when a StructuredDataFragment is received.
     *
     * @param matcher  : a filter for fragment of interest
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


    public <T extends BundleFragment> T publishBundleFragment(T fragment) {
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
     * Build a StructuredDataFragment.Prepared for data.
     * The StructuredDataFragment.Prepared can be used later to insert the dataFragment in Bundle.
     *
     * @return a preparedDataFragment.
     */
    public DataFragment.Prepared prepare(DataFragment.Builder builder) {
        return builder.prepare();
    }

    /**
     * Build a MetadataFragment.Prepared from builder.
     * The MetadataFragment.Prepared can be used later to insert the metadataFragment in a Bundle.
     *
     * @return a prepared MetadataFragment.
     */
    public ClassFragment.Prepared prepareMetadata(ClassFragment.Builder builder) {
        return builder.prepare();
    }


    public DataFragment getDataFragment(DataFragment dataFragment, int index) {
        String headTransactionHash = dataFragment.getReference(index);
        if (headTransactionHash != null) {
            Transaction headTransaction = ixi.findTransactionByHash(headTransactionHash);
            if (headTransaction != null) {
                return new DataFragment(headTransaction);
            }
        }
        return null;
    }


    //EEE

    private void processComputeClassHashRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String dataSize = split[0];
        ClassFragment.Builder builder = new ClassFragment.Builder().withDataSize(Integer.valueOf(dataSize));
        if (split.length > 1) {
            String[] references = argument.substring(argument.indexOf(";") + 1).split(";");
            for (String s : references) {
                builder.addReferencedClasshash(s);
            }
        }
        String ret = builder.build().getClassHash();
        request.submitReturn(ixi, ret);
    }

    private void processBuildDataRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String classHash = split[0];
        DataFragment.Builder builder = new DataFragment.Builder(classHash);
        builder.setData(Trytes.toTrits(split[1]));
        builder.setReferencedTrunk(split[2]);
        builder.setReferencedBranch(split[3]);
        if (split.length > 4) {
            int i = 4;
            while (i < split.length) {
                builder.setReference(i - 4, split[i]);
                i++;
            }
        }
        DataFragment.Prepared prepared = builder.prepare();
        List<String> transactions = new ArrayList<>(prepared.fromTailToHead().size());
        for (TransactionBuilder txBuilder : prepared.fromTailToHead()) {
            transactions.add(txBuilder.build().decodeBytesToTrytes());
        }
        request.submitReturn(ixi, String.join(";", transactions));
    }

    private void processBuildClassRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String dataSize = split[0];
        ClassFragment.Builder builder = new ClassFragment.Builder().withDataSize(Integer.valueOf(dataSize));
        builder.setReferencedTrunk(split[1]);
        builder.setReferencedBranch(split[2]);
        if (split.length > 3) {
            int i = 3;
            while (i < split.length) {
                builder.addReferencedClasshash(split[i]);
                i++;
            }
        }
        ClassFragment.Prepared prepared = builder.prepare();
        List<String> transactions = new ArrayList<>(prepared.fromTailToHead().size());
        for (TransactionBuilder txBuilder : prepared.fromTailToHead()) {
            transactions.add(txBuilder.build().decodeBytesToTrytes());
        }
        request.submitReturn(ixi, String.join(";", transactions));
    }

    private void processGetDataRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String hash = split[0];
        DataFragment fragment = loadDataFragment(hash);
        if (fragment != null) {
            String ret = Trytes.fromTrits(fragment.getData());
            request.submitReturn(ixi, ret);
        } else {
            request.submitReturn(ixi, "");
        }
    }

    private void processGetReferencedDataRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String hash = split[0];
        int index = Integer.valueOf(split[1]);
        DataFragment fragment = loadDataFragment(hash);
        if (fragment != null) {
            String ret = Trytes.fromTrits(getDataAtIndex(fragment, index));
            request.submitReturn(ixi, ret);
        } else {
            request.submitReturn(ixi, "");
        }
    }

    private void processGetReferenceRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String hash = split[0];
        int index = Integer.valueOf(split[1]);
        DataFragment fragment = loadDataFragment(hash);
        if (fragment != null) {
            String ret = fragment.getReference(index);
            request.submitReturn(ixi, ret);
        } else {
            request.submitReturn(ixi, "");
        }
    }

    private void processFindFragmentsForClassRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String classHash = split[0];
        Set<DataFragment> fragments = findDataFragmentForClassHash(classHash);
        returnFragmentSet(request, fragments);
    }

    private void processFindReferencingRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String classHash = split[0];
        String referencedTransactionHash = split[1];
        int index = Integer.valueOf(split[2]);
        Set<DataFragment> fragments = findDataFragmentReferencing(classHash, referencedTransactionHash, index);
        returnFragmentSet(request, fragments);
    }

    private void returnFragmentSet(EEEFunction.Request request, Set<DataFragment> fragments) {
        if (fragments.size() == 0) {
            request.submitReturn(ixi, "");
        } else {
            ArrayList<String> ret = new ArrayList<>(fragments.size());
            for (DataFragment fragment : fragments) {
                ret.add(fragment.getHeadTransaction().hash);
            }
            request.submitReturn(ixi, String.join(";", ret));
        }
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
            if (tx.isBundleHead) {
                Bundle bundle = new Bundle(tx);
                if (bundle.isComplete() && bundle.isStructureValid()) {
                    processBundle(bundle, null);
                } else {
                    throw new RuntimeException("Received an incomplete or invalid bundle. This shouldn't append");
                }
            }
        }

        @Override
        public Environment getEnvironment() {
            return Constants.Environments.GOSSIP_PREPROCESSOR_CHAIN;
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
            return t != null && DataFragment.isTail(t);
        }

        private boolean isFragmentHead(Transaction transaction) {
            return ClassFragment.isHead(transaction) || DataFragment.isHead(transaction);
        }

        private Transaction processClassFragment(Transaction fragmentHead) {
            assert ClassFragment.isHead(fragmentHead);
            Transaction t = fragmentHead;
            ClassFragment classFragment = new ClassFragment(t);
            if (classFragment != null) {
                return classFragment.getTailTransaction();
            }
            return t;
        }

        private Transaction processDataFragment(Transaction fragmentHead) {
            String classHash = fragmentHead.address();
            ClassFragment classFragment = loadClassFragment(classHash);
            if (classFragment != null) {
                DataFragment dataFragment = new DataFragment(fragmentHead);
                notifyListeners(dataFragment);
            }
            return null;
        }
    }


    private class EEERequestHandler extends Thread {

        EEEHandler handler;
        EEEFunction eeeFunction;

        public EEERequestHandler(EEEFunction eeeFunction, EEEHandler handler) {
            this.handler = handler;
            this.eeeFunction = eeeFunction;
        }

        @Override
        public void run() {
            while (isRunning()) {
                try {
                    handler.handleRequest(eeeFunction.requestQueue.take());
                } catch (InterruptedException e) {
                    if (isRunning()) throw new RuntimeException(e);
                }
            }
        }
    }

    interface EEEHandler {
        void handleRequest(EEEFunction.Request request);
    }
}
