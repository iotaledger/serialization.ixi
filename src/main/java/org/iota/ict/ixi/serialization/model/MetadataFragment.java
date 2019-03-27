package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.model.BundleFragmentBuilder;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.util.ArrayList;
import java.util.List;

public class MetadataFragment extends BundleFragment {

    public static final String METADATA_LANGUAGE_VERSION = "A99999999999999999999999999";

    public MetadataFragment(Transaction headTransaction){
        super(headTransaction);
    }

    public boolean hasTailFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[3]==1;
    }

    public boolean hasHeadFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[4]==1;
    }

    public static class Builder extends BundleFragmentBuilder {

        private List<FieldDescriptor> fields = new ArrayList<>();

        public MetadataFragment build(){
            if(fields.size()==0){
                throw new IllegalStateException("Cannot build metadata fragment with no fields");
            }
            buildTransactions();

            setTags();

            Transaction lastTransaction = buildBundleFragment();

            return new MetadataFragment(lastTransaction);
        }

        public void appendField(FieldDescriptor descriptor){
            fields.add(descriptor);
        }

        private void buildTransactions() {
            TransactionBuilder transactionBuilder = prepareFreshTransactionBuilder();
            for(FieldDescriptor descriptor:fields){
                if(currentTransactionMessageIsFull(transactionBuilder)){
                    transactionBuilder.signatureFragments = Trytes.padRight(transactionBuilder.signatureFragments, Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    addFirst(transactionBuilder);
                    transactionBuilder = prepareFreshTransactionBuilder();
                }
                transactionBuilder.signatureFragments += descriptor.toTrytes();
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

        private static boolean currentTransactionMessageIsFull(TransactionBuilder transactionBuilder){
            return transactionBuilder.signatureFragments.length() > Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength - FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH;
        }

        private static TransactionBuilder prepareFreshTransactionBuilder(){
            TransactionBuilder transactionBuilder = new TransactionBuilder();
            transactionBuilder.signatureFragments = METADATA_LANGUAGE_VERSION;
            return  transactionBuilder;
        }
    }

}
