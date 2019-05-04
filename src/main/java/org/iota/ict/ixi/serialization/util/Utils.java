package org.iota.ict.ixi.serialization.util;

import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.math.BigDecimal;
import java.math.BigInteger;

@SuppressWarnings("WeakerAccess")
public class Utils {

    public static TransactionBuilder padRightSignature(TransactionBuilder builder){
        builder.signatureFragments = Trytes.padRight(builder.signatureFragments, Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        return builder;
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

    public static boolean isValidHash(String hash) {
        if(hash == null)
            return false;
        return hash.matches("^[A-Z9]{81,81}$");
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

    public static boolean isBundleHead(String hash){
        byte[] hashTrits = Trytes.toTrits(hash);
        return isFlagSet(hashTrits, Constants.HashFlags.BUNDLE_HEAD_FLAG);
    }

    private static boolean isFlagSet(byte[] hashTrits, int position) {
        assert hashTrits.length == Transaction.Field.TRUNK_HASH.tritLength;
        return hashTrits[position] == 1;
    }
}
