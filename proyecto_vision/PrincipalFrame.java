import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** GUI principal: buscar foto, cámara, tomar foto, guardar y abrir el preprocesamiento. */
public class PrincipalFrame extends JFrame {

    private final String usuario;
    private final PictureBox pictureBox = new PictureBox();
    private final JLabel lblEstado = new JLabel("Busca una foto o enciende la cámara");

    private final JButton btnBuscar = new JButton("Buscar foto");
    private final JButton btnLimpiar = new JButton("Limpiar");
    private final JButton btnCamara = new JButton("Encender Cámara");
    private final JButton btnTomar = new JButton("Tomar Foto");
    private final JButton btnPre = new JButton("Preprocesamiento");
    private final JButton btnGuardar = new JButton("Guardar");

    private final Timer timerCamara;
    private boolean camaraEncendida = false;

    private File imagenBase;      // foto lista para procesar y guardar
    private String nombreImagen;  // nombre con el que se guarda en la BD

    public PrincipalFrame(String usuario) {
        super("Visión Artificial - Usuario: " + usuario);
        this.usuario = usuario;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        timerCamara = new Timer(90, e -> actualizarFrame());

        btnBuscar.addActionListener(e -> buscarFoto());
        btnLimpiar.addActionListener(e -> limpiar());
        btnCamara.addActionListener(e -> alternarCamara());
        btnTomar.addActionListener(e -> tomarFoto());
        btnPre.addActionListener(e -> abrirPreprocesamiento());
        btnGuardar.addActionListener(e -> guardar());

        JPanel botones = new JPanel(new GridLayout(0, 1, 6, 6));
        botones.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        botones.add(btnBuscar);
        botones.add(btnLimpiar);
        botones.add(btnCamara);
        botones.add(btnTomar);
        botones.add(btnPre);
        botones.add(btnGuardar);

        JPanel lateral = new JPanel(new BorderLayout());
        lateral.add(botones, BorderLayout.NORTH);

        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.add(pictureBox, BorderLayout.CENTER);
        contenido.add(lateral, BorderLayout.EAST);
        contenido.add(lblEstado, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    private void buscarFoto() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png", "bmp", "tif", "tiff"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        apagarCamara();
        File elegido = fc.getSelectedFile();
        try {
            File salida = PythonBridge.temp("carga_" + System.currentTimeMillis() + ".png");
            String dims = PythonBridge.get().cargar(elegido.getPath(), salida.getPath());
            imagenBase = salida;
            nombreImagen = elegido.getName();
            pictureBox.setImagen(PythonBridge.leer(salida));
            lblEstado.setText("Foto cargada: " + nombreImagen + " (" + dims + " px)");
        } catch (IOException ex) {
            error(ex.getMessage());
        }
    }

    private void limpiar() {
        apagarCamara();
        pictureBox.limpiar();
        imagenBase = null;
        nombreImagen = null;
        lblEstado.setText("Busca una foto o enciende la cámara");
    }

    private void alternarCamara() {
        if (camaraEncendida) {
            apagarCamara();
            lblEstado.setText("Cámara apagada");
            return;
        }
        btnCamara.setEnabled(false);
        lblEstado.setText("Encendiendo cámara...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                PythonBridge.get().camOn();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    camaraEncendida = true;
                    imagenBase = null;
                    btnCamara.setText("Apagar Cámara");
                    lblEstado.setText("Cámara encendida. Presiona \"Tomar Foto\"");
                    timerCamara.start();
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    error(causa.getMessage());
                    lblEstado.setText("No se pudo encender la cámara");
                } finally {
                    btnCamara.setEnabled(true);
                }
            }
        }.execute();
    }

    private void actualizarFrame() {
        try {
            File f = PythonBridge.temp("live.jpg");
            PythonBridge.get().camFrame(f.getPath());
            pictureBox.setImagen(PythonBridge.leer(f));
        } catch (IOException ex) {
            apagarCamara();
            error(ex.getMessage());
        }
    }

    private void apagarCamara() {
        timerCamara.stop();
        if (camaraEncendida) {
            try {
                PythonBridge.get().camOff();
            } catch (IOException ignorada) {
                // nada que hacer
            }
            camaraEncendida = false;
        }
        btnCamara.setText("Encender Cámara");
    }

    private void tomarFoto() {
        if (!camaraEncendida) {
            aviso("Primero enciende la cámara.");
            return;
        }
        timerCamara.stop();
        try {
            String marca = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File foto = PythonBridge.temp("foto_" + System.currentTimeMillis() + ".png");
            PythonBridge.get().camFrame(foto.getPath());
            apagarCamara();
            imagenBase = foto;
            nombreImagen = "foto_camara_" + marca;
            pictureBox.setImagen(PythonBridge.leer(foto));
            lblEstado.setText("Foto tomada: " + nombreImagen);
        } catch (IOException ex) {
            apagarCamara();
            error(ex.getMessage());
        }
    }

    private void abrirPreprocesamiento() {
        if (imagenBase == null) {
            aviso("Primero busca una foto o toma una con la cámara.");
            return;
        }
        new PreprocesamientoFrame(usuario, imagenBase, nombreImagen).setVisible(true);
    }

    private void guardar() {
        if (imagenBase == null) {
            aviso("No hay ninguna foto para guardar.");
            return;
        }
        btnGuardar.setEnabled(false);
        lblEstado.setText("Guardando en la base de datos...");
        final File base = imagenBase;
        final String nombre = nombreImagen;
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return PythonBridge.get().guardar(usuario, nombre, "original", base.getPath());
            }

            @Override
            protected void done() {
                try {
                    String[] r = get().split("\\|", 2);
                    lblEstado.setText("Imagen guardada (registro #" + r[0] + ")");
                    JOptionPane.showMessageDialog(PrincipalFrame.this,
                            "Imagen guardada en la base de datos (registro #" + r[0] + ")\n"
                            + "y en la carpeta del proyecto:\n" + r[1],
                            "Guardado", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    error(causa.getMessage());
                } finally {
                    btnGuardar.setEnabled(true);
                }
            }
        }.execute();
    }

    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
