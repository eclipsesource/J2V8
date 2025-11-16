import com.eclipsesource.v8.*;

public class J2V8ConstructorExample {
    public static void main(String[] args) {
        // Create a new V8 runtime
        V8 v8 = V8.createV8Runtime();

        try {
            // Define a JS constructor function
            v8.executeVoidScript(
                "function Person(name) { " +
                "  this.name = name;" +
                "  this.greet = function() { return 'Hello ' + this.name; };" +
                "}"
            );

            // Get the constructor function
            V8Function personConstructor = (V8Function) v8.get("Person");

            // Call as constructor (like new Person("Alice"))
            V8Object alice = v8.executeObjectScript("new Person('Alice');");

            // Call a method on the JS object
            String greeting = alice.executeStringFunction("greet", null);
            System.out.println(greeting);  // Output: Hello Alice

            // Release resources
            alice.release();
            personConstructor.release();
        } finally {
            v8.release();
        }
    }
}
