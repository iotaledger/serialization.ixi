package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.iota.ict.utils.Trytes.NULL_HASH;

@SuppressWarnings("WeakerAccess")
public class DataFragment extends BundleFragment {

    private static final int LENGTH_OF_SIZE_FIELD = ClassFragment.TRYTE_LENGTH_OF_SIZE_FIELD;

    private int dataSizeInTrits;
    private int refCount;
    private int attributeCount;
    private ClassFragment classFragment;

    public DataFragment(Transaction headTransaction, ClassFragment classFragment){
        super(headTransaction);
        this.classFragment = classFragment;
        dataSizeInTrits = classFragment.getDataSize();
        refCount = classFragment.getRefCount();
        attributeCount = classFragment.getAttributeCount();
    }

    public String getReference(int index){
        index++;
        Transaction tx = getHeadTransaction();
        while(index>1){
            tx = tx.getTrunk();
            if(tx==null) {
                return NULL_HASH;
            }
            index -=2;
        }
        if(index==0) return tx.address();
        return tx.extraDataDigest();
    }

    public boolean hasTailFlag(Transaction transaction){
        return isTail(transaction);
    }

    public boolean hasHeadFlag(Transaction transaction){
        return isHead(transaction);
    }

    public static boolean isTail(Transaction transaction){
        return transaction!=null && Trytes.toTrits(transaction.tag())[5]==1;
    }
    public static boolean isHead(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[6]==1;
    }

    public String getClassHash() {
        return getHeadTransaction().address();
    }

    public ClassFragment getClassFragment() {
        return classFragment;
    }

    public String getAttributeAsTryte(int attributeIndex) {
        int startIndex = classFragment.getTryteIndexForAttribute(attributeIndex);
        int length = classFragment.getTryteLengthForAttribute(attributeIndex);
        StringBuilder sb = new StringBuilder();
        Transaction tx = getHeadTransaction();
        while(startIndex>Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength){
            tx = tx.getTrunk();
            if(tx==null) return Trytes.padRight("",length);
            startIndex = startIndex-Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength;
        }
        if(startIndex+length<Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength){
            return tx.signatureFragments().substring(startIndex, startIndex+length);
        }
        sb.append(tx.signatureFragments().substring(startIndex));
        int remaining = length -sb.length();
        while(remaining>0) {
            tx = tx.getTrunk();
            if (tx == null) return Trytes.padRight(sb.toString(), length);
            if(remaining>Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength){
                remaining -= Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength;
                sb.append(tx.signatureFragments());
            }else{
                sb.append(tx.signatureFragments(), 0, remaining);
                remaining = 0;
            }
        }
        return sb.toString();
    }


    public static class Builder extends BundleFragment.Builder<DataFragment> {

        private Map<Integer, String> referenceHashes = new HashMap<>();
        private Map<Integer,String> data = new HashMap<>();
        private String classHash;
        private ClassFragment classFragment;

        public Builder(ClassFragment classFragment){
            this.classFragment = classFragment;
            this.classHash = classFragment.getClassHash();
        }

        @Override
        public DataFragment build() {
            prepareTransactionBuilders();
            setTags();
            setBundleBoundaries();

            Transaction lastTransaction = buildBundleFragment();

            return new DataFragment(lastTransaction, classFragment);
        }

        public void setReference(int index, String hash){
            if(hash==null){
                referenceHashes.remove(index);
            }else{
                referenceHashes.put(index, hash);
            }
        }

        public void setReference(int index, DataFragment data){
            if(data==null){
                setReference(index,(String)null);
            }else{
                setReference(index, data.getHeadTransaction().hash);
            }
        }

        private void setTags() {
            if (getTransactionCount() == 1) {
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[]{0, 0, 0, 0, 0, 1, 1, 0, 0}), Transaction.Field.TAG.tryteLength);
            } else {
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[]{0, 0, 0, 0, 0, 1, 0, 0, 0}), Transaction.Field.TAG.tryteLength);
                getHead().tag = Trytes.padRight(Trytes.fromTrits(new byte[]{0, 0, 0, 0, 0, 0, 1, 0, 0}), Transaction.Field.TAG.tryteLength);
            }
        }

        public Prepared prepare() {
            prepareTransactionBuilders();
            setTags();
            return new Prepared(this);
        }

        private void prepareTransactionBuilders() {
            int refCount = referenceHashes.size()==0?0:1+Collections.max(referenceHashes.keySet());
            int transactionsRequiredForData = data==null ? 1 : 1 + (classFragment.getDataSize() / Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
            int transactionsRequiredForReferences = 1 + (refCount)/2;
            int transactionsRequired = Math.max(transactionsRequiredForData, transactionsRequiredForReferences);

            int currentRefIndex = 0;

            TransactionBuilder builder = new TransactionBuilder();
            builder.address = classHash;

            StringBuilder dataTrytes = new StringBuilder();
            for(int i=0;i<classFragment.getAttributeCount();i++){
                String attributeValue = data.get(i);
                if(attributeValue==null){
                    attributeValue = "";
                }
                dataTrytes.append(Trytes.padRight(attributeValue,classFragment.getTryteLengthForAttribute(i)));
            }
            int trytesOffset = 0;
            for(int i=0;i<transactionsRequired;i++){
                if(i>0) {
                    String address = referenceHashes.get(currentRefIndex++);
                    if (address != null) {
                        builder.address = address;
                    }
                }
                String extra = referenceHashes.get(currentRefIndex++);
                if(extra!=null) {
                    builder.extraDataDigest = extra;
                }
                if(trytesOffset<dataTrytes.length()){
                    int tryteLengthStoredHere = Math.min((dataTrytes.length()-trytesOffset),Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
                    builder.signatureFragments = dataTrytes.substring(trytesOffset, trytesOffset+tryteLengthStoredHere);
                    trytesOffset += tryteLengthStoredHere;
                }
                addFirst(builder);
                builder = new TransactionBuilder();
            }
        }

        public Builder setAttribute(int i, byte[] data) {
            if(data==null){
                this.data.remove(i);
            }else {
                this.data.put(i, Trytes.fromTrits(data));
            }
            return this;
        }

        public Builder setAttribute(int i, String data) {
            if(data==null){
                this.data.remove(i);
            }else {
                this.data.put(i,data);
            }
            return this;
        }
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

    public interface Filter {
        boolean match(DataFragment dataFragment);

        public static Filter and(Filter f0, Filter f1){
            return new Filter() {
                @Override
                public boolean match(DataFragment dataFragment) {
                    return f0.match(dataFragment) && f1.match(dataFragment);
                }
            };
        }
    }

}
