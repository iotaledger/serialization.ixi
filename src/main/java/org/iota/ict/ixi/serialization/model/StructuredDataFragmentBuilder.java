package org.iota.ict.ixi.serialization.model;

import org.iota.ict.model.BundleFragmentBuilder;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Trytes;

public class StructuredDataFragmentBuilder extends BundleFragmentBuilder {

    public StructuredDataFragment build(){
       //TODO

        setTags();

        Transaction lastTransaction = buildBundleFragment();

        return new StructuredDataFragment(lastTransaction);
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
