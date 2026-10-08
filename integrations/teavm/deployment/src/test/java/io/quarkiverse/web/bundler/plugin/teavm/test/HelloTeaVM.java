package io.quarkiverse.web.bundler.plugin.teavm.test;

import org.teavm.jso.JSExport;

public class HelloTeaVM {

    @JSExport
    public static String greet(String name) {
        return "Hello from TeaVM, " + name;
    }

    public static void main(String[] args) {
        System.out.println("TeaVM main started");
    }
}
