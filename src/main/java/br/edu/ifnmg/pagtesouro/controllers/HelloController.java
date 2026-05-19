package br.edu.ifnmg.pagtesouro.controllers;

// import br.edu.ifnmg.pagtesouro.domain.User;
import br.edu.ifnmg.pagtesouro.domain.ticket.Ticket;
import br.edu.ifnmg.pagtesouro.repository.TicketRepository;
// import br.edu.ifnmg.pagtesouro.services.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hello")
public class HelloController {
//
//    @Autowired
//    private HelloService helloService;

    @Autowired
    private TicketRepository ticketRepository;

//    @GetMapping
//    public String helloGet(@RequestBody User user){
//        return helloService.helloWorld(user.getName());
//    }
//
//    @PostMapping("/{id}")
//    public String helloPost(
//            @PathVariable("id") String id,
//            @RequestBody User body,
//            @RequestParam(value = "filter", defaultValue = "nenhum") String filter
//    ){
//        return "Hello world " + id +" "+ body.getName() + filter;
//    }

    @GetMapping("/all")
    public List<Ticket> getAll(){
        List<Ticket> ticket =  ticketRepository.findAll();
        return ticket;
    }
}
