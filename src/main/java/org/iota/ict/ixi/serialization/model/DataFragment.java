package org.iota.ict.ixi.serialization.model;

import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.*;

import static org.iota.ict.utils.Trytes.NULL_HASH;

@SuppressWarnings("WeakerAccess")
public class DataFragment extends BundleFragment {


    private final ClassFragment classFragment;

    //store the offset and length of each attribute
    private final int[][] offsetAndLength;

    public DataFragment(Transaction headTransaction, ClassFragment classFragment){
        super(headTransaction); //we keep a WeakReference to the transaction

        this.classFragment = classFragment;
        offsetAndLength = new int[classFragment.getAttributeCount()][2];
        int currentOffset = 0;
        for(int i=0;i<offsetAndLength.length;i++){
            int attributeLength = classFragment.getTryteLengthForAttribute(i);
            if(attributeLength==0){
                //variableSize attribute. Effective size is stored in the 6 first trits
                attributeLength = Trytes.toNumber(getSlice(currentOffset, 6)).intValue();
                currentOffset += 6;
            }
            offsetAndLength[i] = new int[]{currentOffset, attributeLength};
            currentOffset += attributeLength;
        }
    }

    public String getReference(int index){
        index++;  //first address field store the classhash so it is skipped
        Transaction tx = getHeadTransaction();
        if(tx==null) return NULL_HASH;
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
        if(getHeadTransaction()==null) return NULL_HASH;
        return getHeadTransaction().address();
    }

    public ClassFragment getClassFragment() {
        return classFragment;
    }

    /**
     * @param attributeIndex : index of an attribute
     * @return the tryte string representing the value of this attribute or the empty string when the value is not available.
     * @throws ArrayIndexOutOfBoundsException when attributeIndex is not in range
     */
    public String getAttributeAsTryte(int attributeIndex) {
        int startIndex = offsetAndLength[attributeIndex][0];
        int length = offsetAndLength[attributeIndex][1];
        return getSlice(startIndex, length);
    }

    /**
     * Extract a slice from the fragment message (i.e. concat of all message fields of fragment)
     * @param startIndex slice start index
     * @param length slice length
     * @return the tryte string starting at startIndex with specified length or the empty string
     * when the data is not available (because RingTangle may drop old transactions).
     */
    private String getSlice(int startIndex, int length) {
        StringBuilder sb = new StringBuilder();
        Transaction tx = getHeadTransaction();
        if(tx==null) return "";
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
            if (tx == null) return "";
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

        private final Map<Integer, String> referenceHashes = new HashMap<>();
        private final Map<Integer,String> data = new HashMap<>();
        private final String classHash;
        private final ClassFragment classFragment;

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

        public Builder setReference(int index, String hash){
            if(hash==null){
                referenceHashes.remove(index);
            }else{
                referenceHashes.put(index, hash);
            }
            return this;
        }

        public Builder setReference(int index, DataFragment data){
            if(data==null || data.getHeadTransaction()==null){
                setReference(index,(String)null);
            }else{
                setReference(index, data.getHeadTransaction().hash);
            }
            return this;
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
            int dataSize = classFragment.getDataSize();
            dataSize += variableSizeAttributes(classFragment.getVariableSizeAttributeIndexes());
            int transactionsRequiredForData = 1 + (dataSize / Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
            int transactionsRequiredForReferences = 1 + (refCount)/2;
            int transactionsRequired = Math.max(transactionsRequiredForData, transactionsRequiredForReferences);

            int currentRefIndex = 0;

            TransactionBuilder builder = new TransactionBuilder();
            builder.address = classHash;

            StringBuilder dataTrytes = new StringBuilder();
            for(int i=0;i<classFragment.getAttributeCount();i++){

                if(classFragment.getVariableSizeAttributeIndexes().contains(i)){
                    dataTrytes.append(Trytes.fromNumber(data.get(i)==null ? BigInteger.ZERO : BigInteger.valueOf(data.get(i).length()),6));
                }

                String attributeValue = data.get(i);
                if(attributeValue==null){
                    attributeValue = "";
                }
                if(classFragment.getTryteLengthForAttribute(i)>0 && attributeValue.length()<classFragment.getTryteLengthForAttribute(i)) {
                    attributeValue = Trytes.padRight(attributeValue, classFragment.getTryteLengthForAttribute(i));
                }
                if(classFragment.getTryteLengthForAttribute(i)>0 && attributeValue.length()>classFragment.getTryteLengthForAttribute(i)){
                    attributeValue = attributeValue.substring(0,classFragment.getTryteLengthForAttribute(i));
                }
                dataTrytes.append(attributeValue);
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

        private int variableSizeAttributes(List<Integer> variableSizeAttributeIndexes) {
            int ret = 0;
            for(int index : variableSizeAttributeIndexes){
                ret += Optional.ofNullable( data.get(index) ).orElse( "" ).length();
            }
            return ret;
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

        private final Builder builder;

        Prepared(Builder builder){
            this.builder = builder;
        }

        public List<TransactionBuilder> fromTailToHead(){
            return builder.getTailToHead();
        }
    }

    public interface Filter {
        boolean match(DataFragment dataFragment);

        static Filter and(Filter f0, Filter f1){
            return dataFragment -> f0.match(dataFragment) && f1.match(dataFragment);
        }
    }

}
