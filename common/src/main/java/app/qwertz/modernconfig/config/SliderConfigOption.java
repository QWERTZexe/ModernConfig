package app.qwertz.modernconfig.config;

public class SliderConfigOption extends ConfigOption<Double> {
    private final double minValue;
    private final double maxValue;
    private final int precision;

    public SliderConfigOption(String key, String description, String category, 
                             double defaultValue, double minValue, double maxValue, int precision) {
        super(key, description, category, defaultValue);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.precision = precision;
        
        // Ensure default value is within bounds
        setValue(Math.max(minValue, Math.min(maxValue, defaultValue)));
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public int getPrecision() {
        return precision;
    }

    @Override
    public void setValue(Double value) {
        super.setValue(Math.max(minValue, Math.min(maxValue, value)));
    }
} 