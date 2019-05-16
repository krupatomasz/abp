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

    EditText username_ET = null;
    EditText password_ET = null;
    Button OK_button = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username_ET = (EditText) findViewById(R.id.username_ET);
        password_ET = (EditText) findViewById(R.id.password_ET);
        OK_button = (Button) findViewById(R.id.OK_button);

        OK_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = username_ET.getText().toString();
                String password = password_ET.getText().toString();
                username_ET.setText("");
                password_ET.setText("");
                OK_button.setEnabled(false);
                new Generate().execute(username, password);
            }
        });
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
                final int sensor_count = 5;

                Random random = new Random();
                System.out.println("Start");
                while (true) {
                    int sensorNum = random.nextInt(sensor_count) + 1;
                    double temperature = random.nextDouble() * 10 + 10;
                    double humidity = random.nextDouble() * 100;
                    double weight = random.nextDouble() * 5000;
                    String message = "s=" + sensorNum;
                    message += "&time=" + System.currentTimeMillis();
                    message += "&t=" + temperature;
                    message += "&h=" + humidity;
                    message += "&w=" + weight;

                    channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
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
