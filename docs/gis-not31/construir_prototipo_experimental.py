"""Partición SINTÉTICA reproducible; no modifica insumos ni integra el backend.
Ejecutar con Python de QGIS 3.44. Shapely 2.1 / GEOS >=3.12, PyProj.
"""
import copy
import csv
import hashlib
import itertools
import json
import math
import os
import re
from collections import defaultdict
from pathlib import Path

import shapely
from shapely import set_precision, union_all, voronoi_polygons, coverage_is_valid
from shapely.geometry import Polygon, Point, MultiPoint, LineString, box, shape, mapping
from shapely.ops import transform, polygonize
from shapely.strtree import STRtree
from pyproj import Transformer

HERE=Path(__file__).resolve().parent
ROOT=HERE.parent.parent
SOURCE=ROOT/'frontend/src/compartido/geo/comunas-tulua-POT2015-candidato.geojson'
GRID=0.01
STEP=10.0
FWD=Transformer.from_crs(4326,3115,always_xy=True,allow_ballpark=False)
REV=Transformer.from_crs(3115,4326,always_xy=True,allow_ballpark=False)
TAG='SINTETICO_EXPERIMENTAL_ACADEMICO_NO_OFICIAL'

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def area_geom(g): return transform(FWD.transform,g).area
def parts(g):
    if g.geom_type=='Polygon': return [g] if not g.is_empty else []
    return [p for h in getattr(g,'geoms',[]) for p in parts(h)]
def lines(g):
    if g.geom_type in ('LineString','LinearRing'): return [g]
    return [p for h in getattr(g,'geoms',[]) for p in lines(h)]
def rings(g):
    return [r for p in parts(g) for r in [p.exterior,*p.interiors]]
def key(x,y): return (int(round(x/GRID)),int(round(y/GRID)))
def xy(k): return (k[0]*GRID,k[1]*GRID)
def dump(name,obj):
    (HERE/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2,allow_nan=False),encoding='utf-8')
def write_csv(name,rows):
    with (HERE/name).open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
def feature(g,props): return dict(type='Feature',properties=props,geometry=mapping(g))
def collection(name,features,**extra):
    dump(name,dict(type='FeatureCollection',uso=TAG,features=features,**extra))

def audit_features(geom,props):
    """Todas las superficies positivas quedan registradas; trazas lineales tienen área cero.
    Si una microgeometría no puede representarse válidamente en WGS84, se conserva
    como registro con geometría nula y WKT métrico explícito, sin alterar su área.
    """
    result=[]
    for i,p in enumerate(parts(geom)):
        if p.area<=0:continue
        wgs=transform(REV.transform,p)
        attributes=dict(props,parte=i,area_m2=p.area)
        if not wgs.is_valid or wgs.is_empty:
            attributes.update(visualizable_4326=False,wkt_3115=p.wkt,
                motivo='microgeometria_de_auditoria_no_representable_validamente_en_4326')
            result.append(dict(type='Feature',properties=attributes,geometry=None))
        else:
            attributes['visualizable_4326']=True
            result.append(feature(wgs,attributes))
    return result

def node_geometries(geometries):
    """Inserta todos los nodos colineales compartidos; cálculo entero en centímetros.
    Evita que una arista se exporte como A-C y su vecina como A-B-C al reproyectar.
    No desplaza nodos ni elimina superficies.
    """
    allkeys=sorted({key(*c[:2]) for g in geometries for r in rings(g) for c in r.coords})
    tree=STRtree([Point(k) for k in allkeys])
    cache={}
    def segment(a,b):
        if (a,b) in cache:return cache[(a,b)]
        ax,ay=a;bx,by=b
        candidates=tree.query(box(min(ax,bx),min(ay,by),max(ax,bx),max(ay,by)))
        v=[allkeys[int(i)] for i in candidates
           if (allkeys[int(i)][0]-ax)*(by-ay)==(allkeys[int(i)][1]-ay)*(bx-ax)]
        v=sorted(set(v+[a,b]),key=lambda p:(p[0]-ax)*(bx-ax)+(p[1]-ay)*(by-ay))
        cache[(a,b)]=v;cache[(b,a)]=list(reversed(v))
        return v
    def ring(r):
        ks=[key(*p[:2]) for p in r.coords]
        result=[]
        for a,b in zip(ks,ks[1:]):
            if a!=b: result.extend(segment(a,b)[:-1])
        result.append(result[0])
        return [xy(k) for k in result]
    result=[]
    for g in geometries:
        ps=[Polygon(ring(p.exterior),[ring(r) for r in p.interiors]) for p in parts(g)]
        from shapely.geometry import MultiPolygon
        result.append(ps[0] if len(ps)==1 else MultiPolygon(ps))
    return result

