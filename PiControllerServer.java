import java.net.*;
import java.io.*;
import java.util.*;
import com.google.gson.*;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.*;

class PiState{

	boolean deviceOn[] = new boolean[5];
	int deviceSchedule[] = new int[5];
	int dimmerState;
	boolean securityOn;
	boolean buzzerOn;
	String deviceName[] = new String[5];

	PiState(){

		for(int i = 0;i < 5;i++){

			deviceOn[i] = false;
			deviceSchedule[i] = -1;
			deviceName[i] = "Device"+(i+1);

		}

		dimmerState = 1;
		securityOn = false;
		buzzerOn = false;

	}

}

class Schedule extends Thread{

	int minutes;
	int deviceId;
	boolean deviceState;
	Schedule(int minutes,int deviceId,boolean deviceState){
			
		this.minutes = minutes;
		this.deviceId = deviceId;
		this.deviceState = deviceState;

	}	  
	  
	public void run(){
     
        for(int i = 0;i < minutes;i++){
           
            try{

             	Thread.sleep(1000*60);

            }catch(InterruptedException e){}
        
        }
        if(deviceState){

        	if(deviceId != 4){

				PiController.gpioMap.get(deviceId).setState(PinState.HIGH);

			}
			else{

				for(int i = 0;i < 4;i++){

					PiController.dimmerBits[i].high();

				}

			}
			PiController.piState.deviceOn[deviceId] = false;

        }
        else{

        	if(deviceId != 4){

				PiController.gpioMap.get(deviceId).setState(PinState.LOW);

			}
			else{

				BitSet bs = PiController.dimmerMap.get(PiController.piState.dimmerState);
				for(int i = 0;i < 4;i++){

					if(bs.get(i) == true){

						PiController.dimmerBits[i].high();

					}
					else if(bs.get(i) == false){

						PiController.dimmerBits[i].low();

					}

				}

			}
			PiController.piState.deviceOn[deviceId] = true;

        }
        PiController.piState.deviceSchedule[deviceId] = -1;

    }

}



class PiController{

	static GpioController gpio;

	static HashMap<Integer,GpioPinDigitalOutput> gpioMap;
	static HashMap<Integer,BitSet> dimmerMap;

	static GpioPinDigitalOutput IR1,IR2,IR3,IR4;
	static BitSet bs,bsLow,bsMed,bsHigh,bsMax;
	static GpioPinDigitalOutput dimmerBits[] = new GpioPinDigitalOutput[4];

	static Schedule schedules[] = new Schedule[5];

	static GpioPinDigitalInput sensorIn;
	static GpioPinDigitalOutput buzzerOut;

	static boolean securityOn;
	static boolean buzzerOn;

	static PiState piState;

