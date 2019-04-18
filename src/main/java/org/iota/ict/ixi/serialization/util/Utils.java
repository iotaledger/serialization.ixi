package org.iota.ict.ixi.serialization.util;

import org.iota.ict.ixi.serialization.SerializationModule;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

import static org.iota.ict.utils.Trytes.TRITS_BY_TRYTE;
import static org.iota.ict.utils.Trytes.TRYTES;

public class Utils {


    public static final BigInteger BIG_3 = BigInteger.valueOf(3);
    public static final BigInteger BIG_2 = BigInteger.valueOf(2);

    public static TransactionBuilder padRightSignature(TransactionBuilder builder){
        builder.signatureFragments = Trytes.padRight(builder.signatureFragments, Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        return builder;
    }

    public static String fit(String original, int targetSize){
        if(original==null) original = "";
        if(original.length()<targetSize) return Trytes.padRight(original, targetSize);
        if(original.length()>targetSize) return original.substring(0,targetSize);
        return original;
    }

    public static String asciiFromTrits(byte[] trits){
        byte[] b = completeTritsBeforeConversion(trits);
        return Trytes.toAscii(Trytes.fromTrits(b));
    }

    public static byte[] tritsFromAscii(String value){
        return Trytes.toTrits(Trytes.fromAscii(value));
    }

    private static byte[] completeTritsBeforeConversion(byte[] trits) {
        byte[] b;
        if(trits.length % 3 == 1){
            b = new byte[trits.length+2];
        }else if (trits.length % 3 == 2) {
            b = new byte[trits.length + 1];
        } else {
            b = new byte[trits.length];
        }
        System.arraycopy(trits,0,b,0,trits.length);
        return b;
    }

    public static BigInteger integerFromTrits(byte[] trits) {
        byte[] b = completeTritsBeforeConversion(trits);
        return Trytes.toNumber(Trytes.fromTrits(b));
    }

    public static byte[] toTrits(char tryte) {
        return TRITS_BY_TRYTE[TRYTES.indexOf(tryte)];
    }

    public static byte[] readNtritsFromBundleFragment(int n, Transaction t, int startOffset){
        int availableTritsInCurrentTransaction = Transaction.Field.SIGNATURE_FRAGMENTS.tritLength - startOffset;
        byte[] trits = new byte[n];
        int unreadTritsCount = n;
        int readTritsCount = 0;
        while(availableTritsInCurrentTransaction<unreadTritsCount){
            byte[] msgTrits = Trytes.toTrits(t.signatureFragments());
            System.arraycopy(msgTrits, startOffset, trits, readTritsCount, availableTritsInCurrentTransaction);
            t = t.getTrunk();
            readTritsCount +=availableTritsInCurrentTransaction;
            unreadTritsCount -= availableTritsInCurrentTransaction;
            availableTritsInCurrentTransaction = Transaction.Field.SIGNATURE_FRAGMENTS.tritLength;
            startOffset = 0;
        }
        if(unreadTritsCount>0){
            byte[] msgTrits = Trytes.toTrits(t.signatureFragments());
            System.arraycopy(msgTrits, startOffset, trits, readTritsCount, unreadTritsCount);
        }
        return trits;
    }

    public static boolean isValidHash(String hash) {
        if(hash == null)
            return false;
        return hash.matches("^[A-Z9]{81,81}$");
    }

    public static Boolean booleanFromTrits(byte[] value) {
        return value==null? null : value[0] == 1;
    }

    public static byte[] tritsFromBoolean(Boolean value, int length) {
        byte[] ret = new byte[length];
        if(value!=null && value){
            ret[0] = 1;
        }
        return ret;
    }

    public static BigDecimal decimalFromTrits(byte[] value) {
        int expLength = value.length/3;
        int mantissaLength = value.length - expLength;
        byte[] mantissaTrits = new byte[mantissaLength];
        System.arraycopy(value,0, mantissaTrits,0,mantissaLength);
        BigInteger mantissa = integerFromTrits(mantissaTrits);
        byte[] exponent_trits = new byte[expLength];
        System.arraycopy(value,mantissaLength, exponent_trits,0,expLength);
        BigInteger exponent = integerFromTrits(exponent_trits);
        return BigDecimal.valueOf(mantissa.longValue(),exponent.intValue());
    }

    public static byte[] decimalToTrits(Object value, int tritsLength) {
        byte[] ret = new byte[tritsLength];
        BigDecimal decimal = null;
        if(value instanceof Float){
            decimal = new BigDecimal(value.toString());
        }else if(value instanceof Double){
            decimal = new BigDecimal(value.toString());
        }else if(value instanceof BigDecimal){
            decimal = (BigDecimal)value;
        }
        if(decimal!=null){
            int expLength = tritsLength/3;
            int mantissaLength = tritsLength - expLength;
            System.arraycopy(Trytes.toTrits(Trytes.fromNumber(BigInteger.valueOf(decimal.scale()),expLength)),0,ret,mantissaLength,expLength);
            System.arraycopy(Trytes.toTrits(Trytes.fromNumber(decimal.unscaledValue(),mantissaLength)),0,ret,0,mantissaLength);
        }
        return ret;
    }

    public static byte[] tritsFromInteger(Integer value, int length) {
        if(value!=null && length>0){
            return Utils.tritsFromBigInteger(BigInteger.valueOf(value), length);
        }
        return new byte[length];
    }

    public static byte[] tritsFromLong(Long value, int length) {
        if(value!=null && length>0){
            return Utils.tritsFromBigInteger(BigInteger.valueOf(value), length);
        }
        return new byte[length];
    }

    public static byte[] tritsFromBigInteger(BigInteger value, int length){
        if(value==null || length==0 || value.equals(BigInteger.ZERO)){
            return new byte[length];
        }
        boolean negative = value.compareTo(BigInteger.ZERO) < 0;
        if (negative) value = value.negate();
        ArrayList<BigInteger> unbalanced = new ArrayList<>();
        BigInteger quotien = value;
        while (quotien.compareTo(BigInteger.ZERO) > 0) {
            unbalanced.add( quotien.mod(BIG_3));
            quotien = quotien.divide(BIG_3);
        }

        ArrayList<Integer> resp = new ArrayList<>();
        BigInteger carry = BigInteger.ZERO;
        for (int i = 0; i < unbalanced.size(); i++) {
            BigInteger unbal = unbalanced.get(i);
            if(unbal.compareTo(BIG_2)==0){
                if(carry.compareTo(BigInteger.ZERO)==0){
                    resp.add(-1);
                    carry= BigInteger.ONE;
                }else{
                    resp.add(0);
                    carry= BigInteger.ONE;
                }
            }else{
                if(unbal.add(carry).compareTo(BIG_2)==0){
                    resp.add(-1);
                    carry=BigInteger.ONE;
                }else{
                    if(unbal.add(carry).compareTo(BigInteger.ONE)==0){
                        resp.add(1);
                        carry=BigInteger.ZERO;
                    }else{
                        resp.add(0);
                        carry=BigInteger.ZERO;
                    }
                }
            }

        }
        if (carry.compareTo(BigInteger.ZERO)!=0) {
            resp.add(1);
        }
        byte[] ret = new byte[length];
        for(int i=0;i<resp.size() && i<length; i++){
            if(negative){
                switch(resp.get(i)){
                    case 1:ret[i]=-1;break;
                    case -1:ret[i]=1;break;
                }
            }else {
                ret[i] = resp.get(i).byteValue();
            }
        }
        return ret;
    }
}
