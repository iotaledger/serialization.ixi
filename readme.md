## Serialization.ixi

Serialization.ixi provides a framework to publish data referencing other pieces of data on the Tangle. (one data, many keys)

To do that we define two types of BundleFragment :

The ClassFragment representing the metadata of a DataFragment. The DataFragment to store the data and the references 
to other pieces of data.

The data part of a DataFragment is stored in it's message field. (maybe be on multiple transactions when required).
The references to other DataFragment are stored in address and extradata-digest fields of bundleFragment transactions.

### BundleFragment

A bundle fragment is a portion of [standard IOTA bundle](https://docs.iota.org/docs/getting-started/0.1/introduction/what-is-a-bundle). 
Transactions in a bundle fragment are ordered by their trunk transaction (this characteristic is inherited from IOTA bundle). 
Just like IOTA bundles, bundle fragments have a head transaction and a tail transaction (which can be the same transaction 
when the bundle fragment contains only one transaction). 
We set a specific trit in the tag field to 1 to mark the head transaction of a bundle fragment. 
We set another trit of the tag field to 1 to mark the tail transaction of a bundle fragment. 
Those 2 trits must be 0 in all body transactions.

### ClassFragment

We define a ClassFragment as a Bundle fragment using trit at tag[4] set to 1 to indicate the fragment-head-transaction 
and the trit at tag[3] to indicate the fragment-tail-transaction.
The message of ClassFragment contains 54 meaningful trits:
 - 27 first trits encode the reference-count (integer)
 - 27 next trits encode the data-size (integer)

We define the classHash as the hash of the concatenation of the 54 trits of the message and the address and 
extradata-digest fields of the classFragment.

We define the MetaclassHash as the hash of the integer 54: ISHGAHAOUEPGOUT9BEAAAEWFQFFKUR9W9EHLY9CHPQJPJTJARSAWEHSJYWCKTADZCBKRBKOVMZYDRIXQW
This denotes kind of "genesis" for the classes DAG.

Address field of the head transaction of a ClassFragment is reserved to store the MetaclassHash.  
Extradata-digest and address fields of a ClassFragment store the classHash of referenced fragments.
A reference to an arbitrary transaction is denoted by the NULL_HASH. This allow the creation of "chain" of DataFragment, but also referencing non DataFragment.

### DataFragment

We define a ClassFragment as a Bundle fragment using trit at tag[6] set to 1 to indicate the fragment-head-transaction 
and the trit at tag[5] to indicate the fragment-tail-transaction.

The 27 first trits of the message store the size of the data (redundancy with size stored in the corresponding classFragment)
The following trits of the message store the data.
The address field of the headTransaction is reserved to store the ClassHash of the ClassFragment for this DataFragment.
The transaction hash of referenced data-fragment are stored in next extradata-digest and address fields of bundle fragment transactions.

### Search

As the address field of the head transaction of a DataFragment is reserved to store the classHash : 
we can easily search for all DataFragment for a given classHash (findByAddress).

Searching for all DataFragments referencing a specific DataFragment can also be done by a "findByAddress" or "findByExtraDataDigest".

(same kind of search can be done to query the classes DAG).

### API

    // FACTORY
 
    public ClassFragment buildClassFragment(ClassFragment.Builder builder);
    public DataFragment buildDataFragment(DataFragment.Builder builder);
    /**
     * Build a StructuredDataFragment.Prepared for data.
     * The StructuredDataFragment.Prepared can be used later to insert the dataFragment in Bundle.
     * @return a preparedDataFragment.
     */
    public DataFragment.Prepared prepare(DataFragment.Builder builder);
    /**
     * Build a ClassFragment.Prepared from builder.
     * The ClassFragment.Prepared can be used later to insert the classFragment in a Bundle.
     * @return a prepared ClassFragment.
     */
    public ClassFragment.Prepared prepare(ClassFragment.Builder builder);
    
    //SEARCH
    
    /**
     * @param classHash
     * @return all DataFragment for a given classHash
     */
    public Set<DataFragment> findDataFragmentForClassHash(String classHash);
    
    /**
     * @param classHash the classHash of the searched fragments
     * @param referencedTransactionHash the transaction hash of the dataFragment to be referenced
     * @param index index of the reference
     * @return all DataFragment referencing *referencedTransactionHash* from reference at index *index*
     */
    public Set<DataFragment> findDataFragmentReferencing(String classHash, String referencedTransactionHash, int index);

    /**
     * @return the ClassFragment with head transaction identified by transactionHash,
     * or null if the transaction is not the head of a valid ClassFragment.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     */
    public ClassFragment loadClassFragment(String transactionHash);

    public ClassFragment loadClassFragmentForClassHash(String classHash);
    
    /**
     * @return the DataFragment with head transaction identified by transactionHash,
     * or null if the transaction is not the head of a DataFragment.
     * @throws IllegalArgumentException when transactionHash is not a valid transaction hash (81 trytes)
     */
    public DataFragment loadDataFragment(String transactionHash);

    //ACCESSORS
    
    public byte[] getData(DataFragment dataFragment);

    public byte[] getData(String dataFragmentTransactionHash);

    public byte[] getDataAtIndex(DataFragment dataFragment, int index);
        
    public DataFragment getDataFragment(DataFragment dataFragment, int index);
    
    //EEE
    
    /**
     * Request submission of effect when dataFragment with a particular classHash is received.
     *
     * @param classHash : classHash to watch
     * @param environmentId : environment where effect will be sent
     */
    public void registerDataListener(String classHash, String environmentId);