import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/** Ventana (Java, no Matplotlib) que muestra las capas de color R, G y B. */
public class CapasFrame extends JFrame {

    public CapasFrame(File base) {
        super("Separar en capas");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[][] capas = {{"R", "Capa roja (R)"}, {"G", "Capa verde (G)"}, {"B", "Capa azul (B)"}};
        for (String[] c : capas) {
            PictureBox pb = new PictureBox("Sin imagen", 320, 240);
            pb.setBorder(BorderFactory.createTitledBorder(c[1]));
            try {
                File out = PythonBridge.temp("capa_" + c[0] + "_" + System.identityHashCode(this) + ".png");
                PythonBridge.get().procesar("capa", c[0], base.getPath(), out.getPath());
                pb.setImagen(PythonBridge.leer(out));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            panel.add(pb);
        }

        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }
}
