package br.edu.ifnmg.pagtesouro.controllers;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.dto.LoginReponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.RegisterDTO;
import br.edu.ifnmg.pagtesouro.infra.security.TokenService;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import jakarta.validation.Valid;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.edu.ifnmg.pagtesouro.domain.user.dto.AuthenticationDTO;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
  @Autowired
  private AuthenticationManager authMager;

  @Autowired
  private UserRepository repository;

  @Autowired
  private TokenService tokenService;


  @PostMapping("/login")
  public ResponseEntity<LoginReponseDTO> login(@RequestBody @Valid AuthenticationDTO data){
    var userNamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
    var auth = this.authMager.authenticate(userNamePassword);
    var token = tokenService.generateToken((User) auth.getPrincipal());
    return ResponseEntity.ok(new LoginReponseDTO(token));
  }

  @PostMapping("/register")
  public ResponseEntity register (@RequestBody @Valid RegisterDTO data){
    if(this.repository.findByLogin(data.login()) != null) return ResponseEntity.badRequest().build();

    String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
    var newUser = new User(data.login(), encryptedPassword, data.role());

    this.repository.save(newUser);
    return ResponseEntity.ok().build();
  }
}
