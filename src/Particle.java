import java.awt.*;

class Particle {
    double x, y, vx, vy;
    static final double RADIUS = 2.5;

    public Particle(double x, double y, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
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

        if (x < boundingBox.x) {
            x = boundingBox.x;
            vx = Math.abs(vx) * bounce;
        }
        if (x > boundingBox.x + boundingBox.width) {
            x = boundingBox.x + boundingBox.width;
            vx = -Math.abs(vx) * bounce;
        }
        if (y < boundingBox.y) {
            y = boundingBox.y;
            vy = Math.abs(vy) * bounce;
        }
        if (y > boundingBox.y + boundingBox.height) {
            y = boundingBox.y + boundingBox.height;
            vy = -Math.abs(vy) * bounce;
        }
    }

    public void draw(Graphics g, String mode) {
        double value = 0;
        switch (mode) {
            case "Pressure":
                value = Math.sqrt(vx * vx + vy * vy);
                break;
            case "Velocity":
                value = Math.sqrt(vx * vx + vy * vy);
                value = Math.min(1, Math.max(0, value / 10));
                break;
            case "Speed":
                value = Math.sqrt(vx * vx + vy * vy) / 10;
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