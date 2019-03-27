package org.iota.ict.ixi.serialization.model;

import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Trytes;

public class MetadataFragment extends BundleFragment {


    public MetadataFragment(Transaction headTransaction){
        super(headTransaction);
    }



    public boolean hasTailFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[3]==1;
    }

    public boolean hasHeadFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[4]==1;
    }
}
