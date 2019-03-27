package org.iota.ict.ixi.serialization;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.serialization.model.MetadataFragment;
import org.iota.ict.ixi.serialization.model.MetadataFragmentBuilder;
import org.iota.ict.ixi.serialization.model.StructuredDataFragment;
import org.iota.ict.ixi.serialization.model.StructuredDataFragmentBuilder;
import org.iota.ict.ixi.serialization.util.InputValidator;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;

public class SerializationModule extends IxiModule {

    public SerializationModule(Ixi ixi) {
        super(ixi);
    }

    @Override
    public void run() {
        //TODO
    }

    public MetadataFragment buildMetadataFragment(MetadataFragmentBuilder builder){
        if(builder==null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        return builder.build();
    }

    public StructuredDataFragment buildStructuredDataFragment(StructuredDataFragmentBuilder builder){
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
        if(!InputValidator.isValidHash(transactionHash)){
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
        if(!InputValidator.isValidHash(transactionHash)){
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
    public String getTrytesForKeyAtIndex(StructuredDataFragment dataFragment, int index){
        //TODO
        return null;
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

}
