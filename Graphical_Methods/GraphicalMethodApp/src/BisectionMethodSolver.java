import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import net.objecthunter.exp4j.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;

public class BisectionMethodSolver extends JFrame {
    private JTextField functionField, aField, bField, tolField, iterField;
    private JTable iterationTable;
    private DefaultTableModel tableModel;
    private ArrayList<Double> roots = new ArrayList<>();
    private String equation;

    public BisectionMethodSolver() {
        setTitle("Bisection Method Solver - Enhanced Version");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 🔹 Instructions Panel
        JTextArea instructions = new JTextArea(
            "📚 Bisection Method Solver\n" +
            "Instructions:\n" +
            "1. Enter a valid equation in terms of x (e.g., x^3 - x - 2).\n" +
            "2. Enter an interval [a, b] such that f(a)*f(b) < 0.\n" +
            "3. Enter the tolerance (e.g., 1e-5) and max iterations.\n" +
            "4. Click 'Compute' to find the root, view the table, and plot.\n"
        );
        instructions.setEditable(false);
        instructions.setBackground(new Color(240, 240, 240));
        instructions.setBorder(BorderFactory.createTitledBorder("How to Use"));
        add(instructions, BorderLayout.NORTH);

        // 🔹 Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 8, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Parameters"));
        functionField = createField(inputPanel, "Function f(x):", "x^3 - x - 2");
        aField = createField(inputPanel, "Interval Start (a):", "1");
        bField = createField(inputPanel, "Interval End (b):", "2");
        tolField = createField(inputPanel, "Tolerance:", "1e-5");
        iterField = createField(inputPanel, "Max Iterations:", "100");

        JButton computeButton = new JButton("Compute");
        inputPanel.add(computeButton);
        add(inputPanel, BorderLayout.WEST);

        // 🔹 Table for Iteration Log
        String[] columnNames = {"Iter", "xl", "xr", "xu", "f(xl)", "f(xr)", "Error", "f(xl)*f(xr)", "Remarks"};
        tableModel = new DefaultTableModel(columnNames, 0);
        iterationTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(iterationTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Bisection Iteration Table"));
        add(tableScroll, BorderLayout.CENTER);

        // 🔹 Compute Button Action
        computeButton.addActionListener(e -> runBisection());

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

    private void runBisection() {
        tableModel.setRowCount(0);
        roots.clear();
        try {
            equation = functionField.getText().trim();
            double xl = Double.parseDouble(aField.getText().trim());
            double xr = Double.parseDouble(bField.getText().trim());
            double tol = Double.parseDouble(tolField.getText().trim());
            int maxIter = Integer.parseInt(iterField.getText().trim());

            double fxl = evaluate(xl);
            double fxr = evaluate(xr);
            if (fxl * fxr >= 0) {
                JOptionPane.showMessageDialog(this, "Error: f(a) * f(b) must be negative.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double xu = 0, fxu, error;
            for (int iter = 1; iter <= maxIter; iter++) {
                xu = (xl + xr) / 2;
                fxu = evaluate(xu);
                error = Math.abs(xr - xl) / 2;
                double product = fxl * fxu;

                String remarks = "";
                if (Math.abs(fxu) < tol || error < tol) {
                    remarks = "Converged ✅";
                } else {
                    remarks = (product < 0) ? "Root in [xl,xu]" : "Root in [xu,xr]";
                }

                roots.add(xu);
                tableModel.addRow(new Object[]{
                    iter, xl, xr, xu, fxl, fxr, error, product, remarks
                });

                if (Math.abs(fxu) < tol || error < tol) break;

                if (product < 0) {
                    xr = xu;
                    fxr = fxu;
                } else {
                    xl = xu;
                    fxl = fxu;
                }
            }

            showPlot(Double.parseDouble(aField.getText().trim()), Double.parseDouble(bField.getText().trim()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPlot(double a, double b) {
    XYSeries seriesF = new XYSeries("f(x)");
    XYSeries seriesRoots = new XYSeries("Root Approximations");

    for (double x = a - 0.1 * Math.abs(b - a); x <= b + 0.1 * Math.abs(b - a); x += (b - a) / 200.0) {
        try {
            seriesF.add(x, evaluate(x));
        } catch (Exception ignored) {}
    }

    for (double root : roots) {
        seriesRoots.add(root, 0);
    }

    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(seriesF);
    dataset.addSeries(seriesRoots);

    JFreeChart chart = ChartFactory.createXYLineChart(
        "Bisection Method - Root Finding",
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

    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    renderer.setSeriesLinesVisible(0, true);  // f(x) line
    renderer.setSeriesShapesVisible(0, false);
    renderer.setSeriesLinesVisible(1, false); // Dots for roots only
    renderer.setSeriesShapesVisible(1, true);
    renderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
    renderer.setSeriesPaint(0, Color.BLUE);
    renderer.setSeriesPaint(1, Color.RED);

    plot.setRenderer(renderer);

    SwingUtilities.invokeLater(() -> {
        JFrame plotFrame = new JFrame("Bisection Plot");
        plotFrame.setSize(800, 600);
        plotFrame.add(new ChartPanel(chart));
        plotFrame.setLocationRelativeTo(null);
        plotFrame.setVisible(true);
    });
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BisectionMethodSolver::new);
    }
}
