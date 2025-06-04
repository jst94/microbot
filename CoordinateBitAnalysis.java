public class CoordinateBitAnalysis {
    public static void main(String[] args) {
        System.out.println("=== COORDINATE BIT ANALYSIS ===");
        
        // The 3931 coordinate that should be correct
        int correctY = 3931;
        
        // The 3331 coordinate that appears in errors 
        int corruptedY = 3331;
        
        // Check the bit patterns
        System.out.println("Correct Y (3931): " + Integer.toBinaryString(correctY));
        System.out.println("Corrupted Y (3331): " + Integer.toBinaryString(corruptedY));
        System.out.println("Difference: " + (correctY - corruptedY) + " = " + Integer.toBinaryString(correctY - corruptedY));
        
        // Analyze the XOR
        int xor = correctY ^ corruptedY;
        System.out.println("XOR pattern: " + Integer.toBinaryString(xor));
        
        // Check bit positions
        System.out.println("\nBit analysis:");
        for (int i = 0; i < 32; i++) {
            int mask = 1 << i;
            boolean correctBit = (correctY & mask) != 0;
            boolean corruptedBit = (corruptedY & mask) != 0;
            if (correctBit != corruptedBit) {
                System.out.println("Bit " + i + " differs: correct=" + correctBit + ", corrupted=" + corruptedBit);
            }
        }
        
        // Test the WorldPoint.fromCoord() behavior
        System.out.println("\n=== FROMCOORD ANALYSIS ===");
        
        // Create a packed coordinate that would yield 3931
        int packedFor3931 = (0 << 28) | (2998 << 14) | 3931;
        System.out.println("Packed coord for (2998, 3931, 0): " + packedFor3931 + " (0x" + Integer.toHexString(packedFor3931) + ")");
        
        // Extract coordinates using fromCoord formula
        int extractedX = (packedFor3931 >>> 14) & 0x3FFF;
        int extractedY = packedFor3931 & 0x3FFF;
        int extractedPlane = (packedFor3931 >>> 28) & 0x3;
        
        System.out.println("Extracted X: " + extractedX);
        System.out.println("Extracted Y: " + extractedY);
        System.out.println("Extracted Plane: " + extractedPlane);
        
        // Now test what packed coordinate would yield 3331
        int packedFor3331 = (0 << 28) | (2998 << 14) | 3331;
        System.out.println("\nPacked coord for (2998, 3331, 0): " + packedFor3331 + " (0x" + Integer.toHexString(packedFor3331) + ")");
        
        // Check if there's a bit pattern that could cause this corruption
        System.out.println("\n=== POTENTIAL CORRUPTION SOURCES ===");
        
        // Check if 0x3FFF mask could cause the issue
        System.out.println("0x3FFF mask: " + Integer.toBinaryString(0x3FFF));
        System.out.println("3931 & 0x3FFF = " + (3931 & 0x3FFF));
        System.out.println("3331 & 0x3FFF = " + (3331 & 0x3FFF));
        
        // Check if there's any pattern in the difference
        int diff = 3931 - 3331;  // 600
        System.out.println("Difference 600 in binary: " + Integer.toBinaryString(diff));
        System.out.println("600 in hex: 0x" + Integer.toHexString(diff));
        
        // Check bit position 9 (which is 512) and bit position 7 (which is 128)
        // 512 + 88 = 600, so maybe bits are being cleared
        System.out.println("\nBit position analysis for 600:");
        for (int i = 0; i < 16; i++) {
            if ((diff & (1 << i)) != 0) {
                System.out.println("Bit " + i + " is set (value: " + (1 << i) + ")");
            }
        }
    }
}
