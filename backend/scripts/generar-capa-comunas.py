#!/usr/bin/env python3
"""Convierte un GeoJSON de las 10 comunas en una migración SQL que carga comuna.geom.

Sirve para cambiar la capa sin tocar código: cuando llegue la capa oficial (o una mejor),

    python3 backend/scripts/generar-capa-comunas.py capa-nueva.geojson \
        --fuente "Alcaldía de Tuluá, Planeación, 2026" \
        > backend/src/main/resources/db/migration/V15__capa_comunas_oficial.sql

El SQL generado reemplaza la geometría de las 10 comunas (UPDATE por id_comuna, repetible) y
anota la fuente en configuracion.comunas_capa_fuente. También se puede ejecutar a mano con psql.

Opciones:
  --propiedad  nombre de la propiedad con el número de comuna (por defecto COD_LOC_COM)
  --fuente     texto que queda en configuracion.comunas_capa_fuente (máx. 500 caracteres)
"""
import argparse
import hashlib
import json
import sys


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("geojson")
    ap.add_argument("--propiedad", default="COD_LOC_COM")
    ap.add_argument("--fuente", required=True)
    args = ap.parse_args()

    with open(args.geojson, "rb") as f:
        crudo = f.read()
    datos = json.loads(crudo)
    if datos.get("type") != "FeatureCollection":
        sys.exit("El archivo debe ser un FeatureCollection de GeoJSON (en EPSG:4326, lon/lat).")

    comunas = {}
    for feat in datos["features"]:
        numero = int(feat["properties"][args.propiedad])
        tipo = feat["geometry"]["type"]
        if tipo not in ("Polygon", "MultiPolygon"):
            sys.exit(f"Comuna {numero}: la geometría es {tipo}; se espera Polygon o MultiPolygon.")
        if numero in comunas:
            sys.exit(f"Comuna {numero} repetida en el archivo.")
        comunas[numero] = feat["geometry"]
    if sorted(comunas) != list(range(1, 11)):
        sys.exit(f"Se esperan las comunas 1 a 10; el archivo trae: {sorted(comunas)}")
    if len(args.fuente) > 500:
        sys.exit("--fuente admite máximo 500 caracteres.")
    if "'" in args.fuente or "$" in args.fuente:
        sys.exit("--fuente no debe llevar comillas simples ni el signo $.")

    sha = hashlib.sha256(crudo).hexdigest()
    sal = [
        "-- Capa de comunas generada con backend/scripts/generar-capa-comunas.py (no editar a mano).",
        f"-- Archivo origen: {args.geojson.split('/')[-1]}  sha256: {sha}",
        f"-- Fuente: {args.fuente}",
        "-- Para cambiar la capa: volver a generar con el GeoJSON nuevo y guardarlo como una migración",
        "-- nueva (V15, V16...). Nunca se edita una migración ya aplicada.",
        "",
    ]
    for numero in range(1, 11):
        # El espacio tras $geo$ evita la secuencia ${ que Flyway toma por una variable (placeholder).
        geo = json.dumps(comunas[numero], separators=(",", ":"))
        sal.append(
            f"UPDATE comuna SET geom = ST_Multi(ST_CollectionExtract(ST_MakeValid("
            f"ST_SetSRID(ST_GeomFromGeoJSON($geo$ {geo} $geo$), 4326)), 3)) WHERE id_comuna = {numero};"
        )
    sal += [
        "",
        "INSERT INTO configuracion (clave, valor) VALUES ('comunas_capa_fuente', '" + args.fuente + "')",
        "ON CONFLICT (clave) DO UPDATE SET valor = EXCLUDED.valor, actualizado_en = now();",
        "",
    ]
    sys.stdout.write("\n".join(sal))


if __name__ == "__main__":
    main()
