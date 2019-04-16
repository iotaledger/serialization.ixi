package org.iota.ict.ixi.serialization;

import org.iota.ict.eee.Environment;
import org.iota.ict.eee.dispatch.EffectDispatcher;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.ixi.serialization.model.PreparedDataFragment;
import org.iota.ict.ixi.serialization.model.StructuredDataFragment;
import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipPreprocessor;
import org.iota.ict.utils.Constants;

import java.util.*;

public class SerializationModule extends IxiModule {

    public static SerializationModule INSTANCE;

    public SerializationModule(Ixi ixi) {
        super(ixi);
        INSTANCE = this;
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
    }

    public Transaction findTransaction(String hash){
        System.out.println("searching "+hash);
        return ixi.findTransactionByHash(hash);
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
     * @return the MetadataFragment including transaction with transactionHash,
     * or null if the transaction is not part of a valid MetadataFragment.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     */
    public MetadataFragment loadMetadata(String transactionHash) {
        if (!Utils.isValidHash(transactionHash)) {
            throw new IllegalArgumentException("'" + transactionHash + "' is not a valid transaction hash");
        }
        //TODO
        return null;
    }

    /**
     * @return the StructuredDataFragment with head transaction identified by transactionHash,
     * or null if the transaction is not the head of a StructuredDataFragment.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     * @throws UnknownMetadataException when referenced metadata is unknown or invalid
     */
    public StructuredDataFragment loadStructuredData(String transactionHash) throws UnknownMetadataException {
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
     * @return the value in trytes of key at index
     * @throws IndexOutOfBoundsException when index is invalid
     * @throws UnknownMetadataException  when referenced metadata is unknown or invalid
     */
    public byte[] getTritsForKeyAtIndex(StructuredDataFragment dataFragment, int index) throws UnknownMetadataException {
        return dataFragment.getValue(index);
    }

    /**
     * @return the value of key at index
     * @throws IndexOutOfBoundsException when index is invalid
     * @throws UnknownMetadataException  when referenced metadata is unknown or invalid
     */
    public Object getValueAtIndex(StructuredDataFragment dataFragment, int index) {
        //TODO
        return null;
    }

    void registerMetadata(MetadataFragment metadataFragment) {
        metadatas.put(metadataFragment.hash(), metadataFragment);
    }

    MetadataFragment getMetadataFragment(String hash) {
        return metadatas.get(hash);
    }

    public <T> void registerDataListener(Class<T> clazz, final DataFragmentListener<T> listener) {
        MetadataFragment metadataFragment = MetadataFragment.Builder.fromClass(clazz).build();
        registerMetadata(metadataFragment);
        final String classHash = metadataFragment.hash();
        DataFragmentFilter matcher = new DataFragmentFilter() {
            @Override
            public boolean match(StructuredDataFragment dataFragment) {
                return dataFragment.getClassHash().equals(classHash);
            }
        };

        DataFragmentListener<StructuredDataFragment> wrappedListener = new DataFragmentListener<StructuredDataFragment>() {
            @Override
            public void onData(StructuredDataFragment dataFragment) {
                T data = null;
                try {
                    data = dataFragment.deserializeToClass(clazz);
                } catch (UnknownMetadataException e) {
                    return;
                }
                if (data != null) {
                    listener.onData(data);
                }
            }
        };

        registerDataListener(matcher, wrappedListener);
    }




    public void registerDataListener(DataFragmentFilter matcher, DataFragmentListener<StructuredDataFragment> listener) {
        listeners.put(matcher, listener);
    }

    public void notifyListeners(StructuredDataFragment structuredDataFragment) {
        for (DataFragmentFilter dataFragmentFilter : listeners.keySet()) {
            if (dataFragmentFilter.match(structuredDataFragment)) {
                listeners.get(dataFragmentFilter).onData(structuredDataFragment);
            }
        }
    }

    public StructuredDataFragment buildDataFragment(Object data){
        return new StructuredDataFragment.Builder().fromInstance(data).build();
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

    public PreparedDataFragment prepare(Object data) {
        return  new StructuredDataFragment.Builder()
                    .fromInstance(data)
                    .prepare();
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
                    fragmentTail = processMetadataFragment(t);
                }else{
                    fragmentTail = processStructuredDataFragment(t);
                }
            }

            //process the remainder of the bundle
            if(fragmentTail!=null && !fragmentTail.isBundleTail && fragmentTail.getTrunk()!=null){
                processBundle(bundle, fragmentTail.getTrunk());
            }
        }

        private boolean isFragmentHead(Transaction transaction){
            return MetadataFragment.isHead(transaction) || StructuredDataFragment.isHead(transaction);
        }

        private Transaction processMetadataFragment(Transaction fragmentHead){
            assert MetadataFragment.isHead(fragmentHead);
            String message = fragmentHead.signatureFragments();
            if(!message.startsWith(MetadataFragment.METADATA_LANGUAGE_VERSION)){
                //unknown metadata version
                Transaction t = fragmentHead;
                while(!MetadataFragment.isTail(t)) t=t.getTrunk();
                return t;
            }
            //let's count the keys
            int keyCount = 0;
            int startIndex = 3;
            Transaction t = fragmentHead;
            while(true){
                String descriptor;
                if(startIndex+ FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH<Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength) {
                    descriptor = message.substring(startIndex, startIndex + FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH);

                    startIndex += FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH;
                }else{
                    descriptor = message.substring(startIndex, Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    int remainingLength = FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH - descriptor.length();
                    t = t.getTrunk();
                    message = t.signatureFragments();
                    startIndex = remainingLength;
                    descriptor += message.substring(0, remainingLength);
                }

                if (descriptor.equals("99999999999999999999999999999999999999999999999999999")) {
                    break;
                }else{
                    keyCount++;
                }
                assert MetadataFragment.isTail(t);
                MetadataFragment metadataFragment = new MetadataFragment(fragmentHead, keyCount);
                registerMetadata(metadataFragment);
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
