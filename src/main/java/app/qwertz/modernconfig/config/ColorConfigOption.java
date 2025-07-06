package app.qwertz.modernconfig.config;

public class ColorConfigOption extends ConfigOption<Integer> {
    public ColorConfigOption(String key, String description, String category, int defaultValue) {
        super(key, description, category, defaultValue);
        
        // Ensure default value is a valid RGB color (0x000000 to 0xFFFFFF)
        setValue(defaultValue & 0xFFFFFF);
    }

    @Override
    public void setValue(Integer value) {
        // Ensure color value is within valid RGB range
        super.setValue(value & 0xFFFFFF);
    }

    /**
     * Get the red component of the color (0-255)
     */
    public int getRed() {
        return (getValue() >> 16) & 0xFF;
    }

    /**
     * Get the green component of the color (0-255)
     */
    public int getGreen() {
        return (getValue() >> 8) & 0xFF;
    }

    /**
     * Get the blue component of the color (0-255)
     */
    public int getBlue() {
        return getValue() & 0xFF;
    }

    /**
     * Set the color from RGB components (0-255 each)
     */
    public void setRGB(int red, int green, int blue) {
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));
        setValue((red << 16) | (green << 8) | blue);
    }

    /**
     * Get the color as a hex string (e.g., "#FF0000" for red)
     */
    public String getHexString() {
        return String.format("#%06X", getValue());
    }

    /**
     * Set the color from a hex string (e.g., "#FF0000" or "FF0000")
     */
    public void setFromHex(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            setValue(Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            // Invalid hex string, keep current value
        }
    }
} 