def validation(geoms,perimeter,geographic=False):
    # Quita el modelo de precisión SIN mover coordenadas: las pruebas no redondean residuos.
    geoms=[set_precision(g,0) for g in geoms]
    perimeter=set_precision(perimeter,0)
    u=union_all(geoms)
    measure=area_geom if geographic else lambda g:g.area
    overlaps=[]
    for i,j in itertools.combinations(range(10),2):
        hit=geoms[i].intersection(geoms[j])
        for p in parts(hit):
            if p.area>0: overlaps.append(dict(comunas=[f'{i+1:02d}',f'{j+1:02d}'],area_m2=measure(p)))
    missing=perimeter.difference(u)
    excess=u.difference(perimeter)
    voids=[Polygon(r) for p in parts(u) for r in p.interiors]
    result=dict(entidades=len(geoms),validas=sum(g.is_valid for g in geoms),no_vacias=sum(not g.is_empty for g in geoms),
        huecos_interiores=len(voids),huecos_interiores_m2=sum(measure(p) for p in voids),
        fragmentos_sin_asignacion=sum(p.area>0 for p in parts(missing)),sin_asignacion_m2=measure(missing),
        componentes_solapamiento=len(overlaps),solapamientos_m2=sum(p['area_m2'] for p in overlaps),
        fuera_perimetro_m2=measure(excess),cobertura_completa=missing.is_empty,
        coverage_is_valid=bool(coverage_is_valid(geoms)),solapamientos=overlaps,
        perimetro_m2=measure(perimeter),cobertura_m2=measure(u))
    result['correcto']=bool(len(geoms)==10 and result['validas']==10 and result['no_vacias']==10
        and not voids and not overlaps and missing.is_empty and excess.is_empty and result['coverage_is_valid'])
    return result

def make_qgis():
    os.environ.setdefault('QT_QPA_PLATFORM','offscreen')
    from qgis.core import (QgsApplication,QgsProject,QgsVectorLayer,QgsFillSymbol,QgsCategorizedSymbolRenderer,
        QgsRendererCategory,QgsCoordinateReferenceSystem,Qgis)
    from qgis.PyQt.QtGui import QColor
    app=QgsApplication([],False);app.initQgis()
    project=QgsProject.instance()
    original=HERE/'NotiUrba_Analisis_Comunas.qgz'
    original_hash=sha(original)
    assert project.read(str(original))
    project.setCrs(QgsCoordinateReferenceSystem('EPSG:4326'))
    project.setFilePathStorage(Qgis.FilePathType.Relative)
    for filename,title in [
        ('comunas-tulua-NOT31-prototipo-experimental.geojson','Comunas SINTETICAS - prototipo academico NO OFICIAL'),
        ('perimetro-experimental.geojson','Perimetro experimental - exteriores sin huecos'),
        ('areas-reasignadas-prototipo.geojson','Areas alteradas / reasignadas - sinteticas')]:
        layer=QgsVectorLayer(str(HERE/filename),title,'ogr');assert layer.isValid()
        if filename.startswith('comunas'):
            cats=[]
            for i in range(10):
                color=QColor.fromHsv(i*36,150,225,130)
                symbol=QgsFillSymbol.createSimple({'color':color.name(QColor.HexArgb),'outline_color':'30,30,30','outline_width':'0.25'})
                cats.append(QgsRendererCategory(f'{i+1:02d}',symbol,f'Comuna {i+1} sintetica'))
            layer.setRenderer(QgsCategorizedSymbolRenderer('COD_LOC_COM',cats))
        elif filename.startswith('perimetro'):
            layer.renderer().setSymbol(QgsFillSymbol.createSimple({'style':'no','outline_color':'180,0,0','outline_width':'0.65'}))
        else:
            layer.renderer().setSymbol(QgsFillSymbol.createSimple({'color':'255,80,30,110','outline_color':'255,40,0','outline_width':'0.15'}))
        project.addMapLayer(layer)
    target=HERE/'NotiUrba_Prototipo_Experimental.qgz'
    assert project.write(str(target))
    project.clear();assert project.read(str(target))
    assert len(project.mapLayers())==6 and all(l.isValid() for l in project.mapLayers().values())
    assert sha(original)==original_hash
    return dict(archivo=target.name,capas_validas=6,crs=project.crs().authid(),original_sin_cambios=True)

