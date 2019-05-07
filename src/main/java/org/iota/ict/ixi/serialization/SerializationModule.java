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

    /**
     * Input  : <data size decimal>;[<classHash1>;<classHash2>;...]
     * Output : <ClassHash trytes>
     */
    private final EEEFunction computeClassHash = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "computeClassHash"));

    /**
     * Input  : <classHash>;<data trytes>;<trunk_hash>;<branch_hash>;[<ref0_transaction_hash>;<ref1_transaction_hash>;...]
     * Output : <fragment_head_hash>
     */
    private final EEEFunction publishDataFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "publishDataFragment"));

    /**
     * Input  : <data size decimal>;<trunk_hash>;<branch_hash>;[<classHash1>;<classHash2>;...]
     * Output : <fragment_head_hash>
     */
    private final EEEFunction publishClassFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "publishClassFragment"));

    /**
     * Input  : <classHash>;<data trytes>;<trunk_hash>;<branch_hash>;[<ref0_transaction_hash>;<ref1_transaction_hash>;...]
     * Output : <fragment_head_hash>
     */
    private final EEEFunction prepareDataFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "prepareDataFragment"));

    /**
     * Input  : <data size decimal>;<trunk_hash>;<branch_hash>;[<classHash1>;<classHash2>;...]
     * Output : <fragment_head_hash>
     */
    private final EEEFunction prepareClassFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "prepareClassFragment"));

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

        //EEE
        ixi.addListener(computeClassHash);
        ixi.addListener(publishDataFragment);
        ixi.addListener(publishClassFragment);
        ixi.addListener(prepareDataFragment);
        ixi.addListener(prepareClassFragment);
        ixi.addListener(getData);
        ixi.addListener(getReferencedData);
        ixi.addListener(findFragmentsForClass);
        ixi.addListener(findReferencing);
    }

    private Map<DataFragment.Filter, String> listeners = new HashMap<>();

    @Override
    public void run() {
        new EEERequestHandler(computeClassHash, this::processComputeClassHashRequest).start();
        new EEERequestHandler(publishDataFragment, this::processPublishDataRequest).start();
        new EEERequestHandler(publishClassFragment, this::processPublishClassRequest).start();
        new EEERequestHandler(prepareDataFragment, this::processPrepareDataRequest).start();
        new EEERequestHandler(prepareClassFragment, this::processPrepareClassRequest).start();
        new EEERequestHandler(getData, this::processGetDataRequest).start();
        new EEERequestHandler(getReferencedData, this::processGetReferencedDataRequest).start();
        new EEERequestHandler(getReference, this::processGetReferenceRequest).start();
        new EEERequestHandler(findFragmentsForClass, this::processFindFragmentsForClassRequest).start();
        new EEERequestHandler(findReferencing, this::processFindReferencingRequest).start();

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
        DataFragment dataFragment = new DataFragment(tx);
        return dataFragment;
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

    private void processPublishDataRequest(EEEFunction.Request request) {
        DataFragment.Builder builder = dataFragmentBuilderFromRequest(request);
        DataFragment fragment = publishBundleFragment(builder);
        request.submitReturn(ixi, fragment.getHeadTransaction().hash);
    }

    private void processPublishClassRequest(EEEFunction.Request request) {
        ClassFragment.Builder builder = classFragmentBuilderFromRequest(request);
        ClassFragment fragment = publishBundleFragment(builder);
        request.submitReturn(ixi, fragment.getHeadTransaction().hash);
    }

    private ClassFragment.Builder classFragmentBuilderFromRequest(EEEFunction.Request request) {
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
        return builder;
    }

    private void processPrepareDataRequest(EEEFunction.Request request) {
        DataFragment.Builder builder = dataFragmentBuilderFromRequest(request);
        DataFragment fragment = prepareBundleFragment(builder);
        request.submitReturn(ixi, fragment.getHeadTransaction().hash);
    }

    private DataFragment.Builder dataFragmentBuilderFromRequest(EEEFunction.Request request) {
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
        return builder;
    }

    private void processPrepareClassRequest(EEEFunction.Request request) {
        ClassFragment.Builder builder = classFragmentBuilderFromRequest(request);
        ClassFragment fragment = prepareBundleFragment(builder);
        request.submitReturn(ixi, fragment.getHeadTransaction().hash);
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
            DataFragment dataFragment = new DataFragment(fragmentHead);
            notifyListeners(dataFragment);
            return null;
        }
    }


    private class Persistence {
        private Map<String, ClassFragment> classFragments = new HashMap<>();

        public void persist(ClassFragment classFragment){
            classFragments.put(classFragment.getClassHash(), classFragment);
        }

        public ClassFragment search(String classHash){
            ClassFragment fragment = classFragments.get(classHash);
            return fragment;
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
