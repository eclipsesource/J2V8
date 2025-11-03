package com.example.j2v8hello;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.eclipsesource.v8.V8;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView resultText = findViewById(R.id.resultText);
        
        try {
            // Create a V8 runtime
            V8 v8 = V8.createV8Runtime();
            
            // Execute JavaScript code
            String jsCode = "'Hello from ' + 'J2V8!'";
            String result = v8.executeStringScript(jsCode);
            
            // Also demonstrate math
            int mathResult = v8.executeIntegerScript("7 * 6");
            
            // Display results
            String output = "JavaScript Result:\n" + result + "\n\n" +
                          "Math Result (7 * 6):\n" + mathResult + "\n\n" +
                          "V8 Version:\n" + V8.getV8Version();
            
            resultText.setText(output);
            
            // Clean up
            v8.release();
            
        } catch (Exception e) {
            resultText.setText("Error: " + e.getMessage() + "\n\n" + 
                             "Stack trace:\n" + android.util.Log.getStackTraceString(e));
        }
    }
}
