package org.iota.ict.ixi.serialization.util;

import org.iota.ict.utils.Trytes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

public interface TritsConverter<T> {

    T fromTrits(byte[] trits);

    byte[] toTrits(T value, int length);

    TritsConverter<String> ASCII = new AsciiConverter();
    TritsConverter<String> TRYTES = new TrytesConverter();
    TritsConverter<Integer> INTEGER = new IntegerConverter();
    TritsConverter<Long> LONG = new LongConverter();
    TritsConverter<BigInteger> BIG_INTEGER = new BigIntegerConverter();
    TritsConverter<Float> FLOAT = new FloatConverter();
    TritsConverter<Double> DOUBLE = new DoubleConverter();
    TritsConverter<BigDecimal> BIG_DECIMAL = new BigDecimalConverter();
    TritsConverter<Boolean> BOOLEAN = new BooleanConverter();

    class Factory {

        private static HashMap<Class, TritsConverter> cache = new HashMap<>();

        static {
            cache.put(ASCII.getClass(),ASCII);
            cache.put(TRYTES.getClass(),TRYTES);
            cache.put(INTEGER.getClass(),INTEGER);
            cache.put(LONG.getClass(),LONG);
            cache.put(BIG_INTEGER.getClass(),BIG_INTEGER);
            cache.put(FLOAT.getClass(),FLOAT);
            cache.put(DOUBLE.getClass(),DOUBLE);
            cache.put(BIG_DECIMAL.getClass(),BIG_DECIMAL);
            cache.put(BOOLEAN.getClass(),BOOLEAN);
        }

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
    class TrytesConverter implements TritsConverter<String> {
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
    }

    class AsciiConverter implements TritsConverter<String> {
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
    }

    class IntegerConverter implements TritsConverter<Integer> {
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
    }

    class LongConverter implements TritsConverter<Long> {
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
    }

    class BigIntegerConverter implements TritsConverter<BigInteger> {
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
    }

    class FloatConverter implements TritsConverter<Float> {
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
    }

    class DoubleConverter implements TritsConverter<Double> {
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
    }

    class BigDecimalConverter implements TritsConverter<BigDecimal> {
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
    }

    class BooleanConverter implements TritsConverter<Boolean> {
        @Override
        public Boolean fromTrits(byte[] trits) {
            if(trits==null) return null;
            return Utils.booleanFromTrits(trits);
        }

        @Override
        public byte[] toTrits(Boolean value, int length) {
            return Utils.tritsFromBoolean(value, length);
        }
    }


}
