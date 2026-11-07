package com.notiurba.usuario;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/** Ver y editar el perfil propio. Usa las mismas reglas que el registro (ReglasPerfil y los catálogos). */
@Service
public class PerfilServicio {

    private final UsuarioRepository usuarios;
    private final CatalogosRegistro catalogos;

    public PerfilServicio(UsuarioRepository usuarios, CatalogosRegistro catalogos) {
        this.usuarios = usuarios;
        this.catalogos = catalogos;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtener(String correoSesion) {
        return PerfilResponse.de(propio(correoSesion));
    }

    @Transactional
    public PerfilResponse actualizar(String correoSesion, ActualizarPerfilRequest p) {
        Usuario usuario = propio(correoSesion);

        String nombres = ReglasPerfil.limpiarNombre(p.nombres(), "Los nombres", "nombres");
        String apellidos = ReglasPerfil.limpiarNombre(p.apellidos(), "Los apellidos", "apellidos");
        if (!catalogos.existeTipoDocumento(p.idTipoDocumento())) {
            throw error("El tipo de documento no existe", "idTipoDocumento");
        }
        String numeroDocumento = p.numeroDocumento().trim();
        if (numeroDocumento.isEmpty()) {
            throw error("Completa el campo «Número de documento».", "numeroDocumento");
        }
        // Se puede conservar el barrio actual aunque sea interno (el del administrador); elegir uno nuevo exige uno real
        if (!p.idBarrio().equals(usuario.getIdBarrio()) && !catalogos.existeBarrio(p.idBarrio())) {
            throw error("El barrio no existe", "idBarrio");
        }
        String direccion = p.direccionCorrespondencia().trim();
        if (direccion.isEmpty()) {
            throw error("Completa el campo «Dirección de correspondencia».", "direccionCorrespondencia");
        }

        boolean cambioDocumento = !p.idTipoDocumento().equals(usuario.getIdTipoDocumento())
                || !numeroDocumento.equals(usuario.getNumeroDocumento());
        if (cambioDocumento && usuarios.existsByIdTipoDocumentoAndNumeroDocumento(p.idTipoDocumento(), numeroDocumento)) {
            throw new ApiException(HttpStatus.CONFLICT, Codigos.DOCUMENTO_YA_REGISTRADO,
                    "Ya existe una cuenta con este documento", "numeroDocumento");
        }

        usuario.actualizarPerfil(nombres, apellidos, p.idTipoDocumento(), numeroDocumento, p.idBarrio(), direccion,
                p.telefono() == null || p.telefono().isBlank() ? null : p.telefono().trim());
        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException carrera) {
            // Otra cuenta tomó el documento entre la revisión y el guardado
            throw new ApiException(HttpStatus.CONFLICT, Codigos.CONFLICTO,
                    "No se pudo guardar: el documento ya está registrado en otra cuenta");
        }
        return PerfilResponse.de(usuario);
    }

    private Usuario propio(String correoSesion) {
        return usuarios.findByCorreoIgnoreCase(correoSesion)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                        Codigos.MSG_NO_AUTENTICADO));
    }

    private static ApiException error(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, mensaje, campo);
    }
}
