package org.iota.ict.ixi.serialization.model;

import org.iota.ict.model.Transaction;

public abstract class BundleFragment {

    private Transaction headTransaction;

    public BundleFragment(Transaction headTransaction) {
        this.headTransaction = headTransaction;
    }

    public Transaction getHeadTransaction() {
        return headTransaction;
    }

    public Transaction getTailTransaction() {
        Transaction tail = headTransaction;
        while(!hasTailFlag(tail)){
            tail = tail.getTrunk();
        }
        return tail;
    }

    abstract boolean hasTailFlag(Transaction t);
    abstract boolean hasHeadFlag(Transaction t);
}
