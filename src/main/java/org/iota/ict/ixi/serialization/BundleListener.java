package org.iota.ict.ixi.serialization;

import org.iota.ict.eee.EffectListener;
import org.iota.ict.eee.Environment;
import org.iota.ict.eee.chain.ChainedEffectListener;
import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.ixi.serialization.model.StructuredDataFragment;
import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.model.bundle.Bundle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.utils.Constants;

/**
 * Receive Bundles and inspect them to find metadata fragments or structured data fragments.
 * When a metadataFragment is found: it is registered @SerializationModule.
 * When a structureData fragment is found and it's metadata fragment is available : the structured fragment is published.
 * //TODO : what means "published"
 */
public class BundleListener implements EffectListener<ChainedEffectListener.Output> {

    private final SerializationModule serializationModule;

    public BundleListener(SerializationModule serializationModule) {
        this.serializationModule = serializationModule;
    }

    @Override
    public void onReceive(ChainedEffectListener.Output effect) {
        Transaction tx = (Transaction) ((GossipEvent)effect.getEffect()).getTransaction();
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
            serializationModule.registerMetadata(metadataFragment);
        }
        return t;
    }

    private Transaction processStructuredDataFragment(Transaction fragmentHead){
        String classHash = fragmentHead.extraDataDigest();
        MetadataFragment metadataFragment = serializationModule.getMetadataFrament(classHash);
        if(metadataFragment!=null){
            StructuredDataFragment structuredDataFragment = new StructuredDataFragment(fragmentHead, metadataFragment);
            serializationModule.notifyListeners(structuredDataFragment);
        }
        return null;
    }
}
