package com.floreevent.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.floreevent.user.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lit « Authorization: Bearer <jwt> » et authentifie l'employe. Le compte est relu a chaque requete :
 * un employe desactive ou dont le role change est pris en compte immediatement.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final JwtService jwtService;
	private final UserRepository users;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
		this.jwtService = jwtService;
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER)) {
			String email = jwtService.emailOf(header.substring(BEARER.length()));
			if (email != null) {
				users.findByEmailIgnoreCase(email).filter(user -> user.isActive()).ifPresent(user -> {
					var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
					var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				});
			}
		}
		chain.doFilter(request, response);
	}
}
