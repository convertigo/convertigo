/**
 * Object Javelin
 * 
 * @super Global
 * @type constructor
 * @memberOf Javelin
 */
Javelin.prototype = new Global();
function Javelin(){};

/**
 * The implicit Javelin object.
 * 
 * @type Javelin
 */
var javelin = new Javelin();

/**
 * Defines a videotex session.
 * 
 * @type String
 * @memberOf Javelin
 */
Javelin.prototype.VDX = "com.twinsoft.vdx.GenTerm";

/**
 * Defines a Bull DKU session.
 * 
 * @type String
 * @memberOf Javelin
 */
Javelin.prototype.DKU = "com.twinsoft.dku.TerminalDKU";

/**
 * Defines a VT220 session.
 * 
 * @type String
 * @memberOf Javelin
 */
Javelin.prototype.VT  = "com.twinsoft.vt220.TerminalVT220";

/**
 * Defines a IBM 3270 (SNA) session.
 * 
 * @type String
 * @memberOf Javelin
 */
 */
Javelin.prototype.SNA = "com.twinsoft.ibm.TerminalSNA";

/**
 * Defines a IBM 5250 (AS/400) session.
 * 
 * @type String
 * @memberOf Javelin
 */
 */
Javelin.prototype.AS400 = "com.twinsoft.ibm.Terminal400";

/**
 * Defines a Twinsoft 5250 (AS/400) session. added by jmc for test purposes
 * 
 * @type String
 * @memberOf Javelin
 */
Javelin.prototype.TN5250 = "com.twinsoft.tn5250.Terminal5250";

/**
 * Black color
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_COLOR_BLACK = 0;

/**
 * Red color
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_COLOR_RED = 1;

/**
 * Green color
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_COLOR_GREEN = 2;

/**
 * Yellowcolor
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_COLOR_YELLOW = 3;

/**
 * Bluecolor
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_COLOR_BLUE = 4;

/**
 * Magenta color
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_COLOR_MAGENTA = 5;

/**
 * Cyan color
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_COLOR_CYAN = 6;

/**
 * White color
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_COLOR_WHITE = 7;

/**
 * Mask for INK attribute: 0x0007 (0000 0000 0000 0111).
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_INK = 0x0007; // 0000 0000 0000 0111

/**
 * Mask for PAPER attribute: 0x0038 (0000 0000 0011 1000).
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_PAPER = 0x0038; // 0000 0000 0011 1000

/**
 * Mask for double height: 0x0040 (0000 0000 0100 0000). For 3270/5250 also
 * indicate field is AUTO_ENTER
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_DOUBLEHEIGHT = 0x0040; // 0000 0000 0100 0000

/**
 * Mask for auto tabulation: 0x0040 (0000 0000 0100 0000).
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_AUTO_TAB	 = 0x0040; // 0000 0000 0100 0000

/**
 * Mask for double width attribute bit: 0x0080 (0000 0000 1000 0000).
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_DOUBLEWIDTH = 0x0080; // 0000 0000 1000 0000

/**
 * Mask for underline attribute bit: 0x0100 (0000 0001 0000 0000).
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_UNDERLINE = 0x0100; // 0000 0001 0000 0000

/**
 * Mask for inverse attribute bit: 0x0200 (0000 0001 0000 0000).
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_INVERT = 0x0200; // 0000 0010 0000 0000

/**
 * Mask for double height upper row attribute bit: 0x0400 (0000 0100 0000 0000).<br>
 * Note: relevant for VT220 only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_UPHALF = 0x0400; // 0000 0100 0000 0000

/**
 * Mask for bold attribute bit: 0x0800 (0000 0100 0000 0000).
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_BOLD = 0x0800; // 0000 1000 0000 0000

/**
 * Mask for blink attribute bit: 0x1000 (0001 0000 0000 0000).
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_BLINK = 0x1000; // 0001 0000 0000 0000

/**
 * Mask for masked attribute bit: 0x2000 (0010 0000 0000 0000).<br>
 * Note: relevant for VT220 only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_MASKED	  = 0x2000; // 0010 0000 0000 0000
/**
 * NPTUI: 0x2000 (0010 0000 0000 0000).<br>
 * Note: relevant for 5250 only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_NPTUI 	  = 0x2000; // 0100 0000 0000 0000

/**
 * Mask for hidden attribute bit: 0x8000 (1000 0000 0000 0000).
 */
Javelin.prototype.AT_HIDDEN = 0x8000; // 1000 0000 0000 0000

