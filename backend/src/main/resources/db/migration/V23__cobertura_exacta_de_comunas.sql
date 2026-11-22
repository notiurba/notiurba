-- El MVP acepta coordenadas únicamente cubiertas por uno de los polígonos de
-- las diez comunas. El borde cuenta por ST_Covers; los huecos y los puntos
-- externos ya no se aproximan a una comuna por distancia.
UPDATE configuracion
   SET valor = '0'
 WHERE clave = 'comuna_distancia_maxima_m';
