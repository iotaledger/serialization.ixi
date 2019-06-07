package org.iota.ict.ixi.serialization;

import org.iota.ict.eee.call.EEEFunction;
import org.iota.ict.eee.call.FunctionEnvironment;
import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.serialization.model.ClassFragment;
import org.iota.ict.ixi.serialization.model.DataFragment;
import org.iota.ict.utils.Trytes;

import java.util.ArrayList;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class EEEFunctions {

    private final SerializationModule serializationModule;
    private final Ixi ixi;

    /**
     * This function will auto-discrimate between attribute (integer<space>attributeName) or classHash (trytes)
     * Input  : <classname>;<attribute_size attributeName>|<referenced_classhash>;[<attribute_size attributeName>|<referenced_classhash>;]*
     * Output : <ClassHash trytes>
     *
     * Example : to compute the classHash of a class named "MY9CLASS" with 2 attributes:
     *           named "A0" (size 9) and "A1" (variable size)
     *           and 2 references :
     *           first one is referencing a dataFragment with classHash REF9CLASS...VGHGYU  (81 trytes)
     *           second is referencing a random transaction (not a dataFragment, or referenced classHash unknown)
     *
     *           The request will look like this :
     *
     *           MY9CLASS;A0 9;A1 0;REF9CLASS...VGHGYU;9999...999
     *
     */
    private final EEEFunction computeClassHash = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "computeClassHash"));

    /*
     * <ATTRIB_OR_REFERENCE> is a space separated string composed of 3 tokens.
     *                       token 0: 'A' or 'R', to indicate if it is an attribute or a reference
     *                       token 1: index, the index of the attribute or reference
     *                       token 2: trytes, either the data or the 81 trytes of the reference)
     * Input  : <classHash>;<trunk_hash>;<branch_hash>;<ATTRIB_OR_REFERENCE>[;<ATTRIB_OR_REFERENCE>]*
     * Output : <fragment_head_hash>
     *
     * Example : to publish a DataFragment of a class with classHash ADCD...XYZ (81 trytes),
     *            and attribute at index 3 having the value "MY9VALUE"
     *            and reference at index 0 referencing dataFragment with tx_hash "MY9DATA9TX9HASH9...XYZ"
     *
     *            The request will look like this :
     *
     *            ADCD...XYZ;A 3 MY9VALUE;R 0 MY9DATA9TX9HASH9...XYZ
     */
    private final EEEFunction publishDataFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "publishDataFragment"));

    /**
     * Input  : <classname>;<trunk_hash>;<branch_hash>;<attribute_size attributeName>|<referenced_classhash>;[<attribute_size attributeName>|<referenced_classhash>;]*
     * Output : <fragment_head_hash>;<class_hash>
     */
    private final EEEFunction publishClassFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "publishClassFragment"));

    /**
     * <ATTRIB_OR_REFERENCE> is a space separated string composed of 3 tokens.
     *                       token 0: 'A' or 'R', to indicate if it is an attribute or a reference
     *                       token 1: index, the index of the attribute or reference
     *                       token 2: trytes, either the data or the 81 trytes of the reference)
     * Input  : <classHash>;<trunk_hash>;<branch_hash>;<ATTRIB_OR_REFERENCE>[;<ATTRIB_OR_REFERENCE>]*
     * Output : <fragment_head_hash>
     */
    private final EEEFunction prepareDataFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "prepareDataFragment"));

    /**
     * Input  : <classname>;<trunk_hash>;<branch_hash>;<attribute_size attributeName>|<referenced_classhash>;[<attribute_size attributeName>|<referenced_classhash>;]*
     * Output : <fragment_head_hash>
     */
    private final EEEFunction prepareClassFragment = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "prepareClassFragment"));

    /**
     * Input  : <data_fragment_head_transaction_hash>;<index_of_the_attribute>
     * Output : <data as trytes>
     */
    private final EEEFunction getAttribute = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "getAttribute"));

    /**
     * Input  : <data_fragment_head_transaction_hash>;<index_of_the_reference>;<index_of_the_attribute>
     * Output : <data of the referenced fragment (as trytes)>
     */
    private final EEEFunction getReferencedAttribute = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "getReferencedAttribute"));

    /**
     * Input  : <data_fragment_head_transaction_hash>;<index_of_the_reference>
     * Output : <transactionHash referenced at index>
     */
    private final EEEFunction getReference = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "getReference"));

    /**
     * Input  : <classHash>
     * Output : <transaction hash of dataFragment>*
     */
    private final EEEFunction findFragmentsForClass = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "findFragmentsForClass"));

    /**
     * Input  : <referenced transaction hash>[;<index>;<value>]
     * Output : <transaction hash of dataFragment with at least one reference to referenced transaction hash>*
     */
    private final EEEFunction findReferencing = new EEEFunction(new FunctionEnvironment("Serialization.ixi", "findReferencing"));


    EEEFunctions(SerializationModule serializationModule, Ixi ixi){
        this.serializationModule = serializationModule;
        this.ixi = ixi;

        ixi.addListener(computeClassHash);
        ixi.addListener(publishDataFragment);
        ixi.addListener(publishClassFragment);
        ixi.addListener(prepareDataFragment);
        ixi.addListener(prepareClassFragment);
        ixi.addListener(getAttribute);
        ixi.addListener(getReference);
        ixi.addListener(getReferencedAttribute);
        ixi.addListener(findFragmentsForClass);
        ixi.addListener(findReferencing);

        new EEERequestHandler(computeClassHash, this::processComputeClassHashRequest).start();
        new EEERequestHandler(publishDataFragment, this::processPublishDataRequest).start();
        new EEERequestHandler(publishClassFragment, this::processPublishClassRequest).start();
        new EEERequestHandler(prepareDataFragment, this::processPrepareDataRequest).start();
        new EEERequestHandler(prepareClassFragment, this::processPrepareClassRequest).start();
        new EEERequestHandler(getAttribute, this::processGetAttributeRequest).start();
        new EEERequestHandler(getReferencedAttribute, this::processGetReferencedAttributeRequest).start();
        new EEERequestHandler(getReference, this::processGetReferenceRequest).start();
        new EEERequestHandler(findFragmentsForClass, this::processFindFragmentsForClassRequest).start();
        new EEERequestHandler(findReferencing, this::processFindReferencingRequest).start();
    }

    public static void init(SerializationModule serializationModule, Ixi ixi) {
        new EEEFunctions(serializationModule, ixi);
    }



    private void processComputeClassHashRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        ClassFragment.Builder builder = new ClassFragment.Builder(split[0]);
        int i = 1;
        while(i<split.length){
            appendAttributeOrRef(builder, split[i]);
            i++;
        }
        String ret = builder.build().getClassHash();
        request.submitReturn(ixi, ret);
    }

    private void processPublishDataRequest(EEEFunction.Request request) {
        DataFragment.Builder builder = dataFragmentBuilderFromRequest(request);
        if(builder==null) {
            request.submitReturn(ixi,"");
            return;
        }
        DataFragment fragment = serializationModule.publishBundleFragment(builder);
        request.submitReturn(ixi, fragment.getHeadTransaction().hash);
    }

    private void processPublishClassRequest(EEEFunction.Request request) {
        ClassFragment.Builder builder = classFragmentBuilderFromRequest(request);
        if(builder==null) request.submitReturn(ixi,"");
        ClassFragment fragment = serializationModule.publishBundleFragment(builder);
        request.submitReturn(ixi, fragment.getHeadTransaction().hash+";"+fragment.getClassHash());
    }

    private ClassFragment.Builder classFragmentBuilderFromRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        ClassFragment.Builder builder = (ClassFragment.Builder) new ClassFragment.Builder(split[0])
                .setReferencedTrunk(split[1])
                .setReferencedBranch(split[2]);
        if (split.length > 3) {
            int i = 3;
            while (i < split.length) {
                String s = split[i];
                appendAttributeOrRef(builder, s);
                i++;
            }
        }
        return builder;
    }

    private void appendAttributeOrRef(ClassFragment.Builder builder, String s) {
        if (Trytes.NULL_HASH.equals(s)) {
            builder.addReferencedClasshash(s);
        } else {
            if(s.indexOf(" ")>-1){
                String[] split = s.split(" ");
                builder.addAttribute(Integer.valueOf(split[0]), split[1]);
            }else{
                builder.addReferencedClasshash(s);
            }
        }
    }

    private void processPrepareDataRequest(EEEFunction.Request request) {
        DataFragment.Builder builder = dataFragmentBuilderFromRequest(request);
        if(builder==null){
            request.submitReturn(ixi, "");
            return;
        }
        DataFragment fragment = serializationModule.prepareBundleFragment(builder);
        request.submitReturn(ixi, fragment.getHeadTransaction().hash);
    }

    private DataFragment.Builder dataFragmentBuilderFromRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String classHash = split[0];
        if(classHash==null || classHash.equals(Trytes.NULL_HASH)) return null;
        ClassFragment classFragment = serializationModule.loadClassFragmentForClassHash(classHash);
        if(classFragment==null) return null;
        DataFragment.Builder builder = new DataFragment.Builder(classFragment);
        builder.setReferencedTrunk(split[1]);
        builder.setReferencedBranch(split[2]);
        if (split.length > 3) {
            int i = 3;
            while (i < split.length) {
                String[] tokens = split[i].split(" ");
                if(tokens.length!=3)return null;
                int index = Integer.valueOf(tokens[1]);
                String trytes = tokens[2];
                if("A".equals(tokens[0])){
                    builder.setAttribute(index,trytes);
                }else if("R".equals(tokens[0])){
                    builder.setReference(index, trytes);
                }
                i++;
            }
        }
        return builder;
    }

    private void processPrepareClassRequest(EEEFunction.Request request) {
        ClassFragment.Builder builder = classFragmentBuilderFromRequest(request);
        ClassFragment fragment = serializationModule.prepareBundleFragment(builder);
        request.submitReturn(ixi, fragment.getHeadTransaction().hash+";"+fragment.getClassHash());
    }

    private void processGetAttributeRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String hash = split[0];
        int index = Integer.valueOf(split[1]);
        DataFragment fragment = serializationModule.loadDataFragment(hash);
        if (fragment != null) {
            request.submitReturn(ixi, fragment.getAttributeAsTryte(index));
        } else {
            request.submitReturn(ixi, "");
        }
    }

    private void processGetReferencedAttributeRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String hash = split[0];
        int index = Integer.valueOf(split[1]);
        int attributeIndex = Integer.valueOf(split[2]);
        DataFragment fragment = serializationModule.loadDataFragment(hash);
        if (fragment != null) {
            fragment = serializationModule.loadDataFragment(fragment.getReference(index));
            if(fragment!=null) {
                request.submitReturn(ixi, fragment.getAttributeAsTryte(attributeIndex));
                return;
            }
        }
        request.submitReturn(ixi, "");
    }

    private void processGetReferenceRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String hash = split[0];
        int index = Integer.valueOf(split[1]);
        DataFragment fragment = serializationModule.loadDataFragment(hash);
        if (fragment != null) {
            String ret = fragment.getReference(index);
            request.submitReturn(ixi, ret);
        } else {
            request.submitReturn(ixi, "");
        }
    }

    private void processFindFragmentsForClassRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String classHash = split[0];
        Set<DataFragment> fragments = serializationModule.findDataFragmentForClassHash(classHash);
        returnFragmentSet(request, fragments);
    }

    private void processFindReferencingRequest(EEEFunction.Request request) {
        String argument = request.argument;
        String[] split = argument.split(";");
        String referencedTransactionHash = split[0];
        DataFragment.Filter filter = null;
        if(split.length>1){
            int i = 1;
            while(i+1<split.length){
                int fieldIndex = Integer.valueOf(split[i]);
                String fieldValue = split[i+1];
                i +=2;

                DataFragment.Filter itemFilter =
                        dataFragment -> dataFragment.getAttributeAsTryte(fieldIndex).startsWith(fieldValue);

                if(filter==null){
                    filter = itemFilter;
                }else{
                    filter = DataFragment.Filter.and(filter,itemFilter);
                }
            }
        }
        Set<DataFragment> fragments = serializationModule.findDataFragmentReferencing(referencedTransactionHash, filter);
        returnFragmentSet(request, fragments);
    }

    private void returnFragmentSet(EEEFunction.Request request, Set<DataFragment> fragments) {
        if (fragments.size() == 0) {
            request.submitReturn(ixi, "");
        } else {
            ArrayList<String> ret = new ArrayList<>(fragments.size());
            for (DataFragment fragment : fragments) {
                ret.add(fragment.getHeadTransaction().hash);
            }
            request.submitReturn(ixi, String.join(";", ret));
        }
    }

    private class EEERequestHandler extends Thread {

        final EEEHandler handler;
        final EEEFunction eeeFunction;

        public EEERequestHandler(EEEFunction eeeFunction, EEEHandler handler) {
            setName("EEEFunction-"+eeeFunction.getEnvironment().toString());
            this.handler = handler;
            this.eeeFunction = eeeFunction;
        }

        @Override
        public void run() {
            while (serializationModule.isRunning()) {
                try {
                    handler.handleRequest(eeeFunction.requestQueue.take());
                } catch (InterruptedException e) {
                    if (serializationModule.isRunning()) throw new RuntimeException(e);
                }
            }
        }
    }

    interface EEEHandler {
        void handleRequest(EEEFunction.Request request);
    }
}
