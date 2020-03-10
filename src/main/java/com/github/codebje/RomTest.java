package com.github.codebje;

import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.*;
import cucumber.runtime.java.JavaBackend;

import java.net.URL;
import java.util.*;

public class RomTest {

    public static void main(String[] args) throws Exception {

        List<String> opts = new ArrayList<>();
        opts.add("-g");
        opts.add("com.github.codebje.steps");
        opts.addAll(Arrays.asList(args));

        final RuntimeOptions runtimeOptions = new RuntimeOptions(opts);
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final ResourceLoader resourceLoader = new MultiLoader(classLoader);
        final ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        final Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);

        runtime.run();

    }

}
