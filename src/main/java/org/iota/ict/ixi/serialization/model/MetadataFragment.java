package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.BundleFragmentBuilder;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataFragment extends BundleFragment {

    public static final String METADATA_LANGUAGE_VERSION = "A99";

    public static final int FIELD_DESCRIPTOR_PER_TRANSACTION = ( Transaction.Field.SIGNATURE_FRAGMENTS.tritLength - METADATA_LANGUAGE_VERSION.length() ) / FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH ;

    private int keyCount;
    private Map<Integer, BigInteger> valuesOffsets;
    private List<Long> descriptorsOffsets = new ArrayList<>();
    private final BigInteger tritLength;

    private MetadataFragment(Transaction headTransaction, int keyCount, Map<Integer, BigInteger> valuesOffsets, List<Long> descriptorsOffsets){
        super(headTransaction);
        this.keyCount = keyCount;
        this.valuesOffsets = valuesOffsets;
        this.descriptorsOffsets = descriptorsOffsets;
        tritLength = valuesOffsets.get(keyCount-1).add(getDescriptor(keyCount-1).getTritSize());
    }

    public boolean hasTailFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[3]==1;
    }

    public boolean hasHeadFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[4]==1;
    }

    public int getKeyCount() {
        return keyCount;
    }

    public FieldDescriptor getDescriptor(int i) {
        assert i < keyCount;
        Transaction t = getHeadTransaction();
        long offset = descriptorsOffsets.get(i);
        while(offset>Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength){
            offset = offset - Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength;
            t = t.getTrunk();
        }
        int startIndex = (int) offset;
        String fieldDescriptorTrytes;
        if (startIndex+FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH < Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength) {
            fieldDescriptorTrytes = t.signatureFragments().substring(startIndex, startIndex+FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH);
        }else {
            //FieldDescriptor is encoded on 2 transactions
            fieldDescriptorTrytes = t.signatureFragments().substring(startIndex);
            t = t.getTrunk();
            fieldDescriptorTrytes = fieldDescriptorTrytes + t.signatureFragments().substring(0,FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH -  fieldDescriptorTrytes.length() );
        }
        return FieldDescriptor.fromTrytes(fieldDescriptorTrytes);
    }

    public BigInteger getOffsetForValueAtIndex(int i) {
        return valuesOffsets.get(i);
    }

    public BigInteger getTritLength() {
        return tritLength;
    }


    public static class Builder extends BundleFragmentBuilder {

        private List<FieldDescriptor> fields = new ArrayList<>();

        private int keyCount = 0;

        private Map<Integer, BigInteger> valuesOffsets = new HashMap<>();
        private List<Long> descriptorsOffsets = new ArrayList<>();

        public MetadataFragment build(){
            if(fields.size()==0){
                throw new IllegalStateException("Cannot build metadata fragment with no fields");
            }

            buildTransactions();

            setTags();

            Transaction lastTransaction = buildBundleFragment();

            return new MetadataFragment(lastTransaction, keyCount, valuesOffsets, descriptorsOffsets);
        }

        public void appendField(FieldDescriptor descriptor){
            fields.add(descriptor);
        }

        private void buildTransactions() {
            TransactionBuilder transactionBuilder = prepareFreshTransactionBuilder("");
            BigInteger currentOffsetForValue = BigInteger.ZERO;
            int transactionCount = 0;
            for(FieldDescriptor descriptor:fields){
                descriptorsOffsets.add((long)transactionBuilder.signatureFragments.length() + (transactionCount * Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength));
                transactionBuilder.signatureFragments += descriptor.toTrytes();
                if(transactionBuilder.signatureFragments.length() > Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength){
                    String remaining = transactionBuilder.signatureFragments.substring(Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    transactionBuilder.signatureFragments = transactionBuilder.signatureFragments.substring(0,Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    addFirst(transactionBuilder);
                    transactionCount ++;
                    transactionBuilder = prepareFreshTransactionBuilder(remaining);
                }

                valuesOffsets.put(keyCount, currentOffsetForValue);
                currentOffsetForValue = currentOffsetForValue.add(descriptor.getTritSize());
                keyCount++;
            }
            Utils.padRightSignature(transactionBuilder);
            addFirst(transactionBuilder);
        }

        private void setTags(){
            if(getTransactionCount()==1){
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[] { 0, 0, 0, 1, 1 ,0}), Transaction.Field.TAG.tryteLength);
            }else{
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[] { 0, 0, 0, 1, 0 ,0}), Transaction.Field.TAG.tryteLength);
                getHead().tag = Trytes.padRight(Trytes.fromTrits(new byte[] { 0, 0, 0, 0, 1 ,0}), Transaction.Field.TAG.tryteLength);
            }
        }

        private static TransactionBuilder prepareFreshTransactionBuilder(String remaining){
            TransactionBuilder transactionBuilder = new TransactionBuilder();
            transactionBuilder.signatureFragments = METADATA_LANGUAGE_VERSION + remaining;
            return  transactionBuilder;
        }
    }

}
