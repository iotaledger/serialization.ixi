package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
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


    private MetadataFragment(Transaction headTransaction, int keyCount, Map<Integer, BigInteger> valuesOffsets){
        super(headTransaction);
        this.keyCount = keyCount;
        this.valuesOffsets = valuesOffsets;
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
        while(i>FIELD_DESCRIPTOR_PER_TRANSACTION){
            i = i - FIELD_DESCRIPTOR_PER_TRANSACTION;
            t = t.getTrunk();
        }
        int startIndex = METADATA_LANGUAGE_VERSION.length() + ( i * FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH );
        String fieldDescriptorTrytes = t.signatureFragments().substring(startIndex, startIndex+FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH);
        return FieldDescriptor.fromTrytes(fieldDescriptorTrytes);
    }

    public BigInteger getOffsetForValueAtIndex(int i) {
        return valuesOffsets.get(i);
    }



    public static class Builder extends BundleFragmentBuilder {

        private List<FieldDescriptor> fields = new ArrayList<>();

        private int keyCount = 0;

        private Map<Integer, BigInteger> valuesOffsets = new HashMap<>();

        public MetadataFragment build(){
            if(fields.size()==0){
                throw new IllegalStateException("Cannot build metadata fragment with no fields");
            }

            buildTransactions();

            setTags();

            Transaction lastTransaction = buildBundleFragment();

            return new MetadataFragment(lastTransaction, keyCount, valuesOffsets);
        }

        public void appendField(FieldDescriptor descriptor){
            fields.add(descriptor);
        }

        private void buildTransactions() {
            TransactionBuilder transactionBuilder = prepareFreshTransactionBuilder("");
            BigInteger currentOffsetForValue = BigInteger.ZERO;
            for(FieldDescriptor descriptor:fields){
                transactionBuilder.signatureFragments += descriptor.toTrytes();
                if(transactionBuilder.signatureFragments.length() > Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength){
                    String remaining = transactionBuilder.signatureFragments.substring(Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    transactionBuilder.signatureFragments = transactionBuilder.signatureFragments.substring(0,Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    addFirst(transactionBuilder);
                    transactionBuilder = prepareFreshTransactionBuilder(remaining);
                }

                valuesOffsets.put(keyCount, currentOffsetForValue);
                currentOffsetForValue = currentOffsetForValue.add(descriptor.getSize());
                keyCount++;
            }
            transactionBuilder.signatureFragments = Trytes.padRight(transactionBuilder.signatureFragments, Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
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
