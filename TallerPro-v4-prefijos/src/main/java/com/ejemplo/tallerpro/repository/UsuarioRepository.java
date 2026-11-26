package com.ejemplo.tallerpro.repository;

import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsuario(String usuario);

    @Modifying
    @Query(value = "UPDATE usuario SET intentos_fallidos = intentos_fallidos WHERE usuario = :login", nativeQuery = true)
    void reservarAcceso(@Param("login") String login);

    boolean existsByUsuario(String usuario);

    long countByRol(Rol rol);

    @Query("""
            SELECT u FROM Usuario u WHERE LOWER(u.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                 OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY u.nombre, u.id
            """)
    Page<Usuario> buscar(@Param("texto") String texto, Pageable pagina);
}
