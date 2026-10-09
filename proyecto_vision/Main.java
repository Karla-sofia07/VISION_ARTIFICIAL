import javax.swing.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // se usa el aspecto por defecto
        }
        SwingUtilities.invokeLater(() -> {
            try {
                PythonBridge.get(); // arranca el motor de Python
                new LoginFrame().setVisible(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Error al iniciar",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
