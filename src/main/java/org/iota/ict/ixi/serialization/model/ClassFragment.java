package org.iota.ict.ixi.serialization.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ClassFragment extends BundleFragment {

    private static final int LENGTH_OF_SIZE_FIELD = 27;
    private static final int LENGTH_OF_REFCOUNT_FIELD = 27;
    private static final int LENGTH_OF_DATA = LENGTH_OF_REFCOUNT_FIELD+LENGTH_OF_SIZE_FIELD;
    private static final int TRYTE_LENGTH_OF_SIZE_FIELD = LENGTH_OF_SIZE_FIELD/3;
    private static final int TRYTE_LENGTH_OF_REFCOUNT_FIELD = LENGTH_OF_REFCOUNT_FIELD/3;
    private static final int TRYTE_LENGTH_OF_DATA = TRYTE_LENGTH_OF_REFCOUNT_FIELD+TRYTE_LENGTH_OF_SIZE_FIELD;

    private int dataSize;
    private int refCount;
    private String classHash;

    public ClassFragment(Transaction headTransaction) {
        super(headTransaction);
        dataSize = Trytes.toNumber(headTransaction.signatureFragments().substring(0, TRYTE_LENGTH_OF_SIZE_FIELD)).intValue();
        refCount = Trytes.toNumber(headTransaction.signatureFragments().substring(TRYTE_LENGTH_OF_SIZE_FIELD,TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_REFCOUNT_FIELD)).intValue();
    }

    public boolean hasTailFlag(Transaction transaction){
        return isTail(transaction);
    }

    public boolean hasHeadFlag(Transaction transaction){
        return isHead(transaction);
    }

    public String getClassHash(){
        if(classHash==null){
            classHash = computeClassHash();
        }
        return classHash;
    }

    public int getDataSize() {
        return dataSize;
    }

    public int getRefCount() {
        return refCount;
    }

    private String computeClassHash(){
        StringBuilder sb = new StringBuilder(getHeadTransaction().signatureFragments().substring(0, TRYTE_LENGTH_OF_SIZE_FIELD));
        if(refCount>0){
            sb.append(getHeadTransaction().extraDataDigest());
            int refIndex = 1;
            Transaction tx = getHeadTransaction().getTrunk();
            while(refIndex<refCount){
                sb.append(tx.address());
                if(refIndex+1 < refCount){
                    sb.append(tx.extraDataDigest());
                }
                refIndex += 2;
                tx = tx.getTrunk();
            }
        }
        String hash = IotaCurlHash.iotaCurlHash(sb.toString(),sb.length(),CURL_ROUNDS_BUNDLE_FRAGMANT_HASH);
        return hash;
    }

    public static boolean isTail(Transaction transaction) {
        return transaction!=null && Trytes.toTrits(transaction.tag())[3] == 1;
    }

    public static boolean isHead(Transaction transaction) {
        return transaction!=null && Trytes.toTrits(transaction.tag())[4] == 1;
    }

    public String getClassHashForReference(int index) {
        index++;
        Transaction tx = getHeadTransaction();
        while(index >= 2){
            tx = tx.getTrunk();
            index -=2;
        }
        if(tx!=null) {
            if (index == 0) return tx.address();
            if (index == 1) return tx.extraDataDigest();
        }
        return Trytes.NULL_HASH;
    }


    public static class Builder extends BundleFragment.Builder<ClassFragment> {

        private int dataSize;
        private List<String> references = new ArrayList<>();

        public Builder withDataSize(int size){
            this.dataSize = size;
            return this;
        }
        public Builder addReferencedClasshash(String referencedClassHash){
            references.add(referencedClassHash);
            return this;
        }
        public Builder addReferencedClass(ClassFragment classFragment){
            references.add(classFragment.getClassHash());
            return this;
        }

        @Override
        public ClassFragment build() {
            prepareTransactionBuilders();
            setTags();
            setBundleBoundaries();

            Transaction lastTransaction = buildBundleFragment();

            return new ClassFragment(lastTransaction);
        }

        public ClassFragment.Prepared prepare() {
            prepareTransactionBuilders();
            setTags();
            return new ClassFragment.Prepared(this);
        }

        private void prepareTransactionBuilders() {
            int refCount = references.size();
            int transactionsRequiredForData = 1;
            int transactionsRequiredForReferences = 1 + refCount/2;
            int transactionsRequired = Math.max(transactionsRequiredForData, transactionsRequiredForReferences);

            int currentRefIndex = 0;
            StringBuilder sb = new StringBuilder();

            TransactionBuilder builder = new TransactionBuilder();
            for(int i=0;i<transactionsRequired;i++){
                if(i==0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    //sb.append(Trytes.fromNumber(BigInteger.valueOf((LENGTH_OF_SIZE_FIELD+LENGTH_OF_REFCOUNT_FIELD)/3),LENGTH_OF_SIZE_FIELD/3));
                    stringBuilder.append(Trytes.fromNumber(BigInteger.valueOf(dataSize),LENGTH_OF_SIZE_FIELD/3));
                    stringBuilder.append(Trytes.fromNumber(BigInteger.valueOf(references.size()),LENGTH_OF_REFCOUNT_FIELD/3));
                    builder.signatureFragments = stringBuilder.toString();
                    Utils.padRightSignature(builder);
                    sb.append(Trytes.fromNumber(BigInteger.valueOf(dataSize),LENGTH_OF_SIZE_FIELD/3));
                }

                if(i>0) {
                    if (currentRefIndex < references.size()) {
                        String address = references.get(currentRefIndex++);
                        builder.address = address;
                        sb.append(builder.address);
                    }
                }
                if(currentRefIndex < references.size()) {
                    String extra = references.get(currentRefIndex++);
                    builder.extraDataDigest = extra;
                    sb.append(builder.extraDataDigest);
                }

                addFirst(builder);
                builder = new TransactionBuilder();
            }

            //TODO : decide
            getHead().address = IotaCurlHash.iotaCurlHash(sb.toString(),sb.length(),CURL_ROUNDS_BUNDLE_FRAGMANT_HASH);
            //getHead().address = Trytes.NULL_HASH;
        }

        private void setTags(){
            if(getTransactionCount()==1){
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[] { 0, 0, 0, 1, 1 ,0}), Transaction.Field.TAG.tryteLength);
            }else{
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[] { 0, 0, 0, 1, 0 ,0}), Transaction.Field.TAG.tryteLength);
                getHead().tag = Trytes.padRight(Trytes.fromTrits(new byte[] { 0, 0, 0, 0, 1 ,0}), Transaction.Field.TAG.tryteLength);
            }
        }

    }


    public static class Prepared {

        private ClassFragment.Builder builder;

        Prepared(ClassFragment.Builder builder){
            this.builder = builder;
        }

        public List<TransactionBuilder> fromTailToHead(){
            return builder.getTailToHead();
        }
    }

}
