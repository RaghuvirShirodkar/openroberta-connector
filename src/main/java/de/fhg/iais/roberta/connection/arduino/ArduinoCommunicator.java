package de.fhg.iais.roberta.connection.arduino;

import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import de.fhg.iais.roberta.util.PropertyHelper;

class ArduinoCommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(ArduinoCommunicator.class);

    private String avrPath = ""; //path for avrdude bin
    private String avrConfPath = ""; //path for the .conf file
    private final Arduino arduino;

    ArduinoCommunicator(Arduino arduino) {
        this.arduino = arduino;
    }

    private void setParameters() {
        if ( SystemUtils.IS_OS_WINDOWS ) {
            this.avrPath = PropertyHelper.getInstance().getProperty("WinPath");
            this.avrConfPath = PropertyHelper.getInstance().getProperty("WinConfPath");
        } else if ( SystemUtils.IS_OS_LINUX ) {
            if ( SystemUtils.OS_ARCH.equals("i386") ) {
                this.avrPath = PropertyHelper.getInstance().getProperty("LinPath32");
            } else if ( SystemUtils.OS_ARCH.equals("arm") ) {
                this.avrPath = PropertyHelper.getInstance().getProperty("LinPathArm32");
            } else {
                this.avrPath = PropertyHelper.getInstance().getProperty("LinPath64");
            }
            this.avrConfPath = PropertyHelper.getInstance().getProperty("LinConfPath");
        } else {
            this.avrPath = PropertyHelper.getInstance().getProperty("OsXPath");
            this.avrConfPath = PropertyHelper.getInstance().getProperty("MacConfPath");
        }
    }

    JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();

        deviceInfo.put("firmwarename", this.arduino.getType().toString());
        deviceInfo.put("robot", this.arduino.getType().toString());
        deviceInfo.put("brickname", this.arduino.getName());

        return deviceInfo;
    }

    void uploadFile(String portName, String filePath) {
        this.setParameters();
        String portPath = "/dev/";
        if ( SystemUtils.IS_OS_WINDOWS ) {
            portPath = "";
        }
        try {
            String pArg;
            String cArg;
            String eArg = "";
            switch ( this.arduino.getType() ) {
                // specify if different
                case MEGA:
                    pArg = "-patmega2560";
                    cArg = "-cwiring";
                    break;
                case BOB3:
                    pArg = "-patmega88";
                    cArg = "-cavrisp2";
                    eArg = "-e";
                    break;
                default: // take uno config as default, this is used by Uno, Nano, Bot'n Roll and Mbot
                    pArg = "-patmega328p";
                    cArg = "-carduino";
                    break;
            }

            LOG.info("Starting to upload program {} to {}{}", filePath, portPath, portName);
            ProcessBuilder
                processBuilder =
                new ProcessBuilder(this.avrPath,
                                   "-v",
                                   "-D",
                                   pArg,
                                   cArg,
                                   "-Uflash:w:" + filePath + ":i",
                                   "-C" + this.avrConfPath,
                                   "-P" + portPath + portName,
                                   eArg);

            //            processBuilder.redirectInput(Redirect.INHERIT);
            //            processBuilder.redirectOutput(Redirect.INHERIT);
            processBuilder.redirectError(Redirect.INHERIT);

            Process p = processBuilder.start();
            int eCode = p.waitFor();
            if ( eCode == 0 ) {
                LOG.info("Program uploaded successfully");
            } else {
                LOG.info("Program was unable to be uploaded: {}", eCode);
            }
            LOG.debug("Exit code {}", eCode);
        } catch ( IOException | InterruptedException e ) {
            LOG.error("Error while uploading to arduino: {}", e.getMessage());
        }
    }
}