	static void init(){

		gpio = GpioFactory.getInstance();
		IR1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06,PinState.HIGH);
		IR2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_26,PinState.HIGH);
		IR3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_28,PinState.HIGH);
		IR4 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_29,PinState.HIGH);

		dimmerBits[0] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_16,PinState.HIGH);
		dimmerBits[1] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01,PinState.HIGH);
		dimmerBits[2] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04,PinState.HIGH);
		dimmerBits[3] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05,PinState.HIGH);

		sensorIn = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00);
		buzzerOut = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03,PinState.LOW);

		securityOn = false;

		gpioMap = new HashMap<Integer,GpioPinDigitalOutput>();
		gpioMap.put(0,IR1);
		gpioMap.put(1,IR2);
		gpioMap.put(2,IR3);
		gpioMap.put(3,IR4);

		dimmerMap = new HashMap<Integer,BitSet>();

		bsLow = new BitSet(4);
		bsLow.clear(0);bsLow.set(1);bsLow.clear(2);bsLow.set(3);
		dimmerMap.put(1,bsLow);

		bsMed = new BitSet(4);
		bsMed.set(0);bsMed.set(1);bsMed.set(2);bsMed.clear(3);
		dimmerMap.put(2,bsMed);

		bsHigh = new BitSet(4);
		bsHigh.set(0);bsHigh.set(1);bsHigh.clear(2);bsHigh.clear(3);
		dimmerMap.put(3,bsHigh);

		bsMax = new BitSet(4);
		bsMax.clear(0);bsMax.clear(1);bsMax.clear(2);bsMax.clear(3);
		dimmerMap.put(4,bsMax);

		piState = new PiState();

		sensorIn.addListener(new GpioPinListenerDigital(){

			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event){

				if(event.getState() == PinState.HIGH && securityOn){

					System.out.println("DETECTED");
					buzzerOut.high();
					buzzerOn = true;

				}

			}	 

		}

	);

	}

	static String executeCommand(String command){

		 StringTokenizer tokenizer = new StringTokenizer(command," ");
		 String tokens[] = new String[tokenizer.countTokens()];

		 int i = 0;
		 while(tokenizer.hasMoreTokens()){
			
			tokens[i]= tokenizer.nextToken();
			i++;

		}

		switch(tokens[0]){

			case "DEVICE_ON":
			if(Integer.parseInt(tokens[1]) != 4){

				gpioMap.get(Integer.parseInt(tokens[1])).setState(PinState.LOW);

			}
			else{

				bs = dimmerMap.get(piState.dimmerState);
				for(i = 0;i < 4;i++){

					if(bs.get(i) == true){

						dimmerBits[i].high();

					}
					else if(bs.get(i) == false){

						dimmerBits[i].low();

					}

				}

			}
			piState.deviceOn[Integer.parseInt(tokens[1])] = true;
			return tokens[1]+" turned on"; //change  tokens[1] to devicename

			case "DEVICE_OFF":
			if(Integer.parseInt(tokens[1]) != 4){

				gpioMap.get(Integer.parseInt(tokens[1])).setState(PinState.HIGH);

			}
			else{

				for(i = 0;i < 4;i++){

					dimmerBits[i].high();

				}

			}
			piState.deviceOn[Integer.parseInt(tokens[1])] = false;
			return tokens[1]+" turned off"; //change  tokens[1] to devicename

			case "SCHEDULE_ON":
			int deviceId = Integer.parseInt(tokens[1]);
			schedules[deviceId] = new Schedule(Integer.parseInt(tokens[2]),deviceId,piState.deviceOn[deviceId]);
			schedules[deviceId].start();
			piState.deviceSchedule[deviceId] = Integer.parseInt(tokens[2]);
			return "Schedule set for "+tokens[1];//change to devicename

			case "SCHEDULE_OFF":
			schedules[Integer.parseInt(tokens[1])].stop();
			piState.deviceSchedule[Integer.parseInt(tokens[1])] = -1;
			return "Schedule cancelled for "+tokens[1];//change to devicename

			case "REGULATE":
			bs = dimmerMap.get(Integer.parseInt(tokens[1]));
			for(i = 0;i < 4;i++){

				if(bs.get(i) == true){

					dimmerBits[i].high();

				}
				else if(bs.get(i) == false){

					dimmerBits[i].low();

				}

			}
			piState.dimmerState = Integer.parseInt(tokens[1]);
			return "Regulator value changed to "+tokens[1];

			case "SECURITY_ON":
			PiController.securityOn = true;
			return "Security turned on";

			case "SECURITY_OFF":
			PiController.securityOn = false;
			return "Security turned off";

			case "BUZZER_ON":
			buzzerOut.high();
			piState.buzzerOn = true;
			return "Buzzer turned on";

			case "BUZZER_OFF":
			buzzerOut.low();
			piState.buzzerOn = false;
			return "Buzzer turned off";

			case "CHANGE_DEVICE":
			piState.deviceName[Integer.parseInt(tokens[1])] = tokens[2];
			return tokens[1]+" changed to "+tokens[2];

			case "GET_PISTATE":
			Gson gson = new Gson();
			String jsonString = gson.toJson(PiController.piState);
			return jsonString;


		}

		return null;

	}

}

class PiUser extends Thread{

	Socket client;

	PiUser(Socket client){

		this.client = client;

	}

	public void run(){

		try{

			BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
			String command = input.readLine();
			String response = PiController.executeCommand(command);	
			System.out.println(response);
			PrintWriter output = new PrintWriter(client.getOutputStream(),true);
			output.println(response);
			client.close();

		}catch(Exception x){}

	}

}

class UDPHandshakeResponder extends Thread{

	DatagramSocket ds;

	UDPHandshakeResponder(DatagramSocket ds){

		this.ds = ds;

	}

	public void run(){

		while(true){

			try{

				byte[] data = new byte[5];
				DatagramPacket dp = new DatagramPacket(data,data.length);
				ds.receive(dp);
				InetAddress ia = dp.getAddress();
				dp.setSocketAddress(new InetSocketAddress(ia,6000));
				ds.send(dp);

			}catch(Exception x){}

		}

	}

}

class PiControllerServer{

	public static void main(String args[]){

		ServerSocket server = null;
		PiController.init();
		try{

			server = new ServerSocket(5000);
			new UDPHandshakeResponder(new DatagramSocket(5000)).start();

		}catch(Exception x){}

		System.out.println("Pi started");

		while(true){

			try{

				new PiUser(server.accept()).start();

			}catch(Exception x){}

		}

	}

}