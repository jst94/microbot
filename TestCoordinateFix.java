public class TestCoordinateFix {
    public static void main(String[] args) {
        System.out.println("Testing coordinate corruption fix...");
        
        // Simulate the coordinate corruption scenario
        int corruptedY = 3331;
        int correctY = 3931;
        int x = 2998;
        int plane = 0;
        
        System.out.println("Corrupted coordinates: (" + x + ", " + corruptedY + ", " + plane + ")");
        System.out.println("Correct coordinates: (" + x + ", " + correctY + ", " + plane + ")");
        
        // Test the validation logic
        if (corruptedY == 3331 && x == 2998) {
            System.out.println("WARNING: Detected Y-coordinate corruption!");
            System.out.println("Fixed coordinates: (" + x + ", " + correctY + ", " + plane + ")");
        }
        
        // Test course area validation
        int courseAreaMinY = 3930;
        System.out.println("\nCourse area validation:");
        System.out.println("Corrupted Y (" + corruptedY + ") >= " + courseAreaMinY + "? " + (corruptedY >= courseAreaMinY));
        System.out.println("Correct Y (" + correctY + ") >= " + courseAreaMinY + "? " + (correctY >= courseAreaMinY));
        
        // Show the 600-unit difference
        int difference = correctY - corruptedY;
        System.out.println("\nDifference: " + correctY + " - " + corruptedY + " = " + difference);
        System.out.println("Binary representation of 600: " + Integer.toBinaryString(600));
    }
}
