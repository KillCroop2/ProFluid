import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class FluidSimulation {

    private final DrawingPanel drawingPanel;
    private final JComboBox<String> visualizationMode;
    private final ArrayList<Particle> particles;
    private final int NUM_PARTICLES = 0;
    private final double GRAVITY = 0.2;
    private final double DRAG = 0.98;
    private final double BOUNCE = 0.7;
    private final double PRESSURE = 0.05;
    private final double VISCOSITY = 0.02;

    private final Rectangle BOUNDING_BOX = new Rectangle(5, 5, 700 - 2 * (int)Particle.RADIUS, 500 - 2 * (int)Particle.RADIUS);

    private Point mousePoint;
    private boolean isLeftClick = false;
    private boolean isRightClick = false;

    private long lastTime;
    private int frames;
    private double fps;

    private final int CELL_SIZE = 20;
    private static final double DENSITY_THRESHOLD = 0.5;

    private final Map<Point, LinkedList<Particle>> grid = new HashMap<>();

    public FluidSimulation() {
        JFrame frame = new JFrame("2D Fluid Simulation");
        frame.setSize(BOUNDING_BOX.width + 100, BOUNDING_BOX.height + 150);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        particles = new ArrayList<>();
        initParticles();

        lastTime = System.currentTimeMillis();
        frames = 0;
        fps = 0;


        drawingPanel = new DrawingPanel();
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mousePoint = e.getPoint();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isLeftClick = true;
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    isRightClick = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isLeftClick = false;
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    isRightClick = false;
                }
            }
        });
        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mousePoint = e.getPoint();
            }
        });

        frame.add(drawingPanel, BorderLayout.CENTER);

        String[] modes = {"Pressure", "Velocity", "Speed"};
        visualizationMode = new JComboBox<>(modes);
        visualizationMode.addActionListener(e -> drawingPanel.setMode(Objects.requireNonNull(visualizationMode.getSelectedItem()).toString()));
        //frame.add(visualizationMode, BorderLayout.NORTH);

        Timer timer = new Timer(0, e -> {
            applyPressureAndViscosity();
            handleUserInteraction();
            updateParticles();
            drawingPanel.repaint();
            frames++;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTime >= 1000) {
                fps = frames;
                frames = 0;
                lastTime = currentTime;
            }

        });
        timer.start();

        frame.setVisible(true);
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



    private void initParticles() {
        Random rand = new Random();
        for (int i = 0; i < NUM_PARTICLES; i++) {
            double x = BOUNDING_BOX.x + rand.nextDouble() * BOUNDING_BOX.width;
            double y = BOUNDING_BOX.y + rand.nextDouble() * BOUNDING_BOX.height;
            double vx = (rand.nextDouble() - 0.5) * 10;
            double vy = (rand.nextDouble() - 0.5) * 10;
            particles.add(new Particle(x, y, vx, vy));
        }
    }

    private void handleUserInteraction() {
        if (mousePoint == null) return;
        if (isLeftClick) {
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(mousePoint.x, mousePoint.y, (Math.random() - 0.5) * 5, (Math.random() - 0.5) * 5));
            }
        }
        if (isRightClick) {
            for (Particle p : particles) {
                double dx = mousePoint.x - p.x;
                double dy = mousePoint.y - p.y;

                double force = 15 / (1 + Math.sqrt(dx * dx + dy * dy));
                double angle = Math.atan2(dy, dx);
                p.vx += force * Math.cos(angle);
                p.vy += force * Math.sin(angle);
            }
        }
    }

    private void applyPressureAndViscosity() {
        buildGrid();
        for (Particle a : particles) {
            LinkedList<Particle> neighbors = getNeighbors(a);
            for (Particle b : neighbors) {
                if (a != b) {
                    double dx = b.x - a.x;
                    double dy = b.y - a.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < 5) { // Only check interactions for particles within a certain distance
                        double angle = Math.atan2(dy, dx);
                        double force = (5 - dist) * PRESSURE;
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
        }
    }

    private void updateParticles() {
        for (Particle p : particles) {
            p.applyGravity(GRAVITY);
            p.applyDrag(DRAG);
            p.update(BOUNDING_BOX, BOUNCE);
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

    private ArrayList<Polygon> marchingSquares() {
        ArrayList<Polygon> polygons = new ArrayList<>();
        int stepSize = 5; // Adjust for resolution vs performance

        for (int x = 0; x < BOUNDING_BOX.width; x += stepSize) {
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
        }

        return polygons;
    }

    private Polygon generateContour(int x, int y, int stepSize, double[] densities) {
        int caseIndex = 0;
        if (densities[0] > DENSITY_THRESHOLD) caseIndex |= 1;
        if (densities[1] > DENSITY_THRESHOLD) caseIndex |= 2;
        if (densities[2] > DENSITY_THRESHOLD) caseIndex |= 4;
        if (densities[3] > DENSITY_THRESHOLD) caseIndex |= 8;

        // Based on the caseIndex, determine the contour edges
        switch (caseIndex) {
            case 0:
            case 15:
                // No contour
                return null;
            case 1:
            case 14:
                return createPolygon(new int[]{x, x, x + stepSize / 2}, new int[]{y + stepSize / 2, y, y});
            case 2:
            case 13:
                return createPolygon(new int[]{x + stepSize / 2, x + stepSize, x + stepSize}, new int[]{y, y, y + stepSize / 2});
            case 3:
            case 12:
                return createPolygon(new int[]{x, x, x + stepSize}, new int[]{y + stepSize / 2, y, y + stepSize / 2});
            case 4:
            case 11:
                return createPolygon(new int[]{x + stepSize, x + stepSize, x + stepSize / 2}, new int[]{y + stepSize / 2, y + stepSize, y + stepSize});
            case 5:
                return createPolygon(new int[]{x, x, x + stepSize / 2, x + stepSize, x + stepSize, x + stepSize / 2},
                        new int[]{y + stepSize / 2, y, y, y, y + stepSize / 2, y + stepSize});
            case 6:
            case 9:
                return createPolygon(new int[]{x + stepSize / 2, x + stepSize, x, x + stepSize / 2},
                        new int[]{y, y, y + stepSize, y + stepSize});
            case 7:
            case 8:
                return createPolygon(new int[]{x, x, x + stepSize}, new int[]{y + stepSize / 2, y + stepSize, y + stepSize});
            case 10:
                return createPolygon(new int[]{x, x + stepSize / 2, x + stepSize, x + stepSize, x + stepSize / 2, x},
                        new int[]{y, y, y, y + stepSize, y + stepSize, y + stepSize / 2});
        }
        return null;
    }

    private Polygon createPolygon(int[] xPoints, int[] yPoints) {
        return new Polygon(xPoints, yPoints, xPoints.length);
    }


    class DrawingPanel extends JPanel {
        private String mode = "Pressure";

        public void setMode(String mode) {
            this.mode = mode;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            ArrayList<Polygon> fluidContours = marchingSquares();
            for (Polygon contour : fluidContours) {
                g.fillPolygon(contour);
            }
            //g.setColor(Color.GRAY);
            //g.drawRect(BOUNDING_BOX.x, BOUNDING_BOX.y, BOUNDING_BOX.width, BOUNDING_BOX.height);
            //for (Particle p : particles) {
            //    p.draw(g, mode);
            //}

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