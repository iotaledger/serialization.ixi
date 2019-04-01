package org.iota.ict.ixi.serialization.util;

import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;

import java.math.BigInteger;

import static org.iota.ict.utils.Trytes.TRITS_BY_TRYTE;
import static org.iota.ict.utils.Trytes.TRYTES;

public class Utils {

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

    public static String removeTrailing9(String s){
        while(s.length()>0 && s.charAt(s.length()-1)=='9'){
            s = s.substring(0,s.length()-1);
        }
        return s;
    }

    public static String asciiFromTrits(byte[] trits){
        byte[] b = completeTritsBeforeConversion(trits);
        return Trytes.toAscii(Trytes.fromTrits(b));
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
//        byte[] msgTrits = Trytes.toTrits(t.signatureFragments());
//        if(startOffset+n < Transaction.Field.SIGNATURE_FRAGMENTS.tritLength){
//            System.arraycopy(msgTrits, startOffset, trits, 0, n);
//        }else{
//            int remainingTrits = Transaction.Field.SIGNATURE_FRAGMENTS.tritLength - startOffset;
//            int tritsOnNextTransaction = n - remainingTrits;
//            System.arraycopy(msgTrits, startOffset, trits, 0, remainingTrits);
//            byte[] nextMsgTrits = Trytes.toTrits(t.getTrunk().signatureFragments());
//            System.arraycopy(nextMsgTrits, 0, trits, remainingTrits, tritsOnNextTransaction);
//        }
        return trits;
    }
}
