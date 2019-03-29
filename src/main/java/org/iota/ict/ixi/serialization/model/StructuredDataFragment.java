package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.BundleFragmentBuilder;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class StructuredDataFragment extends BundleFragment {

    private static final BigInteger MESSAGE_SIZE = BigInteger.valueOf(Transaction.Field.SIGNATURE_FRAGMENTS.tritLength);

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

    public byte[] getValue(int i) {
        FieldDescriptor descriptor = metadataFragment.getDescriptor(i);
        if(descriptor==null) {
            return null;
        }
        long offset = metadataFragment.getOffsetForValueAtIndex(i).longValue();
        Transaction t = getHeadTransaction();
        while(offset> Constants.TRANSACTION_SIZE_TRITS){
            t = t.getTrunk();
            offset -= Constants.TRANSACTION_SIZE_TRITS;
        }
        byte[] trits = Trytes.toTrits(t.signatureFragments());
        byte[] ret = new byte[(int)descriptor.getTritSize().longValue()];
        System.arraycopy(trits,(int)offset,ret,0,ret.length);
        return ret;
    }

    public boolean getBooleanValue(int i){
        byte[] value = getValue(i);
        return value==null?null:value[0]==1;
    }

    public BigInteger getIntegerValue(int i){
        byte[] value = getValue(i);
        return value==null?null:Utils.integerFromTrits(value);
    }

    public String getAsciiValue(int i){
        byte[] value = getValue(i);
        return value==null?"":Utils.asciiFromTrits(value);
    }

    public static class Builder extends BundleFragmentBuilder {

        private Map<Integer, byte[]> values = new HashMap<>();

        private MetadataFragment metadata;

        public void setMetadata(MetadataFragment metadata) {
            this.metadata = metadata;
        }

        public void setValue(int index, String trytes){
            if(trytes==null){
                values.remove(index);
            }else{
                values.put(index,Trytes.toTrits(trytes));
            }
        }

        public void setBooleanValue(int index, boolean b){
            values.put(index,b?new byte[]{1}:new byte[]{0});
        }

        public void setTritsValue(int index, byte[] trits){
            if(trits==null){
                values.remove(index);
            }else{
                values.put(index,trits);
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
            int transactionRequired = 1 + (metadata.getTritLength().divide(MESSAGE_SIZE).intValue());
            byte[] message = new byte[transactionRequired * MESSAGE_SIZE.intValue()];
            for(Integer keyIndex:values.keySet()){
                byte[] value = values.get(keyIndex);
                System.arraycopy(value,0,message,metadata.getOffsetForValueAtIndex(keyIndex).intValue(), value.length);
            }
            for(int i=0;i<transactionRequired;i++){
                TransactionBuilder builder = new TransactionBuilder();
                byte[] msg = new byte[Transaction.Field.SIGNATURE_FRAGMENTS.tritLength];
                System.arraycopy(message,i*MESSAGE_SIZE.intValue(),msg,0, msg.length);
                builder.signatureFragments = Trytes.fromTrits(msg);
                addFirst(builder);
            }
//            TransactionBuilder builder = new TransactionBuilder();
//            byte[] msg = new byte[Transaction.Field.SIGNATURE_FRAGMENTS.tritLength];
//            for(int i = 0; i< metadata.getKeyCount(); i++){
//                FieldDescriptor descriptor = metadata.getDescriptor(i);
//                byte[] value = values.get(i);
//                if(value==null){
//                    value = new byte[descriptor.getTritSize().intValue()];
//                };
//                System.arraycopy(value);
//                while(builder.signatureFragments.length() > Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength){
//                    String remainder = builder.signatureFragments.substring(Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
//                    append(builder);
//                    builder = new TransactionBuilder();
//                    builder.signatureFragments = remainder;
//                }
//                append(builder);
//            }
//            Utils.padRightSignature(builder);
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
