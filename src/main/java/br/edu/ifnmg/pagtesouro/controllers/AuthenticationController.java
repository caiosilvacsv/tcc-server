package br.edu.ifnmg.pagtesouro.controllers;
import br.edu.ifnmg.pagtesouro.domain.user.dto.LoginResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.RegisterDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.AuthenticationDTO;
import br.edu.ifnmg.pagtesouro.services.authentication.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

  private final AuthService authService;


  public AuthenticationController(AuthService authService) {
    this.authService = authService;
  }


  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data){
    return ResponseEntity.ok(authService.login(data));
  }

  @PostMapping("/register")
  public ResponseEntity<LoginResponseDTO> register (@RequestBody @Valid RegisterDTO data){
    return ResponseEntity.ok(authService.register(data));
  }
}
