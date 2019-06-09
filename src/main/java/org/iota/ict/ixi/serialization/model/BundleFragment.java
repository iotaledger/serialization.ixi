package org.iota.ict.ixi.serialization.model;

import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public abstract class BundleFragment {

    private WeakReference<Transaction> headTransaction;
    private final String txHeadHash;

    public BundleFragment(Transaction headTransaction) {
        txHeadHash = headTransaction.hash;
        init(headTransaction);
    }


    protected void init(Transaction headTransaction){
        if(!hasHeadFlag(headTransaction)){
            throw new IllegalArgumentException("head transaction don't have the head flag");
        }
        if(!hasTailFlag(headTransaction)){
            //search for tail flag is next transactions.
            Transaction tx = headTransaction.getTrunk();
            boolean foundTail = false;
            while(tx!=null && !foundTail){
                if(hasHeadFlag(tx)){
                    throw new IllegalArgumentException("no bundle tail transaction");
                }
                foundTail = hasTailFlag(tx);
                tx = tx.getTrunk();
            }
            if(!foundTail){
                throw new IllegalArgumentException("tailtransaction not found");
            }
        }
        this.headTransaction = new WeakReference<>(headTransaction);
    }

    public Transaction getHeadTransaction() {
        return headTransaction.get();
    }

    public Transaction getTailTransaction() {
        Transaction tail = headTransaction.get();
        while(!hasTailFlag(tail)){
            if(tail==null) return null;
            tail = tail.getTrunk();
        }
        return tail;
    }

    abstract boolean hasTailFlag(Transaction t);
    abstract boolean hasHeadFlag(Transaction t);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BundleFragment)) return false;
        BundleFragment fragment = (BundleFragment) o;
        return Objects.equals(txHeadHash, fragment.txHeadHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(txHeadHash);
    }

    public abstract static class Builder<T extends BundleFragment> {

        private final LinkedList<TransactionBuilder> tailToHead = new LinkedList<>();
        private boolean isHeadFragment = false;
        private boolean isTailFragment = false;
        private String referencedTrunk;
        private String referencedBranch;

        abstract public T build();

        public Builder<T> setReferencedBranch(String referencedBranch) {
            this.referencedBranch = referencedBranch;
            return this;
        }

        public Builder<T> setReferencedTrunk(String referencedTrunk) {
            this.referencedTrunk = referencedTrunk;
            return this;
        }

        public String getReferencedTrunk() {
            return referencedTrunk;
        }

        public Builder<T> setHeadFragment(boolean headFragment) {
            isHeadFragment = headFragment;
            return this;
        }

        public Builder<T> setTailFragment(boolean tailFragment) {
            isTailFragment = tailFragment;
            return this;
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

        List<TransactionBuilder> getTailToHead() {
            return tailToHead;
        }

        public Transaction buildBundleFragment() {
            return buildTrunkLinkedChainAndReturnHead();
        }

        protected void setBundleBoundaries(){
            for(TransactionBuilder builder: tailToHead){
                builder.isBundleHead = false;
                builder.isBundleTail = false;
            }
            if(isHeadFragment){
                getHead().isBundleHead = true;
            }
            if(isTailFragment){
                getTail().isBundleTail = true;
            }
        }

        private Transaction buildTrunkLinkedChainAndReturnHead() {

            Transaction lastTransaction = null;
            for (int i = 0; i < tailToHead.size(); i++) {
                boolean isFirst = i == 0;

                TransactionBuilder unfinished = tailToHead.get(i);

                if (!isFirst) {
                    unfinished.trunkHash = lastTransaction.hash;
                }else {
                    if(referencedTrunk!=null){
                        unfinished.trunkHash = referencedTrunk;
                    }
                }
                if(referencedBranch!=null) {
                    unfinished.branchHash = referencedBranch;
                }

                Transaction currentTransaction = unfinished.build();
                currentTransaction.setTrunk(lastTransaction);
                lastTransaction = currentTransaction;
            }

            return lastTransaction;
        }

    }
}
