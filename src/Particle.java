import java.awt.*;

class Particle {
    double x, y, vx, vy, ax, ay, pressure;
    static double RADIUS = 5.5;

    public Particle(double x, double y, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    public static void setRadius(double newRadius) {
        RADIUS = newRadius;
    }

    public double getSpeed() {
        return Math.sqrt(vx * vx + vy * vy);
    }

    public double getAcceleration() {
        return Math.sqrt(ax * ax + ay * ay);
    }



    public void applyGravity(double gravity) {
        vy += gravity;
    }

    public void applyDrag(double dragCoefficient) {
        vx *= dragCoefficient;
        vy *= dragCoefficient;
    }

    public void update(Rectangle boundingBox, double bounce) {
        x += vx;
        y += vy;

        // Ensure particles remain within the visible frame
        if (x < boundingBox.x + RADIUS) {
            x = boundingBox.x + RADIUS;
            vx = Math.abs(vx) * bounce;
        }
        if (x > boundingBox.x + boundingBox.width - RADIUS) {
            x = boundingBox.x + boundingBox.width - RADIUS;
            vx = -Math.abs(vx) * bounce;
        }
        if (y < boundingBox.y + RADIUS) {
            y = boundingBox.y + RADIUS;
            vy = Math.abs(vy) * bounce;
        }
        if (y > boundingBox.y + boundingBox.height - RADIUS) {
            y = boundingBox.y + boundingBox.height - RADIUS;
            vy = -Math.abs(vy) * bounce;
        }
    }

    public void draw(Graphics g, String mode) {
        double value = 0;
        switch (mode) {
            case "Pressure":
                // Calculate pressure value for the particle (this needs further implementation)
                value = 0;
                break;
            case "Velocity":
                value = Math.sqrt(vx * vx + vy * vy);
                value = Math.min(1, Math.max(0, value / 10));
                break;
            case "Acceleration":
                // Calculate acceleration value for the particle (this needs further implementation)
                value = 0;
                break;
        }
        value = Math.min(1, Math.max(0, value));
        g.setColor(getInterpolatedColor(value));
        g.fillOval((int) (x - RADIUS), (int) (y - RADIUS), (int) (2 * RADIUS), (int) (2 * RADIUS));
    }



    private Color getInterpolatedColor(double value) {
        int blue = (int) (255 * (1 - value));
        int red = (int) (255 * value);
        return new Color(red, 0, blue);
    }


}