/**
 * Mask for DRCS (downloadable font) attribute bit: 0x8000 (0100 0000 0000
 * 0000). Note: relevant for Videotex only. For 3270 and 5250 this bit indicates
 * an AUTO_TAB flag for the field
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_DRCS		= 0x4000; // 0100 0000 0000 0000
Javelin.prototype.AT_AUTO_ENTER	= 0x4000; // 0100 0000 0000 0000

/**
 * Mask for protected attribute bit: 0x4000 (0100 0000 0000 0000). For 5250
 * reprsent also the NPTUI attribute
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_PROTECTED = 0x4000; // 0100 0000 0000 0000

/**
 * Mask for field attribute
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_FIELD_ATTRIBUTE = 0xC00000;

/**
 * Mask for protected Field attribute bit: 0x200000.<br>
 * Note: relevant for IBM 3270 / 5250 and BULL DKU7xxx only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_FIELD_PROTECTED = 0x200000;

/**
 * Mask for numeric field attribute: 0x100000.<br>
 * Note: relevant for IBM 3270 / 5250 and BULL DKU7xxx only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_FIELD_NUMERIC = 0x100000;

/**
 * Mask for pen selectable field attribute: 0x080000.<br>
 * Note: relevant for IBM 3270 / 5250 and BULL DKU7xxx only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_FIELD_PEN_SELECTABLE  = 0x080000;

/**
 * Mask for high intensity field attribute: 0x040000.<br>
 * Note: relevant for IBM 3270 / 5250 and BULL DKU7xxx only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_FIELD_HIGH_INTENSITY  = 0x040000;

/**
 * Mask for hidden field attribute: 0x0C0000.<br>
 * Note: relevant for IBM 3270 / 5250 and BULL DKU7xxx only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_FIELD_HIDDEN = 0x0C0000;

/**
 * Mask for reserved field attribute: 0x020000.<br>
 * Note: relevant for IBM 3270 / 5250 and BULL DKU7xxx only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_FIELD_RESERVED = 0x020000;

/**
 * Mask for modified field attribute: 0x010000.<br>
 * Note: relevant for IBM 3270 / 5250 and BULL DKU7xxx only.
 * 
 * @type Number
 * @memberOf Javelin
 */
Javelin.prototype.AT_FIELD_MODIFIED = 0x010000;

/**
 * Sets a new configuration for <em>Javelin</em>.
 * 
 * @param {String}
 *            configuration the new configuration; this is a string of the form
 *            <code>param<sub>1</sub>=value<sub>1</sub>&amp;param<sub>2</sub>=value<sub>2</sub>&amp; ... &amp;param<sub>n</sub>=value<sub>n</sub></code>.<br>
 *            NB: each <code>value<sub>i</sub></code> should be in UTF8
 *            format.
 * @memberOf Javelin
 */
Javelin.prototype.setClientConfig = function(configuration){};

/**
 * Defines the output stream for traces.
 * 
 * @param {Object}
 *            outputstream the output stream for traces.
 * @memberOf Javelin
 */
Javelin.prototype.setLogOutputStream = function(outputStream){};

/**
 * Writes a message in the emulator log output stream.
 * 
 * @param {String}
 *            message the message to write.
 * @memberOf Javelin
 */
Javelin.prototype.Trace = function(message){};

/**
 * Writes a message in the emulator log output stream with a given log level.
 * 
 * @param {Number}
 *            level the log level to use.
 * @param {String}
 *            message the message to write.
 * @memberOf Javelin
 */
Javelin.prototype.Trace = function(level, message){};

/**
 * Writes an exception in the emulator log output stream.
 * 
 * @param {Object}
 *            e the exception to log.
 * @memberOf Javelin
 */
Javelin.prototype.Trace = function(e){};

/**
 * Retrieves the Javelin connection state.
 * 
 * @return <code> true</code> if connected, <code>false</code> otherwise.
 * 
 * @see #connect
 * @see #connect
 * @see #disconnect
 * @memberOf Javelin
 */
Javelin.prototype.isConnected = function(){};

/**
 * Connects <em>Javelin</em> to a server. The connection is asynchronous. When
 * the connection is made, the <em>Connected</em> event is thrown by
 * <em>Javelin</em>.
 * 
 * <p>
 * The connection is made with the parameters provided by helper functions such
 * as <code>setDteAddress()</code>, <code>setCommDevice()</code>,
 * <code>setServiceCode()</code>...
 * </p>
 * 
 * @see #connect
 * @see #disconnect
 * @memberOf Javelin
 */
Javelin.prototype.connect = function(){};

/**
 * Connects synchronously <em>Javelin</em> to a server.
 * 
 * <p>
 * The connection is made with the parameters provided by helper functions such
 * as <code>setDteAddress()</code>, <code>setCommDevice()</code>,
 * <code>setServiceCode()</code>...
 * </p>
 * 
 * @param {Number} timeout
 *            the maximum amount of milliseconds during which the connection is
 *            expected.
 * 
 * @return <code>true</code> if connected, <code>false</code> if the timeout
 *         expired.
 * 
 * @see #connect
 * @see #disconnect
 * @memberOf Javelin
 */
Javelin.prototype.connect = function(timeout){};

/**
 * Disconnects <em>Javelin</em> from the current connected server. The
 * disconnection is synchronous, i.e. <em>Javelin</em> is disconnected when
 * the method returns.
 * 
 * @see #connect
 * @see #connect
 * @memberOf Javelin
 */
Javelin.prototype.disconnect = function(){};

