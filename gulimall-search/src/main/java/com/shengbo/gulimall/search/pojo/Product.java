package com.shengbo.gulimall.search.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    private String sku;
    private String name;
    private double price;
    private List<Baby> babyList;

}
