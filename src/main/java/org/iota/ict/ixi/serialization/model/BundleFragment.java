package org.iota.ict.ixi.serialization.model;

import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;

import java.util.LinkedList;
import java.util.List;

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

    public static class Builder {

        private final LinkedList<TransactionBuilder> tailToHead = new LinkedList<>();

        public void append(List<TransactionBuilder> unfinishedTransactionsFromTailToHead) {
            for (TransactionBuilder unfinishedTransaction : unfinishedTransactionsFromTailToHead)
                append(unfinishedTransaction);
        }

        public void append(TransactionBuilder unfinishedTransaction) {
            tailToHead.add(unfinishedTransaction);
        }

        public void addFirst(TransactionBuilder unfinishedTransaction) {
            tailToHead.add(0,unfinishedTransaction);
        }

        public int getTransactionCount(){
            return tailToHead.size();
        }

        public TransactionBuilder getHead(){
            return tailToHead.getLast();
        }

        public TransactionBuilder getTail(){
            return tailToHead.getFirst();
        }

        public Transaction buildBundleFragment() {
            return buildTrunkLinkedChainAndReturnHead();
        }

        private Transaction buildTrunkLinkedChainAndReturnHead() {

            Transaction lastTransaction = null;
            for (int i = 0; i < tailToHead.size(); i++) {
                boolean isFirst = i == 0;
                TransactionBuilder unfinished = tailToHead.get(i);
                if (!isFirst)
                    unfinished.trunkHash = lastTransaction.hash;
                Transaction currentTransaction = unfinished.build();
                currentTransaction.setTrunk(lastTransaction);
                lastTransaction = currentTransaction;
            }

            return lastTransaction;
        }
    }
}