/**
 * Sets the connection string.
 * 
 * @param {String} connectionString
 *            the connection string using the following format:
 * 
 * <p align="center">
 * <code>&lt;path&gt;#&lt;destination&gt;-&lt;called sub address&gt;.&lt;calling sub address&gt;/&lt;max level&gt;</code>
 * </p>
 * 
 * <table border="1" cellpadding="6" width="100%">
 * <tr>
 * <td width="16%" align="center" nowrap><b>Description</b></td>
 * <td width="8%" align="center" nowrap><b>path</b></td>
 * <td width="24%" align="center" nowrap><b>destination</b></td>
 * <td width="17%" align="center" nowrap><b>sourceSA</b></td>
 * <td width="17%" align="center" nowrap><b>destinationSA</b></td>
 * <td width="17%" align="center" nowrap><b>max level</b></td>
 * <td width="17%" align="center" nowrap><b>Notes</b></td>
 * </tr>
 * <tr>
 * <td width="16%" valign="top" nowrap>Direct telnet connection</td>
 * <td width="8%" valign="top" nowrap>DIR</td>
 * <td width="24%" valign="top" nowrap>&lt;IP address&gt;:port</td>
 * <td width="17%" valign="top" nowrap>-</td>
 * <td width="17%" valign="top" nowrap>-</td>
 * <td width="17%" valign="top" nowrap>-</td>
 * <td width="17%" valign="top" nowrap>mandatory for IBM and Bull emulators</td>
 * </tr>
 * <tr>
 * <td width="16%" valign="top" nowrap>PAVI + Eicon X25 board</td>
 * <td width="8%" valign="top" nowrap>EIC</td>
 * <td width="24%" valign="top" nowrap>&lt;remote X25 address&gt;</td>
 * <td width="17%" valign="top" nowrap>called sub-address<br>
 * (for Intelmatique billings)</td>
 * <td width="17%" valign="top" nowrap>calling sub-address<br>
 * (for services filter selection)</td>
 * <td width="17%" valign="top" nowrap>0..255</td>
 * <td width="17%" valign="top" nowrap>There is no &quot;-&quot; character
 * between destination<br>
 * and sourceSA in this case only.</td>
 * </tr>
 * <tr>
 * <td width="16%" valign="top" nowrap>PAVI + SAP/IP</td>
 * <td width="8%" valign="top" nowrap>TCP</td>
 * <td width="24%" valign="top" nowrap>&lt;Intelmatique address&gt;@port</td>
 * <td width="17%" valign="top" nowrap>called sub-address<br>
 * (for Intelmatique billings)</td>
 * <td width="17%" valign="top" nowrap>calling sub-address<br>
 * (for services filter selection)</td>
 * <td width="17%" valign="top" nowrap>0..255</td>
 * <td width="17%" valign="top" nowrap>&nbsp;</td>
 * </tr>
 * <tr>
 * <td width="16%" valign="top" nowrap>PAVI + iMinitel</td>
 * <td width="8%" valign="top" nowrap>MIP</td>
 * <td width="24%" valign="top" nowrap>&lt;iMinitel address&gt;@port</td>
 * <td width="17%" valign="top" nowrap>-</td>
 * <td width="17%" valign="top" nowrap>-</td>
 * <td width="17%" valign="top" nowrap>0..255</td>
 * <td width="17%" valign="top" nowrap>Same as DIR but using the PAVI as a
 * gateway.<br>
 * (iMinitel address: 172.31.0.20@7516)</td>
 * </tr>
 * <tr>
 * <td width="16%" valign="top" nowrap>PAVI + iMinitel + RAS</td>
 * <td width="8%" valign="top" nowrap>RAS</td>
 * <td width="24%" valign="top" nowrap>&lt;iMinitel address&gt;@port</td>
 * <td width="17%" valign="top" nowrap>-</td>
 * <td width="17%" valign="top" nowrap>-</td>
 * <td width="17%" valign="top" nowrap>0..255</td>
 * <td width="17%" valign="top" nowrap>Using a modem or ISDN service.<br>
 * (iMinitel address: 172.31.0.20@7516)</td>
 * </tr>
 * </table>
 * @memberOf Javelin
 */
Javelin.prototype.setDteAddress = function(connectionString){};

/**
 * Sets the service code (depending of the terminal class).
 * 
 * @param {String} serviceCode
 *            the service code using the following format: <table border="1"
 *            cellpadding="6">
 *            <tr>
 *            <td>VT</td>
 *            <td>n/a</td>
 *            </tr>
 *            <tr>
 *            <td>Bull</td>
 *            <td>mailbox</td>
 *            </tr>
 *            <tr>
 *            <td>Videotex</td>
 *            <td>service code</td>
 *            </tr>
 *            <tr>
 *            <td>IBM</td>
 *            <td>device name</td>
 *            </tr>
 *            </table>
 * @memberOf Javelin
 */
Javelin.prototype.setServiceCode = function(serviceCode){};

/**
 * Sets the communication device, i.e. the address (IP or DNS) of the PAVI
 * gateway, eventually followed by <code>:port</code>.
 * 
 * @param {String} commDevice
 *            the communication device.
 * @memberOf Javelin
 */
Javelin.prototype.setCommDevice = function(commDevice){};

/**
 * Sets the name of the user.
 * 
 * @param {String} vicUser
 *            the user name.
 * @memberOf Javelin
 */
Javelin.prototype.setVicUser(vicUser){};

/**
 * Sets the group of the user.
 * 
 * @param {String} group
 *            the user group.
 * @memberOf Javelin
 */
Javelin.prototype.setGroup = function(group){};

/**
 * Sets the capture flag.
 * 
 * @param {Boolean} bDoCapture
 *            indicates if capture should be written or not.
 * @memberOf Javelin
 */
Javelin.prototype.setDoCapture(bDoCapture){};

/**
 * Sets the auto connect flag.
 * 
 * @param {Boolean} bDoCapture
 *            indicates if automatic connection should be performed without
 *            having to call one the <code>connect()</code> methods.
 * @memberOf Javelin
 */
Javelin.prototype.setAutoConnect = function(bAutoConnect){};

