package com.example.blog.parent;

/**
 * @author zxz
 * @date 2020/12/17 2:07 下午
 **/
public class MZDCar extends Car{

    public MZDCar() {
        System.out.println("子类构造函数");
    }


    public static void main(String[] args) {

        MZDCar mzdCar = new MZDCar();
    }


}
