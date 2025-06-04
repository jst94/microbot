public class DebugCoordinateTest {
    
    // Simulate the coordinate constant from the script
    private static final WorldPoint AFTER_PIPE_LOCATION = new WorldPoint(2998, 3931, 0);
    
    public static void main(String[] args) {
        System.out.println("=== COORDINATE DEBUG TEST ===");
        System.out.println("AFTER_PIPE_LOCATION constant definition: " + AFTER_PIPE_LOCATION);
        System.out.println("X coordinate: " + AFTER_PIPE_LOCATION.getX());
        System.out.println("Y coordinate: " + AFTER_PIPE_LOCATION.getY());
        System.out.println("Z coordinate: " + AFTER_PIPE_LOCATION.getPlane());
        
        // Test if the coordinate passes course area check
        int COURSE_MIN_Y = 3930;
        int COURSE_MAX_Y = 3965;
        int COURSE_MIN_X = 2985;
        int COURSE_MAX_X = 3007;
        
        boolean isInCourseArea = AFTER_PIPE_LOCATION.getX() >= COURSE_MIN_X && 
                                AFTER_PIPE_LOCATION.getX() <= COURSE_MAX_X &&
                                AFTER_PIPE_LOCATION.getY() >= COURSE_MIN_Y && 
                                AFTER_PIPE_LOCATION.getY() <= COURSE_MAX_Y;
        
        System.out.println("Is in course area: " + isInCourseArea);
        System.out.println("Y >= " + COURSE_MIN_Y + ": " + (AFTER_PIPE_LOCATION.getY() >= COURSE_MIN_Y));
        System.out.println("Y <= " + COURSE_MAX_Y + ": " + (AFTER_PIPE_LOCATION.getY() <= COURSE_MAX_Y));
    }
}

// Simple WorldPoint implementation for testing
class WorldPoint {
    private final int x, y, plane;
    
    public WorldPoint(int x, int y, int plane) {
        this.x = x;
        this.y = y;
        this.plane = plane;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getPlane() { return plane; }
    
    @Override
    public String toString() {
        return "WorldPoint(" + x + ", " + y + ", " + plane + ")";
    }
}
