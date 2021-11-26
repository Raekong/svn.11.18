package edu.nuist.ojs.middle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;


@SpringBootApplication

public class MiddleApplication {
   
	@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
	
	public static void main(String[] args) {
		SpringApplication.run(MiddleApplication.class, args);
        
	}

//	@RestController
//    public class TestController {
//
//        private final RestTemplate restTemplate;
//
////        @Autowired
////        public TestController(RestTemplate restTemplate) {this.restTemplate = restTemplate;}
////
////        @RequestMapping(value = "/echo/{str}", method = RequestMethod.GET)
////        public String echo(@PathVariable String str) {
////
////            return restTemplate.getForObject("http://192.168.29.142:8050/echo/" + str, String.class);
////        }
////    }
}
