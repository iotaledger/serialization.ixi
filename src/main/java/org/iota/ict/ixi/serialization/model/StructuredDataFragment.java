package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructuredDataFragment extends BundleFragment {

    private static final BigInteger MESSAGE_SIZE = BigInteger.valueOf(Transaction.Field.SIGNATURE_FRAGMENTS.tritLength);

    private static final int LIST_SIZE_FIELD_TRIT_SIZE = 12;

    private final MetadataFragment metadataFragment;

    private List<Long> offsets;
    private long totalSize;

    public StructuredDataFragment(Transaction headTransaction, MetadataFragment metadataFragment) {
        super(headTransaction);
        this.metadataFragment = metadataFragment;
        offsets = new ArrayList<>(metadataFragment.getKeyCount());
        long currentOffset = 0;
        Transaction t = headTransaction;
        int transactionIndex = 0;
        for(int i=0;i<metadataFragment.getKeyCount();i++){
            offsets.add(currentOffset);
            if(currentOffset>((transactionIndex+1)*Transaction.Field.SIGNATURE_FRAGMENTS.tritLength)){
                t = t.getTrunk();
                transactionIndex++;
            }
            FieldDescriptor descriptor = metadataFragment.getDescriptor(i);
            if(descriptor.isSingleValue()){
                long elementLength = descriptor.getTritSize().longValue();
                currentOffset += elementLength;
            }else{
                long listLength = readListSizeAtOffset(t, (int) currentOffset % Transaction.Field.SIGNATURE_FRAGMENTS.tritLength);
                currentOffset += LIST_SIZE_FIELD_TRIT_SIZE;
                currentOffset += listLength * descriptor.getTritSize().longValue();
            }
        }
        totalSize = currentOffset;
    }

    private long readListSizeAtOffset(Transaction t, int offsetInTransaction){
        byte[] listLengthInTrits = Utils.readNtritsFromBundleFragment(LIST_SIZE_FIELD_TRIT_SIZE, t, offsetInTransaction);

        return Utils.integerFromTrits(listLengthInTrits).longValue();
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
        long offset = offsets.get(i).longValue();
        int length = (int)  (i+1 == metadataFragment.getKeyCount() ? (totalSize - offset) : offsets.get(i+1) - offset);
        Transaction t = getHeadTransaction();
        while(offset> Transaction.Field.SIGNATURE_FRAGMENTS.tritLength){
            t = t.getTrunk();
            offset = offset - Transaction.Field.SIGNATURE_FRAGMENTS.tritLength;
        }

        byte[] ret  = Utils.readNtritsFromBundleFragment(length, t, (int) offset);
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

    public List<byte[]> getListValue(int i){
        byte[] value = getValue(i);
        FieldDescriptor descriptor = metadataFragment.getDescriptor(i);
        int elementSize = descriptor.getTritSize().intValue();
        byte[] listLengthTrits = new byte[LIST_SIZE_FIELD_TRIT_SIZE];


        System.arraycopy(value,0,listLengthTrits,0,LIST_SIZE_FIELD_TRIT_SIZE);
        int listSize = Utils.integerFromTrits(listLengthTrits).intValue();

        List<byte[]> ret = new ArrayList<>(listSize);

        for(int j=0;j<listSize;j++){
            byte[] v = new byte[elementSize];
            System.arraycopy(value,(LIST_SIZE_FIELD_TRIT_SIZE+(j*elementSize)),v,0,elementSize);
            ret.add(v);
        }
        return ret;
    }

    public static class Builder extends BundleFragment.Builder {

        private Map<Integer, ValueHolder> values = new HashMap<>();

        private MetadataFragment metadata;

        List<Long> offsets;

        private BigInteger totalSize;

        public void setMetadata(MetadataFragment metadata) {
            this.metadata = metadata;
            offsets = new ArrayList<>(metadata.getKeyCount());
        }

        public void setValue(int index, String trytes){
            if(trytes==null){
                values.remove(index);
            }else{
                values.put(index,new SingleValueHolder(Trytes.toTrits(trytes)));
            }
        }

        public void setBooleanValue(int index, boolean b){
            values.put(index,new SingleValueHolder(b?new byte[]{1}:new byte[]{0}));
        }

        public void setTritsValue(int index, byte[] trits){
            if(trits==null){
                values.remove(index);
            }else{
                values.put(index,new SingleValueHolder(trits));
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
            computeOffsets();
            int transactionRequired = 1 + (totalSize.divide(MESSAGE_SIZE).intValue());
            byte[] message = new byte[transactionRequired * MESSAGE_SIZE.intValue()];
            for(Integer keyIndex:values.keySet()) {
                ValueHolder valueHolder = values.get(keyIndex);
                if (valueHolder instanceof SingleValueHolder) {
                    byte[] value = ((SingleValueHolder)valueHolder).value;
                    System.arraycopy(value, 0, message, offsets.get(keyIndex).intValue(), value.length);
                }else if (valueHolder instanceof MultipleValueHolder) {
                    int elementCount = ((MultipleValueHolder) valueHolder).values.size();
                    FieldDescriptor descriptor = metadata.getDescriptor(keyIndex);
                    int elementSize = descriptor.getTritSize().intValue();
                    byte[] listLengthInTrits = Trytes.toTrits(Trytes.fromNumber(BigInteger.valueOf(((MultipleValueHolder)valueHolder).values.size()),LIST_SIZE_FIELD_TRIT_SIZE/3));
                    System.arraycopy(listLengthInTrits, 0, message, offsets.get(keyIndex).intValue(), LIST_SIZE_FIELD_TRIT_SIZE);
                    int index = offsets.get(keyIndex).intValue() + LIST_SIZE_FIELD_TRIT_SIZE;
                    for(int j=0;j<elementCount;j++){
                        System.arraycopy(((MultipleValueHolder) valueHolder).values.get(j),0,message,index,elementSize);
                        index += elementSize;
                    }
                }else{
                    throw new IllegalStateException("Invalid value");
                }
            }
            for(int i=0;i<transactionRequired;i++){
                TransactionBuilder builder = new TransactionBuilder();
                byte[] msg = new byte[Transaction.Field.SIGNATURE_FRAGMENTS.tritLength];
                System.arraycopy(message,i*MESSAGE_SIZE.intValue(),msg,0, msg.length);
                builder.signatureFragments = Trytes.fromTrits(msg);
                addFirst(builder);
            }
        }

        private List<Long> computeOffsets(){
            long currentOffset = 0;
            for(int i=0;i<metadata.getKeyCount();i++){
                offsets.add(currentOffset);
                FieldDescriptor descriptor = metadata.getDescriptor(i);
                if(descriptor.isSingleValue()){
                    currentOffset += descriptor.getTritSize().intValue();
                }else{
                    currentOffset += LIST_SIZE_FIELD_TRIT_SIZE;
                    MultipleValueHolder valueHolder = (MultipleValueHolder) values.get(i);
                    if(valueHolder!=null){
                        long elementSize = descriptor.getTritSize().longValue();
                        int elementCount = valueHolder.values.size();
                        currentOffset += (elementCount * elementSize);
                    }
                }
            }
            totalSize = BigInteger.valueOf(currentOffset);
            return offsets;
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

        public void setValues(int i, String... trytes) {
            MultipleValueHolder holder = new MultipleValueHolder();
            int elementSize = metadata.getDescriptor(i).getTritSize().intValue();
            for(String val:trytes){
                byte[] b = new byte[elementSize];
                byte[] src = Trytes.toTrits(val);
                System.arraycopy(src,0, b, 0, src.length);
                holder.values.add(b);
            }
            values.put(i,holder);
        }

        private class ValueHolder{}
        private class SingleValueHolder extends ValueHolder{
            byte[] value;

            public SingleValueHolder(byte[] value) {
                this.value = value;
            }
        }
        private class MultipleValueHolder extends ValueHolder{
            List<byte[]> values = new ArrayList<>();
        }
    }
}
