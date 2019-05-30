package org.iota.ict.ixi;

import org.iota.ict.ixi.serialization.SerializationModule;
import org.iota.ict.ixi.serialization.model.ClassFragment;
import org.iota.ict.ixi.serialization.util.Utils;
import org.iota.ict.utils.Trytes;

import java.security.SecureRandom;

public class TestUtils {

    private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ9";
    private static final SecureRandom r = new SecureRandom();

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

    public static String randomBundleHeadHash(){
        String ret = null;
        while(ret==null || !Utils.isBundleHead(ret)){
            ret = randomHash();
        }
        return ret;
    }

    public static ClassFragment getRandomPublishedClassFragment(SerializationModule serializationModule, int... attributes) {
        ClassFragment.Builder builder = new ClassFragment.Builder();
        builder.addReferencedClasshash(TestUtils.randomHash());
        for (int i : attributes) builder.addAttribute(i, TestUtils.random(10));
        ClassFragment classFragment = serializationModule.publishBundleFragment(builder);
        safeSleep(100);
        return classFragment;
    }

    public static void safeSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //ignore
        }
    }
}
