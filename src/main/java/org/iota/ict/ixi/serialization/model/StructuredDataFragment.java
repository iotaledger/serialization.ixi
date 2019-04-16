package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.model.md.FieldType;
import org.iota.ict.ixi.serialization.util.SerializableField;
import org.iota.ict.ixi.serialization.util.UnknownMetadataException;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

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
        for (int i = 0; i < metadataFragment.getKeyCount(); i++) {
            offsets.add(currentOffset);
            if (currentOffset > ((transactionIndex + 1) * Transaction.Field.SIGNATURE_FRAGMENTS.tritLength)) {
                t = t.getTrunk();
                transactionIndex++;
            }
            FieldDescriptor descriptor = metadataFragment.getDescriptor(i);
            if (descriptor.isSingleValue()) {
                long elementLength = descriptor.getTritSize().longValue();
                currentOffset += elementLength;
            } else {
                long listLength = readListSizeAtOffset(t, (int) currentOffset % Transaction.Field.SIGNATURE_FRAGMENTS.tritLength);
                currentOffset += LIST_SIZE_FIELD_TRIT_SIZE;
                currentOffset += listLength * descriptor.getTritSize().longValue();
            }
        }
        totalSize = currentOffset;
    }

    private long readListSizeAtOffset(Transaction t, int offsetInTransaction) {
        byte[] listLengthInTrits = Utils.readNtritsFromBundleFragment(LIST_SIZE_FIELD_TRIT_SIZE, t, offsetInTransaction);

        return Utils.integerFromTrits(listLengthInTrits).longValue();
    }

    public String getClassHash() {
        return getHeadTransaction().extraDataDigest();
    }

    public boolean hasTailFlag(Transaction transaction) {
        return isTail(transaction);
    }

    public boolean hasHeadFlag(Transaction transaction) {
        return isHead(transaction);
    }

    public static boolean isTail(Transaction transaction) {
        return Trytes.toTrits(transaction.tag())[5] == 1;
    }

    public static boolean isHead(Transaction transaction) {
        return Trytes.toTrits(transaction.tag())[6] == 1;
    }

    public byte[] getValue(int i) throws UnknownMetadataException {
        if (metadataFragment == null) throw new UnknownMetadataException();
        FieldDescriptor descriptor = metadataFragment.getDescriptor(i);
        if (descriptor == null) {
            throw new IndexOutOfBoundsException(i + " is not a valid field index for this fragment.");
        }
        long offset = offsets.get(i).longValue();
        int length = (int) (i + 1 == metadataFragment.getKeyCount() ? (totalSize - offset) : offsets.get(i + 1) - offset);
        Transaction t = getHeadTransaction();
        while (offset > Transaction.Field.SIGNATURE_FRAGMENTS.tritLength) {
            t = t.getTrunk();
            offset = offset - Transaction.Field.SIGNATURE_FRAGMENTS.tritLength;
        }

        byte[] ret = Utils.readNtritsFromBundleFragment(length, t, (int) offset);
        return ret;
    }

    public Boolean getBooleanValue(int i) throws UnknownMetadataException {
        byte[] value = getValue(i);
        return value == null ? null : value[0] == 1;
    }

    public BigInteger getIntegerValue(int i) throws UnknownMetadataException {
        byte[] value = getValue(i);
        return value == null ? null : Utils.integerFromTrits(value);
    }

    public String getAsciiValue(int i) throws UnknownMetadataException {
        byte[] value = getValue(i);
        return value == null ? "" : Utils.asciiFromTrits(value);
    }

    public String getTrytesValue(int i) throws UnknownMetadataException {
        byte[] value = getValue(i);
        if (value.length % 3 != 0) return null;
        return value == null ? null : Trytes.fromTrits(value);
    }

    public List<byte[]> getListValue(int i) throws UnknownMetadataException {
        byte[] value = getValue(i);
        FieldDescriptor descriptor = metadataFragment.getDescriptor(i);
        int elementSize = descriptor.getTritSize().intValue();
        byte[] listLengthTrits = new byte[LIST_SIZE_FIELD_TRIT_SIZE];


        System.arraycopy(value, 0, listLengthTrits, 0, LIST_SIZE_FIELD_TRIT_SIZE);
        int listSize = Utils.integerFromTrits(listLengthTrits).intValue();

        List<byte[]> ret = new ArrayList<>(listSize);

        for (int j = 0; j < listSize; j++) {
            byte[] v = new byte[elementSize];
            System.arraycopy(value, (LIST_SIZE_FIELD_TRIT_SIZE + (j * elementSize)), v, 0, elementSize);
            ret.add(v);
        }
        return ret;
    }

    public List<String> getTryteList(int index) throws UnknownMetadataException {
        List<byte[]> values = getListValue(index);
        if(values==null || values.size()==0){
            return Collections.EMPTY_LIST;
        }
        ArrayList<String> ret = new ArrayList<>();
        for(int i=0;i<values.size();i++){
            ret.add(Trytes.fromTrits(values.get(i)));
        }
        return ret;
    }




    public <T> T deserializeToClass(Class<T> clazz) throws UnknownMetadataException {
        String classHash = MetadataFragment.Builder.fromClass(clazz).build().hash();
        if (!classHash.equals(getClassHash())) {
            return null;
        }
        Map<Integer, FieldDescriptor> fieldDescriptors = new HashMap<>();
        Map<Integer, Field> javaFields = new HashMap<>();
        int fieldCount = 0;
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (field.getAnnotation(SerializableField.class) != null) {
                SerializableField annotation = field.getAnnotation(SerializableField.class);
                FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes(annotation.fieldType()), annotation.tritLength(), annotation.label());
                fieldDescriptors.put(annotation.index(), descriptor);
                javaFields.put(annotation.index(), field);
                fieldCount++;
            }
        }
        if (fieldCount == 0) {
            return null;
        }
        try {
            T data = clazz.newInstance();
            for (int i = 0; i < fieldCount; i++) {
                FieldDescriptor descriptor = fieldDescriptors.get(i);
                if (descriptor == null) {
                    return null;
                }
                Field javaField = javaFields.get(i);
                if(descriptor.isSingleValue()){
                    byte[] value = getValue(i);
                    javaField.set(data, extractJavaValue(value,javaField,descriptor));
                }else{
                    List<byte[]> values = getListValue(i);
                    javaField.set(data, extractJavaList(values,javaField,descriptor));
                }
            }
            return data;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Object extractJavaValue(byte[] value, Field field, FieldDescriptor descriptor){
        if(descriptor.isInteger()){
            BigInteger integer = Utils.integerFromTrits(value);
            if(field.getType().equals(Integer.class)){
                return integer.intValue();
            }
            if(field.getType().equals(Long.class)){
                return integer.longValue();
            }
            if(field.getType().equals(BigInteger.class)){
                return integer;
            }
            if(field.getType().getName().equals("int")){
                if(integer==null) return 0;
                return integer.intValue();
            }
            if(field.getType().getName().equals("long")){
                if(integer==null) return 0;
                return integer.longValue();
            }
            System.out.println("here");
        }
        if(descriptor.isAscii()){
            return Utils.asciiFromTrits(value);
        }
        if(descriptor.isHash()){
            return Trytes.fromTrits(value);
        }
        if(descriptor.isBoolean()){
            return Utils.booleanFromTrits(value);
        }
        if(descriptor.isDecimal()){
            BigDecimal decimal = Utils.decimalFromTrits(value);
            if(field.getType().equals(Float.class)){
                return decimal.floatValue();
            }
            if(field.getType().equals(Double.class)){
                return decimal.doubleValue();
            }
            if(field.getType().equals(BigDecimal.class)){
                return decimal;
            }
            if(field.getType().getName().equals("float")){
                if(decimal==null) return 0;
                return decimal.floatValue();
            }
            if(field.getType().getName().equals("double")){
                if(decimal==null) return 0;
                return decimal.doubleValue();
            }

        }
        return null;
    }
    private List extractJavaList(List<byte[]> values, Field field, FieldDescriptor descriptor){
        List list = new ArrayList(values.size());
        for(byte[] value:values){
            list.add(extractJavaValue(value, field, descriptor));
        }
        return list;
    }

    public static class Builder extends BundleFragment.Builder<StructuredDataFragment> {

        private Map<Integer, ValueHolder> values = new HashMap<>();

        private MetadataFragment metadata;

        List<Long> offsets;

        private BigInteger totalSize;

        public Builder setMetadata(MetadataFragment metadata) {
            this.metadata = metadata;
            offsets = new ArrayList<>(metadata.getKeyCount());
            return this;
        }

        public Builder setValue(int index, String trytes) {
            if (trytes == null) {
                values.remove(index);
            } else {
                values.put(index, new SingleValueHolder(Trytes.toTrits(trytes)));
            }
            return this;
        }

        public Builder setBooleanValue(int index, boolean b) {
            values.put(index, new SingleValueHolder(b ? new byte[]{1} : new byte[]{0}));
            return this;
        }

        public Builder setTritsValue(int index, byte[] trits) {
            if (trits == null) {
                values.remove(index);
            } else {
                values.put(index, new SingleValueHolder(trits));
            }
            return this;
        }

        public Builder fromInstance(Object data){
            MetadataFragment metadataFragment = new MetadataFragment.Builder().fromClass(data.getClass()).build();
            setMetadata(metadataFragment);
            Map<Integer, FieldDescriptor> fieldDescriptors = new HashMap<>();
            Map<Integer, Field> javaFields = new HashMap<>();
            int fieldCount = 0;
            Field[] fields = data.getClass().getFields();
            for (Field field : fields) {
                if (field.getAnnotation(SerializableField.class) != null) {
                    SerializableField annotation = field.getAnnotation(SerializableField.class);
                    FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes(annotation.fieldType()), annotation.tritLength(), annotation.label());
                    fieldDescriptors.put(annotation.index(), descriptor);
                    javaFields.put(annotation.index(), field);
                    fieldCount++;
                }
            }
            if (fieldCount == 0) {
                return null;
            }

            for (int i = 0; i < fieldCount; i++) {
                FieldDescriptor descriptor = fieldDescriptors.get(i);
                if (descriptor == null) {
                    return null;
                }

                Field javaField = javaFields.get(i);
                Object javaValue = null;
                try {
                    javaValue = javaField.get(data);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    javaValue = null;
                }

                if(descriptor.isSingleValue()){
                    setTritsValue(i, toTrits(javaValue, descriptor));
                }else{
                    setTritsValues(i, toTritsList((List) javaValue, descriptor));
                }
            }

            return this;
        }

        private byte[] toTrits(Object value, FieldDescriptor descriptor){
            if(value==null){
                return new byte[descriptor.getTritSize().intValue()];
            }
            if(descriptor.isHash()){
                return Trytes.toTrits(value.toString());
            }
            if(descriptor.isBoolean()){
                byte[] b = new byte[descriptor.getTritSize().intValue()];
                if((boolean)value){
                    b[0] = 1;
                }else{
                    b[0] = -1;
                }
                return b;
            }
            if(descriptor.isAscii()){
                return Trytes.toTrits(Trytes.fromAscii(value.toString()));
            }
            if(descriptor.isInteger()){
                //TODO : should not enforce size as multiple of 3
                if(value instanceof BigInteger){
                    return Trytes.toTrits(Trytes.fromNumber((BigInteger) value, descriptor.getTryteSize()));
                }
                if(value instanceof Integer){
                    return Trytes.toTrits(Trytes.fromNumber(BigInteger.valueOf((Integer) value), descriptor.getTryteSize()));
                }
                if(value instanceof Long){
                    return Trytes.toTrits(Trytes.fromNumber(BigInteger.valueOf((Long) value), descriptor.getTryteSize()));
                }
            }
            if(descriptor.isDecimal()){
                return Utils.decimalToTrits(value, descriptor.getTritSize().intValue());
            }
            return new byte[descriptor.getTritSize().intValue()];
        }

        private List<byte[]> toTritsList(List values, FieldDescriptor descriptor){
            if(values==null || values.size()==0) return Collections.EMPTY_LIST;
            List<byte[]> ret = new ArrayList<>(values.size());
            for(Object item:values){
                ret.add(toTrits(item,descriptor));
            }
            return ret;
        }

        public PreparedDataFragment prepare(){
            if (metadata == null) {
                throw new IllegalStateException("MetadataFragment cannot be null");
            }

            prepareTransactionBuilders();

            setTags();
            setMetadataHash();
            return new PreparedDataFragment(this);
        }
        public StructuredDataFragment build() {
            if (metadata == null) {
                throw new IllegalStateException("MetadataFragment cannot be null");
            }

            prepareTransactionBuilders();

            setTags();
            setMetadataHash();

            setBundleBoundaries();


            Transaction headTransaction = buildBundleFragment();

            return new StructuredDataFragment(headTransaction, metadata);
        }

        private void prepareTransactionBuilders() {
            computeOffsets();
            int transactionRequired = 1 + (totalSize.divide(MESSAGE_SIZE).intValue());
            byte[] message = new byte[transactionRequired * MESSAGE_SIZE.intValue()];
            for (Integer keyIndex : values.keySet()) {
                ValueHolder valueHolder = values.get(keyIndex);
                if (valueHolder instanceof SingleValueHolder) {
                    byte[] value = ((SingleValueHolder) valueHolder).value;
                    System.arraycopy(value, 0, message, offsets.get(keyIndex).intValue(), value.length);
                } else if (valueHolder instanceof MultipleValueHolder) {
                    int elementCount = ((MultipleValueHolder) valueHolder).values.size();
                    FieldDescriptor descriptor = metadata.getDescriptor(keyIndex);
                    int elementSize = descriptor.getTritSize().intValue();
                    byte[] listLengthInTrits = Trytes.toTrits(Trytes.fromNumber(BigInteger.valueOf(((MultipleValueHolder) valueHolder).values.size()), LIST_SIZE_FIELD_TRIT_SIZE / 3));
                    System.arraycopy(listLengthInTrits, 0, message, offsets.get(keyIndex).intValue(), LIST_SIZE_FIELD_TRIT_SIZE);
                    int index = offsets.get(keyIndex).intValue() + LIST_SIZE_FIELD_TRIT_SIZE;
                    for (int j = 0; j < elementCount; j++) {
                        System.arraycopy(((MultipleValueHolder) valueHolder).values.get(j), 0, message, index, elementSize);
                        index += elementSize;
                    }
                } else {
                    throw new IllegalStateException("Invalid value");
                }
            }
            TransactionBuilder builder = null;
            for (int i = 0; i < transactionRequired; i++) {
                builder = new TransactionBuilder();
                byte[] msg = new byte[Transaction.Field.SIGNATURE_FRAGMENTS.tritLength];
                System.arraycopy(message, i * MESSAGE_SIZE.intValue(), msg, 0, msg.length);
                builder.signatureFragments = Trytes.fromTrits(msg);
                addFirst(builder);
            }
        }

        private List<Long> computeOffsets() {
            long currentOffset = 0;
            for (int i = 0; i < metadata.getKeyCount(); i++) {
                offsets.add(currentOffset);
                FieldDescriptor descriptor = metadata.getDescriptor(i);
                if (descriptor.isSingleValue()) {
                    currentOffset += descriptor.getTritSize().intValue();
                } else {
                    currentOffset += LIST_SIZE_FIELD_TRIT_SIZE;
                    MultipleValueHolder valueHolder = (MultipleValueHolder) values.get(i);
                    if (valueHolder != null) {
                        long elementSize = descriptor.getTritSize().longValue();
                        int elementCount = valueHolder.values.size();
                        currentOffset += (elementCount * elementSize);
                    }
                }
            }
            totalSize = BigInteger.valueOf(currentOffset);
            return offsets;
        }

        private void setTags() {
            if (getTransactionCount() == 1) {
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[]{0, 0, 0, 0, 0, 1, 1, 0, 0}), Transaction.Field.TAG.tryteLength);
            } else {
                getTail().tag = Trytes.padRight(Trytes.fromTrits(new byte[]{0, 0, 0, 0, 0, 1, 0, 0, 0}), Transaction.Field.TAG.tryteLength);
                getHead().tag = Trytes.padRight(Trytes.fromTrits(new byte[]{0, 0, 0, 0, 0, 0, 1, 0, 0}), Transaction.Field.TAG.tryteLength);
            }
        }

        private void setMetadataHash() {
            getHead().extraDataDigest = metadata.hash();
        }

        public Builder setValues(int i, String... trytes) {
            MultipleValueHolder holder = new MultipleValueHolder();
            int elementSize = metadata.getDescriptor(i).getTritSize().intValue();
            for (String val : trytes) {
                byte[] b = new byte[elementSize];
                byte[] src = Trytes.toTrits(val);
                System.arraycopy(src, 0, b, 0, src.length);
                holder.values.add(b);
            }
            values.put(i, holder);
            return this;
        }

        public Builder setTritsValues(int i, List<byte[]> tritsList) {
            if(tritsList==null || tritsList.size()==0) {
                values.remove(i);
                return this;
            }
            MultipleValueHolder holder = new MultipleValueHolder();
            int elementSize = metadata.getDescriptor(i).getTritSize().intValue();
            for (byte[] item : tritsList) {
                byte[] b = new byte[elementSize];
                System.arraycopy(item, 0, b, 0, item.length);
                holder.values.add(b);
            }
            values.put(i, holder);
            return this;
        }

        private class ValueHolder {
        }

        private class SingleValueHolder extends ValueHolder {
            byte[] value;

            public SingleValueHolder(byte[] value) {
                this.value = value;
            }
        }

        private class MultipleValueHolder extends ValueHolder {
            List<byte[]> values = new ArrayList<>();
        }
    }
}
