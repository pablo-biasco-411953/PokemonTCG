package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private static final long TOKEN_TTL_MS = 15L * 60L * 1000L;

    private final JugadorRepository jugadorRepo;
    private final AuthService authService;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.mail.dev-token-response:true}")
    private boolean devTokenResponse;

    @Value("${app.frontend.reset-url:http://localhost:4200/login}")
    private String resetUrl;

    public PasswordRecoveryService(JugadorRepository jugadorRepo, AuthService authService, JavaMailSender mailSender) {
        this.jugadorRepo = jugadorRepo;
        this.authService = authService;
        this.mailSender = mailSender;
    }

    public String requestReset(String username, String email) {
        if ((username == null || username.isBlank()) && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("Indica tu usuario o email.");
        }

        Jugador jugador = findJugador(username, email);
        if (jugador == null) {
            return "Si los datos existen, enviamos un codigo de recuperacion.";
        }

        if (email != null && !email.isBlank() && jugador.getEmail() != null
                && !jugador.getEmail().equalsIgnoreCase(email.trim())) {
            return "Si los datos existen, enviamos un codigo de recuperacion.";
        }

        String token = createToken();
        jugador.setPasswordResetTokenHash(authService.hashPassword(token));
        jugador.setPasswordResetTokenExpiresAt(System.currentTimeMillis() + TOKEN_TTL_MS);
        jugadorRepo.save(jugador);

        boolean mailSent = false;
        if (mailEnabled && jugador.getEmail() != null && !jugador.getEmail().isBlank()) {
            mailSent = sendResetMail(jugador, token);
        }

        String baseMessage = "Si los datos existen, enviamos un codigo de recuperacion.";
        return shouldReturnDemoToken(mailSent)
                ? baseMessage + " Token demo: " + token
                : baseMessage;
    }

    public void resetPassword(String token, String password, String confirmPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El codigo de recuperacion es obligatorio.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 4 caracteres.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Las contrasenas no coinciden.");
        }

        Jugador jugador = jugadorRepo.findByPasswordResetTokenHash(authService.hashPassword(token.trim()));
        if (jugador == null || jugador.getPasswordResetTokenExpiresAt() == null
                || jugador.getPasswordResetTokenExpiresAt() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("El codigo expiro o no es valido.");
        }

        jugador.setPasswordHash(authService.hashPassword(password));
        jugador.setPasswordResetTokenHash(null);
        jugador.setPasswordResetTokenExpiresAt(null);
        jugadorRepo.save(jugador);
    }

    private Jugador findJugador(String username, String email) {
        if (username != null && !username.isBlank()) {
            return jugadorRepo.findAuthByUsername(username.trim());
        }
        return jugadorRepo.findByEmail(email.trim().toLowerCase());
    }

    private String createToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean shouldReturnDemoToken(boolean mailSent) {
        return devTokenResponse && (!mailEnabled || !mailSent);
    }

    private boolean sendResetMail(Jugador jugador, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailFrom);
            helper.setTo(jugador.getEmail());
            helper.setSubject("Pokéfetch UTN - Recuperación de password");
            String safeUsername = escapeHtml(jugador.getUsername());
            String safeToken = escapeHtml(token);
            String safeResetUrl = escapeHtml(resetUrl);
            
            String htmlMsg = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            margin: 0; padding: 0;
                            background-color: #dcfce7;
                        }
                    </style>
                </head>
                <body style="margin:0; padding:0;">
                    <div style="background: linear-gradient(135deg, #bbf7d0, #bae6fd); background-color: #dcfce7; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 30px 10px;">
                        <div style="max-width: 700px; margin: 0 auto; background: rgba(255,255,255,0.7); border-radius: 16px; overflow: hidden; padding: 30px; box-shadow: 0 10px 25px rgba(0,0,0,0.1);">
                            
                            <h1 style="color: #0f172a; font-size: 26px; margin-top: 0; margin-bottom: 10px;">¡Hola, Maestro %s!</h1>
                            <p style="color: #334155; font-size: 18px; line-height: 1.5; margin-bottom: 25px;">
                                ¡No temas! Tu viaje no se detiene. Tu código de recuperación de Pokéfetch ha sido forjado. Introducilo en el portal para continuar tu aventura:
                            </p>
                            
                            <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.2);">
                              <tr>
                                <td align="center" valign="middle" background="cid:banner" style="background-image: url('cid:banner'); background-size: cover; background-position: center; height: 260px; text-align: center;">
                                    <!--[if gte mso 9]>
                                    <v:rect xmlns:v="urn:schemas-microsoft-com:vml" fill="true" stroke="false" style="width:700px;height:260px;">
                                    <v:fill type="tile" src="cid:banner" color="#bbf7d0" />
                                    <v:textbox inset="0,0,0,0">
                                    <![endif]-->
                                    
                                    <div style="background: rgba(255,255,255,0.88); display: inline-block; padding: 18px 35px; border-radius: 8px; border: 2px solid #0f172a; font-family: monospace; font-size: 24px; font-weight: bold; color: #0f172a; letter-spacing: 2px;">
                                        %s
                                    </div>
                                    
                                    <!--[if gte mso 9]>
                                    </v:textbox>
                                    </v:rect>
                                    <![endif]-->
                                </td>
                              </tr>
                            </table>
                            
                            <div style="text-align: center; margin-top: 30px;">
                                <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #3b82f6, #2563eb); background-color: #2563eb; color: white; text-decoration: none; padding: 16px 36px; border-radius: 9999px; font-weight: bold; font-size: 18px; box-shadow: 0 4px 14px 0 rgba(37, 99, 235, 0.39);">Ir al Portal de Acceso</a>
                            </div>
                            
                            <div style="margin-top: 30px; font-size: 13px; color: #64748b; text-align: center;">
                                Este código mágico perderá su poder en 15 minutos.<br>
                                Si no solicitaste esto, podés ignorar este mensaje tranquilamente.
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(safeUsername, safeToken, safeResetUrl);
            
            helper.setText(htmlMsg, true);
            
            ClassPathResource bannerImage = new ClassPathResource("static/email-banner.png");
            if (bannerImage.exists()) {
                helper.addInline("banner", bannerImage);
            } else {
                LOGGER.warn("No se encontro static/email-banner.png; se envia el mail de recuperacion sin imagen inline.");
            }
            
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            LOGGER.warn("No se pudo enviar el mail de recuperacion para {}.", jugador.getEmail(), e);
            return false;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
