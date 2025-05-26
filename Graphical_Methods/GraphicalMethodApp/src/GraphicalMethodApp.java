import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jfree.chart.*;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.*;

public class GraphicalMethodApp extends JFrame {
    private JTextField funcField, iterField;
    private JTable table;

    public GraphicalMethodApp() {
        setTitle("Graphical Method Root Finder");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // === Input Panel ===
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter Inputs"));

        funcField = new JTextField("");
        iterField = new JTextField("");

        inputPanel.add(new JLabel("Enter function f(x):"));
        inputPanel.add(funcField);
        inputPanel.add(new JLabel("Max Iterations:"));
        inputPanel.add(iterField);

        JButton enterButton = new JButton("ENTER");
        JButton aboutButton = new JButton("ABOUT");
        inputPanel.add(enterButton);
        inputPanel.add(aboutButton);

        // === Table Panel ===
        table = new JTable();
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Result Table"));
        tableScroll.setPreferredSize(new Dimension(850, 300));

        add(inputPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);

        // === Actions ===
        enterButton.addActionListener(e -> process());
        aboutButton.addActionListener(e -> showAboutDialog());

        setVisible(true);
    }

    private void process() {
        try {
            String input = funcField.getText().trim();
            input = input.replaceAll("@\\(x\\)", "");  // Remove @(x)
            input = input.replaceAll("\\.\\^", "\\^");  // Replace .^ with ^
            input = input.replaceAll("\\.\\*", "*");    // Replace .* with *
            input = input.replaceAll("\\./", "/");      // Replace ./ with /

            int maxIter = Integer.parseInt(iterField.getText().trim());
            if (maxIter <= 0) {
                JOptionPane.showMessageDialog(this, "Max iterations must be positive.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double a = -10;  // Fixed interval for simplicity
            double b = 10;
            double h = (b - a) / maxIter;

            Expression exp = new ExpressionBuilder(input).variable("x").build();
            List<Double> xVals = new ArrayList<>();
            List<Double> yVals = new ArrayList<>();
            List<Double> roots = new ArrayList<>();

            for (int i = 0; i <= maxIter; i++) {
                double x = a + i * h;
                double y = exp.setVariable("x", x).evaluate();
                xVals.add(x);
                yVals.add(y);
            }

            double tol = 1e-5;
            for (int i = 1; i < yVals.size(); i++) {
                if (yVals.get(i - 1) * yVals.get(i) <= 0) {
                    double root = refineRoot(exp, xVals.get(i - 1), xVals.get(i), tol);
                    if (roots.stream().noneMatch(r -> Math.abs(r - root) < tol)) {
                        roots.add(root);
                    }
                }
            }

            // Populate table
            String[][] data = new String[xVals.size()][2];
            for (int i = 0; i < xVals.size(); i++) {
                data[i][0] = String.format("%.5f", xVals.get(i));
                data[i][1] = String.format("%.7f", yVals.get(i));
            }
            table.setModel(new javax.swing.table.DefaultTableModel(data, new String[]{"x", "f(x)"}));

            // Plot the function
            plotGraph(input, xVals, yVals, roots);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for max iterations.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double refineRoot(Expression exp, double x1, double x2, double tol) {
        double mid;
        while ((x2 - x1) > tol) {
            mid = (x1 + x2) / 2;
            double f1 = exp.setVariable("x", x1).evaluate();
            double fmid = exp.setVariable("x", mid).evaluate();
            if (f1 * fmid <= 0) x2 = mid;
            else x1 = mid;
        }
        return (x1 + x2) / 2;
    }

    private void plotGraph(String func, List<Double> xVals, List<Double> yVals, List<Double> roots) {
        XYSeries series = new XYSeries("f(x)");
        for (int i = 0; i < xVals.size(); i++) {
            series.add(xVals.get(i), yVals.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Graphical Method", "x", "f(x)", dataset);
        XYPlot plot = chart.getXYPlot();
        plot.setRenderer(new XYSplineRenderer());

        // Add X-axis marker at y=0
        plot.addRangeMarker(new org.jfree.chart.plot.ValueMarker(0.0, Color.BLACK, new BasicStroke(1.5f)));

        // Mark roots with red dots
        XYSeries rootSeries = new XYSeries("Roots");
        for (double root : roots) {
            rootSeries.add(root, 0.0);
        }
        dataset.addSeries(rootSeries);

        XYSplineRenderer renderer = new XYSplineRenderer();
        renderer.setSeriesPaint(0, Color.BLUE); // Function line
        renderer.setSeriesPaint(1, Color.RED);  // Root dots
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
        plot.setRenderer(renderer);

        // Text annotations for roots
        for (int i = 0; i < roots.size(); i++) {
            double root = roots.get(i);
            XYTextAnnotation ann = new XYTextAnnotation("Root " + (i + 1), root, 0);
            ann.setPaint(Color.RED);
            ann.setFont(new Font("Arial", Font.BOLD, 12));
            plot.addAnnotation(ann);
        }

        // Display graph
        JFrame frame = new JFrame("Graphical Method");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(850, 650);
        frame.add(new ChartPanel(chart));
        frame.setVisible(true);
    }

    private void showAboutDialog() {
        String message = """
                === Graphical Method Root Finder ===

                Instructions:
                1. Enter the function using JavaScript-like syntax.
                   Example: 2*x^2 - 5*x + 3
                2. Enter the maximum number of iterations.
                   (More iterations give better graph resolution)
                3. The program scans the interval [-10, 10].
                4. The function is plotted, and roots (where sign changes) are marked in red.

                Note: Use element-wise operators:
                      ^ for power, * for multiplication, / for division.

                """;
        JOptionPane.showMessageDialog(this, message, "About - Graphical Method", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) UIManager.setLookAndFeel(info.getClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(GraphicalMethodApp::new);
    }
}
