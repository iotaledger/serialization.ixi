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

public class DataFragment extends BundleFragment {

    private static final int LENGTH_OF_SIZE_FIELD = 27;

    private int dataSizeInTrits = 0;

    public DataFragment(Transaction headTransaction){
        super(headTransaction);
        dataSizeInTrits = Trytes.toNumber(headTransaction.signatureFragments().substring(0,9)).intValue();
    }

    public byte[] getData(){
        byte[] ret = new byte[dataSizeInTrits];
        StringBuilder sb = new StringBuilder(getHeadTransaction().signatureFragments().substring(LENGTH_OF_SIZE_FIELD/3));
        Transaction tx = getHeadTransaction().getTrunk();
        while(tx!=null && dataSizeInTrits/3 > sb.length()){
            sb.append(tx.signatureFragments());
            tx = tx.getTrunk();
        }
        int tryteLength = (dataSizeInTrits/3)+1;
        byte[] trits = Trytes.toTrits(sb.substring(0,Math.min(sb.length(),tryteLength)));
        System.arraycopy(trits,0,ret,0,Math.min(dataSizeInTrits, trits.length));
        return ret;
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


    public static class Builder extends BundleFragment.Builder<DataFragment> {

        private Map<Integer, String> referenceHashes = new HashMap<>();
        private byte[] data;
        private String classHash;

        public Builder(ClassFragment classFragment){
            this(classFragment.getClassHash());
        }

        public Builder(String classHash){
            this.classHash = classHash;
        }

        @Override
        public DataFragment build() {
            prepareTransactionBuilders();
            setTags();
            setBundleBoundaries();

            Transaction lastTransaction = buildBundleFragment();

            return new DataFragment(lastTransaction);
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

        public void setData(byte[] data) {
            this.data = data;
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
            int transactionsRequiredForData = data==null ? 1 : 1 + (data.length / Transaction.Field.SIGNATURE_FRAGMENTS.tritLength);
            int transactionsRequiredForReferences = 1 + (refCount)/2;
            int transactionsRequired = Math.max(transactionsRequiredForData, transactionsRequiredForReferences);

            int dataIndex = 0;
            int currentRefIndex = 0;

            TransactionBuilder builder = new TransactionBuilder();
            builder.address = classHash;
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
                if(data!=null && data.length > 0 && dataIndex < data.length) {
                    StringBuilder sb = new StringBuilder();
                    int offsetInMessageField = 0;
                    if(i==0){
                        //27 first trits store the size of the data
                        offsetInMessageField = LENGTH_OF_SIZE_FIELD;
                        sb.append(Trytes.fromNumber(BigInteger.valueOf(data.length),LENGTH_OF_SIZE_FIELD/3));
                    }
                    byte[] tx_data = new byte[Math.min(data.length-dataIndex, Transaction.Field.SIGNATURE_FRAGMENTS.tritLength - offsetInMessageField)];
                    System.arraycopy(data,dataIndex,tx_data,0,tx_data.length);
                    sb.append(Trytes.fromTrits(tx_data));
                    builder.signatureFragments = sb.toString();
                    Utils.padRightSignature(builder);
                    dataIndex += tx_data.length;
                }
                addFirst(builder);
                builder = new TransactionBuilder();
            }
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
    }

    public interface Listener {
        void onData(DataFragment dataFragment);
    }
}
