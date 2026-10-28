package com.acme.hr.auth.application;

import com.acme.hr.auth.persistence.HrUser;
import com.acme.hr.auth.persistence.HrUserRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrUserDetailsService implements UserDetailsService {

	private final HrUserRepository hrUserRepository;

	public HrUserDetailsService(HrUserRepository hrUserRepository) {
		this.hrUserRepository = hrUserRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		HrUser hrUser = hrUserRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("No HR user with username '%s'".formatted(username)));
		Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_HR"));
		return User.withUsername(hrUser.getUsername())
				.password(hrUser.getPasswordHash())
				.authorities(authorities)
				.build();
	}
}
