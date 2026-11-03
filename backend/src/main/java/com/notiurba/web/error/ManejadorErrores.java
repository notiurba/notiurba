package com.notiurba.web.error;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.notiurba.foto.AlmacenFotosNoDisponibleException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolation;

/** Convierte toda excepción en {codigo, mensaje, campo}. Nunca expone detalles internos. */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Nombre que ve el ciudadano para cada campo; si falta, se usa el nombre técnico. */
    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("correo", "Correo"), Map.entry("password", "Contraseña"),
            Map.entry("nuevaContrasena", "Contraseña"), Map.entry("nombres", "Nombres"),
            Map.entry("apellidos", "Apellidos"), Map.entry("idTipoDocumento", "Tipo de documento"),
            Map.entry("numeroDocumento", "Número de documento"), Map.entry("idBarrio", "Barrio"),
            Map.entry("direccionCorrespondencia", "Dirección"), Map.entry("telefono", "Teléfono"),
            Map.entry("token", "Enlace"), Map.entry("aceptaPrivacidad", "Aviso de privacidad"),
            Map.entry("declaraMayorEdad", "Declaración de mayoría de edad"),
            Map.entry("descripcion", "Descripción"), Map.entry("idCategoria", "Categoría"),
            Map.entry("latitud", "Latitud"), Map.entry("longitud", "Longitud"),
            Map.entry("direccionIncidente", "Dirección del incidente"),
            Map.entry("matriculaCuentaServicio", "Matrícula o cuenta del servicio"),
            Map.entry("radicadoOficial", "Radicado"), Map.entry("autorizaEnvio", "Autorización de envío"));

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorApi> negocio(ApiException e) {
        return respuesta(e.getStatusCode(), new ErrorApi(e.getCodigo(), e.getReason(), e.getCampo()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorApi> estado(ResponseStatusException e) {
        return respuesta(e.getStatusCode(), porEstado(e.getStatusCode(), e.getReason()));
    }

    /** 502: el problema es de un tercero (Cloudinary), no del ciudadano ni de la API. */
    @ExceptionHandler(AlmacenFotosNoDisponibleException.class)
    ResponseEntity<ErrorApi> almacenFotos(AlmacenFotosNoDisponibleException e) {
        LOG.error("El almacén de fotos no respondió: {}", e.getMessage());
        return respuesta(HttpStatus.BAD_GATEWAY, new ErrorApi(Codigos.ALMACEN_FOTOS_NO_DISPONIBLE,
                "No pudimos guardar la foto en este momento. Intenta de nuevo en un momento.", "foto"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorApi> inesperado(Exception e) {
        LOG.error("Error no controlado", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, porEstado(HttpStatus.INTERNAL_SERVER_ERROR, null));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> orden = e.getBindingResult().getTarget() == null ? List.of()
                : camposEnOrden(e.getBindingResult().getTarget().getClass());
        FieldError primero = e.getBindingResult().getFieldErrors().stream()
                .min(Comparator.comparingInt(f -> posicion(orden, f.getField()))).orElse(null);
        ErrorApi error = primero == null
                ? new ErrorApi(Codigos.VALIDACION, "Revisa los datos del formulario.", null)
                : new ErrorApi(Codigos.VALIDACION, mensaje(primero), primero.getField());
        return new ResponseEntity<>(error, headers, HttpStatus.BAD_REQUEST);
    }

    /** Falta la parte multipart (p. ej. «foto»): 400 con el nombre de la parte como campo. */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String campo = e.getRequestPartName();
        return new ResponseEntity<>(new ErrorApi(Codigos.VALIDACION,
                "foto".equals(campo) ? "Elige una foto" : "Falta el campo «" + campo + "»", campo),
                headers, HttpStatus.BAD_REQUEST);
    }

    /** Cuerpo ilegible, ruta inexistente, método o tipo no soportado, etc. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return new ResponseEntity<>(porEstado(status, null), headers, status);
    }

    private static ErrorApi porEstado(HttpStatusCode estado, @Nullable String razon) {
        int codigo = estado.value();
        if (codigo == 401) {
            return new ErrorApi(Codigos.NO_AUTENTICADO, Codigos.MSG_NO_AUTENTICADO, null);
        }
        if (codigo == 403) {
            return new ErrorApi(Codigos.SIN_PERMISO, Codigos.MSG_SIN_PERMISO, null);
        }
        if (codigo == 404) {
            return new ErrorApi(Codigos.NO_ENCONTRADO, "No encontramos lo que buscas.", null);
        }
        if (codigo == 413) {
            // Spring lanza MaxUploadSizeExceededException (413) al pasar el límite multipart; hoy solo suben fotos
            return new ErrorApi(Codigos.ARCHIVO_MUY_GRANDE, "La foto pesa más de 1 MB. Elige una más liviana.", "foto");
        }
        if (codigo == 415) {
            return new ErrorApi(Codigos.FORMATO_NO_SOPORTADO, "El formato enviado no es soportado.", null);
        }
        if (codigo >= 500) {
            return new ErrorApi(Codigos.ERROR_INTERNO, "Ocurrió un error. Intenta de nuevo más tarde.", null);
        }
        if (codigo == 409) {
            return new ErrorApi(Codigos.CONFLICTO, razon != null ? razon : "La acción choca con el estado actual.", null);
        }
        return new ErrorApi(Codigos.VALIDACION, razon != null ? razon : "La petición no es válida.", null);
    }

    private static ResponseEntity<ErrorApi> respuesta(HttpStatusCode estado, ErrorApi error) {
        return ResponseEntity.status(estado).body(error);
    }

    private static String mensaje(FieldError f) {
        String etiqueta = ETIQUETAS.getOrDefault(f.getField(), f.getField());
        String restriccion = f.getCode() == null ? "" : f.getCode();
        return switch (restriccion) {
            case "NotBlank", "NotNull" -> "Completa el campo «" + etiqueta + "».";
            case "Size" -> {
                Object max = f.contains(ConstraintViolation.class)
                        ? f.unwrap(ConstraintViolation.class).getConstraintDescriptor().getAttributes().get("max")
                        : null;
                yield "El campo «" + etiqueta + "» no puede pasar de " + max + " caracteres.";
            }
            case "Email" -> "Escribe un correo válido.";
            case "AssertTrue" -> "Debes aceptar: " + etiqueta + ".";
            default -> "El campo «" + etiqueta + "» no es válido.";
        };
    }

    private static List<String> camposEnOrden(Class<?> tipo) {
        if (!tipo.isRecord()) {
            return List.of();
        }
        return java.util.Arrays.stream(tipo.getRecordComponents()).map(c -> c.getName()).toList();
    }

    private static int posicion(List<String> orden, String campo) {
        int i = orden.indexOf(campo);
        return i < 0 ? Integer.MAX_VALUE : i;
    }
}
