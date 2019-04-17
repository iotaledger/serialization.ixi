## Serialization.ixi

The purpose of serialization ixi is to provide a simple way to store/exchange structured data on the tangle.

In order to achieve maximal flexibility, but also to favorise interoperability between actors, this IXI provides :
1. services to read structured data from the tangle
2. services to store structured data to the tangle
3. a framework to define the structure of the data

To do this, we define 2 new types of `BundleFragment`.

 - The `StructuredDataFragment` to store the data itself.
 - The `MetadataFragment` to describe the content of the StructuredDataFragment.

For more details regarding the serialization format: please read [specs.md](specs.md)

The document is more focused on public API provided by Serialization.ixi

### Overview

The main api of Serialization.ixi follow the publish/subscribe model where Serialization.ixi is pushing the data to the 
subscriber as soon as it is available.

Another way to use it is to pull the data from the Tangle, but to use it: you need to know the MetadataFragment and 
the transaction hash of the StructuredDataFragment. (this is described at the end of this document)

#### Publisher

To publish data on the tangle, a publisher need to :
1. build a MetadataFragment.
2. compute the ClassHash for it's MetadataFragment. (see [specs.md](specs.md) for details)
3. (optional) attach the MetadataFragment to the Tangle.
4. build a StructuredDataFragment.
5. include the StructuredDataFragment in a Bundle.
6. attach the Bundle to the Tangle.

#### Subscriber

Anyone (i.e. any other ixi) interested by the data published on the Tangle have to do the following to receive the data :

1. Registering one (or more) MetadataFragment of interest
2. Registering one (or more) data listener to be notified when new data comes in.
3. Implementing one (or more) handler to process the data

### API

#### High level API

To simplify the publication process, this IXI provides a high level API making use of annotated java class
to define the Metadata.

Example :

Start by defining a data class with required annotations :

```
public class Sample {

    @SerializableField(index=0, tritLength = 1, label = "isTest", fieldType = FieldType.BOOLEAN)
    public boolean isTest;
    
    @SerializableField(index=1, tritLength = 99, label = "myLabel", fieldType = FieldType.ASCII)
    public boolean myLabel;
    
    @SerializableField(index=2, tritLength = 243, label = "aReference", fieldType = FieldType.HASH)
    public String aReferenceHash;
    
    @SerializableField(index=3, tritLength = 243, label = "aReferenceList", fieldType = FieldType.HASH_LIST)
    public List<String> listOfReferences;
}
```
Notes :
 1. each field have an index. All indexes MUST be different and be continuous. (i.e. defining a field at index 0 ,1 and 3 is illegal because index 2 is missing)
 2. serializable fields are public (to simplify implementation)
 3. a set of field types are predefined : Integer, Decimal, Ascii, Boolean, Hash(243 trits), and their *list* variation: 
 IntegerList, DecimalList, AsciiList, BooleanList and HashList
 
##### Publish/serialize

To publish this data on the Tangle, one can use the following code:

```
Sample myData = new Sample();
myData.isTest = true;
myData.myLabel = "hello world";
...
serializationModule.publish(myData);
```
This code will do all required steps: build a StructuredDataBundleFragment, encapsulate the fragment in a Bundle 
(containing only the structuredDataFragment) and send the data to the tangle.
Note that this simple call will "attach" the Bundle to the genesis, so an alternative API can be used to specify the 
trunk and branch to reference :

```
serializationModule.publish(myData, referencedTrunkHash, referencedBranchHash);
```

Using `publish(...)` will publish a Bundle containing one single StructuredDataFragment and nothing more. 
It can be handy in some cases, but in general: a StructuredDataFragment can be included in an arbitrary Bundle
with other transactions.

Crafting a complete arbitrary Bundle is not in the scope of this ixi, but it provide a simple API to craft a 
*PreparedDataFragment* that can be used later to include a StructuredDataFragment in more complex Bundle.

```
Sample myData = new Sample();
myData.isTest = true;
myData.myLabel = "hello world";
...
StructuredDataFragment.Prepared preparedData = serializationModule.prepare(myData);
...
BundleBuilder bundleBuilder = new BundleBuilder();
...

List<TransactionBuilder> transactionBuilders = preparedData.fromTailToHead();
bundleBuilder.append(transactionBuilders);
...
Bundle bundle = bundleBuilder.build();
```

