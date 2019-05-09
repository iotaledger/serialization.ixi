package org.iota.ict.ixi.serialization.model;

import com.iota.curl.IotaCurlHash;
import org.iota.ict.ixi.serialization.util.SerializableField;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class ClassFragment extends BundleFragment {

    public static int CURL_ROUNDS_CLASS_HASH = 27;

    private static final int LENGTH_OF_SIZE_FIELD = 81;
    private static final int LENGTH_OF_REFCOUNT_FIELD = 81;
    private static final int LENGTH_OF_ATTRIBUTECOUNT_FIELD = 81;
    private static final int LENGTH_OF_DATA = LENGTH_OF_REFCOUNT_FIELD+LENGTH_OF_SIZE_FIELD+LENGTH_OF_ATTRIBUTECOUNT_FIELD;
    public static final int TRYTE_LENGTH_OF_SIZE_FIELD = LENGTH_OF_SIZE_FIELD/3;
    public static final int TRYTE_LENGTH_OF_REFCOUNT_FIELD = LENGTH_OF_REFCOUNT_FIELD/3;
    public static final int TRYTE_LENGTH_OF_ATTRIBUTECOUNT_FIELD = LENGTH_OF_ATTRIBUTECOUNT_FIELD/3;
    private static final int TRYTE_LENGTH_OF_DATA = TRYTE_LENGTH_OF_REFCOUNT_FIELD+TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_ATTRIBUTECOUNT_FIELD;
    private static final int ATTRIBUTE_HASH_TRITSIZE = 243;

    private int dataSize;
    private int refCount;
    private int attributeCount;
    private String classHash;
    private int[] attributesLength;
    private int[] attributesOffsets;
    private String[] referencedClassHash;

    public ClassFragment(Transaction headTransaction) {
        super(headTransaction);
        dataSize = Trytes.toNumber(headTransaction.signatureFragments().substring(0, TRYTE_LENGTH_OF_SIZE_FIELD)).intValue();
        refCount = Trytes.toNumber(headTransaction.signatureFragments().substring(TRYTE_LENGTH_OF_SIZE_FIELD,TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_REFCOUNT_FIELD)).intValue();
        attributeCount = Trytes.toNumber(headTransaction.signatureFragments().substring(TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_REFCOUNT_FIELD,TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_REFCOUNT_FIELD+TRYTE_LENGTH_OF_ATTRIBUTECOUNT_FIELD)).intValue();
        attributesLength = new int[attributeCount];
        attributesOffsets = new int[attributeCount];
        int currentOffset = TRYTE_LENGTH_OF_SIZE_FIELD+TRYTE_LENGTH_OF_REFCOUNT_FIELD+TRYTE_LENGTH_OF_ATTRIBUTECOUNT_FIELD;
        for(int i=0;i<attributeCount;i++){
            attributesLength[i] = Trytes.toNumber(headTransaction.signatureFragments().substring(currentOffset, currentOffset+6)).intValue();
            if(i==0){
                attributesOffsets[i] = 0;
            }else{
                attributesOffsets[i] = attributesOffsets[i-1]+attributesLength[i];
            }
            currentOffset += 6;
        }
        referencedClassHash = new String[refCount];
        Transaction tx = headTransaction;
        int i = 0;
        while( i<refCount ){
            if(i==0){
                referencedClassHash[i] = tx.extraDataDigest();
                i++;
            }else{
                if(i<refCount){
                    referencedClassHash[i] = tx.address();
                    i++;
                }
                if(i<refCount){
                    referencedClassHash[i] = tx.extraDataDigest();
                    i++;
                }
            }
            tx = tx.getTrunk();
            if(tx==null){
                while(i<refCount){
                    referencedClassHash[i] = Trytes.NULL_HASH;
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
            sb.append(Trytes.fromNumber(BigInteger.valueOf(attributesLength[i]),6));
            i+=1;
        }
        String hash = IotaCurlHash.iotaCurlHash(sb.toString(),sb.length(),CURL_ROUNDS_CLASS_HASH);
        return hash;
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

    public int getTryteIndexForAttribute(int attributeIndex) {
        return attributesOffsets[attributeIndex];
    }

    protected int getTryteLengthForAttribute(int attributeIndex){
        return attributesLength[attributeIndex];
    }

    public static class Builder extends BundleFragment.Builder<ClassFragment> {

        private int dataSize;
        private List<String> references = new ArrayList<>();
        private List<String> attributes = new ArrayList<>();

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

        public Builder addAttribute(int tryteSize){
            assert tryteSize < 193710245;
            assert tryteSize%3 == 0;
            attributes.add(Trytes.fromNumber(BigInteger.valueOf(tryteSize),6));
            dataSize += tryteSize;
            return this;
        }

        public Builder addAttributesFromClass(Class clazz){
            Map<Integer, Field> javaFields = extractSerializableFields(clazz);
            for(int i=0;i<javaFields.size();i++){
                Field field = javaFields.get(i);
                if(field==null){
                    throw new IllegalArgumentException("Class "+clazz.getName()+" is not a valid serializable class. Indexes are wrong. (near index "+i+")");
                }
                addAttribute(field.getAnnotation(SerializableField.class).tryteLength());
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
            int tritsRequiredForData = LENGTH_OF_DATA + (6*attributes.size()*3);
            int transactionsRequiredForData = 1 + (tritsRequiredForData/Transaction.Field.SIGNATURE_FRAGMENTS.tritLength);
            int transactionsRequiredForReferences = 1 + refCount/2;
            int transactionsRequired = Math.max(transactionsRequiredForData, transactionsRequiredForReferences);

            int currentRefIndex = 0;

            TransactionBuilder builder = new TransactionBuilder();
            int messageOffset = 0;

            StringBuilder dataTrytes = new StringBuilder();
            for(int i=0;i<attributes.size();i++){
                dataTrytes.append(attributes.get(i));
            }
            int trytesOffset = 0;
            for(int i=0;i<transactionsRequired;i++){
                StringBuilder stringBuilder = new StringBuilder();
                if(i==0) {
                    stringBuilder.append(Trytes.fromNumber(BigInteger.valueOf(dataSize),TRYTE_LENGTH_OF_SIZE_FIELD));
                    stringBuilder.append(Trytes.fromNumber(BigInteger.valueOf(references.size()),TRYTE_LENGTH_OF_REFCOUNT_FIELD));
                    stringBuilder.append(Trytes.fromNumber(BigInteger.valueOf(attributes.size()),TRYTE_LENGTH_OF_ATTRIBUTECOUNT_FIELD));
                    messageOffset = TRYTE_LENGTH_OF_DATA;
                }

                if(i>0) {
                    messageOffset = 0;
                    if (currentRefIndex < references.size()) {
                        String address = references.get(currentRefIndex++);
                        builder.address = address;
                    }
                }
                if(currentRefIndex < references.size()) {
                    String extra = references.get(currentRefIndex++);
                    builder.extraDataDigest = extra;
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


    public static class Prepared {

        private ClassFragment.Builder builder;

        Prepared(ClassFragment.Builder builder){
            this.builder = builder;
        }

        public List<TransactionBuilder> fromTailToHead(){
            return builder.getTailToHead();
        }
    }


    private static Map<Integer, Field> extractSerializableFields(Class clazz) {
        Map<Integer, Field> javaFields = new HashMap<>();
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (field.getAnnotation(SerializableField.class) != null) {
                SerializableField annotation = field.getAnnotation(SerializableField.class);
                javaFields.put(annotation.index(), field);
            }
        }
        return javaFields;
    }
}
