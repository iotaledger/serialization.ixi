package org.iota.ict.model;

import java.util.LinkedList;
import java.util.List;

public class BundleFragmentBuilder {

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
            currentTransaction.trunk = lastTransaction;
            lastTransaction = currentTransaction;
        }

        return lastTransaction;
    }
}
