## Serialization.ixi

WIP

The purpose of serialization ixi is to provide a simple way to store/exchange structured data on the tangle.

In order to achieve maximal flexibility, but also to favorise interoperability between actors, this IXI provides :
1. services to read structured data from the tangle
2. services to store structured data to the tangle
3. a framework to define the structure of the data

To do this, we define 2 new types of bundle fragments.

 - The structured-data fragment to store the data itself.
 - The meta-data fragment to describe the structured-data fragment.

### Bundle fragment

A bundle fragment is a portion of [standard IOTA bundle](https://docs.iota.org/docs/getting-started/0.1/introduction/what-is-a-bundle). Transactions in a bundle fragment are ordered by their trunk transaction (this characteristic is inherited from IOTA bundle). Just like IOTA bundles, bundle fragments have a head transaction and a tail transaction (which can be the same transaction when the bundle fragment contains only one transaction). We set a specific trit in the tag field to 1 to mark the head transaction of a bundle fragment. We set another trit of the tag field to 1 to mark the tail transaction of a bundle fragment. Those 2 trits must be 0 in all body transactions.

### meta-data bundle fragment

A meta-data bundle fragment is a bundle-fragment using trit at tag[4] set to 1 to indicate the bundle fragment head transaction and the trit at tag[3] to indicate the tail transaction. 

The signatureMessageFragment of a meta-data bundle fragment contains a non-empty sequence of *field descriptor*.

A *field descriptor* is composed of 3 components of fixed size:

- The <u>type</u> component (9 trits) indicates by a magic number the type of the field. (examples : integer, float, hash, ascii, ... tbd)
- The <u>size</u> component (X trits) indicates the size of the field in trits
- The <u>label</u> component (Y trits) is a human readable description  (ascii encoded) of the field.

 If required, the sequence of field descriptor can be encoded in multiple transactions.


Order of field descriptor define the order of the field values that will be found in the structured data fragment.


### structured-data bundle fragment

A structured-data bundle fragment is a bundle-fragment using trit at tag[6] set to 1 to indicate the bundle fragment head transaction and the trit at tag[5] to indicate the tail transaction. 

Extra-data digest field of the head transaction is a pointer to the head transaction of the corresponding meta-data fragment.
The signature_or_message fragment contains the data. values are of course ordered and sized according field descriptor sequence of the meta-data fragment.

![bundles](https://github.com/iotaledger/serialization.ixi/blob/master/docs/serialization.png?raw=true)

<small>*Metadata and StructuredData are represented in 2 different bundles here, but they could be in the same bundle.*</small>

### API


```
MetadataFragment buildMetadataFragment(MetadataFragmentBuilder builder);

StructuredDataFragment buildStructuredFragment(StructuredFragmentBuilder builder);

/**
 *  @return the MetadataFragment including transaction with transactionHash, 
 *           or null if the transaction is not part of a valid MetadataFragment. 
 */
MetadataFragment loadMetadata(String transactionHash);

/**
 *  @return the StructuredDataFragment including transaction with transactionHash, 
 *          or null if the transaction is not part of a valid StructuredDataFragment. 
 *  @throws UnknownMetadataException when referenced metadata is unknown or invalid
 */
StructuredDataFragment loadStructuredData(String transactionHash);

/**
 * @return the value in trytes of key at index
 * @throws IndexOutBoundException when index is invalid
 */
String getTrytesForKeyAtIndex(StructuredDataFragment dataFragment, int index)

/**
 * @return the value of key at index
 * @throws IndexOutBoundException when index is invalid
 */
Object getValueAtIndex(StructuredDataFragment dataFragment, int index)
```