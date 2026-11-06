#!/usr/bin/env python3
"""Convierte un CSV de barrios (comuna, barrio) en una migración SQL que llena la tabla barrio.

El CSV necesita dos columnas con encabezado: «comuna» y «barrio» (o «nombre»), en cualquier orden:

    comuna,barrio
    1,La Floresta
    Comuna 2,Alvernia

La comuna puede escribirse 1, 01 o «Comuna 1». Separa con coma o punto y coma (Excel en español
guarda con punto y coma) y guarda el archivo en UTF-8. Uso:

    python3 backend/scripts/generar-barrios.py barrios.csv \
        > backend/src/main/resources/db/migration/V17__barrios_reales.sql

La migración solo AGREGA barrios (ON CONFLICT DO NOTHING), así que se puede volver a generar con
un CSV más completo sin duplicar. Un barrio ya cargado no se borra ni se corrige por aquí: los
usuarios y reportes lo referencian. Para corregir un nombre, escribir un UPDATE en una migración
nueva. El barrio interno del administrador (V15) no entra al CSV.
"""
import argparse
import csv
import re
import sys
import unicodedata


def sin_acentos(texto):
    return "".join(c for c in unicodedata.normalize("NFD", texto) if unicodedata.category(c) != "Mn").lower().strip()


def numero_de_comuna(valor, fila):
    m = re.fullmatch(r"(?:comuna\s*)?0*(\d{1,2})", sin_acentos(valor))
    if not m or not 1 <= int(m.group(1)) <= 10:
        sys.exit(f"Fila {fila}: la comuna «{valor}» no es válida (se espera de 1 a 10).")
    return int(m.group(1))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("csv")
    args = ap.parse_args()

    with open(args.csv, encoding="utf-8-sig", newline="") as f:
        muestra = f.read(4096)
        f.seek(0)
        separador = ";" if muestra.count(";") > muestra.count(",") else ","
        lector = csv.reader(f, delimiter=separador)
        encabezado = [sin_acentos(c) for c in next(lector, [])]
        try:
            col_comuna = encabezado.index("comuna")
            col_barrio = encabezado.index("barrio") if "barrio" in encabezado else encabezado.index("nombre")
        except ValueError:
            sys.exit("El encabezado debe tener las columnas «comuna» y «barrio».")

        vistos = {}
        filas_salida = []
        for fila, celdas in enumerate(lector, start=2):
            if not any(c.strip() for c in celdas):
                continue
            if len(celdas) <= max(col_comuna, col_barrio):
                sys.exit(f"Fila {fila}: faltan columnas.")
            comuna = numero_de_comuna(celdas[col_comuna], fila)
            nombre = re.sub(r"\s+", " ", celdas[col_barrio]).strip()
            if not nombre or len(nombre) > 100:
                sys.exit(f"Fila {fila}: el nombre del barrio está vacío o pasa de 100 caracteres.")
            clave = (comuna, sin_acentos(nombre))
            if clave in vistos:
                sys.exit(f"Fila {fila}: «{nombre}» está repetido en la comuna {comuna} (también en la fila {vistos[clave]}).")
            vistos[clave] = fila
            filas_salida.append((comuna, nombre))

    if not filas_salida:
        sys.exit("El CSV no tiene barrios.")

    valores = ",\n".join("  (%d, '%s')" % (c, n.replace("'", "''")) for c, n in sorted(filas_salida))
    sys.stdout.write(
        "-- Barrios generados con backend/scripts/generar-barrios.py (no editar a mano).\n"
        f"-- Archivo origen: {args.csv.split('/')[-1]}  ({len(filas_salida)} barrios)\n"
        "-- Solo agrega: si el barrio ya existe en esa comuna no hace nada.\n\n"
        "INSERT INTO barrio (id_comuna, nombre) VALUES\n" + valores + "\n"
        "ON CONFLICT (id_comuna, nombre) DO NOTHING;\n"
    )


if __name__ == "__main__":
    main()
