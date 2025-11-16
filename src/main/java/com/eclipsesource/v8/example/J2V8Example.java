package com.eclipsesource.v8.example;

import com.eclipsesource.v8.V8;

/**
 * Sample program demonstrating J2V8 usage.
 * 
 * This example shows various ways to execute JavaScript code
 * and retrieve different types of results.
 */
//Added a descriptive note for new contributions:This file demonstrates how J2V8 runs Javascript within java
public class J2V8Example {
    
    public static void main(String[] args) {
        // Create a V8 runtime instance
        V8 runtime = V8.createV8Runtime();
        
        try {
            // Example 1: Execute integer script (string concatenation length)
            System.out.println("=== Example 1: String concatenation length ===");
            int result = runtime.executeIntegerScript(""
                + "var hello = 'hello, ';\n"
                + "var world = 'world!';\n"
                + "hello.concat(world).length;\n");
            System.out.println("Result: " + result);
            
            // Example 2: Execute string script
            System.out.println("\n=== Example 2: String result ===");
            String greeting = runtime.executeStringScript(
                "var msg = 'Hello from V8!';\n" +
                "msg;"
            );
            System.out.println("Greeting: " + greeting);
            
            // Example 3: Execute arithmetic
            System.out.println("\n=== Example 3: Arithmetic ===");
            int sum = runtime.executeIntegerScript(
                "var a = 10;\n" +
                "var b = 20;\n" +
                "a + b;"
            );
            System.out.println("10 + 20 = " + sum);
            
            // Example 4: Execute with function
            System.out.println("\n=== Example 4: Function execution ===");
            int factorial = runtime.executeIntegerScript(
                "function factorial(n) {\n" +
                "  if (n <= 1) return 1;\n" +
                "  return n * factorial(n - 1);\n" +
                "}\n" +
                "factorial(5);"
            );
            System.out.println("Factorial of 5: " + factorial);
            
            // Example 5: Execute boolean script
            System.out.println("\n=== Example 5: Boolean result ===");
            boolean isGreater = runtime.executeBooleanScript(
                "var x = 100;\n" +
                "var y = 50;\n" +
                "x > y;"
            );
            System.out.println("Is 100 > 50? " + isGreater);
            
            // Example 6: Execute with double result
            System.out.println("\n=== Example 6: Double result ===");
            double pi = runtime.executeDoubleScript(
                "Math.PI;"
            );
            System.out.println("Value of PI: " + pi);
            
            System.out.println("\n=== All examples completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Error executing JavaScript: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always release the runtime when done
            runtime.release();
            System.out.println("\nV8 runtime released.");
        }
    }
}
