package org.iota.ict.ixi.serialization.model;

import com.iota.curl.IotaCurlHash;
import com.iota.curl.IotaCurlUtils;
import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;

import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public abstract class BundleFragment {

    public static int CURL_ROUNDS_BUNDLE_FRAGMANT_HASH = 27;

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

    public String hash(){
        StringBuilder sb = new StringBuilder(headTransaction.signatureFragments());
        Transaction tx = headTransaction;
        while(!tx.isBundleTail){
            sb.append(tx.signatureFragments());
        }
        return IotaCurlHash.iotaCurlHash(sb.toString(),sb.length(),CURL_ROUNDS_BUNDLE_FRAGMANT_HASH);
    }

    abstract boolean hasTailFlag(Transaction t);
    abstract boolean hasHeadFlag(Transaction t);

    public abstract static class Builder<T> {

        private final LinkedList<TransactionBuilder> tailToHead = new LinkedList<>();
        private boolean isHeadFragment = true;
        private boolean isTailFragment = true;
        private String referencedTrunk;
        private String referencedBranch;

        abstract public T build();

        public void setIsHeadFragment(boolean isHeadFragment){
            this.isHeadFragment = isHeadFragment;
        }
        public void setIsTailFragment(boolean isTailFragment){
            this.isTailFragment = isTailFragment;
        }

        public Builder<T> setReferencedBranch(String referencedBranch) {
            this.referencedBranch = referencedBranch;
            return this;
        }

        public Builder<T> setReferencedTrunk(String referencedTrunk) {
            this.referencedTrunk = referencedTrunk;
            return this;
        }

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
