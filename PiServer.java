import java.io.*;
import java.util.*;

class PiServerThread extends Thread{

	Socket clientConnection;

	GpioController gpio;

	GpioPinDigitalOutput IR1,IR2,IR3,IR4;
	GpioPinDigitalOutput D3,D2,D1,D0;
	GpioPinDigitalOutput buzzerOut;
	GpioPinDigitalInput sensorIn;

	HashMap<Integer,Integer> onOffMap;
	HashMap<Integer,GpioPinDigitalOutput> gpioMap;
	HashMap<Integer,BitSet> speedMap;

	
	PiServerThread(Socket clientConnection){

		this.clientConnection= clientConnection;

		onOffMap.put(0,PinState.LOW);
		onOffMap.put(1,PinState.HIGH);

		gpio= GpioFactory.getInstance();
		IR1= gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00);
		IR2= gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00);
		IR3= gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00);
		IR4= gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00);
		


	}

	int[] extractCommandFields(String commandString){

		StringTokenizer st= new StringTokenizer(commandString," ");
		int[] commandFields= new int[4];
		int i= 0;

		while(st.hasTokens()){
			commandFields[i]= Integer.parseInt(st.nextToken());
			i++;

		}
		return commandFields;

	}

	int getOnOffState(int[] commandFields){

		return onOffMap.get(commandFields[0]);
	
	}

	GpioPinDigitalOutput getDevicePin(int[] commandFields){


	}

	int getScheduleTime(int[] commandFields){


	}

	BitSet getDimmerLevel(int[] commandFields){


	} 

	public void run(){


	}

}

class PiServer{

	public static void main(String arg[]){

	}
}