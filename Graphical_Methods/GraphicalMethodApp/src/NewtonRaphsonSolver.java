import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import net.objecthunter.exp4j.*;
import org.jfree.chart.*;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;

public class NewtonRaphsonSolver extends JFrame {
    private JTextField functionField, derivativeField, x0Field, tolField, maxIterField;
    private JTable table;
    private DefaultTableModel tableModel;
    private ArrayList<Double> roots = new ArrayList<>();

    public NewtonRaphsonSolver() {
        setTitle("Newton-Raphson Method Solver");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Instructions Panel
        JTextArea instructions = new JTextArea(
            "📚 Newton-Raphson Method Solver\n" +
            "Instructions:\n" +
            "- Enter f(x) and f'(x) using math-like syntax (e.g., x^3 - x - 2).\n" +
            "- Supported functions: sin(x), cos(x), tan(x), log(x), ln(x), exp(x), abs(x), etc.\n" +
            "- For powers, use ^ (e.g., x^3 for x cubed).\n" +
            "- Provide an initial guess x0, a tolerance (e.g., 1e-5), and max iterations.\n" +
            "- Click 'Compute' to find the roots.\n"
        );
        instructions.setEditable(false);
        instructions.setBackground(new Color(240, 240, 240));
        instructions.setBorder(BorderFactory.createTitledBorder("Instructions"));
        add(instructions, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(7, 2, 8, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Parameters"));
        functionField = createField(inputPanel, "Function f(x):", "x^3 - x - 2");
        derivativeField = createField(inputPanel, "Derivative f'(x):", "3*x^2 - 1");
        x0Field = createField(inputPanel, "Initial Guess (x0):", "1.5");
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

        computeButton.addActionListener(e -> runNewtonRaphson());

        helpButton.addActionListener(e -> showHelp());

        setVisible(true);
    }

    private JTextField createField(JPanel panel, String label, String defaultText) {
        JLabel lbl = new JLabel(label);
        JTextField field = new JTextField(defaultText);
        panel.add(lbl);
        panel.add(field);
        return field;
    }

    private void runNewtonRaphson() {
        tableModel.setRowCount(0);
        roots.clear();
        try {
            String fxExpr = functionField.getText().trim();
            String dfxExpr = derivativeField.getText().trim();
            double x0 = Double.parseDouble(x0Field.getText().trim());
            double tol = Double.parseDouble(tolField.getText().trim());
            int maxIter = Integer.parseInt(maxIterField.getText().trim());

            Expression fx = new ExpressionBuilder(fxExpr).variable("x").build();
            Expression dfx = new ExpressionBuilder(dfxExpr).variable("x").build();

            double x = x0, error = Double.MAX_VALUE;
            int iter = 1;

            while (iter <= maxIter && error > tol) {
                double fxVal = fx.setVariable("x", x).evaluate();
                double dfxVal = dfx.setVariable("x", x).evaluate();

                if (Math.abs(dfxVal) < 1e-12) {
                    JOptionPane.showMessageDialog(this, "Derivative too close to zero at x = " + x, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double xNext = x - fxVal / dfxVal;
                error = Math.abs(xNext - x);

                tableModel.addRow(new Object[]{
                    iter, String.format("%.8f", x), String.format("%.8f", fxVal), String.format("%.8f", error)
                });

                x = xNext;
                iter++;
            }

            roots.add(x);
            JOptionPane.showMessageDialog(this, String.format("[OK] Root found at x ≈ %.8f", x), "Result", JOptionPane.INFORMATION_MESSAGE);

            showPlot(fx, x0);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPlot(Expression fx, double x0) {
        XYSeries seriesF = new XYSeries("f(x)");

        double xMin = x0 - 5, xMax = x0 + 5;
        for (double x = xMin; x <= xMax; x += 0.01) {
            seriesF.add(x, fx.setVariable("x", x).evaluate());
        }

        XYSeriesCollection dataset = new XYSeriesCollection(seriesF);
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Newton-Raphson Method",
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

        // Add root points and labels
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
            JFrame plotFrame = new JFrame("Newton-Raphson Plot");
            plotFrame.setSize(900, 600);
            plotFrame.add(new ChartPanel(chart));
            plotFrame.setLocationRelativeTo(null);
            plotFrame.setVisible(true);
        });
    }

    private void showHelp() {
        String helpMessage = """
        📘 Newton-Raphson Method

        The Newton-Raphson method is an iterative technique for finding approximate roots of a real-valued function.

        Formula:
        x_(n+1) = x_n - f(x_n) / f'(x_n)

        How to use this solver:
        1️⃣ Enter the function f(x) in math-like syntax. Example: x^3 - x - 2
        2️⃣ Enter the derivative f'(x). Example: 3*x^2 - 1
        3️⃣ Provide an initial guess x0 (e.g., 1.5), a tolerance (e.g., 1e-5), and maximum iterations.
        4️⃣ Click 'Compute' to see the iteration steps and the root.
        5️⃣ The plot will show the function and the root(s) found.

        🔍 Supported functions: sin(x), cos(x), tan(x), log(x), ln(x), exp(x), abs(x), sqrt(x), etc.
        Use ^ for powers (e.g., x^3 for x cubed).
        """;
        JOptionPane.showMessageDialog(this, helpMessage, "Help - Newton-Raphson Method", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NewtonRaphsonSolver::new);
    }
}