/**
 * Sends a string as if the user has stroken it on keyboard. All characters
 * between 0x00 and 0xFF can be sent by this way.
 * 
 * @param {String} keystrokes
 *            the string to send.
 * @memberOf Javelin
 */
Javelin.prototype.send = function(keystrokes){};

/**
 * Returns the current line of the caret. Lines are designed from left to right
 * and beginning by 0.
 * 
 * @return the current line.
 * 
 * @see #getCurrentColumn
 * @memberOf Javelin
 */
Javelin.prototype.getCurrentLine = function(){};

/**
 * Returns the current column of the caret. Columns are designed from top to
 * bottom and beginning by 0.
 * 
 * @return the current column.
 * 
 * @see #getCurrentLine
 * @memberOf Javelin
 */
Javelin.prototype.getCurrentColumn = function(){};

/**
 * Returns the character attribute at coordinates (<code>column</code>,
 * <code>line</code>). You can access specific attribute bit with
 * <code>AT_xxx</code> masks.
 * 
 * @param {Number} column
 *            the horizontal coordinate from left to right beginning by 0.
 * @param {Number} line
 *            the vertical coordinate from top to bottom beginning by 0.
 * 
 * @return the character attribute at (<code>column</code>,
 *         <code>line</code>) coordinates.
 * 
 * @see #getChar
 * @see #getString
 * @see #setChar
 * @memberOf Javelin
 */
Javelin.prototype.getCharAttribute = function(column, line){};

/**
 * Returns the ASCII code of the character at coordinates (<code>column</code>,
 * <code>line</code>).
 * 
 * @param {Number} column
 *            the horizontal coordinate from left to right beginning by 0.
 * @param {Number} line
 *            the vertical coordinate from top to bottom beginning by 0.
 * 
 * @return the read character at (<code>column</code>, <code>line</code>)
 *         coordinates.
 * 
 * @see #getCharAttribute
 * @see #getString
 * @see #setChar
 * @memberOf Javelin
 */
Javelin.prototype.getChar = function(column, line){};

/**
 * Returns the string at coordinates (<code>column</code>, <code>line</code>).
 * 
 * @param {Number} column
 *            the horizontal coordinate from left to right beginning by 0.
 * @param {Number} line
 *            the vertical coordinate from top to bottom beginning by 0.
 * @param {Number} lenght
 *            the string length to return.
 * 
 * @return the read string at (<code>column</code>, <code>line</code>)
 *         coordinates.
 * 
 * @see #getChar
 * @see #getCharAttribute
 * @see #setChar
 * @memberOf Javelin
 */
Javelin.prototype.getString = function(column, line, length){};

/**
 * Sets the ASCII code of the character at coordinates (<code>column</code>,
 * <code>line</code>).
 * 
 * @param {Number} column
 *            the horizontal coordinate from left to right beginning by 0.
 * @param {Number} line
 *            the vertical coordinate from top to bottom beginning by 0.
 * @param {String} c
 *            the ASCII character code to set.
 * 
 * @see #getChar
 * @see #getCharAttribute
 * @see #getString
 * @memberOf Javelin
 */
Javelin.prototype.setChar = function(column, line, c){};

/**
 * Waits for a trigger that has been set by one the <code>waitAt()</code> or
 * <code>waitFor()</code> functions.
 * 
 * @param {Number} timeout
 *            the maximum amount of milliseconds during which the event is
 *            expected.
 * 
 * @return -1 if the timeout has expired, otherwise the trigger ID.
 * 
 * @see #waitAt(String, int, int, long)
 * @see #waitAtId(int, String, int, int)
 * @see #waitForId(int, String)
 * @see #waitFor(String, long)
 * @see #deleteWaitAts
 * @see #deleteWaitFors
 * @memberOf Javelin
 */
Javelin.prototype.waitTrigger(timeout){};

/**
 * Sets up a synchronous screen trigger. This method returns only when the
 * string <code>searchedString</code> is recognized on the screen at
 * coordinates (<code>column</code>, <code>line</code>).
 * 
 * @param {String} searchedString
 *            the string to be searched.
 * @param {Number} x
 *            the horizontal coordinate of the trigger.
 * @param {Number} y
 *            the vertical coordinate of the trigger.
 * @param {Number} timeout
 *            the maximum amount of milliseconds during which the wait is made.
 * 
 * @return <code>true</code> if the event is fired, <code>false</code>
 *         otherwise.
 * 
 * @see #waitAtId(int, String, int, int)
 * @see #waitForId(int, String)
 * @see #waitFor(String, long)
 * @see #deleteWaitAts()
 * @see #deleteWaitFors()
 * @memberOf Javelin
 */
Javelin.prototype.waitAt = function(searchedString, column, line, timeOut){};

/**
 * Sets up an asynchronous screen trigger. When the string
 * <code>searchedString</code> is recognized on the screen at (<code>column</code>,
 * <code>line</code>) coordinates, the <code>WaitAtDone</code> event is
 * generated by the applet with the identificator <code>id</code>. You can
 * set many observations at the same time. Once the event is generated, the
 * observation is freed.
 * 
 * @param {Number} id
 *            the trigger id. This ID is transmitted by the
 *            <code>WaitAtDone</code> event.
 * @param {String} searchString
 *            the string to be searched.
 * @param {Number} column
 *            the horizontal coordinate of the trigger.
 * @param {Number} line
 *            the vertical coordinate of the trigger.
 * 
 * @see #waitAt(String, int, int, long)
 * @see #waitForId(int, String)
 * @see #waitFor(String, long)
 * @see #deleteWaitAts()
 * @see #deleteWaitFors()
 * @memberOf Javelin
 */