def main():
    raw_hash=sha(SOURCE)
    assert raw_hash=='b2f9f982c8d75be60b1cd6bf2b4eafcbc06d99025e39c3b0b69f870655615bc6'
    data=json.loads(SOURCE.read_text(encoding='utf-8'))
    fs=sorted(data['features'],key=lambda f:f['properties']['COD_LOC_COM'])
    ids=[f['properties']['COD_LOC_COM'] for f in fs]
    assert ids==[f'{i:02d}' for i in range(1,11)]
    prior_hashes={n:sha(HERE/n) for n in ('anomalias.geojson','coordenadas_prueba.csv','validacion_topologica.md')}
    native=[transform(FWD.transform,shape(f['geometry'])) for f in fs]
    source=[set_precision(g,GRID) for g in native]
    assert all(g.is_valid and not g.is_empty for g in source)
    covered=union_all(source,grid_size=GRID)
    perimeter=union_all([Polygon(p.exterior) for p in parts(covered)],grid_size=GRID)
    gaps=shapely.difference(perimeter,covered,grid_size=GRID)
    # Sitios de proximidad: vértices existentes + muestras cada <=10m por anillo.
    seed_owner={}
    vertex_log=[]
    for i,g in enumerate(source):
        for r in rings(g):
            samples=[Point(c) for c in r.coords]+[r.interpolate(k*r.length/math.ceil(r.length/STEP)) for k in range(math.ceil(r.length/STEP))]
            for p in samples:
                k=key(p.x,p.y);seed_owner[k]=min(i,seed_owner.get(k,i))
        for ring_n,r in enumerate(rings(native[i])):
            for n,(x,y,*_) in enumerate(r.coords):
                k=(math.floor(x/GRID+0.5),math.floor(y/GRID+0.5));xx,yy=xy(k)
                vertex_log.append(dict(comuna=ids[i],anillo=ring_n,vertice=n,x_3115_antes=x,y_3115_antes=y,
                    x_3115_cuantizado=xx,y_3115_cuantizado=yy,desplazamiento_m=math.hypot(x-xx,y-yy),
                    motivo='rejilla_sintetica_explicita_1cm_no_precision_oficial'))
    seeds=sorted(seed_owner)
    seed_points=[Point(xy(k)) for k in seeds]
    tree=STRtree(seed_points)
    sites=MultiPoint(seed_points)
    print('PROGRESO: sitios',len(seeds),'area_huecos_a_repartir_m2',gaps.area,flush=True)
    voronoi=voronoi_polygons(sites,extend_to=perimeter.envelope,ordered=True)
    # Sólo se introducen bisectrices dentro de las zonas sin cobertura; se conservan interiores.
    cuts=[]
    for cell in voronoi.geoms:
        if cell.intersects(gaps):
            cut=shapely.intersection(cell.boundary,gaps,grid_size=GRID)
            if not cut.is_empty:cuts.append(cut)
    graph=union_all([perimeter.boundary,*[g.boundary for g in source],*cuts],grid_size=GRID)
    faces=[set_precision(p,GRID) for p in polygonize(graph) if perimeter.covers(p.representative_point())]
    groups=defaultdict(list);gap_groups=defaultdict(list);face_log=[]
    for n,p in enumerate(faces):
        rp=p.representative_point()
        owners=[i for i,g in enumerate(source) if g.covers(rp)]
        if owners:
            owner=min(owners);rule='prioridad_codigo_menor' if len(owners)>1 else 'interior_original_cuantizado'
        else:
            # Ties se resuelven por código y sitio; STRtree no impone ese orden por sí mismo.
            nearest=tree.query_nearest(rp,all_matches=True)
            site=min((int(k) for k in nearest),key=lambda k:(seed_owner[seeds[k]],seeds[k]))
            owner=seed_owner[seeds[site]];rule='proximidad_sintetica_a_sitios_de_frontera'
            gap_groups[owner].append(p)
        groups[owner].append(p)
        face_log.append(dict(cara=n,comuna=ids[owner],regla=rule,superficie_m2=p.area))
    final=[union_all(groups[i],grid_size=GRID) for i in range(10)]
    final_noded=node_geometries([*final,perimeter])
    final=final_noded[:10]
    perimeter=final_noded[10]
    # Auditoría métrica completa; no se descarta ninguna área por umbral.
    metric=validation(final,perimeter)
    if not metric['correcto']:
        dump('resultado_validacion_prototipo.json',dict(estado='FALLO_METRICO',metrico=metric,sha256_fuente=raw_hash))
        raise RuntimeError('La particion metrica no satisface cero exacto; ver JSON real')
    # Registro de TODOS los vértices del resultado y transformación común por clave.
    transform_cache={}
    def to_wgs(x,y,z=None):
        # Shapely.ops.transform puede intentar primero arrays: se usa función escalar aquí.
        k=key(x,y)
        if k not in transform_cache:transform_cache[k]=REV.transform(*xy(k))
        return transform_cache[k]
    geographic=[transform(to_wgs,g) for g in final_noded]
    result_geo,per_geo=geographic[:10],geographic[10]
    exported=validation(result_geo,per_geo,geographic=True)
    stats=[];changes=[];outputs=[]
    audit_source=[set_precision(g,0) for g in source]
    audit_gaps=set_precision(gaps,0)
    for i,g in enumerate(final):
        audit_final=set_precision(g,0)
        gained=audit_final.difference(audit_source[i])
        lost=audit_source[i].difference(audit_final)
        assigned_gap=audit_final.intersection(audit_gaps)
        previous=union_all(audit_source[:i]) if i else Polygon()
        priority_overlap=audit_source[i].intersection(previous)
        ceded_overlap=lost.intersection(priority_overlap)
        noding_lost=lost.difference(priority_overlap)
        noding_gained=gained.difference(audit_gaps)
        priority_retained=audit_final.intersection(priority_overlap)
        precision_added=audit_source[i].difference(native[i])
        precision_removed=native[i].difference(audit_source[i])
        stats.append(dict(comuna=ids[i],nombre=fs[i]['properties']['NOM_LOC_COM'],area_original_3115_m2=native[i].area,
            area_base_1cm_m2=source[i].area,area_final_m2=g.area,huecos_asignados_m2=assigned_gap.area,
            solapes_cedidos_m2=ceded_overlap.area,ganancia_respecto_base_m2=gained.area,
            perdida_respecto_base_m2=lost.area,ganado_por_nodificado_m2=noding_gained.area,
            cedido_por_nodificado_m2=noding_lost.area,prioridad_modificada_por_nodificado_m2=priority_retained.area,
            ajuste_precision_agregado_m2=precision_added.area,ajuste_precision_retirado_m2=precision_removed.area,
            cambio_neto_m2=g.area-native[i].area))
        for geom,stage in [(assigned_gap,'hueco_asignado_sinteticamente'),(ceded_overlap,'solape_cedido_por_prioridad'),
                            (noding_lost,'limite_cedido_por_nodificado_1cm'),(noding_gained,'limite_ganado_por_nodificado_1cm'),
                            (precision_added,'rejilla_1cm_agregado'),(precision_removed,'rejilla_1cm_retirado')]:
            if not geom.is_empty and geom.area>0:
                changes.extend(audit_features(geom,dict(comuna=ids[i],etapa=stage,uso=TAG)))
        props=copy.deepcopy(fs[i]['properties'])
        props.update(VALIDACION=TAG,NOT31_SINTETICO=True,NOT31_AREA_M2=g.area,
            NOT31_HUECOS_ASIGNADOS_M2=assigned_gap.area,NOT31_SOLAPES_CEDIDOS_M2=ceded_overlap.area)
        outputs.append(feature(result_geo[i],props))
    collection('comunas-tulua-NOT31-prototipo-experimental.geojson',outputs,
        fuente_sha256=raw_hash,crs_trabajo='EPSG:3115',crs_salida='EPSG:4326',rejilla_m=GRID,
        regla='prioridad 01..10 en cobertura original; sitios de frontera cada <=10m para vacios; sintetico')
    collection('perimetro-experimental.geojson',[feature(per_geo,dict(uso=TAG,area_3115_m2=perimeter.area,
        metodo='anillos_exteriores_union_originales_proyectados_y_cuantizados_1cm_sin_huecos'))],crs_salida='EPSG:4326')
    collection('areas-reasignadas-prototipo.geojson',changes,nota='Capas de auditoria de etapas; pueden superponerse entre etapas, no son la particion final')
    write_csv('superficies_reasignadas_prototipo.csv',stats)
    write_csv('registro_vertices_precision_prototipo.csv',vertex_log)
    write_csv('registro_caras_prototipo.csv',face_log)
    vertices=[]
    for i,g in enumerate(final_noded[:10]):
        for rn,r in enumerate(rings(g)):
            for vn,(x,y,*_) in enumerate(r.coords):
                lon,lat=REV.transform(x,y)
                vertices.append(dict(comuna=ids[i],anillo=rn,vertice=vn,x_3115=x,y_3115=y,longitud=lon,latitud=lat))
    write_csv('registro_vertices_resultado_prototipo.csv',vertices)
    write_csv('sitios_proximidad_prototipo.csv',[dict(sitio=n,comuna=ids[seed_owner[k]],x_3115=xy(k)[0],y_3115=xy(k)[1]) for n,k in enumerate(seeds)])
    # Releer bytes entregados: la validación geográfica no se queda en memoria.
    reloaded=json.loads((HERE/'comunas-tulua-NOT31-prototipo-experimental.geojson').read_text(encoding='utf-8'))
    per_reload=json.loads((HERE/'perimetro-experimental.geojson').read_text(encoding='utf-8'))
    assert [f['properties']['COD_LOC_COM'] for f in reloaded['features']]==ids
    assert all(f['properties']['NOM_LOC_COM']==fs[i]['properties']['NOM_LOC_COM'] for i,f in enumerate(reloaded['features']))
    assert all(f['properties']['NOT31_SINTETICO'] is True for f in reloaded['features'])
    reload_geoms=[shape(f['geometry']) for f in reloaded['features']]
    assert all(g.equals_exact(h,0) for g,h in zip(reload_geoms,result_geo))
    assert shape(per_reload['features'][0]['geometry']).equals_exact(per_geo,0)
    structure=dict(exactamente_diez_entidades=len(reloaded['features'])==10,
        ids_unicos_01_a_10=[f['properties']['COD_LOC_COM'] for f in reloaded['features']]==ids and len(set(ids))==10,
        nombres_originales=all(f['properties']['NOM_LOC_COM']==fs[i]['properties']['NOM_LOC_COM'] for i,f in enumerate(reloaded['features'])),
        exportacion_epsg4326=reloaded['crs_salida']=='EPSG:4326' and all(-180<=g.bounds[0]<=g.bounds[2]<=180 and -90<=g.bounds[1]<=g.bounds[3]<=90 for g in reload_geoms),
        bytes_releidos_iguales_a_geometrias_validadas=all(g.equals_exact(h,0) for g,h in zip(reload_geoms,result_geo)),
        identificacion_explicita_no_oficial=all(f['properties']['NOT31_SINTETICO'] and f['properties']['VALIDACION']==TAG for f in reloaded['features']),
        areas_experimentales_identificadas=len(changes)>0)
    assert all(structure.values())
    with (HERE/'coordenadas_prueba.csv').open(encoding='utf-8-sig',newline='') as f:tests=list(csv.DictReader(f))
    point_results=[]
    for row in tests:
        p=Point(float(row['longitud']),float(row['latitud']))
        hits=[ids[i] for i,g in enumerate(result_geo) if g.covers(p)]
        inside=per_geo.covers(p)
        state=('INTERIOR_UNICO_EXPERIMENTAL' if len(hits)==1 else 'BORDE_EXPERIMENTAL' if len(hits)>1 else 'FUERA_PERIMETRO_EXPERIMENTAL')
        coherent=bool(hits)==inside
        if row['tipo']=='interior':coherent=coherent and hits==[row['resultado_esperado']]
        if row['tipo']=='fuera_cobertura':coherent=coherent and not hits
        anomaly_match=re.search(r'Anomalia ([HS]\d{3})',row['justificacion'])
        point_results.append(dict(id=row['id'],anomalia_origen=anomaly_match.group(1) if anomaly_match else '',tipo_original=row['tipo'],esperado_original=row['resultado_esperado'],
            resultado_experimental=state,comunas='|'.join(hits),dentro_perimetro=inside,coherente=coherent,
            motivo='interior_original_conservado' if row['tipo']=='interior' else 'reclasificacion_sintetica_documentada_no_equivale_a_aprobacion'))
    write_csv('resultados_puntos_prototipo.csv',point_results)
    # Reparto de H001 mediante la geometría ya inventariada, sin repetir diagnóstico inicial.
    a=json.loads((HERE/'anomalias.geojson').read_text(encoding='utf-8'))
    h=next(f for f in a['features'] if f['properties']['id']=='H001')
    hmetric=transform(FWD.transform,shape(h['geometry']))
    hsplit={ids[i]:hmetric.intersection(set_precision(g,0)).area for i,g in enumerate(final) if hmetric.intersection(set_precision(g,0)).area>0}
    result=dict(estado='OK' if metric['correcto'] and exported['correcto'] and all(p['coherente'] for p in point_results) else 'VALIDACION_FALLIDA',
        uso=TAG,sha256_fuente=raw_hash,rejilla_m=GRID,muestras_frontera_max_m=STEP,
        antes=dict(huecos=27,solapamientos=23,comunas_validas=10,origen='diagnostico_preexistente_no_recalculado'),
        metrico_3115=metric,exportado_4326=exported,pruebas_estructura=structure,
        pruebas_puntos=dict(total=len(tests),coherentes=sum(p['coherente'] for p in point_results),interiores_originales_conservados=sum(p['coherente'] for p in point_results if p['tipo_original']=='interior')),
        superficies_por_comuna=stats,reparto_h001_m2=hsplit,h001_repartida_entre_varias_comunas=len(hsplit)>1,
        sitios=len(seeds),caras=len(faces),max_desplazamiento_rejilla_m=max(r['desplazamiento_m'] for r in vertex_log),
        areas_etapas_ganancias_perdidas_no_sumar_doble=True,originales_sin_cambios=True)
    result['h001_repartida_entre_varias_comunas']=len(hsplit)>1
    if not result['h001_repartida_entre_varias_comunas']:result['estado']='VALIDACION_FALLIDA'
    try:result['qgis']=make_qgis()
    except Exception as exc:result['qgis']=dict(error=type(exc).__name__+': '+str(exc));result['estado']='VALIDACION_FALLIDA'
    assert sha(SOURCE)==raw_hash and all(sha(HERE/n)==h for n,h in prior_hashes.items())
    metadata(result)
    dump('resultado_validacion_prototipo.json',result)
    make_report(result)
    print(json.dumps(dict(estado=result['estado'],metrico=metric,exportado=exported,h001=hsplit,puntos=result['pruebas_puntos'],qgis=result['qgis']),ensure_ascii=False),flush=True)

