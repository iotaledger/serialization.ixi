package org.iota.ict.ixi.serialization.model;


import org.iota.ict.ixi.serialization.model.md.FieldDescriptor;
import org.iota.ict.ixi.serialization.model.md.FieldType;
import org.iota.ict.ixi.serialization.util.InputValidator;
import org.iota.ict.utils.Trytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StructuredDataFragmentTest {


    @Test
    public void singleValueTest(){
        MetadataFragment metadataFragment = buildMetaDataFragment();
        StructuredDataFragment.Builder builder = new StructuredDataFragment.Builder();
        builder.setMetadata(metadataFragment);
        builder.setValue(0, Trytes.fromAscii("hello"));
        StructuredDataFragment structuredDataFragment = builder.build();
        assertEquals(metadataFragment.getHeadTransaction().hash,structuredDataFragment.getHeadTransaction().extraDataDigest(), "ExtraDataDigest must metadata hash");
        assertEquals("hello", Trytes.toAscii(InputValidator.removeTrailing9(structuredDataFragment.getValue(0))));
    }

    private MetadataFragment buildMetaDataFragment(){
        MetadataFragment.Builder builder = new MetadataFragment.Builder();
        FieldDescriptor descriptor = FieldDescriptor.withAsciiLabel(FieldType.fromTrytes("ASC"),48,"a simple label");
        builder.appendField(descriptor);
        return builder.build();
    }
}