Javelin.prototype.waitAtId = function(id, searchedString, column, line){};

/**
 * Set up a screen trigger. When the string <em>Str</em> is recognized in the
 * data stream the event <em>id</em> will be generated. You can set many
 * Triggers at the same time. Once the event is generated, the Trigger is freed.
 * You can wait for these triggers with the WaitTrigger() function.
 * 
 * @param {Number} id
 *            the Trigger id. This id is received in the <em>WaitAtDone</em>
 *            event or in the WaitTrigger() function
 * @param {String} Str
 *            the string to be observed.
 * 
 * @see #waitAt(String, int, int, long)
 * @see #waitAtId(int, String, int, int)
 * @see #waitFor(String, long)
 * @see #deleteWaitAts
 * @see #deleteWaitFors
 * @memberOf Javelin
 */
Javelin.prototype.waitForId = function(id, Str){};

/**
 * Set up a synchrounous line trigger.
 * 
 * @param {String} searchedString
 *            the string to be searched.
 * @param {Number} timeout
 *            the maximum amount of milliseconds during which the search is
 *            performed.
 * 
 * @see #waitAt(String, int, int, long)
 * @see #waitAtId(int, String, int, int)
 * @see #waitForId(int, String)
 * @see #deleteWaitAts
 * @see #deleteWaitFors
 * 
 * @return <code>true</code> if the string is found before timeout,
 *         <code>false</code> otherwise.
 * @memberOf Javelin
 */
Javelin.prototype.waitFor = function(searchedString, timeout){};

/**
 * Waits synchronoulsy for the cursor to be at a specified position. This
 * function is usefull to detect a new page coming in Vidéotex and VTxxx
 * emulators.
 * 
 * @param {Number} column
 *            the horizontal coordinate of the cursor
 * @param {Number} line
 *            the vertical coordinate of the cursor
 * @param {Boolean} here
 *            if true, the function waits for the cursor to be at the specified
 *            position. if false, the functions waits for the cursor to be at
 *            any position different from the one specified.
 * @param {Number} timeout
 *            the maximum amount of milisecs to wait.
 * 
 * @return <code>true</code> if the cursor is found at this position before
 *         timeout, <code>false</code> otherwise.
 * 
 * @see #waitCursorAtId(int, int, int, boolean)
 * @memberOf Javelin
 */
Javelin.prototype.waitCursorAt = function(column, line, here, timeout){};


/**
 * Waits Asynchronoulsy for the cursor to be at a specified position. This
 * function is usefull to detect a new page coming in Vidéotex and VTxxx
 * emulators.
 * 
 * @param {Number} id
 *            the Trigger id. This id is received in the <em>WaitAtDone</em>
 *            event or in the WaitTrigger() function
 * @param {Number} column
 *            the horizontal coordinate of the cursor
 * @param {Number} line
 *            the vertical coordinate of the cursor
 * @param {Boolean} here
 *            if true, the function waits for the cursor to be at the specified
 *            position. if false, the functions waits for the cursor to be at
 *            any position different from the one specified.
 * @memberOf Javelin
 */
Javelin.prototype.waitCursorAtId = function(id, column, line, here){};

/**
 * Deletes all triggers set by the <code>waitAt()</code> method.
 * 
 * @see #waitAt(String, int, int, long)
 * @see #waitAtId(int, String, int, int)
 * @see #waitForId(int, String)
 * @see #waitFor(String, long)
 * @see #deleteWaitFors()
 * @memberOf Javelin
 */

Javelin.prototype.deleteWaitAts = function(){};

/**
 * Deletes all triggers set by the <code>waitFor()</code> method.
 * 
 * @see #waitAt(String, int, int, long)
 * @see #waitAtId(int, String, int, int)
 * @see #waitForId(int, String)
 * @see #waitFor(String, long)
 * @see #deleteWaitAts()
 * @memberOf Javelin
 */
Javelin.prototype.deleteWaitFors = function(){};

/**
 * Calls the <em>Javelin</em> <code>wait()</code> method.
 * 
 * @param {Number} timeout
 *            the maximum amount of milliseconds during which the wait will
 *            occur.
 * @memberOf Javelin
 */
Javelin.prototype.waitSync = function(timeOut){};

/**
 * Waits for the screen to be stable. This routine will return after timeout,
 * even if the screen isn't still stable.
 * 
 * @param {Number} timeout
 *            the maximum amount of milliseconds during which the event is
 *            expected.
 * @param {Number} dataStableThreshold
 *            this value is the maximum time allowed with no data received from
 *            the host to consider a dataStable condition.
 * 
 * @return <code>true</code> if the screen is stable, <code>false</code>
 *         otherwise (i.e. the timeout has expired).
 * @memberOf Javelin
 */
Javelin.prototype.waitForDataStable = function(timeout, dataStableThreshold){};

/**
 * Moves the cursor to coordinates (<code>column</code>, <code>line</code>).
 * 
 * @param {Number} column
 *            the horizontal coordinate from left to right beginning by 0.
 * @param {Number} line
 *            the vertical coordinate from top to bottom beginning by 0.
 * @memberOf Javelin
 */
Javelin.prototype.moveCursor = function(column, line){};

