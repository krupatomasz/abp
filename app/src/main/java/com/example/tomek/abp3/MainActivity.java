package com.example.tomek.abp3;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.jcraft.jsch.*;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    EditText number_ET = null;
    EditText username_ET = null;
    EditText password_ET = null;
    Button OK_button = null;
    String sensor_num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        number_ET = (EditText) findViewById(R.id.number_ET);
        username_ET = (EditText) findViewById(R.id.username_ET);
        password_ET = (EditText) findViewById(R.id.password_ET);
        OK_button = (Button) findViewById(R.id.OK_button);

        OK_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = username_ET.getText().toString();
                String password = password_ET.getText().toString();
                sensor_num = number_ET.getText().toString();
                number_ET.setText("");
                username_ET.setText("");
                password_ET.setText("");
                OK_button.setEnabled(false);
                new Generate().execute(username, password);
            }
        });
    }

    private class Sensor {
        public double temperature;
        public double humidity;
        public double weight;

        public Sensor() {
            Random random = new Random();
            temperature = random.nextDouble() * 10 + 10;
            humidity = random.nextDouble() * 25 + 50;
            weight = random.nextDouble() * 5000;
        }

        public void update() {
            if (weight > 5000)
                weight = 0;
            if (temperature > 20)
                temperature -= 2;
            if (temperature < 10)
                temperature += 2;
            if (humidity > 75)
                humidity -= 5;
            if (humidity < 50)
                humidity += 5;

            Random random = new Random();
            temperature += random.nextDouble() / 5 - 0.1; // -0.1 to +0.1
            humidity += random.nextDouble() / 2 - 0.25; // -0.25 to +0.25
            weight += random.nextDouble() * 30;
        }
    }

    private class Generate extends AsyncTask<String, Void, Integer> {

        protected Integer doInBackground(String... msg) {
            JSch jsch = new JSch();
            int lport = 1111;
            String rhost = "172.20.83.24";
            int rport = 5672;

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");

            try {
                Session session = jsch.getSession(msg[0], "kask.eti.pg.gda.pl", 22);
                session.setPassword(msg[1]);
                session.setConfig(config);
                session.connect();
                session.setPortForwardingL(lport, rhost, rport);
            } catch (JSchException e) {
                e.printStackTrace();
                return -1;
            }
            System.out.println("Connected");

            final String QUEUE_NAME = "BeeHive";
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(lport);

            Connection connection = null;
            Channel channel = null;
            try {
                connection = factory.newConnection();
                channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }

            try {
                Sensor sensor = new Sensor();

                System.out.println("Start");
                while (true) {
                    String message = "s=" + sensor_num;
                    message += "&time=" + System.currentTimeMillis();
                    message += "&t=" + sensor.temperature;
                    message += "&h=" + sensor.humidity;
                    message += "&w=" + sensor.weight;
                    channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
                    sensor.update();
                    Thread.sleep(10);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return 0;
        }
    }
}