##### Subscribe/deserialize

Again, this IXI provides a very simple API to register a listener for a particular annotated class:
```
serializationModule.registerDataListener(Class<T> aTangleSerializableClass,
                                         T -> {
                                              //process the data
                                         });
```
Using this API will take care of registering the required MetadataFragment and filtering fragment of the given class.

An alternative to specify a more complex filter:
```
serializationModule.registerDataListener(
                                         T -> {
                                             //return true when fragment must be processed by the listener
                                         },
                                         T -> {
                                              //process the data
                                         });
```

#### Low level API

Instead of using annotated java classes to use Serialization.ixi,
one can manipulate MetadataFragment and StructuredDataFragment directly.

Going this way requires of course a 'in depth' understanding of the underlying architecture 
(read [specs.md](specs.md) for details) 

##### MetadataFragment

The MetadataFragment.Builder class is the entry point to create MetadataFragment.
Building a MetadataFragment consist essentially in appending FieldDescriptor to the MetadataFragment.
A fieldDescriptor describe a serialized field and match exactly the `@SerializableField` annotation presented earlier.

```
FieldDescriptor name = FieldDescriptor.withAsciiLabel(FieldType.TYPE_ASCII,243,"name");
FieldDescriptor age = FieldDescriptor.withAsciiLabel(FieldType.TYPE_INTEGER,7,"age");
FieldDescriptor isMale = FieldDescriptor.withAsciiLabel(FieldType.TYPE_BOOLEAN,1,"isMale");
MetadataFragment metadatafragment =  new MetadataFragment.Builder()
                                            .appendField(name)
                                            .appendField(age)
                                            .appendField(isMale)
                                            .build();
```

##### Build a StructuredDataFragment

Similarly, a StructuredDataFragment is build with a `StructureDataFragment.Builder`.
To build a StructuredDataFragment by hand, you need to corresponding MetadataFragment, and then you just have to
assign values to fields (fields being identified by their index). Note that values MUST be byte[] (one trit encoded in one byte)

```
StructuredDataFragment dataFragment = new StructuredDataFragment.Builder()
                                            .setMetadata(metadataFragment)
                                            .setValue(0, Trytes.fromAscii("my name"))
                                            .setValue(1, Trytes.fromNumber(BigInteger.valueOf(47),2))
                                            .setBooleanValue(2, true)
                                            .build();
```

##### Receive a StructuredDataFragment

To receive dataFragment: 
1. register the the metadataFragment you need
2. register a dataListener for the StructuredDataFragment class and it's associated DataFragmentFilter

```
MetadataFragment metadataFragment = ...;
serializationModule.registerMetadata(metadataFragment);
final String classHash = metadataFragment.getClassHash();
DataFragmentFilter matcher = new DataFragmentFilter() {
    @Override
    public boolean match(StructuredDataFragment dataFragment) {
        return dataFragment.getClassHash().equals(classHash);
    }
};

DataListener<StructuredDataFragment> wrappedListener = new DataListener<StructuredDataFragment>() {
    @Override
    public void onData(StructuredDataFragment dataFragment) {
        //process dataFragment
    }
};

serializationModule.registerDataListener(matcher, wrappedListener);
```

##### Read a StructuredDataFragment

Fields of a StructuredDataFragment are identified by index and different getter are available
to read a typed value, or a list of values.
There is also a generic getter to read any field as trits (encoded in byte[])

```
String ascii = structuredDataFragment.getAsciiValue(0);
Integer age = structuredDataFragment.getIntegerValue(1);
boolean isMale = structuredDataFragment.getBooleanValue(2);
List<byte[]> values = structuredDataFragment.getListValues(3);
```

### Pull data API

As an alternative to the DataListener, as soon as you know the transaction hash of the bundle-fragment-head
and the corresponding meta-data, you can pull data from the Tangle by using one of those methods:
```
MyClass myPulledData = serializationModule.loadData(transactionHash, MyClass.class);
```
or 
```
StructuredData myPulledData = serializationModule.loadData(transactionHash, metadataFragment);
```
This is useful when receiving a dataFragment where other fragments are referenced in a HASH (or HASH_LIST) field.
