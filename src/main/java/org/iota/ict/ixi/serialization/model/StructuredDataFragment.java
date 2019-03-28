package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.util.InputValidator;
import org.iota.ict.model.BundleFragmentBuilder;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class StructuredDataFragment extends BundleFragment {

    private final MetadataFragment metadataFragment;

    public StructuredDataFragment(Transaction headTransaction, MetadataFragment metadataFragment) {
        super(headTransaction);
        this.metadataFragment = metadataFragment;
    }

    public boolean hasTailFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[5]==1;
    }

    public boolean hasHeadFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[6]==1;
    }

    public String getValue(int i) {
        FieldDescriptor descriptor = metadataFragment.getDescriptor(i);
        if(descriptor==null) {
            return null;
        }
        long offset = metadataFragment.getOffsetForValueAtIndex(i).longValue();
        Transaction t = getHeadTransaction();
        while(offset> Constants.TRANSACTION_SIZE_TRYTES){
            t = t.getTrunk();
            offset -= Constants.TRANSACTION_SIZE_TRYTES;
        }
        return t.signatureFragments().substring((int)offset,(int)offset+descriptor.getSize().intValue());
    }

    public static class Builder extends BundleFragmentBuilder {

        private Map<Integer, String> values = new HashMap<>();

        private MetadataFragment metadata;

        public void setMetadata(MetadataFragment metadata) {
            this.metadata = metadata;
        }

        public void setValue(int index, String value){
            if(value==null){
                values.remove(index);
            }else{
                values.put(index,value);
            }
        }

        public StructuredDataFragment build(){
            if(metadata ==null){
                throw new IllegalStateException("MetadataFragment cannot be null");
            }

            buildTransactions();

            setTags();

            setMetadataHash();

            Transaction headTransaction = buildBundleFragment();

            return new StructuredDataFragment(headTransaction, metadata);
        }

        private void buildTransactions(){
            TransactionBuilder builder = new TransactionBuilder();
            builder.signatureFragments = "";
            for(int i = 0; i< metadata.getKeyCount(); i++){
                FieldDescriptor descriptor = metadata.getDescriptor(i);
                String value = values.get(i);
                String trytes = InputValidator.fit(value, descriptor.getTryteSize());
                builder.signatureFragments += trytes;
                while(builder.signatureFragments.length() > Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength){
                    String remainder = builder.signatureFragments.substring(Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    append(builder);
                    builder = new TransactionBuilder();
                    builder.signatureFragments = remainder;
                }
                builder.signatureFragments = Trytes.padRight(builder.signatureFragments, Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                append(builder);
            }
        }

        private void setTags(){
            if(getTransactionCount()==1){
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[] { 0, 0, 0, 1, 1 ,0}), Transaction.Field.TAG.tryteLength);
            }else{
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[] { 0, 0, 0, 1, 0 ,0}), Transaction.Field.TAG.tryteLength);
                getHead().tag = Trytes.padRight(Trytes.fromTrits(new byte[] { 0, 0, 0, 0, 1 ,0}), Transaction.Field.TAG.tryteLength);
            }
        }

        private void setMetadataHash(){
            getHead().extraDataDigest = metadata.getHeadTransaction().hash;
        }
    }
}
