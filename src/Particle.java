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
}