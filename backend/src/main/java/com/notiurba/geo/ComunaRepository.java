package com.notiurba.geo;

import org.springframework.data.repository.Repository;

/** Read-only access: no save/delete operations are exposed for diagnostics. */
public interface ComunaRepository extends Repository<Comuna, Short> {
    long count();
}
