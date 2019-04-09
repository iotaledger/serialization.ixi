package org.iota.ict.ixi.serialization;

import org.iota.ict.Ict;
import org.iota.ict.utils.properties.Properties;

import java.util.Scanner;

public class Main {


    public static void main(String[] args) throws Exception {
        System.setProperty("log4j.configurationFile","log4j2.xml");
        Properties properties = new Properties();
        Ict ict = new Ict(properties.toFinal());
        ict.getModuleHolder().loadVirtualModule(SerializationModule.class, "Serialization.ixi");
        ict.getModuleHolder().startAllModules();
        SerializationModule serializationModule = (SerializationModule) ict.getModuleHolder().getModule("virtual/Serialization.ixi");
        Scanner scanner = new Scanner(System.in);
        String input;
        System.out.print("ict > ");
        while(!(input = scanner.next()).equalsIgnoreCase("q")){

            if("q".equalsIgnoreCase(input)){
                break;
            }
            System.out.println(input);
            System.out.println("ict > ");
        }
    }
}
