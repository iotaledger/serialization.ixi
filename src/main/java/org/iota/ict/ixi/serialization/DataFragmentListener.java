package org.iota.ict.ixi.serialization;

import org.iota.ict.ixi.serialization.model.StructuredDataFragment;

public interface DataFragmentListener<T> {

    void onData(T dataFragment);
}
