import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Ventana de preprocesamiento. Todos los botones (menos "Separar en capas")
 * actualizan el mismo pictureBox, sin abrir otra ventana.
 */
public class PreprocesamientoFrame extends JFrame {

    private final String usuario;
    private final String nombre;
    private final File base;
    private final File salida;

    private File actual;
    private String etiqueta = "original";

    private final PictureBox pictureBox = new PictureBox();
    private final JSlider slider = new JSlider(0, 200, 100);   // 0.00 a 2.00
    private final JLabel lblGamma = new JLabel("Gamma: 1.00");
    private final JPanel panelGamma = new JPanel(new BorderLayout(10, 0));
    private final Timer debounceGamma;

    public PreprocesamientoFrame(String usuario, File base, String nombre) {
        super("Preprocesamiento - " + nombre);
        this.usuario = usuario;
        this.base = base;
        this.nombre = nombre;
        this.actual = base;
        this.salida = PythonBridge.temp("proc_" + System.identityHashCode(this) + ".png");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        debounceGamma = new Timer(40, e -> aplicarGamma());
        debounceGamma.setRepeats(false);
        slider.addChangeListener(e -> {
            lblGamma.setText(String.format(Locale.US, "Gamma: %.2f", slider.getValue() / 100.0));
            debounceGamma.restart();
        });

        panelGamma.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        panelGamma.add(lblGamma, BorderLayout.WEST);
        panelGamma.add(slider, BorderLayout.CENTER);
        panelGamma.setVisible(false);

        JPanel botones = new JPanel(new GridLayout(0, 1, 6, 6));
        botones.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        botones.add(boton("Separar en capas", () -> new CapasFrame(base).setVisible(true)));
        botones.add(boton("Gris", () -> aplicar("gris", "", "gris", false)));
        botones.add(boton("HSV", () -> aplicar("hsv", "", "hsv", false)));
        botones.add(boton("Destacar color rojo", () -> aplicar("rojo", "", "destacar rojo", false)));
        botones.add(boton("Destacar color verde", () -> aplicar("verde", "", "destacar verde", false)));
        botones.add(boton("Destacar color azul", () -> aplicar("azul", "", "destacar azul", false)));
        botones.add(boton("Negativa", () -> aplicar("negativa", "", "negativa", false)));
        botones.add(boton("Gamma", this::aplicarGammaVisible));
        botones.add(boton("Original", () -> aplicar("original", "", "original", false)));
        botones.add(boton("Guardar", this::guardar));

        JPanel lateral = new JPanel(new BorderLayout());
        lateral.add(botones, BorderLayout.NORTH);

        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.add(pictureBox, BorderLayout.CENTER);
        contenido.add(lateral, BorderLayout.EAST);
        contenido.add(panelGamma, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(null);

        try {
            pictureBox.setImagen(PythonBridge.leer(base));
        } catch (IOException ex) {
            error(ex.getMessage());
        }
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    /** Aplica una operación sobre la foto original y actualiza el pictureBox. */
    private void aplicar(String operacion, String parametro, String nuevaEtiqueta, boolean conservarGamma) {
        try {
            if (!conservarGamma) {
                panelGamma.setVisible(false);
                revalidate();
            }
            PythonBridge.get().procesar(operacion, parametro, base.getPath(), salida.getPath());
            pictureBox.setImagen(PythonBridge.leer(salida));
            actual = salida;
            etiqueta = nuevaEtiqueta;
        } catch (IOException ex) {
            error(ex.getMessage());
        }
    }

    private void aplicarGammaVisible() {
        panelGamma.setVisible(true);
        revalidate();
        aplicarGamma();
    }

    private void aplicarGamma() {
        double g = slider.getValue() / 100.0;
        aplicar("gamma", String.format(Locale.US, "%.2f", g),
                String.format(Locale.US, "gamma %.2f", g), true);
    }

    private void guardar() {
        try {
            String id = PythonBridge.get().guardar(usuario, nombre, etiqueta, actual.getPath());
            JOptionPane.showMessageDialog(this,
                    "Imagen (" + etiqueta + ") guardada en la base de datos (registro #" + id + ").",
                    "Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            error(ex.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
