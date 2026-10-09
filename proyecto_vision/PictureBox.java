import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Equivalente al pictureBox: muestra una imagen ajustada al tamaño disponible. */
public class PictureBox extends JPanel {

    private BufferedImage imagen;
    private final String textoVacio;

    public PictureBox() {
        this("Sin imagen", 640, 480);
    }

    public PictureBox(String textoVacio, int ancho, int alto) {
        this.textoVacio = textoVacio;
        setBackground(new Color(35, 35, 35));
        setPreferredSize(new Dimension(ancho, alto));
    }

    public void setImagen(BufferedImage img) {
        this.imagen = img;
        repaint();
    }

    public void limpiar() {
        this.imagen = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (imagen == null) {
            g2.setColor(Color.GRAY);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(textoVacio)) / 2;
            int y = (getHeight() + fm.getAscent()) / 2;
            g2.drawString(textoVacio, x, y);
        } else {
            double esc = Math.min((double) getWidth() / imagen.getWidth(),
                                  (double) getHeight() / imagen.getHeight());
            int w = Math.max(1, (int) (imagen.getWidth() * esc));
            int h = Math.max(1, (int) (imagen.getHeight() * esc));
            g2.drawImage(imagen, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
        }
        g2.dispose();
    }
}
