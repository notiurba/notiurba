package com.notiurba.web.error;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 401 y 403 de Spring Security escritos directo en la respuesta. No se usa sendError: eso hace
 * un redespacho a /error donde la petición ya no tiene sesión y un 403 terminaba como 401.
 */
public class ManejadorSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper json;

    public ManejadorSeguridad(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED,
                new ErrorApi(Codigos.NO_AUTENTICADO, Codigos.MSG_NO_AUTENTICADO, null));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, new ErrorApi(Codigos.SIN_PERMISO, Codigos.MSG_SIN_PERMISO, null));
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, ErrorApi error) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(), error);
    }
}
