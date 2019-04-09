package org.iota.ict.ixi.serialization;

import org.iota.ict.eee.EffectListener;
import org.iota.ict.eee.Environment;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.ixi.serialization.model.Predicate;
import org.iota.ict.ixi.serialization.model.StructuredDataFragment;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.ixi.serialization.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class SerializationModule extends IxiModule {

    public SerializationModule(Ixi ixi) {
        super(ixi);
    }

    private Map<String, MetadataFragment> metadatas = new HashMap<>();
    private Map<Predicate, EffectListener<StructuredDataFragment>> listeners = new HashMap<>();

    @Override
    public void run() {
        //TODO
    }

    @Override
    public void onStart() {
        super.onStart();
        ixi.addListener(new BundleListener(this));
    }

    public MetadataFragment buildMetadataFragment(MetadataFragment.Builder builder){
        if(builder==null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        return builder.build();
    }

    public StructuredDataFragment buildStructuredDataFragment(StructuredDataFragment.Builder builder){
        if(builder==null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        return builder.build();
    }

    /**
     *  @return the MetadataFragment including transaction with transactionHash,
     *           or null if the transaction is not part of a valid MetadataFragment.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     */
    public MetadataFragment loadMetadata(String transactionHash){
        if(!Utils.isValidHash(transactionHash)){
            throw new IllegalArgumentException("'"+transactionHash+"' is not a valid transaction hash");
        }
        //TODO
        return null;
    }

    /**
     *  @return the StructuredDataFragment including transaction with transactionHash,
     *          or null if the transaction is not part of a valid StructuredDataFragment.
     *  @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     *  @throws UnknownMetadataException when referenced metadata is unknown or invalid
     */
    public StructuredDataFragment loadStructuredData(String transactionHash) throws UnknownMetadataException {
        if(!Utils.isValidHash(transactionHash)){
            throw new IllegalArgumentException("'"+transactionHash+"' is not a valid transaction hash");
        }
        //TODO
        return null;
    }

    /**
     * @return the value in trytes of key at index
     * @throws IndexOutOfBoundsException when index is invalid
     * @throws UnknownMetadataException when referenced metadata is unknown or invalid
     */
    public byte[] getTritsForKeyAtIndex(StructuredDataFragment dataFragment, int index) throws UnknownMetadataException {
        return dataFragment.getValue(index);
    }

    /**
     * @return the value of key at index
     * @throws IndexOutOfBoundsException when index is invalid
     * @throws UnknownMetadataException when referenced metadata is unknown or invalid
     */
    public Object getValueAtIndex(StructuredDataFragment dataFragment, int index){
        //TODO
        return null;
    }

    void registerMetadata(MetadataFragment metadataFragment){
        metadatas.put(metadataFragment.hash(), metadataFragment);
    }

    MetadataFragment getMetadataFrament(String hash){
        return metadatas.get(hash);
    }

    public void registerDataEffectListener(Predicate predicate, EffectListener<StructuredDataFragment> listener) {
        //ixi.addListener(listener);
        listeners.put(predicate, listener);
    }

    public void notifyListeners(StructuredDataFragment structuredDataFragment) {
        for(Predicate predicate:listeners.keySet()){
            if(predicate.match(structuredDataFragment)){
                listeners.get(predicate).onReceive(structuredDataFragment);
            }
        }
    }
}
