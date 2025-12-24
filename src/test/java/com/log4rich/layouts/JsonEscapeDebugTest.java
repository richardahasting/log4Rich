package com.log4rich.layouts;

import com.log4rich.util.JsonObject;

public class JsonEscapeDebugTest {
    public static void main(String[] args) {
        JsonObject obj = new JsonObject();
        obj.addProperty("test", "Message with \"quotes\" and \n newlines and \t tabs");
        
        System.out.println("Compact JSON:");
        System.out.println(obj.toCompactString());
        
        System.out.println("\nPretty JSON:");
        System.out.println(obj.toString());
        
        // Test what we actually expect
        String compact = obj.toCompactString();
        System.out.println("\nChecking escaping:");
        System.out.println("Contains \\\"quotes\\\" (escaped quotes): " + compact.contains("\\\"quotes\\\""));
        System.out.println("Contains \\n (escaped newline): " + compact.contains("\\n"));
        System.out.println("Contains \\t (escaped tab): " + compact.contains("\\t"));
    }
}