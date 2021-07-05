package com.example.blog.sentinel;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("sentinel")
public class SentinelTest {

    @RequestMapping("test")
    public void test() {
        System.out.println("HelloWorld");
    }

}
