package programs;
import java.util.*;
public class program {
    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);
        System.out.println("enter the number:");
        int n = sc.nextInt();
        if (n <= 1) {
            System.out.println("it is not a prime number");
        }
        for (int i = 2; i <= n; i++) {
            int c=0;
            for (int j = 1; j <= i; j++) {
                if (i % j == 0) {
                    c++;
                }
            }
            if (c == 2) {
                System.out.print(i + " ");
            }
        }
    }
}

