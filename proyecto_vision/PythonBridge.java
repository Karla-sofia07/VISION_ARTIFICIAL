import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Puente con Python: lanza vision_server.py una sola vez y le manda comandos.
 * Todo el análisis de imágenes y la base de datos se hacen del lado de Python.
 */
public final class PythonBridge {

    private static PythonBridge instancia;
    private static File dirTemp;

    private final Process proceso;
    private final BufferedReader in;
    private final BufferedWriter out;

    public static synchronized PythonBridge get() throws IOException {
        if (instancia == null) {
            instancia = new PythonBridge();
        }
        return instancia;
    }

    private PythonBridge() throws IOException {
        File script = new File("vision_server.py").getAbsoluteFile();
        if (!script.exists()) {
            throw new IOException("No se encontró vision_server.py en:\n" + script.getParent()
                    + "\nEjecuta el programa desde la carpeta del proyecto.");
        }

        String[][] candidatos = {{"python"}, {"py", "-3"}, {"python3"}};
        Process p = null;
        BufferedReader r = null;
        BufferedWriter w = null;

        for (String[] cmd : candidatos) {
            try {
                List<String> comando = new ArrayList<>(Arrays.asList(cmd));
                comando.add("-u");
                comando.add(script.getPath());
                ProcessBuilder pb = new ProcessBuilder(comando);
                pb.directory(script.getParentFile());
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                Process intento = pb.start();
                BufferedReader lector = new BufferedReader(
                        new InputStreamReader(intento.getInputStream(), StandardCharsets.UTF_8));
                String saludo = lector.readLine();
                if (saludo != null && saludo.startsWith("OK|")) {
                    p = intento;
                    r = lector;
                    w = new BufferedWriter(
                            new OutputStreamWriter(intento.getOutputStream(), StandardCharsets.UTF_8));
                    break;
                }
                intento.destroy();
            } catch (IOException e) {
                // ese comando no existe, probamos con el siguiente
            }
        }

        if (p == null) {
            throw new IOException("No se pudo iniciar Python.\n"
                    + "Verifica que Python esté instalado y que hayas ejecutado:\n"
                    + "pip install opencv-python numpy");
        }
        this.proceso = p;
        this.in = r;
        this.out = w;
        Runtime.getRuntime().addShutdownHook(new Thread(this::cerrar));
    }

    /** Envía un comando y regresa el texto de la respuesta (lanza IOException si Python reporta ERR). */
    public synchronized String enviar(String comando, String... args) throws IOException {
        StringBuilder sb = new StringBuilder(comando);
        for (String a : args) {
            sb.append('|').append(URLEncoder.encode(a, StandardCharsets.UTF_8));
        }
        out.write(sb.toString());
        out.newLine();
        out.flush();

        String respuesta = in.readLine();
        if (respuesta == null) {
            throw new IOException("El proceso de Python se cerró inesperadamente");
        }
        if (respuesta.startsWith("OK|")) {
            return respuesta.substring(3);
        }
        if (respuesta.startsWith("ERR|")) {
            throw new IOException(respuesta.substring(4));
        }
        throw new IOException("Respuesta inesperada de Python: " + respuesta);
    }

    // ---------------- API de alto nivel ----------------

    public void login(String usuario, String password) throws IOException {
        enviar("LOGIN", usuario, password);
    }

    public void registrar(String usuario, String password) throws IOException {
        enviar("REGISTRAR", usuario, password);
    }

    /** Lee la foto, la reduce si es muy grande y la deja como PNG. Regresa "ANCHOxALTO". */
    public String cargar(String ruta, String salida) throws IOException {
        return enviar("CARGAR", ruta, salida);
    }

    public void procesar(String operacion, String parametro, String entrada, String salida)
            throws IOException {
        enviar("PROCESAR", operacion, parametro, entrada, salida);
    }

    /** Guarda la imagen como registro de píxeles en la BD. Regresa el id del registro. */
    public String guardar(String usuario, String nombre, String preprocesamiento, String ruta)
            throws IOException {
        return enviar("GUARDAR", usuario, nombre, preprocesamiento, ruta);
    }

    public void camOn() throws IOException {
        enviar("CAM_ON");
    }

    public void camFrame(String salida) throws IOException {
        enviar("CAM_FRAME", salida);
    }

    public void camOff() throws IOException {
        enviar("CAM_OFF");
    }

    public synchronized void cerrar() {
        try {
            out.write("SALIR");
            out.newLine();
            out.flush();
        } catch (IOException ignorada) {
            // ya estaba cerrado
        }
        proceso.destroy();
    }

    // ---------------- utilidades ----------------

    public static synchronized File temp(String nombre) {
        if (dirTemp == null) {
            dirTemp = new File(System.getProperty("java.io.tmpdir"), "vision_app");
            dirTemp.mkdirs();
        }
        return new File(dirTemp, nombre);
    }

    public static BufferedImage leer(File f) throws IOException {
        BufferedImage img = ImageIO.read(f);
        if (img == null) {
            throw new IOException("No se pudo mostrar la imagen: " + f.getName());
        }
        return img;
    }
}
