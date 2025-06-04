public class CoordinateTest {
    public static void main(String[] args) {
        // Simulate the WorldPoint coordinates from our script
        int x = 2998;
        int y = 3931;
        int z = 0;
        
        System.out.println("Original coordinates: x=" + x + ", y=" + y + ", z=" + z);
        
        // Test if there could be any bit manipulation issues
        System.out.println("Y as binary: " + Integer.toBinaryString(y));
        System.out.println("3331 as binary: " + Integer.toBinaryString(3331));
        
        // Check the difference
        System.out.println("Difference: " + (y - 3331));
        System.out.println("Difference as binary: " + Integer.toBinaryString(y - 3331));
        
        // Check if it's a bit flip issue
        int xor = y ^ 3331;
        System.out.println("XOR difference: " + xor);
        System.out.println("XOR as binary: " + Integer.toBinaryString(xor));
        
        // Test course area logic
        int COURSE_MIN_Y = 3930;
        int COURSE_MAX_Y = 3965;
        
        System.out.println("\nCourse area check for y=3931: " + (y >= COURSE_MIN_Y && y <= COURSE_MAX_Y));
        System.out.println("Course area check for y=3331: " + (3331 >= COURSE_MIN_Y && 3331 <= COURSE_MAX_Y));
    }
}
