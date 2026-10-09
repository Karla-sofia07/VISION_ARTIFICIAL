import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/** Ventana para registrar un usuario nuevo. */
public class RegistroDialog extends JDialog {

    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JPasswordField txtConfirmar = new JPasswordField(18);

    public RegistroDialog(JFrame padre) {
        super(padre, "Registro de usuario", true);

        JButton btnRegistrar = new JButton("Registrar");
        JButton btnCancelar = new JButton("Cancelar");
        btnRegistrar.addActionListener(e -> registrar());
        btnCancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new GridLayout(1, 2, 8, 0));
        botones.add(btnRegistrar);
        botones.add(btnCancelar);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.add(new JLabel("Usuario nuevo:"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtPassword);
        panel.add(new JLabel("Confirmar contraseña:"));
        panel.add(txtConfirmar);
        panel.add(botones);

        setContentPane(panel);
        getRootPane().setDefaultButton(btnRegistrar);
        pack();
        setLocationRelativeTo(padre);
    }

    private void registrar() {
        String usuario = txtUsuario.getText().trim();
        String pass1 = new String(txtPassword.getPassword());
        String pass2 = new String(txtConfirmar.getPassword());
        if (!pass1.equals(pass2)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Revisa los datos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            PythonBridge.get().registrar(usuario, pass1);
            JOptionPane.showMessageDialog(this, "Usuario registrado. Ya puedes iniciar sesión.",
                    "Listo", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo registrar",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
