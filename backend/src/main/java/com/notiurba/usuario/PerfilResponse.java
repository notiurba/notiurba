package com.notiurba.usuario;

/** El perfil de quien tiene sesión (GET /api/usuarios/me). El correo se muestra pero no se edita. */
public record PerfilResponse(
        String nombres,
        String apellidos,
        Short idTipoDocumento,
        String numeroDocumento,
        String correo,
        String telefono,
        Integer idBarrio,
        String direccionCorrespondencia) {

    static PerfilResponse de(Usuario u) {
        return new PerfilResponse(u.getNombres(), u.getApellidos(), u.getIdTipoDocumento(), u.getNumeroDocumento(),
                u.getCorreo(), u.getTelefono(), u.getIdBarrio(), u.getDireccionCorrespondencia());
    }
}
