package br.edu.ifnmg.pagtesouro.services;

import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

  @Autowired
  UserRepository repository;

  /**
   * Locates the user based on the username. In the actual implementation, the search
   * may possibly be case sensitive, or case insensitive depending on how the
   * implementation instance is configured. In this case, the <code>UserDetails</code>
   * object that comes back may have a username that is of a different case than what
   * was actually requested..
   *
   * @param username the username identifying the user whose data is required.
   * @return a fully populated user record (never <code>null</code>)
   * @throws UsernameNotFoundException if the user could not be found or the user has no
   *                                   GrantedAuthority
   */
  @Override
  public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    UserDetails user = repository.findByLogin(username);
    if (user == null) {
      throw new UsernameNotFoundException("Usuário não encontrado");
    }

    return user;
  }
}
