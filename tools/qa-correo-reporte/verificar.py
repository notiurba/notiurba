import json
import os
import re

SALIDA = os.environ["SALIDA"]
ENCOLADOS = json.loads(os.environ["ENCOLADOS"] or "{}")
COD_VIAS, COD_AGUA = os.environ["COD_VIAS"], os.environ["COD_AGUA"]
http = {}
with open(f"{SALIDA}/respuestas/_http.txt") as f:
    for linea in f:
        nombre, codigo, tiempo = linea.split()
        http[nombre] = (int(codigo), float(tiempo))


def cuerpo(nombre):
    with open(f"{SALIDA}/respuestas/{nombre}.json", encoding="utf-8") as f:
        texto = f.read()
    return json.loads(texto) if texto else {}


resultados = []


def check(nombre, condicion, detalle=""):
    condicion = bool(condicion)
    resultados.append(condicion)
    print(("OK    " if condicion else "FALLA ") + nombre + (f"  [{detalle}]" if detalle else ""))


def estado(nombre, esperado):
    c, t = http[nombre]
    check(f"{nombre}: HTTP {esperado}", c == esperado, f"HTTP {c}, {t:.3f} s")


def comparable(texto):
    texto = re.sub(r"Fecha del registro: [^\n]*", "Fecha del registro: —", texto)
    return re.sub(r"Fotografía \d+: [^\n]*\n", "", texto)


print("--- Rutas y canales")
estado("rutas", 200)
rutas = {r["idCategoria"]: r for r in cuerpo("rutas")}
check("14 rutas", len(rutas) == 14, len(rutas))
check("Alcaldía y Centroaguas con correo", rutas[1]["permiteCorreo"] and rutas[11]["permiteCorreo"])
check("CAIC y info@centroaguas", rutas[1]["acceso"] == "servicioalciudadano@tulua.gov.co"
      and rutas[11]["acceso"] == "info@centroaguas.com")
check("gas, energía y aseo sin correo y con canales",
      all(not rutas[c]["permiteCorreo"] and rutas[c]["canales"] for c in (12, 13, 14)))
check("aseo recomienda el WhatsApp de Veolia", rutas[14]["canales"][0]["acceso"] == "317 372 5078")
check("ningún canal de la lista es correo", all(k["tipo"] != "Correo" for r in rutas.values() for k in r["canales"]))

print("--- Vista previa")
estado("previa", 200)
previa = cuerpo("previa")
texto = previa.get("texto", "")
check("asunto formal con barrio o comuna", previa.get("asunto", "").startswith("Petición ciudadana – ")
      and "Comuna 5" in previa.get("asunto", ""), previa.get("asunto"))
check("carta en primera persona", texto.startswith("Respetados señores:") and "Yo, Ana Prueba, con cédula de ciudadanía No. 99000111" in texto)
check("sin NotiUrba ni código", "NotiUrba" not in texto and "NU-" not in texto)
check("cuenta las 2 fotos", "Evidencia: 2 fotografías." in texto)
check("secciones con su paso", {s["id"]: s["paso"] for s in previa.get("secciones", [])}.get("ubicacion") == "ubicacion")
check("la vista previa no escribe en la base", os.environ["ANTES"] == os.environ["DESPUES"],
      f"{os.environ['ANTES']} → {os.environ['DESPUES']}")
estado("previa_aseo", 200)
check("aseo: vista previa sin correo", cuerpo("previa_aseo").get("permiteCorreo") is False)
estado("previa_corta", 400)
check("descripción corta: campo descripcion", cuerpo("previa_corta").get("campo") == "descripcion")
estado("previa_sin_lugar", 400)
check("vías sin lugar: campo idBarrio", cuerpo("previa_sin_lugar").get("campo") == "idBarrio")
estado("previa_fuera", 422)
estado("previa_sin_token", 401)
check("CORS de la vista previa", os.environ["CORS"] == "200", os.environ["CORS"])

print("--- Correo encolado")
estado("crear_vias", 201)
estado("mensaje_antes", 200)
estado("enviar_vias", 202)
estado("mensaje_despues", 200)
vias = ENCOLADOS.get(COD_VIAS, {})
encolado = vias.get("mensaje", {})
check("vías va al CAIC por Resend", vias.get("destino") == "servicioalciudadano@tulua.gov.co" and vias.get("modo") == "RESEND")
check("GET /mensaje = mensaje encolado", cuerpo("mensaje_despues").get("texto") == encolado.get("texto"))
check("antes de enviar dice lo mismo", comparable(cuerpo("mensaje_antes").get("texto", "")) == comparable(encolado.get("texto", "")))
estado("previa_sin_fotos", 200)
check("vista previa = correo encolado (salvo la fecha)",
      comparable(cuerpo("previa_sin_fotos").get("texto", "")) == comparable(encolado.get("texto", "")))
check("el asunto encolado no lleva el código", COD_VIAS not in encolado.get("asunto", "x" + COD_VIAS))
check("responder a la ciudadana", encolado.get("responderA") == "ana.qa@prueba.local")
html = encolado.get("html", "")
check("HTML sin NotiUrba y con viewport", "NotiUrba" not in html and 'name="viewport"' in html)
estado("mensaje_ajeno", 404)
estado("mensaje_mal_escrito", 400)
agua = ENCOLADOS.get(COD_AGUA, {})
estado("enviar_agua", 202)
check("acueducto va a info@centroaguas (canal 13)", agua.get("destino") == "info@centroaguas.com" and agua.get("canal") == 13)
check("acueducto usa la plantilla C con matrícula", "(matrícula o cuenta No. 778899)" in agua.get("mensaje", {}).get("texto", ""))

print(f"== {sum(resultados)} de {len(resultados)} verificaciones en verde")
