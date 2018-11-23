/*
 * Copyright (c) 1999-2004 TWinSoft sarl. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of TWinSoft sarl.
 * The program(s) may  be used  and/or copied  only with the written
 * permission  of TWinSoft  sarl or in accordance with the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * TWinSoft  makes  no  representations  or  warranties  about   the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness  for  a particular purpose, or non-infringement. TWinSoft
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 */

/*
 * $Workfile: keymapdku.js $
 * $Author: Davidm $
 * $Revision: 1 $
 * $Date: 6/09/07 16:45 $
 */


//keycode,doAction?,!shift,shift
var keymap_func=[
	[13,  true,  'XMIT'					,   'XMIT'                   ],
	[112, true,  'FCK01'				,   'FCK01'                  ],
	[113, true,  'FCK02'				,   'FCK02'                  ],
	[114, true,  'FCK03'				,   'FCK03'                  ],
	[115, true,  'FCK04'				,   'FCK04'                  ],
	[116, true,  'convertigo_refresh'	,   'convertigo_refresh'     ],
	[117, true,  'FCK06'				,   'FCK06'                  ],
	[118, true,  'FCK07'				,   'FCK07'                  ],
	[119, true,  'FCK08'				,   'FCK08'                  ],
	[120, true,  'FCK09'				,   'FCK09'                  ],
	[121, true,  'FCK10'				,   'FCK10'                  ],
	[122, true,  'FCK11'				,   'FCK11'                  ],
	[123, true,  'FCK12'				,   'FCK12'                  ],
	[38,  false, focusPrevField,            focusPrevField           ],
	[40,  false, focusNextField, focusNextField],
	[9,   false, focusNextField,            focusPrevField           ]
];
