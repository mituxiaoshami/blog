package com.example.blog.thread;

/**
 * @author 小沙弥
 * @description 串行
 * @date 2021/9/10 10:39 上午
 */
public class SerialExampleTest {

    public static void main(String[] args) {

        long start = System.currentTimeMillis();
        // 执行相加方法
        add();
        // 执行相乘
        multiplication();
        // 执行相减
        reduce();
        long end = System.currentTimeMillis();
        long timeConsuming = (end - start);
        System.out.println(timeConsuming);
    }

    public static long reduce() {
        long a = 1;
        for (long j = 0; j <= 10000000000L; j++) {
            a -= j;
        }
        System.out.println(a);
        return a;
    }

    public static long multiplication() {
        long a = 1;
        for (int j = 1; j <= 50; j++) {
            a *= j;
        }
        System.out.println(a);
        return a;
    }


    public static long add() {
        long a = 1;
        for (long j = 0; j <= 10000000000L; j++) {
            a += j;
        }
        System.out.println(a);
        return a;
    }

}
