package org.iota.ict.ixi.serialization;

import org.iota.ict.Ict;
import org.iota.ict.utils.properties.Properties;

public class Main {


    public static void main(String[] args) throws Exception {
       // System.setProperty("log4j.configurationFile","log4j2.xml");
        Properties properties = new Properties();
        Ict ict = new Ict(properties.toFinal());
        ict.getModuleHolder().loadVirtualModule(SerializationModule.class, "Serialization.ixi");
        ict.getModuleHolder().startAllModules();
    }
}
