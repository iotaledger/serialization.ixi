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

    public static final int CURL_ROUNDS_CLASS_HASH = 27;

    public static final int TRYTE_LENGTH_OF_ATTRIBUTE_SIZE_FIELD = 6;
    public static final int TRYTE_LENGTH_OF_ATTRIBUTE_NAME_FIELD = 27;
    public static final int TRYTE_LENGTH_OF_ATTRIBUTE_FIELD = TRYTE_LENGTH_OF_ATTRIBUTE_SIZE_FIELD+TRYTE_LENGTH_OF_ATTRIBUTE_NAME_FIELD;

    public static final int TRYTE_LENGTH_OF_SIZE_FIELD = 27;
    public static final int TRYTE_LENGTH_OF_REFCOUNT_FIELD = 27;
    public static final int TRYTE_LENGTH_OF_ATTRIBUTECOUNT_FIELD = 27;
    private static final int TRYTE_LENGTH_OF_HEADER = TRYTE_LENGTH_OF_REFCOUNT_FIELD+TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_ATTRIBUTECOUNT_FIELD;

    private final int dataSize;
    private final int refCount;
    private final int attributeCount;
    private String classHash;
    private final int[] attributesLength;
    private final String[] attributesName;
    private final String[] referencedClassHash;
    private final List<Integer> variableSizeAttributes = new ArrayList<>();

    public ClassFragment(Transaction headTransaction) {
        super(headTransaction);

        //extracting the metadata so that we have a quick access to it and
        //more importantly: the existence of underlying transactions is not required when accessing the metadata at a later point in time.
        dataSize = Trytes.toNumber(headTransaction.signatureFragments().substring(0, TRYTE_LENGTH_OF_SIZE_FIELD)).intValue();
        refCount = Trytes.toNumber(headTransaction.signatureFragments().substring(TRYTE_LENGTH_OF_SIZE_FIELD,TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_REFCOUNT_FIELD)).intValue();
        attributeCount = Trytes.toNumber(headTransaction.signatureFragments().substring(TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_REFCOUNT_FIELD,TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_REFCOUNT_FIELD+TRYTE_LENGTH_OF_ATTRIBUTECOUNT_FIELD)).intValue();
        attributesLength = new int[attributeCount];
        attributesName = new String[attributeCount];
        int[] attributesOffsets = new int[attributeCount];

        //parse attributes metadata
        int currentOffset = TRYTE_LENGTH_OF_HEADER;
        for(int i=0;i<attributeCount;i++){
            attributesLength[i] = Trytes.toNumber(headTransaction.signatureFragments().substring(currentOffset, currentOffset+TRYTE_LENGTH_OF_ATTRIBUTE_SIZE_FIELD)).intValue();
            if(i==0){
                attributesOffsets[i] = 0;
            }else{
                attributesOffsets[i] = attributesOffsets[i-1]+attributesLength[i];
            }
            if(attributesLength[i]==0){
                variableSizeAttributes.add(i);
            }
            currentOffset += TRYTE_LENGTH_OF_ATTRIBUTE_SIZE_FIELD;
            attributesName[i] = headTransaction.signatureFragments().substring(currentOffset, currentOffset+TRYTE_LENGTH_OF_ATTRIBUTE_NAME_FIELD);
        }

        //parse references metadata
        referencedClassHash = new String[refCount];
        Transaction tx = headTransaction;
        int i = 0;
        while( i<refCount ){
            if(i==0){
                referencedClassHash[i] = tx.extraDataDigest();
                i++;
            }else{
                referencedClassHash[i] = tx.address();
                i++;
                if(i<refCount){
                    referencedClassHash[i] = tx.extraDataDigest();
                    i++;
                }
            }
            tx = tx.getTrunk();
            if(tx==null){
                while(i<refCount){
                    referencedClassHash[i] = Trytes.NULL_HASH;
                    i++;
                }
            }
        }
    }

    public boolean hasTailFlag(Transaction transaction){
        return isTail(transaction);
    }

    public boolean hasHeadFlag(Transaction transaction){
        return isHead(transaction);
    }

    public List<Integer> getVariableSizeAttributeIndexes(){
        return variableSizeAttributes;
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

    public int getAttributeCount() {
        return attributeCount;
    }

    private String computeClassHash(){
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while(i<refCount){
            sb.append(getClassHashForReference(i));
            i+=1;
        }
        i = 0;
        while(i<attributeCount){
            if(attributesLength[i]==0){
                sb.append(Trytes.fromNumber(BigInteger.valueOf(TRYTE_LENGTH_OF_ATTRIBUTE_SIZE_FIELD), TRYTE_LENGTH_OF_ATTRIBUTE_SIZE_FIELD));
            }else {
                sb.append(Trytes.fromNumber(BigInteger.valueOf(attributesLength[i]), TRYTE_LENGTH_OF_ATTRIBUTE_SIZE_FIELD));
            }
            sb.append(attributesName[i]);
            i+=1;
        }
        return IotaCurlHash.iotaCurlHash(sb.toString(),sb.length(),CURL_ROUNDS_CLASS_HASH);
    }

    public static boolean isTail(Transaction transaction) {
        return transaction!=null && Trytes.toTrits(transaction.tag())[3] == 1;
    }

    public static boolean isHead(Transaction transaction) {
        return transaction!=null && Trytes.toTrits(transaction.tag())[4] == 1;
    }

     public String getClassHashForReference(int index) {
        try {
            return referencedClassHash[index];
        }catch (ArrayIndexOutOfBoundsException e){
            return Trytes.NULL_HASH;
        }
    }

    public boolean isReferencing(int referenceIndex, String anotherClasshash){
        if(anotherClasshash==null) return false;
        if(referenceIndex<0){
            for(int i=0;i<refCount;i++){
                if(anotherClasshash.equals(referencedClassHash[i]))return true;
            }
            return false;
        }
        return anotherClasshash.equals(getClassHashForReference(referenceIndex));
    }
    protected int getTryteLengthForAttribute(int attributeIndex){
        return attributesLength[attributeIndex];
    }


    public static class Builder extends BundleFragment.Builder<ClassFragment> {

        private int dataSize;
        private final List<String> references = new ArrayList<>();
        private final List<String> attributes = new ArrayList<>();
        private final List<String> attributesName = new ArrayList<>();

        public Builder addReferencedClasshash(String referencedClassHash){
            references.add(referencedClassHash);
            return this;
        }

        public Builder addReferencedClass(ClassFragment classFragment){
            references.add(classFragment.getClassHash());
            return this;
        }

        public Builder addAttribute(int tryteSize, String name){
            assert tryteSize < 193710245;
            assert name.length() <= 27;
            assert Utils.isValidAttributeName(name);
            attributes.add(Trytes.fromNumber(BigInteger.valueOf(tryteSize),TRYTE_LENGTH_OF_ATTRIBUTE_SIZE_FIELD));
            attributesName.add(Trytes.padRight(name, TRYTE_LENGTH_OF_ATTRIBUTE_NAME_FIELD));
            dataSize += tryteSize;
            if(tryteSize==0){
                dataSize += TRYTE_LENGTH_OF_ATTRIBUTE_SIZE_FIELD;
            }
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
            int trytesRequiredForData = TRYTE_LENGTH_OF_HEADER + ((TRYTE_LENGTH_OF_ATTRIBUTE_FIELD)*attributes.size());
            int transactionsRequiredForData = 1 + (trytesRequiredForData/Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
            int transactionsRequiredForReferences = 1 + refCount/2;
            int transactionsRequired = Math.max(transactionsRequiredForData, transactionsRequiredForReferences);

            int currentRefIndex = 0;

            TransactionBuilder builder = new TransactionBuilder();
            int messageOffset = 0;

            StringBuilder dataTrytes = new StringBuilder();
            for (String attribute : attributes) {
                dataTrytes.append(attribute);
            }
            int trytesOffset = 0;
            for(int i=0;i<transactionsRequired;i++){
                StringBuilder stringBuilder = new StringBuilder();
                if(i==0) {
                    stringBuilder.append(Trytes.fromNumber(BigInteger.valueOf(dataSize),TRYTE_LENGTH_OF_SIZE_FIELD));
                    stringBuilder.append(Trytes.fromNumber(BigInteger.valueOf(references.size()),TRYTE_LENGTH_OF_REFCOUNT_FIELD));
                    stringBuilder.append(Trytes.fromNumber(BigInteger.valueOf(attributes.size()),TRYTE_LENGTH_OF_ATTRIBUTECOUNT_FIELD));
                    messageOffset = TRYTE_LENGTH_OF_HEADER;
                }

                if(i>0) {
                    if (currentRefIndex < references.size()) {
                        builder.address = references.get(currentRefIndex++);
                    }
                }
                if(currentRefIndex < references.size()) {
                    builder.extraDataDigest = references.get(currentRefIndex++);
                }

                if(trytesOffset<dataTrytes.length()){
                    int tryteLengthStoredHere = Math.min((dataTrytes.length()-trytesOffset),Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength-messageOffset);
                    stringBuilder.append(dataTrytes.substring(trytesOffset, trytesOffset+tryteLengthStoredHere));
                    trytesOffset += tryteLengthStoredHere;
                }
                builder.signatureFragments = stringBuilder.toString();
                Utils.padRightSignature(builder);

                addFirst(builder);
                builder = new TransactionBuilder();
                messageOffset = 0;
            }
            getHead().address = Trytes.NULL_HASH;
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

    public interface Filter<T> {
        boolean match(ClassFragment classFragment);

        static ClassFragment.Filter and(ClassFragment.Filter f0, ClassFragment.Filter f1){
            return fragment -> f0.match(fragment) && f1.match(fragment);
        }
    }

    public static class Prepared {

        private final ClassFragment.Builder builder;

        Prepared(ClassFragment.Builder builder){
            this.builder = builder;
        }

        public List<TransactionBuilder> fromTailToHead(){
            return builder.getTailToHead();
        }
    }

}
