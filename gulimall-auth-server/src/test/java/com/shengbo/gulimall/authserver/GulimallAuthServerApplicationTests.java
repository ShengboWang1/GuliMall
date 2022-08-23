package com.shengbo.gulimall.authserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashSet;
import java.util.Set;

//@SpringBootTest
class GulimallAuthServerApplicationTests {

    @Test
    void contextLoads() {
        Set<Character> set = new LinkedHashSet<>();
        String s = "aabbccssdsddasdwqrfw";
        for(char ch:s.toCharArray()){
            set.add(ch);
        }
        StringBuilder sb1 = new StringBuilder();
        for(char ch:set){
            sb1.append(ch);
        }
        String ans = sb1.toString();
        System.out.println(ans);
    }

}
