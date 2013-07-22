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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.twinsoft.convertigo.engine.Engine;

public class TnClientHandler {

    private Socket mySocket;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
	
    public TnClientHandler(Socket socket)
        throws IOException
    {
        mySocket = socket;
        try
        {
            inputStream = new BufferedInputStream(mySocket.getInputStream());
            outputStream = new BufferedOutputStream(mySocket.getOutputStream());
        }
        catch(IOException ioexception)
        {
        	Engine.logEngine.error("Exception while creating Telnet client handler", ioexception);
            throw ioexception;
        }
        Engine.logEngine.info("Starting session with " + mySocket.getInetAddress().getHostAddress());
    }

    public void close()
    {
        try
        {
        	Engine.logEngine.info("Closing session with " + mySocket.getInetAddress().getHostAddress());
            inputStream.close();
            outputStream.close();
            mySocket.close();
            return;
        }
        catch(IOException _ex)
        {
            return;
        }
    }

    public byte[] receiveTelnetData(int i)
    {
        byte abyte0[] = new byte[i];
        try
        {
            for(int j = 0; j < i; j++)
            {
                int k = inputStream.read();
                if(k == -1)
                    return null;
                abyte0[j] = (byte)k;
            }

            return abyte0;
        }
        catch(IOException ioexception)
        {
        	Engine.logEngine.error("Exception while receiving telnet data", ioexception);
        }
        return null;
    }

    public byte[] receiveTNSE()
    {
        int i = 0;
        byte abyte0[] = new byte[1024];
        try
        {
            do
            {
                int j = inputStream.read();
                if(j == -1)
                    return null;
                abyte0[i] = (byte)j;
                if(abyte0[i] == -1)
                {
                    int k = inputStream.read();
                    if(k == -1)
                        return null;
                    abyte0[i + 1] = (byte)k;
                    if(abyte0[i + 1] == -16)
                    {
                        i += 2;
                        break;
                    }
                    i += 2;
                } else
                {
                    i++;
                }
            } while(true);
        }
        catch(IOException ioexception)
        {
        	Engine.logEngine.error("Exception while receiving TNSE", ioexception);
            return null;
        }
        byte abyte1[] = new byte[i];
        System.arraycopy(abyte0, 0, abyte1, 0, i);
        return abyte1;
    }

    public byte[] receiveBinaryDataEOR()
    {
        int i = 0;
        byte abyte0[] = new byte[8048];
        try
        {
            do
            {
                int j = inputStream.read();
                if(j == -1)
                    return null;
                abyte0[i] = (byte)j;
                if(abyte0[i] == -1)
                {
                    int k = inputStream.read();
                    if(k == -1)
                        return null;
                    abyte0[i + 1] = (byte)k;
                    if(abyte0[i + 1] == -17)
                    {
                        i += 2;
                        break;
                    }
                    i += 2;
                } else
                {
                    i++;
                }
            } while(true);
        }
        catch(IOException ioexception)
        {
        	Engine.logEngine.error("Exception while receiving binary data EOR", ioexception);
            return null;
        }
        byte abyte1[] = new byte[i];
        System.arraycopy(abyte0, 0, abyte1, 0, i);
        return abyte1;
    }

    public byte[] preReceiveNVTData()
    {
        try
        {
            inputStream.mark(1);
            int i = inputStream.read();
            if(i == -1)
                return null;
            try
            {
                inputStream.reset();
            }
            catch(IOException _ex) { }
            return (new byte[] {
                (byte)i
            });
        }
        catch(IOException ioexception)
        {
        	Engine.logEngine.error("Exception while pre-receiving NVT data", ioexception);
        }
        return null;
    }

    public boolean postReceiveNVTData()
    {
        try
        {
            while(true) 
            {
                inputStream.mark(1);
                int i = inputStream.read();
                if(i == -1)
                    return false;
                byte byte0 = (byte)i;
                if(byte0 == 13)
                {
                    inputStream.mark(1);
                    int k = inputStream.read();
                    if(k == -1)
                        return false;
                    byte byte1 = (byte)k;
                    if(byte1 != 10 && byte1 != 0)
                    {
                        try
                        {
                            inputStream.reset();
                        }
                        catch(IOException _ex) { }
                        return true;
                    }
                    break;
                }
                if(!isASCII(i))
                {
                    try
                    {
                        inputStream.reset();
                    }
                    catch(IOException _ex) { }
                    return true;
                }
            }
            int j;
            do
            {
                inputStream.mark(1);
                j = inputStream.read();
                if(j == -1)
                    return false;
            } while(j == 0);
            try
            {
                inputStream.reset();
            }
            catch(IOException _ex) { }
            return true;
        }
        catch(IOException ioexception)
        {
        	Engine.logEngine.error("Exception while post-receiving NVT data", ioexception);
            return false;
        }
    }

    private boolean isASCII(int i)
    {
        return i > 31 && i < 128;
    }

    public void sendTraceRecord(byte abyte0[])
    {
        try
        {
            outputStream.write(abyte0, 0, abyte0.length);
            outputStream.flush();
            return;
        }
        catch(IOException ioexception)
        {
        	Engine.logEngine.error("Exception while sending trace record", ioexception);
        }
    }

}