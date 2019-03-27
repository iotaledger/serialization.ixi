package org.iota.ict.ixi.serialization.model;

import org.iota.ict.model.Transaction;
import org.iota.ict.utils.Trytes;

public class StructuredDataFragment extends BundleFragment {

    public StructuredDataFragment(Transaction headTransaction) {
        super(headTransaction);
    }

    public boolean hasTailFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[5]==1;
    }

    public boolean hasHeadFlag(Transaction transaction){
        return Trytes.toTrits(transaction.tag())[6]==1;
    }
}
