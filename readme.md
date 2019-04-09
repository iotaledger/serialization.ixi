## Serialization.ixi

The purpose of serialization ixi is to provide a simple way to store/exchange structured data on the tangle.

In order to achieve maximal flexibility, but also to favorise interoperability between actors, this IXI provides :
1. services to read structured data from the tangle
2. services to store structured data to the tangle
3. a framework to define the structure of the data

To do this, we define 2 new types of bundle fragments.

 - The structured-data fragment to store the data itself.
 - The meta-data fragment to describe the structured-data fragment.

### Bundle fragment

A bundle fragment is a portion of [standard IOTA bundle](https://docs.iota.org/docs/getting-started/0.1/introduction/what-is-a-bundle). 
Transactions in a bundle fragment are ordered by their trunk transaction (this characteristic is inherited from IOTA bundle). 
Just like IOTA bundles, bundle fragments have a head transaction and a tail transaction (which can be the same transaction when the bundle fragment contains only one transaction). 
We set a specific trit in the tag field to 1 to mark the head transaction of a bundle fragment. 
We set another trit of the tag field to 1 to mark the tail transaction of a bundle fragment. Those 2 trits must be 0 in all body transactions.

We define the **BundleFragmentHash** as the hash (curl, 27 round) of the concatenation of all signature_of_message_fragment field (ordered from head to tail).

### meta-data bundle fragment

A meta-data bundle fragment is a bundle-fragment using trit at tag[4] set to 1 to indicate the bundle fragment head transaction and the trit at tag[3] to indicate the tail transaction. 

The signatureMessageFragment of a meta-data bundle fragment contains a non-empty sequence of *field descriptor*.

A *field descriptor* is composed of 3 components of fixed size:

- The <u>type</u> component (6 trits) indicates the cardinality and optionally the way to interpret the value  
    - the first trit is called the cardinality trit. 
        - A cardinality trit 1 indicates that the field is a "single value" 
        - A cardinality trit -1 indicates that the field is a "multiple value" 
        - A cardinality trit 0 is illegal 
    - the 5 following trits indicates how to interpret the (mutiple-)value(s).
        - 0,0,0,0,0 indicates an unspecified field (i.e. just 'trits')
        - 1,0,0,0,0 denotes an Integer 
        - 0,1,0,0,0 denotes an Boolean 
        - 0,0,1,0,0 denotes an Float 
        - 0,0,0,1,0 denotes a TransactionHash 
        - 0,0,0,0,1 denotes an encode ascii value 
        - (other trit sequences are 'reserved' for future use)
- The <u>size</u> component (X trits) indicates either :
    - the size of the field in trits (when cardinality is "single value")
    - the size of one element of the list (when cardinality is "multiple values")
    Note that a Boolean field should have a size of 1 and a TransactionHash field should have a size of 243.
- The <u>label</u> component (Y trits) is a human readable description  (ascii encoded) of the field.

If required, the sequence of field descriptor can be encoded in multiple transactions.

Order of field descriptor define the order of the field values that will be found in the structured data fragment.

The BundleFragmentHash of a Metadata fragment is called the **ClassHash**.

A MetadataFragment don't necessarily need to be published on the tangle. The publisher and subscriber(s) may just have 
a static knowledge of the class being serialized (i.e. FieldDescriptors sequence) to be able to successfully submit/receive structured data. 

### structured-data bundle fragment

A structured-data bundle fragment is a bundle-fragment using trit at tag[6] set to 1 to indicate the bundle fragment 
head transaction and the trit at tag[5] to indicate the tail transaction. 

Extra-data digest field of each transaction in a StructuredDataFragment contains the ClassHash of the corresponding MetadataFragment.

The signature_or_message fragment contains the data. Values are of course ordered and sized according field descriptor sequence of the meta-data fragment.

//TODO : update picture to new design !
![bundles](https://github.com/iotaledger/serialization.ixi/blob/master/docs/serialization.png?raw=true)

<small>*Metadata and StructuredData are represented in 2 different bundles here, but they could be in the same bundle.*</small>

### API

#### Publisher API (serialize)

//TODO

#### Subscriber API (deserialize)

//WIP

A subscriber can register a listener to receive StructuredDataFragment of interest.

DataFragment of interest are determined by a Predicate. The Predicate API is fairly simple and contains only one `match` method.

For instance, to receive all StructuredDataFragment with a given classHash, the subscriber must call :

```
registerDataListener(new Predicate{
        public boolean match(StructuredDataFragment fragment){
            return fragment.headTransaction.extraDigest().equals(myClassHash);
        }
})
``` 

```
MetadataFragment buildMetadataFragment(MetadataFragmentBuilder builder);

StructuredDataFragment buildStructuredFragment(StructuredFragmentBuilder builder);

/**
 *  @return the MetadataFragment with bundleHeadTransaction identified by bundleHeadTransactionHash, 
 *           or null if the transaction is not the head of a valid MetadataFragment. 
 */
MetadataFragment loadMetadata(String bundleHeadTransactionHash);

/**
 *  @return the StructuredDataFragment with bundleHeadTransaction identified by bundleHeadTransactionHash, 
 *          or null if the transaction is not the head of a valid StructuredDataFragment. 
 *  @throws UnknownMetadataException when referenced metadata is unknown or invalid
 */
StructuredDataFragment loadStructuredData(String bundleHeadTransactionHash);

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