import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import net.objecthunter.exp4j.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;

public class RegulaFalsiMethodSolver extends JFrame {
    private JTextField functionField, aField, bField, tolField, maxIterField;
    private JTable table;
    private DefaultTableModel tableModel;
    private ArrayList<Double> roots = new ArrayList<>();

    public RegulaFalsiMethodSolver() {
        setTitle("Regula Falsi Method Solver");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Instructions
        JTextArea instructions = new JTextArea(
            "📚 Regula Falsi Method Solver\n" +
            "Instructions:\n" +
            "- Enter f(x) using exp4j syntax (e.g., x^3 - x - 2).\n" +
            "- Supported functions: sin, cos, exp, log, sqrt, etc.\n" +
            "- Use '^' for powers (e.g., x^3).\n" +
            "- Provide interval [a, b], tolerance, and max iterations.\n" +
            "- Ensure f(a) * f(b) < 0 (opposite signs).\n"
        );
        instructions.setEditable(false);
        instructions.setBackground(new Color(240, 240, 240));
        instructions.setBorder(BorderFactory.createTitledBorder("Instructions"));
        add(instructions, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 8, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Parameters"));
        functionField = createField(inputPanel, "Function f(x):", "x^3 - x - 2");
        aField = createField(inputPanel, "Interval Start a:", "1.0");
        bField = createField(inputPanel, "Interval End b:", "2.0");
        tolField = createField(inputPanel, "Tolerance:", "1e-5");
        maxIterField = createField(inputPanel, "Max Iterations:", "20");

        JButton computeButton = new JButton("Compute");
        JButton helpButton = new JButton("Help");

        inputPanel.add(computeButton);
        inputPanel.add(helpButton);
        add(inputPanel, BorderLayout.WEST);

        // Table
        String[] columns = {"Iter", "xl", "xu", "xr", "Error", "f(xl)", "f(xr)", "f(xu)", "f(xl)*f(xu)"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Iteration Table"));
        add(tableScroll, BorderLayout.CENTER);

        // Actions
        computeButton.addActionListener(e -> runRegulaFalsi());
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

    private void runRegulaFalsi() {
        tableModel.setRowCount(0);
        roots.clear();
        try {
            String fxExpr = functionField.getText().trim().replaceAll("\\^", "**"); // Replace ^ with ** for exp4j
            fxExpr = fxExpr.replaceAll("\\*\\*", "^"); // Actually, exp4j uses ^, so keep this!

            double a = Double.parseDouble(aField.getText().trim());
            double b = Double.parseDouble(bField.getText().trim());
            double tol = Double.parseDouble(tolField.getText().trim());
            int maxIter = Integer.parseInt(maxIterField.getText().trim());

            Expression fx = new ExpressionBuilder(fxExpr).variable("x").build();
            double fa = fx.setVariable("x", a).evaluate();
            double fb = fx.setVariable("x", b).evaluate();

            if (fa * fb >= 0) {
                JOptionPane.showMessageDialog(this, "Error: f(a) and f(b) must have opposite signs.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int iter = 1;
            double error = Double.MAX_VALUE;
            double xr = 0;

            while (iter <= maxIter && error > tol) {
                fa = fx.setVariable("x", a).evaluate();
                fb = fx.setVariable("x", b).evaluate();
                xr = b - (fb * (a - b)) / (fa - fb);
                double fxr = fx.setVariable("x", xr).evaluate();
                error = Math.abs(fxr);
                double product = fa * fb;

                tableModel.addRow(new Object[]{
                    iter, String.format("%.8f", a), String.format("%.8f", b), String.format("%.8f", xr),
                    String.format("%.8f", error),
                    String.format("%.8f", fa), String.format("%.8f", fxr), String.format("%.8f", fb),
                    String.format("%.8f", product)
                });

                if (Math.abs(fxr) < tol) break;

                if (fa * fxr < 0) {
                    b = xr;
                } else {
                    a = xr;
                }

                iter++;
            }

            roots.add(xr);
            JOptionPane.showMessageDialog(this, String.format("[OK] Root found at x ≈ %.8f", xr), "Result", JOptionPane.INFORMATION_MESSAGE);
            showPlot(fx, a, b, roots);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPlot(Expression fx, double a, double b, ArrayList<Double> roots) {
        XYSeries seriesF = new XYSeries("f(x)");

        double xMin = a - 2, xMax = b + 2;
        for (double x = xMin; x <= xMax; x += 0.01) {
            try {
                seriesF.add(x, fx.setVariable("x", x).evaluate());
            } catch (Exception e) {
                // Skip invalid evaluations
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection(seriesF);
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Regula Falsi Method: Root Finding",
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

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, Color.BLUE);
        plot.setRenderer(renderer);

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
        }

        SwingUtilities.invokeLater(() -> {
            JFrame plotFrame = new JFrame("Regula Falsi Plot");
            plotFrame.setSize(900, 600);
            plotFrame.add(new ChartPanel(chart));
            plotFrame.setLocationRelativeTo(null);
            plotFrame.setVisible(true);
        });
    }

    private void showHelpDialog() {
        String message = """
            🌟 Regula Falsi Method Overview 🌟

            - The Regula Falsi Method is a bracketing technique for root finding.
            - Requires initial guesses a and b such that f(a)*f(b) < 0.
            - Formula: xr = b - fb*(a-b)/(fa-fb)

            Supported functions: sin, cos, exp, log, sqrt, etc.
            Use '^' for powers (e.g., x^3).

            📌 Example:
            Function: x^3 - x - 2
            a: 1.0
            b: 2.0
            Tolerance: 1e-5
            Max Iterations: 20
            """;
        JOptionPane.showMessageDialog(this, message, "Regula Falsi Method Help", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RegulaFalsiMethodSolver::new);
    }
}
