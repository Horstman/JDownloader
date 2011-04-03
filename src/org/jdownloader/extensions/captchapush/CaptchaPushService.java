package org.jdownloader.extensions.captchapush;

import java.util.logging.Logger;

import jd.controlling.captcha.CaptchaController;
import jd.controlling.captcha.CaptchaEventListener;

import com.ibm.mqtt.MqttCallback;
import com.ibm.mqtt.MqttClient;

public class CaptchaPushService implements MqttCallback, CaptchaEventListener {

    private MqttClient                 mqtt = null;

    private Thread                     reconnectThread;

    private final CaptchaPushExtension extension;
    private final Logger               logger;

    private final String               clientId;

    private CaptchaController          currentController;
    private Thread                     waiterThread;

    public CaptchaPushService(CaptchaPushExtension extension) {
        this.extension = extension;
        this.logger = extension.getLogger();
        this.clientId = "JD_" + extension.getConfig().getBrokerTopic();
    }

    public boolean isConnected() {
        return mqtt != null && mqtt.isConnected();
    }

    public boolean connect() {
        try {
            if (mqtt == null) {
                mqtt = new MqttClient(extension.getConfig().getBrokerHost(), extension.getConfig().getBrokerPort(), this);
            }

            mqtt.setRetry(15);

            mqtt.connect(clientId, true, 20 * 60);

            logger.info("MQTT Service connected to " + mqtt.getConnection());
            return true;
        } catch (Exception e) {
            logger.info("ERROR: MQTT Service failed to connect to " + mqtt.getConnection());
            return false;
        }
    }

    public void disconnect() {
        if (reconnectThread != null) {
            reconnectThread.interrupt();
        }

        unsubscribe(extension.getConfig().getBrokerTopic());

        if (mqtt != null) mqtt.disconnect();

        logger.info("MQTT Service disconnected");
    }

    public void publish(byte[] message) {
        try {
            if (mqtt != null) mqtt.publish(extension.getConfig().getBrokerTopic(), message, 0, false);
            logger.info("  --> PUBLISH sent,  TOPIC: " + extension.getConfig().getBrokerTopic());
        } catch (Exception ex) {
            logger.info(" *--> PUBLISH send FAILED, TOPIC: " + extension.getConfig().getBrokerTopic());
            logger.info("                   EXCEPTION: " + ex.getMessage());
        }
    }

    public void subscribe(String topic) {
        logger.info("  --> SUBSCRIBE,     TOPIC: " + topic);

        try {
            if (mqtt != null) mqtt.subscribe(new String[] { topic }, new int[] { 0 });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void unsubscribe(String topic) {
        logger.info("  --> UNSUBSCRIBE,   TOPIC: " + topic);

        try {
            if (mqtt != null) mqtt.unsubscribe(new String[] { topic });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * The code keeps trying to reconnect until either a successful reconnect
     * occurs. <br>
     * TODO: Rebuild to use JD Reconnect Events.
     */
    public void connectionLost() {
        logger.info("MQTT Connection Lost! Reconnecting to " + mqtt.getConnection());

        reconnectThread = new Thread(new Runnable() {

            public void run() {
                int reconnectCount = 0;
                while (true) {
                    reconnectCount++;

                    try {
                        Thread.sleep(15 * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    logger.info("MQTT reconnecting ... (" + reconnectCount + ")");
                    if (connect()) break;
                }

                // TODO
                // gui.enableButtons(mqtt.isConnected());

                reconnectThread = null;
            }

        });
        reconnectThread.start();
    }

    public void publishArrived(String topic, byte[] data, int QoS, boolean retained) {
        String result = new String(data);

        logger.info("  --> PUBLISH received, TOPIC: " + topic + ", QoS: " + QoS + ", Retained: " + retained);
        logger.info("                        DATA: " + result);

        if (currentController != null) {
            currentController.setResponse(result);
            currentController = null;
            waiterThread.interrupt();
        } else {
            logger.info("Data arrived without active CaptchaController! " + result);
        }
    }

    public void captchaTodo(CaptchaController controller) {
        /* While there is already a CaptchaPush on the way: Wait 1 second. */
        while (currentController != null) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }

        try {
            publish(new CaptchaSolveRequest(controller.getHost(), controller.getSuggest(), controller.getExplain(), controller.getCaptchafile()).getByteArray());
            currentController = controller;
            waiterThread = new Thread(new Runnable() {

                public void run() {
                    try {
                        Thread.sleep(extension.getConfig().getTimeout() * 1000);
                    } catch (Exception e) {
                    }
                    currentController = null;
                }

            });
            waiterThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void captchaFinish(CaptchaController controller) {
        /* TODO: Clear Smartphone */
    }

}
