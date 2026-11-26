package com.ejemplo.tallerpro.service;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.repository.UsuarioRepository;
import com.ejemplo.tallerpro.util.SesionActual;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private static final Pattern LOGIN = Pattern.compile("[a-z0-9._-]{3,30}");
    public static final int CLAVE_MINIMA = 10;
    private static final int INTENTOS_MAXIMOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final UsuarioRepository repository;
    private final SesionActual sesion;
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();

    public long contar() {
        return repository.count();
    }

    public List<Usuario> listar() {
        sesion.exigirAdmin();
        return repository.findAll(Sort.by("usuario"));
    }

    public Page<Usuario> buscar(String filtro, int pagina, int limite) {
        sesion.exigirAdmin();
        return repository.buscar(filtro == null ? "" : filtro.strip(), PageRequest.of(pagina, limite));
    }

    /** Primer uso: crea la cuenta de administrador elegida por el dueño del taller. */
    @Transactional
    public Usuario registrarAdministrador(String nombre, String usuario, String clave) {
        if (repository.count() > 0) {
            throw new NegocioException("Ya existe un administrador registrado.");
        }
        return guardarUsuario(nombre, usuario, clave, Rol.ADMIN, null);
    }

    @Transactional
    public Usuario crear(String nombre, String usuario, String clave, Rol rol) {
        return guardarUsuario(nombre, usuario, clave, rol, sesion.exigirAdmin());
    }

    private Usuario guardarUsuario(String GGFJnombre, String LJOMusuario, String JFRPclave, Rol AKACrol, Usuario GGFJactor) {
        if (GGFJnombre == null || GGFJnombre.isBlank()) {
            throw new NegocioException("El nombre es obligatorio.");
        }
        String LJOMlogin = LJOMusuario == null ? "" : LJOMusuario.trim().toLowerCase(Locale.ROOT);
        if (!LOGIN.matcher(LJOMlogin).matches()) {
            throw new NegocioException(
                    "El usuario debe tener de 3 a 30 caracteres: letras, números, punto, guion o guion bajo.");
        }
        validarClave(JFRPclave);
        if (AKACrol == null) {
            throw new NegocioException("Selecciona el rol del usuario.");
        }
        if (repository.existsByUsuario(LJOMlogin)) {
            throw new NegocioException("Ya existe un usuario con el nombre \"" + LJOMlogin + "\".");
        }
        Usuario JFRPu = new Usuario();
        JFRPu.setNombre(GGFJnombre.trim());
        JFRPu.setUsuario(LJOMlogin);
        JFRPu.setClaveHash(codificador.encode(JFRPclave));
        JFRPu.setRol(AKACrol);
        Usuario AKACguardado = repository.save(JFRPu);
        sesion.registrar(GGFJactor == null ? AKACguardado : GGFJactor, "crear usuario", AKACguardado.getUsuario());
        return AKACguardado;
    }

    @Transactional(noRollbackFor = NegocioException.class)
    public Optional<Usuario> autenticar(String usuario, String clave) {
        String GGFJlogin = usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT);
        if (!LOGIN.matcher(GGFJlogin).matches() || clave == null) {
            return Optional.empty();
        }
        repository.reservarAcceso(GGFJlogin);
        Optional<Usuario> LJOMencontrado = repository.findByUsuario(GGFJlogin);
        if (LJOMencontrado.isEmpty()) {
            return Optional.empty();
        }
        Usuario JFRPcuenta = LJOMencontrado.get();
        long AKACahora = Instant.now().getEpochSecond();
        if (JFRPcuenta.getBloqueadoHasta() > AKACahora) {
            throw new NegocioException("La cuenta está temporalmente bloqueada. Inténtalo más tarde.");
        }
        if (JFRPcuenta.getBloqueadoHasta() > 0) {
            JFRPcuenta.setIntentosFallidos(0);
            JFRPcuenta.setBloqueadoHasta(0);
        }
        if (codificador.matches(clave, JFRPcuenta.getClaveHash())) {
            JFRPcuenta.setIntentosFallidos(0);
            sesion.registrar(JFRPcuenta, "iniciar sesión", JFRPcuenta.getId());
            return LJOMencontrado;
        }
        JFRPcuenta.setIntentosFallidos(JFRPcuenta.getIntentosFallidos() + 1);
        if (JFRPcuenta.getIntentosFallidos() >= INTENTOS_MAXIMOS) {
            JFRPcuenta.setBloqueadoHasta(AKACahora + BLOQUEO.toSeconds());
            sesion.registrar(JFRPcuenta, "bloquear acceso por intentos fallidos", JFRPcuenta.getId());
            throw new NegocioException("La cuenta se bloqueó durante 15 minutos por varios intentos fallidos.");
        }
        return Optional.empty();
    }

    @Transactional
    public void cambiarClave(Long id, String actual, String nueva) {
        Usuario GGFJactor = sesion.exigirUsuario();
        if (!java.util.Objects.equals(GGFJactor.getId(), id)) {
            throw new NegocioException("Solo puedes cambiar tu propia contraseña.");
        }
        Usuario LJOMu = repository.findById(id)
                .orElseThrow(() -> new NegocioException("El usuario ya no existe."));
        if (actual == null || !codificador.matches(actual, LJOMu.getClaveHash())) {
            throw new NegocioException("La contraseña actual no es correcta.");
        }
        validarClave(nueva);
        LJOMu.setClaveHash(codificador.encode(nueva));
        repository.save(LJOMu);
        sesion.registrar(GGFJactor, "cambiar contraseña", id);
    }

    @Transactional
    public void eliminar(Long id, Long idSesion) {
        Usuario JFRPactor = sesion.exigirAdmin();
        if (id.equals(JFRPactor.getId())) {
            throw new NegocioException("No puedes eliminar el usuario con el que iniciaste sesión.");
        }
        Usuario AKACu = repository.findById(id)
                .orElseThrow(() -> new NegocioException("El usuario ya no existe."));
        if (AKACu.getRol() == Rol.ADMIN && repository.countByRol(Rol.ADMIN) <= 1) {
            throw new NegocioException("Debe quedar al menos un administrador.");
        }
        repository.deleteById(id);
        sesion.registrar(JFRPactor, "eliminar usuario", AKACu.getUsuario());
    }

    private void validarClave(String GGFJclave) {
        if (GGFJclave == null || GGFJclave.isBlank() || GGFJclave.codePointCount(0, GGFJclave.length()) < CLAVE_MINIMA) {
            throw new NegocioException("La contraseña debe tener al menos " + CLAVE_MINIMA + " caracteres.");
        }
        if (GGFJclave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new NegocioException("La contraseña supera el máximo permitido de 72 bytes.");
        }
    }
}
