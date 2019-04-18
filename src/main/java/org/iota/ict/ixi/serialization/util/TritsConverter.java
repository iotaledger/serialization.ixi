package org.iota.ict.ixi.serialization.util;

import org.iota.ict.utils.Trytes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

public interface TritsConverter<T> {

    T fromTrits(byte[] trits);

    byte[] toTrits(T value, int length);

    public static class Factory {
        private static HashMap<Class, TritsConverter> cache = new HashMap<>();

        public static TritsConverter get(Class clazz){
            TritsConverter ret = cache.get(clazz);
            if(ret == null){
                try {
                    ret = (TritsConverter) clazz.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("Invalid TritsConverter class "+clazz.getName()+". Should have a default constructor.");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Invalid TritsConverter class "+clazz.getName()+". Should have a default constructor.");
                }
                cache.put(clazz, ret);
            }
            return ret;
        }
    }
    public static class TRYTES implements TritsConverter<String> {
        @Override
        public String fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Trytes.fromTrits(trits);
        }

        @Override
        public byte[] toTrits(String value, int length) {
            byte[] ret = new byte[length];
            if(value!=null){
                byte[] trits = Trytes.toTrits(value);
                System.arraycopy(trits,0,ret,0,Math.min(trits.length, length));
            }
            return ret;
        }
    };

    public static class ASCII implements TritsConverter<String> {
        @Override
        public String fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Utils.asciiFromTrits(trits);
        }

        @Override
        public byte[] toTrits(String value, int length) {
            byte[] ret = new byte[length];
            if(value!=null){
                byte[] trits = Utils.tritsFromAscii(value);
                System.arraycopy(trits,0,ret,0,Math.min(trits.length, length));
            }
            return ret;
        }
    };

    public static class INTEGER implements TritsConverter<Integer> {
        @Override
        public Integer fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Utils.integerFromTrits(trits).intValue();
        }

        @Override
        public byte[] toTrits(Integer value, int length) {
            byte[] ret = new byte[length];
            if(value!=null){
                return Utils.tritsFromInteger(value, length);
            }
            return ret;
        }
    };

    public static class LONG implements TritsConverter<Long> {
        @Override
        public Long fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Utils.integerFromTrits(trits).longValue();
        }

        @Override
        public byte[] toTrits(Long value, int length) {
            byte[] ret = new byte[length];
            if(value!=null){
                return Utils.tritsFromLong(value, length);
            }
            return ret;
        }
    };

    public static class BIG_INTEGER implements TritsConverter<BigInteger> {
        @Override
        public BigInteger fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Utils.integerFromTrits(trits);
        }

        @Override
        public byte[] toTrits(BigInteger value, int length) {
            byte[] ret = new byte[length];
            if(value!=null){
                return Utils.tritsFromBigInteger(value, length);
            }
            return ret;
        }
    };

    public static class FLOAT implements TritsConverter<Float> {
        @Override
        public Float fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Utils.decimalFromTrits(trits).floatValue();
        }

        @Override
        public byte[] toTrits(Float value, int length) {
            byte[] ret = new byte[length];
            if(value!=null){
                return Utils.decimalToTrits(value, length);
            }
            return ret;
        }
    };

    public static class DOUBLE implements TritsConverter<Double> {
        @Override
        public Double fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Utils.decimalFromTrits(trits).doubleValue();
        }

        @Override
        public byte[] toTrits(Double value, int length) {
            byte[] ret = new byte[length];
            if(value!=null){
                return Utils.decimalToTrits(value, length);
            }
            return ret;
        }
    };

    public static class BIG_DECIMAL implements TritsConverter<BigDecimal> {
        @Override
        public BigDecimal fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Utils.decimalFromTrits(trits);
        }

        @Override
        public byte[] toTrits(BigDecimal value, int length) {
            byte[] ret = new byte[length];
            if(value!=null){
                return Utils.decimalToTrits(value, length);
            }
            return ret;
        }
    };

    public static class BOOLEAN implements TritsConverter<Boolean> {
        @Override
        public Boolean fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Utils.booleanFromTrits(trits);
        }

        @Override
        public byte[] toTrits(Boolean value, int length) {
            return Utils.tritsFromBoolean(value, length);
        }
    };
}
