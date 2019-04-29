package org.iota.ict.ixi;

import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Trytes;

import java.security.SecureRandom;

public class TestUtils {

    private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ9";
    private static SecureRandom r = new SecureRandom();

    public static String random(int tryteLength){
        StringBuilder sb = new StringBuilder(tryteLength);
        for( int i = 0; i < tryteLength; i++ )
            sb.append( alphabet.charAt( r.nextInt(alphabet.length()) ) );
        return sb.toString();
    }

    public static byte[] randomTrits(int length){
        byte[] ret = new byte[length];
        String randomTrytes = random((length/3)+1);
        byte[] randomTrits = Trytes.toTrits(randomTrytes);
        System.arraycopy(randomTrits,0, ret, 0,ret.length);
        return ret;
    }

    public static String randomHash(){
        return random(81);
    }

    public static String randomValidTransactionHash(){
        String s = random(81);
        byte[] trits = Trytes.toTrits(s);
        System.arraycopy(new byte[Constants.MIN_WEIGHT_MAGNITUDE],0,trits,243-Constants.MIN_WEIGHT_MAGNITUDE, Constants.MIN_WEIGHT_MAGNITUDE);
        return Trytes.fromTrits(trits);
    }
}
