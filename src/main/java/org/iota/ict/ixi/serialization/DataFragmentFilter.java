package org.iota.ict.ixi.serialization;

import org.iota.ict.ixi.serialization.model.StructuredDataFragment;

public interface DataFragmentFilter {

    boolean match(StructuredDataFragment dataFragment);

}
