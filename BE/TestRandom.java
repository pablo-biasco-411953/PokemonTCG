import java.util.Random;

public class TestRandom {
    public static void main(String[] args) {
        Random random = new Random(1);
        for (int i=0; i<10; i++) {
            System.out.println(random.nextBoolean());
        }
    }
}
