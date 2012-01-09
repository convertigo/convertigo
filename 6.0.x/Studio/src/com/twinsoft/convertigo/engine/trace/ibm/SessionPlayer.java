/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.trace.ibm;

import java.io.IOException;
import java.net.Socket;

import com.twinsoft.convertigo.engine.Engine;

public class SessionPlayer extends Thread {
	
    private TraceReader traceReader;
    private TnClientHandler tnClientHandler;
    private Socket mySocket;

    public SessionPlayer(Socket socket, String s)
        throws Exception
    {
        try
        {
            mySocket = socket;
            tnClientHandler = new TnClientHandler(mySocket);
            traceReader = new TraceReader(s);
            return;
        }
        catch(Exception exception)
        {
            throw exception;
        }
    }

    public void close()
    {
        if(mySocket != null)
        {
            try
            {
                mySocket.close();
            }
            catch(IOException _ex) { }
            finally
            {
                mySocket = null;
            }
            return;
        } else
        {
            return;
        }
    }

    public void run()
    {
        try
        {
            doIt();
            return;
        }
        catch(Exception _ex)
        {
            return;
        }
    }

    private void doIt()
    {
label0:
        {
            boolean flag = false;
            do
            {
                String s;
                boolean flag1;
                boolean flag2;
                boolean flag3;
                boolean flag4;
                byte abyte0[];
                int i;
                int j;
                do
                {
                    flag1 = false;
                    flag2 = false;
                    flag3 = false;
                    flag4 = false;
                    i = 0;
                    j = 0;
                    s = traceReader.getNextTelnet();
                    if(s.equals("###EOF###"))
                    {
                        synchronized(this)
                        {
                            try
                            {
                                wait(1000L);
                            }
                            catch(InterruptedException _ex) { }
                        }
                        Engine.logEngine.info("End of trace ...");
                        break label0;
                    }
                    flag2 = traceReader.lookForString(s, "NVT_DATA");
                    flag3 = traceReader.lookForString(s, "END_OF_RECORD");
                    flag4 = flag3 || traceReader.lookForString(s, "RECORD");
                    abyte0 = traceReader.getData();
                    if(abyte0 == null)
                    {
                    	Engine.logEngine.error("Trace error: Telnet header line not followed by data block!");
                        break label0;
                    }
                    i = abyte0.length;
                    for(int k = 0; k < i; k++)
                        if(abyte0[k] == -1 && k + 1 < i && abyte0[k + 1] == -6)
                            flag1 = true;

                    flag4 = flag4 || !flag4 && !flag2 && abyte0[0] != -1;
                    if(flag4)
                    {
                        for(int l = 0; l < i; l++)
                            if(abyte0[l] == -1)
                                j++;

                    }
                    if(!traceReader.lookForString(s, "RECV"))
                        break;
                    if(flag4)
                    {
                        if(j == 0 && !flag3)
                            tnClientHandler.sendTraceRecord(abyte0);
                        else
                        if(j == 0 && flag3)
                        {
                            byte abyte1[] = new byte[i + 2];
                            System.arraycopy(abyte0, 0, abyte1, 0, i);
                            abyte1[i] = -1;
                            abyte1[i + 1] = -17;
                            tnClientHandler.sendTraceRecord(abyte1);
                        } else
                        {
                            byte abyte2[];
                            if(flag3)
                                abyte2 = new byte[i + j + 2];
                            else
                                abyte2 = new byte[i + j];
                            int j1 = 0;
                            for(int l1 = 0; l1 < i; l1++)
                                if(abyte0[l1] != -1)
                                {
                                    abyte2[j1] = abyte0[l1];
                                    j1++;
                                } else
                                {
                                    abyte2[j1] = abyte0[l1];
                                    abyte2[j1 + 1] = abyte0[l1];
                                    j1 += 2;
                                }

                            if(flag3)
                            {
                                abyte2[j1] = -1;
                                abyte2[j1 + 1] = -17;
                            }
                            tnClientHandler.sendTraceRecord(abyte2);
                        }
                    } else
                    {
                        tnClientHandler.sendTraceRecord(abyte0);
                    }
                } while(true);
                if(!traceReader.lookForString(s, "SEND"))
                    break;
                int i1 = 0;
                if(flag)
                {
                    if(!tnClientHandler.postReceiveNVTData())
                        break label0;
                    if(flag4 && !flag3)
                    {
                        for(int k1 = 0; k1 < i; k1++)
                        {
                            if(abyte0[k1] != 0)
                                break;
                            i1++;
                        }

                    }
                }
                byte abyte3[];
                if(flag1)
                {
                    flag = false;
                    abyte3 = tnClientHandler.receiveTNSE();
                } else
                if(flag2)
                {
                    flag = true;
                    abyte3 = tnClientHandler.preReceiveNVTData();
                } else
                {
                    flag = false;
                    if(flag3)
                        abyte3 = tnClientHandler.receiveBinaryDataEOR();
                    else
                        abyte3 = tnClientHandler.receiveTelnetData((i + j) - i1);
                }
                if(abyte3 == null)
                {
                	Engine.logEngine.info("Session aborted ...");
                    break label0;
                }
            } while(true);
            Engine.logEngine.error("Trace error: Telnet header contains neither SEND nor RECV!");
        }
        tnClientHandler.close();
        traceReader.close();
    }

}