def metadata(r):
    import platform,pyproj
    r['versiones']=dict(python=platform.python_version(),shapely=shapely.__version__,geos=shapely.geos_version_string,
        pyproj=pyproj.__version__,proj=pyproj.proj_version_str)
    r['operacion_transformacion']=FWD.description
    r['artefactos_sha256']={name:sha(HERE/name) for name in (
        'construir_prototipo_experimental.py','comunas-tulua-NOT31-prototipo-experimental.geojson',
        'perimetro-experimental.geojson','areas-reasignadas-prototipo.geojson',
        'superficies_reasignadas_prototipo.csv','registro_vertices_precision_prototipo.csv',
        'registro_caras_prototipo.csv','registro_vertices_resultado_prototipo.csv',
        'sitios_proximidad_prototipo.csv','resultados_puntos_prototipo.csv','NotiUrba_Prototipo_Experimental.qgz')}
    audit=json.loads((HERE/'areas-reasignadas-prototipo.geojson').read_text(encoding='utf-8'))
    r['auditoria_superficies']=dict(registros=len(audit['features']),
        registros_solo_metricos=sum(f['geometry'] is None for f in audit['features']),
        todas_areas_registradas=True,nota='Registros no representables en WGS84 conservan WKT EPSG3115 y superficie; no son huecos de la particion')

