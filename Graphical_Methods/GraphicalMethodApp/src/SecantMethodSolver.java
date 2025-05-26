import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import net.objecthunter.exp4j.*;
import net.objecthunter.exp4j.tokenizer.UnknownFunctionOrVariableException;
import org.jfree.chart.*;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;

public class SecantMethodSolver extends JFrame {
    private JTextField functionField, x0Field, x1Field, tolField, maxIterField;
    private JTable table;
    private DefaultTableModel tableModel;
    private ArrayList<Double> roots = new ArrayList<>();

    public SecantMethodSolver() {
        setTitle("Secant Method Solver");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Instructions Panel
        JTextArea instructions = new JTextArea(
            "📚 Secant Method Solver\n" +
            "Instructions:\n" +
            "- Enter f(x) using standard math syntax (e.g., x^3 - x - 2).\n" +
            "- Supported functions: sin, cos, tan, exp, log, abs, sqrt, etc.\n" +
            "- Use ^ for exponents (e.g., x^3 means x cubed).\n" +
            "- Provide two initial guesses x0 and x1, a tolerance, and max iterations.\n" +
            "- Click 'Compute' to find the roots.\n"
        );
        instructions.setEditable(false);
        instructions.setBackground(new Color(240, 240, 240));
        instructions.setBorder(BorderFactory.createTitledBorder("Instructions"));
        add(instructions, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 8, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Parameters"));
        functionField = createField(inputPanel, "Function f(x):", "x^3 - x - 2");
        x0Field = createField(inputPanel, "Initial Guess x0:", "1.0");
        x1Field = createField(inputPanel, "Initial Guess x1:", "2.0");
        tolField = createField(inputPanel, "Tolerance:", "1e-5");
        maxIterField = createField(inputPanel, "Max Iterations:", "20");

        JButton computeButton = new JButton("Compute");
        JButton helpButton = new JButton("Help");

        inputPanel.add(computeButton);
        inputPanel.add(helpButton);
        add(inputPanel, BorderLayout.WEST);

        // Iteration Table
        String[] columns = {"Iteration", "x", "f(x)", "Error"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Iteration Table"));
        add(tableScroll, BorderLayout.CENTER);

        // Actions
        computeButton.addActionListener(e -> runSecantMethod());
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

    private void runSecantMethod() {
        tableModel.setRowCount(0);
        roots.clear();
        try {
            String fxExpr = functionField.getText().trim();
            // Warn user if they accidentally use Math.pow
            if (fxExpr.contains("Math.pow")) {
                JOptionPane.showMessageDialog(this,
                        "⚠️ Use ^ for exponents, not Math.pow!\nExample: x^3 - x - 2",
                        "Syntax Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Replace ^ with ** for exp4j (it supports ^ for exponents)
            Expression fx = new ExpressionBuilder(fxExpr)
                                .variable("x")
                                .build();

            double x0 = Double.parseDouble(x0Field.getText().trim());
            double x1 = Double.parseDouble(x1Field.getText().trim());
            double tol = Double.parseDouble(tolField.getText().trim());
            int maxIter = Integer.parseInt(maxIterField.getText().trim());

            double error = Double.MAX_VALUE;
            int iter = 1;

            while (iter <= maxIter && error > tol) {
                double fx0 = fx.setVariable("x", x0).evaluate();
                double fx1 = fx.setVariable("x", x1).evaluate();

                if (Math.abs(fx1 - fx0) < 1e-12) {
                    JOptionPane.showMessageDialog(this, "Error: Division by zero in Secant formula.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double x2 = x1 - fx1 * (x1 - x0) / (fx1 - fx0);
                double fx2 = fx.setVariable("x", x2).evaluate();
                error = Math.abs(x2 - x1);

                tableModel.addRow(new Object[] {
                    iter, String.format("%.8f", x2), String.format("%.8f", fx2), String.format("%.8f", error)
                });

                x0 = x1;
                x1 = x2;
                iter++;
            }

            roots.add(x1);
            JOptionPane.showMessageDialog(this, String.format("[OK] Root found at x ≈ %.8f", x1), "Result", JOptionPane.INFORMATION_MESSAGE);

            showPlot(fx, x1);

        } catch (UnknownFunctionOrVariableException ufve) {
            JOptionPane.showMessageDialog(this, "Error: Unknown function or variable in your expression.\n" +
                    "Use functions like sin(x), cos(x), exp(x), log(x), etc.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPlot(Expression fx, double xApprox) {
        XYSeries seriesF = new XYSeries("f(x)");

        double xMin = xApprox - 5, xMax = xApprox + 5;
        for (double x = xMin; x <= xMax; x += 0.01) {
            seriesF.add(x, fx.setVariable("x", x).evaluate());
        }

        XYSeriesCollection dataset = new XYSeriesCollection(seriesF);
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Secant Method: Root Finding",
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

            XYTextAnnotation annotation = new XYTextAnnotation(String.format("%.5f", root), root, 0.05);
            annotation.setFont(new Font("SansSerif", Font.BOLD, 12));
            annotation.setPaint(Color.RED);
            plot.addAnnotation(annotation);
        }

        SwingUtilities.invokeLater(() -> {
            JFrame plotFrame = new JFrame("Secant Method Plot");
            plotFrame.setSize(900, 600);
            plotFrame.add(new ChartPanel(chart));
            plotFrame.setLocationRelativeTo(null);
            plotFrame.setVisible(true);
        });
    }

    private void showHelpDialog() {
        String message = """
            🌟 Secant Method Overview 🌟

            - The Secant Method is an iterative technique for finding roots of nonlinear equations.
            - Formula: x_{n+1} = x_n - f(x_n) * (x_n - x_{n-1}) / (f(x_n) - f(x_{n-1}))
            - Provide two initial guesses (x0, x1), tolerance, and max iterations.

            📝 Function Syntax:
            - Use standard math functions: x^3 - x - 2
            - Supported functions: sin(x), cos(x), tan(x), exp(x), log(x), abs(x), sqrt(x), etc.
            - Use ^ for exponents (e.g., x^3 for x cubed).

            📌 Example:
            Function: x^3 - x - 2
            x0: 1.0
            x1: 2.0
            Tolerance: 1e-5
            Max Iterations: 20
            """;
        JOptionPane.showMessageDialog(this, message, "Secant Method Help", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SecantMethodSolver::new);
    }
}