/**
 * Gets the number of columns of the current emulator screen
 * 
 * @return the number of columns.
 * 
 * @see #getScreenHeight
 * @memberOf Javelin
 */
Javelin.prototype.getScreenWidth(){};

/**
 * Gets the number of lines of the current emulator screen
 * 
 * @return the number of lines.
 * 
 * @see #getScreenWidth
 * @memberOf Javelin
 */
Javelin.prototype.getScreenHeight = function(){};

/**
 * Gets the number of fields on the screen. If the number of fields is 0, then
 * the screen is called "unformatted". This is always the case for VT and VDX
 * emulators but can be the case for IBM in the case of a unformatted screen.
 * 
 * @return the number of fields.
 * @memberOf Javelin
 */
Javelin.prototype.getNumberOfFields(){};

/**
 * Gets the text of a given field.
 * 
 * @param {Number} fieldIndex
 *            the index of the field from 0 to <em>n</em>.
 * 
 * @return the text of the field.
 * @memberOf Javelin
 */
Javelin.prototype.getFieldText = function(fieldIndex){};

/**
 * Gets the attribute of a given field.
 * 
 * @param {Number} fieldIndex
 *            the index of the field from 0 to <em>n</em>.
 * 
 * @return the attribute of the field.
 * @memberOf Javelin
 */
Javelin.prototype.getFieldAttribute = function(fieldIndex){};

/**
 * Gets the line of a given field.
 * 
 * @param {Number} fieldIndex
 *            the index of the field from 0 to <em>n</em>.
 * 
 * @return the line of the field.
 * @memberOf Javelin
 */
Javelin.prototype.getFieldLine = function(fieldIndex){};

/**
 * Gets the column of a given field.
 * 
 * @param {Number} fieldIndex
 *            the index of the field from 0 to <em>n</em>.
 * 
 * @return the column of the field.
 * @memberOf Javelin
 */
Javelin.prototype.getFieldColumn = function(fieldIndex){};


/**
 * Gets a Specific Field next continuous field line (5250 only)
 * 
 * @param {Number} index
 * @memberOf Javelin
 */
Javelin.prototype.getNextContinuousFieldLine = function(index){};

/**
 * Gets a Specific Field next continuous Column position (5250 only)
 * 
 * @param {Number} index
 * @memberOf Javelin
 */
Javelin.prototype.getNextContinuousColumn = function(index){};

/**
 * Gets a Specific Field Previous continuous field line (5250 only)
 * 
 * @param {Number} index
 * @memberOf Javelin
 */
Javelin.prototype.getPreviousContinuousFieldLine = function(index){};

/**
 * Gets a Specific Field Previous continuous Column position (5250 only)
 * 
 * @param {Number} index
 * @memberOf Javelin
 */
Javelin.prototype.getPreviousContinuousColumn = function(index){};

/**
 * Gets the lenght of a given field. If the lenght is greater than the width of
 * the screen, it means that the field spans on several lines.
 * 
 * @param {Number} fieldIndex
 *            the index of the field from 0 to <em>n</em>.
 * 
 * @return the attribute of the field.
 * @memberOf Javelin
 */
Javelin.prototype.getFieldLength = function(fieldIndex){};

