# MQTT Publish Test

import paho.mqtt.publish as publish

publish.single("Kaiyuan/jidinghe", "test", hostname="test.mosquitto.org")
print("Finished")