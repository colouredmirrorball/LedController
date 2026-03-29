package be.cmbsoft.ledcontrol.output;

import java.util.Objects;

public final class Pixel {
    private int x;
    private int y;
    private int red;
    private int green;
    private int blue;

    public Pixel(int x, int y, int red, int green, int blue) {
        this.x = x;
        this.y = y;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int red() {
        return red;
    }

    public int green() {
        return green;
    }

    public int blue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (Pixel) obj;
        return this.x == that.x &&
                this.y == that.y &&
                this.red == that.red &&
                this.green == that.green &&
                this.blue == that.blue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, red, green, blue);
    }

    @Override
    public String toString() {
        return "Pixel[" +
                "x=" + x + ", " +
                "y=" + y + ", " +
                "red=" + red + ", " +
                "green=" + green + ", " +
                "blue=" + blue + ']';
    }

}
