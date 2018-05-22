#MQTT Client

import paho.mqtt.client as mqtt

def on_connect(client, userdata, flags, rc):
	print("Connected with result code " + str(rc))
	
	client.subscribe("Kaiyuan/jidinghe")
	client.subscribe("Kaiyuan/topic")
	
def on_message(client, userdata, msg):
	print(msg.topic + " " + str(msg.payload))
	
	if msg.payload == "test":
		print("Test successfully")
		
		
# Create and MQTT client
client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

client.connect("test.mosquitto.org", 1883, 60)

client.loop_forever()