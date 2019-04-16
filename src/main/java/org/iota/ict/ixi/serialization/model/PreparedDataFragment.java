package org.iota.ict.ixi.serialization.model;

import org.iota.ict.model.transaction.TransactionBuilder;

import java.util.List;

public class PreparedDataFragment {

    private StructuredDataFragment.Builder builder;

    public PreparedDataFragment(StructuredDataFragment.Builder builder) {
        this.builder = builder;
    }

    public List<TransactionBuilder> fromTailToHead(){
        return builder.getTailToHead();
    }
}
