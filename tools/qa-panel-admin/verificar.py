import json
import os

SALIDA = os.environ["SALIDA"]
ORACULO = json.loads(os.environ["ORACULO"])
http = {}
with open(f"{SALIDA}/respuestas/_http.txt") as f:
    for linea in f:
        nombre, codigo, tiempo = linea.split()
        http[nombre] = (int(codigo), float(tiempo))


def cuerpo(nombre):
    with open(f"{SALIDA}/respuestas/{nombre}.json") as f:
        return json.load(f)


resultados = []


def check(nombre, condicion, detalle=""):
    condicion = bool(condicion)
    resultados.append(condicion)
    print(("OK    " if condicion else "FALLA ") + nombre + (f"  [{detalle}]" if detalle else ""))


def estado(nombre, esperado, codigo=None, campo="sin-campo"):
    c, t = http[nombre]
    ok = c == esperado
    det = f"HTTP {c}, {t:.3f} s"
    if codigo:
        b = cuerpo(nombre)
        ok = ok and b.get("codigo") == codigo and bool(b.get("mensaje"))
        if campo != "sin-campo":
            ok = ok and b.get("campo") == campo
        det += f", codigo={b.get('codigo')}, campo={b.get('campo')}"
    check(f"{nombre} -> {esperado}", ok, det)


print("\n--- Seguridad (servidor real, sin MockMvc)")
estado("mapa_sin_token", 401, "NO_AUTENTICADO", None)
estado("mapa_ana", 403, "SIN_PERMISO", None)
estado("calor_ana", 403, "SIN_PERMISO", None)
estado("est_ana", 403, "SIN_PERMISO", None)
origenes = os.environ.get("CORS_ORIGINS", "")
if "localhost:5173" in origenes:
    check("CORS preflight desde localhost:5173 aceptado", os.environ["CORS_OK"] == "200", os.environ["CORS_OK"])
check("CORS preflight desde un sitio ajeno rechazado", os.environ["CORS_MALO"] == "403", os.environ["CORS_MALO"])

print("\n--- Validación")
estado("calor_t11", 400, "VALIDACION", "tamano")
estado("est_vig_mala", 400, "VALIDACION", "vigencia")
estado("est_comuna11", 400, "VALIDACION", "comuna")
estado("est_fechas", 400, "VALIDACION", "desde")
estado("est_fecha_mala", 400)
estado("listado_comuna11", 400, "VALIDACION", "comuna")

print("\n--- Mapa")
estado("mapa", 200)
m = cuerpo("mapa")
check("total = v_reporte_mapa de la base", m["total"] == ORACULO["mapa"], f"{m['total']} vs {ORACULO['mapa']}")
check("sinComuna = v_reporte_mapa sin comuna", m["sinComuna"] == ORACULO["mapaSinComuna"],
      f"{m['sinComuna']} vs {ORACULO['mapaSinComuna']}")
check("no truncado y lista completa", not m["truncado"] and len(m["reportes"]) == m["total"])
campos = {"codigoSeguimiento", "estado", "categoria", "barrio", "comuna", "latitud", "longitud", "creadoEn",
          "vigenteHasta"}
check("marcadores sin datos personales", all(set(r) == campos for r in m["reportes"]))
fechas = [r["creadoEn"] for r in m["reportes"]]
check("del más reciente al más antiguo", fechas == sorted(fechas, reverse=True))
m5 = cuerpo("mapa_comuna5")
check("filtro comuna=5", m5["reportes"] and all(r["comuna"] == 5 for r in m5["reportes"]), f"{len(m5['reportes'])}")
mt = cuerpo("mapa_tramite")
check("filtro estado=EN_TRAMITE", mt["reportes"] and all(r["estado"] == "En trámite" for r in mt["reportes"]),
      f"{len(mt['reportes'])}")

print("\n--- Calor")
estado("calor", 200)
c = cuerpo("calor")
suma_pesos = sum(p["peso"] for p in c["puntos"])
check("totalReportes = suma de pesos = mapa.total", c["totalReportes"] == suma_pesos == m["total"],
      f"{c['totalReportes']} / {suma_pesos} / {m['total']}")
t10 = cuerpo("calor_t10")["comunasCriticas"]
suma_ranking = sum(i["total"] for i in t10["items"])
check("ranking + sinComuna = totalReportes", suma_ranking + c["sinComuna"] == c["totalReportes"],
      f"{suma_ranking} + {c['sinComuna']} vs {c['totalReportes']}")
totales = [(-i["total"], i["comuna"]) for i in t10["items"]]
check("ranking de más a menos (empate: menor comuna)", totales == sorted(totales))
check("posiciones 1..n", [i["posicion"] for i in t10["items"]] == list(range(1, len(t10["items"]) + 1)))
p1 = c["comunasCriticas"]
p2 = cuerpo("calor_p2")["comunasCriticas"]
check("página 1 de 5 y página 2 continúa el ranking",
      [i["comuna"] for i in p1["items"] + p2["items"]] == [i["comuna"] for i in t10["items"]],
      f"p1={[i['comuna'] for i in p1['items']]} p2={[i['comuna'] for i in p2['items']]}")
check("comuna 5 encabeza con su categoría principal", t10["items"][0]["comuna"] == 5
      and t10["items"][0]["categoriaPrincipal"]["nombre"], json.dumps(t10["items"][0], ensure_ascii=False))

print("\n--- Estadísticas")
estado("est", 200)
e = cuerpo("est")
check("total = todos los reportes de la base", e["total"] == ORACULO["todos"], f"{e['total']} vs {ORACULO['todos']}")
check("Σ porEstado = total", sum(x["total"] for x in e["porEstado"]) == e["total"])
check("Σ porComuna + sinComuna = total", sum(x["total"] for x in e["porComuna"]) + e["sinComuna"] == e["total"])
check("Σ porCategoria = total", sum(x["total"] for x in e["porCategoria"]) == e["total"])
check("4 estados, 10 comunas, categorías activas", len(e["porEstado"]) == 4 and len(e["porComuna"]) == 10
      and len(e["porCategoria"]) >= ORACULO["categoriasActivas"])
check("atendidos = resueltos de la base", e["atendidos"] == ORACULO["resueltos"], f"{e['atendidos']}")
esperado = round(e["atendidos"] * 100 / e["total"], 1) if e["total"] else 0
check("porcentaje de atendidos", abs(e["porcentajeAtendidos"] - esperado) < 0.05, f"{e['porcentajeAtendidos']}")
v, fu = cuerpo("est_vig"), cuerpo("est_fuera")
check("vigentes = base", v["total"] == ORACULO["vigentes"], f"{v['total']} vs {ORACULO['vigentes']}")
check("vigentes + fuera de plazo = todos", v["total"] + fu["total"] == e["total"], f"{v['total']} + {fu['total']}")
check("vigencia acepta mayúsculas", fu["vigencia"] == "fuera_de_plazo")
c1 = cuerpo("est_cat1")
check("con idCategoria solo aparece esa categoría", [x["idCategoria"] for x in c1["porCategoria"]] == [1])

print("\n--- Listado del panel (regresión del filtro compartido)")
estado("listado_comuna5", 200)
check("listado comuna=5 trae solo la comuna 5", all(r["comuna"] == 5 for r in cuerpo("listado_comuna5")))

print("\n--- Tiempos (servidor real, tras calentar)")
for n in ("mapa", "calor", "est"):
    check(f"{n} en menos de 2 s", http[n][1] < 2, f"{http[n][1]:.3f} s")

print(f"\n== {sum(resultados)} de {len(resultados)} verificaciones en verde")
