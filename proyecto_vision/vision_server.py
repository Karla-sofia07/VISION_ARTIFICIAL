"""
vision_server.py  -  Motor de visión artificial (Python)

La interfaz gráfica (Java) lanza este programa y se comunica con él por
entrada/salida estándar, una línea por comando:

    COMANDO|arg1|arg2|...      (cada argumento viene codificado como URL)
    -> OK|resultado            (todo bien)
    -> ERR|mensaje             (algo falló)

Aquí vive TODO el análisis de imágenes (OpenCV / NumPy) y la base de datos
(SQLite, incluida en Python, no necesita instalar nada).

Requisitos:  pip install opencv-python numpy
"""

import sys
import os
import json
import sqlite3
import hashlib
import hmac
import secrets
import datetime
from contextlib import closing
from urllib.parse import unquote_plus

import numpy as np
import cv2

BASE = os.path.dirname(os.path.abspath(__file__))
RUTA_BD = os.path.join(BASE, "vision.db")
MAX_LADO = 800  # las fotos más grandes se reducen para poder guardarlas como registro
CARPETA_IMAGENES = os.path.join(BASE, "imagenes_guardadas")  # copias .png de lo que se guarda


class ErrorApp(Exception):
    """Error esperado que se le muestra al usuario."""


# ======================================================================
#  BASE DE DATOS
# ======================================================================
def conectar():
    return closing(sqlite3.connect(RUTA_BD))


def iniciar_bd():
    with conectar() as con:
        con.execute(
            """CREATE TABLE IF NOT EXISTS usuarios (
                   id      INTEGER PRIMARY KEY AUTOINCREMENT,
                   usuario TEXT NOT NULL UNIQUE,
                   sal     TEXT NOT NULL,
                   hash    TEXT NOT NULL,
                   creado  TEXT NOT NULL)"""
        )
        con.execute(
            """CREATE TABLE IF NOT EXISTS imagenes (
                   id               INTEGER PRIMARY KEY AUTOINCREMENT,
                   usuario_id       INTEGER NOT NULL REFERENCES usuarios(id),
                   nombre           TEXT NOT NULL,
                   preprocesamiento TEXT NOT NULL,
                   ancho            INTEGER NOT NULL,
                   alto             INTEGER NOT NULL,
                   pixeles          TEXT NOT NULL,
                   fecha            TEXT NOT NULL)"""
        )
        con.commit()


def _hash(password, sal):
    return hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), sal, 120_000).hex()


def registrar(usuario, password):
    usuario = usuario.strip()
    if len(usuario) < 3:
        raise ErrorApp("El usuario debe tener al menos 3 caracteres")
    if len(password) < 4:
        raise ErrorApp("La contraseña debe tener al menos 4 caracteres")
    sal = secrets.token_bytes(16)
    with conectar() as con:
        try:
            con.execute(
                "INSERT INTO usuarios (usuario, sal, hash, creado) VALUES (?,?,?,?)",
                (usuario, sal.hex(), _hash(password, sal),
                 datetime.datetime.now().isoformat(timespec="seconds")),
            )
            con.commit()
        except sqlite3.IntegrityError:
            raise ErrorApp("Ese usuario ya existe")
    return "Usuario registrado"


def login(usuario, password):
    with conectar() as con:
        fila = con.execute(
            "SELECT sal, hash FROM usuarios WHERE usuario = ?", (usuario.strip(),)
        ).fetchone()
    if fila is None or not hmac.compare_digest(_hash(password, bytes.fromhex(fila[0])), fila[1]):
        raise ErrorApp("Usuario o contraseña incorrectos")
    return "Bienvenido"


def _limpiar_nombre(texto):
    """Quita extensión y caracteres no válidos para usarlo como nombre de archivo."""
    texto = os.path.splitext(texto)[0]
    texto = "".join(c if (c.isalnum() or c in "-_") else "_" for c in texto.replace(" ", "_"))
    return texto.strip("_") or "imagen"


def guardar(usuario, nombre, preprocesamiento, ruta):
    """Guarda la imagen descompuesta en píxeles: [[B,G,R], [B,G,R], ...]
    y además una copia .png en la carpeta 'imagenes_guardadas' del proyecto.
    Regresa 'id|ruta_del_png'."""
    img = leer(ruta)
    alto, ancho = img.shape[:2]
    pixeles = json.dumps(img.reshape(-1, 3).tolist())
    with conectar() as con:
        fila = con.execute("SELECT id FROM usuarios WHERE usuario = ?", (usuario,)).fetchone()
        if fila is None:
            raise ErrorApp("Usuario no encontrado")
        cur = con.execute(
            """INSERT INTO imagenes
               (usuario_id, nombre, preprocesamiento, ancho, alto, pixeles, fecha)
               VALUES (?,?,?,?,?,?,?)""",
            (fila[0], nombre, preprocesamiento, ancho, alto, pixeles,
             datetime.datetime.now().isoformat(timespec="seconds")),
        )
        con.commit()
        id_registro = cur.lastrowid

    # Copia de la imagen en la carpeta del proyecto
    os.makedirs(CARPETA_IMAGENES, exist_ok=True)
    archivo = "%s_%s_%s_%d.png" % (
        _limpiar_nombre(usuario), _limpiar_nombre(nombre),
        _limpiar_nombre(preprocesamiento), id_registro)
    ruta_png = os.path.join(CARPETA_IMAGENES, archivo)
    escribir(ruta_png, img)
    return "%d|%s" % (id_registro, ruta_png)


def leer(ruta):
    if not os.path.isfile(ruta):
        raise ErrorApp("No existe el archivo: " + ruta)
    datos = np.fromfile(ruta, dtype=np.uint8)
    img = cv2.imdecode(datos, cv2.IMREAD_COLOR)
    if img is None:
        raise ErrorApp("No se pudo leer la imagen (formato no válido)")
    return img