/**
 * Executes an action on the emulator.
 * 
 * @param {String} action
 *            the action to execute.
 * 
 * <p>
 * An action is a special keystroke that an emulator can execute as ENTER or
 * SOMMAIRE. Here is the table of the valid emulator actions:
 * </p>
 * 
 * <table border="1" cellpadding="6">
 * <tr>
 * <td width="100%" colspan="2" align="center" bgcolor="#000080" height="23"><b><font
 * color="#FFFFFF">Videotex</font></b></td>
 * </tr>
 * <tr>
 * <td width="50%" align="center" height="23"><b>Description</b></td>
 * <td width="50%" align="center" height="23"><b>Code</b></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Suite</td>
 * <td width="50%" height="22">KSuite</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Sommaire</td>
 * <td width="50%" height="22">KSommaire</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Guide</td>
 * <td width="50%" height="22">KGuide</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Répétition</td>
 * <td width="50%" height="22">KRepetition</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Annulation</td>
 * <td width="50%" height="22">KAnnulation</td>
 * </tr>
 * <tr>
 * <td width="50%" height="13">Correction</td>
 * <td width="50%" height="13">KCorrection</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Retour</td>
 * <td width="50%" height="22">KRetour</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Envoi</td>
 * <td width="50%" height="22">KEnvoi</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Connexion/Fin</td>
 * <td width="50%" height="22">KCnxFin</td>
 * </tr>
 * <tr>
 * <td width="100%" colspan="2" align="center" bgcolor="#000080" height="23"><b><font
 * color="#FFFFFF">VT220</font></b></td>
 * </tr>
 * <tr>
 * <td width="50%" align="center" height="23"><b>Description</b></td>
 * <td width="50%" align="center" height="23"><b>Code</b></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F1</td>
 * <td width="50%" height="22">F01</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F2</td>
 * <td width="50%" height="22">F02</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F3</td>
 * <td width="50%" height="22">F03</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F4</td>
 * <td width="50%" height="22">F04</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F5</td>
 * <td width="50%" height="22">F05</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F6</td>
 * <td width="50%" height="22">F06</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F7</td>
 * <td width="50%" height="22">F07</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F8</td>
 * <td width="50%" height="22">F08</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F9</td>
 * <td width="50%" height="22">F09</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F10</td>
 * <td width="50%" height="22">F10</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F11</td>
 * <td width="50%" height="22">F11</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">F12</td>
 * <td width="50%" height="22">F12</td>
 * </tr>
 * <tr>
 * <td width="100%" colspan="2" align="center" bgcolor="#000080" height="23"><b><font
 * color="#FFFFFF">Bull</font></b></td>
 * </tr>
 * <tr>
 * <td width="50%" align="center" height="23"><b>Description</b></td>
 * <td width="50%" align="center" height="23"><b>Code</b></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">FKC<i>x</i> [1..12]</td>
 * <td width="50%" height="22">FKC<i>x</i></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Right arrow</td>
 * <td width="50%" height="22">RIGHT</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Left arrow</td>
 * <td width="50%" height="22">LEFT</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Up arrow</td>
 * <td width="50%" height="22">UP</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Down arrow</td>
 * <td width="50%" height="22">DOWN</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Delete active partition</td>
 * <td width="50%" height="22">CLEARAP</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Delete end of partition</td>
 * <td width="50%" height="22">CLEAREP</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Initialize active partition</td>
 * <td width="50%" height="22">INITAP</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Initialize two partitions</td>
 * <td width="50%" height="22">INITBP</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Partial initialization</td>
 * <td width="50%" height="22">INITPA</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Insert line</td>
 * <td width="50%" height="22">INSLINE</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Delete line</td>
 * <td width="50%" height="22">SUPLINE</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Delete end of line</td>
 * <td width="50%" height="22">ERAEOL</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Move cursor to begin of line</td>
 * <td width="50%" height="22">CURBOL</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Move cursor to begin of map</td>
 * <td width="50%" height="22">CURHOME</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Delete character</td>
 * <td width="50%" height="22">DELCHAR</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Insert character</td>
 * <td width="50%" height="22">INSCHAR</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Tabulation</td>
 * <td width="50%" height="22">TAB</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Back tabulation</td>
 * <td width="50%" height="22">BTAB</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Insert tabulation</td>
 * <td width="50%" height="22">INSTAB</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Delete tabulation</td>
 * <td width="50%" height="22">CLRTAB</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Baskspace</td>
 * <td width="50%" height="22">BS</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Total transmission</td>
 * <td width="50%" height="22">XMITALL</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Transmission</td>
 * <td width="50%" height="22">XMIT</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">Break</td>
 * <td width="50%" height="22">BREAK</td>
 * </tr>
 * <tr>
 * <td width="100%" colspan="2" align="center" bgcolor="#000080" height="23"><b><font
 * color="#FFFFFF">IBM</font></b></td>
 * </tr>
 * <tr>
 * <td width="50%" align="center" height="23"><b>Description</b></td>
 * <td width="50%" align="center" height="23"><b>Code</b></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">PA<i>x</i> [1..3]</td>
 * <td width="50%" height="22">KEY_PA<i>x</i> [1..3]</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22">SysReq</td>
 * <td width="50%" height="22"><font size="3">KEY_SYSREQ</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Enter</font></td>
 * <td width="50%" height="22"><font size="3">KEY_ENTER</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Attn</font></td>
 * <td width="50%" height="22"><font size="3">KEY_ATTN</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">PF<i>x</i></font><i></i>
 * [1..24]</td>
 * <td width="50%" height="22"><font size="3">KEY_PF<i>x</i></font><i></i>
 * [1..24]</td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Right arrow</font></td>
 * <td width="50%" height="22"><font size="3">KEY_CURRIGHT</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Left arrow</font></td>
 * <td width="50%" height="22"><font size="3">KEY_CURLEFT</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Up arrow</font></td>
 * <td width="50%" height="22"><font size="3">KEY_CURUP</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Down arrow</font></td>
 * <td width="50%" height="22"><font size="3">KEY_CURDOWN</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Backspace</font></td>
 * <td width="50%" height="22"><font size="3">KEY_BACKSP</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Tabulation</font></td>
 * <td width="50%" height="22"><font size="3">KEY_TAB</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Back tabulation</font></td>
 * <td width="50%" height="22"><font size="3">KEY_BACKTAB</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">New line</font></td>
 * <td width="50%" height="22"><font size="3">KEY_NEWLINE</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Move cursor to begin of map</font></td>
 * <td width="50%" height="22"><font size="3">KEY_HOME</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Insert</font></td>
 * <td width="50%" height="22"><font size="3">KEY_INSERT</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Delete</font></td>
 * <td width="50%" height="22"><font size="3">KEY_DELCHAR</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Reset</font></td>
 * <td width="50%" height="22"><font size="3">KEY_RESET</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Duplication</font></td>
 * <td width="50%" height="22"><font size="3">KEY_DUP</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Field mark</font></td>
 * <td width="50%" height="22"><font size="3">KEY_FLDMRK</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Delete end of line</font></td>
 * <td width="50%" height="22"><font size="3">KEY_ERASEEOF</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Delete entry</font></td>
 * <td width="50%" height="22"><font size="3">KEY_ERASEINPUT</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Current selection</font></td>
 * <td width="50%" height="22"><font size="3">KEY_CURSEL</font></td>
 * </tr>
 * <tr>
 * <td width="50%" height="22"><font size="3">Clear</font></td>
 * <td width="50%" height="22"><font size="3">KEY_CLEAR</font></td>
 * </tr>
 * </table>
 * @memberOf Javelin
 */
