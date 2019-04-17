package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.model.md.FieldType;
import org.iota.ict.ixi.serialization.util.SerializableField;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataFragment extends BundleFragment {

    public static final String METADATA_LANGUAGE_VERSION = "A99";

    private int keyCount;

    public MetadataFragment(Transaction headTransaction, int keyCount){
        super(headTransaction);
        this.keyCount = keyCount;
    }

    public boolean hasTailFlag(Transaction transaction){
        return isTail(transaction);
    }

    public boolean hasHeadFlag(Transaction transaction){
        return isHead(transaction);
    }

    public static boolean isTail(Transaction transaction){
        return transaction!=null && Trytes.toTrits(transaction.tag())[3]==1;
    }

    public static boolean isHead(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[4]==1;
    }

    public int getKeyCount() {
        return keyCount;
    }

    public FieldDescriptor getDescriptor(int i) {
        if(!( i < keyCount)){
            throw new IndexOutOfBoundsException(i+" is an invalid key index");
        }
        Transaction t = getHeadTransaction();
        long offset = offsetInTrytes(i);//descriptorsOffsets.get(i);
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

    private long offsetInTrytes(int index){
        return METADATA_LANGUAGE_VERSION.length() + index * FieldDescriptor.FIELD_DESCRIPTOR_TRYTE_LENGTH;
    }

    public static class Prepared {

        private Builder builder;

        Prepared(Builder builder){
            this.builder = builder;
        }

        public List<TransactionBuilder> fromTailToHead(){
            return builder.getTailToHead();
        }
    }

    public static class Builder extends BundleFragment.Builder<MetadataFragment> {

        private List<FieldDescriptor> fields = new ArrayList<>();

        private int keyCount = 0;

        public MetadataFragment build(){
            if(fields.size()==0){
                throw new IllegalStateException("Cannot build metadata fragment with no fields");
            }

            prepareTransactionBuilders();
            setTags();

            setBundleBoundaries();

            Transaction lastTransaction = buildBundleFragment();

            return new MetadataFragment(lastTransaction, keyCount);
        }


        public MetadataFragment.Prepared prepare() {
            if(fields.size()==0){
                throw new IllegalStateException("Cannot build metadata fragment with no fields");
            }

            prepareTransactionBuilders();
            setTags();
            return new Prepared(this);
        }

        public static Builder fromClass(Class clazz){
            Map<Integer,FieldDescriptor> fieldDescriptors = new HashMap<>();
            int fieldCount = 0;
            Field[] fields = clazz.getFields();
            for(Field field:fields){
                if(field.getAnnotation(SerializableField.class)!=null){
                    SerializableField annotation = field.getAnnotation(SerializableField.class);
                    FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes(annotation.fieldType()),annotation.tritLength(), annotation.label());
                    fieldDescriptors.put(annotation.index(),descriptor);
                    fieldCount++;
                }
            }
            if(fieldCount==0){
                throw new RuntimeException("class '"+clazz.getName()+"' is not a valid Serializable class. It don't contains serializable field.");
            }
            Builder builder = new Builder();
            for(int i=0;i<fieldCount;i++){
                FieldDescriptor descriptor = fieldDescriptors.get(i);
                if(descriptor==null){
                    throw new RuntimeException("class '"+clazz.getName()+"' is not a valid Serializable class. Field at index "+i+" is missing");
                }
                builder.appendField(descriptor);
            }
            return builder;
        }

        public MetadataFragment fromTransaction(Transaction fragmentHead){
            int keyCount = 0;
            int startIndex = MetadataFragment.METADATA_LANGUAGE_VERSION.length();
            Transaction t = fragmentHead;
            String message = fragmentHead.signatureFragments();
            if(!message.startsWith(MetadataFragment.METADATA_LANGUAGE_VERSION)){
                //unknown metadata version
                return null;
            }
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
                    assert MetadataFragment.isTail(t);
                    return new MetadataFragment(fragmentHead, keyCount);
                }else{
                    keyCount++;
                }
            }
        }

        public Builder appendField(FieldDescriptor descriptor){
            fields.add(descriptor);
            return this;
        }

        private void prepareTransactionBuilders() {
            TransactionBuilder transactionBuilder = prepareFreshTransactionBuilder(METADATA_LANGUAGE_VERSION);
            for(FieldDescriptor descriptor:fields){
                transactionBuilder.signatureFragments += descriptor.toTrytes();
                if(transactionBuilder.signatureFragments.length() > Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength){
                    String remaining = transactionBuilder.signatureFragments.substring(Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    transactionBuilder.signatureFragments = transactionBuilder.signatureFragments.substring(0,Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    addFirst(transactionBuilder);
                    transactionBuilder = prepareFreshTransactionBuilder(remaining);
                }
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
            transactionBuilder.signatureFragments = remaining;
            return  transactionBuilder;
        }

    }

}
