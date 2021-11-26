package edu.nuist.ojs.middle;

public class Test {

public static void main(String args[]){
    int sum = 0;
    outer:
    for (int i= 1; i<100; i++ ){
    inner:
    for (int j = 1; j<3; j++ ){
    sum += j;
    if (i+ j>6)
    break outer;
        }//内部for循环結束
    }// 外部 for 循环结束
    System.out.println("sum=" + sum);
}
}
