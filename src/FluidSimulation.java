import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

public class FluidSimulation {
    // Constants
    private final int MAX_PARTICLES = 10000;
    private final double GRAVITY = 0.2;
    private final double DRAG = 0.98;
    private final double BOUNCE = 0.2;
    private final double PRESSURE = 0.05;
    private final double VISCOSITY = 0.1;
    private final Rectangle BOUNDING_BOX = new Rectangle(5, 5, 700 - 2 * (int)Particle.RADIUS, 500 - 2 * (int)Particle.RADIUS);
    private final int CELL_SIZE = 20;
    private static final double DENSITY_THRESHOLD = 3;

    // Variables
    private DrawingPanel drawingPanel;
    private JComboBox<String> visualizationMode;
    private ArrayList<Particle> particles;
    private final Map<Point, LinkedList<Particle>> grid = new HashMap<>();
    private Point mousePoint;
    private boolean isLeftClick = false;
    private boolean isRightClick = false;
    private long lastTime;
    private int frames;
    private double fps;

    public FluidSimulation() {
        setupUI();
        setupTimer();
    }

    private void setupUI() {
        JFrame frame = new JFrame("2D Fluid Simulation");
        frame.setSize(BOUNDING_BOX.width + 100, BOUNDING_BOX.height + 150);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        particles = new ArrayList<>();
        lastTime = System.currentTimeMillis();
        frames = 0;
        fps = 0;

        drawingPanel = new DrawingPanel();
        setupMouseListeners();
        frame.add(drawingPanel, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void setupMouseListeners() {
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }
        });
        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mousePoint = e.getPoint();
            }
        });
    }

    private void handleMousePressed(MouseEvent e) {
        if (mousePoint != null) {
            if (!mousePoint.equals(e.getPoint())) {
                mousePoint = e.getPoint();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isLeftClick = true;
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    isRightClick = true;
                }
            }
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            isLeftClick = false;
        } else if (SwingUtilities.isRightMouseButton(e)) {
            isRightClick = false;
        }
    }

    private void setupTimer() {
        Timer timer = new Timer(0, e -> {
            applyPressureAndViscosity();
            handleUserInteraction();
            updateParticles();
            drawingPanel.repaint();
            updateFPS();
        });
        timer.start();
    }

    private void updateFPS() {
        frames++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime >= 1000) {
            fps = frames;
            frames = 0;
            lastTime = currentTime;
        }
    }

    private void applyGravityToParticle(Particle p, double gravity) {
        p.vy += gravity;
    }

    private void applyDragToParticle(Particle p, double dragCoefficient) {
        p.vx *= dragCoefficient;
        p.vy *= dragCoefficient;
    }

    private void updateParticle(Particle p, Rectangle boundingBox, double bounce) {
        p.x += p.vx;
        p.y += p.vy;

        // Ensure particles remain within the visible frame
        p.x = Math.min(Math.max(p.x, boundingBox.x + Particle.RADIUS), boundingBox.x + boundingBox.width - Particle.RADIUS);
        p.y = Math.min(Math.max(p.y, boundingBox.y + Particle.RADIUS), boundingBox.y + boundingBox.height - Particle.RADIUS);

        if (p.x == boundingBox.x + Particle.RADIUS || p.x == boundingBox.x + boundingBox.width - Particle.RADIUS) {
            p.vx = -p.vx * bounce;
        }
        if (p.y == boundingBox.y + Particle.RADIUS || p.y == boundingBox.y + boundingBox.height - Particle.RADIUS) {
            p.vy = -p.vy * bounce;
        }
    }

    private void updateParticles() {
        particles.parallelStream().forEach(p -> {
            applyGravityToParticle(p, GRAVITY);
            applyDragToParticle(p, DRAG);
            updateParticle(p, BOUNDING_BOX, BOUNCE);
        });
    }

    private void buildGrid() {
        grid.clear();
        for (Particle p : particles) {
            Point cell = new Point((int) (p.x / CELL_SIZE), (int) (p.y / CELL_SIZE));
            grid.computeIfAbsent(cell, k -> new LinkedList<>()).add(p);
        }
    }

    private LinkedList<Particle> getNeighbors(Particle p) {
        int cellX = (int) (p.x / CELL_SIZE);
        int cellY = (int) (p.y / CELL_SIZE);
        LinkedList<Particle> neighbors = new LinkedList<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                neighbors.addAll(grid.getOrDefault(new Point(cellX + i, cellY + j), new LinkedList<>()));
            }
        }
        return neighbors;
    }

    private void handleUserInteraction() {
        if (particles.size() >= MAX_PARTICLES) return; // Check for max particles
        if (isLeftClick) {
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(mousePoint.x, mousePoint.y, (Math.random() - 0.5) * 5, (Math.random() - 0.5) * 5));
            }
        }
        if (isRightClick) {
            double constantForce = 3.0; // Adjust this value for desired force strength
            double forceRadius = 100.0;  // Adjust this value for the desired radius of effect

            for (Particle p : particles) {
                double dx = mousePoint.x - p.x;
                double dy = mousePoint.y - p.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance <= forceRadius) {
                    double angle = Math.atan2(dy, dx);
                    p.vx += constantForce * Math.cos(angle);
                    p.vy += constantForce * Math.sin(angle);
                }
            }
        }
    }


    private double calculateDensity(int x, int y) {
        double density = 0;
        for (Particle p : particles) {
            double dx = x - p.x;
            double dy = y - p.y;
            double distanceSquared = dx * dx + dy * dy;
            density += Particle.RADIUS * Particle.RADIUS / (distanceSquared + 1);
        }
        return density;
    }

    private void applyPressureAndViscosity() {
        buildGrid();
        particles.parallelStream().forEach(a -> {
            LinkedList<Particle> neighbors = getNeighbors(a);
            for (Particle b : neighbors) {
                if (a != b) {
                    double dx = b.x - a.x;
                    double dy = b.y - a.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < 2 * Particle.RADIUS) { // Only check interactions for particles within a certain distance
                        double angle = Math.atan2(dy, dx);
                        double force = (2 * Particle.RADIUS - dist) * PRESSURE;
                        a.vx -= force * Math.cos(angle);
                        a.vy -= force * Math.sin(angle);
                        b.vx += force * Math.cos(angle);
                        b.vy += force * Math.sin(angle);

                        double avgVx = (a.vx + b.vx) * 0.5;
                        double avgVy = (a.vy + b.vy) * 0.5;
                        a.vx = avgVx * VISCOSITY + a.vx * (1 - VISCOSITY);
                        a.vy = avgVy * VISCOSITY + a.vy * (1 - VISCOSITY);
                        b.vx = avgVx * VISCOSITY + b.vx * (1 - VISCOSITY);
                        b.vy = avgVy * VISCOSITY + b.vy * (1 - VISCOSITY);
                    }
                }
            }
        });
    }

    private List<Polygon> marchingSquares() {
        List<Polygon> polygons = Collections.synchronizedList(new ArrayList<>());
        int stepSize = 5; // Adjust for resolution vs performance

        IntStream.range(0, BOUNDING_BOX.width / stepSize).parallel().forEach(i -> {
            int x = i * stepSize;
            for (int y = 0; y < BOUNDING_BOX.height; y += stepSize) {
                // Calculate densities at the corners of the square
                double[] densities = {
                        calculateDensity(x, y),
                        calculateDensity(x + stepSize, y),
                        calculateDensity(x + stepSize, y + stepSize),
                        calculateDensity(x, y + stepSize)
                };

                // Generate contour for this square
                Polygon contour = generateContour(x, y, stepSize, densities);
                if (contour != null) {
                    polygons.add(contour);
                }
            }
        });

        return polygons;
    }

    private Polygon generateContour(int x, int y, int stepSize, double[] densities) {
        int caseIndex = 0;
        if (densities[0] > DENSITY_THRESHOLD) caseIndex |= 1;
        if (densities[1] > DENSITY_THRESHOLD) caseIndex |= 2;
        if (densities[2] > DENSITY_THRESHOLD) caseIndex |= 4;
        if (densities[3] > DENSITY_THRESHOLD) caseIndex |= 8;

        // Based on the caseIndex, determine the contour edges
        return switch (caseIndex) {
            case 0, 15 ->
                // No contour
                    null;
            case 1, 14 -> createPolygon(new int[]{x, x, x + stepSize / 2}, new int[]{y + stepSize / 2, y, y});
            case 2, 13 ->
                    createPolygon(new int[]{x + stepSize / 2, x + stepSize, x + stepSize}, new int[]{y, y, y + stepSize / 2});
            case 3, 12 ->
                    createPolygon(new int[]{x, x, x + stepSize}, new int[]{y + stepSize / 2, y, y + stepSize / 2});
            case 4, 11 ->
                    createPolygon(new int[]{x + stepSize, x + stepSize, x + stepSize / 2}, new int[]{y + stepSize / 2, y + stepSize, y + stepSize});
            case 5 -> createPolygon(new int[]{x, x, x + stepSize / 2, x + stepSize, x + stepSize, x + stepSize / 2},
                    new int[]{y + stepSize / 2, y, y, y, y + stepSize / 2, y + stepSize});
            case 6, 9 -> createPolygon(new int[]{x + stepSize / 2, x + stepSize, x, x + stepSize / 2},
                    new int[]{y, y, y + stepSize, y + stepSize});
            case 7, 8 ->
                    createPolygon(new int[]{x, x, x + stepSize}, new int[]{y + stepSize / 2, y + stepSize, y + stepSize});
            case 10 -> createPolygon(new int[]{x, x + stepSize / 2, x + stepSize, x + stepSize, x + stepSize / 2, x},
                    new int[]{y, y, y, y + stepSize, y + stepSize, y + stepSize / 2});
            default -> null;
        };
    }

    private Polygon createPolygon(int[] xPoints, int[] yPoints) {
        return new Polygon(xPoints, yPoints, xPoints.length);
    }

    private double calculateAveragePressure(Particle p) {
        LinkedList<Particle> neighbors = getNeighbors(p);
        return neighbors.size();
    }

    private Color getPressureColor(double pressure) {
        int maxPressure = 50; // Adjust based on expected max pressure
        double normalizedPressure = Math.min(pressure / maxPressure, 1.0);
        int red = (int) (255 * normalizedPressure);
        int blue = (int) (255 * (1 - normalizedPressure));
        return new Color(red, 0, blue);
    }

    private double calculateContourAveragePressure(Polygon contour) {
        double totalPressure = 0;
        int count = 0;
        for (Particle p : particles) {
            if (contour.contains(p.x, p.y)) {
                totalPressure += calculateAveragePressure(p);
                count++;
            }
        }
        return count > 0 ? totalPressure / count : 0;
    }

    class DrawingPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            List<Polygon> fluidContours = marchingSquares();
            for (Polygon contour : fluidContours) {
                double contourAveragePressure = calculateContourAveragePressure(contour);
                Color contourColor = getPressureColor(contourAveragePressure);
                g.setColor(contourColor);
                g.fillPolygon(contour);
            }

            // Draw FPS and particle count
            g.setColor(Color.BLACK);
            g.drawString("FPS: " + fps, 10, 20);
            g.drawString("Particles: " + particles.size(), 10, 40);
        }
    }

    public static void main(String[] args) {
        new FluidSimulation();
    }
}