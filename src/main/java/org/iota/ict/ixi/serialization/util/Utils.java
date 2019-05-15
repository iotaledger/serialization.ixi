package org.iota.ict.ixi.serialization.util;

import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

public class Utils {

    public static TransactionBuilder padRightSignature(TransactionBuilder builder){
        builder.signatureFragments = Trytes.padRight(builder.signatureFragments, Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        return builder;
    }

    public static boolean isValidHash(String hash) {
        if(hash == null)
            return false;
        return hash.matches("^[A-Z9]{81,81}$");
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
