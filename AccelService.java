/* AccelService.java - Service to change frequency of accelerometer events */
/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2009 Eric J. Gaudet

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.		     */
/* ------------------------------------------------------------------------- */

package org.webosinternals.accelservice;

import com.palm.luna.LSException;
import com.palm.luna.service.LunaServiceThread;
import com.palm.luna.service.ServiceMessage;
import com.palm.luna.message.ErrorMessage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import org.json.JSONException;
import org.json.JSONObject;

public class AccelService extends LunaServiceThread {
  private String myVersion = "0.3.1";

  private final String POLL_PATH = 
    "/sys/class/input/input5/poll_interval";


  private boolean setPollInterval(int period) {
    BufferedWriter output;

    // Only going to allow between 1HZ and 1KHZ
    if (period > 1000 || period < 1) {
      return false;
    }

    try { 
      output = new BufferedWriter(new FileWriter(POLL_PATH,false));
      output.write(Integer.toString(period));
      output.newLine();
      output.close();
    } catch (IOException e) {
      return false;
    }

    return true;
  }

  private int getPollInterval() {
    BufferedReader input;
    FileReader fr;
    int value;

    try {
      fr = new FileReader(POLL_PATH);
      input = new BufferedReader(fr);
      value = Integer.valueOf(input.readLine()).intValue();
      input.close();
      fr.close();
    } catch (IOException e) {
      value = 0;
    }

    return value;
  }

  @LunaServiceThread.PublicMethod
    public void version(ServiceMessage message) {
        try {
            StringBuilder sb = new StringBuilder(8192);
            sb.append("{version:");
            sb.append(JSONObject.quote(this.myVersion));
            sb.append("}");
            message.respond(sb.toString());
        } catch (LSException e) {
            this.logger.severe("", e);
        }
    }

  @LunaServiceThread.PublicMethod
    public void getPollFreq(ServiceMessage msg)
    throws JSONException, LSException {
      try {
        StringBuilder sb = new StringBuilder(256);
        int interval = this.getPollInterval();
        sb.append("{freq:");
        if (interval == 0) {
          sb.append("0");
        }
        else {
          sb.append(Integer.toString(1000/this.getPollInterval()));
        }
        sb.append("}");
        msg.respond(sb.toString());
      } catch (LSException e) {
        this.logger.severe("", e);
      }
    }

  @LunaServiceThread.PublicMethod
    public void getPollPeriod(ServiceMessage msg)
    throws JSONException, LSException {
      try {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{period:");
        sb.append(Integer.toString(this.getPollInterval()));
        sb.append("}");
        msg.respond(sb.toString());
      } catch (LSException e) {
        this.logger.severe("", e);
      }
    }

  @LunaServiceThread.PublicMethod
    public void setPollFreq(ServiceMessage msg)
    throws JSONException, LSException {
      if (msg.getJSONPayload().has("freq")) {
        int freq = msg.getJSONPayload().getInt("freq");
        if((freq > 0) && (setPollInterval(1000/freq))) {
          JSONObject reply = new JSONObject();
          reply.put("success", true);
          msg.respond(reply.toString());
        }
      }
      msg.respondError(ErrorMessage.ERROR_CODE_INVALID_PARAMETER, 
          "Need freq in HZ");
    }

  @LunaServiceThread.PublicMethod
    public void setPollPeriod(ServiceMessage msg)
    throws JSONException, LSException {
      if (msg.getJSONPayload().has("period")) {
        int period = msg.getJSONPayload().getInt("period");
        if(setPollInterval(period)) {
          JSONObject reply = new JSONObject();
          reply.put("success", true);
          msg.respond(reply.toString());
        }
      }
      msg.respondError(ErrorMessage.ERROR_CODE_INVALID_PARAMETER, 
          "Need period in ms");
    }

    @LunaServiceThread.PublicMethod
	public void status(ServiceMessage msg)
	throws JSONException, LSException {
	JSONObject reply = new JSONObject();
	reply.put("returnValue",true);
	msg.respond(reply.toString());
    }

}