Javelin.prototype.doAction = function(action){};

/**
 * Writes a string on a given printer port. This port can be any string
 * containing LPT<em>x</em> or a network printer specification as
 * <code>\\SERVER\PRINTER</code> or a file as <code>print.txt</code> The
 * specified port will be opened, then the buffer will be written to it and the
 * port will be closed in this sequence. If the port is <code>null</code>,
 * then the port will be the default port specified as RawPrinter in the applet.
 * 
 * @param {String} printerPort
 *            the printer port.
 * @param {String} buffer
 *            the buffer to be printed.
 * 
 * @return <code>true</code> if ok, <code>false</code> otherwise.
 * @memberOf Javelin
 */
Javelin.prototype.printBuffer = function(printerPort, buffer){};

/**
 * Retrieves the terminal class of the Javelin object.
 * 
 * @return the terminal class.
 * @memberOf Javelin
 */
Javelin.prototype.getTerminalClass = function(){};

/**
 * Retrieves the DataStableOnCursorOn flag. this flags controls how the data
 * stable condition is detected. for Vidéotex emulators. if true, this condition
 * is met when a Vidéotex cursor is shown by the host.
 * 
 * @return the DataStableOnCursorOn.
 * @memberOf Javelin
 */
Javelin.prototype.getDataStableOnCursorOn = function(){};
/**
 * Sets the DataStableOnCursorOn flag.
 * 
 * @param {Boolean} bool the
 *            DataStableOnCursorOn flag.
 * @memberOf Javelin
 */
Javelin.prototype.setDataStableOnCursorOn = function(bool){};

/**
 * Sets the dataStableThreshold value. this value is the maximum time allowed
 * with no data received from the host to consider a dataStable condition.
 * 
 * @param {Number} the
 *            dataStableThreshold value
 * @memberOf Javelin
 */
Javelin.prototype.setDataStableThreshold = function(val){};

/**
 * Gets the dataStableThreshold value
 * 
 * @return the dataStableThreshold value
 * @memberOf Javelin
 */
Javelin.prototype.getDataStableThreshold = function(){};


/**
 * Sets the current terminal properties. the properties depends on the emulator
 * technology. only the 3270 and 5250 emulators supports this api
 * 
 * @param {Object} p
 *            the properties ti set in the emulator
 * @memberOf Javelin
 */
Javelin.prototype.setClientConfig = function(p){};

/**
 * Shows Zones on the emulator screen. the zones will be represented as
 * rectangles bounding characters. This feature can be used to enhance or to
 * show a grid system on the emulator screen. Zones are described in a vector of
 * rectangles in character 0 based coordinates.
 * 
 * Example Rectangle (0, 0, 10, 1){};
 * 
 * will describe a zone in Column 0, line 0, of 10 chars
 * 
 * @param {Object} rv
 *            the vector containing the zones rectangles. If rv is null, the
 *            zones will not be shown anymore.
 * 
 * @param {Object} cv
 *            the vector containing the color of each zone.
 * 
 * @memberOf Javelin
 */
Javelin.prototype.showZones = function(rv, cv){};

/**
 * Sets a selection zone on Javelin. this will result as if the user had
 * selected the zone with the mouse. After this api is called, the user will be
 * able to modify the selection zone.
 * 
 * @param {Object} zone
 *            the rectangle that holds the zone (x = column 0 based y = line 0
 *            based width = width of the selection in chars height= height of
 *            the selection in chars){};
 * @memberOf Javelin
 */

Javelin.prototype.setSelectionZone = function(zone){};
/**
 * Gets the current selection zone on Javelin.
 * 
 * @return zone the rectangle that holds the zone (x = column 0 based y = line 0
 *         based width = width of the selection in chars height= height of the
 *         selection in chars){};
 * @memberOf Javelin
 */
Javelin.prototype.getSelectionZone = function(){};

/**
 * Registers a client on the zoneListener. zoneListener delivers events for
 * selection changes.
 * 
 * @param {Object} l
 *            the component interested by the events.
 * @memberOf Javelin
 */
Javelin.prototype.addZoneListener = function(l){};

/**
 * Unregisters a client on the zoneListener. zoneListener delivers events for
 * selection changes.
 * 
 * @param {Object} l
 *            the component not anymore interested by the events.
 * @memberOf Javelin
 */
Javelin.prototype.removeZoneListener = function(l){};

/**
 * Unregisters all clients on the zoneListener. zoneListener delivers events for
 * selection changes.
 * 
 * @memberOf Javelin
 */
Javelin.prototype.removeAllZoneListeners = function(){};

/**
 * Registers a client on the keyListener. KeyListener delivers events for
 * keystokes on Javelin.
 * 
 * @param {Object} l the
 *            component interested by the events.
 * @memberOf Javelin
 */
Javelin.prototype.addKeyListener = function(l){};

/**
 * Unregisters a client on the keyListener. KeyListener delivers events for
 * events for keystokes on Javelin.
 * 
 * @param {Object} l the
 *            component not anymore interested by the events.
 * @memberOf Javelin
 */
Javelin.prototype.removeKeyListener = function(l){};

/**
 * Remove all arrived trigger from the MsgQueue.
 * 
 * @memberOf Javelin
 */
Javelin.prototype.clearTrigger = function(){};

/**
 * Get proprietary Internal data structure.
 * 
 * @memberOf Javelin
 */
Javelin.prototype.getInternalDataStructure = function(){};
