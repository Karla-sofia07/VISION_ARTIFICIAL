import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/** Ventana de login. Si no hay usuario, se puede registrar uno nuevo. */
public class LoginFrame extends JFrame {

    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);

    public LoginFrame() {
        super("Visión Artificial - Inicio de sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel titulo = new JLabel("Sistema de Visión Artificial", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JButton btnEntrar = new JButton("Iniciar sesión");
        JButton btnRegistro = new JButton("Registrarse");
        btnEntrar.addActionListener(e -> iniciarSesion());
        btnRegistro.addActionListener(e -> new RegistroDialog(this).setVisible(true));

        JPanel botones = new JPanel(new GridLayout(1, 2, 8, 0));
        botones.add(btnEntrar);
        botones.add(btnRegistro);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.add(titulo);
        panel.add(new JLabel("Usuario:"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtPassword);
        panel.add(botones);

        setContentPane(panel);
        getRootPane().setDefaultButton(btnEntrar);
        pack();
        setLocationRelativeTo(null);
    }

    private void iniciarSesion() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe tu usuario y contraseña.", "Faltan datos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            PythonBridge.get().login(usuario, password);
            new PrincipalFrame(usuario).setVisible(true);
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo iniciar sesión",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
