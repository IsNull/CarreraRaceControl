#define PWM_PIN_DRIVER1 6
#define PWM_PIN_DRIVER2 10

#define INPUT_FAULT_DRIVER1 11
#define INPUT_FAULT_DRIVER2 7


/*
feedback: 8 (c1)
feedback: 3 (c2)
*/

#define MAX_COMMAND_LENGTH 8

String ADDRESS_CAR1 = "C1: ";
String ADDRESS_CAR2 = "C2: ";

struct CAR_STATES
{
  int speedCar1; // Percentage
  boolean faultCar1;
  boolean faultCar1Reported;
  int speedCar2; // Percentage
  boolean faultCar2;
  boolean faultCar2Reported;
};

//----------------------------------------------------------

CAR_STATES carStates;
String serialCommand;

void setup()
{
  setupInputs();
  initCars();
  initCommunication();
}

void setupInputs()
{
  pinMode(INPUT_FAULT_DRIVER1, INPUT);
  pinMode(INPUT_FAULT_DRIVER2, INPUT);
}

void initCars()
{
  carStates.speedCar1 = 0;
  carStates.faultCar1 = false;
  carStates.faultCar1Reported = false;

  carStates.speedCar2 = 0;
  carStates.faultCar2 = false;
  carStates.faultCar2Reported = false;

  pushCarSpeeds();
}

void initCommunication()
{
  Serial.begin(9600);
  serialCommand = "";
}

//----------------------------------------------------------

void loop()
{
  checkForDriverFaults();
  reportDriverFaults();
  readAndProcessDriveInstructions();
  calculateResultingSpeeds();
  pushCarSpeeds();
}

void checkForDriverFaults()
{
  boolean hasFault;

  hasFault = (digitalRead(INPUT_FAULT_DRIVER1) == HIGH) && false;
  if (carStates.faultCar1 != hasFault)
  {
    carStates.faultCar1 = hasFault;
    carStates.faultCar1Reported = false;
  }

  hasFault = (digitalRead(INPUT_FAULT_DRIVER2) == HIGH) && false;
  if (carStates.faultCar2 != hasFault)
  {
    carStates.faultCar2 = hasFault;
    carStates.faultCar2Reported = false;
  }
}

void reportDriverFaults()
{
  if (carStates.faultCar1 && !carStates.faultCar1Reported)
  {
    Serial.println("E: Fault on driver 1");
    carStates.faultCar1Reported = true;
  }

  if (carStates.faultCar2 && !carStates.faultCar2Reported)
  {
    Serial.println("E: Fault on driver 2");
    carStates.faultCar2Reported = true;
  }
}

void readAndProcessDriveInstructions()
{
  readAvailableBytes();
  interpretCommand();
}

void readAvailableBytes()
{
  while (Serial.available() > 0)
  {
    if (serialCommand.length() < MAX_COMMAND_LENGTH)
    {
      serialCommand += (char)Serial.read();
    }
    else
    {
      Serial.println("E: Command too long");
      serialCommand = "";
    }
  }
}

void interpretCommand()
{
  if (serialCommand.endsWith(";"))
  {
    if (serialCommand.startsWith(ADDRESS_CAR1))
    {
      long percentage = extractValueAfterAddress(ADDRESS_CAR1);

      carStates.speedCar1 = (255 * percentage) / 100;

      Serial.print("I: Car 1 requested power %: ");
      Serial.println(percentage);
    }
    else if (serialCommand.startsWith(ADDRESS_CAR2))
    {
      long percentage = extractValueAfterAddress(ADDRESS_CAR2);

      carStates.speedCar2 = (255 * percentage) / 100;

      Serial.print("I: Car 2 requested power %: ");
      Serial.println(percentage);
    }
    else
    {
      Serial.println("E: Unknown command");
    }

    serialCommand = "";
  }
}

long extractValueAfterAddress(String address)
{
  String value = serialCommand.substring(address.length(), serialCommand.length() - 1);

  return value.toInt();
}

void calculateResultingSpeeds()
{
  if (carStates.faultCar1)
  {
    carStates.speedCar1 = 0;
  }

  if (carStates.faultCar2)
  {
    carStates.speedCar2 = 0;
  }
}

void pushCarSpeeds()
{
  analogWrite(PWM_PIN_DRIVER1, carStates.speedCar1);
  analogWrite(PWM_PIN_DRIVER2, carStates.speedCar2);
}
