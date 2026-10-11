"""Diagnostico sin reparar ni sobrescribir las comunas; areas en EPSG:3115."""
import argparse
import hashlib
import io
import json
from itertools import combinations
from pathlib import Path
import zipfile

from pyproj import CRS, Transformer
from shapely import transform as transform_geometry
from shapely.geometry import Polygon, mapping, shape
from shapely.ops import polygonize, unary_union
from shapely.validation import explain_validity


def transform(function, geometry):
    return transform_geometry(geometry, function, interleaved=False)


def polygons(geometry):
    if geometry.geom_type == 'Polygon':
        if not geometry.is_empty and geometry.area > 0:
            yield geometry
    elif hasattr(geometry, 'geoms'):
        for part in geometry.geoms:
            yield from polygons(part)


def analyze(geometries, adjacency_tolerance=1e-6):
    pairs = []
    for (a, ga), (b, gb) in combinations(geometries.items(), 2):
        parts = list(polygons(ga.intersection(gb)))
        if parts:
            pairs.append((a, b, unary_union(parts)))
    # Split intersections into disjoint faces: triple coverage is counted once.
    groups = {}
    if pairs:
        for face in polygonize(unary_union([g.boundary for _, _, g in pairs])):
            point = face.representative_point()
            codes = tuple(sorted({c for a, b, g in pairs if g.covers(point) for c in (a, b)}))
            if len(codes) >= 2:
                groups.setdefault(codes, []).append(face)
    expected = unary_union([g for _, _, g in pairs])
    found = unary_union([g for faces in groups.values() for g in faces])
    # Retain positive-area numeric remnants instead of silently discarding them.
    for remnant in polygons(expected.difference(found)):
        codes = tuple(sorted({c for a, b, g in pairs if remnant.intersection(g).area > 0 for c in (a, b)}))
        assert len(codes) >= 2
        groups.setdefault(codes, []).append(remnant)
    overlaps = [(p, codes) for codes, faces in groups.items()
                for p in polygons(unary_union(faces))]
    residual = unary_union([g for g, _ in overlaps]).symmetric_difference(expected).area
    assert residual < (1e-12 if adjacency_tolerance < 1e-8 else 1e-4), residual
    union = unary_union(list(geometries.values()))
    holes = []
    for part in polygons(union):
        for ring in part.interiors:
            for hole in polygons(Polygon(ring).difference(union)):
                # Adjacent means a nonzero common boundary, with a 1 micrometre
                # tolerance only for adjacency attribution, never for geometry.
                codes = tuple(c for c, g in geometries.items()
                              if hole.boundary.intersection(g.boundary.buffer(adjacency_tolerance)).length > 0)
                holes.append((hole, codes))
    return overlaps, holes


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--zip', type=Path)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    geo_dir = Path(__file__).resolve().parents[1] / 'data' / 'geo'
    source = geo_dir / 'comunas-tulua-POT2015-candidato.geojson'
    output = geo_dir / 'diagnostico-limites-POT2015.geojson'
    raw = source.read_bytes()
    source_sha = hashlib.sha256(raw).hexdigest()
    p28_visual_source = source_sha == 'b2f9f982c8d75be60b1cd6bf2b4eafcbc06d99025e39c3b0b69f870655615bc6'
    data = json.loads(raw)
    assert data['source']['originalCRS'] == 'EPSG:3115'
    projected = CRS.from_epsg(3115)
    forward = Transformer.from_crs(4326, projected, always_xy=True, allow_ballpark=False)
    reverse = Transformer.from_crs(projected, 4326, always_xy=True, allow_ballpark=False)
    geographic = {f['properties']['COD_LOC_COM']: shape(f['geometry']) for f in data['features']}
    geometries = {f['properties']['COD_LOC_COM']: transform(forward.transform, shape(f['geometry']))
                  for f in data['features']}
    assert sorted(geometries) == [f'{i:02d}' for i in range(1, 11)]
    validity = {c: explain_validity(g) for c, g in geometries.items()}
    assert all(g.is_valid and not g.is_empty for g in geometries.values()), validity
    # Topology is evaluated on the actual Leaflet input to avoid inverse-projection
    # roundoff manufacturing submicrometre slivers at identical boundaries.
    overlaps, holes = analyze(geographic, adjacency_tolerance=1e-11)
    original_check = None
    original_holes = []
    original_overlap_union = None
    if args.zip:
        import shapefile
        with zipfile.ZipFile(args.zip) as archive:
            base = next(n[:-4] for n in archive.namelist() if n.endswith('/01_P_Comunas_urbanas_2014.shp'))
            assert CRS.from_wkt(archive.read(base + '.prj').decode()).to_epsg() == 3115
            reader = shapefile.Reader(**{k: io.BytesIO(archive.read(base + '.' + k))
                                        for k in ('shp', 'shx', 'dbf')}, encoding='latin1')
            originals = {r.record.as_dict()['Comuna'].split()[-1].zfill(2): shape(r.shape.__geo_interface__)
                         for r in reader.iterShapeRecords()}
        oo, original_holes = analyze(originals)
        original_overlap_union = unary_union([g for g, _ in oo])
        original_check = {
            'zip': args.zip.name, 'validity': {c: explain_validity(g) for c, g in originals.items()},
            'overlap_count': len(oo), 'hole_count': len(original_holes),
            'overlap_area_m2': sum(g.area for g, _ in oo),
            'hole_area_m2': sum(g.area for g, _ in original_holes),
            'max_vertex_roundtrip_displacement_m': max(
                g.hausdorff_distance(originals[c]) for c, g in geometries.items()),
        }
    features = []
    for kind, items, prefix in [('solapamiento', overlaps, 'S'), ('hueco', holes, 'H')]:
        items.sort(key=lambda item: (-transform(forward.transform, item[0]).area, item[0].bounds))
        for index, (geometry, codes) in enumerate(items, 1):
            wgs = geometry
            reference = wgs.representative_point()
            geometry = transform(forward.transform, wgs)
            assert wgs.is_valid
            assert -77 < wgs.bounds[0] <= wgs.bounds[2] < -75
            assert 3 < wgs.bounds[1] <= wgs.bounds[3] < 5
            micro = geometry.area < 0.01
            if kind == 'solapamiento':
                classification = 'posible error geometrico'
                cause = 'Cobertura de dos o mas comunas; no demuestra conflicto administrativo.'
                effect = 'Pendiente de revision si un reporte intersecta esta superficie.'
                confirmed = original_overlap_union is not None and geometry.intersection(original_overlap_union).area > geometry.area * 0.5
            else:
                classification = 'posible exclusion cartografica' if len(codes) == 1 and not micro else 'pendiente de validacion'
                cause = ('Hueco adyacente a una comuna; posible exclusion, sin confirmacion documental.'
                         if len(codes) == 1 else 'Espacio interior entre comunas; posible discontinuidad o exclusion.')
                effect = 'Comuna sin determinar para puntos interiores; no implica estar fuera de Tulua.'
                confirmed = any(geometry.intersection(g).area > geometry.area * 0.5 for g, _ in original_holes)
            if micro:
                cause += ' Superficie menor de 0.01 m2: revisar precision numerica antes de interpretarla.'
            visual_river = p28_visual_source and kind == 'hueco' and index == 1 and not micro
            if visual_river:
                classification = 'posible exclusion cartografica'
                cause = 'El hueco sigue principalmente el corredor del rio Tulua en P-28; posible exclusion hidrografica, no confirmada administrativamente.'
            props = {
                'id': f'{prefix}{index:03d}', 'tipo': kind, 'comunas': list(codes),
                'area_m2': geometry.area, 'referencia': [reference.x, reference.y],
                'estado': 'pendiente_revision', 'clasificacion': classification,
                'posible_causa': cause, 'efecto_reportes': effect,
                'recomendacion': 'Contrastar con P-28 y Planeacion; no corregir automaticamente.',
                'micro_superficie': micro,
                'presente_en_shp_original': confirmed if args.zip else None,
                'contraste_p28': ('Coincidencia visual con corredor del rio Tulua mediante grilla impresa; no georreferenciacion definitiva.'
                                 if visual_river else 'No resoluble a escala de ploteo 1:10000; pendiente de validacion.'),
            }
            features.append({'type': 'Feature', 'properties': props, 'geometry': mapping(wgs)})
    summary = {kind: {'cantidad': sum(f['properties']['tipo'] == kind for f in features),
                      'area_m2': sum(f['properties']['area_m2'] for f in features if f['properties']['tipo'] == kind)}
               for kind in ('hueco', 'solapamiento')}
    summary['comunas_involucradas'] = sorted({c for f in features for c in f['properties']['comunas']})
    result = {'type': 'FeatureCollection', 'name': 'Diagnostico POT 2015 sin correcciones',
              'metadata': {'source': source.name, 'source_sha256': source_sha,
                           'area_crs': 'EPSG:3115', 'geometry_crs': 'EPSG:4326',
                           'validity': validity, 'original_check': original_check,
                           'summary': summary, 'administrative_validation': 'pendiente',
                           'area_note': 'Area geometrica proyectada, no precision administrativa; sin umbral de descarte.'},
              'features': features}
    # Check disjoint coverage and its area against the union of pair intersections.
    assert abs(sum(g.area for g, _ in overlaps) - unary_union([g for g, _ in overlaps]).area) < 1e-20
    for geometry, _ in holes:
        assert geometry.intersection(unary_union(list(geographic.values()))).area < 1e-20
    if args.check:
        saved = json.loads(output.read_text(encoding='utf-8'))
        assert saved == json.loads(json.dumps(result)), 'El diagnostico no corresponde al analisis reproducido'
    else:
        output.write_text(json.dumps(result, ensure_ascii=True, allow_nan=False, separators=(',', ':')), encoding='utf-8')
    print(json.dumps({'summary': summary, 'original_check': original_check,
                      'output': str(output), 'check': args.check}, ensure_ascii=True))


if __name__ == '__main__':
    main()