def make_report(r):
    rows='\n'.join(f"| {s['comuna']} | {s['huecos_asignados_m2']:.6f} | {s['solapes_cedidos_m2']:.6f} | {s['cambio_neto_m2']:.6f} |" for s in r['superficies_por_comuna'])
    text=f'''# NOT-31 — Partición experimental del prototipo académico

**SINTÉTICA, NO OFICIAL.** Fecha: 2026-11-04. Estado de validación: **{r['estado']}**. No se integró al backend ni se cambió Linear.

## Perímetro declarado y reglas reproducibles

Se utiliza exclusivamente la capa original con SHA256 `{r['sha256_fuente']}`. Se transforma a EPSG:3115 con XY explícito y sin ballpark; se cuantiza en una rejilla **real de 0,01 m** como decisión del modelo experimental, no como exactitud oficial ni tolerancia para ocultar errores. Se disuelven los diez polígonos cuantizados y se toman sus anillos exteriores, eliminando sus anillos interiores para definir el dominio que DEBE cubrirse. No se usa envolvente convexa, buffer expansivo ni se unen componentes separados artificialmente. Si un componente exterior incluye un vacío, ese vacío pasa explícitamente al dominio experimental y debe asignarse. El archivo de perímetro representa esa decisión sintética, no el perímetro urbano oficial.

La rejilla cambia coordenadas y puede colapsar microincidencias: no se afirma que todas fueran errores numéricos. Cada vértice de entrada tiene su destino cuantizado y desplazamiento en registro_vertices_precision_prototipo.csv; máximo observado: {r['max_desplazamiento_rejilla_m']:.12g} m. Las áreas agregadas/retiradas por comuna durante esa etapa se registran separadamente. No se descartan superficies con un umbral de área o de visualización.

En solapamientos, la prioridad determinista es código menor 01→10. Se conserva el interior que ya tenía cobertura, salvo cambios explícitos de rejilla y resolución de doble cobertura. Para zonas sin asignación se construyen sitios de frontera con los vértices y muestras cada <=10 m de cada anillo de las comunas cuantizadas. Sitios coincidentes y empates usan código menor y orden de coordenadas. Dentro de esos vacíos se introducen bisectrices de Voronoi; cada cara recibe la comuna del sitio más próximo. Es aproximación a proximidad de frontera muestreada (error de distancia por muestreo acotado por 5 m, más cuantización), **no una frontera territorial inferida**. Las bisectrices sólo se utilizan en vacíos: no se reemplazan todos los interiores por un Voronoi global.

La red completa de límites se noda y poligoniza con precisión centimétrica. Cada cara interior se asigna exactamente una vez mediante esas reglas y se agrupa por ID. Antes de exportar se insertan todos los nodos compartidos colineales usando aritmética entera en centímetros; no se altera la geometría métrica. Cada coordenada compartida se transforma de manera común a EPSG:4326. Esto evita que una frontera se reproyecte como A-C en una comuna y A-B-C en su vecina, generando microvacíos. También se noda el perímetro declarado, sin cambiar su trazado métrico.

## H001 y superficies sintéticas

H001 y el resto de zonas sin asignación se identifican con el inventario existente. Se autoriza aquí su reparto **sintético para pruebas**, sin confirmar ni negar la hipótesis hidrográfica. H001 no se asigna completa a una sola comuna. Reparto medido por intersección del H001 inventariado con el resultado métrico: `{json.dumps(r['reparto_h001_m2'],ensure_ascii=False)}` m². Estas cifras incluyen cualquier cobertura previa de tamaño numérico y la etapa de rejilla; el registro de huecos asignados distingue la nueva atribución desde la base cuantizada.

| Comuna | Vacíos asignados m² | Solapes cedidos m² | Cambio neto frente al original m² |
| --- | ---: | ---: | ---: |
{rows}

superficies_reasignadas_prototipo.csv incluye además el cambio agregado/retirado por rejilla. Los nuevos cruces también se cuantizan durante el nodificado de la red: eso puede mover pequeñas franjas de las fronteras de base. Se registran como ganado_por_nodificado_m2, cedido_por_nodificado_m2 y prioridad_modificada_por_nodificado_m2, sin atribuirlas falsamente a huecos o solapamientos originales. La prioridad se aplica a cada cara de esa red sintética; las desviaciones geométricas frente a una prioridad de polígonos sin redondear quedan cuantificadas. Los vacíos asignados se miden por intersección con el vacío de referencia, no por el área entera de una cara que lo atraviese.

areas-reasignadas-prototipo.geojson permite inspeccionar cada etapa; esas capas de auditoría pueden superponerse entre etapas y no deben sumarse como área única ni confundirse con la partición final. Sus componentes poligonales se exportan individualmente. Si una microgeometría de auditoría se degenera al pasar a WGS84, el registro conserva superficie y WKT EPSG:3115 con geometría visual nula, sin eliminar el registro ni confundirlo con una falta de cobertura. Recuento: {json.dumps(r.get('auditoria_superficies',{}),ensure_ascii=False)}. Las diez geometrías de la partición final son válidas y no nulas.

registro_caras_prototipo.csv identifica regla, ID y superficie de cada cara. registro_vertices_resultado_prototipo.csv identifica todos los vértices finales, anillo, ID y coordenadas en ambos CRS. Los sitios se conservan en sitios_proximidad_prototipo.csv y resultados_puntos_prototipo.csv vincula los cincuenta puntos de anomalías a sus ID H/S originales. Los campos heredados Shape_Area/Shape_Leng siguen siendo atributos del insumo; NOT31_AREA_M2 y el CSV contienen las superficies del resultado. Ninguna de estas fronteras sintéticas es oficial.

## Validación obligatoria, sin reutilizar conteos posteriores

Antes se conservan los conteos documentados 27/23; **después se vuelve a calcular** unión, huecos y las 45 intersecciones por parejas tanto en la partición EPSG:3115 como en EPSG:4326. No se acepta una superficie residual distinta de cero, ni una tolerancia de visualización. También se comprueba coverage_is_valid, geometrías válidas/no vacías, diez ID/nombres originales, nulos residuales del perímetro menos cobertura y cobertura menos perímetro. Se releen los GeoJSON escritos y se comprueba igualdad exacta de sus coordenadas con las validadas en memoria. La cobertura completa se comprueba contra el perímetro independiente derivado de los anillos originales cuantizados, no redefiniéndolo para ocultar huecos del resultado.

- EPSG:3115: {r['metrico_3115']['huecos_interiores']} huecos; {r['metrico_3115']['componentes_solapamiento']} solapes; sin asignar {r['metrico_3115']['sin_asignacion_m2']:.12g} m²; fuera del perímetro {r['metrico_3115']['fuera_perimetro_m2']:.12g} m².
- EPSG:4326 exportado: {r['exportado_4326']['huecos_interiores']} huecos; {r['exportado_4326']['componentes_solapamiento']} solapes; sin asignar {r['exportado_4326']['sin_asignacion_m2']:.12g} m²; fuera {r['exportado_4326']['fuera_perimetro_m2']:.12g} m² (medidos tras proyectar las diferencias a EPSG:3115).
- Puntos existentes: {r['pruebas_puntos']['coherentes']}/{r['pruebas_puntos']['total']} coherentes con el nuevo dominio; diez interiores originales deben conservar su ID y los tres exteriores seguir fuera. Los puntos antiguos de anomalías se reclasifican según la partición sintética; sus expectativas previas no se sobrescriben. Los bordes pueden tener dos candidatos sin que exista solapamiento de superficie. Ver resultados_puntos_prototipo.csv.
- QGIS: `{json.dumps(r['qgis'],ensure_ascii=False)}`.

## Reproducción y límites de uso

Desde la copia aislada ejecutar con `C:\\Program Files\\QGIS 3.44.15\\bin\\python-qgis-ltr.bat` el script `docs/gis-not31/construir_prototipo_experimental.py`. Requiere Shapely 2.1, GEOS >=3.12, PyProj y PyQGIS existentes. Parámetros: EPSG:3115, rejilla 0,01 m, muestreo <=10 m, prioridad ascendente por ID. Los resultados corresponden a estas decisiones; cambiar parámetros crea otra partición, no mayor certeza administrativa.

La capa es apta únicamente para ensayos geométricos académicos cuando resultado_validacion_prototipo.json indica OK. No certifica límites, barrios, exclusiones de ríos, cobertura oficial, vigencia o aceptación del equipo. No debe usarse para gestión territorial real, decisiones administrativas o asignación oficial de reportes. No se sustituyen originales, no se toca producción/backend/migraciones y no se hicieron commits, push o PR. Los informes previos describen la capa original y se conservan como respaldo; sus bloqueos administrativos no se presentan como resueltos por esta simulación.
'''
    (HERE/'informe_correccion_prototipo.md').write_text(text,encoding='utf-8')

if __name__=='__main__':main()