def escribir(ruta, img):
    ext = os.path.splitext(ruta)[1] or ".png"
    ok, buf = cv2.imencode(ext, img)
    if not ok:
        raise ErrorApp("No se pudo escribir la imagen")
    buf.tofile(ruta)


def cargar(ruta, salida):
    img = leer(ruta)
    alto, ancho = img.shape[:2]
    if max(alto, ancho) > MAX_LADO:
        esc = MAX_LADO / max(alto, ancho)
        img = cv2.resize(img, (int(ancho * esc), int(alto * esc)), interpolation=cv2.INTER_AREA)
    escribir(salida, img)
    return "%dx%d" % (img.shape[1], img.shape[0])


def gris_ecuacion(img):
    """Gris = 0.299 R + 0.587 G + 0.114 B  (con la ecuación, NO con cvtColor)."""
    b = img[:, :, 0].astype(np.float32)
    g = img[:, :, 1].astype(np.float32)
    r = img[:, :, 2].astype(np.float32)
    gris = np.clip(0.299 * r + 0.587 * g + 0.114 * b, 0, 255).astype(np.uint8)
    return cv2.merge((gris, gris, gris))  


def negativo(img):
    return 255 - img


def hsv(img):
    return cv2.cvtColor(img, cv2.COLOR_BGR2HSV)


def gamma(img, valor):
    valor = max(0.0, min(2.0, valor))
    return np.uint8(np.clip(np.power(img / 255.0, valor) * 255, 0, 255))


def destacar(img, color):
    """Deja el color pedido a color y todo lo demás en escala de grises."""
    img_hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    if color == "rojo":
        m1 = cv2.inRange(img_hsv, np.array([0, 100, 20], np.uint8), np.array([8, 255, 255], np.uint8))
        m2 = cv2.inRange(img_hsv, np.array([175, 100, 20], np.uint8), np.array([179, 255, 255], np.uint8))
        mascara = cv2.add(m1, m2)
    elif color == "verde":
        mascara = cv2.inRange(img_hsv, np.array([35, 100, 20], np.uint8), np.array([85, 255, 255], np.uint8))
    elif color == "azul":
        mascara = cv2.inRange(img_hsv, np.array([100, 100, 20], np.uint8), np.array([125, 255, 255], np.uint8))
    else:
        raise ErrorApp("Color desconocido: " + color)
    fondo_gris = gris_ecuacion(img)            # lo que NO es del color -> gris
    mascara3 = (mascara > 0)[:, :, np.newaxis]  # máscara de 1 canal -> 3 canales
    return np.where(mascara3, img, fondo_gris)


def capa(img, canal):
    """Muestra una sola capa de color (R, G o B) con su color real."""
    b, g, r = cv2.split(img)
    z = np.zeros_like(b)
    if canal == "R":
        return cv2.merge((z, z, r))
    if canal == "G":
        return cv2.merge((z, g, z))
    if canal == "B":
        return cv2.merge((b, z, z))
    raise ErrorApp("Capa desconocida: " + canal)


def procesar(op, param, entrada, salida):
    img = leer(entrada)
    if op == "original":
        res = img
    elif op == "gris":
        res = gris_ecuacion(img)
    elif op == "hsv":
        res = hsv(img)
    elif op == "negativa":
        res = negativo(img)
    elif op == "gamma":
        res = gamma(img, float(param))
    elif op in ("rojo", "verde", "azul"):
        res = destacar(img, op)
    elif op == "capa":
        res = capa(img, param)
    else:
        raise ErrorApp("Operación desconocida: " + op)
    escribir(salida, res)
    return "ok"


_cap = None


def cam_on():
    global _cap
    if _cap is not None and _cap.isOpened():
        return "ya estaba encendida"
    _cap = cv2.VideoCapture(0, cv2.CAP_DSHOW) if os.name == "nt" else cv2.VideoCapture(0)
    if not _cap.isOpened():
        _cap = None
        raise ErrorApp("No se pudo abrir la cámara")
    return "ok"


def cam_frame(salida):
    if _cap is None:
        raise ErrorApp("La cámara está apagada")
    ok, frame = _cap.read()
    if not ok:
        raise ErrorApp("No se pudo leer el frame de la cámara")
    escribir(salida, frame)
    return "ok"


def cam_off():
    global _cap
    if _cap is not None:
        _cap.release()
        _cap = None
    return "ok"


COMANDOS = {
    "REGISTRAR": registrar,
    "LOGIN": login,
    "CARGAR": cargar,
    "PROCESAR": procesar,
    "GUARDAR": guardar,
    "CAM_ON": cam_on,
    "CAM_FRAME": cam_frame,
    "CAM_OFF": cam_off,
}


def main():
    sys.stdin.reconfigure(encoding="utf-8")
    sys.stdout.reconfigure(encoding="utf-8")
    iniciar_bd()
    print("OK|listo", flush=True)

    for linea in sys.stdin:
        linea = linea.strip()
        if not linea:
            continue
        try:
            partes = [unquote_plus(p) for p in linea.split("|")]
            cmd, args = partes[0].upper(), partes[1:]
            if cmd == "SALIR":
                print("OK|adios", flush=True)
                break
            if cmd not in COMANDOS:
                raise ErrorApp("Comando desconocido: " + cmd)
            print("OK|" + COMANDOS[cmd](*args), flush=True)
        except ErrorApp as e:
            print("ERR|" + str(e).replace("\n", " "), flush=True)
        except Exception as e: 
            print("ERR|Error interno: %s" % str(e).replace("\n", " "), flush=True)

    cam_off()


if __name__ == "__main__":
    main()
