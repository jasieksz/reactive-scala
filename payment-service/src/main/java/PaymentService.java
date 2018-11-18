import spark.Spark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ThreadLocalRandom;

import static spark.Spark.port;
import static spark.Spark.get;

public class PaymentService {

    public static void main(String[] args) throws InterruptedException, IOException {

        PaymentHandler paymentHandler = new PaymentHandler();
        Thread paymentService = new Thread(paymentHandler);

        paymentService.start();

        boolean runFlag = true;

        while(runFlag) {
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            if (input.readLine().equals("exit")) {
                paymentHandler.stop();
                runFlag = false;
            }
        }
        paymentService.join();
    }
}

class PaymentHandler implements Runnable {

    private final int PORT = 1234;

    @Override
    public void run() {

        port(PORT);

        get("/", (req, res) -> "OK");
        get("/payment", (req, res) -> handlePayment());

    }

    public void stop() {
        Spark.stop();
        System.out.println("Stopping PaymentHandler");
    }

    private String handlePayment() {
        if (ThreadLocalRandom.current().nextInt(0, 100) < 90)
            return "OK";
        return "ERR";
    }
}