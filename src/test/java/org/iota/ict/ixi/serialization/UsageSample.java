package org.iota.ict.ixi.serialization;

import org.iota.ict.Ict;
import org.iota.ict.eee.call.EEEFunctionCaller;
import org.iota.ict.eee.call.EEEFunctionCallerImplementation;
import org.iota.ict.eee.call.FunctionEnvironment;
import org.iota.ict.ixi.TestUtils;
import org.iota.ict.ixi.serialization.model.ClassFragment;
import org.iota.ict.ixi.serialization.model.DataFragment;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.model.transaction.TransactionBuilder;
import org.iota.ict.utils.Trytes;
import org.iota.ict.utils.properties.EditableProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UsageSample {

    private static SerializationModule serializationModule;
    private static Ict ict;

    @BeforeAll
    public static void startIct() throws Exception {
        //System.setProperty("log4j.configurationFile","log4j2.xml");
        EditableProperties properties = new EditableProperties().host("localhost").port(2487).minForwardDelay(0).maxForwardDelay(10).guiEnabled(false);
        ict = new Ict(properties.toFinal());
        serializationModule = (SerializationModule) ict.getModuleHolder().loadVirtualModule(SerializationModule.class, "Serialization.ixi");
        ict.getModuleHolder().startAllModules();
    }

    @AfterAll
    public static void terminate(){
        serializationModule.terminate();
    }

    @Test
    public void demoJavaAPI(){
        //First, let's publish a random transaction
        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.signatureFragments = Trytes.padRight("THE9REFERENCED9TRANSACTION", Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        Transaction theTargetTransaction = transactionBuilder.build();
        ict.submit(theTargetTransaction);

        //Keep the tx hash
        String theTargetTransactionHash = theTargetTransaction.hash;

        //=================================================
        //Now let's build and publish a few DataFragments
        //=================================================

        //To build a DataFragment, we need a ClassFragment :
        //(in this example, our class have a reference to a random transaction and an attribute name 'ATTRIB' with a length of 33 trytes)
        ClassFragment.Builder myClassFragmentBuilder = new ClassFragment.Builder("JUST9A9CLASS9NAME")
                .addReferencedClasshash(Trytes.NULL_HASH)
                .addAttribute(33,"ATTRIB");

        ClassFragment myClassFragment = serializationModule.publishBundleFragment(myClassFragmentBuilder);

        //now that we have our classFragment, let's build and publish a few DataFragments

        //the first dataFragment will reference the target transaction that we published earlier.
        DataFragment.Builder dataFragmentBuilder = new DataFragment.Builder(myClassFragment)
                .setReference(0, theTargetTransactionHash)
                .setAttribute(0,"VALUE9A");
        DataFragment firstDataFragment = serializationModule.publishBundleFragment(dataFragmentBuilder);

        //now we publish a second DataFragment of the same class, referencing our target tx, but with a different value for ATTRIB
        dataFragmentBuilder = new DataFragment.Builder(myClassFragment)
                .setReference(0, theTargetTransactionHash)
                .setAttribute(0,"VALUE9B");
        DataFragment secondDataFragment = serializationModule.publishBundleFragment(dataFragmentBuilder);

        //now we publish a third DataFragment of the same class, but referencing a random tx instead of our target
        dataFragmentBuilder = new DataFragment.Builder(myClassFragment)
                .setReference(0, TestUtils.randomHash())
                .setAttribute(0,"VALUE9A");
        DataFragment thirdDataFragment = serializationModule.publishBundleFragment(dataFragmentBuilder);

        //in a test context: make a small pause to ensure that transactions are propagated.
        safeSleep(100);

        //=================================================
        //Our data is now published, let's search for it
        //=================================================

        //search for allDataFragments referencing our target. (we expect 2)
        Set<DataFragment> allDataFragmentReferencingTarget = serializationModule.findDataFragmentReferencing(theTargetTransactionHash, null);
        assertEquals(2, allDataFragmentReferencingTarget.size());


        //search for allDataFragments referencing our target with ATTRIB value "VALUE9A". (we expect 1, and it should be firstDataFragment)
        Set<DataFragment> allDataFragmentReferencingTargetWithAttribVALUE9A = serializationModule.findDataFragmentReferencing(theTargetTransactionHash, new DataFragment.Filter() {
            @Override
            public boolean match(DataFragment dataFragment) {
                return dataFragment.getAttributeAsTryte(0).equals(Trytes.padRight("VALUE9A",33));
            }
        });
        assertEquals(1, allDataFragmentReferencingTargetWithAttribVALUE9A.size());
        assertEquals(firstDataFragment.getHeadTransaction().hash,allDataFragmentReferencingTargetWithAttribVALUE9A.iterator().next().getHeadTransaction().hash);

    }



    @Test
    public void demoEEE_API(){
        //First, let's publish a random transaction
        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.signatureFragments = Trytes.padRight("THE9REFERENCED9TRANSACTION", Transaction.Field.SIGNATURE_FRAGMENTS.tryteLength);
        Transaction theTargetTransaction = transactionBuilder.build();
        ict.submit(theTargetTransaction);

        //Keep the tx hash
        String theTargetTransactionHash = theTargetTransaction.hash;

        //=================================================
        //Now let's build and publish a few DataFragments
        //=================================================

        EEEFunctionCallerImplementation caller = new EEEFunctionCallerImplementation(ict);

        //To build a DataFragment, we need a ClassFragment :
        //(in this example, our class have a reference to a random transaction and an attribute name 'ATTRIB' with a length of 33 trytes)
        String response = caller.call(new FunctionEnvironment("Serialization.ixi","publishClassFragment"),
                "JUST9ANOTHER9CLASS9NAME;"+Trytes.NULL_HASH+";"+Trytes.NULL_HASH+";33 ATTRIB;"+Trytes.NULL_HASH,
                250);

        String myClassFragmentClassHash = response.split(";")[1];

            safeSleep(300);
        //now that we have our classFragment, let's build and publish a few DataFragments

        //the first dataFragment will reference the target transaction that we published earlier.

        response = caller.call(new FunctionEnvironment("Serialization.ixi","publishDataFragment"),
                myClassFragmentClassHash+";"+Trytes.NULL_HASH+";"+Trytes.NULL_HASH+";A 0 VALUE9A;R 0 "+theTargetTransactionHash,
                250);
        String firstFragmentHash = response;

        //now we publish a second DataFragment of the same class, referencing our target tx, but with a different value for ATTRIB
        response = caller.call(new FunctionEnvironment("Serialization.ixi","publishDataFragment"),
                myClassFragmentClassHash+";"+Trytes.NULL_HASH+";"+Trytes.NULL_HASH+";A 0 VALUE9B;R 0 "+theTargetTransactionHash,
                250);
        String secondFragmentHash = response;

        //now we publish a third DataFragment of the same class, but referencing a random tx instead of our target
        response = caller.call(new FunctionEnvironment("Serialization.ixi","publishDataFragment"),
                myClassFragmentClassHash+";"+Trytes.NULL_HASH+";"+Trytes.NULL_HASH+";A 0 VALUE9B;R 0 "+TestUtils.randomHash(),
                250);
        String thirdFragmentHash = response;

        //in a test context: make a small pause to ensure that transactions are propagated.
        safeSleep(1000);

        //=================================================
        //Our data is now published, let's search for it
        //=================================================

        //search for allDataFragments referencing our target. (we expect 2)
        response = caller.call(new FunctionEnvironment("Serialization.ixi","findReferencing"),
                theTargetTransactionHash,
                250);

        assertEquals(2, response.split(";").length);
        assertTrue(response.contains(firstFragmentHash));
        assertTrue(response.contains(secondFragmentHash));

        //search for allDataFragments referencing our target with ATTRIB value "VALUE9A". (we expect 1, and it should be firstDataFragment)
        response = caller.call(new FunctionEnvironment("Serialization.ixi","findReferencing"),
                theTargetTransactionHash+";0;VALUE9A",
                250);

        assertEquals(1, response.split(";").length);
        assertEquals(firstFragmentHash,response);

    }


    private void safeSleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //ignore
        }
    }
}
