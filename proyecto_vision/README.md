# Proyecto de Visión Artificial (Examen 1er parcial)

Interfaz en **Java (Swing)** + análisis de imágenes en **Python (OpenCV/NumPy)** + base de datos **SQLite**.

## Requisitos
1. **JDK 11 o superior** (`javac -version` debe funcionar).
2. **Python 3.8+** con: `pip install opencv-python numpy`
3. Cámara web (solo para "Encender Cámara").

## Cómo ejecutar
- Windows: doble clic en `ejecutar.bat`
- Linux/Mac: `./ejecutar.sh`
- Manual: `javac -encoding UTF-8 *.java` y luego `java Main`
- En NetBeans / IntelliJ / VS Code: importa los .java y pon el **directorio de trabajo en la carpeta del proyecto**
  (ahí debe estar `vision_server.py`).

## Archivos
| Archivo | Qué hace |
|---|---|
| `Main.java` | Arranca Python y abre el login |
| `LoginFrame.java` / `RegistroDialog.java` | Ventana de login y de registro de usuario nuevo |
| `PrincipalFrame.java` | Buscar foto, Limpiar, Encender Cámara (vista en vivo), Tomar Foto, Preprocesamiento, Guardar |
| `PreprocesamientoFrame.java` | Separar en capas, Gris, HSV, Destacar rojo/verde/azul, Negativa, Gamma (con barra 0–2), Guardar |
| `CapasFrame.java` | Ventana de Java con las capas R, G y B |
| `PictureBox.java` | El "pictureBox" que muestra la imagen |
| `PythonBridge.java` | Comunicación Java ↔ Python |
| `vision_server.py` | Todo el procesamiento de imágenes y la base de datos |
| `imagenes_guardadas/` | Se crea sola: copias .png de todo lo que guardas |

## Qué pide el examen y dónde está
- **Login y registro** → `LoginFrame` / `RegistroDialog` (contraseñas guardadas con hash PBKDF2 + sal).
- **Preprocesamiento solo si hay foto** → el botón avisa si no hay foto cargada o tomada.
- **Guardar como registro de píxeles** → tabla `imagenes`, columna `pixeles` con el formato `[[B, G, R], [B, G, R], ...]`.
- **Gris con la ecuación, no la función** → `gris_ecuacion()`: `0.299 R + 0.587 G + 0.114 B`.
- **Gris, HSV, Negativa y Destacar color actualizan el mismo pictureBox** (no abren ventana).
- **Separar en capas abre una ventana de Java**, no Matplotlib.
- **Gamma con barra deslizadora de 0 a 2** que actualiza la imagen en vivo.
- **Guardar en preprocesamiento** guarda la imagen con el filtro aplicado (columna `preprocesamiento`) y además una copia `.png` en la carpeta `imagenes_guardadas/` del proyecto (nombre: `usuario_imagen_filtro_id.png`).
- **Destacar rojo/verde/azul**: el color elegido se ve a color y todo lo demás en escala de grises.

## Base de datos (`vision.db`, se crea sola)
- `usuarios(id, usuario, sal, hash, creado)`
- `imagenes(id, usuario_id, nombre, preprocesamiento, ancho, alto, pixeles, fecha)`

Para enseñarla en la revisión: abre `vision.db` con **DB Browser for SQLite**, o en consola:
```
sqlite3 vision.db "select id, nombre, preprocesamiento, ancho, alto, substr(pixeles,1,60) from imagenes"
```

## Notas
- Las fotos con lado mayor a 800 px se reducen al cargarlas, para que el registro de píxeles no pese cientos de MB.
- Gris, negativa y demás se guardan en 3 canales ([B,G,R]) para que todos los registros tengan el mismo formato.
- HSV se muestra y guarda tal como lo devuelve OpenCV (los valores H,S,V leídos como si fueran B,G,R), igual que en clase.
- Si el nombre de usuario ya existe, el registro avisa; si Python no se encuentra, la ventana de error lo indica.
