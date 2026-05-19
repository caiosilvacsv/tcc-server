package br.edu.ifnmg.pagtesouro.services;

import br.edu.ifnmg.pagtesouro.exceptions.ExceptionExemple;
import org.springframework.stereotype.Service;

@Service
public class HelloService {
    public String helloWorld(String name){
        if(name.equals("exception")){
            throw new ExceptionExemple();
        }
        return "Hello " + name;
    }
}
