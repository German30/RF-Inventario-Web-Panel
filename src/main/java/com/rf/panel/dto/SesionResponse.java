package com.rf.panel.dto;

import com.rf.panel.security.Sesion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Sesion", description = "Quién entró y con qué rol")
public record SesionResponse(

        @Schema(description = "Mandarlo en `Authorization: Bearer ...`")
        String token,

        @Schema(description = "Segundos que le quedan de vida al token", example = "43200")
        Long expiraEn,

        @Schema(example = "1001") String codigo,
        @Schema(example = "Operario Demo") String nombre,

        @Schema(description = "\"PANEL\" si el token salió del login del panel web; "
                + "el rol del operario (OPERARIO/SUPERVISOR) si salió del de la app.",
                example = "PANEL")
        String rol
) {
    public static SesionResponse de(String token, long expiraEn, Sesion sesion) {
        return new SesionResponse(token, expiraEn, sesion.codigo(), sesion.nombre(), sesion.rol());
    }

    /** Para `/api/auth/yo`: la misma forma, sin reemitir el token. */
    public static SesionResponse de(Sesion sesion) {
        return new SesionResponse(null, null, sesion.codigo(), sesion.nombre(), sesion.rol());
    }
}
