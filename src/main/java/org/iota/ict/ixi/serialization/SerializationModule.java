package org.iota.ict.ixi.serialization;

import org.iota.ict.eee.Environment;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.ixi.serialization.model.StructuredDataFragment;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipPreprocessor;
import org.iota.ict.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class SerializationModule extends IxiModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationModule.class);

    public SerializationModule(Ixi ixi) {
        super(ixi);
    }

    private Map<String, MetadataFragment> metadatas = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<DataFragmentFilter, DataFragmentListener> listeners = new HashMap<>();

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

    public MetadataFragment buildMetadataFragment(MetadataFragment.Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        return builder.build();
    }

    public StructuredDataFragment buildStructuredDataFragment(StructuredDataFragment.Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        return builder.build();
    }

    /**
     * @return the MetadataFragment with head transaction identified by transactionHash,
     * or null if the transaction is not the head of a valid MetadataFragment.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     */
    public MetadataFragment loadMetadata(String transactionHash) {
        if (!Utils.isValidHash(transactionHash)) {
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid transaction hash");
        }
        Transaction tx = ixi.findTransactionByHash(transactionHash);
        if(tx==null){
            return null;
        }
        if(!MetadataFragment.isHead(tx)){
            return null;
        }
        return new MetadataFragment.Builder().fromTransaction(tx);
    }

    /**
     * @return the StructuredDataFragment with head transaction identified by transactionHash,
     * or null if the transaction is not the head of a StructuredDataFragment.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     * @throws UnknownMetadataException when referenced metadata is unknown or invalid
     */
    public StructuredDataFragment loadFragmentData(String transactionHash) throws UnknownMetadataException {
        if (!Utils.isValidHash(transactionHash)) {
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid transaction hash");
        }
        Transaction tx = ixi.findTransactionByHash(transactionHash);
        if(tx==null){
            return null;
        }
        if(!StructuredDataFragment.isHead(tx)){
            return null;
        }
        MetadataFragment metadata = metadatas.get(tx.extraDataDigest());
        if(metadata==null){
            throw new UnknownMetadataException();
        }
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment(tx, metadata);
        return structuredDataFragment;
    }

    /**
     * Pull structured data from the tangle
     * @param transactionHash structuredDtaFragment head transaction
     * @param clazz the serializable class (i.e. class annotated with @SerializableField) corresponding to the target transactionHash
     * @return the deserialized data or null when transaction with transactionHash is not found or is not data-fragment-head transaction.
     */
    public <T> T loadFragmentData(String transactionHash, Class<T> clazz) {
        if (!Utils.isValidHash(transactionHash)) {
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid transaction hash");
        }
        Transaction tx = ixi.findTransactionByHash(transactionHash);
        if(tx==null){
            return null;
        }
        if(!StructuredDataFragment.isHead(tx)){
            return null;
        }
        MetadataFragment metadata = MetadataFragment.Builder.fromClass(clazz).build();
        registerMetadata(metadata);
        StructuredDataFragment structuredDataFragment = new StructuredDataFragment(tx, metadata);
        try {
            return structuredDataFragment.deserializeToClass(clazz);
        } catch (UnknownMetadataException e) {
            throw new RuntimeException("Should not append", e);
        }
    }

    /**
     * @return the value in trytes of key at index
     * @throws IndexOutOfBoundsException when index is invalid
     * @throws UnknownMetadataException  when referenced metadata is unknown or invalid
     */
    public byte[] getTritsForKeyAtIndex(StructuredDataFragment dataFragment, int index) throws UnknownMetadataException {
        return dataFragment.getValue(index);
    }

    void registerMetadata(MetadataFragment metadataFragment) {
        metadatas.put(metadataFragment.hash(), metadataFragment);
    }

    MetadataFragment getMetadataFragment(String hash) {
        return metadatas.get(hash);
    }

    /**
     * Register a dataListener to be notified when a StructuredDataFragment of a particular class clazz is received.
     * @param listener : the listener to callback
     */
    public <T> void registerDataListener(Class<T> clazz, final DataFragmentListener<T> listener) {
        MetadataFragment metadataFragment = MetadataFragment.Builder.fromClass(clazz).build();
        registerMetadata(metadataFragment);
        final String classHash = metadataFragment.hash();
        DataFragmentFilter matcher = dataFragment ->
                dataFragment.getClassHash().equals(classHash);

        DataFragmentListener<StructuredDataFragment> wrappedListener = dataFragment -> {
            T data = null;
            try {
                data = dataFragment.deserializeToClass(clazz);
            } catch (UnknownMetadataException e) {
                return;
            }
            if (data != null) {
                listener.onData(data);
            }
        };

        registerDataListener(matcher, wrappedListener);
    }


    /**
     * Register a dataListener to be notified when a StructuredDataFragment is received.
     * @param matcher : a filter for fragment of interest
     * @param listener : the listener to callback
     */
    public void registerDataListener(DataFragmentFilter matcher, DataFragmentListener<StructuredDataFragment> listener) {
        listeners.put(matcher, listener);
    }

    void notifyListeners(StructuredDataFragment structuredDataFragment) {
        for (DataFragmentFilter dataFragmentFilter : listeners.keySet()) {
            if (dataFragmentFilter.match(structuredDataFragment)) {
                listeners.get(dataFragmentFilter).onData(structuredDataFragment);
            }
        }
    }

    public void publish(Object data){
        publish(data,null,null);
    }

    public void publish(Object data, String referencedTrunk, String referencedBranch){
        StructuredDataFragment dataFragment = new StructuredDataFragment.Builder()
                .fromInstance(data)
                .setReferencedTrunk(referencedTrunk)
                .setReferencedBranch(referencedBranch)
                .build();
        Stack<Transaction> stack = new Stack<>();
        Transaction t = dataFragment.getHeadTransaction();
        stack.push(t);
        while(t.getTrunk()!=null) {
            t = t.getTrunk();
            stack.push(t);
        }
        while(!stack.isEmpty()) {
            ixi.submit(stack.pop());
        }
    }

    /**
     * Build a StructuredDataFragment.Prepared for data.
     * The StructuredDataFragment.Prepared can be used later to insert the dataFragment in Bundle.
     * @param data data to serialize
     * @return a preparedDataFragment.
     */
    public StructuredDataFragment.Prepared prepare(Object data) {
        return  new StructuredDataFragment.Builder()
                    .fromInstance(data)
                    .prepare();
    }

    /**
     * Build a MetadataFragment.Prepared for clazz.
     * The MetadataFragment.Prepared can be used later to insert the metadataFragment in a Bundle.
     * @param clazz : properly annotated class
     * @return a prepared MetadataFragment.
     */
    public MetadataFragment.Prepared prepareMetadata(Class clazz) {
        return  MetadataFragment.Builder
                .fromClass(clazz)
                .prepare();
    }

    //for testing only
    public void forgetAllMetadata() {
        metadatas.clear();
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
                if(MetadataFragment.isHead(t)){
                    if(foundMetadataFragmentTail(bundle, t)) {
                        fragmentTail = processMetadataFragment(t);
                    }
                }else{
                    if(foundStructuredDataFragmentTail(bundle, t)) {
                        fragmentTail = processStructuredDataFragment(t);
                    }
                }
            }

            //process the remainder of the bundle
            if(fragmentTail!=null && !fragmentTail.isBundleTail && fragmentTail.getTrunk()!=null){
                processBundle(bundle, fragmentTail.getTrunk());
            }
        }

        private boolean foundMetadataFragmentTail(Bundle bundle, Transaction fragmentHead){
            Transaction t = fragmentHead;
            while(t!=null && !MetadataFragment.isTail(t)){
                t = t.getTrunk();
                if(!bundle.getTransactions().contains(t)){
                    return false;
                }
            }
            return MetadataFragment.isTail(t);
        }

        private boolean foundStructuredDataFragmentTail(Bundle bundle, Transaction fragmentHead){
            Transaction t = fragmentHead;
            while(t!=null && !StructuredDataFragment.isTail(t)){
                t = t.getTrunk();
                if(!bundle.getTransactions().contains(t)){
                    return false;
                }
            }
            return t!=null && StructuredDataFragment.isTail(t);
        }

        private boolean isFragmentHead(Transaction transaction){
            return MetadataFragment.isHead(transaction) || StructuredDataFragment.isHead(transaction);
        }

        private Transaction processMetadataFragment(Transaction fragmentHead){
            assert MetadataFragment.isHead(fragmentHead);
            String message = fragmentHead.signatureFragments();
            Transaction t = fragmentHead;
            if(!message.startsWith(MetadataFragment.METADATA_LANGUAGE_VERSION)){
                //unknown metadata version
                while(t!=null && !MetadataFragment.isTail(t)) t=t.getTrunk();
                return t;
            }
            MetadataFragment metadataFragment = new MetadataFragment.Builder().fromTransaction(t);
            if(metadataFragment != null){
                registerMetadata(metadataFragment);
                return metadataFragment.getTailTransaction();
            }
            return t;
        }

        private Transaction processStructuredDataFragment(Transaction fragmentHead){
            String classHash = fragmentHead.extraDataDigest();
            MetadataFragment metadataFragment = getMetadataFragment(classHash);
            if(metadataFragment!=null){
                StructuredDataFragment structuredDataFragment = new StructuredDataFragment(fragmentHead, metadataFragment);
                notifyListeners(structuredDataFragment);
            }
            return null;
        }
    }

}
