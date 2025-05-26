import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import net.objecthunter.exp4j.*;
import org.jfree.chart.*;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;

public class IncrementalMethodSolver extends JFrame {
    private JTextField functionField, aField, bField, hField;
    private JTable table;
    private DefaultTableModel tableModel;
    private ArrayList<Double> roots = new ArrayList<>();
    private String equation;

    public IncrementalMethodSolver() {
        setTitle("Incremental Method Root Finder");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Instructions Panel
        JTextArea instructions = new JTextArea(
            "📚 Incremental Method Solver\n" +
            "Instructions:\n" +
            "1. Enter a valid equation in terms of x (e.g., x^3 - x - 2).\n" +
            "2. Enter an interval [a, b] such that a < b.\n" +
            "3. Enter a small positive step size h (e.g., 0.1).\n" +
            "4. Click 'Compute' to estimate roots.\n"
        );
        instructions.setEditable(false);
        instructions.setBackground(new Color(240, 240, 240));
        instructions.setBorder(BorderFactory.createTitledBorder("How to Use"));
        add(instructions, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Parameters"));
        functionField = createField(inputPanel, "Function f(x):", "x^3 - x - 2");
        aField = createField(inputPanel, "Interval Start (a):", "0");
        bField = createField(inputPanel, "Interval End (b):", "5");
        hField = createField(inputPanel, "Increment h:", "0.1");

        JButton computeButton = new JButton("Compute");
        JButton helpButton = new JButton("Help");

        inputPanel.add(computeButton);
        inputPanel.add(helpButton);

        add(inputPanel, BorderLayout.WEST);

        // Table for Iteration Log (new columns)
        String[] columns = {"Iteration", "xl", "deltax", "xu", "f(xl)", "f(xu)*f(xl)", "Remark"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Iteration Table"));
        add(tableScroll, BorderLayout.CENTER);

        computeButton.addActionListener(e -> runIncremental());

        helpButton.addActionListener(e -> showHelpDialog());

        setVisible(true);
    }

    private JTextField createField(JPanel panel, String label, String defaultText) {
        JLabel lbl = new JLabel(label);
        JTextField field = new JTextField(defaultText);
        panel.add(lbl);
        panel.add(field);
        return field;
    }

    private double evaluate(double x) throws Exception {
        Expression expr = new ExpressionBuilder(equation).variable("x").build().setVariable("x", x);
        return expr.evaluate();
    }

    private void runIncremental() {
        tableModel.setRowCount(0);
        roots.clear();
        try {
            equation = functionField.getText().trim();
            double a = Double.parseDouble(aField.getText().trim());
            double b = Double.parseDouble(bField.getText().trim());
            double h = Double.parseDouble(hField.getText().trim());

            if (h <= 0 || a >= b || h >= (b - a)) {
                JOptionPane.showMessageDialog(this, "[!] Invalid input. Ensure:\n- a < b\n- h > 0\n- h < (b - a).", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ArrayList<Double> xValues = new ArrayList<>();
            ArrayList<Double> fxValues = new ArrayList<>();

            // Fill values & table rows for intervals
            int iteration = 1;
            for (double x = a; x <= b; x += h) {
                double fx = evaluate(x);
                xValues.add(x);
                fxValues.add(fx);
            }

            for (int i = 0; i < xValues.size() - 1; i++) {
                double xl = xValues.get(i);
                double xu = xValues.get(i + 1);
                double fx_l = fxValues.get(i);
                double fx_u = fxValues.get(i + 1);
                double prod = fx_l * fx_u;
                String remark = prod <= 0 ? "Root Interval" : "-";

                if (prod <= 0) {
                    double approxRoot = (xl + xu) / 2;
                    roots.add(approxRoot);
                }

                tableModel.addRow(new Object[]{
                    iteration++,
                    String.format("%.6f", xl),
                    String.format("%.6f", h),
                    String.format("%.6f", xu),
                    String.format("%.6f", fx_l),
                    String.format("%.6f", prod),
                    remark
                });
            }

            if (roots.isEmpty()) {
                JOptionPane.showMessageDialog(this, "[!] No root found in the interval.", "Result", JOptionPane.INFORMATION_MESSAGE);
            } else {
                StringBuilder result = new StringBuilder("[OK] Estimated roots at:\n");
                for (double root : roots) {
                    result.append(String.format("Root at x ≈ %.6f\n", root));
                }
                JOptionPane.showMessageDialog(this, result.toString(), "Result", JOptionPane.INFORMATION_MESSAGE);
            }

            showPlot(a, b, xValues, fxValues);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPlot(double a, double b, ArrayList<Double> xVals, ArrayList<Double> fxVals) {
        // Function series (line only)
        XYSeries seriesF = new XYSeries("f(x)");
        for (int i = 0; i < xVals.size(); i++) {
            seriesF.add(xVals.get(i), fxVals.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(seriesF);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Incremental Method - Root Finding",
            "x",
            "f(x)",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        XYPlot plot = chart.getXYPlot();
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);

        // Renderer for function: line only, blue
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, Color.BLUE);
        plot.setRenderer(renderer);

        // Plot roots as red dots with labels near dots
        for (double root : roots) {
            XYSeries rootPoint = new XYSeries("Root");
            rootPoint.add(root, 0);
            XYSeriesCollection rootDataset = new XYSeriesCollection(rootPoint);

            XYLineAndShapeRenderer rootRenderer = new XYLineAndShapeRenderer(false, true);
            rootRenderer.setSeriesPaint(0, Color.RED);
            rootRenderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-5, -5, 10, 10));

            int datasetIndex = plot.getDatasetCount();
            plot.setDataset(datasetIndex, rootDataset);
            plot.setRenderer(datasetIndex, rootRenderer);

            // Add label near the dot
            XYTextAnnotation annotation = new XYTextAnnotation(
                String.format("%.4f", root),
                root,
                0.02 * (plot.getRangeAxis().getUpperBound() - plot.getRangeAxis().getLowerBound())
            );
            annotation.setFont(new Font("SansSerif", Font.BOLD, 12));
            annotation.setPaint(Color.RED);
            plot.addAnnotation(annotation);
        }

        SwingUtilities.invokeLater(() -> {
            JFrame plotFrame = new JFrame("Incremental Plot");
            plotFrame.setSize(900, 600);
            plotFrame.add(new ChartPanel(chart));
            plotFrame.setLocationRelativeTo(null);
            plotFrame.setVisible(true);
        });
    }

    private void showHelpDialog() {
        String helpMessage = """
            📚 Incremental Method Root Finder Help

            Instructions:
            1. Enter a valid equation in terms of x (e.g., x^3 - x - 2).
            2. Enter an interval [a, b] such that a < b.
            3. Enter a small positive step size h (e.g., 0.1).
            4. Click 'Compute' to estimate roots.

            What is the Incremental Method?
            -------------------------------
            The Incremental Method is a root-finding technique that evaluates the function at
            equally spaced points in the given interval [a, b]. It detects roots by checking
            where the function changes sign between these points.

            Uses:
            - Simple way to locate approximate root intervals.
            - Good for functions where derivatives are hard to compute.
            - Helps provide initial guesses for other root-finding methods.

            Note: Smaller step sizes (h) yield better root approximations but require
            more computations.
            """;

        JOptionPane.showMessageDialog(this, helpMessage, "Help - Incremental Method", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(IncrementalMethodSolver::new);
    }
}
