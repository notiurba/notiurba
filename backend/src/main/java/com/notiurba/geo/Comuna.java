package com.notiurba.geo;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.locationtech.jts.geom.MultiPolygon;

@Entity
@Immutable
@Table(name = "comuna")
public class Comuna {
    @Id @Column(name = "id_comuna")
    private Short id;
    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;
    protected Comuna() {}
    public Short getId() { return id; }
    public MultiPolygon getGeom() { return geom; }
}
