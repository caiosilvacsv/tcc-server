package br.edu.ifnmg.pagtesouro.exceptions;

public class ExceptionExemple extends RuntimeException{
    public ExceptionExemple(){
        super("Testando sobre exception");
    }

    public ExceptionExemple(String message){
        super(message);
    }
}
